/**
 * Copyright (c) 2019-2021 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.parsers.aid;

import io.github.mtrevisan.hunlinter.services.system.FileHelper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class AidParser{

	private final List<String> lines = new ArrayList<>(0);


	/**
	 * Parse the rows out from a .aid file.
	 *
	 * @param aidFile	The content of the dictionary file
	 * @throws IOException	If an I/O error occurs
	 */
	public final void parse(final File aidFile) throws IOException{
		lines.clear();

		final Path aidPath = aidFile.toPath();
		final Charset charset = FileHelper.determineCharset(aidPath);
		try(final Scanner scanner = FileHelper.createScanner(aidPath, charset)){
			while(scanner.hasNextLine()){
				final String line = scanner.nextLine();
				if(!line.isEmpty())
					lines.add(line);
			}
		}
	}

	public final void clear(){
		lines.clear();
	}

	public final List<String> getLines(){
		return lines;
	}

}
