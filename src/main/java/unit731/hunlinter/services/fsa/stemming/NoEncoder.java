package unit731.hunlinter.services.fsa.stemming;

import java.nio.ByteBuffer;


/**
 * No relative encoding at all (full target form is returned).
 *
 * @see "org.carrot2.morfologik-parent, 2.1.8-SNAPSHOT, 2020-01-02"
 */
public class NoEncoder implements SequenceEncoderInterface{

	@Override
	public ByteBuffer encode(ByteBuffer reuse, ByteBuffer source, ByteBuffer target){
		reuse = BufferUtils.clearAndEnsureCapacity(reuse, target.remaining());

		target.mark();
		reuse.put(target).flip();
		target.reset();

		return reuse;
	}

	@Override
	public ByteBuffer decode(ByteBuffer reuse, ByteBuffer source, ByteBuffer encoded){
		reuse = BufferUtils.clearAndEnsureCapacity(reuse, encoded.remaining());

		encoded.mark();
		reuse.put(encoded).flip();
		encoded.reset();

		return reuse;
	}

	@Override
	public int prefixBytes(){
		return 0;
	}

	@Override
	public String toString(){
		return getClass().getSimpleName();
	}

}
