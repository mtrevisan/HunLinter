package unit731.hunspeller.collections.bloomfilter.core;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class BitArrayTest{

	private static final int MAX = 10_000;


	@Test
	public void java() throws IOException{
		try(JavaBitArray bits = new JavaBitArray(MAX)){
			for(int i = 0; i < MAX; i ++){
				Assertions.assertFalse(bits.get(i));
				bits.set(i);
				Assertions.assertTrue(bits.get(i));
				bits.clear(i);
				Assertions.assertFalse(bits.get(i));
			}
		}
	}

	@Test
	public void memoryMappedFile() throws IOException{
		File file = File.createTempFile("hunspeller-duplications-bitarray", ".bits");
		file.deleteOnExit();
		try(MemoryMappedFileBitArray bits = new MemoryMappedFileBitArray(file, MAX)){
			for(int i = 0; i < MAX; i ++){
				Assertions.assertFalse(bits.get(i));
				bits.set(i);
				Assertions.assertTrue(bits.get(i));
				bits.clear(i);
				Assertions.assertFalse(bits.get(i));
			}
		}
	}

}
