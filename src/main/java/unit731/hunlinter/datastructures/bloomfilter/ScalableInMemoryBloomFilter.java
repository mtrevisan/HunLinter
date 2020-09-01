/**
 * Copyright (c) 2019-2020 Mauro Trevisan
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
package unit731.hunlinter.datastructures.bloomfilter;

import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.stream.IntStream;

import static unit731.hunlinter.services.system.LoopHelper.forEach;
import static unit731.hunlinter.services.system.LoopHelper.match;


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

	private final Deque<BloomFilterInterface<T>> filters = new ArrayDeque<>();


	public ScalableInMemoryBloomFilter(final Charset charset, final BloomFilterParameters parameters){
		Objects.requireNonNull(charset);
		Objects.requireNonNull(parameters);

		parameters.validate();

		this.charset = charset;
		this.parameters = parameters;
	}

	@Override
	public synchronized boolean add(final T value){
		if(value == null)
			return false;

		final BloomFilterInterface<T> currentFilter = chooseCurrentFilter(value);
		return currentFilter.add(value);
	}

	private BloomFilterInterface<T> chooseCurrentFilter(final T value){
		BloomFilterInterface<T> currentFilter = (!filters.isEmpty()? filters.peek(): null);
		if(currentFilter == null || !currentFilter.contains(value) && currentFilter.isFull()){
			currentFilter = fork(filters.size());

			filters.push(currentFilter);
		}
		return currentFilter;
	}

	private BloomFilterInterface<T> fork(final int count){
		return new BloomFilter<>(charset, (int)Math.ceil(parameters.getExpectedNumberOfElements() * Math.pow(parameters.getGrowRatioWhenFull(), count)),
			parameters.getFalsePositiveProbability() * Math.pow(parameters.getTighteningRatio(), count), parameters.getBitArrayType(), null, null);
	}

	@Override
	public synchronized boolean contains(final T value){
		return (value != null && match(filters, filter -> filter.contains(value)) != null);
	}

	@Override
	public synchronized int getAddedElements(){
		int elements = 0;
		for(final BloomFilterInterface<T> filter : filters)
			elements += filter.getAddedElements();
		return elements;
	}

	@Override
	public synchronized boolean isFull(){
		final int addedElements = (!filters.isEmpty()? filters.peek().getAddedElements(): 0);
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
	public double getTrueFalsePositiveProbability(final int insertedElements){
		throw new UnsupportedOperationException("Not supported yet.");
	}

	//P = 1 - Prod(i = 0 to n - 1 of (1 - P0 * r^i)) <= P0 / (1 - r)
	@Override
	public synchronized double getTrueFalsePositiveProbability(){
		final int size = filters.size();
		final double p0 = filters.getLast().getFalsePositiveProbability();
		final double probability = IntStream.range(0, size)
			.mapToDouble(i -> 1. - p0 * Math.pow(parameters.getTighteningRatio(), i))
			.reduce(1., (a, b) -> a * b);
		return 1. - probability;
//		final double p0 = filters.get(filters.size() - 1).getFalsePositiveProbability();
//		return p0 / (1. - parameters.getTighteningRatio());
	}

	@Override
	public synchronized void clear(){
		forEach(filters, BloomFilterInterface::clear);
	}

	@Override
	public synchronized void close(){
		forEach(filters, BloomFilterInterface::close);
	}

}
