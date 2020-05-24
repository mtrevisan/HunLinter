package unit731.hunlinter.datastructures.dynamicarray;


class IntBlock{

	final int[] data;
	int limit;


	IntBlock(final int capacity){
		data = new int[capacity];
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
		limit --;
	}

}
