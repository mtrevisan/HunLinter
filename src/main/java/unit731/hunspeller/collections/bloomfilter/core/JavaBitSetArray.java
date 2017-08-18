package unit731.hunspeller.collections.bloomfilter.core;

import java.io.IOException;
import java.util.BitSet;
import java.util.Objects;


/**
 * A {@link BitArray} implementation that uses the standard Java {@link BitSet} as the underlying implementation.
 */
public class JavaBitSetArray implements BitArray{

	private final BitSet bitSet;


	public JavaBitSetArray(int numBits){
		if(numBits <= 0)
			throw new IllegalArgumentException("Number of bits must be strict positive");

		bitSet = new BitSet(numBits);
	}

	@Override
	public void clear(){
		bitSet.clear();
	}

	@Override
	public boolean getBit(int index){
		return bitSet.get(index);
	}

	@Override
	public boolean setBit(int index){
		boolean previousBit = bitSet.get(index);
		if(!previousBit)
			bitSet.set(index);

		return !previousBit;
	}

	@Override
	public void clearBit(int index){
		bitSet.clear(index);
	}

	@Override
	public boolean setBitIfUnset(int index){
		return (!bitSet.get(index) && setBit(index));
	}

	@Override
	public void or(BitArray bitArray){
		Objects.requireNonNull(bitArray, "BitArray cannot be null");
		if(bitSet.size() != bitArray.bitSize())
			throw new IllegalArgumentException("The size of the arrays mismatched");

		throw new RuntimeException("Operation not yet supported");
	}

	@Override
	public void and(BitArray bitArray){
		Objects.requireNonNull(bitArray, "BitArray cannot be null");
		if(bitSet.size() != bitArray.bitSize())
			throw new IllegalArgumentException("The size of the arrays mismatched");

		throw new RuntimeException("Operation not yet supported");
	}

	@Override
	public int bitSize(){
		return bitSet.size();
	}

	@Override
	public void close() throws IOException{
		//do nothing
	}

}
