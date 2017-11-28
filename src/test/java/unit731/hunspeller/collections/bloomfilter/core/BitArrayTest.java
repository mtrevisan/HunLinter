package unit731.hunspeller.collections.bloomfilter.core;

import java.io.File;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;


public class BitArrayTest{

	private static final int MAX = 100 * 100;


	@Test
	public void fast() throws IOException{
		FastBitArray bits = new FastBitArray(MAX);
		for(int i = 0; i < MAX; i ++){
			Assert.assertFalse(bits.get(i));
			bits.set(i);
			Assert.assertTrue(bits.get(i));
			bits.clear(i);
			Assert.assertFalse(bits.get(i));
		}
		bits.close();
	}

	@Test
	public void java() throws IOException{
		JavaBitArray bits = new JavaBitArray(MAX);
		for(int i = 0; i < MAX; i ++){
			Assert.assertFalse(bits.get(i));
			bits.set(i);
			Assert.assertTrue(bits.get(i));
			bits.clear(i);
			Assert.assertFalse(bits.get(i));
		}
		bits.close();
	}

	@Test
	public void memoryMappedFile() throws IOException{
		File file = File.createTempFile("hunspeller-duplications-bitarray", ".bits");
		file.deleteOnExit();
		MemoryMappedFileBitArray bits = new MemoryMappedFileBitArray(file, MAX);
		for(int i = 0; i < MAX; i ++){
			Assert.assertFalse(bits.get(i));
			bits.set(i);
			Assert.assertTrue(bits.get(i));
			bits.clear(i);
			Assert.assertFalse(bits.get(i));
		}
		bits.close();
	}

}
