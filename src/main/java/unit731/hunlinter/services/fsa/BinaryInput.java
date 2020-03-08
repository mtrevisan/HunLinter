package unit731.hunlinter.services.fsa;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public final class BinaryInput{

	private interface LineConsumer{
		byte[] process(final byte[] buffer, final int length);
	}


	public List<byte[]> readBinarySequences(final Path input, final byte separator) throws IOException{
		final List<byte[]> sequences = new ArrayList<>();
		try(final InputStream is = new BufferedInputStream(Files.newInputStream(input))){
			forAllLines(is, separator, (buffer, length) -> {
				if(length != 0)
					sequences.add(Arrays.copyOf(buffer, length));

				return buffer;
			});
		}

		return sequences;
	}

	/**
	 * Read all byte-separated sequences.
	 */
	private static int forAllLines(final InputStream is, final byte separator, final LineConsumer lineConsumer) throws IOException{
		int lines = 0;
		byte[] buffer = new byte[0];
		int b;
		int pos = 0;
		while((b = is.read()) != -1){
			if(b == separator){
				buffer = lineConsumer.process(buffer, pos);
				pos = 0;
				lines ++;
			}
			else{
				if(pos >= buffer.length)
					buffer = Arrays.copyOf(buffer, buffer.length + Math.max(10, buffer.length / 10));
				buffer[pos ++] = (byte)b;
			}
		}

		if(pos > 0){
			lineConsumer.process(buffer, pos);
			lines ++;
		}
		return lines;
	}

}
