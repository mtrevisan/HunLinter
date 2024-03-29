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


final class SuperBlock{

	//as in S0, S1, etc
	private final boolean even;
	final int maxNumberOfDataBlocks;
	final int maxNumberOfElementsPerBlock;
	private int numberOfDataBlocks;


	public static SuperBlock createEven(final int maxNumberOfDataBlocks, final int maxNumberOfElementsPerBlock,
			final int numberOfDataBlocks){
		return new SuperBlock(true, maxNumberOfDataBlocks, maxNumberOfElementsPerBlock, numberOfDataBlocks);
	}

	public static SuperBlock createOdd(final int maxNumberOfDataBlocks, final int maxNumberOfElementsPerBlock,
			final int numberOfDataBlocks){
		return new SuperBlock(false, maxNumberOfDataBlocks, maxNumberOfElementsPerBlock, numberOfDataBlocks);
	}

	private SuperBlock(final boolean even, final int maxNumberOfDataBlocks, final int maxNumberOfElementsPerBlock,
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
