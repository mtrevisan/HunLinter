package unit731.hunlinter.services.sorters;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;


/**
 * https://github.com/ChrisKitching/JavaExternalSort/blob/master/src/uk/ac/cam/cdk23/fjava/tick0/SmoothSort.java
 * https://github.com/molgenis/systemsgenetics/blob/master/genetica-libraries/src/main/java/umcg/genetica/util/SmoothSort.java
 * @see <a href="https://www.keithschwarz.com/smoothsort/">Smoothsort Demystified</a>
 * @see <a href="https://www.keithschwarz.com/interesting/code/?dir=smoothsort>Smoothsort Demystified - source code</a>
 * https://code.google.com/archive/p/combsortcs2p-and-other-sorting-algorithms/wikis/SmoothSort.wiki
 */
public class SmoothSort{

	//by keeping these constants, we can avoid the tiresome business of keeping track of Dijkstra's b and c. Instead of keeping
	//b and c, I will keep an index into this array (the number past the last one is > 63 bits)
	private static final int[] LEONARDO_NUMBER = {1, 1, 3, 5, 9, 15, 25, 41, 67, 109, 177, 287, 465, 753, 1_219,
		1_973, 3_193, 5_167, 8_361, 13_529, 21_891, 35_421, 57_313, 92_735, 150_049, 242_785, 392_835, 635_621, 1_028_457, 1_664_079,
		2_692_537, 4_356_617, 7_049_155 /* limit for pMantissa as int*/, 11_405_773, 18_454_929, 29_860_703, 48_315_633, 78_176_337,
		126_491_971, 204_668_309, 331_160_281, 535_828_591, 866_988_873, 1_402_817_465 /* limit for data indexing as int */};

//	public static void main(String[] arg) {
//		int n = 100;
//		long[] leo = leonardo(n, 1l, 1l, 1l);
//	}
//
//	private static long[] leonardo(int n, long l0, long l1, long add) {
//		long[] leo = new long[n];
//		leo[0] = l0;
//		leo[1] = l1;
//		System.out.print(leo[0] + "l, " + leo[1] + "l");
//		for(int i = 2; i < n; i ++){
//			leo[i] = leo[i - 1] + leo[i - 2] + add;
//			System.out.print(", " + leo[i] + "l");
//		}
//		return leo;
//	}


	public static <T extends Comparable<? super T>> void sort(final T[] data, final Comparator<? super T> comparator){
		sort(data, 0, data.length, comparator, null);
	}

	public static <T extends Comparable<? super T>> void sort(final T[] data, final Comparator<? super T> comparator,
			final Consumer<Integer> progressCallback){
		sort(data, 0, data.length, comparator, progressCallback);
	}

	public static <T extends Comparable<? super T>> void sort(final T[] data, final int low, final int high,
			final Comparator<? super T> comparator){
		sort(data, low, high, comparator, null);
	}

	public static synchronized <T extends Comparable<? super T>> void sort(final T[] data, int low, int high,
			final Comparator<? super T> comparator, final Consumer<Integer> progressCallback){
		Objects.requireNonNull(data);
		Objects.requireNonNull(comparator);

		if(high - low > LEONARDO_NUMBER[LEONARDO_NUMBER.length - 1] + 1){
			//array too big to sort using this method, switch to heapsort
			HeapSort.sort(data, low, high, comparator, progressCallback);
			return;
		}

		high --;

		//the offset of the first element of the prefix into `data`
		int head = low;

		//These variables need a little explaining.
		//If our string of heaps is of length 38, then the heaps will be of size 25+9+3+1, which are Leonardo numbers 6, 4, 2, 1.
		//Turning this into a binary number, we get 0b01010110 = 0x56. We represent this number as a pair of numbers by
		//right-shifting all the zeros and storing the mantissa and exponent as `pMantissa` and `pExponent`.
		//This is handy, because the exponent is the index into L[] giving the size of the rightmost heap, and because we can
		//instantly find out if the rightmost two heaps are consecutive Leonardo numbers by checking `(pMantissa & 3) == 3`

		//the bitmap of the current standard concatenation >> pExponent
		long pMantissa = 1l;
		int pExponent = 1;

		int progress = 0;
		int progressIndex = 0;
		final int progressStep = (int)Math.ceil(((high - low) << 1) / 100.f);
		while(head < high){
			if((pMantissa & 0x03) == 0x03){
				//add 1 by merging the first two blocks into a larger one
				//the next Leonardo number is one bigger
				sift(data, pExponent, head, comparator);
				pMantissa >>>= 2;
				pExponent += 2;
			}
			else{
				//adding a new block of length 1
				if(LEONARDO_NUMBER[pExponent - 1] >= high - head)
					//this block is its final size
					trinkle(data, pMantissa, pExponent, head, false, comparator);
				else
					//this block will get merged, just make it trusty
					sift(data, pExponent, head, comparator);

				if(pExponent == 1){
					//LP[1] is being used, so we add use LP[0]
					pMantissa <<= 1;
					pExponent --;
				}
				else{
					//shift out to position 1, add LP[1]
					pMantissa <<= pExponent - 1;
					pExponent = 1;
				}
			}
			pMantissa |= 1;
			head ++;

			if(progressCallback != null && ++ progress % progressStep == 0)
				progressCallback.accept(++ progressIndex);
		}

		trinkle(data, pMantissa, pExponent, head, false, comparator);


		progressIndex = 50;
		while(pExponent != 1 || pMantissa != 1l){
			if(pExponent <= 1){
				//block of length 1. No fiddling needed
				final int trail = Long.numberOfTrailingZeros(pMantissa & ~1l);
				pMantissa >>>= trail;
				pExponent += trail;
			}
			else{
				pMantissa <<= 2;
				pMantissa ^= 7;
				pExponent -= 2;

				//This block gets broken into three bits.
				//The rightmost bit is a block of length 1.
				//The left hand part is split into two, a block of length LP[pExponent + 1] and one of LP[pExponent].
				//Both these two are appropriately heapified, but the root nodes are not necessarily in order. We therefore
				//semi-trinkle both of them

				//trinkle first child (head - LEONARDO_NUMBER[pExponent] - 1)
				trinkle(data, pMantissa >>> 1, pExponent + 1, head - LEONARDO_NUMBER[pExponent] - 1, true, comparator);
				//trinkle second child (head - 1)
				trinkle(data, pMantissa, pExponent, head - 1, true, comparator);
			}

			head --;

			if(progressCallback != null && ++ progress % progressStep == 0)
				progressCallback.accept(++ progressIndex);
		}

		if(progressCallback != null)
			progressCallback.accept(100);
	}

	/** Rebalance the tree using the standard "bubble-down" approach */
	private static <T> void sift(final T[] data, int pExponent, int root, final Comparator<? super T> comparator){
		//loop until the current node has no children, which happens when the order of the tree is 0 or 1
		while(pExponent > 1){
			final int firstChild = root - 1 - LEONARDO_NUMBER[pExponent - 2];
			final int secondChild = root - 1;

			//select larger child (first has order `k - 1`, second has order `k - 2`)
			final int largerChild = (comparator.compare(data[firstChild], data[secondChild]) >= 0? firstChild: secondChild);

			//if the root is bigger than this child, we're done
			if(comparator.compare(data[root], data[largerChild]) >= 0)
				return;

			//otherwise, swap down and update order
			swap(data, root, largerChild);
			root = largerChild;
			pExponent -= (largerChild == firstChild? 1: 2);
		}
	}

	private static <T> void swap(final T[] data, final int i, final int j){
		final T temp = data[i];
		data[i] = data[j];
		data[j] = temp;
	}

	/**
	 * Given an implicit Leonardo heap that has just had an element inserted into it at the very end, along with the
	 * size list for that heap, rectifies the heap structure by shuffling the new root down to the proper position
	 * and rebalancing the target heap
	 */
	private static <T> void trinkle(final T[] data, long pMantissa, int pExponent, int head, boolean trusty,
			final Comparator<? super T> comparator){
		//Heap with root at head has the heap property - now restoring the string property

		final T val = data[head];

		while(pMantissa != 1l){
			final int stepson = head - LEONARDO_NUMBER[pExponent];

			if(comparator.compare(data[stepson], val) <= 0)
				//current node is greater than head, sift
				break;

			//no need to check this if we know the current node is trusty,
			//because we just checked the head (which is val, in the first iteration)
			if(!trusty && pExponent > 1){
				final int firstChild = head - 1 - LEONARDO_NUMBER[pExponent - 2];
				final int secondChild = head - 1;
				if(comparator.compare(data[secondChild], data[stepson]) >= 0
						|| comparator.compare(data[firstChild], data[stepson]) >= 0)
					break;
			}

			data[head] = data[stepson];

			head = stepson;
			final int trail = Long.numberOfTrailingZeros(pMantissa & ~1l);
			pMantissa >>>= trail;
			pExponent += trail;
			trusty = false;
		}

		if(!trusty){
			data[head] = val;
			sift(data, pExponent, head, comparator);
		}
	}


	public static void sort(final byte[][] data, final Comparator<? super byte[]> comparator){
		sort(data, 0, data.length, comparator, null);
	}

	public static void sort(final byte[][] data, final Comparator<? super byte[]> comparator,
			final Consumer<Integer> progressCallback){
		sort(data, 0, data.length, comparator, progressCallback);
	}

	public static void sort(final byte[][] data, final int low, final int high, final Comparator<? super byte[]> comparator){
		sort(data, low, high, comparator, null);
	}

	public static synchronized void sort(final byte[][] data, int low, int high, final Comparator<? super byte[]> comparator,
			final Consumer<Integer> progressCallback){
		Objects.requireNonNull(data);
		Objects.requireNonNull(comparator);

		if(high - low > LEONARDO_NUMBER[LEONARDO_NUMBER.length - 1] + 1){
			//array too big to sort using this method, switch to heapsort
			HeapSort.sort(data, low, high, comparator, progressCallback);
			return;
		}

		high --;

		//the offset of the first element of the prefix into `data`
		int head = low;

		//These variables need a little explaining.
		//If our string of heaps is of length 38, then the heaps will be of size 25+9+3+1, which are Leonardo numbers 6, 4, 2, 1.
		//Turning this into a binary number, we get 0b01010110 = 0x56. We represent this number as a pair of numbers by
		//right-shifting all the zeros and storing the mantissa and exponent as `pMantissa` and `pExponent`.
		//This is handy, because the exponent is the index into L[] giving the size of the rightmost heap, and because we can
		//instantly find out if the rightmost two heaps are consecutive Leonardo numbers by checking `(pMantissa & 3) == 3`

		//the bitmap of the current standard concatenation >> pExponent
		long pMantissa = 1l;
		int pExponent = 1;

		int progress = 0;
		int progressIndex = 0;
		final int progressStep = (int)Math.ceil(((high - low) << 1) / 100.f);
		while(head < high){
			if((pMantissa & 0x03) == 0x03){
				//add 1 by merging the first two blocks into a larger one
				//the next Leonardo number is one bigger
				sift(data, pExponent, head, comparator);
				pMantissa >>>= 2;
				pExponent += 2;
			}
			else{
				//adding a new block of length 1
				if(LEONARDO_NUMBER[pExponent - 1] >= high - head)
					//this block is its final size
					trinkle(data, pMantissa, pExponent, head, false, comparator);
				else
					//this block will get merged, just make it trusty
					sift(data, pExponent, head, comparator);

				if(pExponent == 1){
					//LP[1] is being used, so we add use LP[0]
					pMantissa <<= 1;
					pExponent --;
				}
				else{
					//shift out to position 1, add LP[1]
					pMantissa <<= pExponent - 1;
					pExponent = 1;
				}
			}
			pMantissa |= 1;
			head ++;

			if(progressCallback != null && ++ progress % progressStep == 0)
				progressCallback.accept(++ progressIndex);
		}

		trinkle(data, pMantissa, pExponent, head, false, comparator);


		progressIndex = 50;
		while(pExponent != 1 || pMantissa != 1l){
			if(pExponent <= 1){
				//block of length 1. No fiddling needed
				final int trail = Long.numberOfTrailingZeros(pMantissa & ~1);
				pMantissa >>>= trail;
				pExponent += trail;
			}
			else{
				pMantissa <<= 2;
				pMantissa ^= 7;
				pExponent -= 2;

				//This block gets broken into three bits.
				//The rightmost bit is a block of length 1.
				//The left hand part is split into two, a block of length LP[pExponent + 1] and one of LP[pExponent].
				//Both these two are appropriately heapified, but the root nodes are not necessarily in order. We therefore
				//semi-trinkle both of them

				//trinkle first child (head - LEONARDO_NUMBER[pExponent] - 1)
				trinkle(data, pMantissa >>> 1, pExponent + 1, head - LEONARDO_NUMBER[pExponent] - 1, true, comparator);
				//trinkle second child (head - 1)
				trinkle(data, pMantissa, pExponent, head - 1, true, comparator);
			}

			head --;

			if(progressCallback != null && ++ progress % progressStep == 0)
				progressCallback.accept(++ progressIndex);
		}

		if(progressCallback != null)
			progressCallback.accept(100);
	}

	/** Rebalance the tree using the standard "bubble-down" approach */
	private static void sift(final byte[][] data, int pExponent, int root, final Comparator<? super byte[]> comparator){
		//loop until the current node has no children, which happens when the order of the tree is 0 or 1
		while(pExponent > 1){
			final int firstChild = root - 1 - LEONARDO_NUMBER[pExponent - 2];
			final int secondChild = root - 1;

			//select larger child (first has order `k - 1`, second has order `k - 2`)
			final int largerChild = (comparator.compare(data[firstChild], data[secondChild]) >= 0? firstChild: secondChild);

			//if the root is bigger than this child, we're done
			if(comparator.compare(data[root], data[largerChild]) >= 0)
				return;

			//otherwise, swap down and update order
			swap(data, root, largerChild);
			root = largerChild;
			pExponent -= (largerChild == firstChild? 1: 2);
		}
	}

	private static void swap(final byte[][] data, final int i, final int j){
		final byte[] temp = data[i];
		data[i] = data[j];
		data[j] = temp;
	}

	/**
	 * Given an implicit Leonardo heap that has just had an element inserted into it at the very end, along with the
	 * size list for that heap, rectifies the heap structure by shuffling the new root down to the proper position
	 * and rebalancing the target heap
	 */
	private static void trinkle(final byte[][] data, long pMantissa, int pExponent, int head, boolean trusty,
			final Comparator<? super byte[]> comparator){
		//Heap with root at head has the heap property - now restoring the string property

		final byte[] val = data[head];

		while(pMantissa != 1l){
			final int stepson = head - LEONARDO_NUMBER[pExponent];

			if(comparator.compare(data[stepson], val) <= 0)
				//current node is greater than head, sift
				break;

			//no need to check this if we know the current node is trusty,
			//because we just checked the head (which is val, in the first iteration)
			if(!trusty && pExponent > 1){
				final int firstChild = head - 1 - LEONARDO_NUMBER[pExponent - 2];
				final int secondChild = head - 1;
				if(comparator.compare(data[secondChild], data[stepson]) >= 0
						|| comparator.compare(data[firstChild], data[stepson]) >= 0)
					break;
			}

			data[head] = data[stepson];

			head = stepson;
			final int trail = Long.numberOfTrailingZeros(pMantissa & ~1);
			pMantissa >>>= trail;
			pExponent += trail;
			trusty = false;
		}

		if(!trusty){
			data[head] = val;
			sift(data, pExponent, head, comparator);
		}
	}

}
