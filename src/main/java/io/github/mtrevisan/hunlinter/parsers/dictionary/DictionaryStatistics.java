/**
 * Copyright (c) 2019-2022 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.parsers.dictionary;

import io.github.mtrevisan.hunlinter.datastructures.bloomfilter.BloomFilterInterface;
import io.github.mtrevisan.hunlinter.datastructures.bloomfilter.BloomFilterParameters;
import io.github.mtrevisan.hunlinter.datastructures.bloomfilter.ScalableInMemoryBloomFilter;
import io.github.mtrevisan.hunlinter.languages.BaseBuilder;
import io.github.mtrevisan.hunlinter.languages.Orthography;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.Hyphenation;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.HyphenationParser;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.io.Closeable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;


/**
 * @see <a href="https://home.ubalt.edu/ntsbarsh/Business-stat/otherapplets/PoissonTest.htm">Goodness-of-Fit for Poisson</a>
 */
public class DictionaryStatistics implements Closeable{

	private static final LevenshteinDistance LEVENSHTEIN_DISTANCE = LevenshteinDistance.getDefaultInstance();


	private int totalInflections;
	private int longestWordCountByCharacters;
	private int longestWordCountBySyllabes;
	private int compoundWords;
	private int contractedWords;
	private final Frequency<Integer> lengthsFrequencies = new Frequency<>();
	private final Frequency<String> syllabesFrequencies = new Frequency<>();
	private final Frequency<Integer> syllabeLengthsFrequencies = new Frequency<>();
	private final Frequency<Integer> stressFromLastFrequencies = new Frequency<>();
	private final List<String> longestWordsByCharacters = new ArrayList<>(0);
	private final List<Hyphenation> longestWordsBySyllabes = new ArrayList<>(0);

	private final BloomFilterInterface<String> bloomFilter;
	private final Orthography orthography;


	public DictionaryStatistics(final String language, final Charset charset){
		final BloomFilterParameters dictionaryBaseData = BaseBuilder.getDictionaryBaseData(language);
		bloomFilter = new ScalableInMemoryBloomFilter<>(charset, dictionaryBaseData);
		orthography = BaseBuilder.getOrthography(language);
	}

	public final int getTotalInflections(){
		return totalInflections;
	}

	public final int getLongestWordCountByCharacters(){
		return longestWordCountByCharacters;
	}

	public final int getLongestWordCountBySyllabes(){
		return longestWordCountBySyllabes;
	}

	/** @return	The count of unique words. */
	public final int getUniqueWords(){
		return bloomFilter.getAddedElements();
	}

	/** @return	The count of compound words. */
	public final int getCompoundWords(){
		return compoundWords;
	}

	public final int getContractedWords(){
		return contractedWords;
	}

	public final Frequency<Integer> getLengthsFrequencies(){
		return lengthsFrequencies;
	}

	public final Frequency<Integer> getSyllabeLengthsFrequencies(){
		return syllabeLengthsFrequencies;
	}

	public final Frequency<Integer> getStressFromLastFrequencies(){
		return stressFromLastFrequencies;
	}

	public final List<String> getLongestWordsByCharacters(){
		return longestWordsByCharacters;
	}

	public final List<Hyphenation> getLongestWordsBySyllabes(){
		return longestWordsBySyllabes;
	}

	public final boolean hasSyllabeStatistics(){
		return (totalInflections > 0 && syllabeLengthsFrequencies.getSumOfFrequencies() > 0);
	}

	public final void addData(final String word){
		addData(word, null);
	}

	public final void addData(final String word, final Hyphenation hyphenation){
		if(hyphenation != null && !orthography.hasSyllabationErrors(hyphenation.getSyllabes())){
			final List<String> syllabes = hyphenation.getSyllabes();

			final int stressIndex = orthography.getStressedSyllabeIndexFromLast(syllabes);
			if(stressIndex >= 0)
				stressFromLastFrequencies.addValue(stressIndex);
			syllabeLengthsFrequencies.addValue(syllabes.size());
			final StringBuilder sb = new StringBuilder();
			for(int  i = 0; i < syllabes.size(); i ++){
				final String syllabe = syllabes.get(i);
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
			totalInflections ++;
		}
		else{
			lengthsFrequencies.addValue(word.length());
			storeLongestWord(word);
			if(word.contains(HyphenationParser.APOSTROPHE))
				contractedWords ++;
			totalInflections ++;
		}
	}

	private void storeLongestWord(final String word){
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

	private void storeHyphenation(final Hyphenation hyphenation){
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

	@SuppressWarnings("StringConcatenationInFormatCall")
	public final List<String> getMostCommonSyllabes(final int size){
		final List<String> values = syllabesFrequencies.getMostCommonValues(size);
		final List<String> list = new ArrayList<>(values.size());
		for(int i = 0; i < values.size(); i ++){
			final String value = values.get(i);
			list.add(value + String.format(Locale.ROOT, " (%." + Frequency.getDecimals(syllabesFrequencies.getPercentOf(value))
				+ "f%%)", syllabesFrequencies.getPercentOf(value) * 100.));
		}
		return list;
	}

	@Override
	public final void close(){
		bloomFilter.close();
	}

	public final void clear(){
		totalInflections = 0;
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
