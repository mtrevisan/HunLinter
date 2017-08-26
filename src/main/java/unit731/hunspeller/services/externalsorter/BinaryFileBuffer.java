package unit731.hunspeller.services.externalsorter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Objects;


/**
 * This is essentially a thin wrapper on top of a BufferedReader which keeps the last line in memory.
 */
public class BinaryFileBuffer{

	public BufferedReader br;
	private String cache;


	public BinaryFileBuffer(BufferedReader r) throws IOException{
		Objects.nonNull(r);

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
		String answer = peek();
		reload();
		return answer;
	}

	private void reload() throws IOException{
		cache = br.readLine();
	}

}
