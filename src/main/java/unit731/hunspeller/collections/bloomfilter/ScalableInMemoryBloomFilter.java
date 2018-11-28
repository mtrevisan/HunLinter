package unit731.hunspeller.collections.bloomfilter;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import unit731.hunspeller.collections.bloomfilter.core.BitArrayBuilder;


/**
 * A scalable in-memory implementation of the bloom filter.
 * Not suitable for persistence.
 * 
 * @see <a href="https://github.com/rupeshmane/scalable-bloom-filter">Scalable Bloom Filtre</a>
 * @see <a href="http://gsd.di.uminho.pt/members/cbm/ps/dbloom.pdf">DBloom</a>
 *
 * @param <T> the type of object to be stored in the filter
 */
public class ScalableInMemoryBloomFilter<T> implements BloomFilterInterface<T>{

	/** The default {@link Charset} is the platform encoding charset */
	private Charset charset;
	/** Expected (maximum) number of elements to be added without to transcend the falsePositiveProbability */
	private int expectedElements;
	/** The maximum false positive probability rate that the bloom filter can give */
	private double falsePositiveProbability;
	private final double growRatioWhenFull;
	private final double tighteningRatio;
	private BitArrayBuilder.Type bitArrayType;

	private final List<BloomFilterInterface<T>> filters = new ArrayList<>();


	public ScalableInMemoryBloomFilter(Charset charset, int expectedNumberOfElements, double falsePositiveProbability){
		this(charset, expectedNumberOfElements, falsePositiveProbability, 2., 0.85, BitArrayBuilder.Type.JAVA);
	}

	public ScalableInMemoryBloomFilter(Charset charset, int expectedNumberOfElements, double falsePositiveProbability,
			BitArrayBuilder.Type bitArrayType){
		this(charset, expectedNumberOfElements, falsePositiveProbability, 2., 0.85, bitArrayType);
	}

	public ScalableInMemoryBloomFilter(Charset charset, int expectedNumberOfElements, double falsePositiveProbability, double growRatioWhenFull){
		this(charset, expectedNumberOfElements, falsePositiveProbability, growRatioWhenFull, 0.85, BitArrayBuilder.Type.JAVA);
	}

	public ScalableInMemoryBloomFilter(Charset charset, int expectedNumberOfElements, double falsePositiveProbability, double growRatioWhenFull,
			BitArrayBuilder.Type bitArrayType){
		this(charset, expectedNumberOfElements, falsePositiveProbability, growRatioWhenFull, 0.85, bitArrayType);
	}

	public ScalableInMemoryBloomFilter(Charset charset, int expectedNumberOfElements, double falsePositiveProbability, double growRatioWhenFull,
			double tighteningRatio, BitArrayBuilder.Type bitArrayType){
		Objects.nonNull(charset);
		Objects.nonNull(bitArrayType);
		if(expectedNumberOfElements <= 0)
			throw new IllegalArgumentException("Number of elements must be strict positive");
		if(falsePositiveProbability <= 0. || falsePositiveProbability >= 1.)
			throw new IllegalArgumentException("False positive probability must be in ]0, 1[ interval");
		if(growRatioWhenFull <= 1.)
			throw new IllegalArgumentException("Grow ratio when full must be strictly greater than one");
		if(tighteningRatio <= 0. && tighteningRatio >= 1.)
			throw new IllegalArgumentException("Tightening ratio must be in the interval ]0, 1[");

		this.charset = charset;
		expectedElements = expectedNumberOfElements;
		this.falsePositiveProbability = falsePositiveProbability;
		this.growRatioWhenFull = growRatioWhenFull;
		this.tighteningRatio = tighteningRatio;
		this.bitArrayType = bitArrayType;
	}

	@Override
	public boolean add(T value){
		if(value == null)
			return false;

		BloomFilterInterface<T> currentFilter = (!filters.isEmpty()? filters.get(0): null);
		if(currentFilter == null || !currentFilter.contains(value) && currentFilter.isFull()){
			currentFilter = fork(filters.size());

			filters.add(0, currentFilter);
		}

		return currentFilter.add(value);
	}

	private BloomFilterInterface<T> fork(int count){
		return new BloomFilter<>(charset, (int)Math.ceil(expectedElements * Math.pow(growRatioWhenFull, count)),
			falsePositiveProbability * Math.pow(tighteningRatio, count), bitArrayType);
	}

	@Override
	public boolean contains(T value){
		boolean result = false;
		if(value != null)
			for(BloomFilterInterface<T> filter : filters)
				if(filter.contains(value)){
					result = true;
					break;
				}
		return result;
	}

	@Override
	public int getAddedElements(){
		return filters.stream()
			.map(BloomFilterInterface::getAddedElements)
			.reduce(0, Integer::sum);
	}

	@Override
	public boolean isFull(){
		int addedElements = (!filters.isEmpty()? filters.get(0).getAddedElements(): 0);
		return (addedElements >= expectedElements / 2);
	}

	@Override
	public double getFalsePositiveProbability(){
		return falsePositiveProbability;
	}

	@Override
	public double getExpectedFalsePositiveProbability(){
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public double getTrueFalsePositiveProbability(int insertedElements){
		throw new UnsupportedOperationException("Not supported yet.");
	}

	//P = 1 - Prod(i = 0 to n - 1 of (1 - P0 * r^i)) <= P0 / (1 - r)
	@Override
	public double getTrueFalsePositiveProbability(){
		int size = filters.size();
		double p0 = filters.get(size - 1).getFalsePositiveProbability();
		double probability = 1.;
		for(int i = 0; i < size; i ++)
			probability *= 1 - p0 * Math.pow(tighteningRatio, i);
		return 1. - probability;
//		double p0 = filters.get(filters.size() - 1).getFalsePositiveProbability();
//		return p0 / (1. - tighteningRatio);
	}

	@Override
	public void clear(){
		for(BloomFilterInterface<T> filter : filters)
			filter.clear();
	}

	@Override
	public void close(){
		for(BloomFilterInterface<T> filter : filters)
			filter.close();
	}

}
