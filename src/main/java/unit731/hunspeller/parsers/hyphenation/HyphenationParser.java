package unit731.hunspeller.parsers.hyphenation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.SwingWorker;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.collections.radixtree.tree.RadixTree;
import unit731.hunspeller.collections.radixtree.tree.RadixTreeNode;
import unit731.hunspeller.collections.radixtree.tree.RadixTreeVisitor;
import unit731.hunspeller.collections.radixtree.sequencers.StringSequencer;
import unit731.hunspeller.collections.radixtree.tree.SearchResult;
import unit731.hunspeller.interfaces.Resultable;
import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.languages.builders.ComparatorBuilder;
import unit731.hunspeller.languages.builders.OrthographyBuilder;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.FileService;
import unit731.hunspeller.services.PatternService;


/**
 * Implements Franklin Mark Liang's hyphenation algorithm with Petr Soijka's non-standard hyphenation extension.
 * 
 * @see <a href="https://tug.org/docs/liang/liang-thesis.pdf">Liang's thesis</a>
 * @see <a href="http://hunspell.sourceforge.net/tb87nemeth.pdf">László Németh's paper</a>
 * @see <a href="https://android.googlesource.com/platform/external/hyphenation/+/ics-mr0">László Németh's non-standard readme</a>
 * @see <a href="https://github.com/hunspell/hyphen">C source code</a>
 * @see <a href="https://wiki.openoffice.org/wiki/Documentation/SL/Using_TeX_hyphenation_patterns_in_OpenOffice.org">Using TeX hyphenation patterns in OpenOffice.org</a>
 */
public class HyphenationParser{

	private static final String NEXT_LEVEL = "NEXTLEVEL";

	//Hyphens from the wikipedia article: https://en.wikipedia.org/wiki/Hyphen#Unicode
	public static final String HYPHEN = "\u2010";
	public static final String HYPHEN_MINUS = "\u002D";
	private static final String HYPHEN_EQUALS = "=";
	private static final String SOFT_HYPHEN = "\u00AD";
	private static final String EN_DASH = "\u2013";
	private static final String RIGHT_SINGLE_QUOTATION_MARK = "\u2019";

	private static final String ONE = "1";
	private static final String WORD_BOUNDARY = ".";
	private static final String AUGMENTED_RULE = "/";

	private static final Pattern PATTERN_COMMA = PatternService.pattern(",");

	private static final Matcher MATCHER_VALID_RULE = PatternService.matcher("^\\.?[^.]+\\.?$");
	private static final Matcher MATCHER_VALID_RULE_BREAK_POINTS = PatternService.matcher("[\\d]");
	private static final Matcher MATCHER_INVALID_RULE_START = PatternService.matcher("^\\.[\\d]");
	private static final Matcher MATCHER_INVALID_RULE_END = PatternService.matcher("[\\d]\\.$");
	private static final Matcher MATCHER_AUGMENTED_RULE = PatternService.matcher("^(?<rule>[^/]+)/(?<addBefore>.*?)(?:=|(?<hyphen>.)_)(?<addAfter>[^,]*)(?:,(?<start>\\d+),(?<cut>\\d+))?$");
	private static final Matcher MATCHER_AUGMENTED_RULE_HYPHEN_INDEX = PatternService.matcher("[13579]");

	private static final Pattern PATTERN_HYPHEN_MINUS = PatternService.pattern(HYPHEN_MINUS);
	private static final Matcher MATCHER_HYPHEN_MINUS_OR_EQUALS = PatternService.matcher("[" + HYPHEN_MINUS + HYPHEN_EQUALS + "]");
	private static final Matcher MATCHER_HYPHENS = PatternService.matcher("[" + Pattern.quote(HYPHEN) + "]");
	private static final Matcher MATCHER_WORD_BOUNDARIES = PatternService.matcher("[" + Pattern.quote(WORD_BOUNDARY) + "]");
	private static final Matcher MATCHER_POINTS_AND_NUMBERS = PatternService.matcher("[.\\d]");
	private static final Matcher MATCHER_KEY = PatternService.matcher("\\d|/.+$");
	private static final Matcher MATCHER_HYPHENATION_POINT = PatternService.matcher("[^13579]|/.+$");

	private static final Matcher MATCHER_REDUCE = PatternService.matcher("/.+$");
	private static final Matcher MATCHER_COMMENT = PatternService.matcher("^$|\\s*%.*$");
	private static final Matcher MATCHER_WORD_INITIAL = PatternService.matcher("^" + Pattern.quote(WORD_BOUNDARY));

	public static enum Level{
		//defines the rules to be used at compound word boundaries
		COMPOUND,
		//defines the rules to be used within words or word parts
		NON_COMPOUND
	};

	private static final ReentrantLock LOCK_SAVING = new ReentrantLock();


	private static final Map<Level, Set<String>> REDUCED_PATTERNS = new EnumMap<>(Level.class);
	static{
		for(Level level : Level.values())
			REDUCED_PATTERNS.put(level, new HashSet<>());
	}

	private final Comparator<String> comparator;
	private final Orthography orthography;

	private final Map<Level, RadixTree<String, String>> patterns = new EnumMap<>(Level.class);
	private final Map<Level, Map<String, String>> customHyphenations = new EnumMap<>(Level.class);
	private HyphenationOptions options;


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

	public HyphenationParser(String language, Map<Level, RadixTree<String, String>> patterns, Map<Level, Map<String, String>> customHyphenations, HyphenationOptions options){
		this(language);

		if(patterns != null)
			for(Level level : Level.values()){
				RadixTree<String, String> p = patterns.get(level);
				if(p != null)
					this.patterns.put(level, p);
			}
		if(customHyphenations != null)
			for(Level level : Level.values()){
				Map<String, String> ch = customHyphenations.get(level);
				if(ch != null)
					this.customHyphenations.put(level, ch);
			}
		if(options != null)
			this.options = options;
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

			try{
				publish("Opening Hyphenation file for parsing: " + hypFile.getName());
				setProgress(0);

				long readSoFar = 0l;
				long totalSize = hypFile.length();

				Charset charset = FileService.determineCharset(hypFile.toPath());
				try(BufferedReader br = Files.newBufferedReader(hypFile.toPath(), charset)){
					String line = br.readLine();
					if(Charset.forName(line) != charset)
						throw new IllegalArgumentException("Hyphenation data file malformed, the first line is not '" + charset.name() + "'");

					//start with compound level
					Level level = Level.COMPOUND;
					REDUCED_PATTERNS.get(level).clear();

					while((line = br.readLine()) != null){
						readSoFar += line.length();

						line = removeComment(line);
						if(line.isEmpty())
							continue;

						if(!line.isEmpty()){
							boolean parsedLine = hypParser.options.parseLine(line);
							if(!parsedLine){
								if(line.startsWith(NEXT_LEVEL)){
									if(level == Level.NON_COMPOUND)
										throw new IllegalArgumentException("Cannot have more than two levels");

									//start with non-compound level
									level = Level.NON_COMPOUND;
									REDUCED_PATTERNS.get(level).clear();
								}
								else if(line.contains(HYPHEN_MINUS) || line.contains(HYPHEN_EQUALS)){
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

					if(level == Level.COMPOUND && charset == StandardCharsets.UTF_8){
						//en-dash and right single quotation mark added by default (retro-compatibility)
						hypParser.options.getNoHyphen().addAll(Arrays.asList(EN_DASH, RIGHT_SINGLE_QUOTATION_MARK));

						line = ONE + EN_DASH + ONE;
						if(!isRuleDuplicated(EN_DASH, line, level))
							hypParser.patterns.get(level).put(EN_DASH, line);

						line = ONE + RIGHT_SINGLE_QUOTATION_MARK + ONE;
						if(!isRuleDuplicated(RIGHT_SINGLE_QUOTATION_MARK, line, level))
							hypParser.patterns.get(level).put(RIGHT_SINGLE_QUOTATION_MARK, line);
					}

					hypParser.patterns.get(Level.COMPOUND).prepare();
					hypParser.patterns.get(Level.NON_COMPOUND).prepare();

					setProgress(100);
				}
//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(hypParser.patterns));
//103 352 B compact trie
//106 800 B basic trie

				publish("Finished reading Hyphenation file");
			}
			catch(IOException | IllegalArgumentException e){
				publish(e instanceof ClosedChannelException? "Hyphenation parser thread interrupted": e.getClass().getSimpleName() + ": "
					+ e.getMessage());
			}
			catch(Exception e){
				String message = ExceptionService.getMessage(e, getClass());
				publish(e.getClass().getSimpleName() + ": " + message);
				publish("Stopped reading Hyphenation file");
			}
			finally{
				LOCK_SAVING.unlock();
			}
			return null;
		}

		private boolean isRuleDuplicated(String key, String line, Level level){
			boolean duplicatedRule = false;
			String foundNodeValue = hypParser.patterns.get(level).get(key);
			if(foundNodeValue != null){
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
			if(postExecution != null)
				postExecution.run();
		}
	};

	public void clear(){
		LOCK_SAVING.lock();

		try{
			patterns.values()
				.forEach(RadixTree::clear);
			customHyphenations.values()
				.forEach(Map::clear);
			if(options != null)
				options.clear();
		}
		finally{
			LOCK_SAVING.unlock();
		}
	}

	public String correctOrthography(String text){
		text = text.toLowerCase(Locale.ROOT);
		return orthography.correctOrthography(text);
	}

	/**
	 * NOTE: Calling the method {@link #correctOrthography(String)} may be necessary
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
			if(newRule == null)
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

			String[] parts = PatternService.split(rule, PATTERN_COMMA);
			if(parts.length > 1){
				Matcher m = MATCHER_AUGMENTED_RULE_HYPHEN_INDEX.reset(rule);
				m.find();
				int index = m.start();

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


		//a standard and a non-standard hyphenation pattern matching the same hyphenation point must not be on the same hyphenation level
		//(for instance, c1 and zuc1ker/k=k,3,2 are invalid, while c1 and zuc3ker/k=k,3,2 are valid extended hyphenation patterns)
		String alreadyPresentRule = null;
		Set<String> reducedPatterns = REDUCED_PATTERNS.get(level);
		for(String pattern : reducedPatterns)
			if(pattern.contains(cleanedRule) || cleanedRule.contains(pattern)){
				alreadyPresentRule = pattern;
				break;
			}
		if(alreadyPresentRule != null)
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

				savePatternsByLevel(writer, Level.COMPOUND);

				writeln(writer, NEXT_LEVEL);

				savePatternsByLevel(writer, Level.NON_COMPOUND);
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
	 * Performs hyphenation
	 * NOTE: Calling the method {@link #correctOrthography(String)} may be necessary
	 *
	 * @param word	String to hyphenate
	 * @return the hyphenation object
	 */
	public Hyphenation hyphenate(String word){
		return hyphenate(word, patterns);
	}

	/**
	 * Performs hyphenation including an additional rule
	 * NOTE: Calling the method {@link #correctOrthography(String)} may be necessary
	 *
	 * @param word	String to hyphenate
	 * @param addedRule	Rule to add to the set of rules that will generate the hyphenation
	 * @param level	The level to add the rule to
	 * @return the hyphenation object
	 * @throws CloneNotSupportedException	If the radix tree does not support the {@code Cloneable} interface
	 */
	public Hyphenation hyphenate(String word, String addedRule, Level level) throws CloneNotSupportedException{
		LOCK_SAVING.lock();

		try{
			String key = getKeyFromData(addedRule);
			Hyphenation hyph = null;
			if(!patterns.get(level).containsKey(key)){
				patterns.get(level).put(key, addedRule);

				hyph = hyphenate(word, patterns);

				patterns.get(level).remove(key);
			}
			return hyph;
		}
		finally{
			LOCK_SAVING.unlock();
		}
	}

	/**
	 * NOTE: Calling the method {@link #correctOrthography(String)} may be necessary
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

	private static String getKeyFromData(String rule){
		return PatternService.clear(rule, MATCHER_KEY);
	}


	/**
	 * Performs hyphenation
	 * NOTE: Calling the method {@link #correctOrthography(String)} may be necessary
	 *
	 * @param word	String to hyphenate
	 * @param patterns	The radix tree containing the patterns
	 * @return the hyphenation object
	 */
	private Hyphenation hyphenate(String word, Map<Level, RadixTree<String, String>> patterns){
		boolean[] uppercases = extractUppercases(word);

		//clear already present hyphens
		word = PatternService.replaceAll(word, MATCHER_HYPHENS, SOFT_HYPHEN);
		//clear already present word boundaries' characters
		word = PatternService.clear(word, MATCHER_WORD_BOUNDARIES);

		List<String> hyphenatedWord;
		List<String> rules;
		boolean[] errors;

		//FIXME manage second level
		Level level = Level.COMPOUND;

		String customHyphenation = customHyphenations.get(level).get(word);
		if(customHyphenation != null){
			//hyphenation is custom
			hyphenatedWord = Arrays.asList(PatternService.split(customHyphenation, PATTERN_HYPHEN_MINUS));

			rules = hyphenatedWord;
		}
		else if(word.length() <= options.getLeftMin() + options.getRightMin()){
			//ignore short words (early out)
			hyphenatedWord = Arrays.asList(word);

			rules = hyphenatedWord;
		}
		else{
			HyphenationBreak hyphBreak = calculateBreakpoints(word, patterns, level);

			hyphenatedWord = createHyphenatedWord(word, hyphBreak);

			rules = Arrays.asList(hyphBreak.getRules());
		}
		errors = orthography.getSyllabationErrors(hyphenatedWord);

		hyphenatedWord = restoreUppercases(hyphenatedWord, uppercases);

		return new Hyphenation(hyphenatedWord, rules, errors);
	}

	private boolean[] extractUppercases(String word){
		int size = word.length();
		boolean[] uppercases = new boolean[size];
		for(int i = 0; i < size; i ++)
			if(Character.isUpperCase(word.charAt(i)))
				uppercases[i] = true;
		return uppercases;
	}

	private List<String> restoreUppercases(List<String> hyphenatedWord, boolean[] uppercases){
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

	private HyphenationBreak calculateBreakpoints(String word, Map<Level, RadixTree<String, String>> patterns, Level level){
		String w = WORD_BOUNDARY + word + WORD_BOUNDARY;

		int size = w.length() - 1;
		int wordSize = word.length();
		//stores the (maximum) break numbers
		int[] indexes = new int[wordSize];
		//the rules applied to the word
		String[] rules = new String[wordSize];
		//stores the augmented patterns
		String[] augmentedPatternData = new String[wordSize];
		for(int i = 0; i < size; i ++){
			//find all the prefixes of w.substring(i)
			List<String> prefixes = patterns.get(level).getValues(w.substring(i));
			for(String rule : prefixes){
				int j = -1;
				//remove non-standard part
				String reducedData = PatternService.clear(rule, MATCHER_REDUCE);
				int ruleSize = reducedData.length();
				//cycle the pattern's characters searching for numbers
				for(int k = 0; k < ruleSize; k ++){
					int idx = i + j;
					char chr = reducedData.charAt(k);
					if(!Character.isDigit(chr))
						j ++;
					//check if a break point should be skipped based on left and right min options
					else if(options.getLeftMin() <= idx && idx <= wordSize - options.getRightMin()){
						int dd = Character.digit(chr, 10);
						//check if the break number is great than the one stored so far
						if(dd > indexes[idx]){
							indexes[idx] = dd;
							rules[idx] = rule;
							augmentedPatternData[idx] = (rule.contains(AUGMENTED_RULE)? rule: null);
						}
					}
				}
			}
		}
		return new HyphenationBreak(indexes, rules, augmentedPatternData);
	}


	Hyphenation hyphenate2(String word){
		boolean[] uppercases = extractUppercases(word);

		//clear already present hyphens
		word = PatternService.replaceAll(word, MATCHER_HYPHENS, SOFT_HYPHEN);
		//clear already present word boundaries' characters
		word = PatternService.clear(word, MATCHER_WORD_BOUNDARIES);

		List<String> hyphenatedWord;
		List<String> rules;
		boolean[] errors;

		//FIXME manage second level
		Level level = Level.COMPOUND;

		String customHyphenation = customHyphenations.get(level).get(word);
		if(customHyphenation != null){
			//hyphenation is custom
			hyphenatedWord = Arrays.asList(PatternService.split(customHyphenation, PATTERN_HYPHEN_MINUS));

			rules = hyphenatedWord;
		}
		else if(word.length() <= options.getLeftMin() + options.getRightMin()){
			//ignore short words (early out)
			hyphenatedWord = Arrays.asList(word);

			rules = hyphenatedWord;
		}
		else{
			HyphenationBreak hyphBreak = calculateBreakpoints2(word, patterns, level);

			hyphenatedWord = createHyphenatedWord(word, hyphBreak);

			rules = Arrays.asList(hyphBreak.getRules());
		}
		errors = orthography.getSyllabationErrors(hyphenatedWord);

		hyphenatedWord = restoreUppercases(hyphenatedWord, uppercases);

		return new Hyphenation(hyphenatedWord, rules, errors);
	}

	private HyphenationBreak calculateBreakpoints2(String word, Map<Level, RadixTree<String, String>> patterns, Level level){
		int wordSize = word.length();
		//stores the (maximum) break numbers
		int[] indexes = new int[wordSize];
		//the rules applied to the word
		String[] rules = new String[wordSize];
		//stores the augmented patterns
		String[] augmentedPatternData = new String[wordSize];
		Iterator<SearchResult<String, String>> itr = patterns.get(level).search(WORD_BOUNDARY + word + WORD_BOUNDARY);
		while(itr.hasNext()){
			SearchResult<String, String> r = itr.next();
			String rule = r.getNode().getValue();
			int i = r.getIndex();
System.out.println(rule);

			//remove non-standard part
			String reducedData = PatternService.clear(rule, MATCHER_REDUCE);
			int ruleSize = reducedData.length();
			//cycle the pattern's characters searching for numbers
			int j = -1;
			for(int k = 0; k < ruleSize; k ++){
				char chr = reducedData.charAt(k);
				if(!Character.isDigit(chr))
					j ++;
				//check if a break point should be skipped based on left and right min options
				else{
					int idx = i + j;
					if(options.getLeftMin() <= idx && idx <= wordSize - options.getRightMin()){
						int dd = Character.digit(chr, 10);
						//check if the break number is great than the one stored so far
						if(dd > indexes[idx]){
							indexes[idx] = dd;
							rules[idx] = rule;
							augmentedPatternData[idx] = (rule.contains(AUGMENTED_RULE)? rule: null);
						}
					}
				}
			}

			List<String> rls = r.getNode().getAdditionalValues();
			if(rls != null)
				for(String rl : rls){
					j = -1;
					//cycle the pattern's characters searching for numbers
					for(int k = 0; k < ruleSize; k ++){
						char chr = reducedData.charAt(k);
						if(!Character.isDigit(chr))
							j ++;
						//check if a break point should be skipped based on left and right min options
						else{
							int idx = i + j;
							if(options.getLeftMin() <= idx && idx <= wordSize - options.getRightMin()){
								int dd = Character.digit(chr, 10);
								//check if the break number is great than the one stored so far
								if(dd > indexes[idx]){
									indexes[idx] = dd;
									rules[idx] = rl;
									augmentedPatternData[idx] = (rl.contains(AUGMENTED_RULE)? rl: null);
								}
							}
						}
					}
				}
		}
		return new HyphenationBreak(indexes, rules, augmentedPatternData);
	}

	private List<String> createHyphenatedWord(String word, HyphenationBreak hyphBreak){
		List<String> result = new ArrayList<>();

		int startIndex = 0;
		int endIndex = 0;
		int size = word.length();
		int after = 0;
		String addAfter = null;
		for(int i = 0; i < size; i ++, endIndex ++)
			if(hyphBreak.getIndexes()[i] % 2 != 0){
				String subword = word.substring(startIndex, endIndex);

				if(StringUtils.isNotBlank(addAfter)){
					//append first characters to next subword
					subword = addAfter + subword.substring(Math.min(after, subword.length()));
					addAfter = null;
				}

				//manage augmented patterns:
				String augmentedPatternData = hyphBreak.getAugmentedPatternData()[i];
				if(augmentedPatternData != null){
					Matcher m = MATCHER_AUGMENTED_RULE_HYPHEN_INDEX.reset(PatternService.clear(augmentedPatternData, MATCHER_WORD_INITIAL));
					m.find();
					int index = m.start();

					m = MATCHER_AUGMENTED_RULE.reset(augmentedPatternData);
					m.find();
					String addBefore = m.group("addBefore");
					addAfter = m.group("addAfter");
					String start = m.group("start");
					String cut = m.group("cut");
					if(start == null){
						String rule = m.group("rule");
						start = Integer.toString(1);
						cut = Integer.toString(PatternService.clear(rule, MATCHER_POINTS_AND_NUMBERS).length());
					}

					//remove last characters from subword
					//  ll3a/aa=b,2,2
					//syll
					//sylaa-b
					int end = subword.length() - index + Integer.parseInt(start) - 1;
					after = end + Integer.parseInt(cut) - endIndex;
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
