package unit731.hunspeller.parsers.dictionary.valueobjects;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.commons.text.similarity.LevenshteinDistance;
import unit731.hunspeller.collections.bloomfilter.BloomFilterInterface;
import unit731.hunspeller.collections.bloomfilter.ScalableInMemoryBloomFilter;
import unit731.hunspeller.collections.bloomfilter.core.BitArrayBuilder;
import unit731.hunspeller.languages.CorrectnessChecker;
import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.languages.builders.OrthographyBuilder;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.hyphenation.dtos.Hyphenation;


/**
 * @see <a href="https://home.ubalt.edu/ntsbarsh/Business-stat/otherapplets/PoissonTest.htm">Goodness-of-Fit for Poisson</a>
 */
@Getter
public class DictionaryStatistics{

	private static final LevenshteinDistance LEVENSHTEIN_DISTANCE = LevenshteinDistance.getDefaultInstance();


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

	private final BloomFilterInterface<String> bloomFilter;
	@Getter
	private final Orthography orthography;


	public DictionaryStatistics(String language, Charset charset, CorrectnessChecker checker){
		bloomFilter = new ScalableInMemoryBloomFilter<>(BitArrayBuilder.Type.JAVA,
			checker.getExpectedNumberOfElements(), checker.getFalsePositiveProbability(), checker.getGrowRatioWhenFull());
		bloomFilter.setCharset(charset);
		orthography = OrthographyBuilder.getOrthography(language);
	}

	public void addData(String word){
		addData(word, null);
	}

	public void addData(String word, Hyphenation hyphenation){
		if(hyphenation != null && !hyphenation.hasErrors()){
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
			storeLongestWord(subword);
			storeHyphenation(hyphenation);
			if(subword.length() < word.length())
				compoundWords ++;
			if(subword.contains(HyphenationParser.APOSTROPHE))
				contractedWords ++;
			totalProductions ++;
		}
		else{
			lengthsFrequencies.addValue(word.length());
			storeLongestWord(word);
			if(word.length() < word.length())
				compoundWords ++;
			if(word.contains(HyphenationParser.APOSTROPHE))
				contractedWords ++;
			totalProductions ++;
		}
	}

	private void storeLongestWord(String word){
		int letterCount = orthography.countGraphemes(word);
		if(letterCount > longestWordCountByCharacters){
			longestWordsByCharacters.clear();
			longestWordsByCharacters.add(word);
			longestWordCountByCharacters = letterCount;
		}
		else if(letterCount == longestWordCountByCharacters)
			longestWordsByCharacters.add(word);

		bloomFilter.add(word);
	}

	private void storeHyphenation(Hyphenation hyphenation){
		List<String> syllabes = hyphenation.getSyllabes();
		int syllabeCount = syllabes.size();
		if(syllabeCount > longestWordCountBySyllabes){
			longestWordsBySyllabes.clear();
			longestWordsBySyllabes.add(hyphenation);
			longestWordCountBySyllabes = syllabeCount;
		}
		else if(syllabeCount == longestWordCountBySyllabes)
			longestWordsBySyllabes.add(hyphenation);
	}

	public long getTotalProductions(){
		return totalProductions;
	}

	public List<String> getMostCommonSyllabes(int size){
		return syllabesFrequencies.getMostCommonValues(size).stream()
			.map(value -> value + " (" + DictionaryParser.PERCENT_FORMATTER_1.format(syllabesFrequencies.getPercentOf(value)) + ")")
			.collect(Collectors.toList());
	}

	/** @return	The count of unique words */
	public int getUniqueWords(){
		return bloomFilter.getAddedElements();
	}

	/** @return	The count of compound words */
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
		List<String> result = new ArrayList<>(population);
		int minimumDistance = 4;
		do{
			removeClosestRepresentatives(result, limitPopulation, minimumDistance);

			minimumDistance ++;
		}while(result.size() > limitPopulation);
		return result;
	}

	private static void removeClosestRepresentatives(List<String> population, int limitPopulation, int minimumDistance){
		int index = 0;
		limitPopulation = Math.min(limitPopulation, population.size());
		while(index < limitPopulation){
			String elem = population.get(index);

			int i = 0;
			Iterator<String> itrRemoval = population.iterator();
			while(itrRemoval.hasNext()){
				String removal = itrRemoval.next();
				if(i ++ > index){
					int distance = LEVENSHTEIN_DISTANCE.apply(elem, removal);
					if(distance < minimumDistance)
						itrRemoval.remove();
				}
			}

			index ++;
			limitPopulation = Math.min(limitPopulation, population.size());
		}
	}

}
