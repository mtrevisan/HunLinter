package unit731.hunspeller.collections.bloomfilter;

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
public class ScalableInMemoryBloomFilter<T> extends BloomFilter<T>{

	private final double growRatioWhenFull;
	private final double tighteningRatio;

	private final List<BloomFilterInterface<T>> filters = new ArrayList<>();


	public ScalableInMemoryBloomFilter(BitArrayBuilder.Type type, int initialNumberOfElements, double falsePositiveProbability){
		this(type, initialNumberOfElements, falsePositiveProbability, 2., 0.85);
	}

	public ScalableInMemoryBloomFilter(BitArrayBuilder.Type type, int initialNumberOfElements, double falsePositiveProbability, double growRatioWhenFull){
		this(type, initialNumberOfElements, falsePositiveProbability, growRatioWhenFull, 0.85);
	}

	public ScalableInMemoryBloomFilter(BitArrayBuilder.Type type, int initialNumberOfElements, double falsePositiveProbability,
			double growRatioWhenFull, double tighteningRatio){
		super(type, initialNumberOfElements, falsePositiveProbability);

		if(growRatioWhenFull <= 1.)
			throw new IllegalArgumentException("Grow ratio when full must be strictly greater than one");
		if(tighteningRatio <= 0. && tighteningRatio >= 1.)
			throw new IllegalArgumentException("Tightening ratio must be in the interval ]0, 1[");

		this.growRatioWhenFull = growRatioWhenFull;
		this.tighteningRatio = tighteningRatio;
	}

	@Override
	public boolean add(T value){
		if(Objects.isNull(value))
			return false;

		BloomFilterInterface<T> currentFilter = (!filters.isEmpty()? filters.get(0): null);
		if(Objects.isNull(currentFilter) || currentFilter.isFull() && !currentFilter.contains(value)){
			int size = filters.size();
			currentFilter = new BloomFilter<>(type, (int)Math.ceil(expectedElements * Math.pow(growRatioWhenFull, size)),
				falsePositiveProbability * Math.pow(tighteningRatio, size));
			currentFilter.setCharset(currentCharset);
			filters.add(0, currentFilter);
		}

		return currentFilter.add(value);
	}

	@Override
	public boolean contains(T value){
		boolean result = false;
		if(Objects.nonNull(value))
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
			.map(filter -> filter.getAddedElements())
			.reduce(0, Integer::sum);
	}

	@Override
	public boolean isFull(){
		return (addedElements >= expectedElements / 2);
	}

	// P = 1 - Prod(i = 0 to l - 1 of (1 - P0 * r^i)) <= P0 / (1 - r)
	@Override
	public double getTrueFalsePositiveProbability(){
//		double p = 1.;
//		int size = filters.size();
//		for(int i = 0; i < size; i ++){
//			BloomFilter<T> filter = filters.get(size - i - 1);
//			p *= 1 - filter.getFalsePositiveProbability() * Math.pow(tighteningRatio, i);
//		}
//		return 1. - p;
		int size = filters.size();
		double probability = 0.;
		if(size > 0){
			BloomFilterInterface<T> filter = filters.get(size - 1);
			probability = filter.getFalsePositiveProbability() / (1. - tighteningRatio);
		}
		return probability;
	}

	@Override
	public void clear(){
		filters.forEach(BloomFilterInterface::clear);
	}

	@Override
	public void close(){
		filters.forEach(BloomFilterInterface::close);
	}

}
