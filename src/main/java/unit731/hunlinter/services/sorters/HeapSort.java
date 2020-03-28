package unit731.hunlinter.services.sorters;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


/**
 * Time complexity (best/average/worst): O(n * log(n))
 * Space complaxity: O(1)
 *
 * @see <a href="https://en.wikipedia.org/wiki/Heapsort">Heapsort</a>
 */
public class HeapSort{

	public static <T extends Comparable<T>> void sort(final T[] array, final Comparator<? super T> comparator){
		sort(array, 0, array.length, comparator);
	}

	public static <T extends Comparable<T>> void sort(final T[] array, int low, final int high,
			final Comparator<? super T> comparator){
		Objects.requireNonNull(array);
		Objects.requireNonNull(comparator);
		assert low < high && low < array.length && high <= array.length;

		if(high - low < 2)
			//arrays of size 0 and 1 are always sorted
			return;

		buildMaxHeap(array, low, high, comparator);

		//The following loop maintains the invariants that a[0:end] is a heap and every element
		//beyond `end` is greater than everything before it (so a[end:count] is in sorted order)
		for(int heapsize = high - 1; heapsize > low; heapsize --){
			//swap root value with last element
			swap(array, low, heapsize);

			//sift down:
			siftDown(array, low, heapsize, comparator);
		}
	}

	/** Build the heap in array a so that largest value is at the root */
	private static <T> void buildMaxHeap(final T[] array, final int low, final int high, final Comparator<? super T> comparator){
		for(int heapsize = low + 1; heapsize < high; heapsize ++)
			//if child is bigger than parent
			if(comparator.compare(array[heapsize], array[parent(heapsize)]) > 0){
				//swap child and parent until parent is smaller
				int node = heapsize;
				while(node > 0){
					final int parent = parent(node);
					if(comparator.compare(array[node], array[parent]) > 0)
						swap(array, node, parent);
					node = parent;
				}
			}
	}

	private static <T> void siftDown(final T[] array, final int low, final int heapsize, final Comparator<? super T> comparator){
		//index of the element being moved down the tree
		int parent = low;
		int leftChild;
		do{
			leftChild = 2 * parent + 1;

			//if left child is smaller than right child
			if(leftChild < heapsize - 1 && comparator.compare(array[leftChild], array[leftChild + 1]) < 0)
				//point index variable to right child
				leftChild ++;

			//if parent is smaller than left child, then swapping parent with left child
			if(leftChild < heapsize && comparator.compare(array[parent], array[leftChild]) < 0)
				swap(array, parent, leftChild);

			parent = leftChild;
		}while(leftChild < heapsize);
	}

	private static <T> void swap(final T[] array, final int i, final int j){
		final T temp = array[i];
		array[i] = array[j];
		array[j] = temp;
	}


	public static void sort(final List<byte[]> array, final Comparator<? super byte[]> comparator){
		sort(array, 0, array.size(), comparator, null);
	}

	public static void sort(final List<byte[]> array, final Comparator<? super byte[]> comparator,
			final BiConsumer<Integer, Integer> progressCallback){
		sort(array, 0, array.size(), comparator, progressCallback);
	}

	public static void sort(final List<byte[]> array, int low, final int high, final Comparator<? super byte[]> comparator){
		sort(array, low, high, comparator, null);
	}

	public static void sort(final List<byte[]> array, int low, final int high, final Comparator<? super byte[]> comparator,
			final BiConsumer<Integer, Integer> progressCallback){
		Objects.requireNonNull(array);
		Objects.requireNonNull(comparator);
		assert low < high && low < array.size() && high <= array.size();

		final int maxProgress = array.size() << 1;
		final Consumer<Integer> callback = (progressCallback != null?
			index -> progressCallback.accept(index, maxProgress):
			null);
		if(callback != null)
			callback.accept(0);

		if(high - low < 2){
			if(callback != null)
				callback.accept(maxProgress);

			//arrays of size 0 and 1 are always sorted
			return;
		}

		buildMaxHeap(array, low, high, comparator, callback);

		//The following loop maintains the invariants that a[0:end] is a heap and every element
		//beyond `end` is greater than everything before it (so a[end:count] is in sorted order)
		int progressIndex = maxProgress >>> 1;
		for(int heapsize = high - 1; heapsize > low; heapsize --){
			//swap root value with last element
			swap(array, low, heapsize);

			//sift down:
			siftDown(array, low, heapsize, comparator);

			if(callback != null)
				callback.accept(++ progressIndex);
		}
	}

	/** Build the heap in array a so that largest value is at the root */
	private static void buildMaxHeap(final List<byte[]> array, final int low, final int high,
			final Comparator<? super byte[]> comparator, final Consumer<Integer> progressCallback){
		int progressIndex = 0;
		for(int heapsize = low + 1; heapsize < high; heapsize ++){
			//if child is bigger than parent
			if(comparator.compare(array.get(heapsize), array.get(parent(heapsize))) > 0){
				//swap child and parent until parent is smaller
				int node = heapsize;
				while(node > 0){
					final int parent = parent(node);
					if(comparator.compare(array.get(node), array.get(parent)) > 0)
						swap(array, node, parent);
					node = parent;
				}
			}

			if(progressCallback != null)
				progressCallback.accept(++ progressIndex);
		}
	}

	private static void siftDown(final List<byte[]> array, final int low, final int heapsize,
			final Comparator<? super byte[]> comparator){
		//index of the element being moved down the tree
		int parent = low;
		int leftChild;
		do{
			leftChild = 2 * parent + 1;

			//if left child is smaller than right child
			if(leftChild < heapsize - 1 && comparator.compare(array.get(leftChild), array.get(leftChild + 1)) < 0)
				//point index variable to right child
				leftChild ++;

			//if parent is smaller than left child, then swapping parent with left child
			if(leftChild < heapsize && comparator.compare(array.get(parent), array.get(leftChild)) < 0)
				swap(array, parent, leftChild);

			parent = leftChild;
		}while(leftChild < heapsize);
	}

	private static void swap(final List<byte[]> array, final int i, final int j){
		final byte[] temp = array.get(i);
		array.set(i, array.get(j));
		array.set(j, temp);
	}


	private static int parent(final int index){
		return (index - 1) >> 1;
	}

}
