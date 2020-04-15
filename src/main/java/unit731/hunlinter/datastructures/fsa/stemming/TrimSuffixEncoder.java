package unit731.hunlinter.datastructures.fsa.stemming;


/**
 * Encodes <code>target</code> relative to <code>source</code> by trimming whatever
 * non-equal suffix <code>source</code> has. The output code is (bytes):
 *
 * <pre>
 * {K}{suffix}
 * </pre>
 *
 * where (<code>K</code> - 'A') bytes should be trimmed from the end of
 * <code>source</code> and then the <code>suffix</code> should be appended to the
 * resulting byte sequence.
 *
 * <p>
 * Examples:
 * </p>
 *
 * <pre>
 * source:	foo
 * target:	foobar
 * encoded:	Abar
 *
 * source:	foo
 * target:	bar
 * encoded:	Dbar
 * </pre>
 *
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public class TrimSuffixEncoder implements SequenceEncoderInterface{

	@Override
	public byte[] encode(final byte[] source, final byte[] target){
		int sharedPrefix = BufferUtils.sharedPrefixLength(source, target);
		int truncateBytes = source.length - sharedPrefix;
		if(truncateBytes >= REMOVE_EVERYTHING){
			truncateBytes = REMOVE_EVERYTHING;
			sharedPrefix = 0;
		}

		final byte[] encoded = new byte[1 + target.length - sharedPrefix];
		final byte suffixTrimCode = (byte)(truncateBytes + 'A');
		encoded[0] = suffixTrimCode;
		System.arraycopy(target, 0, encoded, 1, target.length - sharedPrefix);
		return encoded;
	}

	@Override
	public byte[] decode(final byte[] source, final byte[] encoded){
		final int suffixTrimCode = encoded[0];
		int truncateBytes = (suffixTrimCode - 'A') & 0xFF;
		if(truncateBytes == REMOVE_EVERYTHING)
			truncateBytes = source.length;

		final int len1 = source.length - truncateBytes;
		final int len2 = encoded.length - 1;

		final byte[] decoded = new byte[len1 + len2];
		System.arraycopy(source, 0, decoded, 0, len1);
		System.arraycopy(encoded, 1, decoded, len1, len2);
		return decoded;
	}

	@Override
	public String toString(){
		return getClass().getSimpleName();
	}

}
