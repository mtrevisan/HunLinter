package unit731.hunlinter.datastructures.fsa.stemming;


import unit731.hunlinter.services.text.ArrayHelper;


/**
 * Encodes <code>target</code> relative to <code>source</code> by trimming whatever
 * non-equal suffix and infix <code>source</code> and <code>target</code> have. The
 * output code is (bytes):
 *
 * <pre>
 * {X}{L}{K}{suffix}
 * </pre>
 *
 * where <code>source</code>'s infix at position (<code>X</code> - 'A') and of
 * length (<code>L</code> - 'A') should be removed, then (<code>K</code> -
 * 'A') bytes should be trimmed from the end and then the <code>suffix</code>
 * should be appended to the resulting byte sequence.
 *
 * <p>
 * Examples:
 * </p>
 *
 * <pre>
 * source:	ayz
 * target:	abc
 * encoded:	AACbc
 *
 * source:	aillent
 * target:	aller
 * encoded:	BBCr
 * </pre>
 *
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public class TrimInfixAndSuffixEncoder implements SequenceEncoderInterface{

	@Override
	public byte[] encode(final byte[] source, final byte[] target){
		//Search for the infix that can be encoded and remove from `source`
		//to get a maximum-length prefix of `target`. This could be done more efficiently
		//by running a smarter longest-common-subsequence algorithm and some pruning (?).
		//For now, naive loop should do.

		//There can be only two positions for the infix to delete:
		//	1) we remove leading bytes, even if they are partially matching (but a longer match exists somewhere later on)
		//	2) we leave maximum matching prefix and remove non-matching bytes that follow
		int maxInfixIndex = 0;
		int maxSubsequenceLength = ArrayHelper.longestCommonPrefix(source, target);
		int maxInfixLength = 0;
		for(final int i : new int[]{0, maxSubsequenceLength}){
			for(int j = 1; j <= source.length - i; j ++){
				//compute temporary `source` with the infix removed
				//concatenate in scratch space for simplicity
				final int len2 = source.length - (i + j);
				final byte[] scratch = new byte[i + len2];
				System.arraycopy(source, 0, scratch, 0, i);
				System.arraycopy(source, i + j, scratch, i, len2);

				final int sharedPrefix = ArrayHelper.longestCommonPrefix(scratch, target);

				//only update `maxSubsequenceLength` if we will be able to encode it
				if(sharedPrefix > 0 && sharedPrefix > maxSubsequenceLength && i < REMOVE_EVERYTHING && j < REMOVE_EVERYTHING){
					maxSubsequenceLength = sharedPrefix;
					maxInfixIndex = i;
					maxInfixLength = j;
				}
			}
		}

		int truncateSuffixBytes = source.length - (maxInfixLength + maxSubsequenceLength);

		//special case: if we're removing the suffix in the infix code, move it to the suffix code instead
		if(truncateSuffixBytes == 0 && maxInfixIndex + maxInfixLength == source.length){
			truncateSuffixBytes = maxInfixLength;
			maxInfixIndex = maxInfixLength = 0;
		}

		if(maxInfixIndex >= REMOVE_EVERYTHING || maxInfixLength >= REMOVE_EVERYTHING || truncateSuffixBytes >= REMOVE_EVERYTHING){
			maxInfixIndex = maxSubsequenceLength = 0;
			maxInfixLength = truncateSuffixBytes = REMOVE_EVERYTHING;
		}

		final int len1 = target.length - maxSubsequenceLength;
		final byte[] encoded = new byte[3 + len1];
		encoded[0] = encodeValue(maxInfixIndex);
		encoded[1] = encodeValue(maxInfixLength);
		encoded[2] = encodeValue(truncateSuffixBytes);
		System.arraycopy(target, maxSubsequenceLength, encoded, 3, len1);
		return encoded;
	}

	@Override
	public byte[] decode(final byte[] source, final byte[] encoded){
		int infixIndex = decodeValue(encoded[0]);
		int infixLength = decodeValue(encoded[1]);
		int truncateSuffixBytes = decodeValue(encoded[2]);

		if(infixLength == REMOVE_EVERYTHING || truncateSuffixBytes == REMOVE_EVERYTHING){
			infixIndex = 0;
			infixLength = source.length;
			truncateSuffixBytes = 0;
		}

		final int len1 = source.length - (infixIndex + infixLength + truncateSuffixBytes);
		final int len2 = encoded.length - 3;
		final byte[] decoded = new byte[infixIndex + len1 + len2];
		System.arraycopy(source, 0, decoded, 0, infixIndex);
		System.arraycopy(source, infixIndex + infixLength, decoded, infixIndex, len1);
		System.arraycopy(encoded, 3, decoded, infixIndex + len1, len2);
		return decoded;
	}

	@Override
	public String toString(){
		return getClass().getSimpleName();
	}

}
