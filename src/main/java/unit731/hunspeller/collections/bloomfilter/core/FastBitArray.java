package unit731.hunspeller.collections.bloomfilter.core;

import lombok.Getter;


/**
 * A fast bit-set implementation that allows direct access to data property so that it can be easily serialized.
 */
public class FastBitArray implements BitArray{

	/** The data-set */
	private final long[] data;

	/** The current bit count */
	@Getter private int bitCount;


	/**
	 * Construct an instance of the {@link FastBitArray} that can hold the given number of bits
	 *
	 * @param bits the number of bits this instance can hold
	 */
	public FastBitArray(long bits){
		if(bits <= 0)
			throw new IllegalArgumentException("Number of bits must be strict positive");

		data = new long[(int)(bits >>> 6) + 1];
		bitCount = 0;
	}

	public FastBitArray(long[] data){
		if(data == null || data.length == 0)
			throw new IllegalArgumentException("Data must be valued");

		this.data = data;
		bitCount = 0;
		for(long value : data)
			bitCount += Long.bitCount(value);
	}

	@Override
	public boolean get(int index){
		return ((data[index >> 6] & (1l << index)) != 0l);
	}

	/** Returns true if the bit changed value. */
	@Override
	public boolean set(int index){
		if(!get(index)){
			data[index >> 6] |= (1l << index);
			bitCount ++;
			return true;
		}
		return false;
	}

	@Override
	public void clear(int index){
		if(get(index)){
			data[index >> 6] &= ~(1l << index);
			bitCount --;
		}
	}

	@Override
	public void clearAll(){
		int size = data.length;
		while(size > 0)
			data[-- size] = 0l;
	}

	/**
	 * Number of bits
	 *
	 * @return total number of bits allocated
	 */
	@Override
	public int bitSize(){
		return data.length * Long.SIZE;
	}

}
