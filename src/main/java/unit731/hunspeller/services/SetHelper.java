package unit731.hunspeller.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;


public class SetHelper{

	private SetHelper(){}


	public static <T> Set<T> setOf(T... values){
		return new HashSet<>(Arrays.asList(values));
	}

	public static <T> Set<T> sortedSetOf(Comparator<? super T> comparator, T... values){
		Set<T> set = new TreeSet<>(comparator);
		set.addAll(Arrays.asList(values));
		return set;
	}

	public static <T> Set<T> emptySet(){
		return Collections.<T>emptySet();
	}

	/**
	 * Returns {@code true} if the specified set have no elements (A = ∅).
	 * 
	 * @param <T>	The type of the values contained into the set
	 * @param set	Set
	 * @return	The emptiness of {@code set}
	 */
	public static <T> boolean isEmpty(Set<T> set){
		return set.isEmpty();
	}

	/**
	 * Returns {@code true} if the specified set have no elements (A = B).
	 * 
	 * @param <T>	The type of the values contained into the sets
	 * @param set1	First set
	 * @param set2	Second set
	 * @return	The equality of {@code set1} and {@code set2}
	 */
	public static <T> boolean isEquals(Set<T> set1, Set<T> set2){
		return (set1.size() == set2.size() && set1.containsAll(set2));
	}

	/**
	 * Returns the cardinality of the specified set (|A|).
	 * 
	 * @param <T>	The type of the values contained into the set
	 * @param set	Set
	 * @return	The cardinality of {@code set}
	 */
	public static <T> int cardinality(Set<T> set){
		return set.size();
	}

	/**
	 * Returns a set with the union of two sets (A ∪ B).
	 * <p>
	 * The returned set contains all elements that are contained either in {@code set1} and {@code set2}.
	 * The iteration order of the returned set is undefined.
	 * 
	 * @param <T>	The type of the values contained into the sets
	 * @param set1	First set
	 * @param set2	Second set
	 * @return	The union of {@code set1} and {@code set2}
	 */
	public static <T> Set<T> union(Set<T> set1, Set<T> set2){
		Set<T> union = new HashSet<>(set1);
		union.addAll(set2);
		return union;
	}

	/**
	 * Returns a set with the intersection between two sets (A ∩ B).
	 * <p>
	 * The returned set contains all elements that are contained in {@code set1} and {@code set2}.
	 * The iteration order of the returned set is undefined.
	 * 
	 * @param <T>	The type of the values contained into the sets
	 * @param set1	First set
	 * @param set2	Second set
	 * @return	The intersection of {@code set1} and {@code set2}
	 */
	public static <T> Set<T> intersection(Set<T> set1, Set<T> set2){
		Set<T> intersection = new HashSet<>(set1);
		intersection.retainAll(set2);
		return intersection;
	}

	/**
	 * Returns a set with the intersection between two sets (A ∩ B).
	 * <p>
	 * The returned set contains all elements that are contained in {@code set1} and {@code set2}.
	 * The iteration order of the returned set is undefined.
	 * 
	 * @param <T>	The type of the values contained into the sets
	 * @param set1	First set
	 * @param set2	Second set
	 * @return	The intersection size of {@code set1} and {@code set2}
	 */
	public static <T> int intersectionSize(Set<T> set1, Set<T> set2){
		Set<T> a = set1;
		Set<T> b = set2;
		if(set1.size() > set2.size()){
			a = set2;
			b = set1;
		}

		int count = 0;
		for(T e : a)
			if(b.contains(e))
				count ++;
		return count;
	}

	/**
	 * Returns a set with the difference between two sets, aka relative complement (A \ B).
	 * <p>
	 * The returned set contains all elements that are contained in {@code set1} and not in {@code set2}.
	 * The iteration order of the returned set is undefined.
	 * 
	 * @param <T>	The type of the values contained into the sets
	 * @param set1	First set
	 * @param set2	Second set
	 * @return	The difference of {@code set1} and {@code set2}
	 */
	public static <T> Set<T> difference(Set<T> set1, Set<T> set2){
		Set<T> intersection = new HashSet<>(set1);
		intersection.removeAll(set2);
		return intersection;
	}

	/**
	 * Returns {@code true} if the two specified sets have no elements in common (A ∩ B = ∅).
	 * 
	 * @param <T>	The type of the values contained into the sets
	 * @param set1	First set
	 * @param set2	Second set
	 * @return	The disjointness of {@code set1} and {@code set2}
	 */
	public static <T> boolean isDisjoint(Set<T> set1, Set<T> set2){
		return intersection(set2, set1)
			.isEmpty();
	}

	/**
	 * Returns {@code true} if {@code set2} is a proper subset of {@code set1} (A ⊂ B).
	 * 
	 * @param <T>	The type of the values contained into the sets
	 * @param set1	First set
	 * @param set2	Second set
	 * @return	The proper subset of {@code set2} into {@code set1}
	 */
	public static <T> boolean isProperSubset(Set<T> set1, Set<T> set2){
		return set1.containsAll(set2);
	}

	/**
	 * Returns a set with the symmetric difference of two sets (A △ B = (A \ B) ∪ (B \ A) = (A ∪ B) \ (A ∩ B)).
	 * <p>
	 * The returned set contains all elements that are contained in either {@code set1} or {@code set2} but not in
	 * both. The iteration order of the returned set is undefined.
	 * 
	 * @param <T>	The type of the values contained into the sets
	 * @param set1	First set
	 * @param set2	Second set
	 * @return	The symmetric difference between {@code set1} and {@code set2}
	 */
	public static <T> Set<T> symmetricDifference(Set<T> set1, Set<T> set2){
		Set<T> union = new HashSet<>(set1);
		union.addAll(set2);

		Set<T> intersection = new HashSet<>(set1);
		intersection.retainAll(set2);

		union.removeAll(intersection);

		return union;
	}

}
