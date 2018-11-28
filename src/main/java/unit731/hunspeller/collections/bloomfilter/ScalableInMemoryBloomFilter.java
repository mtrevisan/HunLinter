package unit731.hunspeller.collections.bloomfilter;

import java.util.ArrayList;
import java.util.List;
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
public class ScalableInMemoryBloomFilter<T> extends BloomFilter<T>{

	private final double growRatioWhenFull;
	private final double tighteningRatio;
	private BitArrayBuilder.Type bitArrayType;

	private final List<BloomFilterInterface<T>> filters = new ArrayList<>();


	public ScalableInMemoryBloomFilter(int expectedNumberOfElements, double falsePositiveProbability){
		this(expectedNumberOfElements, falsePositiveProbability, 2., 0.85, BitArrayBuilder.Type.JAVA);

		bitArrayType = BitArrayBuilder.Type.JAVA;
	}

	public ScalableInMemoryBloomFilter(int expectedNumberOfElements, double falsePositiveProbability, BitArrayBuilder.Type bitArrayType){
		this(expectedNumberOfElements, falsePositiveProbability, 2., 0.85, bitArrayType);

		this.bitArrayType = bitArrayType;
	}

	public ScalableInMemoryBloomFilter(int expectedNumberOfElements, double falsePositiveProbability, double growRatioWhenFull){
		this(expectedNumberOfElements, falsePositiveProbability, growRatioWhenFull, 0.85, BitArrayBuilder.Type.JAVA);

		bitArrayType = BitArrayBuilder.Type.JAVA;
	}

	public ScalableInMemoryBloomFilter(int expectedNumberOfElements, double falsePositiveProbability, double growRatioWhenFull,
			BitArrayBuilder.Type bitArrayType){
		this(expectedNumberOfElements, falsePositiveProbability, growRatioWhenFull, 0.85, bitArrayType);

		this.bitArrayType = bitArrayType;
	}

	public ScalableInMemoryBloomFilter(int expectedNumberOfElements, double falsePositiveProbability, double growRatioWhenFull, double tighteningRatio,
			BitArrayBuilder.Type bitArrayType){
		super(expectedNumberOfElements, falsePositiveProbability, bitArrayType);

		if(growRatioWhenFull <= 1.)
			throw new IllegalArgumentException("Grow ratio when full must be strictly greater than one");
		if(tighteningRatio <= 0. && tighteningRatio >= 1.)
			throw new IllegalArgumentException("Tightening ratio must be in the interval ]0, 1[");

		this.growRatioWhenFull = growRatioWhenFull;
		this.tighteningRatio = tighteningRatio;
		this.bitArrayType = bitArrayType;
	}

	@Override
	public boolean add(T value){
		if(value == null)
			return false;

		BloomFilterInterface<T> currentFilter = (!filters.isEmpty()? filters.get(0): null);
		if(currentFilter == null || currentFilter.isFull() && !currentFilter.contains(value)){
			int size = filters.size();
			currentFilter = new BloomFilter<>((int)Math.ceil(expectedElements * Math.pow(growRatioWhenFull, size)),
				falsePositiveProbability * Math.pow(tighteningRatio, size), bitArrayType);
			currentFilter.setCharset(currentCharset);
			filters.add(0, currentFilter);
		}

		return currentFilter.add(value);
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
		return (addedElements >= expectedElements / 2);
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
