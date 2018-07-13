package unit731.hunspeller.parsers.dictionary.valueobjects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;


public class Frequency<T>{

	private final TreeMap<T, Long> frequencies = new TreeMap<>();


	/**
	 * Adds one to the frequency count for the value.
	 *
	 * @param value	the value to add.
	 */
	public void addValue(T value){
		incrementValue(value, 1);
	}

	/**
	 * Increments the frequency count for the value.
	 * <p>
	 * If other objects have already been added to this Frequency, the value must be comparable to those that have already been added.
	 * </p>
	 *
	 * @param value	the value to add.
	 * @param increment	the amount by which the value should be incremented
	 */
	public void incrementValue(T value, long increment){
		Long count = frequencies.getOrDefault(value, 0l);
		frequencies.put(value, count + increment);
	}

	public void clear(){
		frequencies.clear();
	}

	/**
	 * Return an Iterator over the set of keys and values that have been added.
	 * Using the entry set to iterate is more efficient in the case where you
	 * need to access respective counts as well as values, since it doesn't
	 * require a "get" for every key...the value is provided in the Map.Entry.
	 * <p>
	 * If added values are integral (i.e., integers, longs, Integers, or Longs),
	 * they are converted to Longs when they are added, so the values of the
	 * map entries returned by the Iterator will in this case be Longs.</p>
	 *
	 * @return	entry set Iterator
	 */
	public Iterator<Map.Entry<T, Long>> entrySetIterator(){
		return frequencies.entrySet().iterator();
	}

	public T getMode(){
		return frequencies.firstKey();
	}

	public List<T> getMostCommonValues(int limit){
		List<Map.Entry<T, Long>> sortedEntries = new ArrayList<>(frequencies.entrySet());
		Collections.sort(sortedEntries, (e1, e2) -> e2.getValue().compareTo(e1.getValue()));

		return sortedEntries.stream()
			.limit(limit)
			.map(Map.Entry::getKey)
			.collect(Collectors.toList());
	}

	/**
	 * Returns the number of values equal to the given value.
	 * Returns 0 if the value is not comparable.
	 *
	 * @param value	the value to lookup.
	 * @return	the frequency of the given value.
	 */
	public long getCount(T value){
		return frequencies.getOrDefault(value, 0l);
	}

	/**
	 * Returns the percentage of values that are equal to the given value (as a proportion between 0 and 1).
	 * <p>
	 * Returns <code>Double.NaN</code> if no values have been added.
	 * Returns 0 if at least one value has been added, but the value is not comparable to the values set.</p>
	 *
	 * @param value	the value to lookup
	 * @return	the proportion of values equal to the given value
	 */
	public double getPercentOf(T value){
		long sumFreq = getSumOfFrequencies();
		return (sumFreq > 0? (double)getCount(value) / sumFreq: Double.NaN);
	}

	/**
	 * Returns the sum of all frequencies.
	 *
	 * @return	the total frequency count.
	 */
	public long getSumOfFrequencies(){
		long result = 0l;
		Iterator<Long> iterator = frequencies.values().iterator();
		while(iterator.hasNext())
			result += iterator.next();
		return result;
	}

}
