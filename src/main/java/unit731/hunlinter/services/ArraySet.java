package unit731.hunlinter.services;

import org.apache.commons.lang3.ArrayUtils;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import static unit731.hunlinter.services.system.LoopHelper.match;


public class ArraySet<E> extends AbstractSet<E> implements Set<E>, Cloneable, Serializable{

	private class ArrayIterator implements Iterator<E>{
		//position of next item to return
		int offset = 0;

		public boolean hasNext(){
			return (offset < values.length);
		}

		public E next(){
			if(offset == values.length)
				throw new NoSuchElementException();

			//noinspection unchecked
			return (E)values[offset ++];
		}

		public void remove(){
			values = (values.length == 1? EMPTY_ARRAY: ArrayUtils.remove(values, -- offset));
		}
	}


	private static final Object[] EMPTY_ARRAY = new Object[0];


	private Object[] values = EMPTY_ARRAY;


	public ArraySet(){}

	@Override
	public Object[] toArray(){
		return values;
	}

	@Override
	@SuppressWarnings("MethodDoesntCallSuperMethod")
	public ArraySet<E> clone(){
		final ArraySet<E> ret = new ArraySet<>();
		ret.values = (values == EMPTY_ARRAY? EMPTY_ARRAY: values.clone());
		return ret;
	}

	@Override
	public void clear(){
		values = EMPTY_ARRAY;
	}

	@Override
	public boolean isEmpty(){
		return (values.length == 0);
	}

	@Override
	public Iterator<E> iterator(){
		return new ArrayIterator();
	}

	@Override
	public int size(){
		return values.length;
	}

	@Override
	public boolean add(final E value){
		int n = values.length;
		for(final Object o : values)
			if(o.equals(value))
				return false;

		final Object[] newValues = new Object[n + 1];
		System.arraycopy(values, 0, newValues, 0, n);
		newValues[n] = value;
		values = newValues;
		return true;
	}

	public boolean addAll(final E[] values){
		boolean modified = false;
		for(final E value : values)
			if(add(value))
				modified = true;
		return modified;
	}

	@Override
	public boolean addAll(final Collection<? extends E> values){
		boolean modified = false;
		for(final E value : values)
			if(add(value))
				modified = true;
		return modified;
	}

	@Override
	public boolean contains(final Object value){
		return (match(values, v -> v.equals(value)) != null);
	}

}
