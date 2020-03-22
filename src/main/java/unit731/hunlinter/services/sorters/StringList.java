package unit731.hunlinter.services.sorters;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;


public class StringList extends AbstractList<String> implements List<String>{

	/**
	 * Shared empty array instance used for empty instances.
	 */
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

	/**
	 * The size of the ArrayList (the number of elements it contains).
	 *
	 * @serial
	 */
	private int size;


	/**
	 * Constructs an empty list with the specified initial capacity.
	 *
	 * @param  initialCapacity  the initial capacity of the list
	 * @throws IllegalArgumentException if the specified initial capacity
	 *         is negative
	 */
	public StringList(final int initialCapacity){
		if(initialCapacity > 0)
			elementData = new String[initialCapacity];
		else if(initialCapacity == 0)
			elementData = EMPTY_ELEMENTDATA;

		throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
	}

	public StringList() {
		this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
	}

	/**
	 * Trims the capacity of this {@code ArrayList} instance to be the
	 * list's current size.  An application can use this operation to minimize
	 * the storage of an {@code ArrayList} instance.
	 */
	public void trimToSize(){
		modCount ++;
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
		if(minCapacity > elementData.length && (elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA || minCapacity > 0)){
			modCount ++;
			grow(minCapacity);
		}
	}

	private String[] grow(){
		return grow(size + 1);
	}

	/**
	 * Increases the capacity to ensure that it can hold at least the
	 * number of elements specified by the minimum capacity argument.
	 *
	 * @param minCapacity the desired minimum capacity
	 * @throws OutOfMemoryError if minCapacity is less than zero
	 */
	private String[] grow(final int minCapacity){
		final int oldCapacity = elementData.length;
		if(oldCapacity > 0 || elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA){
			final int newCapacity = newLength(oldCapacity, minCapacity - oldCapacity,
				oldCapacity >> 1);
			elementData = Arrays.copyOf(elementData, newCapacity);
		}
		else
			elementData = new String[minCapacity];
		return elementData;
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
	 * @param minGrowth   minimum required growth of the array length (must be
	 *                    positive)
	 * @param prefGrowth  preferred growth of the array length (ignored, if less
	 *                    then {@code minGrowth})
	 * @return the new length of the array
	 * @throws OutOfMemoryError if increasing {@code oldLength} by
	 *                    {@code minGrowth} overflows.
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
	@Override
	public int size(){
		return size;
	}

	/**
	 * Returns {@code true} if this list contains no elements.
	 *
	 * @return {@code true} if this list contains no elements
	 */
	@Override
	public boolean isEmpty(){
		return (size == 0);
	}

	public String[] getArray(){
		return elementData;
	}

	@Override
	public String get(final int index){
		Objects.checkIndex(index, size);

		return elementData[index];
	}

	/**
	 * Replaces the element at the specified position in this list with
	 * the specified element.
	 *
	 * @param index index of the element to replace
	 * @param element element to be stored at the specified position
	 * @return the element previously at the specified position
	 * @throws IndexOutOfBoundsException {@inheritDoc}
	 */
	@Override
	public String set(final int index, final String element){
		Objects.checkIndex(index, size);

		final String oldValue = elementData[index];
		elementData[index] = element;
		return oldValue;
	}

	/**
	 * Appends the specified element to the end of this list.
	 *
	 * @param e element to be appended to this list
	 * @return {@code true} (as specified by {@link Collection#add})
	 */
	public boolean add(final String e){
		modCount ++;

		if(size == elementData.length)
			elementData = grow();
		elementData[size] = e;
		size = size + 1;

		return true;
	}

	/**
	 * Removes the element at the specified position in this list.
	 * Shifts any subsequent elements to the left (subtracts one from their
	 * indices).
	 *
	 * @param index the index of the element to be removed
	 * @return the element that was removed from the list
	 * @throws IndexOutOfBoundsException {@inheritDoc}
	 */
	public String remove(final int index){
		Objects.checkIndex(index, size);

		final String oldValue = elementData[index];

		modCount ++;

		final int newSize = size - 1;
		if(newSize > index)
			System.arraycopy(elementData, index + 1, elementData, index, newSize - index);
		elementData[newSize] = null;
		size = newSize;

		return oldValue;
	}

	/**
	 * Removes all of the elements from this list.  The list will
	 * be empty after this call returns.
	 */
	public void clear(){
		modCount ++;
		for(int to = size, i = size = 0; i < to; i ++)
			elementData[i] = null;
	}


	/**
	 * Returns an iterator over the elements in this list in proper sequence.
	 *
	 * <p>The returned iterator is <a href="#fail-fast"><i>fail-fast</i></a>.
	 *
	 * @return an iterator over the elements in this list in proper sequence
	 */
	public Iterator<String> iterator(){
		return new StringList.Itr();
	}

	/**
	 * An optimized version of AbstractList.Itr
	 */
	private class Itr implements Iterator<String>{
		int cursor;       // index of next element to return
		int lastRet = -1; // index of last element returned; -1 if no such

		int expectedModCount = modCount;

		// prevent creating a synthetic constructor
		Itr(){}

		public boolean hasNext(){
			return cursor != size;
		}

		public String next(){
			checkForComodification();

			int i = cursor;
			if(i >= size)
				throw new NoSuchElementException();

			final String[] elementData = StringList.this.elementData;
			if(i >= elementData.length)
				throw new ConcurrentModificationException();

			cursor = i + 1;
			return elementData[lastRet = i];
		}

		public void remove(){
			if(lastRet < 0)
				throw new IllegalStateException();

			checkForComodification();

			try{
				StringList.this.remove(lastRet);
				cursor = lastRet;
				lastRet = -1;

				expectedModCount = modCount;
			}
			catch(final IndexOutOfBoundsException ex){
				throw new ConcurrentModificationException();
			}
		}

		final void checkForComodification(){
			if(modCount != expectedModCount)
				throw new ConcurrentModificationException();
		}
	}


	/**
	 * @throws NullPointerException {@inheritDoc}
	 */
	@Override
	public void forEach(final Consumer<? super String> action){
		Objects.requireNonNull(action);

		final int expectedModCount = modCount;

		final int size = this.size;
		for(int i = 0; modCount == expectedModCount && i < size; i ++)
			action.accept(elementData[i]);

		if(modCount != expectedModCount)
			throw new ConcurrentModificationException();
	}

	@Override
	public void sort(final Comparator<? super String> c){
		final int expectedModCount = modCount;

		TimSort.sort(elementData, 0, size, c);

		if(modCount != expectedModCount)
			throw new ConcurrentModificationException();

		modCount ++;
	}

	/** Assume the list is already sorted! */
	public void removeDuplicates(){
		final int expectedModCount = modCount;

		TimSort.removeDuplicates(elementData);

		if(modCount != expectedModCount)
			throw new ConcurrentModificationException();

		modCount ++;
	}

}
