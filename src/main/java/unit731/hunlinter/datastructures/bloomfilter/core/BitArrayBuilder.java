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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.services.downloader.DownloaderHelper;

import java.io.File;
import java.io.IOException;
import java.util.Objects;


public final class BitArrayBuilder{

	private static final Logger LOGGER = LoggerFactory.getLogger(BitArrayBuilder.class);


	public enum Type{JAVA, MEMORY_MAPPED_FILE}


	private BitArrayBuilder(){}

	public static BitArray getBitArray(final Type type, final int bits){
		Objects.requireNonNull(type);

		BitArray ba = null;
		switch(type){
			case JAVA:
				ba = new JavaBitArray(bits);
				break;

			case MEMORY_MAPPED_FILE:
				try{
					final File file = File.createTempFile(DownloaderHelper.APPLICATION_PROPERTIES.get(DownloaderHelper.PROPERTY_KEY_ARTIFACT_ID) + "-duplicates-bitarray", ".bits");
					file.deleteOnExit();
					ba = new MemoryMappedFileBitArray(file, bits);
				}
				catch(final IOException e){
					ba = new JavaBitArray(bits);

					LOGGER.warn("Cannot instantiate a Memory-Mapped File BitArray, fallback to standard java implementation", e);
				}
		}
		return ba;
	}

}
