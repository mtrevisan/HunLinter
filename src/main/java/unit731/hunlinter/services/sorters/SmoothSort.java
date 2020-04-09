package unit731.hunlinter.services.sorters;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;


/**
 * https://github.com/ChrisKitching/JavaExternalSort/blob/master/src/uk/ac/cam/cdk23/fjava/tick0/SmoothSort.java
 * https://github.com/molgenis/systemsgenetics/blob/master/genetica-libraries/src/main/java/umcg/genetica/util/SmoothSort.java
 * https://www.keithschwarz.com/smoothsort/
 */
public class SmoothSort{

	//by keeping these constants, we can avoid the tiresome business of keeping track of Dijkstra's b and c. Instead of keeping
	//b and c, I will keep an index into this array (the number past the last one is > 63 bits)
	private static final int[] LEONARDO_NUMBER = {1, 1, 3, 5, 9, 15, 25, 41, 67, 109, 177, 287, 465, 753, 1_219,
		1_973, 3_193, 5_167, 8_361, 13_529, 21_891, 35_421, 57_313, 92_735, 150_049, 242_785, 392_835, 635_621, 1_028_457, 1_664_079,
		2_692_537, 4_356_617, 7_049_155, 11_405_773, 18_454_929, 29_860_703, 48_315_633, 78_176_337, 126_491_971, 204_668_309,
		331_160_281, 535_828_591, 866_988_873, 1_402_817_465};

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


	public static <T extends Comparable<T>> void sort(final T[] data, final Comparator<? super T> comparator){
		sort(data, 0, data.length, comparator, null);
	}

	public static <T extends Comparable<T>> void sort(final T[] data, final Comparator<? super T> comparator,
			final Consumer<Integer> progressCallback){
		sort(data, 0, data.length, comparator, progressCallback);
	}

	public static <T extends Comparable<T>> void sort(final T[] data, final int low, final int high,
			final Comparator<? super T> comparator){
		sort(data, low, high, comparator, null);
	}

	public static synchronized <T extends Comparable<T>> void sort(final T[] data, int low, int high,
			final Comparator<? super T> comparator, final Consumer<Integer> progressCallback){
		Objects.requireNonNull(data);
		Objects.requireNonNull(comparator);

		if(high - low >= 866_988_873){
			//array too big to sort using this method
			HeapSort.sort(data, low, high, comparator, progressCallback);
			return;
		}

		high --;

		//the offset of the first element of the prefix into m
		int head = low;

		//These variables need a little explaining.
		//If our string of heaps is of length 38, then the heaps will be of size 25+9+3+1, which are
		//Leonardo numbers 6, 4, 2, 1.
		//Turning this into a binary number, we get 0b01010110 = 0x56. We represent
		//this number as a pair of numbers by right-shifting all the zeros and
		//storing the mantissa and exponent as `p` and `pshift`.
		//This is handy, because the exponent is the index into L[] giving the
		//size of the rightmost heap, and because we can instantly find out if
		//the rightmost two heaps are consecutive Leonardo numbers by checking
		//`(p & 3) == 3`

		//the bitmap of the current standard concatenation >> pshift
		int p = 1;
		int pshift = 1;

		int progress = 0;
		int progressIndex = 0;
		final int progressStep = (int)Math.ceil(((high - low) << 1) / 100.f);
		while(head < high){
			if((p & 3) == 3){
				//add 1 by merging the first two blocks into a larger one
				//the next Leonardo number is one bigger
				sift(data, pshift, head, comparator);
				p >>>= 2;
				pshift += 2;
			}
			else{
				//adding a new block of length 1
				if(LEONARDO_NUMBER[pshift - 1] >= high - head)
					//this block is its final size
					trinkle(data, p, pshift, head, false, comparator);
				else
					//this block will get merged, just make it trusty
					sift(data, pshift, head, comparator);

				if(pshift == 1){
					//LP[1] is being used, so we add use LP[0]
					p <<= 1;
					pshift --;
				}
				else{
					//shift out to position 1, add LP[1]
					p <<= pshift - 1;
					pshift = 1;
				}
			}
			p |= 1;
			head ++;

			if(progressCallback != null && ++ progress % progressStep == 0)
				progressCallback.accept(++ progressIndex);
		}

		trinkle(data, p, pshift, head, false, comparator);


		progressIndex = 50;
		while(pshift != 1 || p != 1){
			if(pshift <= 1){
				//block of length 1. No fiddling needed
				final int trail = Integer.numberOfTrailingZeros(p & ~1);
				p >>>= trail;
				pshift += trail;
			}
			else{
				p <<= 2;
				p ^= 7;
				pshift -= 2;

				//This block gets broken into three bits. The rightmost
				//bit is a block of length 1. The left hand part is split into
				//two, a block of length LP[pshift+1] and one of LP[pshift].
				//Both these two are appropriately heapified, but the root
				//nodes are not necessarily in order. We therefore semitrinkle
				//both of them

				trinkle(data, p >>> 1, pshift + 1, head - LEONARDO_NUMBER[pshift] - 1, true, comparator);
				trinkle(data, p, pshift, head - 1, true, comparator);
			}

			head --;

			if(progressCallback != null && ++ progress % progressStep == 0)
				progressCallback.accept(++ progressIndex);
		}
	}

	private static <T> void sift(final T[] data, int pshift, int head, final Comparator<? super T> comparator){
		//we do not use Floyd's improvements to the heapsort sift, because we
		//are not doing what heapsort does - always moving nodes from near
		//the bottom of the tree to the root.

		final T val = data[head];

		while(pshift > 1){
			final int rt = head - 1;
			final int lf = head - 1 - LEONARDO_NUMBER[pshift - 2];

			if(comparator.compare(val, data[lf]) >= 0 && comparator.compare(val, data[rt]) >= 0)
				break;

			if(comparator.compare(data[lf], data[rt]) >= 0){
System.out.println("lf"+head+"|"+data[lf]);
				data[head] = data[lf];
				head = lf;
				pshift -= 1;
			}
			else{
System.out.println("rt"+head+"|"+data[rt]);
				data[head] = data[rt];
				head = rt;
				pshift -= 2;
			}
		}

System.out.println("val"+head+"|"+val);
		data[head] = val;
	}

	private static <T> void trinkle(final T[] data, int p, int pshift, int head, boolean isTrusty,
			final Comparator<? super T> comparator){
		//Heap with root at head has the heap property - now restoring the string property

		final T val = data[head];

		while(p != 1){
			final int stepson = head - LEONARDO_NUMBER[pshift];

			if(comparator.compare(data[stepson], val) <= 0)
				//current node is greater than head, sift
				break;

			//no need to check this if we know the current node is trusty,
			//because we just checked the head (which is val, in the first iteration)
			if(!isTrusty && pshift > 1){
				final int rt = head - 1;
				final int lf = head - 1 - LEONARDO_NUMBER[pshift - 2];
				if(comparator.compare(data[rt], data[stepson]) >= 0
						|| comparator.compare(data[lf], data[stepson]) >= 0)
					break;
			}

System.out.println("h'"+head+"|"+data[stepson]);
			data[head] = data[stepson];

			head = stepson;
			final int trail = Integer.numberOfTrailingZeros(p & ~1);
			p >>>= trail;
			pshift += trail;
			isTrusty = false;
		}

		if(!isTrusty){
System.out.println("h\""+head+"|"+val);
			data[head] = val;
			sift(data, pshift, head, comparator);
		}
	}


	public static void sort(final List<byte[]> data, final Comparator<? super byte[]> comparator){
		sort(data, 0, data.size(), comparator, null);
	}

	public static void sort(final List<byte[]> data, final Comparator<? super byte[]> comparator,
			final Consumer<Integer> progressCallback){
		sort(data, 0, data.size(), comparator, progressCallback);
	}

	public static void sort(final List<byte[]> data, final int low, final int high, final Comparator<? super byte[]> comparator){
		sort(data, low, high, comparator, null);
	}

	public static synchronized void sort(final List<byte[]> data, final int low, int high,
			final Comparator<? super byte[]> comparator, final Consumer<Integer> progressCallback){
		if(high - low > 866_988_873){
			//list too big to sort using this method
			HeapSort.sort(data, low, high, comparator, progressCallback);
			return;
		}

		high --;

		//the offset of the first element of the prefix into m
		int head = low;

		//These variables need a little explaining. If our string of heaps
		//is of length 38, then the heaps will be of size 25+9+3+1, which are
		//Leonardo numbers 6, 4, 2, 1.
		//Turning this into a binary number, we get b01010110 = 0x56. We represent
		//this number as a pair of numbers by right-shifting all the zeros and
		//storing the mantissa and exponent as "p" and "pshift".
		//This is handy, because the exponent is the index into L[] giving the
		//size of the rightmost heap, and because we can instantly find out if
		//the rightmost two heaps are consecutive Leonardo numbers by checking
		//(p&3)==3

		//the bitmap of the current standard concatenation >> pshift
		int p = 1;
		int pshift = 1;

		int progress = 0;
		int progressIndex = 0;
		final int progressStep = (int)Math.ceil(((high - low) << 1) / 100.f);
		while(head < high){
			if((p & 3) == 3){
				//add 1 by merging the first two blocks into a larger one
				//the next Leonardo number is one bigger
				sift(data, pshift, head, comparator);
				p >>>= 2;
				pshift += 2;
			}
			else{
				//adding a new block of length 1
				if(LEONARDO_NUMBER[pshift - 1] >= high - head)
					//this block is its final size
					trinkle(data, p, pshift, head, false, comparator);
				else
					//this block will get merged, just make it trusty
					sift(data, pshift, head, comparator);

				if(pshift == 1){
					//LP[1] is being used, so we add use LP[0]
					p <<= 1;
					pshift --;
				}
				else{
					//shift out to position 1, add LP[1]
					p <<= pshift - 1;
					pshift = 1;
				}
			}
			p |= 1;
			head ++;

			if(progressCallback != null && ++ progress % progressStep == 0)
				progressCallback.accept(++ progressIndex);
		}

		trinkle(data, p, pshift, head, false, comparator);


		progressIndex = 50;
		while(pshift != 1 || p != 1){
			if(pshift <= 1){
				//block of length 1, no fiddling needed
				final int trail = Integer.numberOfTrailingZeros(p & ~1);
				p >>>= trail;
				pshift += trail;
			}
			else{
				p <<= 2;
				p ^= 7;
				pshift -= 2;

				//This block gets broken into three bits
				//The rightmost bit is a block of length 1
				//The left hand part is split into two, a block of length LP[pshift+1] and one of LP[pshift]
				//Both these two are appropriately heapified, but the root nodes are not necessarily in order
				//We therefore semitrinkle both of them

				trinkle(data, p >>> 1, pshift + 1, head - LEONARDO_NUMBER[pshift] - 1, true, comparator);
				trinkle(data, p, pshift, head, true, comparator);
			}

			head --;

			if(progressCallback != null && ++ progress % progressStep == 0)
				progressCallback.accept(++ progressIndex);
		}
	}

	private static void sift(final List<byte[]> data, int pshift, int head, final Comparator<? super byte[]> comparator){
		//we do not use Floyd's improvements to the heapsort sift, because we
		//are not doing what heapsort does - always moving nodes from near
		//the bottom of the tree to the root.

		final byte[] val = data.get(head);

		while(pshift > 1){
			final int rt = head - 1;
			final int lf = head - 1 - LEONARDO_NUMBER[pshift - 2];

			final byte[] lfElement = data.get(lf);
			final byte[] rtElement = data.get(rt);
			if(comparator.compare(val, lfElement) >= 0 && comparator.compare(val, rtElement) >= 0)
				break;

			if(comparator.compare(lfElement, rtElement) >= 0){
System.out.println("lf"+head+"|"+new String(lfElement));
				data.set(head, lfElement);
				head = lf;
				pshift -= 1;
			}
			else{
System.out.println("rt"+head+"|"+new String(rtElement));
				data.set(head, rtElement);
				head = rt;
				pshift -= 2;
			}
		}

System.out.println("val"+head+"|"+new String(val));
		data.set(head, val);
	}

	private static void trinkle(final List<byte[]> data, int p, int pshift, int head, boolean isTrusty,
			final Comparator<? super byte[]> comparator){
		//Heap with root at head has the heap property - now restoring the string property

		final byte[] val = data.get(head);

		while(p != 1){
			final int stepson = head - LEONARDO_NUMBER[pshift];
			final byte[] stepsonElement = data.get(stepson);

			if(comparator.compare(stepsonElement, val) <= 0)
				//current node is greater than head, sift
				break;

			//no need to check this if we know the current node is trusty,
			//because we just checked the head (which is val, in the first iteration)
			if(!isTrusty && pshift > 1){
				final int rt = head - 1;
				final int lf = head - 1 - LEONARDO_NUMBER[pshift - 2];
				if(comparator.compare(data.get(rt), stepsonElement) >= 0
						|| comparator.compare(data.get(lf), stepsonElement) >= 0)
					break;
			}

System.out.println("h'"+head+"|"+new String(stepsonElement));
			data.set(head, stepsonElement);

			head = stepson;
			final int trail = Integer.numberOfTrailingZeros(p & ~1);
			p >>>= trail;
			pshift += trail;
			isTrusty = false;
		}

		if(!isTrusty){
System.out.println("h\""+head+"|"+new String(val));
			data.set(head, val);
			sift(data, pshift, head, comparator);
		}
	}

}
