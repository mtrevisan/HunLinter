package unit731.hunspeller.parsers.dictionary.valueobjects;

import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.commons.text.similarity.LevenshteinDistance;
import unit731.hunspeller.collections.bloomfilter.BloomFilterInterface;
import unit731.hunspeller.collections.bloomfilter.ScalableInMemoryBloomFilter;
import unit731.hunspeller.collections.bloomfilter.core.BitArrayBuilder;
import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.languages.builders.OrthographyBuilder;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.hyphenation.dtos.Hyphenation;


/**
 * @see <a href="https://home.ubalt.edu/ntsbarsh/Business-stat/otherapplets/PoissonTest.htm">Goodness-of-Fit for Poisson</a>
 */
@Getter
public class DictionaryStatistics{

	public static final DecimalFormat PERCENT_FORMATTER = (DecimalFormat)NumberFormat.getInstance(Locale.US);
	static{
		DecimalFormatSymbols symbols = PERCENT_FORMATTER.getDecimalFormatSymbols();
		PERCENT_FORMATTER.setMultiplier(100);
		PERCENT_FORMATTER.setPositiveSuffix("%");
		symbols.setGroupingSeparator(' ');
		PERCENT_FORMATTER.setDecimalFormatSymbols(symbols);
		PERCENT_FORMATTER.setMinimumFractionDigits(1);
		PERCENT_FORMATTER.setMaximumFractionDigits(1);
	}

	private static final LevenshteinDistance LEVENSHTEIN_DISTANCE = LevenshteinDistance.getDefaultInstance();

	private static final int REPRESENTATIVE_MIN_DISTANCE = 3;


	private int totalProductions;
	private int longestWordCountByCharacters;
	private int longestWordCountBySyllabes;
	private int compoundWords;
	private int contractedWords;
	private final Frequency<Integer> lengthsFrequencies = new Frequency<>();
	private final Frequency<String> syllabesFrequencies = new Frequency<>();
	private final Frequency<Integer> syllabeLengthsFrequencies = new Frequency<>();
	private final Frequency<Integer> stressFromLastFrequencies = new Frequency<>();
	private final List<String> longestWordsByCharacters = new ArrayList<>();
	private final List<Hyphenation> longestWordsBySyllabes = new ArrayList<>();
	private final BloomFilterInterface<String> bloomFilter = new ScalableInMemoryBloomFilter<>(BitArrayBuilder.Type.FAST, 40_000_000, 0.000_000_01, 1.3);

	@Getter
	private final Orthography orthography;


	public DictionaryStatistics(String language, Charset charset){
		bloomFilter.setCharset(charset);
		orthography = OrthographyBuilder.getOrthography(language);
	}

	public void addData(String word, Hyphenation hyphenation){
		if(!hyphenation.hasErrors()){
			List<String> syllabes = hyphenation.getSyllabes();

			List<Integer> stressIndexes = orthography.getStressIndexFromLast(syllabes);
			if(stressIndexes != null)
				stressFromLastFrequencies.addValue(stressIndexes.get(stressIndexes.size() - 1));
			syllabeLengthsFrequencies.addValue(syllabes.size());
			StringBuilder sb = new StringBuilder();
			for(String syllabe : syllabes){
				sb.append(syllabe);
				if(orthography.countGraphemes(syllabe) == syllabe.length())
					syllabesFrequencies.addValue(syllabe);
			}
			String subword = sb.toString();
			lengthsFrequencies.addValue(subword.length());
			storeLongestWord(subword, hyphenation);
			if(subword.length() < word.length())
				compoundWords ++;
			if(subword.contains(HyphenationParser.APOSTROPHE))
				contractedWords ++;
			totalProductions ++;
		}
	}

	private void storeLongestWord(String word, Hyphenation hyphenation){
		int letterCount = orthography.countGraphemes(word);
		if(letterCount > longestWordCountByCharacters){
			longestWordsByCharacters.clear();
			longestWordsByCharacters.add(word);
			longestWordCountByCharacters = letterCount;
		}
		else if(letterCount == longestWordCountByCharacters)
			longestWordsByCharacters.add(word);

		List<String> syllabes = hyphenation.getSyllabes();
		int syllabeCount = syllabes.size();
		if(syllabeCount > longestWordCountBySyllabes){
			longestWordsBySyllabes.clear();
			longestWordsBySyllabes.add(hyphenation);
			longestWordCountBySyllabes = syllabeCount;
		}
		else if(syllabeCount == longestWordCountBySyllabes)
			longestWordsBySyllabes.add(hyphenation);

		bloomFilter.add(word);
	}

	public long getTotalProductions(){
		return totalProductions;
	}

	public List<String> getMostCommonSyllabes(int size){
		return syllabesFrequencies.getMostCommonValues(5).stream()
			.map(value -> value + " (" + PERCENT_FORMATTER.format(syllabesFrequencies.getPercentOf(value)) + ")")
			.collect(Collectors.toList());
//		return syllabesFrequencies.getMode().stream()
//			.limit(size)
//			.map(String.class::cast)
//			.collect(Collectors.toList());
	}

	/** Returns the countr of unique words */
	public int getUniqueWords(){
		return bloomFilter.getAddedElements();
	}

	/** Returns the count of compound words */
	public int getCompoundWords(){
		return compoundWords;
	}

	public void clear(){
		totalProductions = 0;
		longestWordCountByCharacters = 0;
		longestWordCountBySyllabes = 0;
		lengthsFrequencies.clear();
		syllabeLengthsFrequencies.clear();
		syllabesFrequencies.clear();
		stressFromLastFrequencies.clear();
		longestWordsByCharacters.clear();
		longestWordsBySyllabes.clear();
		bloomFilter.clear();
	}


	public static List<String> extractRepresentatives(List<String> population, int limitPopulation){
		int index = 0;
		List<String> result = new ArrayList<>(population);
		limitPopulation = Math.min(limitPopulation, population.size());
		while(index < limitPopulation){
			String elem = result.get(index);

			int i = 0;
			Iterator<String> itrRemoval = result.iterator();
			while(itrRemoval.hasNext()){
				String removal = itrRemoval.next();
				if(i ++ > index){
					int distance = LEVENSHTEIN_DISTANCE.apply(elem, removal);
					if(distance < REPRESENTATIVE_MIN_DISTANCE)
						itrRemoval.remove();
				}
			}

			index ++;
			limitPopulation = Math.min(limitPopulation, result.size());
		}
		return result;
	}

}
