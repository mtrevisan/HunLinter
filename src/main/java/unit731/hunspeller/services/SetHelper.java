package unit731.hunspeller.services;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;


public class SetHelper{

	private SetHelper(){}


	public static Set<Character> makeCharacterSetFrom(final String text){
		return text.codePoints()
			.mapToObj(chr -> (char)chr)
			.collect(Collectors.toSet());
	}

	public static <T> Set<T> setOf(final T... values){
		return new HashSet<>(Arrays.asList(values));
	}

	public static <T> Set<T> sortedSetOf(final Comparator<? super T> comparator, final T... values){
		final Set<T> set = new TreeSet<>(comparator);
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
	public static <T> boolean isEmpty(final Set<T> set){
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
	public static <T> boolean isEquals(final Set<T> set1, final Set<T> set2){
		return (set1.size() == set2.size() && set1.containsAll(set2));
	}

	/**
	 * Returns the cardinality of the specified set (|A|).
	 * 
	 * @param <T>	The type of the values contained into the set
	 * @param set	Set
	 * @return	The cardinality of {@code set}
	 */
	public static <T> int cardinality(final Set<T> set){
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
	public static <T> Set<T> union(final Set<T> set1, final Set<T> set2){
		final Set<T> union = new HashSet<>(set1);
		union.addAll(set2);
		return union;
	}

	/**
	 * Returns a set with the union of a list of sets (A ∪ B ∪ ...).
	 * <p>
	 * The returned set contains all elements that are contained either in each set of {@code sets}.
	 * The iteration order of the returned set is undefined.
	 * 
	 * @param <T>	The type of the values contained into the sets
	 * @param sets	List of sets
	 * @return	The union of {@code sets}
	 */
	public static <T> Set<T> union(final Collection<Set<T>> sets){
		final Set<T> union = new HashSet<>();
		sets.forEach(union::addAll);
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
	public static <T> Set<T> intersection(final Set<T> set1, final Set<T> set2){
		final Set<T> intersection = new HashSet<>(set1);
		intersection.retainAll(set2);
		return intersection;
	}

	/**
	 * Returns a set with the intersection between a list of sets (A ∩ B ∩ ...).
	 * <p>
	 * The returned set contains all elements that are contained in every set of {@code sets}.
	 * The iteration order of the returned set is undefined.
	 * 
	 * @param <T>	The type of the values contained into the sets
	 * @param sets	List of sets
	 * @return	The intersection of {@code sets}
	 */
	public static <T> Set<T> intersection(final Collection<Set<T>> sets){
		final Iterator<Set<T>> itr = sets.iterator();
		final Set<T> intersection = new HashSet<>(itr.next());
		while(itr.hasNext())
			intersection.retainAll(itr.next());
		return intersection;
	}

	/**
	 * Returns a set with the intersection between two sets (A ∩ B).
	 * <p>
	 * The returned set contains all elements that are contained in {@code set1} and {@code set2}.
	 * 
	 * @param <T>	The type of the values contained into the sets
	 * @param set1	First set
	 * @param set2	Second set
	 * @return	The intersection size of {@code set1} and {@code set2}
	 */
	public static <T> int intersectionSize(final Set<T> set1, final Set<T> set2){
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
	public static <T> Set<T> difference(final Set<T> set1, final Set<T> set2){
		final Set<T> intersection = new HashSet<>(set1);
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
	public static <T> boolean isDisjoint(final Set<T> set1, final Set<T> set2){
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
	public static <T> boolean isProperSubset(final Set<T> set1, final Set<T> set2){
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
	public static <T> Set<T> symmetricDifference(final Set<T> set1, final Set<T> set2){
		final Set<T> union = new HashSet<>(set1);
		union.addAll(set2);

		final Set<T> intersection = new HashSet<>(set1);
		intersection.retainAll(set2);

		union.removeAll(intersection);

		return union;
	}

}
