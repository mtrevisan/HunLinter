package unit731.hunlinter.datastructures.fsa.stemming;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;


/**
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public class BufferUtils{

	private BufferUtils(){}

	/**
	 * Ensure the buffer's capacity is large enough to hold a given number
	 * of elements. If the input buffer is not large enough, a new buffer is allocated
	 * and returned.
	 *
	 * @param elements The required number of elements to be appended to the buffer.
	 * @param buffer   The buffer to check or <code>null</code> if a new buffer should be
	 *                 allocated.
	 * @return Returns the same buffer or a new buffer with the given capacity.
	 */
	public static ByteBuffer clearAndEnsureCapacity(ByteBuffer buffer, final int elements){
		if(buffer == null || buffer.capacity() < elements)
			buffer = ByteBuffer.allocate(elements);
		else
			buffer.clear();
		return buffer;
	}

	/**
	 * @param buffer  The buffer to convert to a string.
	 * @param charset The charset to use when converting bytes to characters.
	 * @return A string representation of buffer's content.
	 */
	public static String toString(ByteBuffer buffer, final Charset charset){
		buffer = buffer.slice();
		final byte[] buf = new byte[buffer.remaining()];
		buffer.get(buf);
		return new String(buf, charset);
	}

	public static String toString(CharBuffer buffer){
		buffer = buffer.slice();
		final char[] buf = new char[buffer.remaining()];
		buffer.get(buf);
		return new String(buf);
	}

	/** Compute the length of the shared prefix between two byte sequences */
	static int sharedPrefixLength(final byte[] a, int aStart, final byte[] b, int bStart){
		int i = 0;
		final int max = Math.min(a.length - aStart, b.length - bStart);
		while(i < max && a[aStart ++] == b[bStart ++])
			i ++;
		return i;
	}

}
