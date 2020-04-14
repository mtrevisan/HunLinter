package unit731.hunlinter.datastructures.fsa.stemming;

import java.nio.ByteBuffer;


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

	/**
	 * Maximum encodable single-byte code.
	 */
	private static final int REMOVE_EVERYTHING = 255;


	public ByteBuffer encode(final ByteBuffer source, final ByteBuffer target, ByteBuffer reuse){
		int sharedPrefix = BufferUtils.sharedPrefixLength(source, target);
		int truncateBytes = source.remaining() - sharedPrefix;
		if(truncateBytes >= REMOVE_EVERYTHING){
			truncateBytes = REMOVE_EVERYTHING;
			sharedPrefix = 0;
		}

		reuse = BufferUtils.clearAndEnsureCapacity(reuse, 1 + target.remaining() - sharedPrefix);

		//assert target.hasArray() && target.position() == 0 && target.arrayOffset() == 0;

		final byte suffixTrimCode = (byte)(truncateBytes + 'A');
		reuse.put(suffixTrimCode).put(target.array(), sharedPrefix, target.remaining() - sharedPrefix).flip();

		return reuse;
	}

	@Override
	public int prefixBytes(){
		return 1;
	}

	public ByteBuffer decode(ByteBuffer reuse, final ByteBuffer source, final ByteBuffer encoded){
		//assert encoded.remaining() >= 1;

		int suffixTrimCode = encoded.get(encoded.position());
		int truncateBytes = (suffixTrimCode - 'A') & 0xFF;
		if(truncateBytes == REMOVE_EVERYTHING)
			truncateBytes = source.remaining();

		final int len1 = source.remaining() - truncateBytes;
		final int len2 = encoded.remaining() - 1;

		reuse = BufferUtils.clearAndEnsureCapacity(reuse, len1 + len2);

		//assert source.hasArray() && source.position() == 0 && source.arrayOffset() == 0;
		//assert encoded.hasArray() && encoded.position() == 0 && encoded.arrayOffset() == 0;

		reuse.put(source.array(), 0, len1).put(encoded.array(), 1, len2).flip();

		return reuse;
	}

	@Override
	public String toString(){
		return getClass().getSimpleName();
	}

}
