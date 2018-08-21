package unit731.hunspeller.services;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.commons.lang3.ArrayUtils;


/**
 * Generates permutations of <code>n</code> items taken <code>k</code> at a time, in lexicographic order, with repetitions
 * 
 * @see <a href="https://github.com/dasanjos/java-examples/blob/master/src/main/java/com/dasanjos/java/util/math/PermutationWithRepetitionIterator.java">dasanjos/java-examples</a>
 */
public class PermutationsWithRepetitions implements Iterator<int[]>{

	public static final int MAX_COMPOUNDS_INFINITY = -1;


	private final int n;
	private final int k;
	private final boolean forbidDuplications;

	private long currentIndex;


	/**
	 * @param n	the number of elements
	 * @param k	taken at most k at a time
	 * @param forbidDuplications	Forbid duplications
	 */
	public PermutationsWithRepetitions(int n, int k, boolean forbidDuplications){
		if(n < 1)
			throw new IllegalArgumentException("At least one element needed");
		if(k != MAX_COMPOUNDS_INFINITY && k < 2)
			throw new IllegalArgumentException("Maximum number of maximum compounds must be greater than one or -1 (infinity)");

		this.n = n;
		this.k = k;
		this.forbidDuplications = forbidDuplications;

		currentIndex = 0l;
	}

	/**
	 * @param limit	Count limit for the results
	 * @return	Total permutations with repetitions of <code>n</code> elements taken <code>2…maxCompounds</code> at a time
	 */
	public List<int[]> permutations(int limit){
		if(limit < 1)
			throw new IllegalArgumentException("Output count must be greater than one");

		List<int[]> all = new ArrayList<>();
		for(int kk = 2; (k == MAX_COMPOUNDS_INFINITY || kk <= k) && all.size() < limit; kk ++){
			PermutationsWithRepetitions pr = new PermutationsWithRepetitions(n, kk, forbidDuplications);
			while(pr.hasNext() && all.size() < limit)
				all.add(pr.next());
		}
		return all;
	}

	@Override
	public boolean hasNext(){
		return (k == MAX_COMPOUNDS_INFINITY || (currentIndex > 0l? (int)Math.floor(Math.log(currentIndex) / Math.log(n)) + 1: 1) <= k);
	}

	@Override
	public int[] next(){
		if(!hasNext())
			throw new NoSuchElementException("No permutations left");
	
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
