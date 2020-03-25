package unit731.hunlinter.services.sorters;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;


/**
 * A fast and unsecure version of ArrayList&lt;String&gt;
 */
public class StringList implements Iterable<String>{

	/** Shared empty array instance used for empty instances */
	private static final String[] EMPTY_ELEMENTDATA = {};

	/**
	 * Shared empty array instance used for default sized empty instances. We
	 * distinguish this from EMPTY_ELEMENTDATA to know how much to inflate when
	 * first element is added.
	 */
	private static final String[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

	/**
	 * The maximum length of array to allocate (unless necessary).
	 * Some VMs reserve some header words in an array.
	 * Attempts to allocate larger arrays may result in
	 * {@code OutOfMemoryError: Requested array size exceeds VM limit}
	 */
	private static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;


	/**
	 * The array buffer into which the elements of the ArrayList are stored.
	 * The capacity of the ArrayList is the length of this array buffer. Any
	 * empty ArrayList with elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA
	 * will be expanded to DEFAULT_CAPACITY when the first element is added.
	 */
	transient String[] elementData;

	/** The size of the ArrayList (the number of elements it contains) */
	private int size;


	/**
	 * Constructs an empty list with the specified initial capacity.
	 *
	 * @param  initialCapacity  the initial capacity of the list
	 * @throws IllegalArgumentException if the specified initial capacity is negative
	 */
	public StringList(final int initialCapacity){
		if(initialCapacity > 0)
			elementData = new String[initialCapacity];
		else if(initialCapacity == 0)
			elementData = EMPTY_ELEMENTDATA;

		throw new IllegalArgumentException("Illegal capacity: " + initialCapacity);
	}

	public StringList(){
		this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
	}

	/**
	 * Trims the capacity of this {@code ArrayList} instance to be the
	 * list's current size.  An application can use this operation to minimize
	 * the storage of an {@code ArrayList} instance.
	 */
	public void trimToSize(){
		if(size < elementData.length)
			elementData = (size == 0? EMPTY_ELEMENTDATA: Arrays.copyOf(elementData, size));
	}

	/**
	 * Increases the capacity of this {@code ArrayList} instance, if
	 * necessary, to ensure that it can hold at least the number of elements
	 * specified by the minimum capacity argument.
	 *
	 * @param minCapacity the desired minimum capacity
	 */
	public void ensureCapacity(final int minCapacity){
		if(minCapacity > elementData.length)
			grow(minCapacity);
	}

	private void grow(){
		grow(size + 1);
	}

	/**
	 * Increases the capacity to ensure that it can hold at least the
	 * number of elements specified by the minimum capacity argument.
	 *
	 * @param minCapacity the desired minimum capacity
	 * @throws OutOfMemoryError if minCapacity is less than zero
	 */
	private void grow(final int minCapacity){
		final int oldCapacity = elementData.length;
		if(oldCapacity > 0 || elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA){
			final int newCapacity = newLength(oldCapacity, minCapacity - oldCapacity,
				oldCapacity >> 1);
			elementData = Arrays.copyOf(elementData, newCapacity);
		}
		else
			elementData = new String[minCapacity];
	}

	/**
	 * Calculates a new array length given an array's current length, a preferred
	 * growth value, and a minimum growth value.  If the preferred growth value
	 * is less than the minimum growth value, the minimum growth value is used in
	 * its place.  If the sum of the current length and the preferred growth
	 * value does not exceed {@link #MAX_ARRAY_LENGTH}, that sum is returned.
	 * If the sum of the current length and the minimum growth value does not
	 * exceed {@code MAX_ARRAY_LENGTH}, then {@code MAX_ARRAY_LENGTH} is returned.
	 * If the sum does not overflow an int, then {@code Integer.MAX_VALUE} is
	 * returned.  Otherwise, {@code OutOfMemoryError} is thrown.
	 *
	 * @param oldLength   current length of the array (must be non negative)
	 * @param minGrowth   minimum required growth of the array length (must be positive)
	 * @param prefGrowth  preferred growth of the array length (ignored, if less then {@code minGrowth})
	 * @return the new length of the array
	 * @throws OutOfMemoryError if increasing {@code oldLength} by {@code minGrowth} overflows.
	 */
	private int newLength(final int oldLength, final int minGrowth, final int prefGrowth){
		final int newLength = Math.max(minGrowth, prefGrowth) + oldLength;
		return (newLength - MAX_ARRAY_LENGTH <= 0? newLength: hugeLength(oldLength, minGrowth));
	}

	private int hugeLength(final int oldLength, final int minGrowth){
		final int minLength = oldLength + minGrowth;
		if(minLength < 0)
			throw new OutOfMemoryError("Required array length too large");

		return (minLength <= MAX_ARRAY_LENGTH? MAX_ARRAY_LENGTH: Integer.MAX_VALUE);
	}

	/**
	 * Returns the number of elements in this list.
	 *
	 * @return the number of elements in this list
	 */
	public int size(){
		return size;
	}

	public boolean isEmpty(){
		return (size == 0);
	}

	public String get(final int index){
		return elementData[index];
	}

	/**
	 * Replaces the element at the specified position in this list with
	 * the specified element.
	 *
	 * @param index index of the element to replace
	 * @param element element to be stored at the specified position
	 * @throws IndexOutOfBoundsException if the index is out of range ({@code index < 0 || index >= size()})
	 */
	public void set(final int index, final String element){
		elementData[index] = element;
	}

	/**
	 * Appends the specified element to the end of this list.
	 *
	 * @param elem element to be appended to this list
	 */
	public void add(final String elem){
		if(size == elementData.length)
			grow();

		elementData[size ++] = elem;
	}

	public void addAll(final Collection<? extends String> collection){
		if(!collection.isEmpty()){
			grow(size + collection.size());

			for(final String elem : collection)
				elementData[size ++] = elem;
		}
	}

	/**
	 * Removes the element at the specified position in this list.
	 * Shifts any subsequent elements to the left (subtracts one from their
	 * indices).
	 *
	 * @param index the index of the element to be removed
	 * @return the element that was removed from the list
	 * @throws IndexOutOfBoundsException if the index is out of range ({@code index < 0 || index >= size()})
	 */
	public String remove(final int index){
		final String oldValue = elementData[index];

		final int newSize = size - 1;
		if(newSize > index)
			System.arraycopy(elementData, index + 1, elementData, index, newSize - index);
		elementData[newSize] = null;
		size = newSize;

		return oldValue;
	}

	/**
	 * Removes all of the elements from this list.
	 * The list will be empty after this call returns.
	 */
	public void clear(){
		for(int i = 0; i < size; i ++)
			elementData[i] = null;
		size = 0;
	}


	/**
	 * Returns an iterator over the elements in this list in proper sequence.
	 *
	 * <p>The returned iterator is <a href="#fail-fast"><i>fail-fast</i></a>.
	 *
	 * @return an iterator over the elements in this list in proper sequence
	 */
	@Override
	public Iterator<String> iterator(){
		return new Itr();
	}

	/** An optimized version of AbstractList.Itr */
	private class Itr implements Iterator<String>{
		//index of next element to return
		int cursor;
		//index of last element returned; -1 if no such
		int lastReturnedIndex = -1;

		// prevent creating a synthetic constructor
		Itr(){}

		@Override
		public boolean hasNext(){
			return cursor != size;
		}

		@Override
		public String next(){
			if(cursor >= size)
				throw new NoSuchElementException();

			lastReturnedIndex = cursor;
			cursor ++;
			return StringList.this.elementData[lastReturnedIndex];
		}

		@Override
		public void remove(){
			if(lastReturnedIndex < 0)
				throw new IllegalStateException();

			try{
				StringList.this.remove(lastReturnedIndex);

				cursor = lastReturnedIndex;
				lastReturnedIndex = -1;
			}
			catch(final IndexOutOfBoundsException ex){
				throw new ConcurrentModificationException();
			}
		}
	}


	/**
	 * Performs the given action for each element of the {@code Iterable}
	 * until all elements have been processed or the action throws an
	 * exception.  Actions are performed in the order of iteration, if that
	 * order is specified.  Exceptions thrown by the action are relayed to the
	 * caller.
	 * <p>
	 * The behavior of this method is unspecified if the action performs
	 * side-effects that modify the underlying source of elements, unless an
	 * overriding class has specified a concurrent modification policy.
	 *
	 * @implSpec
	 * <p>The default implementation behaves as if:
	 * <pre>{@code
	 *     for (T t : this)
	 *         action.accept(t);
	 * }</pre>
	 *
	 * @param action The action to be performed for each element
	 * @throws NullPointerException if the specified action is null
	 */
	@Override
	public void forEach(final Consumer<? super String> action){
		for(int i = 0; i < size; i ++)
			action.accept(elementData[i]);
	}

//	public void sort(final Comparator<? super String> comparator){
//		TimSort.sort(elementData, 0, size, comparator);
//	}

	public void sortParallel(final Comparator<? super String> comparator){
		trimToSize();
		elementData = Arrays.stream(elementData).parallel()
			.sorted(comparator)
			.toArray(String[]::new);
	}

	/** Assume the list is already sorted! */
//	public void removeDuplicates(){
//		TimSort.removeDuplicates(elementData);
//	}

}
