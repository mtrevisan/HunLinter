package unit731.hunspeller.parsers.dictionary.dtos;

import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.Getter;
import unit731.hunspeller.collections.bloomfilter.BloomFilterInterface;
import unit731.hunspeller.collections.bloomfilter.ScalableInMemoryBloomFilter;
import unit731.hunspeller.collections.bloomfilter.core.BitArrayBuilder;
import unit731.hunspeller.languages.vec.WordVEC;
import unit731.hunspeller.parsers.dictionary.valueobjects.Frequency;


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


	private int totalProductions;
//	private final DescriptiveStatistics lengthsStatistics = new DescriptiveStatistics();
//	private final DescriptiveStatistics syllabesStatistics = new DescriptiveStatistics();
	private final Frequency<Integer> lengthsFrequencies = new Frequency<>();
	private final Frequency<Integer> syllabeLengthsFrequencies = new Frequency<>();
	private final Frequency<Integer> stressFromLastFrequencies = new Frequency<>();
	private final Frequency<String> syllabesFrequencies = new Frequency<>();
	private int longestWordCountByCharacters;
	private final List<String> longestWordsByCharacters = new ArrayList<>();
	private int longestWordCountBySyllabes;
	private final List<String> longestWordsBySyllabes = new ArrayList<>();
	private final BloomFilterInterface<String> bloomFilter = new ScalableInMemoryBloomFilter<>(BitArrayBuilder.Type.FAST, 40_000_000, 0.000_000_01, 1.3);


	public DictionaryStatistics(Charset charset){
		bloomFilter.setCharset(charset);
	}

	public void addLengthAndSyllabeLengthAndStressFromLast(int length, int syllabes, int stress){
//		lengthsStatistics.addValue(length);
//		syllabesStatistics.addValue(syllabes);

		lengthsFrequencies.addValue(length);
		syllabeLengthsFrequencies.addValue(syllabes);
		stressFromLastFrequencies.addValue(stress);
		totalProductions ++;
	}

	public void addSyllabes(List<String> syllabes){
		for(String syllabe : syllabes)
//FIXME WordVEC
			if(WordVEC.countLetters(syllabe) == syllabe.length())
				syllabesFrequencies.addValue(syllabe);
	}

	public void storeLongestWord(String word, int syllabes){
//FIXME WordVEC
		int letterCount = WordVEC.countLetters(word);
		if(letterCount > longestWordCountByCharacters){
			longestWordsByCharacters.clear();
			longestWordsByCharacters.add(word);
			longestWordCountByCharacters = letterCount;
		}
		else if(letterCount == longestWordCountByCharacters)
			longestWordsByCharacters.add(word);

		if(syllabes > longestWordCountBySyllabes){
			longestWordsBySyllabes.clear();
			longestWordsBySyllabes.add(word);
			longestWordCountBySyllabes = letterCount;
		}
		else if(letterCount == longestWordCountBySyllabes)
			longestWordsBySyllabes.add(word);

		bloomFilter.add(word);
	}

	public long getTotalProductions(){
		return totalProductions;
	}

	public List<String> getMostCommonSyllabes(int size){
		List<String> response = new ArrayList<>(size);
		List<String> values = syllabesFrequencies.getMostCommonValues(5);
		for(String value : values)
			response.add(value + " (" + PERCENT_FORMATTER.format(syllabesFrequencies.getPercentOf(value)) + ")");
		return response;
//		return syllabesFrequencies.getMode().stream()
//			.limit(size)
//			.map(String.class::cast)
//			.collect(Collectors.toList());
	}

	/** Returns the percentage of unique words */
	public double uniqueWords(){
		return (double)bloomFilter.getAddedElements() / totalProductions;
	}

	public void clear(){
		lengthsFrequencies.clear();
		syllabeLengthsFrequencies.clear();
	}

}
