package unit731.hunlinter.services.sorters;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;


/**
 * Time complexity (best/average/worst): O(n * log(n))
 * Space complaxity: O(1)
 *
 * @see <a href="https://en.wikipedia.org/wiki/Heapsort">Heapsort</a>
 */
public class HeapSort{

	public static <T extends Comparable<? super T>> void sort(final T[] data, final Comparator<? super T> comparator){
		sort(data, 0, data.length, comparator);
	}

	public static <T extends Comparable<? super T>> void sort(final T[] data, final Comparator<? super T> comparator,
			final Consumer<Integer> progressCallback){
		sort(data, 0, data.length, comparator, progressCallback);
	}

	public static <T extends Comparable<? super T>> void sort(final T[] data, int low, final int high,
			final Comparator<? super T> comparator){
		sort(data, low, high, comparator, null);
	}

	public static synchronized <T extends Comparable<? super T>> void sort(final T[] data, int low, final int high,
			final Comparator<? super T> comparator, final Consumer<Integer> progressCallback){
		Objects.requireNonNull(data);
		Objects.requireNonNull(comparator);
		assert low < high && low < data.length && high <= data.length;

		final int progressStep = (int)Math.ceil((data.length << 1) / 100.f);
		if(progressCallback != null)
			progressCallback.accept(0);

		if(high - low < 2){
			if(progressCallback != null)
				progressCallback.accept(100);

			//arrays of size 0 and 1 are always sorted
			return;
		}

		buildMaxHeap(data, low, high, comparator, progressStep, progressCallback);

		//The following loop maintains the invariants that a[0:end] is a heap and every element
		//beyond `end` is greater than everything before it (so a[end:count] is in sorted order)
		int progress = data.length;
		int progressIndex = 50;
		for(int heapsize = high - 1; heapsize > low; heapsize --){
			//swap root value with last element
			swap(data, low, heapsize);

			//sift down:
			siftDown(data, low, heapsize, comparator);

			if(progressCallback != null && ++ progress % progressStep == 0)
				progressCallback.accept(++ progressIndex);
		}

		if(progressCallback != null)
			progressCallback.accept(100);
	}

	/** Build the heap in array a so that largest value is at the root */
	private static <T> void buildMaxHeap(final T[] data, final int low, final int high, final Comparator<? super T> comparator,
			final int progressStep, final Consumer<Integer> progressCallback){
		int progress = 0;
		for(int heapsize = low + 1; heapsize < high; heapsize ++){
			//if child is bigger than parent
			if(comparator.compare(data[heapsize], data[parent(heapsize)]) > 0){
				//swap child and parent until parent is smaller
				int node = heapsize;
				while(node > 0){
					final int parent = parent(node);
					if(comparator.compare(data[node], data[parent]) > 0)
						swap(data, node, parent);
					node = parent;
				}
			}

			if(progressCallback != null && (heapsize - low) % progressStep == 0)
				progressCallback.accept(++ progress);
		}
	}

	private static <T> void siftDown(final T[] data, final int low, final int heapsize, final Comparator<? super T> comparator){
		//index of the element being moved down the tree
		int parent = low;
		int leftChild;
		do{
			leftChild = 2 * parent + 1;

			//if left child is smaller than right child
			if(leftChild < heapsize - 1 && comparator.compare(data[leftChild], data[leftChild + 1]) < 0)
				//point index variable to right child
				leftChild ++;

			//if parent is smaller than left child, then swapping parent with left child
			if(leftChild < heapsize && comparator.compare(data[parent], data[leftChild]) < 0)
				swap(data, parent, leftChild);

			parent = leftChild;
		}while(leftChild < heapsize);
	}

	private static <T> void swap(final T[] data, final int i, final int j){
		final T temp = data[i];
		data[i] = data[j];
		data[j] = temp;
	}


	public static void sort(final byte[][] data, final Comparator<? super byte[]> comparator){
		sort(data, 0, data.length, comparator, null);
	}

	public static void sort(final byte[][] data, final Comparator<? super byte[]> comparator,
			final Consumer<Integer> progressCallback){
		sort(data, 0, data.length, comparator, progressCallback);
	}

	public static void sort(final byte[][] data, int low, final int high, final Comparator<? super byte[]> comparator){
		sort(data, low, high, comparator, null);
	}

	public static synchronized void sort(final byte[][] data, int low, final int high,
			final Comparator<? super byte[]> comparator, final Consumer<Integer> progressCallback){
		Objects.requireNonNull(data);
		Objects.requireNonNull(comparator);
		assert low < high && low < data.length && high <= data.length;

		final int progressStep = (int)Math.ceil((data.length << 1) / 100.f);
		if(progressCallback != null)
			progressCallback.accept(0);

		if(high - low < 2){
			if(progressCallback != null)
				progressCallback.accept(100);

			//arrays of size 0 and 1 are always sorted
			return;
		}

		buildMaxHeap(data, low, high, comparator, progressStep, progressCallback);

		//The following loop maintains the invariants that a[0:end] is a heap and every element
		//beyond `end` is greater than everything before it (so a[end:count] is in sorted order)
		int progress = data.length;
		int progressIndex = 50;
		for(int heapsize = high - 1; heapsize > low; heapsize --){
			//swap root value with last element
			swap(data, low, heapsize);

			//sift down:
			siftDown(data, low, heapsize, comparator);

			if(progressCallback != null && ++ progress % progressStep == 0)
				progressCallback.accept(++ progressIndex);
		}

		if(progressCallback != null)
			progressCallback.accept(100);
	}

	/** Build the heap in array a so that largest value is at the root */
	private static void buildMaxHeap(final byte[][] data, final int low, final int high,
			final Comparator<? super byte[]> comparator, final int progressStep, final Consumer<Integer> progressCallback){
		int progress = 0;
		for(int heapsize = low + 1; heapsize < high; heapsize ++){
			//if child is bigger than parent
			if(comparator.compare(data[heapsize], data[parent(heapsize)]) > 0){
				//swap child and parent until parent is smaller
				int node = heapsize;
				while(node > 0){
					final int parent = parent(node);
					if(comparator.compare(data[node], data[parent]) > 0)
						swap(data, node, parent);
					node = parent;
				}
			}

			if(progressCallback != null && (heapsize - low) % progressStep == 0)
				progressCallback.accept(++ progress);
		}
	}

	private static void siftDown(final byte[][] data, final int low, final int heapsize,
			final Comparator<? super byte[]> comparator){
		//index of the element being moved down the tree
		int parent = low;
		int leftChild;
		do{
			leftChild = 2 * parent + 1;

			//if left child is smaller than right child
			if(leftChild < heapsize - 1 && comparator.compare(data[leftChild], data[leftChild + 1]) < 0)
				//point index variable to right child
				leftChild ++;

			//if parent is smaller than left child, then swapping parent with left child
			if(leftChild < heapsize && comparator.compare(data[parent], data[leftChild]) < 0)
				swap(data, parent, leftChild);

			parent = leftChild;
		}while(leftChild < heapsize);
	}

	private static void swap(final byte[][] data, final int i, final int j){
		final byte[] temp = data[i];
		data[i] = data[j];
		data[j] = temp;
	}


	private static int parent(final int index){
		return (index - 1) >> 1;
	}

}
