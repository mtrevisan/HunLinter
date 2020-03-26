package unit731.hunlinter.services.sorters.externalsorter;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.Scanner;


/**
 * This is essentially a thin wrapper on top of a BufferedReader which keeps the last line in memory.
 *
 * @see <a href="https://github.com/lemire/externalsortinginjava">External-Memory Sorting in Java</a>, version 0.4.4, 11/3/2020
 */
class BinaryFileBuffer implements Closeable{

	protected final Scanner scanner;
	private String cache;


	BinaryFileBuffer(final Scanner scanner){
		Objects.requireNonNull(scanner);

		this.scanner = scanner;

		readNextLine();
	}

	@Override
	public void close() throws IOException{
		scanner.close();
	}

	public boolean isEmpty(){
		return (cache == null);
	}

	public String peek(){
		return cache;
	}

	public String pop(){
		final String answer = peek();
		readNextLine();
		return answer;
	}

	private void readNextLine(){
		cache = (scanner.hasNextLine()? scanner.nextLine(): null);
	}

}
