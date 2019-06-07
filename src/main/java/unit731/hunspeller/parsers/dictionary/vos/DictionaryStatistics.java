package unit731.hunspeller.parsers.dictionary.vos;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.text.similarity.LevenshteinDistance;
import unit731.hunspeller.collections.bloomfilter.BloomFilterInterface;
import unit731.hunspeller.collections.bloomfilter.BloomFilterParameters;
import unit731.hunspeller.collections.bloomfilter.ScalableInMemoryBloomFilter;
import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.languages.BaseBuilder;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.hyphenation.dtos.Hyphenation;


/**
 * @see <a href="https://home.ubalt.edu/ntsbarsh/Business-stat/otherapplets/PoissonTest.htm">Goodness-of-Fit for Poisson</a>
 */
public class DictionaryStatistics implements Closeable{

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
	private final Orthography orthography;


	public DictionaryStatistics(final String language, final Charset charset){
		final BloomFilterParameters dictionaryBaseData = BaseBuilder.getDictionaryBaseData(language);
		bloomFilter = new ScalableInMemoryBloomFilter<>(charset, dictionaryBaseData);
		orthography = BaseBuilder.getOrthography(language);
	}

	public int getTotalProductions(){
		return totalProductions;
	}

	public int getLongestWordCountByCharacters(){
		return longestWordCountByCharacters;
	}

	public int getLongestWordCountBySyllabes(){
		return longestWordCountBySyllabes;
	}

	/** @return	The count of unique words */
	public int getUniqueWords(){
		return bloomFilter.getAddedElements();
	}

	/** @return	The count of compound words */
	public int getCompoundWords(){
		return compoundWords;
	}

	public int getContractedWords(){
		return contractedWords;
	}

	public synchronized Frequency<Integer> getLengthsFrequencies(){
		return lengthsFrequencies;
	}

	public synchronized Frequency<String> getSyllabesFrequencies(){
		return syllabesFrequencies;
	}

	public synchronized Frequency<Integer> getSyllabeLengthsFrequencies(){
		return syllabeLengthsFrequencies;
	}

	public synchronized Frequency<Integer> getStressFromLastFrequencies(){
		return stressFromLastFrequencies;
	}

	public synchronized List<String> getLongestWordsByCharacters(){
		return longestWordsByCharacters;
	}

	public synchronized List<Hyphenation> getLongestWordsBySyllabes(){
		return longestWordsBySyllabes;
	}

	public synchronized boolean hasSyllabeStatistics(){
		return (totalProductions > 0 && syllabeLengthsFrequencies.getSumOfFrequencies() > 0);
	}

	public Orthography getOrthography(){
		return orthography;
	}

	public void addData(final String word){
		addData(word, null);
	}

	public synchronized void addData(final String word, final Hyphenation hyphenation){
		if(hyphenation != null && !hyphenation.hasErrors()){
			final List<String> syllabes = hyphenation.getSyllabes();

			final List<Integer> stressIndexes = orthography.getStressIndexFromLast(syllabes);
			if(!stressIndexes.isEmpty())
				stressFromLastFrequencies.addValue(stressIndexes.get(stressIndexes.size() - 1));
			syllabeLengthsFrequencies.addValue(syllabes.size());
			final StringBuffer sb = new StringBuffer();
			for(final String syllabe : syllabes){
				sb.append(syllabe);
				if(orthography.countGraphemes(syllabe) == syllabe.length())
					syllabesFrequencies.addValue(syllabe);
			}
			final String subword = sb.toString();
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
			if(word.contains(HyphenationParser.APOSTROPHE))
				contractedWords ++;
			totalProductions ++;
		}
	}

	private synchronized void storeLongestWord(final String word){
		final int letterCount = orthography.countGraphemes(word);
		if(letterCount > longestWordCountByCharacters){
			longestWordsByCharacters.clear();
			longestWordsByCharacters.add(word);
			longestWordCountByCharacters = letterCount;
		}
		else if(letterCount == longestWordCountByCharacters)
			longestWordsByCharacters.add(word);

		bloomFilter.add(word);
	}

	private synchronized void storeHyphenation(final Hyphenation hyphenation){
		final List<String> syllabes = hyphenation.getSyllabes();
		final int syllabeCount = syllabes.size();
		if(syllabeCount > longestWordCountBySyllabes){
			longestWordsBySyllabes.clear();
			longestWordsBySyllabes.add(hyphenation);
			longestWordCountBySyllabes = syllabeCount;
		}
		else if(syllabeCount == longestWordCountBySyllabes)
			longestWordsBySyllabes.add(hyphenation);
	}

	public synchronized List<String> getMostCommonSyllabes(final int size){
		return syllabesFrequencies.getMostCommonValues(size).stream()
			.map(value -> value + " (" + DictionaryParser.PERCENT_FORMATTER_1.format(syllabesFrequencies.getPercentOf(value)) + ")")
			.collect(Collectors.toList());
	}

	@Override
	public void close(){
		bloomFilter.close();
	}

	public synchronized void clear(){
		totalProductions = 0;
		longestWordCountByCharacters = 0;
		longestWordCountBySyllabes = 0;
		lengthsFrequencies.clear();
		syllabesFrequencies.clear();
		syllabeLengthsFrequencies.clear();
		stressFromLastFrequencies.clear();
		longestWordsByCharacters.clear();
		longestWordsBySyllabes.clear();
		bloomFilter.clear();
	}


	public static List<String> extractRepresentatives(final List<String> population, final int limitPopulation){
		final List<String> result = new ArrayList<>(population);
		int minimumDistance = 4;
		do{
			removeClosestRepresentatives(result, limitPopulation, minimumDistance);

			minimumDistance ++;
		}while(result.size() > limitPopulation);
		return result;
	}

	private static void removeClosestRepresentatives(final List<String> population, int limitPopulation, final int minimumDistance){
		int index = 0;
		limitPopulation = Math.min(limitPopulation, population.size());
		while(index < limitPopulation){
			final String elem = population.get(index);

			int i = 0;
			final Iterator<String> itrRemoval = population.iterator();
			while(itrRemoval.hasNext()){
				final String removal = itrRemoval.next();
				if(i ++ > index){
					final int distance = LEVENSHTEIN_DISTANCE.apply(elem, removal);
					if(distance < minimumDistance)
						itrRemoval.remove();
				}
			}

			index ++;
			limitPopulation = Math.min(limitPopulation, population.size());
		}
	}

}
