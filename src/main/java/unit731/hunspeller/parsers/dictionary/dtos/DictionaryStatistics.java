package unit731.hunspeller.parsers.dictionary.dtos;

import java.util.Iterator;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.stat.inference.ChiSquareTest;


/**
 * @see <a href="https://home.ubalt.edu/ntsbarsh/Business-stat/otherapplets/PoissonTest.htm">Goodness-of-Fit for Poisson</a>
 */
@Getter
public class DictionaryStatistics{

	private static final ChiSquareTest CST = new ChiSquareTest();

	@AllArgsConstructor
	@Getter
	public static enum ChiSquareConclusion{
		STRONG("Very strong evidence against Poisson"),
		MODERATE("Moderate evidence against Poisson"),
		SUGGESTIVE("Suggestive evidence against Poisson"),
		LITTLE("Little or no real evidences against Poisson");

		private final String description;

		public static ChiSquareConclusion determineConclusion(double t2){
			ChiSquareConclusion conclusion = null;
			if(t2 < 0.01)
				conclusion = ChiSquareConclusion.STRONG;
			else if(0.01 <= t2 && t2 < 0.05)
				conclusion = ChiSquareConclusion.MODERATE;
			else if(0.05 <= t2 && t2 < 0.10)
				conclusion = ChiSquareConclusion.SUGGESTIVE;
			else if(t2 >= 0.10)
				conclusion = ChiSquareConclusion.LITTLE;
			return conclusion;
		}

	}


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

	public static void main(String[] args){
		DictionaryStatistics stat = new DictionaryStatistics();
		for(int i = 0; i < 35; i ++)
			stat.addLengthAndSyllabes(0, 0);
		for(int i = 0; i < 33; i ++)
			stat.addLengthAndSyllabes(1, 0);
		for(int i = 0; i < 20; i ++)
			stat.addLengthAndSyllabes(2, 0);
		for(int i = 0; i < 6; i ++)
			stat.addLengthAndSyllabes(3, 0);
		for(int i = 0; i < 1; i ++)
			stat.addLengthAndSyllabes(4, 0);
		double bla2 = chiSquareTest(stat.getLengthsFrequencies());
		System.out.println(bla2);
	}

	public static double[] getEquivalentPoissonDistribution(Frequency frequencies){
		//calculate lambda
		long sumY = 0;
		double mean = 0.;
		Iterator<Map.Entry<Comparable<?>, Long>> itr = frequencies.entrySetIterator();
		while(itr.hasNext()){
			Map.Entry<Comparable<?>, Long> elem = itr.next();
			long xval = (Long)elem.getKey();
			double freq = elem.getValue().doubleValue();
			sumY += freq;
			mean += xval * freq;
		}
		//check for insufficent data
		if(sumY == 0)
			throw new IllegalArgumentException("Insufficient data");

		mean /= sumY;
		PoissonDistribution poisson = new PoissonDistribution(mean);

		int size = frequencies.getUniqueCount();
		double[] expected = new double[size];
		for(int i = 0; i < size; i ++)
			expected[i] = sumY * poisson.probability(i);
		return expected;
	}

	/** Returns the p-value */
	public static double chiSquareTest(Frequency frequencies){
		double[] expected = getEquivalentPoissonDistribution(frequencies);
		long[] observed = extractFrequencies(frequencies);
		int size = frequencies.getUniqueCount();
		int degreesOfFreedom = size - 2;
		ChiSquaredDistribution distribution = new ChiSquaredDistribution(null, degreesOfFreedom);
		return 1. - distribution.cumulativeProbability(CST.chiSquare(expected, observed));
	}

	private static long[] extractFrequencies(Frequency frequencies){
		int idx = 0;
		long[] normalizedValues = new long[frequencies.getUniqueCount()];
		Iterator<Map.Entry<Comparable<?>, Long>> itr = frequencies.entrySetIterator();
		while(itr.hasNext())
			normalizedValues[idx ++] = itr.next().getValue();
		return normalizedValues;
	}

}
