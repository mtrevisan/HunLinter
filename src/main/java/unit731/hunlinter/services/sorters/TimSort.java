package unit731.hunlinter.services.sorters;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


/**
 * A stable, adaptive, iterative mergesort that requires far fewer than
 * n lg(n) comparisons when running on partially sorted arrays, while
 * offering performance comparable to a traditional mergesort when run
 * on random arrays. Like all proper mergesorts, this sort is stable and
 * runs O(n log n) time (worst case). In the worst case, this sort requires
 * temporary storage space for n/2 object references; in the best case,
 * it requires only a small constant amount of space.
 *
 * This implementation was adapted from Tim Peters's list sort for
 * Python, which is described in detail here:
 *
 *		http://svn.python.org/projects/python/trunk/Objects/listsort.txt
 *
 * Tim's C code may be found here:
 *
 *		http://svn.python.org/projects/python/trunk/Objects/listobject.c
 *
 * The underlying techniques are described in this paper (and may have
 * even earlier origins):
 *
 *		"Optimistic Sorting and Information Theoretic Complexity"
 *		Peter McIlroy
 *		SODA (Fourth Annual ACM-SIAM Symposium on Discrete Algorithms),
 *		pp 467-474, Austin, Texas, 25-27 January 1993.
 *
 * While the API to this class consists solely of static methods, it is
 * (privately) instantiable; a TimSort instance holds the state of an ongoing
 * sort, assuming the input array is large enough to warrant the full-blown
 * TimSort. Small arrays are sorted in place, using a binary insertion sort.
 *
 * @author Josh Bloch
 *
 * @see <a href="https://hackernoon.com/timsort-the-fastest-sorting-algorithm-youve-never-heard-of-36b28417f399">Timsort — the fastest sorting algorithm you’ve never heard of</>
 * @see <a href="https://github.com/abstools/timsort-benchmark">Java TimSort Benchmarking</>
 */
public class TimSort<T>{

	/**
	 * This is the minimum sized sequence that will be merged. Shorter
	 * sequences will be lengthened by calling binarySort. If the entire
	 * array is less than this length, no merges will be performed.
	 * <p>
	 * This constant should be a power of two. It was 64 in Tim Peter's C
	 * implementation, but 32 was empirically determined to work better in
	 * this implementation. In the unlikely event that you set this constant
	 * to be a number that's not a power of two, you'll need to change the
	 * {@link #minRunLength} computation.
	 * <p>
	 * If you decrease this constant, you must change the stackLen
	 * computation in the TimSort constructor, or you risk an
	 * ArrayOutOfBounds exception. See listsort.txt for a discussion
	 * of the minimum stack length required as a function of the length
	 * of the array being sorted and the minimum merge sequence length.
	 */
	private static final int MIN_MERGE = 32;

	/** The array being sorted */
	private final T[] array;

	/** The comparator for this sort */
	private final Comparator<? super T> comparator;

	/**
	 * When we get into galloping mode, we stay there until both runs win less
	 * often than MIN_GALLOP consecutive times.
	 */
	private static final int MIN_GALLOP = 7;

	/**
	 * This controls when we get *into* galloping mode. It is initialized
	 * to MIN_GALLOP. The mergeLo and mergeHi methods nudge it higher for
	 * random data, and lower for highly structured data.
	 */
	private int minGallop = MIN_GALLOP;

	/**
	 * Maximum initial size of tmp array, which is used for merging. The array
	 * can grow to accommodate demand.
	 * <p>
	 * Unlike Tim's original C version, we do not allocate this much storage
	 * when sorting smaller arrays. This change was required for performance.
	 */
	private static final int INITIAL_TMP_STORAGE_LENGTH = 256;

	/** Temporary storage for merges (actual runtime type will be Object[], regardless of T) */
	private T[] tmp;

	/**
	 * A stack of pending runs yet to be merged. Run `i` starts at
	 * address base[i] and extends for len[i] elements. It's always
	 * true (so long as the indices are in bounds) that:
	 * <p>
	 * runBase[i] + runLen[i] == runBase[i + 1]
	 * <p>
	 * so we could cut the storage for this, but it's a minor amount,
	 * and keeping all the info explicit simplifies the code.
	 */
	private int stackSize = 0;	//number of pending runs on stack
	private final int[] runBase;
	private final int[] runLen;


	/**
	 * Creates a TimSort instance to maintain the state of an ongoing sort.
	 *
	 * @param array the array to be sorted
	 * @param comparator the comparator to determine the order of the sort
	 */
	private TimSort(final T[] array, final Comparator<? super T> comparator){
		this.array = array;
		this.comparator = comparator;

		//allocate temp storage (which may be increased later if necessary)
		final int len = array.length;
		@SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
		final T[] newArray = (T[])new Object[len < 2 * INITIAL_TMP_STORAGE_LENGTH? len >>> 1: INITIAL_TMP_STORAGE_LENGTH];
		tmp = newArray;

		/*
		 * Allocate runs-to-be-merged stack (which cannot be expanded). The
		 * stack length requirements are described in listsort.txt. The C
		 * version always uses the same stack length (85), but this was
		 * measured to be too expensive when sorting "mid-sized" arrays (e.g.,
		 * 100 elements) in Java. Therefore, we use smaller (but sufficiently
		 * large) stack lengths for smaller arrays. The "magic numbers" in the
		 * computation below must be changed if MIN_MERGE is decreased. See
		 * the MIN_MERGE declaration above for more information.
		 */
		final int stackLen = (len < 120? 5: (len < 1542? 10: (len < 119151? 24: 40)));
		runBase = new int[stackLen];
		runLen = new int[stackLen];
	}

	public static <T> void sort(final T[] array, final Comparator<? super T> comparator){
		sort(array, 0, array.length, comparator);
	}

	public static <T> void sort(final T[] array, int low, final int high, final Comparator<? super T> comparator){
		Objects.requireNonNull(array);
		Objects.requireNonNull(comparator);
		assert low < high;

		rangeCheck(array.length, low, high);
		int remaining = high - low;
		if(remaining < 2)
			//arrays of size 0 and 1 are always sorted
			return;

		//if array is small, do a "mini-TimSort" with no merges
		if(remaining < MIN_MERGE){
			final int initRunLen = countRunAndMakeAscending(array, low, high, comparator);
			binarySort(array, low, high, low + initRunLen, comparator);
			return;
		}

		/*
		 * March over the array once, left to right, finding natural runs,
		 * extending short natural runs to minRun elements, and merging runs
		 * to maintain stack invariant.
		 */
		final TimSort<T> ts = new TimSort<>(array, comparator);
		final int minRun = minRunLength(remaining);
		do{
			//identify next run
			int runLen = countRunAndMakeAscending(array, low, high, comparator);

			//if run is short, extend to min(minRun, nRemaining)
			if(runLen < minRun){
				final int force = Math.min(remaining, minRun);
				binarySort(array, low, low + force, low + runLen, comparator);
				runLen = force;
			}

			//push run onto pending-run stack, and maybe merge
			ts.pushRun(low, runLen);
			ts.mergeCollapse();

			//advance to find next run
			low += runLen;
			remaining -= runLen;
		}while(remaining != 0);

		assert low == high;

		//merge all remaining runs to complete sort
		ts.mergeForceCollapse();

		assert ts.stackSize == 1;
	}

	/**
	 * Sorts the specified portion of the specified array using a binary
	 * insertion sort. This is the best method for sorting small numbers
	 * of elements. It requires O(n * log(n)) compares, but O(n^2) data
	 * movement (worst case).
	 * <p>
	 * If the initial part of the specified range is already sorted,
	 * this method can take advantage of it: the method assumes that the
	 * elements from index {@code lo}, inclusive, to {@code start},
	 * exclusive are already sorted.
	 *
	 * @param array	the array in which a range is to be sorted
	 * @param low	the index of the first element in the range to be sorted
	 * @param high	the index after the last element in the range to be sorted
	 * @param start	the index of the first element in the range that is not already known to be sorted (@code lo <= start <= hi}
	 * @param comparator	comparator to used for the sort
	 */
	@SuppressWarnings("fallthrough")
	private static <T> void binarySort(final T[] array, final int low, final int high, int start,
			final Comparator<? super T> comparator){
		assert low <= start && start <= high;

		if(start == low)
			start ++;
		for(; start < high; start ++){
			final T pivot = array[start];

			//set left (and right) to the index where a[start] (pivot) belongs
			int left = low;
			int right = start;

			assert left <= right;

			/*
			 * Invariants:
			 *		pivot >= all in [low, left).
			 *		pivot < all in [right, start).
			 */
			while(left < right){
				final int mid = (left + right) >>> 1;
				if(comparator.compare(pivot, array[mid]) < 0)
					right = mid;
				else
					left = mid + 1;
			}
			assert left == right;

			/*
			 * The invariants still hold: pivot >= all in [low, left) and
			 * pivot < all in [left, start), so pivot belongs at left. Note
			 * that if there are elements equal to pivot, left points to the
			 * first slot after them -- that's why this sort is stable.
			 * Slide elements over to make room to make room for pivot.
			 */
			final int n = start - left;	//the number of elements to move
			//switch is just an optimization for arraycopy in default case
			switch(n){
				case 2:
					array[left + 2] = array[left + 1];

				case 1:
					array[left + 1] = array[left];
					break;

				default:
					System.arraycopy(array, left, array, left + 1, n);
			}
			array[left] = pivot;
		}
	}

	/**
	 * Returns the length of the run beginning at the specified position in
	 * the specified array and reverses the run if it is descending (ensuring
	 * that the run will always be ascending when the method returns).
	 * <p>
	 * A run is the longest ascending sequence with:
	 * <p>
	 * a[lo] <= a[lo + 1] <= a[lo + 2] <= ...
	 * <p>
	 * or the longest descending sequence with:
	 * <p>
	 * a[lo] > a[lo + 1] > a[lo + 2] > ...
	 * <p>
	 * For its intended use in a stable mergesort, the strictness of the
	 * definition of "descending" is needed so that the call can safely
	 * reverse a descending sequence without violating stability.
	 *
	 * @param array	the array in which a run is to be counted and possibly reversed
	 * @param low	index of the first element in the run
	 * @param high	index after the last element that may be contained in the run.
	 *		It is required that @code{lo < hi}.
	 * @param comparator	the comparator to used for the sort
	 * @return	the length of the run beginning at the specified position in the specified array
	 */
	private static <T> int countRunAndMakeAscending(final T[] array, final int low, final int high,
			final Comparator<? super T> comparator){
		int runHigh = low + 1;
		if(runHigh == high)
			return 1;

		//find end of run, and reverse range if descending
		//descending
		if(comparator.compare(array[runHigh ++], array[low]) < 0){
			while(runHigh < high && comparator.compare(array[runHigh], array[runHigh - 1]) < 0)
				runHigh ++;
			reverseRange(array, low, runHigh);
		}
		//ascending
		else
			while(runHigh < high && comparator.compare(array[runHigh], array[runHigh - 1]) >= 0)
				runHigh ++;

		return runHigh - low;
	}

	/**
	 * Reverse the specified range of the specified array.
	 *
	 * @param array	the array in which a range is to be reversed
	 * @param low	the index of the first element in the range to be reversed
	 * @param high	the index after the last element in the range to be reversed
	 */
	private static void reverseRange(final Object[] array, int low, int high){
		high --;
		while(low < high){
			final Object t = array[low];
			array[low ++] = array[high];
			array[high --] = t;
		}
	}

	/**
	 * Returns the minimum acceptable run length for an array of the specified
	 * length. Natural runs shorter than this will be extended with
	 * {@link #binarySort}.
	 * <p>
	 * Roughly speaking, the computation is:
	 * <p>
	 * If n < MIN_MERGE, return n (it's too small to bother with fancy stuff).
	 * Else if n is an exact power of 2, return MIN_MERGE/2.
	 * Else return an int k, MIN_MERGE/2 <= k <= MIN_MERGE, such that n/k
	 * is close to, but strictly less than, an exact power of 2.
	 * <p>
	 * For the rationale, see listsort.txt.
	 *
	 * @param n the length of the array to be sorted
	 * @return the length of the minimum run to be merged
	 */
	private static int minRunLength(int n){
		assert n >= 0;

		//becomes 1 if any 1 bits are shifted off
		int r = 0;
		while(n >= MIN_MERGE){
			r |= (n & 1);
			n >>= 1;
		}
		return n + r;
	}

	/**
	 * Pushes the specified run onto the pending-run stack.
	 *
	 * @param runBase	index of the first element in the run
	 * @param runLen	the number of elements in the run
	 */
	private void pushRun(final int runBase, final int runLen){
		this.runBase[stackSize] = runBase;
		this.runLen[stackSize] = runLen;
		stackSize ++;
	}

	/**
	 * Examines the stack of runs waiting to be merged and merges adjacent runs
	 * until the stack invariants are reestablished:
	 * <p>
	 * 1. runLen[i - 3] > runLen[i - 2] + runLen[i - 1]
	 * 2. runLen[i - 2] > runLen[i - 1]
	 * <p>
	 * This method is called each time a new run is pushed onto the stack,
	 * so the invariants are guaranteed to hold for i < stackSize upon
	 * entry to the method.
	 */
	private void mergeCollapse(){
		while(stackSize > 1){
			int n = stackSize - 2;
			if(n > 0 && runLen[n - 1] <= runLen[n] + runLen[n + 1]){
				if(runLen[n - 1] < runLen[n + 1])
					n --;
				mergeAt(n);
			}
			else if(runLen[n] <= runLen[n + 1])
				mergeAt(n);

			//invariant is established
			break;
		}
	}

	private void newMergeCollapse(){
		while(stackSize > 1){
			int n = stackSize - 2;
			if(n >= 1 && runLen[n - 1] <= runLen[n] + runLen[n + 1] || n >= 2 && runLen[n - 2] <= runLen[n - 1] + runLen[n]){
				if(runLen[n - 1] < runLen[n + 1])
					n --;
			}
			else if(n < 0 || runLen[n] > runLen[n + 1])
				//invariant is established
				break;

			mergeAt(n);
		}
	}

	/** Merges all runs on the stack until only one remains. This method is called once, to complete the sort. */
	private void mergeForceCollapse(){
		while(stackSize > 1){
			int n = stackSize - 2;
			if(n > 0 && runLen[n - 1] < runLen[n + 1])
				n --;

			mergeAt(n);
		}
	}

	/**
	 * Merges the two runs at stack indices i and i+1. Run i must be
	 * the penultimate or antepenultimate run on the stack. In other words,
	 * i must be equal to stackSize-2 or stackSize-3.
	 *
	 * @param i stack index of the first of the two runs to merge
	 */
	private void mergeAt(final int i){
		assert stackSize >= 2;
		assert i >= 0;
		assert i == stackSize - 2 || i == stackSize - 3;

		int base1 = runBase[i];
		int len1 = runLen[i];
		final int base2 = runBase[i + 1];
		int len2 = runLen[i + 1];
		assert len1 > 0 && len2 > 0;
		assert base1 + len1 == base2;

		/*
		 * Record the length of the combined runs; if i is the 3rd-last
		 * run now, also slide over the last run (which isn't involved
		 * in this merge). The current run (i+1) goes away in any case.
		 */
		runLen[i] = len1 + len2;
		if(i == stackSize - 3){
			runBase[i + 1] = runBase[i + 2];
			runLen[i + 1] = runLen[i + 2];
		}
		stackSize--;

		/*
		 * Find where the first element of run2 goes in run1. Prior elements
		 * in run1 can be ignored (because they're already in place).
		 */
		final int k = gallopRight(array[base2], array, base1, len1, 0, comparator);
		assert k >= 0;
		base1 += k;
		len1 -= k;
		if(len1 == 0)
			return;

		/*
		 * Find where the last element of run1 goes in run2. Subsequent elements
		 * in run2 can be ignored (because they're already in place).
		 */
		len2 = gallopLeft(array[base1 + len1 - 1], array, base2, len2, len2 - 1, comparator);
		assert len2 >= 0;
		if(len2 == 0)
			return;

		//merge remaining runs, using tmp array with min(len1, len2) elements
		if(len1 <= len2)
			mergeLow(base1, len1, base2, len2);
		else
			mergeHigh(base1, len1, base2, len2);
	}

	/**
	 * Locates the position at which to insert the specified key into the
	 * specified sorted range; if the range contains an element equal to key,
	 * returns the index of the leftmost equal element.
	 *
	 * @param key	the key whose insertion point to search for
	 * @param array	the array in which to search
	 * @param base	the index of the first element in the range
	 * @param len 	the length of the range; must be > 0
	 * @param hint	the index at which to begin the search, 0 <= hint < n.
	 *		The closer hint is to the result, the faster this method will run.
	 * @param comparator	the comparator used to order the range, and to search
	 * @return	the int k, 0 <= k <= n such that a[b + k - 1] < key <= a[b + k],
	 * pretending that a[b - 1] is minus infinity and a[b + n] is infinity.
	 * In other words, key belongs at index b + k; or in other words,
	 * the first k elements of a should precede key, and the last n - k
	 * should follow it.
	 */
	private static <T> int gallopLeft(final T key, final T[] array, final int base, final int len, final int hint,
			final Comparator<? super T> comparator){
		assert hint >= 0 && hint < len;

		int lastOfs = 0;
		int ofs = 1;
		if(comparator.compare(key, array[base + hint]) > 0){
			//gallop right until a[base+hint+lastOfs] < key <= a[base+hint+ofs]
			final int maxOfs = len - hint;
			while(ofs < maxOfs && comparator.compare(key, array[base + hint + ofs]) > 0){
				lastOfs = ofs;
				ofs = (ofs << 1) + 1;
				//int overflow
				if(ofs <= 0)
					ofs = maxOfs;
			}
			if(ofs > maxOfs)
				ofs = maxOfs;

			//make offsets relative to base
			lastOfs += hint;
			ofs += hint;
		}
		//key <= a[base + hint]
		else{
			//gallop left until a[base+hint-ofs] < key <= a[base+hint-lastOfs]
			final int maxOfs = hint + 1;
			while(ofs < maxOfs && comparator.compare(key, array[base + hint - ofs]) <= 0){
				lastOfs = ofs;
				ofs = (ofs << 1) + 1;
				//int overflow
				if(ofs <= 0)
					ofs = maxOfs;
			}
			if(ofs > maxOfs)
				ofs = maxOfs;

			//make offsets relative to base
			final int tmp = lastOfs;
			lastOfs = hint - ofs;
			ofs = hint - tmp;
		}
		assert -1 <= lastOfs && lastOfs < ofs && ofs <= len;

		/*
		 * Now a[base+lastOfs] < key <= a[base+ofs], so key belongs somewhere
		 * to the right of lastOfs but no farther right than ofs. Do a binary
		 * search, with invariant a[base + lastOfs - 1] < key <= a[base + ofs].
		 */
		lastOfs ++;
		while(lastOfs < ofs){
			final int m = lastOfs + ((ofs - lastOfs) >>> 1);
			if(comparator.compare(key, array[base + m]) > 0)
				//a[base + m] < key
				lastOfs = m + 1;
			else
				//key <= a[base + m]
				ofs = m;
		}
		//so a[base + ofs - 1] < key <= a[base + ofs]
		assert lastOfs == ofs;

		return ofs;
	}

	/**
	 * Like gallopLeft, except that if the range contains an element equal to
	 * key, gallopRight returns the index after the rightmost equal element.
	 *
	 * @param key	the key whose insertion point to search for
	 * @param array	the array in which to search
	 * @param base	the index of the first element in the range
	 * @param len 	the length of the range; must be > 0
	 * @param hint	the index at which to begin the search, 0 <= hint < n.
	 *		The closer hint is to the result, the faster this method will run.
	 * @param comparator	the comparator used to order the range, and to search
	 * @return	the int k, 0 <= k <= n such that a[b + k - 1] <= key < a[b + k]
	 */
	private static <T> int gallopRight(final T key, final T[] array, final int base, final int len, final int hint,
			final Comparator<? super T> comparator){
		assert hint >= 0 && hint < len;

		int ofs = 1;
		int lastOfs = 0;
		if(comparator.compare(key, array[base + hint]) < 0){
			//gallop left until a[b+hint - ofs] <= key < a[b+hint - lastOfs]
			final int maxOfs = hint + 1;
			while(ofs < maxOfs && comparator.compare(key, array[base + hint - ofs]) < 0){
				lastOfs = ofs;
				ofs = (ofs << 1) + 1;
				//int overflow
				if(ofs <= 0)
					ofs = maxOfs;
			}
			if(ofs > maxOfs)
				ofs = maxOfs;

			//make offsets relative to b
			final int tmp = lastOfs;
			lastOfs = hint - ofs;
			ofs = hint - tmp;
		}
		//a[b + hint] <= key
		else{
			//gallop right until a[b+hint + lastOfs] <= key < a[b+hint + ofs]
			final int maxOfs = len - hint;
			while(ofs < maxOfs && comparator.compare(key, array[base + hint + ofs]) >= 0){
				lastOfs = ofs;
				ofs = (ofs << 1) + 1;
				// int overflow
				if(ofs <= 0)
					ofs = maxOfs;
			}
			if(ofs > maxOfs)
				ofs = maxOfs;

			//make offsets relative to b
			lastOfs += hint;
			ofs += hint;
		}
		assert -1 <= lastOfs && lastOfs < ofs && ofs <= len;

		/*
		 * Now a[b + lastOfs] <= key < a[b + ofs], so key belongs somewhere to
		 * the right of lastOfs but no farther right than ofs. Do a binary
		 * search, with invariant a[b + lastOfs - 1] <= key < a[b + ofs].
		 */
		lastOfs ++;
		while(lastOfs < ofs){
			final int m = lastOfs + ((ofs - lastOfs) >>> 1);
			if(comparator.compare(key, array[base + m]) < 0)
				//key < a[b + m]
				ofs = m;
			else
				//a[b + m] <= key
				lastOfs = m + 1;
		}
		//so a[b + ofs - 1] <= key < a[b + ofs]
		assert lastOfs == ofs;

		return ofs;
	}

	/**
	 * Merges two adjacent runs in place, in a stable fashion. The first
	 * element of the first run must be greater than the first element of the
	 * second run (a[base1] > a[base2]), and the last element of the first run
	 * (a[base1 + len1-1]) must be greater than all elements of the second run.
	 * <p>
	 * For performance, this method should be called only when len1 <= len2;
	 * its twin, mergeHi should be called if len1 >= len2. (Either method
	 * may be called if len1 == len2.)
	 *
	 * @param base1	index of first element in first run to be merged
	 * @param len1	length of first run to be merged (must be > 0)
	 * @param base2	index of first element in second run to be merged (must be aBase + aLen)
	 * @param len2	length of second run to be merged (must be > 0)
	 */
	private void mergeLow(final int base1, int len1, final int base2, int len2){
		assert len1 > 0 && len2 > 0 && base1 + len1 == base2;

		//copy first run into temp array
		final T[] a = this.array;	//for performance
		final T[] tmp = ensureCapacity(len1);
		System.arraycopy(a, base1, tmp, 0, len1);

		//indexes into tmp array
		int cursor1 = 0;
		//indexes int a
		int cursor2 = base2;
		//indexes int a
		int destination = base1;

		//move first element of second run and deal with degenerate cases
		a[destination ++] = a[cursor2 ++];
		if(-- len2 == 0){
			System.arraycopy(tmp, cursor1, a, destination, len1);
			return;
		}
		if(len1 == 1){
			System.arraycopy(a, cursor2, a, destination, len2);
			//last elt of run 1 to end of merge
			a[destination + len2] = tmp[cursor1];
			return;
		}

		//use local variable for performance
		final Comparator<? super T> comparator = this.comparator;
		//use local variable for performance
		int minGallop = this.minGallop;
		outer:
		while(true){
			//number of times in a row that first run won
			int count1 = 0;
			//number of times in a row that second run won
			int count2 = 0;

			//do the straightforward thing until (if ever) one run starts winning consistently
			do{
				assert len1 > 1 && len2 > 0;

				if(comparator.compare(a[cursor2], tmp[cursor1]) < 0){
					a[destination ++] = a[cursor2 ++];
					count2 ++;
					count1 = 0;
					if(-- len2 == 0)
						break outer;
				}
				else{
					a[destination ++] = tmp[cursor1 ++];
					count1 ++;
					count2 = 0;
					if(-- len1 == 1)
						break outer;
				}
			}while((count1 | count2) < minGallop);

			/*
			 * One run is winning so consistently that galloping may be a
			 * huge win. So try that, and continue galloping until (if ever)
			 * neither run appears to be winning consistently anymore.
			 */
			do{
				assert len1 > 1 && len2 > 0;

				count1 = gallopRight(a[cursor2], tmp, cursor1, len1, 0, comparator);
				if(count1 != 0){
					System.arraycopy(tmp, cursor1, a, destination, count1);
					destination += count1;
					cursor1 += count1;
					len1 -= count1;
					//len1 == 1 || len1 == 0
					if(len1 <= 1)
						break outer;
				}
				a[destination ++] = a[cursor2 ++];
				if(-- len2 == 0)
					break outer;

				count2 = gallopLeft(tmp[cursor1], a, cursor2, len2, 0, comparator);
				if(count2 != 0){
					System.arraycopy(a, cursor2, a, destination, count2);
					destination += count2;
					cursor2 += count2;
					len2 -= count2;
					if(len2 == 0)
						break outer;
				}
				a[destination ++] = tmp[cursor1 ++];
				if(-- len1 == 1)
					break outer;
				minGallop --;
			}while(count1 >= MIN_GALLOP | count2 >= MIN_GALLOP);
			if(minGallop < 0)
				minGallop = 0;
			//penalize for leaving gallop mode
			minGallop += 2;
		}
		//write back to field
		this.minGallop = Math.max(minGallop, 1);

		if(len1 == 1){
			assert len2 > 0;
			System.arraycopy(a, cursor2, a, destination, len2);
			//last elt of run 1 to end of merge
			a[destination + len2] = tmp[cursor1];
		}
		else if(len1 == 0)
			throw new IllegalArgumentException("Comparison method violates its general contract!");
		else{
			assert len2 == 0;
			System.arraycopy(tmp, cursor1, a, destination, len1);
		}
	}

	/**
	 * Like mergeLow, except that this method should be called only if
	 * len1 >= len2; mergeLo should be called if len1 <= len2. (Either method
	 * may be called if len1 == len2.)
	 *
	 * @param base1	index of first element in first run to be merged
	 * @param len1	length of first run to be merged (must be > 0)
	 * @param base2	index of first element in second run to be merged (must be aBase + aLen)
	 * @param len2	length of second run to be merged (must be > 0)
	 */
	private void mergeHigh(final int base1, int len1, final int base2, int len2){
		assert len1 > 0 && len2 > 0 && base1 + len1 == base2;

		//copy second run into temp array
		final T[] a = this.array;	//for performance
		final T[] tmp = ensureCapacity(len2);
		System.arraycopy(a, base2, tmp, 0, len2);

		//indexes into a
		int cursor1 = base1 + len1 - 1;
		//indexes into tmp array
		int cursor2 = len2 - 1;
		//indexes into a
		int destination = base2 + len2 - 1;

		//move last element of first run and deal with degenerate cases
		a[destination --] = a[cursor1 --];
		if(-- len1 == 0){
			System.arraycopy(tmp, 0, a, destination - (len2 - 1), len2);
			return;
		}
		if(len2 == 1){
			destination -= len1;
			cursor1 -= len1;
			System.arraycopy(a, cursor1 + 1, a, destination + 1, len1);
			a[destination] = tmp[cursor2];
			return;
		}

		//use local variable for performance
		final Comparator<? super T> comparator = this.comparator;
		//use local variable for performance
		int minGallop = this.minGallop;
		outer:
		while(true){
			//number of times in a row that first run won
			int count1 = 0;
			//number of times in a row that second run won
			int count2 = 0;

			//do the straightforward thing until (if ever) one run appears to win consistently
			do{
				assert len1 > 0 && len2 > 1;

				if(comparator.compare(tmp[cursor2], a[cursor1]) < 0){
					a[destination --] = a[cursor1 --];
					count1 ++;
					count2 = 0;
					if(-- len1 == 0)
						break outer;
				}
				else{
					a[destination --] = tmp[cursor2 --];
					count2 ++;
					count1 = 0;
					if(-- len2 == 1)
						break outer;
				}
			}while((count1 | count2) < minGallop);

			/*
			 * One run is winning so consistently that galloping may be a
			 * huge win. So try that, and continue galloping until (if ever)
			 * neither run appears to be winning consistently anymore.
			 */
			do{
				assert len1 > 0 && len2 > 1;
				count1 = len1 - gallopRight(tmp[cursor2], a, base1, len1, len1 - 1, comparator);
				if(count1 != 0){
					destination -= count1;
					cursor1 -= count1;
					len1 -= count1;
					System.arraycopy(a, cursor1 + 1, a, destination + 1, count1);
					if(len1 == 0)
						break outer;
				}
				a[destination --] = tmp[cursor2 --];
				if(-- len2 == 1)
					break outer;

				count2 = len2 - gallopLeft(a[cursor1], tmp, 0, len2, len2 - 1, comparator);
				if(count2 != 0){
					destination -= count2;
					cursor2 -= count2;
					len2 -= count2;
					System.arraycopy(tmp, cursor2 + 1, a, destination + 1, count2);
					//len2 == 1 || len2 == 0
					if(len2 <= 1)
						break outer;
				}
				a[destination --] = a[cursor1 --];
				if(-- len1 == 0)
					break outer;
				minGallop --;
			}while(count1 >= MIN_GALLOP | count2 >= MIN_GALLOP);
			if(minGallop < 0)
				minGallop = 0;
			//penalize for leaving gallop mode
			minGallop += 2;
		}
		//write back to field
		this.minGallop = Math.max(minGallop, 1);

		if(len2 == 1){
			assert len1 > 0;
			destination -= len1;
			cursor1 -= len1;
			System.arraycopy(a, cursor1 + 1, a, destination + 1, len1);
			//move first elt of run2 to front of merge
			a[destination] = tmp[cursor2];
		}
		else if(len2 == 0)
			throw new IllegalArgumentException("Comparison method violates its general contract!");
		else{
			assert len1 == 0;
			assert len2 > 0;
			System.arraycopy(tmp, 0, a, destination - (len2 - 1), len2);
		}
	}

	/**
	 * Ensures that the external array tmp has at least the specified
	 * number of elements, increasing its size if necessary. The size
	 * increases exponentially to ensure amortized linear time complexity.
	 *
	 * @param minCapacity the minimum required capacity of the tmp array
	 * @return tmp, whether or not it grew
	 */
	private T[] ensureCapacity(final int minCapacity){
		if(tmp.length < minCapacity){
			//compute smallest power of 2 > minCapacity
			int newSize = minCapacity;
			newSize |= newSize >> 1;
			newSize |= newSize >> 2;
			newSize |= newSize >> 4;
			newSize |= newSize >> 8;
			newSize |= newSize >> 16;
			newSize++;

			//not bloody likely!
			if(newSize < 0)
				newSize = minCapacity;
			else
				newSize = Math.min(newSize, array.length >>> 1);

			@SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
			final T[] newArray = (T[])new Object[newSize];
			tmp = newArray;
		}
		return tmp;
	}

	/**
	 * Checks that fromIndex and toIndex are in range, and throws an
	 * appropriate exception if they aren't.
	 *
	 * @param arrayLen	the length of the array
	 * @param fromIndex	the index of the first element of the range
	 * @param toIndex	the index after the last element of the range
	 * @throws IllegalArgumentException	if fromIndex > toIndex
	 * @throws ArrayIndexOutOfBoundsException	if fromIndex < 0 or toIndex > arrayLen
	 */
	private static void rangeCheck(final int arrayLen, final int fromIndex, final int toIndex){
		if(fromIndex > toIndex)
			throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
		if(fromIndex < 0)
			throw new ArrayIndexOutOfBoundsException(fromIndex);
		if(toIndex > arrayLen)
			throw new ArrayIndexOutOfBoundsException(toIndex);
	}

	/** Assume the array is already sorted! */
	public static <T> T[] removeDuplicates(final T[] array){
		return removeDuplicates(array, 0, array.length);
	}

	public static <T> T[] removeDuplicates(final T[] array, final int low, final int high){
		//fetch all the duplicates
		final Set<T> set = new HashSet<>();
		int[] indexes = new int[0];
		for(int i = low; i < high; i ++)
			if(set.add(array[i]))
				indexes = ArrayUtils.add(indexes, i);
		return ArrayUtils.removeAll(array, indexes);
	}

}
