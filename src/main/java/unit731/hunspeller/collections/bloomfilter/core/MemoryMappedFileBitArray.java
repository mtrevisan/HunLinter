package unit731.hunspeller.collections.bloomfilter.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.text.MessageFormat;
import java.util.Arrays;


/**
 * An implementation of {@link BitArray} that uses a memory-mapped file to persist all changes synchronously for the underlying
 * bit array. This is useful for stateful bit-arrays which are expensive to construct yet need the best overall performance.
 * 
 * NOTE: for some reason this class does not work!!
 */
public class MemoryMappedFileBitArray implements BitArray{

	private static final MessageFormat EMPTY_BACKUP_FILE = new MessageFormat("Backup file cannot be empty/null");
	private static final MessageFormat INVALID_BACKUP_FILE = new MessageFormat("Backup file does not represent a valid file");
	private static final MessageFormat INVALID_NUMBER_OF_BITS = new MessageFormat("Number of bits must be strictly positive");

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
			throw new IllegalArgumentException(EMPTY_BACKUP_FILE.format(new Object[0]));
		if(backingFile.exists() && !backingFile.isFile())
			throw new IllegalArgumentException(INVALID_BACKUP_FILE.format(new Object[0]));
		if(bits <= 0)
			throw new IllegalArgumentException(INVALID_NUMBER_OF_BITS.format(new Object[0]));

		//we open in "rwd" mode, to save one i/o operation than in "rws" mode
		this.backingFile = new RandomAccessFile(backingFile, "rwd");

		numberOfBytes = (bits >> 3) + 1;
		extendFile(numberOfBytes);

		//initialize the rest
		this.maxElements = bits;
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
			//but static import from sun.* package is risky for non–SUN virtual machine
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
				final MethodHandle unmapper = MethodHandles.lookup().findVirtual(unsafeClass, "invokeCleaner", MethodType.methodType(void.class, ByteBuffer.class));
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
