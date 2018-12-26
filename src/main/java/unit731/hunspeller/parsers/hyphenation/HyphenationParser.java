package unit731.hunspeller.parsers.hyphenation;

import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.collections.radixtree.tree.RadixTree;
import unit731.hunspeller.collections.radixtree.tree.RadixTreeNode;
import unit731.hunspeller.collections.radixtree.tree.RadixTreeVisitor;
import unit731.hunspeller.collections.radixtree.sequencers.StringSequencer;
import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.languages.BaseBuilder;
import unit731.hunspeller.parsers.hyphenation.vos.HyphenationOptionsParser;
import unit731.hunspeller.services.ExceptionHelper;
import unit731.hunspeller.services.FileHelper;
import unit731.hunspeller.services.PatternHelper;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


/**
 * Implements Franklin Mark Liang's hyphenation algorithm with Petr Soijka's non–standard hyphenation extension.
 * 
 * @see <a href="https://tug.org/docs/liang/liang-thesis.pdf">Liang's thesis</a>
 * @see <a href="http://hunspell.sourceforge.net/tb87nemeth.pdf">László Németh's paper</a>
 * @see <a href="https://android.googlesource.com/platform/external/hyphenation/+/ics-mr0">László Németh's non–standard readme</a>
 * @see <a href="https://github.com/hunspell/hyphen">C source code</a>
 * @see <a href="https://wiki.openoffice.org/wiki/Documentation/SL/Using_TeX_hyphenation_patterns_in_OpenOffice.org">Using TeX hyphenation patterns in OpenOffice.org</a>
 */
public class HyphenationParser extends ReadWriteLockable{

	private static final String NEXT_LEVEL = "NEXTLEVEL";

	//Hyphens from the wikipedia article: https://en.wikipedia.org/wiki/Hyphen#Unicode
	public static final String HYPHEN = "\u2010";
	public static final String HYPHEN_MINUS = "\u002D";
	public static final String MINUS_SIGN = "-";
	public static final String HYPHEN_EQUALS = "=";
	public static final String SOFT_HYPHEN = "\u00AD";
	public static final String EN_DASH = "\u2013";
	public static final String EM_DASH = "\u2014";
	public static final String APOSTROPHE = "ʼ";
	public static final String RIGHT_SINGLE_QUOTATION_MARK = "\u2019";
	/**
	 * https://en.wikipedia.org/wiki/Modifier_letter_apostrophe
	 * https://en.wikipedia.org/wiki/Quotation_mark
	 */
	public static final String MODIFIER_LETTER_APOSTROPHE = "\u02bc";

	public static final String BREAK_CHARACTER = SOFT_HYPHEN;

	private static final String ONE = "1";
	public static final String WORD_BOUNDARY = ".";
	public static final String AUGMENTED_RULE = "/";

	private static final String COMMA = ",";

	private static final Pattern PATTERN_VALID_RULE = PatternHelper.pattern("^\\.?[^.]+\\.?$");
	private static final Pattern PATTERN_VALID_RULE_BREAK_POINTS = PatternHelper.pattern("[\\d]");
	private static final Pattern PATTERN_INVALID_RULE_START = PatternHelper.pattern("^\\.[\\d]");
	private static final Pattern PATTERN_INVALID_RULE_END = PatternHelper.pattern("[\\d]\\.$");
	private static final Pattern PATTERN_AUGMENTED_RULE_HYPHEN_INDEX = PatternHelper.pattern("[13579]");

	public static final int PARAM_RULE = 1;
	public static final int PARAM_ADD_BEFORE = 2;
	public static final int PARAM_HYPHEN = 3;
	public static final int PARAM_ADD_AFTER = 4;
	public static final int PARAM_START = 5;
	public static final int PARAM_CUT = 6;
	public static final Pattern PATTERN_AUGMENTED_RULE = PatternHelper.pattern("^(?<rule>[^/]+)/(?<addBefore>.*?)(?:=|(?<hyphen>.)_)(?<addAfter>[^,]*)(?:,(?<start>\\d+),(?<cut>\\d+))?$");
	public static final Pattern PATTERN_POINTS_AND_NUMBERS = PatternHelper.pattern("[.\\d]");
	public static final Pattern PATTERN_WORD_INITIAL = PatternHelper.pattern("^" + Pattern.quote(HyphenationParser.WORD_BOUNDARY));

	public static final Pattern PATTERN_WORD_BOUNDARIES = PatternHelper.pattern(Pattern.quote(HyphenationParser.WORD_BOUNDARY));

	private static final Pattern PATTERN_EQUALS = PatternHelper.pattern(HYPHEN_EQUALS);
	private static final Pattern PATTERN_KEY = PatternHelper.pattern("\\d|/.+$");
	private static final Pattern PATTERN_HYPHENATION_POINT = PatternHelper.pattern("[^13579]|/.+$");

	public static final Pattern PATTERN_REDUCE = PatternHelper.pattern("/.+$");
	private static final Pattern PATTERN_COMMENT = PatternHelper.pattern("^$|\\s*[%#].*$");

	public static enum Level{NON_COMPOUND, COMPOUND}

	/** Extract (compound) data from the radix tree */
	private static RadixTreeVisitor<String, String, Map<Integer, List<String>>> SAVE_VISITOR = new RadixTreeVisitor<String, String, Map<Integer, List<String>>>(new HashMap<>()){
		@Override
		public boolean visit(String key, RadixTreeNode<String, String> node, RadixTreeNode<String, String> parent){
			String value = node.getValue();
			result.computeIfAbsent(value.length(), k -> new ArrayList<>())
				.add(value);

			return false;
		}
	};


	private static final Map<Level, Set<String>> REDUCED_PATTERNS = new EnumMap<>(Level.class);
	static{
		for(Level level : Level.values())
			REDUCED_PATTERNS.put(level, new HashSet<>());
	}

	private final Comparator<String> comparator;
	private final Orthography orthography;

	private boolean secondLevelPresent;
	public Pattern patternNoHyphen;
	private final Map<Level, RadixTree<String, String>> patterns = new EnumMap<>(Level.class);
	private final Map<Level, Map<String, String>> customHyphenations = new EnumMap<>(Level.class);
	private final HyphenationOptionsParser optParser;


	public HyphenationParser(String language){
		Objects.requireNonNull(language);

		comparator = BaseBuilder.getComparator(language);
		orthography = BaseBuilder.getOrthography(language);

		Objects.requireNonNull(comparator);
		Objects.requireNonNull(orthography);

		for(Level level : Level.values()){
			patterns.put(level, RadixTree.createTree(new StringSequencer()));
			customHyphenations.put(level, new HashMap<>());
		}
		optParser = new HyphenationOptionsParser();
	}

	HyphenationParser(String language, Map<Level, RadixTree<String, String>> patterns, Map<Level, Map<String, String>> customHyphenations, HyphenationOptionsParser optParser){
		Objects.requireNonNull(language);
		Objects.requireNonNull(patterns);

		comparator = BaseBuilder.getComparator(language);
		orthography = BaseBuilder.getOrthography(language);

		Objects.requireNonNull(comparator);
		Objects.requireNonNull(orthography);

		secondLevelPresent = patterns.containsKey(Level.COMPOUND);
		for(Level level : Level.values()){
			RadixTree<String, String> p = patterns.getOrDefault(level, RadixTree.createTree(new StringSequencer()));
			this.patterns.put(level, p);
		}
		customHyphenations = Optional.ofNullable(customHyphenations).orElse(Collections.<Level, Map<String, String>>emptyMap());
		for(Level level : Level.values()){
			Map<String, String> ch = customHyphenations.getOrDefault(level, Collections.<String, String>emptyMap());
			this.customHyphenations.put(level, ch);
		}
		this.optParser = (optParser != null? optParser: new HyphenationOptionsParser());
	}

	public Orthography getOrthography(){
		return orthography;
	}

	public boolean isSecondLevelPresent(){
		return secondLevelPresent;
	}

	public Pattern getPatternNoHyphen(){
		return patternNoHyphen;
	}

	public Map<Level, RadixTree<String, String>> getPatterns(){
		return patterns;
	}

	public Map<Level, Map<String, String>> getCustomHyphenations(){
		return customHyphenations;
	}

	public HyphenationOptionsParser getOptParser(){
		return optParser;
	}

	/**
	 * Parse the hyphenation rules out from a .dic file.
	 *
	 * @param hypFile	The content of the hyphenation file
	 * @throws IOException	If an I/O error occurs
	 * @throws	IllegalArgumentException	If something is wrong while parsing the file
	 */
	public void parse(File hypFile) throws IOException, IllegalArgumentException{
		acquireWriteLock();
		try{
			clearInternal();

			Level level = Level.NON_COMPOUND;

			Path hypPath = hypFile.toPath();
			Charset charset = FileHelper.determineCharset(hypPath);
			try(LineNumberReader br = new LineNumberReader(Files.newBufferedReader(hypPath, charset))){
				String line = extractLine(br);

				//ignore any BOM marker on first line
				if(br.getLineNumber() == 1)
					line = FileHelper.clearBOMMarker(line);
				if(Charset.forName(line) != charset)
					throw new IllegalArgumentException("Hyphenation data file malformed, the first line is not '" + charset.name() + "'");

				REDUCED_PATTERNS.get(level).clear();

				while((line = br.readLine()) != null){
					line = removeComment(line);
					if(line.isEmpty())
						continue;

					if(!line.isEmpty()){
						boolean parsedLine = optParser.parseLine(line);
						if(!parsedLine){
							if(line.startsWith(NEXT_LEVEL)){
								if(level == Level.COMPOUND)
									throw new IllegalArgumentException("Cannot have more than two levels");

								//start with non–compound level
								level = Level.COMPOUND;
								secondLevelPresent = true;
								REDUCED_PATTERNS.get(level).clear();
							}
							else if(!isAugmentedRule(line) && line.contains(HYPHEN_EQUALS)){
								String key = PatternHelper.clear(line, PATTERN_EQUALS);
								if(customHyphenations.get(level).containsKey(key))
									throw new IllegalArgumentException("Custom hyphenation " + line + " is already present");

								customHyphenations.get(level).put(key, StringUtils.replaceChars(line, HYPHEN_EQUALS, MINUS_SIGN));
							}
							else{
								validateRule(line, level);

								String key = getKeyFromData(line);
								boolean duplicatedRule = isRuleDuplicated(key, line, level);
								if(duplicatedRule)
									throw new IllegalArgumentException("Duplication found: " + line);
								else
									//insert current pattern into the radix tree (remove all numbers)
									patterns.get(level).put(key, line);
							}
						}
					}
				}

				if(level == Level.NON_COMPOUND){
					//dash and apostrophe are added by default (retro-compatibility)
					List<String> retroCompatibilityNoHyphen = new ArrayList<>(Arrays.asList(APOSTROPHE, MINUS_SIGN));
					if(charset == StandardCharsets.UTF_8)
						retroCompatibilityNoHyphen.addAll(Arrays.asList(RIGHT_SINGLE_QUOTATION_MARK, EN_DASH));

					patternNoHyphen = PatternHelper.pattern("[" + StringUtils.join(retroCompatibilityNoHyphen, StringUtils.EMPTY) + "]");

					optParser.getNoHyphen().addAll(retroCompatibilityNoHyphen);

					for(String noHyphen : retroCompatibilityNoHyphen){
						line = ONE + noHyphen + ONE;
						if(!isRuleDuplicated(noHyphen, line, level))
							patterns.get(level).put(noHyphen, line);
					}
				}
			}
//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(hypParser.patterns));
//103 352 B compact trie
//106 800 B basic trie
//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(hypParser.patterns));
//103 352 B compact trie
//106 800 B basic trie
//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(hypParser.patterns));
//103 352 B compact trie
//106 800 B basic trie
//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(hypParser.patterns));
//103 352 B compact trie
//106 800 B basic trie
//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(hypParser.patterns));
//103 352 B compact trie
//106 800 B basic trie
//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(hypParser.patterns));
//103 352 B compact trie
//106 800 B basic trie
//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(hypParser.patterns));
//103 352 B compact trie
//106 800 B basic trie
//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(hypParser.patterns));
//103 352 B compact trie
//106 800 B basic trie
//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(hypParser.patterns));
//103 352 B compact trie
//106 800 B basic trie
//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(hypParser.patterns));
//103 352 B compact trie
//106 800 B basic trie
//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(hypParser.patterns));
//103 352 B compact trie
//106 800 B basic trie
//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(hypParser.patterns));
//103 352 B compact trie
//106 800 B basic trie
//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(hypParser.patterns));
//103 352 B compact trie
//106 800 B basic trie
//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(hypParser.patterns));
//103 352 B compact trie
//106 800 B basic trie
//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(hypParser.patterns));
//103 352 B compact trie
//106 800 B basic trie
//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(hypParser.patterns));
//103 352 B compact trie
//106 800 B basic trie
		}
		catch(Throwable t){
			String message = ExceptionHelper.getMessage(t);
			throw new IllegalArgumentException(t.getClass().getSimpleName() + ": " + message);
		}
		finally{
			for(Level level : Level.values())
				REDUCED_PATTERNS.get(level).clear();

			releaseWriteLock();
		}
	}

	private String extractLine(final LineNumberReader br) throws IOException, EOFException{
		String line = br.readLine();
		if(line == null)
			throw new EOFException("Unexpected EOF while reading Hyphenation file");

		return line;
	}

	public static boolean isAugmentedRule(String line){
		return line.contains(AUGMENTED_RULE);
	}

	private boolean isRuleDuplicated(String key, String line, Level level){
		boolean duplicatedRule = false;
		String foundNodeValue = patterns.get(level).get(key);
		if(foundNodeValue != null){
			String clearedLine = PatternHelper.clear(line, PATTERN_REDUCE);
			String clearedFoundNodeValue = PatternHelper.clear(foundNodeValue, PATTERN_REDUCE);
			duplicatedRule = (clearedLine.contains(clearedFoundNodeValue) || clearedFoundNodeValue.contains(clearedLine));
		}
		return duplicatedRule;
	}

	/**
	 * Removes comment lines and then cleans up blank lines and trailing whitespace.
	 *
	 * @param {String} data	The data from an affix file.
	 * @return {String}		The cleaned-up data.
	 */
	private static String removeComment(String line){
		//remove comments
		line = PatternHelper.clear(line, PATTERN_COMMENT);
		//trim the entire string
		return StringUtils.strip(line);
	}

	public void clear(){
		acquireWriteLock();
		try{
			clearInternal();
		}
		finally{
			releaseWriteLock();
		}
	}

	private void clearInternal(){
		patterns.values()
			.forEach(RadixTree::clear);
		customHyphenations.values()
			.forEach(Map::clear);
		optParser.clear();
	}

	/**
	 * NOTE: Calling the method {@link Orthography#correctOrthography(String)} may be necessary
	 * 
	 * @param rule	The rule to add
	 * @param level	Level to add the rule to
	 * @return The value of a rule if already in place, <code>null</code> if the insertion has completed successfully
	 */
	public String addRule(String rule, Level level){
		validateRule(rule, level);

		String key = getKeyFromData(rule);
		String newRule = patterns.get(level).get(key);
		if(newRule == null)
			patterns.get(level).put(key, rule);

		return newRule;
	}

	/**
	 * Line must contains exactly one hyphenation point
	 * 
	 * @param rule	Rule to be validated
	 * @param level	Level to add the rule to
	 */
	public static void validateRule(String rule, Level level){
		validateBasicRules(rule);

		String cleanedRule = rule;
		int augmentedIndex = rule.indexOf('/');
		if(augmentedIndex >= 0){
			cleanedRule = rule.substring(0, augmentedIndex);
			validateAugmentedRule(cleanedRule, rule);
		}

		Set<String> reducedPatterns = REDUCED_PATTERNS.get(level);
		ensureUniqueness(reducedPatterns, cleanedRule, rule);

		reducedPatterns.add(cleanedRule);
	}

	private static void validateBasicRules(String rule) throws IllegalArgumentException{
		if(!PatternHelper.find(rule, PATTERN_VALID_RULE))
			throw new IllegalArgumentException("Rule " + rule + " has an invalid format");
		if(!PatternHelper.find(rule, PATTERN_VALID_RULE_BREAK_POINTS))
			throw new IllegalArgumentException("Rule " + rule + " has no hyphenation point(s)");
		if(PatternHelper.find(rule, PATTERN_INVALID_RULE_START) || PatternHelper.find(rule, PATTERN_INVALID_RULE_END))
			throw new IllegalArgumentException("Rule " + rule + " is invalid, the hyphenation point should not be adjacent to a dot");
	}

	private static void validateAugmentedRule(String cleanedRule, String rule) throws IllegalArgumentException{
		int count = PatternHelper.clear(cleanedRule, PATTERN_HYPHENATION_POINT).length();
		if(count != 1)
			throw new IllegalArgumentException("Augmented rule " + rule + " has not exactly one hyphenation point");
		String[] parts = StringUtils.split(rule, COMMA);
		if(parts.length > 1){
			int index = getIndexOfBreakpoint(rule);
			
			int startIndex = (parts[1] != null? Integer.parseInt(parts[1]) - 1: -1);
			int length = (parts.length > 2 && parts[2] != null? Integer.parseInt(parts[2]): 0);
			if(startIndex < 0 || startIndex >= index)
				throw new IllegalArgumentException("Augmented rule " + rule + " has the index number not less than the hyphenation point");
			if(length < 0 || startIndex + length < index)
				throw new IllegalArgumentException("Augmented rule " + rule + " has the length number not less than the hyphenation point");
			if(startIndex + length >= parts[0].length())
				throw new IllegalArgumentException("Augmented rule " + rule + " has the length number that exceeds the length of the rule");
		}
	}

	/**
	 * A standard and a non–standard hyphenation pattern matching the same hyphenation point must not be on the same hyphenation level
	 * (for instance, c1 and zuc1ker/k=k,3,2 are invalid, while c1 and zuc3ker/k=k,3,2 are valid extended hyphenation patterns)
	 */
	private static void ensureUniqueness(Set<String> reducedPatterns, String cleanedRule, String rule) throws IllegalArgumentException{
		String alreadyPresentRule = null;
		for(String pattern : reducedPatterns)
			if(pattern.contains(cleanedRule) || cleanedRule.contains(pattern)){
				alreadyPresentRule = pattern;
				break;
			}
		if(alreadyPresentRule != null)
			throw new IllegalArgumentException("Pattern " + rule + " already present as " + alreadyPresentRule);
	}

	public static int getIndexOfBreakpoint(String rule){
		Matcher m = PATTERN_AUGMENTED_RULE_HYPHEN_INDEX.matcher(rule);
		m.find();
		return m.start();
	}

	public void save(File hypFile) throws IOException{
		Charset charset = StandardCharsets.UTF_8;
		try(BufferedWriter writer = Files.newBufferedWriter(hypFile.toPath(), charset)){
			writeln(writer, charset.name());

			writer.newLine();
			optParser.write(writer);

			writer.newLine();
			savePatternsByLevel(writer, Level.NON_COMPOUND);

			writer.newLine();
			writeln(writer, NEXT_LEVEL);

			writer.newLine();
			savePatternsByLevel(writer, Level.COMPOUND);
		}
	}

	private void savePatternsByLevel(final BufferedWriter writer, Level level) throws IOException{
		patterns.get(level).visitPrefixedBy(SAVE_VISITOR);

		Collection<List<String>> values = SAVE_VISITOR.getResult().values();
		for(List<String> value : values){
			//sort values
			Collections.sort(value, comparator::compare);

			for(String rule : value)
				writeln(writer, rule);
		}

		//write custom hyphenations
		List<String> customs = new ArrayList<>(customHyphenations.get(level).values());
		customs.sort(comparator);
		for(String rule : customs)
			writeln(writer, rule);
	}

	private void writeln(BufferedWriter writer, String line) throws IOException{
		writer.write(line);
		writer.write(StringUtils.LF);
	}

	/**
	 * NOTE: Calling the method {@link Orthography#correctOrthography(String)} may be necessary
	 * 
	 * @param rule	The rule to be checked
	 * @param level	The level to check the rule for
	 * @return	Whether the hyphenator has the given rule
	 */
	public boolean hasRule(String rule, Level level){
		String key = getKeyFromData(rule);
		return (customHyphenations.get(level).containsKey(key) || patterns.get(level).containsKey(key));
	}

	public static String getKeyFromData(String rule){
		return PatternHelper.clear(rule, PATTERN_KEY);
	}

}
