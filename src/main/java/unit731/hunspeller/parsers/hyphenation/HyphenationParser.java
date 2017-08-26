package unit731.hunspeller.parsers.hyphenation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.SwingWorker;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.collections.trie.Prefix;
import unit731.hunspeller.collections.trie.Trie;
import unit731.hunspeller.collections.trie.TrieNode;
import unit731.hunspeller.interfaces.Resultable;
import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.languages.builders.ComparatorBuilder;
import unit731.hunspeller.languages.builders.OrthographyBuilder;
import unit731.hunspeller.services.FileService;


/**
 * Implements Franklin Mark Liang's hyphenation algorithm with Petr Soijka's non-standard hyphenation extension.
 * 
 * @see <a href="https://tug.org/docs/liang/liang-thesis.pdf">Liang's thesis</a>
 * @see <a href="http://hunspell.sourceforge.net/tb87nemeth.pdf">László Németh's paper</a>
 * @see <a href="https://github.com/hunspell/hyphen">C source code</a>
 * @see <a href="https://wiki.openoffice.org/wiki/Documentation/SL/Using_TeX_hyphenation_patterns_in_OpenOffice.org">Using TeX hyphenation patterns in OpenOffice.org</a>
 */
public class HyphenationParser{

	private static final String NEXT_LEVEL = "NEXTLEVEL";

	//Hyphens from the wikipedia article: https://en.wikipedia.org/wiki/Hyphen#Unicode
	public static final String HYPHEN = "\u2010";
	public static final String HYPHEN_MINUS = "\u002D";
	private static final String SOFT_HYPHEN = "\u00AD";
	private static final String HYPHENS = "[" + Pattern.quote(HYPHEN) + "]";

	private static final String WORD_BOUNDARY = ".";

	private static final String COMMA = ",";

	private static final Matcher VALID_RULE = Pattern.compile("[\\d]").matcher(StringUtils.EMPTY);
	private static final Matcher AUGMENTED_RULE = Pattern.compile("^(?<rule>.+)/(?<addBefore>.*)(=|(?<hyphen>.)_)(?<addAfter>[^,]*)(,(?<indexBefore>\\d+),(?<indexAfter>\\d+))?$").matcher(StringUtils.EMPTY);
	private static final Matcher AUGMENTED_RULE_HYPHEN_INDEX = Pattern.compile("[13579]").matcher(StringUtils.EMPTY);



	private final Comparator<String> comparator;
	private final Orthography orthography;

	private Trie<String> patterns = new Trie<>();
	private HyphenationOptions options;
	private final Map<String, String> nonStandardHyphenation = new HashMap<>();


	public HyphenationParser(String language){
		Objects.nonNull(language);

		language = Optional.ofNullable(language)
			.orElse(StringUtils.EMPTY);

		comparator = ComparatorBuilder.getComparator(language);
		orthography = OrthographyBuilder.getOrthography(language);

		Objects.nonNull(comparator);
		Objects.nonNull(orthography);
	}

	public HyphenationParser(String language, Trie<String> patterns, HyphenationOptions options){
		this(language);

		Objects.nonNull(patterns);
		Objects.nonNull(options);

		this.patterns = patterns;
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
			try{
				publish("Opening Hyphenation file: " + hypFile.getName());
				setProgress(0);

				long readSoFar = 0l;
				long totalSize = hypFile.length();

				Charset charset = FileService.determineCharset(hypFile.toPath());
				try(BufferedReader br = Files.newBufferedReader(hypFile.toPath(), charset)){
					String line = br.readLine();
					if(Charset.forName(line) != charset)
						throw new IllegalArgumentException("Hyphenation data file malformed, the first line is not '" + charset.name() + "'");

					int level = 0;
					hypParser.options = new HyphenationOptions();
					while((line = br.readLine()) != null){
						readSoFar += line.length();

						line = removeComment(line);
						if(line.isEmpty())
							continue;

						if(!line.isEmpty()){
							if(hypParser.options.parseLine(line)){}
							else if(line.startsWith(NEXT_LEVEL)){
								level ++;

								if(level > 1)
									throw new IllegalArgumentException("Cannot have more than two levels");
							}
							else if(!VALID_RULE.reset(line).find() && line.contains(HYPHEN_MINUS)){
								String key = line.replaceAll(HYPHEN_MINUS, StringUtils.EMPTY);
								if(hypParser.nonStandardHyphenation.containsKey(key))
									throw new IllegalArgumentException("Non-standard hyphenation " + line + " is already present");

								hypParser.nonStandardHyphenation.put(key, line);
							}
							else{
								validateRule(line);

								String key = getKeyFromData(line);
								TrieNode<String> foundRule = hypParser.patterns.contains(key);
								if(foundRule != null && foundRule.getData().equals(line))
									publish("Duplication found: " + foundRule.getData() + " <-> " + line);
								else
									//insert current pattern into the trie (remove all numbers)
									hypParser.patterns.add(key, line);
							}
						}

						setProgress((int)((readSoFar * 100.) / totalSize));
					}

					if(level == 1){
						//default first level (after the NEXTLEVEL tag): hyphen and ASCII apostrophe
//						if(hypParser.options[1].getNoHyphen() == null)
//							//en dash and right single quotation mark
//							hypParser.options[1].setNoHyphen(new String[]{"\\u2013", "\\u2019"});
//						line = "1-1/=,1,1";
//						hypParser.patterns[1].add(getKeyFromData(line), line);
//						line = "1'1";
//						hypParser.patterns[1].add(getKeyFromData(line), line);
//						hypParser.options[1].setLeftMin(hypParser.options[0].getLeftMin());
//						hypParser.options[1].setRightMin(hypParser.options[0].getRightMin());
//						hypParser.options[1].setLeftCompoundMin(hypParser.options[0].getLeftCompoundMin() > 0? hypParser.options[0].getLeftCompoundMin(): (hypParser.options[0].getLeftMin() > 0? hypParser.options[0].getLeftMin(): 3));
//						hypParser.options[1].setRightCompoundMin(hypParser.options[0].getRightCompoundMin() > 0? hypParser.options[0].getRightCompoundMin(): (hypParser.options[0].getRightMin() > 0? hypParser.options[0].getRightMin(): 3));
					}

					setProgress(100);
				}

				publish("Finished reading Hyphenation file");
			}
			catch(IOException | IllegalArgumentException e){
				publish(e.getClass().getSimpleName() + ": " + e.getMessage());
			}
			return null;
		}

		/**
		 * Removes comment lines and then cleans up blank lines and trailing whitespace.
		 *
		 * @param {String} data	The data from an affix file.
		 * @return {String}		The cleaned-up data.
		 */
		private String removeComment(String line){
			//remove comments
			line = StringUtils.replaceAll(line, "^$|\\s*%.*$", StringUtils.EMPTY);
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
		patterns.clear();
		if(options != null)
			options.clear();
		nonStandardHyphenation.clear();
	}

	public String correctOrthography(String text){
		return orthography.correctOrthography(text);
	}

	/**
	 * @param rule	The rule to add
	 * @return A {@link TrieNode} if a rule was already in place, <code>null</code> if the insertion has completed successfully
	 */
	public TrieNode<String> addRule(String rule){
		rule = correctOrthography(rule);

		validateRule(rule);

		String key = getKeyFromData(rule);
		TrieNode<String> foundRule = patterns.contains(key);
		if(foundRule == null || !foundRule.getData().equals(rule))
			patterns.add(key, rule);
		return foundRule;
	}

	/**
	 * Line must contains exactly one hyphenation point
	 * 
	 * @param rule	Rule to be validated
	 */
	public static void validateRule(String rule){
		if(!VALID_RULE.reset(rule).find())
			throw new IllegalArgumentException("Rule " + rule + " has no hyphenation point(s)");
		if(isAugmentedRule(rule)){
			int augmentedIndex = rule.indexOf('/');
			if((augmentedIndex > 0? rule.substring(0, augmentedIndex): rule).replaceAll("[^13579]|/.+$", StringUtils.EMPTY).length() != 1)
				throw new IllegalArgumentException("Augmented rule " + rule + " has not exactly one hyphenation point");

			String[] parts = rule.split(COMMA);
			if(parts.length > 1){
				Matcher m = AUGMENTED_RULE_HYPHEN_INDEX.reset(rule);
				m.find();
				int index = m.start();

				int startIndex = (parts[1] != null? Integer.parseInt(parts[1]) - 1: -1);
				int length = (parts.length > 2 && parts[2] != null? Integer.parseInt(parts[2]): 0);
				if(startIndex <= 0 || startIndex >= index)
					throw new IllegalArgumentException("Augmented rule " + rule + " has the index number not less than the hyphenation point");
				if(length < 0 || startIndex + length < index)
					throw new IllegalArgumentException("Augmented rule " + rule + " has the length number not less than the hyphenation point");
				if(startIndex + length >= parts[0].length())
					throw new IllegalArgumentException("Augmented rule " + rule + " has the length number that exceeds the length of the rule");
			}
		}
	}

	private static boolean isAugmentedRule(String rule){
		return AUGMENTED_RULE.reset(rule).find();
	}

	public void save(File hypFile) throws IOException{
		Charset charset = StandardCharsets.UTF_8;
		try(BufferedWriter writer = Files.newBufferedWriter(hypFile.toPath(), charset)){
			//save charset
			writeln(writer, charset.name());
			//save options
			options.write(writer);
			//extract data from the trie
			Map<Integer, List<String>> content = new HashMap<>();
			patterns.forEachLeaf(node -> {
				String data = node.getData();
				content.computeIfAbsent(data.length(), ArrayList::new)
					.add(data);
			});
			if(!nonStandardHyphenation.isEmpty()){
				//non-standard hyphenations
				Collection<String> nonStandards = nonStandardHyphenation.values();
				for(String hyphenation : nonStandards)
					writeln(writer, hyphenation);
				writeln(writer, NEXT_LEVEL);
			}
			//sort values
			content.values()
				.forEach(v -> Collections.sort(v, comparator::compare));
			List<String> rules = content.values().stream()
				.flatMap(List::stream)
				.collect(Collectors.toList());
			for(String rule : rules)
				writeln(writer, rule);
		}
	}

	private void writeln(BufferedWriter writer, String line) throws IOException{
		writer.write(line);
		writer.write(StringUtils.LF);
	}

	/**
	 * Performs hyphenation
	 *
	 * @param word	String to hyphenate
	 * @return the hyphenation object
	 */
	public Hyphenation hyphenate(String word){
		return hyphenate(word, patterns);
	}

	/**
	 * Performs hyphenation including an additional rule
	 *
	 * @param word	String to hyphenate
	 * @param addedRule	Rule to add to the set of rules that will generate the hyphenation
	 * @return the hyphenation object
	 */
	public Hyphenation hyphenate(String word, String addedRule){
		Trie<String> patternsWithAddedRule = new Trie<>(patterns);
		addedRule = correctOrthography(addedRule);
		String key = getKeyFromData(addedRule);
		patternsWithAddedRule.add(key, addedRule);

		return hyphenate(word, patternsWithAddedRule);
	}

	public boolean hasRule(String rule){
		rule = correctOrthography(rule);
		String key = getKeyFromData(rule);
		TrieNode<String> foundRule = patterns.contains(key);
		return (foundRule != null && foundRule.getData().equals(rule));
	}

	private static String getKeyFromData(String rule){
		return rule.replaceAll("\\d|/.+$", StringUtils.EMPTY);
	}

	/**
	 * Performs hyphenation
	 *
	 * @param word	String to hyphenate
	 * @param patterns	The trie containing the subdivision rules
	 * @return the hyphenation object
	 */
	private Hyphenation hyphenate(String word, Trie<String> patterns){
		word = correctOrthography(word)
			.replaceAll(HYPHENS, SOFT_HYPHEN);

		List<String> hyphenatedWord;
		boolean[] errors;

		String nonStandard = nonStandardHyphenation.get(word);
		if(nonStandard != null)
			//hyphenation is non-standard
			hyphenatedWord = Arrays.asList(nonStandard.split(HYPHEN_MINUS));
		else if(word.length() < options.getLeftMin() + options.getRightMin())
			//ignore short words (early out)
			hyphenatedWord = Arrays.asList(word);
		else{
			HyphenationBreak hyphBreak = getHyphenationIndexes(word, patterns);
			hyphenatedWord = createHyphenatedWord(word, hyphBreak);
		}
		errors = orthography.getSyllabationErrors(hyphenatedWord);

		return new Hyphenation(hyphenatedWord, errors);
	}

	private HyphenationBreak getHyphenationIndexes(String word, Trie<String> patterns){
		String w = WORD_BOUNDARY + word + WORD_BOUNDARY;

		int size = w.length() - 1;
		int wordSize = word.length();
		int[] indexes = new int[wordSize];
		String[] augmentedPatternData = new String[wordSize];
		for(int i = 0; i < size; i ++){
			List<Prefix<String>> prefixes = patterns.findPrefix(w.substring(i));
			for(Prefix<String> prefix : prefixes){
				int j = -1;
				String data = prefix.getNode().getData();
				String reducedData = data.replaceFirst("/.+$", StringUtils.EMPTY);
				int ruleSize = reducedData.length();
				for(int k = 0; k < ruleSize; k ++){
					char chr = reducedData.charAt(k);
					if(!Character.isDigit(chr))
						j ++;
					else{
						int idx = i + j;
						if(options.getLeftMin() <= idx && idx <= wordSize - options.getRightMin()){
							int dd = Character.digit(chr, 10);
							if(dd > indexes[idx]){
								indexes[idx] = dd;
								augmentedPatternData[idx] = (isAugmentedRule(data)? data: null);
							}
						}
					}
				}
			}
		}
		return new HyphenationBreak(indexes, augmentedPatternData);
	}

	private List<String> createHyphenatedWord(String word, HyphenationBreak hyphBreak){
		List<String> result = new ArrayList<>();
		int startIndex = 0;
		int endIndex = 0;
		int size = word.length();
		int after = 0;
		String addAfter = null;
		for(int i = 0; i < size; i ++){
			if(hyphBreak.getIndexes()[i] % 2 != 0){
				String subword = word.substring(startIndex, endIndex);

				if(StringUtils.isNotBlank(addAfter)){
					//append first characters to next subword
					subword = addAfter + subword.substring(Math.min(after, subword.length()));
					addAfter = null;
				}

				String augmentedPatternData = hyphBreak.getAugmentedPatternData()[i];
				if(augmentedPatternData != null){
					Matcher m = AUGMENTED_RULE_HYPHEN_INDEX.reset(augmentedPatternData.replaceFirst("^\\.", StringUtils.EMPTY));
					m.find();
					int index = m.start();

					m = AUGMENTED_RULE.reset(augmentedPatternData);
					m.find();
					String addBefore = m.group("addBefore");
					addAfter = m.group("addAfter");
					String indexBefore = m.group("indexBefore");
					String indexAfter = m.group("indexAfter");
					if(indexBefore == null){
						String rule = m.group("rule");
						indexBefore = Integer.toString(1);
						indexAfter = Integer.toString(rule.replaceAll("[.\\d]", StringUtils.EMPTY).length());
					}

					//remove last characters from subword
					//  ll3a/aa=b,2,2
					//syll
					//sylaa-b
					int end = subword.length() - index + Integer.parseInt(indexBefore) - 1;
					after = end + Integer.parseInt(indexAfter) - endIndex;
					subword = subword.substring(0, end) + addBefore;
				}

				result.add(subword);
				startIndex = endIndex;
			}
			endIndex ++;
		}
		String subword = word.substring(startIndex);
		if(StringUtils.isNotBlank(addAfter))
			subword = addAfter + subword.substring(Math.min(Math.max(after, 0), subword.length()));
		result.add(subword);

		return result;
	}

	public String formatHyphenation(Hyphenation hyphenation, StringJoiner sj, Function<String, String> errorFormatter){
		List<String> syllabes = hyphenation.getSyllabes();
		boolean[] erros = hyphenation.getErrors();
		int size = syllabes.size();
		for(int i = 0; i < size; i ++){
			Function<String, String> fun = (erros[i]? errorFormatter: Function.identity());
			sj.add(fun.apply(syllabes.get(i)));
		}
		return sj.toString();
	}

	public long countSyllabes(Hyphenation hyphenation){
		return hyphenation.getSyllabes().size();
	}

	public boolean hasErrors(Hyphenation hyphenation){
		boolean response = false;
		boolean[] errors = hyphenation.getErrors();
		for(boolean error : errors)
			if(error){
				response = true;
				break;
			}
		return response;
	}

}
