package unit731.hunspeller.collections.bloomfilter.hash;

import com.sangupta.murmur.Murmur3;


/**
 * A Murmur3 hash function.
 */
public class Murmur3HashFunction implements HashFunction{

	private static final long SEED = 0x7F3A21EAl;


	@Override
	public boolean isSingleValued(){
		return false;
	}

	@Override
	public long hash(byte[] bytes){
		return Murmur3.hash_x86_32(bytes, 0, SEED);
	}

	@Override
	public long[] hashMultiple(byte[] bytes){
		return Murmur3.hash_x64_128(bytes, 0, SEED);
	}

}
