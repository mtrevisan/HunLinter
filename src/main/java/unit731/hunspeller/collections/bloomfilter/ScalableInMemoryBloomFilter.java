package unit731.hunspeller.collections.bloomfilter;

import java.util.ArrayList;
import java.util.List;
import unit731.hunspeller.collections.bloomfilter.interfaces.BloomFilter;


/**
 * A scalable in-memory implementation of the bloom filter.
 * Not suitable for persistence.
 * 
 * @see <a href="https://github.com/rupeshmane/scalable-bloom-filter">Scalable Bloom Filtre</a>
 * @see <a href="http://gsd.di.uminho.pt/members/cbm/ps/dbloom.pdf">DBloom</a>
 *
 * @param <T> the type of object to be stored in the filter
 */
public class ScalableInMemoryBloomFilter<T> extends InMemoryBloomFilter<T>{

	private final double growRatioWhenFull;
	private final double tighteningRatio;

	private final List<BloomFilter<T>> filters = new ArrayList<>();


	public ScalableInMemoryBloomFilter(int initialNumberOfElements, double falsePositiveProbability){
		this(initialNumberOfElements, falsePositiveProbability, 2., 0.85);
	}

	public ScalableInMemoryBloomFilter(int initialNumberOfElements, double falsePositiveProbability, double growRatioWhenFull){
		this(initialNumberOfElements, falsePositiveProbability, growRatioWhenFull, 0.85);
	}

	public ScalableInMemoryBloomFilter(int initialNumberOfElements, double falsePositiveProbability, double growRatioWhenFull,
			double tighteningRatio){
		super(initialNumberOfElements, falsePositiveProbability);

		if(growRatioWhenFull <= 1.)
			throw new IllegalArgumentException("Grow ratio when full must be strictly greater than one");
		if(tighteningRatio <= 0. && tighteningRatio >= 1.)
			throw new IllegalArgumentException("Tightening ratio must be in the interval ]0, 1[");

		this.growRatioWhenFull = growRatioWhenFull;
		this.tighteningRatio = tighteningRatio;
	}

	@Override
	public boolean add(T value){
		if(value == null)
			return false;

		BloomFilter<T> currentFilter = (!filters.isEmpty()? filters.get(0): null);
		if(currentFilter == null || currentFilter.isFull() && !currentFilter.contains(value)){
			if(currentFilter != null)
				currentFilter.close();

			int size = filters.size();
			currentFilter = new InMemoryBloomFilter<>((int)Math.ceil(expectedElements * Math.pow(growRatioWhenFull, size)),
				falsePositiveProbability * Math.pow(tighteningRatio, size));
			currentFilter.setCharset(currentCharset);
			filters.add(0, currentFilter);
		}

		return currentFilter.add(value);
	}

	@Override
	public boolean contains(T value){
		return (value != null && filters.stream().anyMatch(f -> f.contains(value)));
	}

	@Override
	public int getAddedElements(){
		return filters.stream()
			.mapToInt(BloomFilter::getAddedElements)
			.sum();
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
		BloomFilter<T> filter = filters.get(filters.size() - 1);
		return filter.getFalsePositiveProbability() / (1. - tighteningRatio);
	}

	@Override
	public void clear(){
		filters.forEach(BloomFilter::clear);
	}

	@Override
	public void close(){
		filters.forEach(BloomFilter::close);
	}

}
