package unit731.hunspeller.collections.bloomfilter.core;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class BitArrayBuilder{

	public static enum Type{FAST, JAVA, MEMORY_MAPPED_FILE};


	public static BitArray getBitArray(Type type, int bits){
		Objects.nonNull(type);

		BitArray ba = null;
		switch(type){
			case FAST:
				ba = new FastBitArray(bits);
				break;

			case MEMORY_MAPPED_FILE:
				try{
					File file = File.createTempFile("hunspeller-duplications-bitarray", ".bits");
					file.deleteOnExit();
					ba = new MemoryMappedFileBitArray(file, bits);

					break;
				}
				catch(IOException e){
					log.warn("Cannot instantiate a Memory-Mapped File BitArray, fallback to standard java implementation", e);
				}

			case JAVA:
				ba = new JavaBitArray(bits);
		}
		return ba;
	}

}
