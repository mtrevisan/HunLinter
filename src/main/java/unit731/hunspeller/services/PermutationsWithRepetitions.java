package unit731.hunspeller.services;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;


/**
 * Generates permutations of <code>n</code> items taken <code>k</code> at a time, in lexicographic order, with repetitions
 * 
 * @see <a href="https://github.com/dasanjos/java-examples/blob/master/src/main/java/com/dasanjos/java/util/math/PermutationWithRepetitionIterator.java">dasanjos/java-examples</a>
 */
public class PermutationsWithRepetitions implements Iterator<int[]>{

	private final int n;
	private final int k;

	private long currentIndex;


	/**
	 * @param n	the number of elements
	 * @param k	taken k at a time
	 */
	public PermutationsWithRepetitions(int n, int k){
		if(n < 1)
			throw new IllegalArgumentException("At least one element needed");
		if(k <= 0)
			throw new IllegalArgumentException("The number of elements taken should be positive");

		this.n = n;
		this.k = k;

		currentIndex = 0l;
	}

	/**
	 * @param limit	Count limit for the results
	 * @return	Total permutations with repetitions of <code>n</code> elements taken <code>1…</code> at a time
	 */
	public List<int[]> permutations(int limit){
		int size = 0;
		List<int[]> all = new ArrayList<>();
		for(int kk = 2; size < limit; kk ++){
			PermutationsWithRepetitions pr = new PermutationsWithRepetitions(n, kk);
			while(pr.hasNext() && size < limit){
				all.add(pr.next());
				size ++;
			}
		}
		return all;
	}

	@Override
	public boolean hasNext(){
		int size = (currentIndex > 0l? (int)Math.floor(Math.log(currentIndex) / Math.log(n)) + 1: 1);
		return (size <= k);
	}

	@Override
	public int[] next(){
		return convertBase(currentIndex ++, n);
	}

	/**
	 * Convert a number base 10 to another base
	 *
	 * @param decimalNumber	the input number (base 10)
	 * @param base	the desired new number base
	 * @return	The array of integers representing decimal number on new base
	 */
	private int[] convertBase(long decimalNumber, int radix) throws IllegalArgumentException{
		int[] result = new int[k];
		int i = 0;
		while(decimalNumber != 0l){
			result[i ++] = (int)(decimalNumber % radix);
			decimalNumber /= radix;
		}
		ArrayUtils.reverse(result);
		return result;
	}

}
