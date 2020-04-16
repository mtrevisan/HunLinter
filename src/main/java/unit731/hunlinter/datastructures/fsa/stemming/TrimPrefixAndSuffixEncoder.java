package unit731.hunlinter.datastructures.fsa.stemming;


/**
 * Encodes <code>target</code> relative to <code>source</code> by trimming whatever
 * non-equal suffix and prefix <code>source</code> and <code>target</code> have. The
 * output code is (bytes):
 *
 * <pre>
 * {P}{K}{suffix}
 * </pre>
 *
 * where (<code>P</code> - 'A') bytes should be trimmed from the start of
 * <code>source</code>, (<code>K</code> - 'A') bytes should be trimmed from the
 * end of <code>source</code> and then the <code>suffix</code> should be appended
 * to the resulting byte sequence.
 *
 * <p>
 * Examples:
 * </p>
 *
 * <pre>
 * source: abc
 * target: abcd
 * encoded: AAd
 *
 * source: abc
 * target: xyz
 * encoded: ADxyz
 * </pre>
 *
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public class TrimPrefixAndSuffixEncoder implements SequenceEncoderInterface{

	@Override
	public byte[] encode(final byte[] source, final byte[] target){
		//search for the maximum matching subsequence that can be encoded
		int maxSubsequenceLength = 0;
		int maxSubsequenceIndex = 0;
		for(int i = 0; i < source.length; i ++){
			//prefix at i => shared subsequence (infix)
			final int sharedPrefix = BufferUtils.sharedPrefixLength(source, i, target, 0);
			//only update maxSubsequenceLength if we will be able to encode it
			if(sharedPrefix > maxSubsequenceLength && i < REMOVE_EVERYTHING
					&& (source.length - (i + sharedPrefix)) < REMOVE_EVERYTHING){
				maxSubsequenceLength = sharedPrefix;
				maxSubsequenceIndex = i;
			}
		}

		//determine how much to remove (and where) from `source` to get a prefix of `target`
		int truncatePrefixBytes = maxSubsequenceIndex;
		int truncateSuffixBytes = (source.length - (maxSubsequenceIndex + maxSubsequenceLength));
		if(truncatePrefixBytes >= REMOVE_EVERYTHING || truncateSuffixBytes >= REMOVE_EVERYTHING){
			maxSubsequenceLength = 0;
			truncatePrefixBytes = truncateSuffixBytes = REMOVE_EVERYTHING;
		}

		final int len1 = target.length - maxSubsequenceLength;
		final byte[] encoded = new byte[2 + len1];
		encoded[0] = (byte)((truncatePrefixBytes + 'A') & 0xFF);
		encoded[1] = (byte)((truncateSuffixBytes + 'A') & 0xFF);
		System.arraycopy(target, maxSubsequenceLength, encoded, 2, len1);
		return encoded;
	}

	@Override
	public byte[] decode(final byte[] source, final byte[] encoded){
		int truncatePrefixBytes = (encoded[0] - 'A') & 0xFF;
		int truncateSuffixBytes = (encoded[1] - 'A') & 0xFF;
		if(truncatePrefixBytes == REMOVE_EVERYTHING || truncateSuffixBytes == REMOVE_EVERYTHING){
			truncatePrefixBytes = source.length;
			truncateSuffixBytes = 0;
		}

		final int len1 = source.length - (truncateSuffixBytes + truncatePrefixBytes);
		final int len2 = encoded.length - 2;
		final byte[] decoded = new byte[len1 + len2];
		System.arraycopy(source, truncatePrefixBytes, decoded, 0, len1);
		System.arraycopy(encoded, 2, decoded, len1, len2);
		return decoded;
	}

	@Override
	public String toString(){
		return getClass().getSimpleName();
	}

}
