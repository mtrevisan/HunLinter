package unit731.hunspeller.collections.bloomfilter.hash;

import java.util.zip.CRC32;


/**
 * A CRC32 hash function.
 */
public class CRC32HashFunction implements HashFunction{

	@Override
	public boolean isSingleValued(){
		return true;
	}

	@Override
	public long hash(final byte[] bytes){
		final CRC32 crc32 = new CRC32();
		crc32.update(bytes);
		return crc32.getValue();
	}

	@Override
	public long[] hashMultiple(final byte[] bytes){
		return new long[]{hash(bytes)};
	}

}
