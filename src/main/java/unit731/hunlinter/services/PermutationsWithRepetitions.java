package unit731.hunlinter.services;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;


/**
 * Generates permutations of <code>n</code> items taken <code>k</code> at a time, in lexicographic order, with repetitions
 *
 * @see <a href="https://github.com/dasanjos/java-examples/blob/master/src/main/java/com/dasanjos/java/util/math/PermutationWithRepetitionIterator.java">dasanjos/java-examples</a>
 * @see <a href="https://stackoverflow.com/questions/51946590/sequence-of-numbers-without-repeating-subsequent-digits">Sequence of numbers without repeating adjacent digits</a>
 */
public class PermutationsWithRepetitions implements Iterator<int[]>{

	private static final MessageFormat ONE_ELEMENT_MINIMUM = new MessageFormat("At least one element needed");
	private static final MessageFormat MORE_THAN_ONE_MAX_COMPOUND_MINIMUM = new MessageFormat("Number of maximum compounds must be greater than one or -1 (infinity)");
	private static final MessageFormat ONE_OUTPUT_MINIMUM = new MessageFormat("Output count must be greater than one");

	public static final int MAX_COMPOUNDS_INFINITY = -1;


	private final int n;
	private final int k;
	private final boolean forbidDuplicates;

	private long currentIndex;
	private long maximumIndex;


	/**
	 * @param n	the number of elements
	 * @param k	taken at most k at a time
	 * @param forbidDuplicates	Forbid duplicates
	 */
	public PermutationsWithRepetitions(int n, int k, boolean forbidDuplicates){
		if(n < 1)
			throw new IllegalArgumentException(ONE_ELEMENT_MINIMUM.format(new Object[0]));
		if(k != MAX_COMPOUNDS_INFINITY && k < 2)
			throw new IllegalArgumentException(MORE_THAN_ONE_MAX_COMPOUND_MINIMUM.format(new Object[0]));

		this.n = n;
		this.k = k;
		this.forbidDuplicates = forbidDuplicates;

		currentIndex = 0l;
		maximumIndex = (long)Math.pow(n, k);
		if(forbidDuplicates){
			currentIndex ++;
			maximumIndex --;
		}
	}

	/**
	 * @param limit	Count limit for the results
	 * @return	Total permutations with repetitions of <code>n</code> elements taken <code>2…maxCompounds</code> at a time
	 */
	public List<int[]> permutations(int limit){
		if(limit < 1)
			throw new IllegalArgumentException(ONE_OUTPUT_MINIMUM.format(new Object[0]));

		List<int[]> all = new ArrayList<>();
		for(int kk = 2; (k == MAX_COMPOUNDS_INFINITY || kk <= k) && all.size() < limit; kk ++)
			all.addAll(extractAllKPermutations(kk, all.size(), limit));
		return all;
	}

	private List<int[]> extractAllKPermutations(int kk, int currentCount, int limit){
		PermutationsWithRepetitions pr = new PermutationsWithRepetitions(n, kk, forbidDuplicates);

		List<int[]> all = new ArrayList<>();
		while(pr.hasNext() && all.size() + currentCount < limit)
			all.add(pr.next());
		return all;
	}

	@Override
	public boolean hasNext(){
		return (k == MAX_COMPOUNDS_INFINITY || currentIndex < maximumIndex);
	}

	@Override
	public int[] next(){
		if(!hasNext())
			throw new NoSuchElementException("No permutations left");

		int[] result = convertBase(currentIndex, n);

		if(forbidDuplicates){
			boolean consecutiveDuplicates = true;
			while(consecutiveDuplicates && currentIndex < maximumIndex)
				//if next doesn't contains consecutive duplicates, break
				consecutiveDuplicates = hasConsecutiveDuplicates(++ currentIndex, n);
		}
		else
			currentIndex ++;

		return result;
	}

	/**
	 * Convert a number base 10 to another base
	 *
	 * @param decimalNumber	the input number (base 10)
	 * @param radix	the desired new number base
	 * @return	The array of integers representing decimal number on new base
	 */
	private int[] convertBase(long decimalNumber, int radix){
		int[] result = new int[k];
		for(int i = k - 1; decimalNumber != 0l; i --){
			result[i] = (int)(decimalNumber % radix);
			decimalNumber /= radix;
		}
		return result;
	}

	/** In num = sum(i=0..k, a[i] * b^i), check if a[i] is equals to a[i - 1] for i = 1..k */
	private boolean hasConsecutiveDuplicates(long decimalNumber, int radix){
		int digit = -1;
		while(decimalNumber != 0l){
			int newDigit = (int)(decimalNumber % radix);
			if(newDigit == digit)
				return true;

			decimalNumber /= radix;
			digit = newDigit;
		}
		return false;
	}

}
