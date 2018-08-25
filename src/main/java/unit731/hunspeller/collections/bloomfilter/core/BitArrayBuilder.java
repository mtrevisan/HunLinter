package unit731.hunspeller.collections.bloomfilter.core;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class BitArrayBuilder{

	public static enum Type{JAVA, MEMORY_MAPPED_FILE}


	@SuppressWarnings("fallthrough")
	public static BitArray getBitArray(Type type, int bits){
		Objects.requireNonNull(type);

		BitArray ba = null;
		switch(type){
			case JAVA:
				ba = new JavaBitArray(bits);
				break;

			case MEMORY_MAPPED_FILE:
				try{
					File file = File.createTempFile("hunspeller-duplications-bitarray", ".bits");
					file.deleteOnExit();
					ba = new MemoryMappedFileBitArray(file, bits);
				}
				catch(IOException e){
					ba = new JavaBitArray(bits);

					log.warn("Cannot instantiate a Memory-Mapped File BitArray, fallback to standard java implementation", e);
				}
		}
		return ba;
	}

}
