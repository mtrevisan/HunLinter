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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.services.downloader.DownloaderHelper;


class BitArrayTest{

	private static final int MAX = 10_000;


	@Test
	void java() throws IOException{
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
	void memoryMappedFile() throws IOException{
		File file = File.createTempFile(DownloaderHelper.getApplicationProperties().get(DownloaderHelper.PROPERTY_KEY_ARTIFACT_ID) + "-duplications-bitarray", ".bits");
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
