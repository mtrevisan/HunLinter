package unit731.hunspeller.collections.bloomfilter;

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Stack;


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
	private final Charset charset;
	private final BloomFilterParameters parameters;

	private final Stack<BloomFilterInterface<T>> filters = new Stack<>();


	public ScalableInMemoryBloomFilter(Charset charset, BloomFilterParameters parameters){
		Objects.nonNull(charset);
		Objects.nonNull(parameters);

		parameters.validate();

		this.charset = charset;
		this.parameters = parameters;
	}

	@Override
	public synchronized boolean add(T value){
		if(value == null)
			return false;

		BloomFilterInterface<T> currentFilter = chooseCurrentFilter(value);
		return currentFilter.add(value);
	}

	private BloomFilterInterface<T> chooseCurrentFilter(T value){
		BloomFilterInterface<T> currentFilter = (!filters.isEmpty()? filters.peek(): null);
		if(currentFilter == null || !currentFilter.contains(value) && currentFilter.isFull()){
			currentFilter = fork(filters.size());

			filters.push(currentFilter);
		}
		return currentFilter;
	}

	private BloomFilterInterface<T> fork(int count){
		return new BloomFilter<>(charset, (int)Math.ceil(parameters.getExpectedNumberOfElements() * Math.pow(parameters.getGrowRatioWhenFull(), count)),
			parameters.getFalsePositiveProbability() * Math.pow(parameters.getTighteningRatio(), count), parameters.getBitArrayType(), null, null);
	}

	@Override
	public synchronized boolean contains(T value){
		return (value != null && filters.stream().anyMatch(filter -> filter.contains(value)));
	}

	@Override
	public synchronized int getAddedElements(){
		return filters.stream()
			.map(BloomFilterInterface::getAddedElements)
			.reduce(0, Integer::sum);
	}

	@Override
	public synchronized boolean isFull(){
		int addedElements = (!filters.isEmpty()? filters.peek().getAddedElements(): 0);
		return (addedElements >= parameters.getExpectedNumberOfElements() / 2);
	}

	@Override
	public double getFalsePositiveProbability(){
		return parameters.getFalsePositiveProbability();
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
	public synchronized double getTrueFalsePositiveProbability(){
		int size = filters.size();
		double p0 = filters.lastElement().getFalsePositiveProbability();
		double probability = 1.;
		for(int i = 0; i < size; i ++)
			probability *= 1 - p0 * Math.pow(parameters.getTighteningRatio(), i);
		return 1. - probability;
//		double p0 = filters.get(filters.size() - 1).getFalsePositiveProbability();
//		return p0 / (1. - parameters.getTighteningRatio());
	}

	@Override
	public synchronized void clear(){
		filters.forEach(BloomFilterInterface::clear);
	}

	@Override
	public synchronized void close(){
		filters.forEach(BloomFilterInterface::close);
	}

}
