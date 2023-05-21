/**
 * Copyright (c) 2019-2022 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.datastructures.dynamicarray;

import java.util.RandomAccess;


/**
 * @see <a href="https://cs.uwaterloo.ca/~imunro/cs840/ResizableArrays.pdf">Resizable arrays in optimal time and space</a>
 * @see <a href="https://github.com/LHongy/DynamicArray">DynamicArray</a>
 */
public class DynamicIntArray implements RandomAccess{

	private static final int CAPACITY_DEFAULT = 4;

	private IntBlock[] blocks;
	//number of Blocks in `blocks`
	private int sizeOfBlocks;
	//number of elements in DynamicArray
	private int size;
	private int numberOfEmptyDataBlocks;
	private int indexOfLastNonEmptyDataBlock;
	private int indexOfLastDataBlock;

	//right-most SuperBlock
	private SuperBlock lastSuperBlock;


	public DynamicIntArray(){
		clear();
	}

	public final synchronized void clear(){
		blocks = new IntBlock[CAPACITY_DEFAULT];
		//the first Block, this is in SB0, so it can only have one element
		blocks[0] = new IntBlock(1);

		//SB0 has only one Block, and that Block can only have one element
		lastSuperBlock = SuperBlock.createEven(1, 1, 0);
		//SB0 contains a Block, incrementCurrentNumberOfDataBlocks
		lastSuperBlock.incrementCurrentNumberOfDataBlocks();

		sizeOfBlocks = 1;
		size = 0;
		numberOfEmptyDataBlocks = 1;
		indexOfLastNonEmptyDataBlock = -1;
		indexOfLastDataBlock = 0;
	}

	// Returns the element at position i in the DynamicArray.
	// Throws IllegalArgumentException if index < 0 or
	// index > size -1;
	// Target complexity: O(1)
	public final synchronized int get(final int i){
		// We need to find which Block contains the requested element, also in what position of that Block.
		// locate() gives us a Location object.
		// The object will contain the index of Block that we want,
		// and also the index of element in the Block.
		final Location location = new Location(i);
		// Use the blockIndex in the Location object to find the Block.
		final IntBlock block = blocks[location.block];
		// Use the elementIndex in the Location object to find the element within the Block.
		return block.data[location.element];
	}

	// Sets the value at position i in DynamicArray to x.
	// Throws IllegalArgumentException if index < 0 or
	// index > size -1;
	// Target complexity: O(1)
	public final synchronized void set(final int index, final int x){
		final Location location = new Location(index);
		final IntBlock block = blocks[location.block];
		//use the elementIndex in the Location object to set the element within the Block to x
		block.data[location.element] = x;
	}

	/**
	 * Allocates one more spaces in the DynamicArray.
	 * This may require the creation of a Block and the last SuperBlock may change.
	 * Also, expandArray is called if the `blocks` is full when a Block is created.
	 * Called by add.
	 * Target complexity: O(1)
	 */
	private void grow(){
		IntBlock lastDataBlock = blocks[indexOfLastDataBlock];

		//if the last Block is full, we need to make a new Block
		if(lastDataBlock.isFull()){
			if(sizeOfBlocks == blocks.length)
				//`blocks` is full, need to expand
				expandArray();

			//if the lastSuperBlock is full of Blocks, we need to create a new SuperBlock and increment numberOfSuperBlocks
			if(lastSuperBlock.isFull()){
				if(lastSuperBlock.isEven())
					// If the number of the current full lastSuperBlock is even,
					// The new SuperBlock will have the same MaxNumberOfDataBlocks as the old one,
					// but twice the MaxNumberOfElementsPerBlock.
					// This new superBlock currently has no Block in it.
					lastSuperBlock = SuperBlock.createOdd(lastSuperBlock.maxNumberOfDataBlocks, lastSuperBlock.maxNumberOfElementsPerBlock << 1, 0);
				else
					// If the number of the current full lastSuperBlock is not even,
					// The new SuperBlock will have the same MaxNumberOfElementsPerBlock as the old one,
					// but twice the MaxNumberOfDataBlocks.
					// This new superBlock currently has no Block in it.
					lastSuperBlock = SuperBlock.createEven(lastSuperBlock.maxNumberOfDataBlocks << 1,
						lastSuperBlock.maxNumberOfElementsPerBlock, 0);
			}

			// Create a new Block, use lastSuperBlock to figure out how many elements the Block can store.
			// Update the fields, also lastSuperBlock has one more Block in it, so incrementCurrentNumberOfDataBlocks.
			indexOfLastDataBlock ++;
			blocks[sizeOfBlocks ++] = new IntBlock(lastSuperBlock.maxNumberOfElementsPerBlock);
			numberOfEmptyDataBlocks ++;
			lastSuperBlock.incrementCurrentNumberOfDataBlocks();
			//since we create a new Block, we need to update variable lastDataBlock.
			lastDataBlock = blocks[indexOfLastDataBlock];
		}
		lastDataBlock.grow();
		if(numberOfEmptyDataBlocks == 1){
			// if numberOfEmptyDataBlocks = 1, it means our lastDataBlock was empty.
			// But it is supposed to not be empty now because we just grew the size of it.
			// So we have to update fields below.
			numberOfEmptyDataBlocks --;
			indexOfLastNonEmptyDataBlock ++;
		}

	}

	public final void push(final int x){
		add(x);
	}

	// Grows the DynamicArray by one space, increases the size of the
	// DynamicArray, and sets the last element to x.
	// Target complexity: O(1)
	public final synchronized void add(final int x){
		grow();

		size ++;
		set(size - 1, x);
	}

	public final void addAll(final DynamicIntArray array){
		for(int i = 0; i < array.size; i ++)
			add(array.get(i));
	}

	public final void addAll(final int[] array){
		for(final int value : array)
			add(value);
	}

	public final synchronized int pop(){
		final int elem = get(size - 1);
		remove();
		return elem;
	}

	public final synchronized void shrink(final int newSize){
		while(size > newSize)
			remove();
	}

	/**
	 * Write a null value to the last element, shrinks the DynamicArray by one space, and decreases the size of the DynamicArray.
	 * A Block may be deleted and the last SuperBlock may change.
	 * Also, shrinkArray is called if the `blocks` is less than or equal to a quarter full when a Block is deleted.
	 * Target complexity: O(1)
	 *
	 * @throws IllegalStateException	If the DynamicArray is empty when remove is called
	 */
	public final synchronized void remove(){
		final IntBlock lastNonEmptyDataBlock = blocks[indexOfLastNonEmptyDataBlock];
		lastNonEmptyDataBlock.shrink();
		size --;

		if(lastNonEmptyDataBlock.size() == 0){
			// The lastNonEmptyDataBlock is empty now after shrinking,
			// we have to update fields below.
			numberOfEmptyDataBlocks ++;
			indexOfLastNonEmptyDataBlock --;
		}

		//if we have two empty Blocks, we have to delete the last one.
		if(numberOfEmptyDataBlocks == 2){
			//set the last empty Block to null
			//-- sizeOfBlocks gives us the index of last Block, also decrement sizeOfBlocks
			blocks[-- sizeOfBlocks] = null;
			//update the fields, also `lastSuperBlock` has one less Block in it, so `decrementCurrentNumberOfDataBlocks`
			numberOfEmptyDataBlocks --;
			indexOfLastDataBlock --;
			lastSuperBlock.decrementCurrentNumberOfDataBlocks();

			//the length of `blocks` should never be less than 4
			if(sizeOfBlocks <= blocks.length / 4 && blocks.length > 4)
				//need to shrink
				shrinkArray();

			// If the lastSuperBlock has no Blocks in it,
			// we need to change lastSuperBlock to the previous superBlock and decrement numberOfSuperBlocks
			if(lastSuperBlock.isEmpty()){
				if(lastSuperBlock.isEven())
					// If the number of the current empty lastSuperBlock is even,
					// The previous SuperBlock will have one half of MaxNumberOfDataBlocks as the old one,
					// but the same MaxNumberOfElementsPerBlock.
					// This previous superBlock currently is full of Blocks.
					// --numberOfSuperBlocks - 1 gives us the number of this previous superBlock.
					lastSuperBlock = SuperBlock.createOdd(lastSuperBlock.maxNumberOfDataBlocks / 2,
						lastSuperBlock.maxNumberOfElementsPerBlock, lastSuperBlock.maxNumberOfDataBlocks / 2);
				else
					// If the number of the current empty lastSuperBlock is not even,
					// The previous SuperBlock will have one half of MaxNumberOfElementsPerBlock as the old one,
					// but the same MaxNumberOfDataBlocks.
					// This previous superBlock currently is full of Blocks.
					lastSuperBlock = SuperBlock.createEven(lastSuperBlock.maxNumberOfDataBlocks,
						lastSuperBlock.maxNumberOfElementsPerBlock / 2, lastSuperBlock.maxNumberOfDataBlocks);
			}
		}
	}

	/**
	 * Decreases the length of the `blocks` by half.
	 * Create a new `blocks` and copy the Blocks from the old one to this new array.
	 */
	private void shrinkArray(){
		final IntBlock[] newBlocks = new IntBlock[blocks.length / 2];
		if(sizeOfBlocks >= 0)
			System.arraycopy(blocks, 0, newBlocks, 0, sizeOfBlocks);
		blocks = newBlocks;
	}

	/**
	 * Doubles the length of the `blocks`.
	 * Create a new `blocks` and copy the Blocks from the old one to this new array.
	 */
	private void expandArray(){
		final IntBlock[] newBlocks = new IntBlock[(blocks.length << 1)];
		if(sizeOfBlocks >= 0)
			System.arraycopy(blocks, 0, newBlocks, 0, sizeOfBlocks);
		blocks = newBlocks;
	}

	// Returns the size of the DynamicArray which is the number of elements that
	// have been added to it with the add(x) method but not removed.
	public final synchronized int size(){
		return size;
	}

	public final synchronized boolean isEmpty(){
		return (size == 0);
	}

}
