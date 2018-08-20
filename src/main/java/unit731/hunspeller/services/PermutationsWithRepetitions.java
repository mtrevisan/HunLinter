package unit731.hunspeller.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


/**
 * Generates permutations of <code>n</code> items taken <code>k</code> at a time, in lexicographic order, with repetitions
 * 
 * @see <a href="https://github.com/dasanjos/java-examples/blob/master/src/main/java/com/dasanjos/java/util/math/PermutationWithRepetitionIterator.java">dasanjos/java-examples</a>
 */
public class PermutationsWithRepetitions implements Iterator<int[]>{

	private final int n;

	private long currentIndex;
	private int[] a;


	/**
	 * @param n	the number of elements
	 */
	public PermutationsWithRepetitions(int n){
		if(n < 1)
			throw new IllegalArgumentException("At least one element needed");

		this.n = n;

		currentIndex = 0l;
		a = new int[n];
	}

	/**
	 * @param limit	Count limit for the results
	 * @return	Total permutations with repetitions of <code>n</code> elements taken <code>1…k</code> at a time
	 */
	public List<int[]> permutations(int limit){
		List<int[]> all = new ArrayList<>();
		for(int kk = 2; kk <= n && all.size() < limit; kk ++){
			PermutationsWithRepetitions pr = new PermutationsWithRepetitions(kk);
			while(pr.hasNext() && all.size() < limit)
				all.add(pr.next());
		}
		return all;
	}

	@Override
	public boolean hasNext(){
		boolean hasNext = false;
		int[] keys = convertBase(currentIndex + 1, n);
		for(int i = 0; i < keys.length; i ++)
			if(keys[i] != 0){
				hasNext = true;
				break;
			}
		return hasNext;
	}

	@Override
	public int[] next(){
		int[] result = new int[n];
		System.arraycopy(a, 0, result, 0, n);

		int[] keys = convertBase(currentIndex ++, n);
		for(int i = 0; i < keys.length; i ++)
			result[i] = a[keys[i]];
		return result;
	}

	/**
	 * Convert a number base 10 to another base
	 *
	 * @param decimalNumber	the input number (base 10)
	 * @param base	the desired new number base
	 * @return	The array of integers representing decimal number on new base
	 */
	private int[] convertBase(long decimalNumber, int base){
		int[] result = new int[n];
		int i = n;
		while(decimalNumber >= base){
			result[-- i] = (int)(decimalNumber % base);
			decimalNumber /= base;
		}
		result[-- i] = (int)decimalNumber;
		return result;
	}


	public static void main(String[] args){
//		PermutationsWithRepetitions pr = new PermutationsWithRepetitions(3);
//		while(pr.hasNext())
//			System.out.println(Arrays.toString(pr.next()));

		PermutationsWithRepetitions pr = new PermutationsWithRepetitions(3);
		List<int[]> result = pr.permutations(10);
		for(int[] res : result)
			System.out.println(Arrays.toString(res));
	}

}
