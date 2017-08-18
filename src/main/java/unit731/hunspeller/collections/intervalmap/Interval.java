package unit731.hunspeller.collections.intervalmap;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;


/**
 * An Interval is an immutable range over any Comparable types - they do not necessarily need to be numbers.
 *
 * @param <K> the type of the bounds on the interval
 */
@Getter
@ToString
@EqualsAndHashCode
public class Interval<K extends Comparable<K>> implements Comparable<Interval<K>>{

	/* lower <= upper */
	@NonNull
	private final K lowerBound;
	@NonNull
	private final K upperBound;


	public Interval(K lower, K upper){
		if(lower.compareTo(upper) > 0){
			lowerBound = upper;
			upperBound = lower;
		}
		else{
			lowerBound = lower;
			upperBound = upper;
		}
	}

	/**
	 * Determine if two intervals overlap. Two intervals that share a bound are considered to overlap for the purposes of this method.
	 *
	 * @param other	The other interval to check
	 * @return true if they overlap
	 */
	public boolean overlaps(Interval<K> other){
		return (lowerBound.compareTo(other.getUpperBound()) <= 0 && upperBound.compareTo(other.getLowerBound()) >= 0);
	}

	@Override
	public int compareTo(Interval<K> o){
		int comparison = getLowerBound().compareTo(o.getLowerBound());
		return (comparison == 0? getUpperBound().compareTo(o.getUpperBound()): comparison);
	}

	public int compareTo(K o){
		if(getLowerBound().compareTo(o) > 0)
			return -1;
		if(getUpperBound().compareTo(o) < 0)
			return 1;
		return 0;
	}

}
