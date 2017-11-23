package unit731.hunspeller.collections.bloomfilter.core;

import java.util.BitSet;


/**
 * A {@link BitArray} implementation that uses the standard Java {@link BitSet} as the underlying implementation.
 */
public class JavaBitArray implements BitArray{

	private final BitSet bitSet;


	public JavaBitArray(int bits){
		if(bits <= 0)
			throw new IllegalArgumentException("Number of bits must be strict positive");

		bitSet = new BitSet(bits);
	}

	public JavaBitArray(long[] data){
		if(data == null || data.length == 0)
			throw new IllegalArgumentException("Data must be valued");

		int size = data.length;
		bitSet = new BitSet(size);
		for(int i = 0; i < size; i ++)
			if(data[i] != 0)
				for(int j = 0; j < Long.SIZE; j ++)
					if((data[i] & (1l << j)) != 0l)
						bitSet.set(i);
	}

	@Override
	public boolean get(int index){
		return bitSet.get(index);
	}

	@Override
	public boolean set(int index){
		boolean previousBit = bitSet.get(index);
		if(!previousBit)
			bitSet.set(index);
		return !previousBit;
	}

	@Override
	public void clear(int index){
		bitSet.clear(index);
	}

	@Override
	public void clearAll(){
		bitSet.clear();
	}

	@Override
	public int bitSize(){
		return bitSet.size();
	}

}
