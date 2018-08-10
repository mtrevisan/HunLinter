package unit731.hunspeller.services;

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
	private boolean hasNext = true;


	/**
	 * @param n	the number of elements
	 * @param k	taken k at a time
	 */
	public Permutations(int n, int k){
		if(n < 1)
			throw new IllegalArgumentException("Need at least 1 element!");
		if(k < 0 || k > n)
			throw new IllegalArgumentException("0 < k <= n");

		this.n = n;
		this.k = k;
		a = identityPermutation(n);
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
		int[] a = new int[n];
		for(int i = a.length - 1; i >= 0; i --)
			a[i] = i;
		return a;
	}

	private void swap(int[] a, int i, int j){
		int t = a[i];
		a[i] = a[j];
		a[j] = t;
	}

	public static void main(String[] args){
		Permutations p = new Permutations(3, 3);
		while(p.hasNext())
			System.out.println(Arrays.toString(p.next()));
	}

//	public static List<int[]> permutations(int n){
//		List<int[]> all = new ArrayList<>(n);
//		for(int k = 1; k <= n; k ++){
//			int[] kPermutations = permutations(n, k);
//			all.add(kPermutations);
//		}
//		return all;
//	}
//
//	public static int[] permutations(int n, int k){
//		if(n < 1)
//			throw new IllegalArgumentException("Need at least 1 element!");
//		if(k < 0 || k > n)
//			throw new IllegalArgumentException("0 < k <= n");
//
//		int[] a = identityPermutation(n);
//
//		//find j in (k…n-1) where a[j] > a[edge]
//		int edge = k - 1;
//		int j = k;
//		while(j < n && a[edge] >= a[j])
//			j ++;
//
//		if(j < n)
//			//swap a[edge] with a[j]
//			swap(a, edge, j);
//		else{
//			//reverse a[k] to a[n-1]
//			ArrayUtils.reverse(a, k, n);
//
//			//find rightmost ascent to left of edge
//			int i = edge - 1;
//			while(i >= 0 && a[i] >= a[i + 1])
//				i --;
//
//			if(i < 0)
//				//no more permutations
//				return null;
//
//			//find j in (n-1…i+1) where a[j] > a[i]
//			j = n - 1;
//			while(j > i && a[i] >= a[j])
//				j --;
//
//			//swap a[i] with a[j]
//			swap(a, i, j);
//			//reverse a[i+1] to a[n-1]
//			ArrayUtils.reverse(a, i + 1, n);
//		}
//
//		return Arrays.copyOfRange(a, 0, k);
//	}
//
//	public static long factorial(int n, int k){
//		if(n < 0 || n > 20)
//			throw new IllegalArgumentException(n + " is out of range");
//
//		return LongStream.rangeClosed(Math.max(k, 2), n)
//			.reduce(1, (a, b) -> a * b);
//	}
//
//	public static <T> List<T> permutation(long index, List<T> items){
//		Objects.requireNonNull(items);
//
//		return permutationHelper(index, new LinkedList<>(items), new ArrayList<>());
//
//	}
//
//	private static <T> List<T> permutationHelper(long index, LinkedList<T> in, List<T> out){
//		if(in.isEmpty())
//			return out;
//
//		long subFactorial = factorial(in.size() - 1, 0);
//		out.add(in.remove((int)(index / subFactorial)));
//		return permutationHelper((int)(index % subFactorial), in, out);
//	}
//
//	@SafeVarargs
//	@SuppressWarnings("varargs")
//	public static <T> Stream<Stream<T>> of(T... items){
//		List<T> itemList = Arrays.asList(items);
//		return LongStream.range(0, factorial(items.length, 0))
//			.mapToObj(index -> permutation(index, itemList).stream());
//	}

}
