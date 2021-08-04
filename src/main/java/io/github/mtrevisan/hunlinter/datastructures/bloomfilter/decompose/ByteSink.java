/**
 * Copyright (c) 2019-2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.datastructures.bloomfilter.decompose;

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
		try{
			stream.close();
		}
		catch(final IOException ignored){}
		try{
			dataStream.close();
		}
		catch(final IOException ignored){}
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
