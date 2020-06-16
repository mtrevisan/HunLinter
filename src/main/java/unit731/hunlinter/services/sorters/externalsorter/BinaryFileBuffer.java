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
package unit731.hunlinter.services.sorters.externalsorter;

import java.io.Closeable;
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
	public void close(){
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
