package unit731.hunspeller.parsers.dictionary.dtos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.LongStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.math3.distribution.PoissonDistribution;
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
	public double chiSquareTest(){
		double mean = ((Long)lengthsFrequencies.getMode().get(0)).doubleValue();
		PoissonDistribution poisson = new PoissonDistribution(mean);

		int size = lengthsFrequencies.getUniqueCount();
		double[] expected = new double[size];
		for(int i = 0; i < size; i ++)
			expected[i] = poisson.probability(i);

		long[] observed = normalize(lengthsFrequencies);
		return CST.chiSquareTest(expected, observed);
	}

	private long[] normalize(Frequency freqs){
//		long sum = freqs.getSumFreq();
long sum = 1l;
		long[] normalizedValues = new long[freqs.getUniqueCount()];
		int idx = 0;
		Iterator<Comparable<?>> itr = freqs.valuesIterator();
		while(itr.hasNext())
			normalizedValues[idx ++] = Math.round(((Long)itr.next()).doubleValue() / sum);
		return normalizedValues;
	}

	@AllArgsConstructor
	@Getter
	public static enum ChiSquareConclusion{
		STRONG("Very strong evidence against Poisson"),
		MODERATE("Moderate evidence against Poisson"),
		SUGGESTIVE("Suggestive evidence against Poisson"),
		LITTLE("Little or no real evidences against Poisson");

		private final String description;

	}

	private double compute(int[] xval, int[] freq){
		int ny = freq.length;
		//check for insufficent data
		if(ny <= 2)
			throw new IllegalArgumentException("Insufficient data");

		//calculate lam
		long sumY = 0l;
		double lam = 0.;
		for(int i = 0; i < xval.length; i ++){
			sumY += freq[i];
			lam += xval[i] * freq[i];
		}
		//check for insufficent data
		if(sumY <= 5)
			throw new IllegalArgumentException("Insufficient data");

		double lamm = lam / sumY;
		double e_lamm = Math.exp(-lamm);
		double[] jval = new double[ny];
		int x = 0;
		for(int i = 0; i < ny; i ++){
			double lamm_x = Math.pow(lamm, x);
			jval[i] = (lamm_x * e_lamm * sumY) / factorial(x);
			x ++;
		}
		//calculate chi^2 = sum((observed - expected)^2 / expected)
		int cs = 0;
		for(int j = 0; j < jval.length; j ++){
			double delta = freq[j] - jval[j];
			cs += (delta * delta) / jval[j];
		}

		int degreesOfFreedom = ny - 2;
		double t2 = chiSquare(cs, degreesOfFreedom);

		return t2;
	}

	public ChiSquareConclusion determineConclusion(double t2){
		ChiSquareConclusion conclusion = null;
		if(t2 < 0.01)
			conclusion = ChiSquareConclusion.STRONG;
		else if(t2 < 0.05 && t2 >= 0.01)
			conclusion = ChiSquareConclusion.MODERATE;
		else if(t2 < 0.10 && t2 >= 0.05)
			conclusion = ChiSquareConclusion.SUGGESTIVE;
		else if(t2 >= 0.10)
			conclusion = ChiSquareConclusion.LITTLE;
		return conclusion;
	}

	private double chiSquare(double x, int n){
		if(n == 1 & x > 1000)
			return 0.;

		if(x > 1000 | n > 1000){
			double q = chiSquare((x - n) * (x - n) / (2 * n), 1) / 2;
			return (x > n? q: 1 - q);
		}
		double p = Math.exp(-0.5 * x);
		if((n % 2) == 1)
			p = p * Math.sqrt(2 * x / Math.PI);
		double k = n;
		while(k >= 2){
			p = p * x / k;
			k = k - 2;
		}
		double t = p;
		double a = n;
		while(t > 0.000_000_000_1 * p){
			a = a + 2;
			t = t * x / a;
			p = p + t;
		}
		return 1 - p;
	}

	private static long factorial(long n){
		return LongStream.rangeClosed(1, n)
			.reduce(1, (a, b) -> a * b);
	}

}
