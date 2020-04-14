package unit731.hunlinter.datastructures.dynamicarray;


class Block<T>{

	final T[] data;
	int limit;


	Block(final int capacity){
		//noinspection unchecked
		data = (T[])new Object[capacity];
	}

	int size(){
		return limit;
	}

	boolean isFull(){
		return (limit == data.length);
	}

	/** Increase the space allocated for storing elements */
	void grow(){
		limit ++;
	}

	/** Set the last element to null and decrease the space allocated for storing elements */
	void shrink(){
		data[-- limit] = null;
	}

}
