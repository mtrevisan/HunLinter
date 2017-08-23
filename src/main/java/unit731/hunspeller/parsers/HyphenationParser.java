package unit731.hunspeller.parsers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
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
import unit731.hunspeller.resources.Hyphenation;
import unit731.hunspeller.resources.HyphenationOptions;
import unit731.hunspeller.resources.HyphenationPattern;
import unit731.hunspeller.services.FileService;


public class HyphenationParser{

	/** minimal hyphenation distance from the left word end */
	private static final String MIN_LEFT_HYPHENATION = "LEFTHYPHENMIN";
	/** minimal hyphation distance from the right word end */
	private static final String MIN_RIGHT_HYPHENATION = "RIGHTHYPHENMIN";
	/** minimal hyphation distance from the left compound word boundary */
	private static final String MIN_COMPOUND_LEFT_HYPHENATION = "COMPOUNDLEFTHYPHENMIN";
	/** minimal hyphation distance from the right compound word boundary */
	private static final String MIN_COMPOUND_RIGHT_HYPHENATION = "COMPOUNDRIGHTHYPHENMIN";
	/** comma separated list of characters or character sequences with forbidden hyphenation */
	private static final String NO_HYPHEN = "NOHYPHEN";
	private static final String NEXT_LEVEL = "NEXTLEVEL";
	private static final String COMMA = ",";

	//Hyphens from the wikipedia article: https://en.wikipedia.org/wiki/Hyphen#Unicode
	private static final String HYPHEN = "\u2010";
	private static final String HYPHEN_MINUS = "\u002D";
	private static final String SOFT_HYPHEN = "\u00AD";
//	private static final String NON_BREAKING_HYPHEN = "\u2011";
//	private static final String ZERO_WIDTH_SPACE = "\u200B";
	private static final String HYPHENS = "[" + Pattern.quote(HYPHEN + SOFT_HYPHEN) + "]";

	private static final String WORD_BOUNDARY = ".";

	private static final Matcher VALID_RULE = Pattern.compile("[\\d]").matcher(StringUtils.EMPTY);
	private static final Matcher AUGMENTED_RULE = Pattern.compile("^(?<rule>.+)/(?<addBefore>.*)(=|-_)(?<addAfter>[^,]*)(,(?<indexBefore>\\d+),(?<indexAfter>\\d+))?$").matcher(StringUtils.EMPTY);
	private static final Matcher AUGMENTED_RULE_HYPHEN_INDEX = Pattern.compile("[13579]").matcher(StringUtils.EMPTY);



	private final Comparator<String> comparator;
	private final Orthography orthography;

	private Trie<String> patterns;
	private HyphenationOptions options;


	public HyphenationParser(String language){
		language = Optional.ofNullable(language)
			.orElse(StringUtils.EMPTY);

		comparator = ComparatorBuilder.getComparator(language);
		orthography = OrthographyBuilder.getOrthography(language);
	}

	public HyphenationParser(String language, Trie<String> patterns, HyphenationOptions options){
		this(language);

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

					int leftMin = 2;
					int rightMin = 2;
					int leftCompoundMin = 0;
					int rightCompoundMin = 0;
					String[] noHyphen = null;
					while((line = br.readLine()) != null){
						readSoFar += line.length();

						line = removeComment(line);
						if(line.isEmpty())
							continue;

						line = StringUtils.strip(line);
						if(!line.isEmpty()){
							if(line.startsWith(MIN_LEFT_HYPHENATION))
								leftMin = Integer.parseInt(StringUtils.strip(line.substring(MIN_LEFT_HYPHENATION.length())));
							else if(line.startsWith(MIN_RIGHT_HYPHENATION))
								rightMin = Integer.parseInt(StringUtils.strip(line.substring(MIN_RIGHT_HYPHENATION.length())));
							else if(line.startsWith(MIN_COMPOUND_LEFT_HYPHENATION))
								leftCompoundMin = Integer.parseInt(StringUtils.strip(line.substring(MIN_COMPOUND_LEFT_HYPHENATION.length())));
							else if(line.startsWith(MIN_COMPOUND_RIGHT_HYPHENATION))
								rightCompoundMin = Integer.parseInt(StringUtils.strip(line.substring(MIN_COMPOUND_RIGHT_HYPHENATION.length())));
							else if(line.startsWith(NO_HYPHEN))
								noHyphen = line.substring(NO_HYPHEN.length()).split(COMMA);
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
					
					hypParser.options = HyphenationOptions.builder()
						.leftMin(leftMin)
						.rightMin(rightMin)
						.leftCompoundMin(leftCompoundMin)
						.rightCompoundMin(rightCompoundMin)
						.noHyphen(noHyphen)
						.build();

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
			line = StringUtils.replaceAll(line, "^[^\\S\\r\\n]+|[^\\S\\r\\n]+$|^\\r?\\n$", StringUtils.EMPTY);
			return line;
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
	}

	/**
	 * @param rule	The rule to add
	 * @return A {@link TrieNode} if a rule was already in place, <code>null</code> if the insertion has completed successfully
	 */
	public TrieNode<String> addRule(String rule){
		rule = orthography.correctOrthography(rule.toLowerCase(Locale.ROOT));

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

			//having the rule ../m=m,1,2 assure that the indexes are before and after the hyphen respectively
			String[] parts = rule.split(COMMA);
			if(parts.length > 1){
				Matcher m = AUGMENTED_RULE_HYPHEN_INDEX.reset(rule);
				m.find();
				int index = m.start();

				int startIndex = (parts[1] != null? Integer.parseInt(parts[1]) - 1: -1);
				int length = (parts.length > 2 && parts[2] != null? Integer.parseInt(parts[2]) - 1: -1);
				if(startIndex < 0 || startIndex >= index)
					throw new IllegalArgumentException("Augmented rule " + rule + " has the first number not less than the hyphenation point");
				if(length < 0 || startIndex + length < index)
					throw new IllegalArgumentException("Augmented rule " + rule + " has the second number not less than the hyphenation point");
				if(startIndex + length >= parts[0].length())
					throw new IllegalArgumentException("Augmented rule " + rule + " has the second number not less than the length of the rule");
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
			writer.write(charset.name());
			writer.write(StringUtils.LF);
			//save options
			if(options.getLeftMin() > 0){
				writer.write(MIN_LEFT_HYPHENATION);
				writer.write(StringUtils.SPACE);
				writer.write(Integer.toString(options.getLeftMin()));
				writer.write(StringUtils.LF);
			}
			if(options.getRightMin() > 0){
				writer.write(MIN_RIGHT_HYPHENATION);
				writer.write(StringUtils.SPACE);
				writer.write(Integer.toString(options.getRightMin()));
				writer.write(StringUtils.LF);
			}
			if(options.getLeftCompoundMin() > 0){
				writer.write(MIN_COMPOUND_LEFT_HYPHENATION);
				writer.write(StringUtils.SPACE);
				writer.write(Integer.toString(options.getLeftCompoundMin()));
				writer.write(StringUtils.LF);
			}
			if(options.getRightCompoundMin() > 0){
				writer.write(MIN_COMPOUND_RIGHT_HYPHENATION);
				writer.write(StringUtils.SPACE);
				writer.write(Integer.toString(options.getRightCompoundMin()));
				writer.write(StringUtils.LF);
			}
			if(options.getNoHyphen() != null){
				writer.write(NO_HYPHEN);
				writer.write(StringUtils.SPACE);
				writer.write(StringUtils.join(options.getNoHyphen(), COMMA));
				writer.write(StringUtils.LF);
			}
			//extract data from the trie
			Map<Integer, List<String>> content = new HashMap<>();
			patterns.forEachLeaf(node -> {
				String data = node.getData();
				content.computeIfAbsent(data.length(), ArrayList::new)
					.add(data);
			});
			//sort values
			content.values()
				.forEach(v -> Collections.sort(v, comparator::compare));
			List<String> rules = content.values().stream()
				.flatMap(List::stream)
				.collect(Collectors.toList());
			for(String rule : rules){
				writer.write(rule);
				writer.write(StringUtils.LF);
			}
		}
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
		addedRule = orthography.correctOrthography(addedRule.toLowerCase(Locale.ROOT));
		String key = getKeyFromData(addedRule);
		patternsWithAddedRule.add(key, addedRule);

		return hyphenate(word, patternsWithAddedRule);
	}

	public boolean hasRule(String rule){
		rule = orthography.correctOrthography(rule.toLowerCase(Locale.ROOT));
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
		word = orthography.correctOrthography(word.toLowerCase(Locale.ROOT))
			.replaceAll(HYPHENS, SOFT_HYPHEN);

		List<String> hyphenatedWord;
		boolean[] errors;
		if(word.length() < options.getLeftMin() + options.getRightMin())
			//ignore short words (early out)
			hyphenatedWord = Arrays.asList(word);
		else{
			HyphenationPattern hyphPattern = getHyphenationIndexes(word, patterns);
			hyphenatedWord = createHyphenatedWord(word, hyphPattern);
		}
		errors = orthography.getSyllabationErrors(hyphenatedWord);

		return new Hyphenation(hyphenatedWord, errors);
	}

	private HyphenationPattern getHyphenationIndexes(String word, Trie<String> patterns){
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
		return new HyphenationPattern(indexes, augmentedPatternData);
	}

	private List<String> createHyphenatedWord(String word, HyphenationPattern hyphPattern){
		List<String> result = new ArrayList<>();
		int startIndex = 0;
		int endIndex = 0;
//		int maxLength = word.length() - options.getRightMin();
		int size = word.length();
		int after = 0;
		String addAfter = null;
		for(int i = 0; i < size; i ++){
			//manage hyphenation characters already present in the word
//			int idx = word.substring(endIndex).indexOf(HYPHEN_MINUS);
//			idx = (idx >= 0? idx + endIndex - options.getRightMin() - 1: maxLength);
			if(/*i >= leftMin && i <= idx &&*/ hyphPattern.getIndexes()[i] % 2 != 0){
				String subword = word.substring(startIndex, endIndex)
					/*.replaceFirst(HYPHEN_MINUS, StringUtils.EMPTY)*/;

				if(StringUtils.isNotBlank(addAfter)){
					subword = addAfter + subword.substring(Math.min(after, subword.length()));
					addAfter = null;
				}

				String augmentedPatternData = hyphPattern.getAugmentedPatternData()[i];
				if(augmentedPatternData != null){
					Matcher m = AUGMENTED_RULE_HYPHEN_INDEX.reset(augmentedPatternData.replaceFirst("^\\.", StringUtils.EMPTY));
					m.find();
					int index = m.start();

					m = AUGMENTED_RULE.reset(augmentedPatternData);
					m.find();
					String rule = m.group("rule");
					String addBefore = m.group("addBefore");
					addAfter = m.group("addAfter");
					String indexBefore = m.group("indexBefore");
					String indexAfter = m.group("indexAfter");
					if(indexBefore == null){
						indexBefore = "1";
						indexAfter = Integer.toString(rule.replaceAll("[.\\d]", StringUtils.EMPTY).length());
					}

					//remove last characters from subword
					int end = i - index + Integer.parseInt(indexBefore) - 1 /*- (StringUtils.isEmpty(addBefore)? 0: 0)*/;
					subword = subword.substring(0, end) + addBefore;

					//append first characters to next subword
					after = end + Integer.parseInt(indexAfter) - endIndex /*- (StringUtils.isEmpty(addBefore)? 1: 0)*/;
				}

				result.add(subword);
				startIndex = endIndex;
			}
			endIndex ++;
		}
		String subword = word.substring(startIndex);
		if(StringUtils.isNotBlank(addAfter))
			subword = addAfter + subword.substring(Math.min(after, subword.length()));
		result.add(subword);
		return result;
	}

	public String formatHyphenation(Hyphenation hyphenation){
		StringJoiner sj = new StringJoiner(HYPHEN, "<html>", "</html>");
		List<String> syllabes = hyphenation.getSyllabes();
		boolean[] erros = hyphenation.getErrors();
		int size = syllabes.size();
		for(int i = 0; i < size; i ++)
			sj.add(erros[i]? "<b style=\"color:red\">" + syllabes.get(i) + "</b>": syllabes.get(i));
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
