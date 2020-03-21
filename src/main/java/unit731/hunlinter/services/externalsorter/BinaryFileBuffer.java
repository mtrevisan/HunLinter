package unit731.hunlinter.services.externalsorter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Objects;


/**
 * This is essentially a thin wrapper on top of a BufferedReader which keeps the last line in memory.
 */
class BinaryFileBuffer{

	public final BufferedReader br;
	private String cache;


	BinaryFileBuffer(final BufferedReader r) throws IOException{
		Objects.requireNonNull(r);

		br = r;

		reload();
	}

	public void close() throws IOException{
		br.close();
	}

	public boolean empty(){
		return (cache == null);
	}

	public String peek(){
		return cache;
	}

	public String pop() throws IOException{
		final String answer = peek();
		reload();
		return answer;
	}

	private void reload() throws IOException{
		cache = br.readLine();
	}

}
