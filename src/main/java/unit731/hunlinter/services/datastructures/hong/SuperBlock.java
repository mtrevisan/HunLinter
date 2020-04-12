package unit731.hunlinter.services.datastructures.hong;


class SuperBlock{

	//as in S0, S1, etc
	private final boolean even;
	final int maxNumberOfDataBlocks;
	final int maxNumberOfElementsPerBlock;
	private int numberOfDataBlocks;


	SuperBlock(final boolean even, final int maxNumberOfDataBlocks, final int maxNumberOfElementsPerBlock,
			final int numberOfDataBlocks){
		this.even = even;
		this.maxNumberOfDataBlocks = maxNumberOfDataBlocks;
		this.maxNumberOfElementsPerBlock = maxNumberOfElementsPerBlock;
		this.numberOfDataBlocks = numberOfDataBlocks;
	}

	boolean isEven(){
		return even;
	}

	boolean isEmpty(){
		return (numberOfDataBlocks == 0);
	}

	boolean isFull(){
		return (numberOfDataBlocks == maxNumberOfDataBlocks);
	}

	void incrementCurrentNumberOfDataBlocks(){
		numberOfDataBlocks ++;
	}

	void decrementCurrentNumberOfDataBlocks(){
		numberOfDataBlocks --;
	}

}
