package unit731.hunspeller.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;


/**
 * Generates permutations of <code>n</code> items taken <code>k</code> at a time, in lexicographic order
 * 
 * @see <a href="https://dzone.com/articles/java-8-master-permutations">Java 8: Master Permutations</a>
 * @see <a href="https://alistairisrael.wordpress.com/2009/09/22/simple-efficient-pnk-algorithm/">A simple, efficient P(n, k) algorithm </a>
 * @see <a href="https://github.com/aisrael/jcombinatorics">JCombinatorics</a>
 */
public class Permutations implements Iterator<int[]>{

	private final int n;
	private final int k;
	private final int[] a;
	private boolean hasNext;


	/**
	 * @param n	the number of elements
	 * @param k	taken k at a time
	 */
	public Permutations(int n, int k){
		if(n < 1)
			throw new IllegalArgumentException("At least one element needed");
		if(k <= 0 || k > n)
			throw new IllegalArgumentException("The number of elements taken should be between 0 and the total number of elements inclusive");

		this.n = n;
		this.k = k;
		a = identityPermutation(n);
		hasNext = true;
	}

	/**
	 * @param n	the number of elements
	 */
	public Permutations(int n){
		this(n, n);
	}

	/** Returns the count of permutations of <code>n</code> elements taken <code>k</code> at a time, that is <code>nPk</code> */
	public long count(){
		return count(k);
	}

	private long count(int kk){
		long ret = 1l;
		for(int i = n; i > n - kk; i --){
			if(i > Long.MAX_VALUE / ret)
				throw new IllegalArgumentException(String.format("Overflow. Too big numbers are used %sP%s: %d * %d", n, kk, ret, i));

			ret *= i;
		}
		return ret;
	}

	/**
	 * Returns the total count of permutations of <code>n</code> elements taken <code>2…k</code> at a time, that is
	 * <code>sum for i = 1…k of nPk</code>
	 */
	public long totalCount(){
		long ret = 0l;
		for(int kk = 2; kk <= n; kk ++)
			ret += count(kk);
		return ret;
	}

	/** Returns the total permutations of <code>n</code> elements taken <code>2…k</code> at a time */
	public List<int[]> totalPermutations(){
		List<int[]> all = new ArrayList<>(n);
		for(int kk = 2; kk <= n; kk ++){
			Permutations p = new Permutations(n, kk);
			while(p.hasNext())
				all.add(p.next());
		}
		return all;
	}

	@Override
	public boolean hasNext(){
		return hasNext;
	}

	@Override
	public int[] next(){
		int[] result = new int[k];
		System.arraycopy(a, 0, result, 0, k);

		computeNext();

		return result;
	}

	private void computeNext(){
		//find j in (k…n-1) where a[j] > a[edge]
		int edge = k - 1;
		int j = k;
		while(j < n && a[edge] >= a[j])
			j ++;

		if(j < n)
			//swap a[edge] with a[j]
			swap(a, edge, j);
		else{
			//reverse a[k] to a[n-1]
			ArrayUtils.reverse(a, k, n);

			//find rightmost ascent to left of edge
			int i = edge - 1;
			while(i >= 0 && a[i] >= a[i + 1])
				i --;

			if(i < 0){
				//no more permutations
				hasNext = false;
				return;
			}

			//find j in (n-1…i+1) where a[j] > a[i]
			j = n - 1;
			while(j > i && a[i] >= a[j])
				j --;

			//swap a[i] with a[j]
			swap(a, i, j);
			//reverse a[i+1] to a[n-1]
			ArrayUtils.reverse(a, i + 1, n);
		}
	}

	/**
	 * Creates and fills an array with a[i] = i. For example, if n = 4, then returns <code>{ 0, 1, 2, 3 }</code>. Used throughout
	 * permutation and combination generation as the first result (lexicographically).
	 *
	 * @param n	the size of the array
	 * @return the initialized array
	 */
	private int[] identityPermutation(int n){
		int[] aa = new int[n];
		for(int i = aa.length - 1; i >= 0; i --)
			aa[i] = i;
		return aa;
	}

	private void swap(int[] a, int i, int j){
		int t = a[i];
		a[i] = a[j];
		a[j] = t;
	}

	public static void main(String[] args){
//		Permutations p = new Permutations(3, 3);
//		while(p.hasNext())
//			System.out.println(Arrays.toString(p.next()));
		Permutations p = new Permutations(3);
		List<int[]> result = p.totalPermutations();
		for(int[] res : result)
			System.out.println(Arrays.toString(res));
	}

}
