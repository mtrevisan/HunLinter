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
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.swing.SwingWorker;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.collections.trie.Prefix;
import unit731.hunspeller.collections.trie.Trie;
import unit731.hunspeller.collections.trie.TrieNode;
import unit731.hunspeller.interfaces.Resultable;
import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.languages.builders.ComparatorBuilder;
import unit731.hunspeller.languages.builders.OrthographyBuilder;
import unit731.hunspeller.resources.Hyphenation;
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

	//Hyphens from the wikipedia article: https://en.wikipedia.org/wiki/Hyphen#Unicode
	public static final String HYPHEN = "\u2010";
	private static final String HYPHEN_MINUS = "\u002D";
	private static final String SOFT_HYPHEN = "\u00AD";
//	private static final String NON_BREAKING_HYPHEN = "\u2011";
//	private static final String ZERO_WIDTH_SPACE = "\u200B";
	public static final String WORD_HYPHEN = HYPHEN_MINUS;

	private static final String WORD_BOUNDARY = ".";

	private static final String NEW_LINE = "\n";


	private final Comparator<String> comparator;
	private final Orthography orthography;

	private final Trie<String> trie = new Trie<>();
	@Getter private int leftMin = -1;
	@Getter private int rightMin = -1;
	@Getter private int leftCompoundMin = -1;
	@Getter private int rightCompoundMin = -1;



	public HyphenationParser(AffixParser affParser){
		Objects.nonNull(affParser);

		String language = Optional.ofNullable(affParser.getLanguage())
			.orElse(StringUtils.EMPTY);
		comparator = ComparatorBuilder.getComparator(language);
		orthography = OrthographyBuilder.getOrthography(language);
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

					while((line = br.readLine()) != null){
						readSoFar += line.length();

						line = StringUtils.strip(line);
						if(!line.isEmpty()){
							if(line.startsWith(MIN_LEFT_HYPHENATION))
								hypParser.leftMin = Integer.parseInt(StringUtils.strip(line.replace(MIN_LEFT_HYPHENATION, "")));
							else if(line.startsWith(MIN_RIGHT_HYPHENATION))
								hypParser.rightMin = Integer.parseInt(StringUtils.strip(line.replace(MIN_RIGHT_HYPHENATION, "")));
							else if(line.startsWith(MIN_COMPOUND_LEFT_HYPHENATION))
								hypParser.leftCompoundMin = Integer.parseInt(StringUtils.strip(line.replace(MIN_COMPOUND_LEFT_HYPHENATION, "")));
							else if(line.startsWith(MIN_COMPOUND_RIGHT_HYPHENATION))
								hypParser.rightCompoundMin = Integer.parseInt(StringUtils.strip(line.replace(MIN_COMPOUND_RIGHT_HYPHENATION, "")));
							else{
								String key = line.replaceAll("\\d", StringUtils.EMPTY);
								TrieNode<String> foundRule = hypParser.trie.contains(key);
								if(foundRule != null)
									publish("Duplication found: " + foundRule.getData() + " <-> " + line);
								else
									//insert current pattern into the trie (remove all numbers)
									hypParser.trie.add(key, line);
							}
						}

						setProgress((int)((readSoFar * 100.) / totalSize));
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
		trie.clear();
		leftMin = -1;
		rightMin = -1;
		leftCompoundMin = -1;
		rightCompoundMin = -1;
	}

	/**
	 * @param rule	The rule to add
	 * @return A {@link TrieNode} if a rule was already in place, <code>null</code> if the insertion has completed successfully
	 */
	public TrieNode<String> addRule(String rule){
		rule = orthography.correctOrthography(rule.toLowerCase(Locale.ROOT));
		String key = rule.replaceAll("\\d", StringUtils.EMPTY);
		TrieNode<String> foundRule = trie.contains(key);
		if(foundRule == null || !foundRule.getData().equals(rule))
			trie.add(key, rule);
		return foundRule;
	}

	public void save(File hypFile) throws IOException{
		Charset charset = StandardCharsets.UTF_8;
		try(BufferedWriter writer = Files.newBufferedWriter(hypFile.toPath(), charset)){
			//save charset
			writer.write(charset.name());
			writer.write(NEW_LINE);
			//save options
			if(leftMin >= 0){
				writer.write(MIN_LEFT_HYPHENATION);
				writer.write(StringUtils.SPACE);
				writer.write(Integer.toString(leftMin));
				writer.write(NEW_LINE);
			}
			if(rightMin >= 0){
				writer.write(MIN_RIGHT_HYPHENATION);
				writer.write(StringUtils.SPACE);
				writer.write(Integer.toString(rightMin));
				writer.write(NEW_LINE);
			}
			if(leftCompoundMin >= 0){
				writer.write(MIN_COMPOUND_LEFT_HYPHENATION);
				writer.write(StringUtils.SPACE);
				writer.write(Integer.toString(leftCompoundMin));
				writer.write(NEW_LINE);
			}
			if(leftCompoundMin >= 0){
				writer.write(MIN_COMPOUND_RIGHT_HYPHENATION);
				writer.write(StringUtils.SPACE);
				writer.write(Integer.toString(leftCompoundMin));
				writer.write(NEW_LINE);
			}
			//extract data from the trie
			Map<Integer, List<String>> content = new HashMap<>();
			trie.forEachLeaf(node -> {
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
				writer.write(NEW_LINE);
			}
		}
	}

	/**
	 * Performs hyphenation
	 *
	 * @param word	String to hyphenate
	 * @return the list of hyphenation objects
	 */
	public List<Hyphenation> hyphenate(String word){
		return hyphenate(word, trie);
	}

	/**
	 * Performs hyphenation including an additional rule
	 *
	 * @param word	String to hyphenate
	 * @param addedRule	Rule to add to the set of rules that will generate the hyphenation
	 * @return the list of hyphenation objects
	 */
	public List<Hyphenation> hyphenate(String word, String addedRule){
		Trie<String> augmentedTrie = new Trie<>(trie);
		addedRule = orthography.correctOrthography(addedRule.toLowerCase(Locale.ROOT));
		String key = addedRule.replaceAll("\\d", StringUtils.EMPTY);
		augmentedTrie.add(key, addedRule);

		return hyphenate(word, augmentedTrie);
	}

	public boolean hasRule(String rule){
		rule = orthography.correctOrthography(rule.toLowerCase(Locale.ROOT));
		String key = rule.replaceAll("\\d", StringUtils.EMPTY);
		TrieNode<String> foundRule = trie.contains(key);
		return (foundRule != null && foundRule.getData().equals(rule));
	}

	/**
	 * Performs hyphenation
	 *
	 * @param word	String to hyphenate
	 * @param trie	The trie containing the subdivision rules
	 * @return the list of hyphenation objects
	 */
	private List<Hyphenation> hyphenate(String word, Trie<String> trie){
		word = orthography.correctOrthography(word.toLowerCase(Locale.ROOT));

		List<Hyphenation> result = new ArrayList<>();
		//ignore short words (early out)
		if(word.length() < leftMin + rightMin){
			List<String> hyphenatedWord = Arrays.asList(word);
			boolean[] errors = orthography.getSyllabationErrors(hyphenatedWord);
			result.add(new Hyphenation(hyphenatedWord, errors));
		}
		else if(word.contains(HYPHEN) || word.contains(HYPHEN_MINUS) || word.contains(SOFT_HYPHEN)){
			//if word contains an hyphen then hyphenate each single part
			String[] subwords = word.split(HYPHEN + "|" + HYPHEN_MINUS + "|" + SOFT_HYPHEN);
			for(String subword : subwords)
				result.addAll(hyphenate(subword));
		}
		else{
			int[] pattern = getHyphenationPattern(word, trie);
			List<String> hyphenatedWord = createHyphenatedWord(word, pattern);
			boolean[] errors = orthography.getSyllabationErrors(hyphenatedWord);
			result.add(new Hyphenation(hyphenatedWord, errors));
		}

		return result;
	}

	private int[] getHyphenationPattern(String word, Trie<String> trie){
		String w = WORD_BOUNDARY + word + WORD_BOUNDARY;

		int size = w.length() - 1;
		int[] hyp = new int[word.length()];
		for(int i = 0; i < size; i ++){
			List<Prefix<String>> prefixes = trie.findPrefix(w.substring(i));
			for(Prefix<String> prefix : prefixes){
				int j = -1;
				String data = prefix.getNode().getData();
				int size2 = data.length();
				for(int k = 0; k < size2; k ++){
					char chr = data.charAt(k);
					if(!Character.isDigit(chr))
						j ++;
					else{
						int dd = Character.digit(data.charAt(k), 10);
						if(hyp[i + j] == 0 || dd > hyp[i + j])
							hyp[i + j] = dd;
					}
				}
			}
		}
		return hyp;
	}

	private List<String> createHyphenatedWord(String word, int[] pattern){
		List<String> result = new ArrayList<>();
		int startIndex = 0;
		int endIndex = 0;
		int maxLength = word.length() - rightMin;
		int size = word.length();
		for(int i = 0; i < size; i ++){
			if(i >= leftMin && i <= maxLength && pattern[i] % 2 != 0){
				result.add(word.substring(startIndex, endIndex));
				startIndex = endIndex;
			}
			endIndex ++;
		}
		result.add(word.substring(startIndex));
		return result;
	}

	public String formatHyphenation(List<Hyphenation> hyphenations){
		StringJoiner sj = new StringJoiner(HyphenationParser.WORD_HYPHEN);
		for(Hyphenation hyphenation : hyphenations){
			StringJoiner subword = new StringJoiner(HyphenationParser.HYPHEN, "<html>", "</html>");
			List<String> syllabes = hyphenation.getSyllabes();
			boolean[] erros = hyphenation.getErrors();
			int size = syllabes.size();
			for(int i = 0; i < size; i ++)
				subword.add(erros[i]? "<b style=\"color:red\">" + syllabes.get(i) + "</b>": syllabes.get(i));
			sj.add(subword.toString());
		}
		return sj.toString();
	}

	public long countSyllabes(List<Hyphenation> hyphenations){
		return hyphenations.stream()
			.map(Hyphenation::getSyllabes)
			.flatMap(List::stream)
			.count();
	}

	public boolean hasErrors(List<Hyphenation> hyphenations){
		return !hyphenations.stream()
			.map(Hyphenation::getErrors)
			.anyMatch(errors -> IntStream.range(0, errors.length).anyMatch(i -> errors[i]));
	}

}
