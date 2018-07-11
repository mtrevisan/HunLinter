package unit731.hunspeller.parsers.hyphenation;

import unit731.hunspeller.parsers.hyphenation.valueobjects.HyphenationOptions;
import unit731.hunspeller.parsers.hyphenation.dtos.HyphenationBreak;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.SwingWorker;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.collections.radixtree.tree.RadixTree;
import unit731.hunspeller.collections.radixtree.tree.RadixTreeNode;
import unit731.hunspeller.collections.radixtree.tree.RadixTreeVisitor;
import unit731.hunspeller.collections.radixtree.sequencers.StringSequencer;
import unit731.hunspeller.interfaces.Resultable;
import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.languages.builders.ComparatorBuilder;
import unit731.hunspeller.languages.builders.OrthographyBuilder;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.FileService;
import unit731.hunspeller.services.PatternService;


/**
 * Implements Franklin Mark Liang's hyphenation algorithm with Petr Soijka's non–standard hyphenation extension.
 * 
 * @see <a href="https://tug.org/docs/liang/liang-thesis.pdf">Liang's thesis</a>
 * @see <a href="http://hunspell.sourceforge.net/tb87nemeth.pdf">László Németh's paper</a>
 * @see <a href="https://android.googlesource.com/platform/external/hyphenation/+/ics-mr0">László Németh's non–standard readme</a>
 * @see <a href="https://github.com/hunspell/hyphen">C source code</a>
 * @see <a href="https://wiki.openoffice.org/wiki/Documentation/SL/Using_TeX_hyphenation_patterns_in_OpenOffice.org">Using TeX hyphenation patterns in OpenOffice.org</a>
 */
@Slf4j
public class HyphenationParser{

	private static final String NEXT_LEVEL = "NEXTLEVEL";

	//Hyphens from the wikipedia article: https://en.wikipedia.org/wiki/Hyphen#Unicode
	public static final String HYPHEN = "\u2010";
	public static final String HYPHEN_MINUS = "\u002D";
	public static final String HYPHEN_EQUALS = "=";
	public static final String SOFT_HYPHEN = "\u00AD";
	public static final String EN_DASH = "\u2013";
	public static final String EM_DASH = "\u2014";
	private static final String APOSTROPHE = "'";
	private static final String RIGHT_SINGLE_QUOTATION_MARK = "\u2019";

	private static final String ONE = "1";
	public static final String WORD_BOUNDARY = ".";
	public static final String AUGMENTED_RULE = "/";

	private static final String COMMA = ",";

	private static final Matcher MATCHER_VALID_RULE = PatternService.matcher("^\\.?[^.]+\\.?$");
	private static final Matcher MATCHER_VALID_RULE_BREAK_POINTS = PatternService.matcher("[\\d]");
	private static final Matcher MATCHER_INVALID_RULE_START = PatternService.matcher("^\\.[\\d]");
	private static final Matcher MATCHER_INVALID_RULE_END = PatternService.matcher("[\\d]\\.$");
	private static final Matcher MATCHER_AUGMENTED_RULE = PatternService.matcher("^(?<rule>[^/]+)/(?<addBefore>.*?)(?:=|(?<hyphen>.)_)(?<addAfter>[^,]*)(?:,(?<start>\\d+),(?<cut>\\d+))?$");
	private static final Matcher MATCHER_AUGMENTED_RULE_HYPHEN_INDEX = PatternService.matcher("[13579]");

	private static final Matcher MATCHER_HYPHEN_MINUS_OR_EQUALS = PatternService.matcher("[" + HYPHEN_MINUS + HYPHEN_EQUALS + "]");
	private static final Matcher MATCHER_POINTS_AND_NUMBERS = PatternService.matcher("[.\\d]");
	private static final Matcher MATCHER_KEY = PatternService.matcher("\\d|/.+$");
	private static final Matcher MATCHER_HYPHENATION_POINT = PatternService.matcher("[^13579]|/.+$");

	public static final Matcher MATCHER_REDUCE = PatternService.matcher("/.+$");
	private static final Matcher MATCHER_COMMENT = PatternService.matcher("^$|\\s*[%#].*$");
	private static final Matcher MATCHER_WORD_INITIAL = PatternService.matcher("^" + Pattern.quote(WORD_BOUNDARY));


	public static enum Level{FIRST, SECOND}

	private static final ReentrantLock LOCK_SAVING = new ReentrantLock();


	private static final Map<Level, Set<String>> REDUCED_PATTERNS = new EnumMap<>(Level.class);
	static{
		for(Level level : Level.values())
			REDUCED_PATTERNS.put(level, new HashSet<>());
	}

	private final Comparator<String> comparator;
	@Getter
	private final Orthography orthography;

	@Getter
	private final Map<Level, RadixTree<String, String>> patterns = new EnumMap<>(Level.class);
	@Getter
	private final Map<Level, Map<String, String>> customHyphenations = new EnumMap<>(Level.class);
	@Getter
	private final HyphenationOptions options;


	public HyphenationParser(String language){
		Objects.requireNonNull(language);

		comparator = ComparatorBuilder.getComparator(language);
		orthography = OrthographyBuilder.getOrthography(language);

		Objects.requireNonNull(comparator);
		Objects.requireNonNull(orthography);

		for(Level level : Level.values()){
			patterns.put(level, RadixTree.createTree(new StringSequencer()));
			customHyphenations.put(level, new HashMap<>());
		}
		options = HyphenationOptions.createEmpty();
	}

	HyphenationParser(String language, Map<Level, RadixTree<String, String>> patterns, Map<Level, Map<String, String>> customHyphenations, HyphenationOptions options){
		Objects.requireNonNull(language);
		Objects.requireNonNull(patterns);

		comparator = ComparatorBuilder.getComparator(language);
		orthography = OrthographyBuilder.getOrthography(language);

		Objects.requireNonNull(comparator);
		Objects.requireNonNull(orthography);

		for(Level level : Level.values()){
			RadixTree<String, String> p = patterns.getOrDefault(level, RadixTree.createTree(new StringSequencer()));
			this.patterns.put(level, p);
		}
		customHyphenations = Optional.ofNullable(customHyphenations).orElse(Collections.<Level, Map<String, String>>emptyMap());
		for(Level level : Level.values()){
			Map<String, String> ch = customHyphenations.getOrDefault(level, Collections.<String, String>emptyMap());
			this.customHyphenations.put(level, ch);
		}
		this.options = (Objects.nonNull(options)? options: HyphenationOptions.createEmpty());
	}

	public void acquireLock(){
		LOCK_SAVING.lock();
	}

	public void releaseLock(){
		LOCK_SAVING.unlock();
	}

	@AllArgsConstructor
	public static class ParserWorker extends SwingWorker<Void, String>{

		private final File hypFile;
		private final HyphenationParser hypParser;
		private final Runnable postExecution;
		private final Resultable resultable;


		@Override
		protected Void doInBackground() throws Exception{
			LOCK_SAVING.lock();

			boolean stopped = false;
			try{
				publish("Opening Hyphenation file for parsing: " + hypFile.getName());
				setProgress(0);

				long readSoFar = 0l;
				long totalSize = hypFile.length();

				Level level = Level.FIRST;

				Path hypPath = hypFile.toPath();
				Charset charset = FileService.determineCharset(hypPath);
				try(BufferedReader br = Files.newBufferedReader(hypPath, charset)){
					String line = br.readLine();
					if(Charset.forName(line) != charset)
						throw new IllegalArgumentException("Hyphenation data file malformed, the first line is not '" + charset.name() + "'");

					REDUCED_PATTERNS.get(level).clear();

					while(Objects.nonNull(line = br.readLine())){
						readSoFar += line.length();

						line = removeComment(line);
						if(line.isEmpty())
							continue;

						if(!line.isEmpty()){
							boolean parsedLine = hypParser.options.parseLine(line);
							if(!parsedLine){
								if(line.startsWith(NEXT_LEVEL)){
									if(level == Level.SECOND)
										throw new IllegalArgumentException("Cannot have more than two levels");

									//start with non–compound level
									level = Level.SECOND;
									REDUCED_PATTERNS.get(level).clear();
								}
								else if(!isAugmentedRule(line) && line.contains(HYPHEN_EQUALS)){
									String key = PatternService.clear(line, MATCHER_HYPHEN_MINUS_OR_EQUALS);
									if(hypParser.customHyphenations.get(level).containsKey(key))
										throw new IllegalArgumentException("Custom hyphenation " + line + " is already present");

									hypParser.customHyphenations.get(level).put(key, StringUtils.replaceChars(line, HYPHEN_EQUALS, HYPHEN_MINUS));
								}
								else{
									validateRule(line, level);

									String key = getKeyFromData(line);
									boolean duplicatedRule = isRuleDuplicated(key, line, level);
									if(duplicatedRule)
										publish("Duplication found: " + line);
									else
										//insert current pattern into the radix tree (remove all numbers)
										hypParser.patterns.get(level).put(key, line);
								}
							}
						}

						setProgress((int)((readSoFar * 100.) / totalSize));
					}

					if(level == Level.FIRST){
						//dash and apostrophe are added by default (retro-compatibility)
						List<String> addedNoHyphen = new ArrayList<>(Arrays.asList(APOSTROPHE, HYPHEN_MINUS));
						if(charset == StandardCharsets.UTF_8)
							addedNoHyphen.addAll(Arrays.asList(RIGHT_SINGLE_QUOTATION_MARK, EN_DASH));

						hypParser.options.getNoHyphen().addAll(addedNoHyphen);

						for(String noHyphen : addedNoHyphen){
							line = ONE + noHyphen + ONE;
							if(!isRuleDuplicated(noHyphen, line, level))
								hypParser.patterns.get(level).put(noHyphen, line);
						}
					}

					hypParser.postParsingInitialization();

					setProgress(100);
				}
//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(hypParser.patterns));
//103 352 B compact trie
//106 800 B basic trie

				publish("Finished reading Hyphenation file");
			}
			catch(IOException | IllegalArgumentException e){
				stopped = true;

				publish(e instanceof ClosedChannelException? "Hyphenation parser thread interrupted": e.getClass().getSimpleName() + ": "
					+ e.getMessage());
			}
			catch(Exception e){
				stopped = true;

				String message = ExceptionService.getMessage(e, getClass());
				publish(e.getClass().getSimpleName() + ": " + message);
			}
			finally{
				LOCK_SAVING.unlock();
			}
			if(stopped)
				publish("Stopped reading Hyphenation file");

			return null;
		}

		private boolean isRuleDuplicated(String key, String line, Level level){
			boolean duplicatedRule = false;
			String foundNodeValue = hypParser.patterns.get(level).get(key);
			if(Objects.nonNull(foundNodeValue)){
				String clearedLine = PatternService.clear(line, MATCHER_REDUCE);
				String clearedFoundNodeValue = PatternService.clear(foundNodeValue, MATCHER_REDUCE);
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
		private String removeComment(String line){
			//remove comments
			line = PatternService.clear(line, MATCHER_COMMENT);
			//trim the entire string
			return StringUtils.strip(line);
		}

		@Override
		protected void process(List<String> chunks){
			resultable.printResultLine(chunks);
		}

		@Override
		protected void done(){
			if(Objects.nonNull(postExecution))
				postExecution.run();
		}
	}

	protected void postParsingInitialization(){}

	public void clear(){
		LOCK_SAVING.lock();

		try{
			patterns.values()
				.forEach(RadixTree::clear);
			customHyphenations.values()
				.forEach(Map::clear);
			if(Objects.nonNull(options))
				options.clear();
		}
		finally{
			LOCK_SAVING.unlock();
		}
	}

	/**
	 * NOTE: Calling the method {@link Orthography#correctOrthography(String)} may be necessary
	 * 
	 * @param rule	The rule to add
	 * @param level	Level to add the rule to
	 * @return The value of a rule if already in place, <code>null</code> if the insertion has completed successfully
	 */
	public String addRule(String rule, Level level){
		LOCK_SAVING.lock();

		try{
			validateRule(rule, level);

			String key = getKeyFromData(rule);
			String newRule = patterns.get(level).get(key);
			if(Objects.isNull(newRule))
				patterns.get(level).put(key, rule);

			return newRule;
		}
		finally{
			LOCK_SAVING.unlock();
		}
	}

	/**
	 * Line must contains exactly one hyphenation point
	 * 
	 * @param rule	Rule to be validated
	 * @param level	Level to add the rule to
	 */
	public static void validateRule(String rule, Level level){
		if(!PatternService.find(rule, MATCHER_VALID_RULE))
			throw new IllegalArgumentException("Rule " + rule + " has an invalid format");
		if(!PatternService.find(rule, MATCHER_VALID_RULE_BREAK_POINTS))
			throw new IllegalArgumentException("Rule " + rule + " has no hyphenation point(s)");
		if(PatternService.find(rule, MATCHER_INVALID_RULE_START) || PatternService.find(rule, MATCHER_INVALID_RULE_END))
			throw new IllegalArgumentException("Rule " + rule + " is invalid, the hyphenation point should not be adjacent to a dot");

		String cleanedRule = rule;
		int augmentedIndex = rule.indexOf('/');
		if(augmentedIndex >= 0){
			cleanedRule = rule.substring(0, augmentedIndex);
			int count = PatternService.clear(cleanedRule, MATCHER_HYPHENATION_POINT).length();
			if(count != 1)
				throw new IllegalArgumentException("Augmented rule " + rule + " has not exactly one hyphenation point");

			String[] parts = StringUtils.split(rule, COMMA);
			if(parts.length > 1){
				Matcher m = MATCHER_AUGMENTED_RULE_HYPHEN_INDEX.reset(rule);
				m.find();
				int index = m.start();

				int startIndex = (Objects.nonNull(parts[1])? Integer.parseInt(parts[1]) - 1: -1);
				int length = (parts.length > 2 && Objects.nonNull(parts[2])? Integer.parseInt(parts[2]): 0);
				if(startIndex < 0 || startIndex >= index)
					throw new IllegalArgumentException("Augmented rule " + rule + " has the index number not less than the hyphenation point");
				if(length < 0 || startIndex + length < index)
					throw new IllegalArgumentException("Augmented rule " + rule + " has the length number not less than the hyphenation point");
				if(startIndex + length >= parts[0].length())
					throw new IllegalArgumentException("Augmented rule " + rule + " has the length number that exceeds the length of the rule");
			}
		}


		//a standard and a non–standard hyphenation pattern matching the same hyphenation point must not be on the same hyphenation level
		//(for instance, c1 and zuc1ker/k=k,3,2 are invalid, while c1 and zuc3ker/k=k,3,2 are valid extended hyphenation patterns)
		String alreadyPresentRule = null;
		Set<String> reducedPatterns = REDUCED_PATTERNS.get(level);
		for(String pattern : reducedPatterns)
			if(pattern.contains(cleanedRule) || cleanedRule.contains(pattern)){
				alreadyPresentRule = pattern;
				break;
			}
		if(Objects.nonNull(alreadyPresentRule))
			throw new IllegalArgumentException("Pattern " + rule + " already present as " + alreadyPresentRule);

		reducedPatterns.add(cleanedRule);
	}

	public void save(File hypFile) throws IOException{
		LOCK_SAVING.lock();

		try{
			Charset charset = StandardCharsets.UTF_8;
			try(BufferedWriter writer = Files.newBufferedWriter(hypFile.toPath(), charset)){
				//save charset
				writeln(writer, charset.name());
				//save options
				options.write(writer);

				savePatternsByLevel(writer, Level.FIRST);

				writeln(writer, NEXT_LEVEL);

				savePatternsByLevel(writer, Level.SECOND);
			}
		}
		finally{
			LOCK_SAVING.unlock();
		}
	}

	private void savePatternsByLevel(final BufferedWriter writer, Level level) throws IOException{
		//extract (compound) data from the radix tree
		RadixTreeVisitor<String, String, Map<Integer, List<String>>> visitor = new RadixTreeVisitor<String, String, Map<Integer, List<String>>>(new HashMap<>()){
			@Override
			public boolean visit(String key, RadixTreeNode<String, String> node, RadixTreeNode<String, String> parent){
				String value = node.getValue();
				result.computeIfAbsent(value.length(), k -> new ArrayList<>())
					.add(value);
				
				return false;
			}
		};
		
		patterns.get(level).visitPrefixedBy(visitor);

		//sort values
		visitor.getResult().values()
			.forEach(v -> Collections.sort(v, comparator::compare));
		List<String> rules = visitor.getResult().values().stream()
			.flatMap(List::stream)
			.collect(Collectors.toList());
		for(String rule : rules)
			writeln(writer, rule);

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
		LOCK_SAVING.lock();

		try{
			String key = getKeyFromData(rule);
			return (customHyphenations.get(level).containsKey(key) || patterns.get(level).containsKey(key));
		}
		finally{
			LOCK_SAVING.unlock();
		}
	}

	public static String getKeyFromData(String rule){
		return PatternService.clear(rule, MATCHER_KEY);
	}


	public static boolean[] extractUppercases(String word){
		int size = word.length();
		boolean[] uppercases = new boolean[size];
		for(int i = 0; i < size; i ++)
			if(Character.isUpperCase(word.charAt(i)))
				uppercases[i] = true;
		return uppercases;
	}

	public static List<String> restoreUppercases(List<String> hyphenatedWord, boolean[] uppercases){
		int size = uppercases.length;
		for(int i = 0; i < size; i ++)
			if(uppercases[i]){
				int j = i;
				int indexSoFar = 0;
				String syll = hyphenatedWord.get(indexSoFar);
				while(j > syll.length()){
					j -= syll.length();
					indexSoFar ++;
					syll = hyphenatedWord.get(indexSoFar);
				}
				StringBuilder syllabe = new StringBuilder(syll);
				String chr = Character.valueOf(syllabe.charAt(j)).toString();
				syllabe.setCharAt(j, chr.toUpperCase(Locale.ROOT).charAt(0));
				hyphenatedWord.set(indexSoFar, syllabe.toString());
			}
		return hyphenatedWord;
	}

	public static int getNormalizedLength(String word){
		return Normalizer.normalize(word, Normalizer.Form.NFKC).length();
	}

	public static int getNormalizedLength(String word, int index){
		return Normalizer.normalize(word.substring(0, index - 1), Normalizer.Form.NFKC).length() + 1;
	}

	public void enforceNoHyphens(String word, int[] indexes, String[] rules, String[] augmentedPatternData){
		int size = word.length() + WORD_BOUNDARY.length() * 2;

		Set<String> noHyphen = options.getNoHyphen();
		for(String nohyp : noHyphen){
			int nohypLength = nohyp.length();
			if(nohyp.charAt(0) == '^'){
				nohyp = nohyp.substring(1);
				if(word.startsWith(nohyp)){
					resetBreakpoint(indexes, rules, augmentedPatternData, 0);
					resetBreakpoint(indexes, rules, augmentedPatternData, nohypLength - 1);
				}
			}
			else if(nohyp.charAt(nohypLength - 1) == '$'){
				nohyp = nohyp.substring(0, nohypLength - 1);
				if(word.endsWith(nohyp)){
					resetBreakpoint(indexes, rules, augmentedPatternData, size - nohypLength - 1);
					resetBreakpoint(indexes, rules, augmentedPatternData, size - 2);
				}
			}
			else{
				int idx = -1;
				while((idx = word.indexOf(nohyp, idx + 1)) >= 0){
					resetBreakpoint(indexes, rules, augmentedPatternData, idx);
					resetBreakpoint(indexes, rules, augmentedPatternData, idx + nohypLength);
				}
			}
		}
	}

	private void resetBreakpoint(int[] indexes, String[] rules, String[] augmentedPatternData, int index){
		if(index < indexes.length){
			indexes[index] = 0;
			rules[index] = null;
			augmentedPatternData[index] = null;
		}
	}

	public static boolean isAugmentedRule(String line){
		return line.contains(AUGMENTED_RULE);
	}

	public static List<String> createHyphenatedWord(String word, HyphenationBreak hyphBreak){
		List<String> result = new ArrayList<>();

		int startIndex = 0;
		int size = word.length();
		int after = 0;
		String addAfter = null;
		for(int endIndex = 0; endIndex < size; endIndex ++)
			if(hyphBreak.isBreakpoint(endIndex)){
				String subword = word.substring(startIndex, endIndex);

				if(StringUtils.isNotBlank(addAfter)){
					//append first characters to next subword
					subword = addAfter + subword.substring(after);
					after = 0;
					addAfter = null;
				}

				//manage augmented patterns:
				String augmentedPatternData = hyphBreak.getAugmentedPatternData(endIndex);
				if(Objects.nonNull(augmentedPatternData)){
					Matcher m = MATCHER_AUGMENTED_RULE_HYPHEN_INDEX.reset(PatternService.clear(augmentedPatternData, MATCHER_WORD_INITIAL));
					m.find();
					int index = m.start();

					m = MATCHER_AUGMENTED_RULE.reset(augmentedPatternData);
					m.find();
					String addBefore = m.group("addBefore");
					addAfter = m.group("addAfter");
					String start = m.group("start");
					String cut = m.group("cut");
					if(Objects.isNull(start)){
						String rule = m.group("rule");
						start = Integer.toString(1);
						cut = Integer.toString(PatternService.clear(rule, MATCHER_POINTS_AND_NUMBERS).length());
					}

					//remove last characters from subword
					//  ll3a/aa=b,2,2
					//syll able
					//sylaa-bble
					int delta = index - Integer.parseInt(start) + 1;
					int end = subword.length() - delta;
					after = Integer.parseInt(cut) - delta;
					subword = subword.substring(0, end) + addBefore;
				}

				result.add(subword);
				startIndex = endIndex;
			}

		String subword = word.substring(startIndex);
		if(StringUtils.isNotBlank(addAfter))
			subword = addAfter + subword.substring(Math.min(Math.max(after, 0), subword.length()));
		result.add(subword);

		return result;
	}

}
