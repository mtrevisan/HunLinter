package unit731.hunlinter.datastructures.fsa.stemming;

import java.nio.ByteBuffer;


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

}
