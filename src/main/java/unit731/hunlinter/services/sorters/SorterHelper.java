package unit731.hunlinter.services.sorters;

import org.apache.commons.lang3.ArrayUtils;

import java.util.HashSet;
import java.util.Set;


public class SorterHelper{

	private SorterHelper(){}

	public static <T> void swap(final T[] data, final int i, final int j){
		final T temp = data[i];
		data[i] = data[j];
		data[j] = temp;
	}

	/* Assume the array is already sorted! */
	public static <T extends Comparable<? super T>> T[] removeDuplicates(final T[] array){
		return removeDuplicates(array, 0, array.length);
	}

	/* Assume the array is already sorted! */
	public static <T extends Comparable<? super T>> T[] removeDuplicates(final T[] array, final int low, final int high){
		//fetch all the duplicates
		final Set<T> set = new HashSet<>();
		int[] indexes = new int[0];
		for(int i = low; i < high; i ++)
			if(!set.add(array[i]))
				indexes = ArrayUtils.add(indexes, i);
		return ArrayUtils.removeAll(array, indexes);
	}

}
