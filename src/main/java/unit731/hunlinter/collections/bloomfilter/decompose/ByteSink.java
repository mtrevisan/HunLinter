package unit731.hunlinter.collections.bloomfilter.decompose;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;


/**
 * An in-memory sink that uses a {@link ByteArrayOutputStream} to store the incoming bytes.
 */
public class ByteSink{

	/** The actual storage stream */
	private final ByteArrayOutputStream stream = new ByteArrayOutputStream();
	/** Wrapper over the byte stream */
	private final DataOutputStream dataStream = new DataOutputStream(stream);


	/**
	 * Get the byte-array of bytes currently stored
	 *
	 * @return	The byte array
	 */
	public byte[] getByteArray(){
		return stream.toByteArray();
	}

	/**
	 * Store a single byte in this sink
	 *
	 * @param b	Byte to be added
	 * @return	This instance
	 */
	public ByteSink putByte(final byte b){
		stream.write(b);
		return this;
	}

	/**
	 * Store the given bytes in this sink
	 *
	 * @param bytes	The array of bytes to be added
	 * @return	This instance
	 */
	public ByteSink putBytes(final byte[] bytes){
		try{
			stream.write(bytes);
		}
		catch(final IOException e){
			throw new RuntimeException("Unable to store bytes inside the sink", e);
		}
		return this;
	}

	public ByteSink putBytes(final byte[] bytes, final int offset, final int length){
		stream.write(bytes, offset, length);
		return this;
	}

	public ByteSink putChar(final char c){
		try{
			dataStream.writeChar(c);
			return this;
		}
		catch(final IOException e){
			throw new RuntimeException("Unable to store char inside the sink", e);
		}
	}

	public ByteSink putShort(final short s){
		try{
			dataStream.writeShort(s);
			return this;
		}
		catch(final IOException e){
			throw new RuntimeException("Unable to store short inside the sink", e);
		}
	}

	public ByteSink putInt(final int i){
		try{
			dataStream.writeInt(i);
			return this;
		}
		catch(final IOException e){
			throw new RuntimeException("Unable to store int inside the sink", e);
		}
	}

	public ByteSink putLong(final long l){
		try{
			dataStream.writeLong(l);
			return this;
		}
		catch(final IOException e){
			throw new RuntimeException("Unable to store long inside the sink", e);
		}
	}

	public ByteSink putFloat(final float f){
		try{
			dataStream.writeFloat(f);
			return this;
		}
		catch(final IOException e){
			throw new RuntimeException("Unable to store float inside the sink", e);
		}
	}

	public ByteSink putDouble(final double d){
		try{
			dataStream.writeDouble(d);
			return this;
		}
		catch(final IOException e){
			throw new RuntimeException("Unable to store double inside the sink", e);
		}
	}

	public ByteSink putBoolean(final boolean b){
		try{
			dataStream.writeBoolean(b);
			return this;
		}
		catch(final IOException e){
			throw new RuntimeException("Unable to store boolean inside the sink", e);
		}
	}

	public ByteSink putChars(final CharSequence charSequence){
		try{
			dataStream.writeBytes(charSequence.toString());
			return this;
		}
		catch(final IOException e){
			throw new RuntimeException("Unable to store charSequence inside the sink", e);
		}
	}

}
