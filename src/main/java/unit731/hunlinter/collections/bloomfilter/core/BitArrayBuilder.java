package unit731.hunlinter.collections.bloomfilter.core;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.services.downloader.DownloaderHelper;


public class BitArrayBuilder{

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
					final File file = File.createTempFile(DownloaderHelper.getPOMProperties().get(DownloaderHelper.PROPERTY_KEY_ARTIFACT_ID) + "-duplicates-bitarray", ".bits");
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
