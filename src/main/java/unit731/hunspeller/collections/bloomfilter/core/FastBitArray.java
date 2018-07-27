package unit731.hunspeller.collections.bloomfilter.core;



/**
 * A fast bit-set implementation that allows direct access to data property so that it can be easily serialized.
 */
public class FastBitArray implements BitArray{

	/** The data-set */
	private final long[] data;


	/**
	 * Construct an instance of the {@link FastBitArray} that can hold the given number of bits
	 *
	 * @param bits the number of bits this instance can hold
	 */
	public FastBitArray(long bits){
		if(bits <= 0)
			throw new IllegalArgumentException("Number of bits must be strict positive");

		data = new long[(int)(bits >>> 6) + 1];
	}

	public FastBitArray(long[] data){
		if(data == null || data.length == 0)
			throw new IllegalArgumentException("Data must be valued");

		this.data = data;
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
			return true;
		}
		return false;
	}

	@Override
	public void clear(int index){
		if(get(index))
			data[index >> 6] &= ~(1l << index);
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
	public int size(){
		return data.length * Long.SIZE;
	}

}
