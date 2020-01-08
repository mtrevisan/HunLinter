package unit731.hunlinter.parsers.hyphenation;

import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import unit731.hunlinter.collections.ahocorasicktrie.AhoCorasickTrie;
import unit731.hunlinter.collections.ahocorasicktrie.AhoCorasickTrieBuilder;
import unit731.hunlinter.parsers.workers.exceptions.HunLintException;
import unit731.hunlinter.services.FileHelper;
import unit731.hunlinter.services.PatternHelper;
import unit731.hunlinter.services.StringHelper;


/**
 * Implements Franklin Mark Liang's hyphenation algorithm with Petr Soijka's non–standard hyphenation extension.
 *
 * @see <a href="https://tug.org/docs/liang/liang-thesis.pdf">Liang's thesis</a>
 * @see <a href="http://hunspell.sourceforge.net/tb87nemeth.pdf">László Németh's paper</a>
 * @see <a href="https://android.googlesource.com/platform/external/hyphenation/+/ics-mr0">László Németh's non–standard readme</a>
 * @see <a href="https://github.com/hunspell/hyphen">C source code</a>
 * @see <a href="https://wiki.openoffice.org/wiki/Documentation/SL/Using_TeX_hyphenation_patterns_in_OpenOffice.org">Using TeX hyphenation patterns in OpenOffice.org</a>
 */
public class HyphenationParser{

	private static final MessageFormat WRONG_FILE_FORMAT = new MessageFormat("Hyphenation data file malformed, cannot determine charset for ''{0}''");
	private static final MessageFormat MORE_THAN_TWO_LEVELS = new MessageFormat("Cannot have more than two levels");
	private static final MessageFormat DUPLICATED_CUSTOM_HYPHENATION = new MessageFormat("Custom hyphenation ''{0}'' is already present");
	private static final MessageFormat DUPLICATED_HYPHENATION = new MessageFormat("Duplicate found: ''{0}''");
	private static final MessageFormat INVALID_RULE = new MessageFormat("Rule {0} has an invalid format");
	private static final MessageFormat INVALID_HYPHENATION_POINT = new MessageFormat("Rule {0} has no hyphenation point(s)");
	private static final MessageFormat INVALID_HYPHENATION_POINT_NEAR_DOT = new MessageFormat("Rule {0} is invalid, the hyphenation point should not be adjacent to a dot");
	private static final MessageFormat MORE_HYPHENATION_DOTS = new MessageFormat("Augmented rule {0} has not exactly one hyphenation point");
	private static final MessageFormat AUGMENTED_RULE_INDEX_NOT_LESS_THAN = new MessageFormat("Augmented rule {0} has the index number not less than the hyphenation point");
	private static final MessageFormat AUGMENTED_RULE_LENGTH_NOT_LESS_THAN = new MessageFormat("Augmented rule {0} has the length number not less than the hyphenation point");
	private static final MessageFormat AUGMENTED_RULE_LENGTH_EXCEEDS = new MessageFormat("Augmented rule {0} has the length number that exceeds the length of the rule");
	private static final MessageFormat DUPLICATED_RULE = new MessageFormat("Pattern {0} already present as {1}");

	private static final String NEXT_LEVEL = "NEXTLEVEL";

	//Hyphens from the wikipedia article: https://en.wikipedia.org/wiki/Hyphen#Unicode
//	private static final String HYPHEN = "\u2010";
//	private static final String HYPHEN_MINUS = "\u002D";
	public static final String MINUS_SIGN = "-";
	public static final String HYPHEN_EQUALS = "=";
	public static final String SOFT_HYPHEN = "\u00AD";
	public static final String EN_DASH = "\u2013";
//	private static final String EM_DASH = "\u2014";
	public static final String APOSTROPHE = "ʼ";
	public static final String RIGHT_MODIFIER_LETTER_APOSTROPHE = "\u02BC";
	/**
	 * https://en.wikipedia.org/wiki/Modifier_letter_apostrophe
	 * https://en.wikipedia.org/wiki/Quotation_mark
	 */
//	private static final String MODIFIER_LETTER_APOSTROPHE = "\u02bc";

	public static final String BREAK_CHARACTER = SOFT_HYPHEN;

	private static final String ONE = "1";
	public static final String WORD_BOUNDARY = ".";
	public static final String AUGMENTED_RULE = "/";

	private static final String COMMA = ",";

	private static final String ESCAPE_SEQUENCE = "^^";
	private static final Pattern PATTERN_ESCAPED_UNICODE = PatternHelper.pattern("(\\^{2}[a-fA-F0-9]{2})|.");
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
	private static final Pattern PATTERN_KEY = PatternHelper.pattern("[\\d=]|/.+$");
	private static final Pattern PATTERN_HYPHENATION_POINT = PatternHelper.pattern("[^13579]|/.+$");

	public static final Pattern PATTERN_REDUCE = PatternHelper.pattern("/.+$");
	private static final Pattern PATTERN_COMMENT = PatternHelper.pattern("^$|\\s*[%#].*$");

	public enum Level{NON_COMPOUND, COMPOUND}


	private static final Map<Level, Set<String>> REDUCED_PATTERNS = new EnumMap<>(Level.class);
	static{
		Arrays.stream(Level.values())
			.forEach(level -> REDUCED_PATTERNS.put(level, new HashSet<>()));
	}

	private final Comparator<String> comparator;

	private boolean secondLevelPresent;
	public Pattern patternNoHyphen;
	private final Map<Level, Map<String, String>> rules = new EnumMap<>(Level.class);
	private final Map<Level, AhoCorasickTrie<String>> patterns = new EnumMap<>(Level.class);
	private final Map<Level, Map<String, String>> customHyphenations = new EnumMap<>(Level.class);
	private HyphenationOptionsParser options;


	public HyphenationParser(final Comparator<String> comparator){
		Objects.requireNonNull(comparator);

		this.comparator = comparator;

		for(final Level level : Level.values()){
			rules.put(level, new HashMap<>());
			customHyphenations.put(level, new HashMap<>());
		}
		options = new HyphenationOptionsParser();
	}

	HyphenationParser(final Comparator<String> comparator, final Map<Level, AhoCorasickTrie<String>> patterns,
			Map<Level, Map<String, String>> customHyphenations, final HyphenationOptionsParser options){
		Objects.requireNonNull(patterns);
		Objects.requireNonNull(comparator);

		this.comparator = comparator;

		secondLevelPresent = patterns.containsKey(Level.COMPOUND);
		Arrays.stream(Level.values())
			.forEach(level -> this.patterns.put(level, patterns.get(level)));
		customHyphenations = Optional.ofNullable(customHyphenations).orElse(new HashMap<>(0));
		for(final Level level : Level.values()){
			final Map<String, String> ch = customHyphenations.getOrDefault(level, new HashMap<>(0));
			this.customHyphenations.put(level, ch);
		}
		this.options = (options != null? options: new HyphenationOptionsParser());
	}

	public boolean isSecondLevelPresent(){
		return secondLevelPresent;
	}

	public Pattern getPatternNoHyphen(){
		return patternNoHyphen;
	}

	public Map<Level, AhoCorasickTrie<String>> getPatterns(){
		return patterns;
	}

	public Map<Level, Map<String, String>> getCustomHyphenations(){
		return customHyphenations;
	}

	public HyphenationOptionsParser getOptions(){
		return options;
	}

	public void setOptions(final HyphenationOptionsParser options){
		this.options = options;
	}

	/**
	 * Parse the hyphenation rules out from a .dic file.
	 *
	 * @param hypFile	The content of the hyphenation file
	 * @throws HunLintException   If something is wrong while parsing the file
	 */
	public void parse(final File hypFile){
		clearInternal();

		final Path path = hypFile.toPath();
		Level level = Level.NON_COMPOUND;
		Charset charset = FileHelper.determineCharset(path);
		try(final LineNumberReader br = FileHelper.createReader(path, charset)){
			String line = extractLine(br);

			charset = FileHelper.readCharset(line);

			while((line = br.readLine()) != null){
				line = removeComment(line);
				if(line.isEmpty())
					continue;

				final boolean parsedLine = options.parseLine(line);
				if(parsedLine)
					continue;

				//extract next level
				if(line.equals(NEXT_LEVEL)){
					if(level == Level.COMPOUND)
						throw new HunLintException(MORE_THAN_TWO_LEVELS.format(new Object[0]));

					//start with non–compound level
					level = Level.COMPOUND;
					continue;
				}

				if(charset == StandardCharsets.ISO_8859_1)
					line = convertUnicode(line);

				if(isCustomRule(line))
					parseCustomRule(level, line);
				else{
					validateRule(line, level);

					parseCommonRule(level, line);
				}
			}

			if(level == Level.NON_COMPOUND)
				addDefaults(level, charset);
		}
		catch(final Exception t){
			throw new HunLintException(t.getMessage());
		}

		//build tries
		Arrays.stream(Level.values())
			.forEach(l -> buildTrie(l, rules.get(l)));

		secondLevelPresent = (level == Level.COMPOUND);
//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(hypParser.patterns));
//103 352 B compact trie
//106 800 B basic trie
	}

	private String extractLine(final LineNumberReader br) throws IOException{
		final String line = br.readLine();
		if(line == null)
			throw new EOFException("Unexpected EOF while reading Hyphenation file");

		return line;
	}

	/** Transform escaped unicode into true unicode (ex. `^^e1` into `á`) */
	private String convertUnicode(final String line){
		final String[] components = PatternHelper.extract(line, PATTERN_ESCAPED_UNICODE);
		for(int i = 0; i < components.length; i ++)
			if(components[i].startsWith(ESCAPE_SEQUENCE))
				components[i] = String.valueOf((char)Integer.parseInt(components[i].substring(2), 16));
		return StringHelper.join(null, components);
	}

	private void parseCustomRule(final Level level, final String line){
		final String key = PatternHelper.clear(line, PATTERN_EQUALS);
		if(customHyphenations.get(level).containsKey(key))
			throw new HunLintException(DUPLICATED_CUSTOM_HYPHENATION.format(new Object[]{line}));

		customHyphenations.get(level).put(key, line);
	}

	private void parseCommonRule(final Level level, final String line){
		final String key = getKeyFromData(line);
		final boolean duplicatedRule = isRuleDuplicated(key, line, level);
		if(duplicatedRule)
			throw new HunLintException(DUPLICATED_HYPHENATION.format(new Object[]{line}));

		//insert current pattern into the radix tree (remove all numbers)
		rules.get(level)
			.put(key, line);
	}

	private void addDefaults(final Level level, final Charset charset){
		//dash and apostrophe are added by default (retro-compatibility)
		final List<String> retroCompatibilityNoHyphen = new ArrayList<>(Arrays.asList(APOSTROPHE, MINUS_SIGN));
		if(charset == StandardCharsets.UTF_8)
			retroCompatibilityNoHyphen.addAll(Arrays.asList(RIGHT_MODIFIER_LETTER_APOSTROPHE, EN_DASH));

		patternNoHyphen = PatternHelper.pattern("[" + StringHelper.join(null, retroCompatibilityNoHyphen) + "]");

		options.getNoHyphen().addAll(retroCompatibilityNoHyphen);

		for(final String noHyphen : retroCompatibilityNoHyphen){
			final String line = ONE + noHyphen + ONE;
			if(!isRuleDuplicated(noHyphen, line, level))
				rules.get(level)
					.put(noHyphen, line);
		}
	}

	public static boolean isAugmentedRule(final String line){
		return line.contains(AUGMENTED_RULE);
	}

	public static boolean isCustomRule(final String line){
		return (!isAugmentedRule(line) && line.contains(HYPHEN_EQUALS));
	}

	private boolean isRuleDuplicated(final String key, final String line, final Level level){
		boolean duplicatedRule = false;
		final String foundNodeValue = rules.get(level)
			.get(key);
		if(foundNodeValue != null){
			final String clearedLine = PatternHelper.clear(line, PATTERN_REDUCE);
			final String clearedFoundNodeValue = PatternHelper.clear(foundNodeValue, PATTERN_REDUCE);
			duplicatedRule = (clearedLine.contains(clearedFoundNodeValue) || clearedFoundNodeValue.contains(clearedLine));
		}
		return duplicatedRule;
	}

	/**
	 * Removes comment lines and then cleans up blank lines and trailing whitespace.
	 *
	 * @param line	The line from an affix file
	 * @return The cleaned-up line
	 */
	private static String removeComment(String line){
		//remove comments
		line = PatternHelper.clear(line, PATTERN_COMMENT);
		//trim the entire string
		return StringUtils.strip(line);
	}

	public void clear(){
		clearInternal();
	}

	private void clearInternal(){
		Arrays.stream(Level.values())
			.map(REDUCED_PATTERNS::get)
			.forEach(Set::clear);
		customHyphenations.values()
			.forEach(Map::clear);
		options.clear();
	}

	/**
	 * NOTE: Calling the method {@link unit731.hunlinter.languages.Orthography#correctOrthography(String)} may be necessary
	 *
	 * @param rule	The rule to add
	 * @param level	Level to add the rule to
	 * @return The value of a rule if already in place, <code>null</code> if the insertion has completed successfully
	 */
	public String addRule(final String rule, final Level level){
		validateRule(rule, level);

		final String oldRule;
		if(isCustomRule(rule)){
			final String key = PatternHelper.clear(rule, PATTERN_EQUALS);
			oldRule = customHyphenations.get(level)
				.putIfAbsent(key, rule);
		}
		else{
			final String key = getKeyFromData(rule);
			final Map<String, String> rulesByLevel = rules.get(level);
			oldRule = rulesByLevel.get(key);
			if(oldRule == null){
				rulesByLevel.put(key, rule);

				buildTrie(level, rulesByLevel);
			}
		}
		return oldRule;
	}

	/**
	 * NOTE: Calling the method {@link unit731.hunlinter.languages.Orthography#correctOrthography(String)} may be necessary
	 *
	 * @param rule	The rule to remove
	 * @param level	Level to remove the rule from
	 * @return <code>true</code> if the removal has completed successfully
	 */
	public boolean removeRule(final String rule, final Level level){
		final String oldRule;
		if(isCustomRule(rule)){
			final String key = PatternHelper.clear(rule, PATTERN_EQUALS);
			oldRule = customHyphenations.get(level).get(key);
			if(oldRule != null)
				customHyphenations.get(level).remove(key);
		}
		else{
			final String key = getKeyFromData(rule);
			final Map<String, String> rulesByLevel = rules.get(level);
			oldRule = rulesByLevel.get(key);
			if(oldRule != null)
				buildTrie(level, rulesByLevel);
		}
		return (oldRule != null);
	}

	private void buildTrie(final Level level, final Map<String, String> rulesByLevel){
		final AhoCorasickTrie<String> trie = new AhoCorasickTrieBuilder<String>()
			.build(rulesByLevel);
		patterns.put(level, trie);
	}

	/**
	 * Line must contains exactly one hyphenation point
	 *
	 * @param rule	Rule to be validated
	 * @param level	Level to add the rule to
	 */
	public static void validateRule(final String rule, final Level level){
		validateBasicRules(rule);

		String cleanedRule = rule;
		final int augmentedIndex = rule.indexOf('/');
		if(augmentedIndex >= 0){
			cleanedRule = rule.substring(0, augmentedIndex);
			validateAugmentedRule(cleanedRule, rule);
		}

		final Set<String> reducedPatterns = REDUCED_PATTERNS.get(level);
		ensureUniqueness(reducedPatterns, cleanedRule, rule);
	}

	private static void validateBasicRules(final String rule){
		if(!PatternHelper.find(rule, PATTERN_VALID_RULE))
			throw new HunLintException(INVALID_RULE.format(new Object[]{rule}));
		if(!rule.contains(HYPHEN_EQUALS)){
			if(!PatternHelper.find(rule, PATTERN_VALID_RULE_BREAK_POINTS))
				throw new HunLintException(INVALID_HYPHENATION_POINT.format(new Object[]{rule}));
			if(PatternHelper.find(rule, PATTERN_INVALID_RULE_START) || PatternHelper.find(rule, PATTERN_INVALID_RULE_END))
				throw new HunLintException(INVALID_HYPHENATION_POINT_NEAR_DOT.format(new Object[]{rule}));
		}
	}

	private static void validateAugmentedRule(final String cleanedRule, final String rule){
		final int count = PatternHelper.clear(cleanedRule, PATTERN_HYPHENATION_POINT).length();
		if(count != 1)
			throw new HunLintException(MORE_HYPHENATION_DOTS.format(new Object[]{rule}));

		final String[] parts = StringUtils.split(rule, COMMA);
		if(parts.length > 1){
			final int index = getIndexOfBreakpoint(rule);

			final int startIndex = extractStartIndex(parts);
			if(startIndex < 0 || startIndex >= index)
				throw new HunLintException(AUGMENTED_RULE_INDEX_NOT_LESS_THAN.format(new Object[]{rule}));
			final int length = extractLength(parts);
			if(length < 0 || startIndex + length < index)
				throw new HunLintException(AUGMENTED_RULE_LENGTH_NOT_LESS_THAN.format(new Object[]{rule}));
			if(startIndex + length >= parts[0].length())
				throw new HunLintException(AUGMENTED_RULE_LENGTH_EXCEEDS.format(new Object[]{rule}));
		}
	}

	private static int extractStartIndex(final String[] parts) throws NumberFormatException{
		return (parts[1] != null? Integer.parseInt(parts[1]) - 1: -1);
	}

	private static int extractLength(final String[] parts) throws NumberFormatException{
		return (parts.length > 2 && parts[2] != null? Integer.parseInt(parts[2]): 0);
	}

	/**
	 * A standard and a non–standard hyphenation pattern matching the same hyphenation point must not be on the same hyphenation level
	 * (for instance, c1 and zuc1ker/k=k,3,2 are invalid, while c1 and zuc3ker/k=k,3,2 are valid extended hyphenation patterns)
	 */
	private static void ensureUniqueness(final Set<String> reducedPatterns, final String cleanedRule, final String rule){
		String alreadyPresentRule = null;
		for(final String pattern : reducedPatterns)
			if(pattern.contains(cleanedRule) || cleanedRule.contains(pattern)){
				alreadyPresentRule = pattern;
				break;
			}
		if(alreadyPresentRule != null)
			throw new HunLintException(DUPLICATED_RULE.format(new Object[]{rule, alreadyPresentRule}));
	}

	public static int getIndexOfBreakpoint(final String rule){
		final Matcher m = PATTERN_AUGMENTED_RULE_HYPHEN_INDEX.matcher(rule);
		m.find();
		return m.start();
	}

	public void save(final File hypFile) throws IOException{
		final Charset charset = StandardCharsets.UTF_8;
		try(final BufferedWriter writer = Files.newBufferedWriter(hypFile.toPath(), charset)){
			writeln(writer, charset.name());

			writer.newLine();
			options.write(writer);

			writer.newLine();
			savePatternsByLevel(writer, Level.NON_COMPOUND);

			writer.newLine();
			writeln(writer, NEXT_LEVEL);

			writer.newLine();
			savePatternsByLevel(writer, Level.COMPOUND);
		}
	}

	private void savePatternsByLevel(final BufferedWriter writer, final Level level) throws IOException{
		final List<String> patternsByLevel = new ArrayList<>(rules.get(level).values());
		patternsByLevel.sort(comparator);
		for(final String pattern : patternsByLevel)
			writeln(writer, pattern);

		//write custom hyphenations
		final List<String> customs = new ArrayList<>(customHyphenations.get(level).values());
		customs.sort(comparator);
		for(String rule : customs)
			writeln(writer, rule);
	}

	private void writeln(final BufferedWriter writer, final String line) throws IOException{
		writer.write(line);
		writer.write(StringUtils.LF);
	}

	/**
	 * NOTE: Calling the method {@link unit731.hunlinter.languages.Orthography#correctOrthography(String)} may be necessary
	 *
	 * @param rule	The rule to be checked
	 * @param level	The level to check the rule for
	 * @return	Whether the hyphenator has the given rule
	 */
	public boolean hasRule(final String rule, final Level level){
		return (isCustomRule(rule)?
			customHyphenations.get(level).containsValue(rule):
			rules.get(level).containsKey(getKeyFromData(rule)));
	}

	public static String getKeyFromData(final String rule){
		return PatternHelper.clear(rule, PATTERN_KEY);
	}

}
