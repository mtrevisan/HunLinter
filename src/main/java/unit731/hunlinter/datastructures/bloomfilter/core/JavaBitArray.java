package unit731.hunlinter.datastructures.bloomfilter.core;


/**
 * A fast bit-set implementation that allows direct access to data property so that it can be easily serialized.
 */
public class JavaBitArray implements BitArray{

	private static final String WRONG_NUMBER_OF_BITS = "Number of bits must be strictly positive";


	/** The data-set */
	private final long[] data;


	/**
	 * Construct an instance of the {@link JavaBitArray} that can hold the given number of bits
	 *
	 * @param bits the number of bits this instance can hold
	 */
	public JavaBitArray(final long bits){
		if(bits <= 0)
			throw new IllegalArgumentException(WRONG_NUMBER_OF_BITS);

		data = new long[(int)(bits >>> 6) + 1];
	}

	@Override
	public boolean get(final int index){
		return ((data[index >> 6] & (1l << index)) != 0l);
	}

	/** Returns true if the bit changed value. */
	@Override
	public boolean set(final int index){
		if(!get(index)){
			data[index >> 6] |= (1l << index);
			return true;
		}
		return false;
	}

	@Override
	public void clear(final int index){
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
