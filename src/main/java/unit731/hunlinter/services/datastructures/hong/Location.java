package unit731.hunlinter.services.datastructures.hong;


class Location{

	private static final double LN2 = Math.log(2);

	final int block;
	final int element;


	Location(final int index){
		final int r = index + 1;
		final int k = (int)log2(r);
		block = computeP(k) + computeDataBlock(r, k);
		element = computeElement(r, k);
	}

	/** Helper method computing `p`, the number of data block in superblock prior to superblock `k` */
	private static int computeP(final int k){
		final int p;
		//for even k
		if(k % 2 == 0)
			//p = 2 * (2^floor(k/2) - 1)
			//k / 2 will give us floor(k / 2), 1 << k / 2 raise 2 to power floor(k / 2).
			p = 2 * ((1 << k / 2) - 1);
			//for odd k
		else
			//p = (2 * (2^floor(k/2) - 1)) + 2^floor(k/2)
			p = 2 * ((1 << k / 2) - 1) + (1 << k / 2);
		return p;
	}

	/** Helper method computing `b`, the floor(k/2) bits of `r` immediately after the leading 1-bit */
	private static int computeDataBlock(int r, final int k){
		//the value of `b` is given by the base 10 value of the floor(k/2) bits of `r` immediately after the leading 1 bit in `r`
		//k / 2 will give us floor(k / 2), this is the number of bits we want to capture
		final int numOfBitsToCapture = k / 2;
		//floor(log2 r) is the total number of bits immediately after the leading 1 bit in `r`,
		//and k = floor(log2 r)
		//k - numOfBitsToCapture gives us how many places we need to shift the bits to the right in `r`
		r = r >> (k - numOfBitsToCapture);
		//mask depends on how many bits we want to capture
		final int mask = mask(numOfBitsToCapture);
		return r & mask;
	}

	/** Helper method computing `e`, the last ceil(k/2) bits of `r` */
	private static int computeElement(final int r, final int k){
		//the value of `e` is given by the base 10 value of the last ceiling(k/2) bits of `r`.
		//we want to capture the last ceil(k / 2) bits of `r`
		//quickly compute ceil(k / 2)
		final int numOfBitsToCapture = (k + 1) >> 1;
		//mask depends on how many bits we want to capture
		final int mask = mask(numOfBitsToCapture);
		return r & mask;
	}

	/** Returns the log base 2 of n */
	private static double log2(final int n){
		return (Math.log(n) / LN2);
	}

	/** Returns a mask of `N` 1 bits */
	private static int mask(final int n){
		return (1 << n) - 1;
	}

}
