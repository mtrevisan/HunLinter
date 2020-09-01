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
package unit731.hunlinter.datastructures.bloomfilter.core;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.Arrays;


/**
 * An implementation of {@link BitArray} that uses a memory-mapped file to persist all changes synchronously for the underlying
 * bit array. This is useful for stateful bit-arrays which are expensive to construct yet need the best overall performance.
 *
 * NOTE: for some reason this class doesn't work!!
 */
public class MemoryMappedFileBitArray implements BitArray{

	private static final String EMPTY_BACKUP_FILE = "Backup file cannot be empty/null";
	private static final String INVALID_BACKUP_FILE = "Backup file doesn''t represent a valid file";
	private static final String INVALID_NUMBER_OF_BITS = "Number of bits must be strictly positive";


	/** Underlying file that represents the state of the {@link BitArray} */
	private final RandomAccessFile backingFile;
	/** The maximum number of elements this file will store */
	private final int maxElements;
	/** The number of bytes being used for this byte-array */
	private final int numberOfBytes;
	/** The memory-mapped byte-buffer */
	private MappedByteBuffer buffer;


	public MemoryMappedFileBitArray(final File backingFile, final int bits) throws IOException{
		if(backingFile == null)
			throw new IllegalArgumentException(EMPTY_BACKUP_FILE);
		if(backingFile.exists() && !backingFile.isFile())
			throw new IllegalArgumentException(INVALID_BACKUP_FILE);
		if(bits <= 0)
			throw new IllegalArgumentException(INVALID_NUMBER_OF_BITS);

		//we open in "rwd" mode, to save one i/o operation than in "rws" mode
		this.backingFile = new RandomAccessFile(backingFile, "rwd");

		numberOfBytes = (bits >> 3) + 1;
		extendFile(numberOfBytes);

		//initialize the rest
		maxElements = bits;
		buffer = this.backingFile.getChannel().map(MapMode.READ_WRITE, 0, this.backingFile.length());
	}

	private void extendFile(final long newLength) throws IOException{
		final long current = backingFile.length();
		final int delta = (int)(newLength - current) + 1;
		if(delta > 0){
			backingFile.setLength(newLength);
			backingFile.seek(current);
			final byte[] bytes = new byte[delta];
			Arrays.fill(bytes, (byte)0x00);
			backingFile.write(bytes);
		}
	}

	@Override
	public boolean get(final int index){
		if(index > maxElements)
			throw new IndexOutOfBoundsException("Index is greater than max allowed elements");

		final int pos = index >> 3;
		final int bit = 1 << (index & 0x07);
		final byte bite = buffer.get(pos);
		return ((bite & bit) != 0);
	}

	@Override
	public boolean set(final int index){
		if(!get(index)){
			final int pos = index >> 3;
			final int bit = 1 << (index & 0x07);
			buffer.put(pos, (byte)(buffer.get(pos) | bit));
			return true;
		}
		return false;
	}

	@Override
	public void clear(final int index){
		if(index > maxElements)
			throw new IndexOutOfBoundsException("Index is greater than max allowed elements");

		final int pos = index >> 3;
		final int bit = ~(1 << (index & 0x07));
		buffer.put(pos, (byte)(buffer.get(pos) & bit));
	}

	@Override
	public void clearAll(){
		if(buffer != null)
			for(int index = 0; index < numberOfBytes; index ++)
				buffer.put(index, (byte)0);
	}

	@Override
	public int size(){
		return numberOfBytes * Byte.SIZE;
	}

	@Override
	public void close() throws IOException{
		try(backingFile){
			closeDirectBuffer();
		}
	}

	private void closeDirectBuffer(){
		//helps unmap a memory-mapped file before being garbage-collected.
		if(buffer != null && buffer.isDirect()){
			//we could use this type cast and call functions without reflection code,
			//but static import from sun.* package is risky for non-SUN virtual machine
//			try{
//				((sun.nio.ch.DirectBuffer)buffer).cleaner().clean();
//			}
//			catch(final Exception e){ }

//			try{
//				final Method cleaner = buffer.getClass().getMethod("cleaner");
//				cleaner.setAccessible(true);
//				final Method clean = Class.forName("sun.misc.Cleaner").getMethod("clean");
//				clean.setAccessible(true);
//				clean.invoke(cleaner.invoke(buffer));
//			}
//			catch(final ClassNotFoundException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException e){ }

			try{
				final Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
				//do not need to check for a specific class, we can call the Unsafe method with any buffer class
				final MethodHandle unmapper = MethodHandles.lookup()
					.findVirtual(unsafeClass, "invokeCleaner", MethodType.methodType(void.class, ByteBuffer.class));
				//fetch the unsafe instance and bind it to the virtual MethodHandle
				final Field f = unsafeClass.getDeclaredField("theUnsafe");
				f.setAccessible(true);
				final Object theUnsafe = f.get(null);
				unmapper.bindTo(theUnsafe)
					.invokeExact(buffer);
			}
			catch(final Throwable ignored){ }

			buffer = null;
		}
	}

}
