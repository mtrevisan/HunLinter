package unit731.hunspeller.parsers.dictionary.dtos;

import java.util.Iterator;
import lombok.Getter;
import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.ChiSquareTest;


@Getter
public class DictionaryStatistics{

	private static final ChiSquareTest CST = new ChiSquareTest();


	private int totalProductions;
//	private final DescriptiveStatistics lengthsStatistics = new DescriptiveStatistics();
//	private final DescriptiveStatistics syllabesStatistics = new DescriptiveStatistics();
	private final Frequency lengthsFrequencies = new Frequency();
	private final Frequency syllabesFrequencies = new Frequency();


	public void addLengthAndSyllabes(int length, int syllabes){
//		lengthsStatistics.addValue(length);
//		syllabesStatistics.addValue(syllabes);

		lengthsFrequencies.addValue(length);
		syllabesFrequencies.addValue(syllabes);
		totalProductions ++;
	}

	public long getTotalProductions(){
		return totalProductions;
	}

	public void clear(){
		lengthsFrequencies.clear();
		syllabesFrequencies.clear();
	}

	/** Returns the p-value */
//	public double chiSquareTest(){
//		double[] expected = ;
//		long[] observed = normalize(lengthsFrequencies);
//		return CST.chiSquareTest(expected, observed);
//	}
//
//	private long[] normalize(Frequency freqs){
////		long sum = freqs.getSumFreq();
//long sum = 1l;
//		long[] normalizedValues = new long[freqs.getUniqueCount()];
//		int idx = 0;
//		Iterator<Comparable<?>> itr = freqs.valuesIterator();
//		while(itr.hasNext())
//			normalizedValues[idx ++] = Math.round(((Long)itr.next()).doubleValue() / sum);
//		return normalizedValues;
//	}

}
