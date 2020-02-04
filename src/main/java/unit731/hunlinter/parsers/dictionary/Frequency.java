package unit731.hunlinter.parsers.dictionary;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import unit731.hunlinter.services.system.Memoizer;


/**
 * Maintains a frequency distribution.
 * <p>
 * New values added must be comparable to those that have been added, otherwise the add method will throw an Exception.
 * </p>
 * <p>
 * The values are ordered using the default (natural order).
 * </p>
 *
 * @param <T>	Type of value
 */
public class Frequency<T extends Comparable<?>>{

	private final Function<Integer, Long> SUM_OF_FREQUENCIES = Memoizer.memoize(this::sumOfFrequencies);


	private final TreeMap<T, Long> frequencies = new TreeMap<>();


	/**
	 * Adds one to the frequency count for the value.
	 *
	 * @param value	the value to add.
	 */
	public void addValue(final T value){
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
	public void incrementValue(final T value, final long increment){
		frequencies.put(value, getCount(value) + increment);
	}

	public void clear(){
		frequencies.clear();
	}

	/**
	 * Return an Iterator over the set of keys and values that have been added.
	 * Using the entry set to iterate is more efficient in the case where you
	 * need to access respective counts as well as values, since it doesn't
	 * require a "get" for every key… the value is provided in the Map.Entry.
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

	/**
	 * Returns the mode value(s) in comparator order.
	 *
	 * @return	a list containing the value(s) which appear most often.
	 */
	public List<T> getMode(){
		final long mostPopular = calculateMostPopularFrequency();

		return getMode(mostPopular);
	}

	/** Get the max count first, so we avoid having to recreate the list each time */
	private long calculateMostPopularFrequency(){
		return frequencies.values().stream()
			.mapToLong(frequency -> frequency)
			.filter(frequency -> frequency >= 0l)
			.max()
			.orElse(0l);
	}

	private List<T> getMode(final long mostPopular){
		return frequencies.entrySet().stream()
			.filter(ent -> ent.getValue() == mostPopular)
			.map(Map.Entry::getKey)
			.collect(Collectors.toList());
	}

	public List<T> getMostCommonValues(final int limit){
		final List<Map.Entry<T, Long>> sortedEntries = new ArrayList<>(frequencies.entrySet());
		sortedEntries.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

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
	public long getCount(final T value){
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
	public double getPercentOf(final T value){
		final long sumFreq = getSumOfFrequencies();
		return (sumFreq > 0? (double)getCount(value) / sumFreq: Double.NaN);
	}

	/**
	 * Returns the sum of all frequencies.
	 *
	 * @return	the total frequency count.
	 */
	public long getSumOfFrequencies(){
		return SUM_OF_FREQUENCIES.apply(frequencies.hashCode());
	}

	private long sumOfFrequencies(final int hashCode){
		return frequencies.values().stream()
			.mapToLong(value -> value)
			.sum();
	}

	public static int getDecimals(final double x){
		return (x != 0.? Math.max((int)Math.floor(Math.log10(1. / x)) - 1, 1): 0);
	}

	/**
	 * Return a string representation of this frequency distribution.
	 *
	 * @return a string representation.
	 */
	@Override
	public String toString(){
		final StringBuffer sb = new StringBuffer("Value \t Freq. \t Perc. \n");
		for(final T value : frequencies.keySet())
			sb.append(value)
				.append('\t')
				.append(getCount(value))
				.append('\t')
				.append(String.format(Locale.ROOT, "%." + getDecimals(getPercentOf(value)) + "f%%", getPercentOf(value) * 100.))
				.append('\n');
		return sb.toString();
	}

}
