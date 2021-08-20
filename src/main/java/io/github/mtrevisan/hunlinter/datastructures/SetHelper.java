/**
 * Copyright (c) 2019-2021 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.datastructures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.github.mtrevisan.hunlinter.services.system.LoopHelper.forEach;


public final class SetHelper{

	private SetHelper(){}


	public static Set<Character> makeCharacterSetFrom(final CharSequence text){
		return text.codePoints()
			.mapToObj(chr -> (char)chr)
			.collect(Collectors.toSet());
	}

	public static <T> Set<T> newConcurrentSet(){
		return Collections.newSetFromMap(new ConcurrentHashMap<>());
	}

	@SafeVarargs
	public static <T> Set<T> setOf(final T... values){
		return new HashSet<>(Arrays.asList(values));
	}

	@SafeVarargs
	public static <T> Set<T> sortedSetOf(final Comparator<? super T> comparator, final T... values){
		final Set<T> set = new TreeSet<>(comparator);
		set.addAll(Arrays.asList(values));
		return set;
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
	 * Returns a set with the union of a list of sets (A ∪ B ∪ …).
	 * <p>
	 * The returned set contains all elements that are contained either in each set of {@code sets}.
	 * The iteration order of the returned set is undefined.
	 *
	 * @param <T>	The type of the values contained into the sets
	 * @param sets	List of sets
	 * @return	The union of {@code sets}
	 */
	public static <T> Set<T> union(final Iterable<Set<T>> sets){
		final Set<T> union = new HashSet<>();
		forEach(sets, union::addAll);
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
	 * Returns a set with the intersection between a list of sets (A ∩ B ∩ …).
	 * <p>
	 * The returned set contains all elements that are contained in every set of {@code sets}.
	 * The iteration order of the returned set is undefined.
	 *
	 * @param <T>	The type of the values contained into the sets
	 * @param sets	List of sets
	 * @return	The intersection of {@code sets}
	 */
	public static <T> Set<T> intersection(final Iterable<Set<T>> sets){
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
		for(final T e : a)
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

		final Collection<T> intersection = new HashSet<>(set1);
		intersection.retainAll(set2);

		union.removeAll(intersection);

		return union;
	}

	public static <K extends Enum<K>, V> Map<K, List<V>> bucket(final V[] entries, final Function<V, K> keyMapper,
			final Class<K> enumClass){
		final Map<K, List<V>> bucket = new EnumMap<>(enumClass);
		for(final V entry : entries)
			processBucketEntry(bucket, keyMapper, entry);
		return bucket;
	}

	public static <K, V> Map<K, List<V>> bucket(final Iterable<V> entries, final Function<V, K> keyMapper){
		final Map<K, List<V>> bucket = new HashMap<>();
		for(final V entry : entries)
			processBucketEntry(bucket, keyMapper, entry);
		return bucket;
	}

	private static <K, V> void processBucketEntry(final Map<K, List<V>> bucket, final Function<V, K> keyMapper, final V entry){
		final K key = keyMapper.apply(entry);
		if(key != null)
			bucket.computeIfAbsent(key, k -> new ArrayList<>(1))
				.add(entry);
	}

	public static <K, V> List<V> collect(final Iterable<V> entries, final Function<V, K> keyMapper,
			final BiConsumer<V, V> mergeFunction){
		final Map<K, V> compaction = new HashMap<>();
		for(final V entry : entries){
			final K key = keyMapper.apply(entry);
			final V rule = compaction.putIfAbsent(key, entry);
			if(rule != null)
				mergeFunction.accept(rule, entry);
		}
		return new ArrayList<>(compaction.values());
	}

	@SafeVarargs
	public static <V> Set<V> getDuplicates(final V... list){
		final Collection<V> uniques = new HashSet<>(list.length);
		final Set<V> duplicates = new HashSet<>(list.length);
		for(final V elem : list)
			if(!uniques.add(elem))
				duplicates.add(elem);
		return duplicates;
	}

}
