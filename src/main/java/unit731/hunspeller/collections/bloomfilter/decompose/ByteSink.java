package unit731.hunspeller.collections.bloomfilter.decompose;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;


/**
 * An in-memory sink that uses a {@link ByteArrayOutputStream} to store the incoming bytes.
 */
public class ByteSink{

	/** The actual storage stream */
	protected ByteArrayOutputStream stream = new ByteArrayOutputStream();
	/** Wrapper over the byte stream */
	protected DataOutputStream dataStream = new DataOutputStream(stream);


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
	public ByteSink putByte(byte b){
		stream.write(b);
		return this;
	}

	/**
	 * Store the given bytes in this sink
	 *
	 * @param bytes	The arra of bytes to be added
	 * @return	This instance
	 */
	public ByteSink putBytes(byte[] bytes){
		try{
			stream.write(bytes);
		}
		catch(IOException e){
			throw new RuntimeException("Unable to store bytes inside the sink", e);
		}
		return this;
	}

	public ByteSink putBytes(byte[] bytes, int offset, int length){
		stream.write(bytes, offset, length);
		return this;
	}

	public ByteSink putChar(char c){
		try{
			dataStream.writeChar(c);
			return this;
		}
		catch(IOException e){
			throw new RuntimeException("Unable to store char inside the sink", e);
		}
	}

	public ByteSink putShort(short s){
		try{
			dataStream.writeShort(s);
			return this;
		}
		catch(IOException e){
			throw new RuntimeException("Unable to store short inside the sink", e);
		}
	}

	public ByteSink putInt(int i){
		try{
			dataStream.writeInt(i);
			return this;
		}
		catch(IOException e){
			throw new RuntimeException("Unable to store int inside the sink", e);
		}
	}

	public ByteSink putLong(long l){
		try{
			dataStream.writeLong(l);
			return this;
		}
		catch(IOException e){
			throw new RuntimeException("Unable to store long inside the sink", e);
		}
	}

	public ByteSink putFloat(float f){
		try{
			dataStream.writeFloat(f);
			return this;
		}
		catch(IOException e){
			throw new RuntimeException("Unable to store float inside the sink", e);
		}
	}

	public ByteSink putDouble(double d){
		try{
			dataStream.writeDouble(d);
			return this;
		}
		catch(IOException e){
			throw new RuntimeException("Unable to store double inside the sink", e);
		}
	}

	public ByteSink putBoolean(boolean b){
		try{
			dataStream.writeBoolean(b);
			return this;
		}
		catch(IOException e){
			throw new RuntimeException("Unable to store boolean inside the sink", e);
		}
	}

	public ByteSink putChars(CharSequence charSequence){
		try{
			dataStream.writeBytes(charSequence.toString());
			return this;
		}
		catch(IOException e){
			throw new RuntimeException("Unable to store charSequence inside the sink", e);
		}
	}

}
