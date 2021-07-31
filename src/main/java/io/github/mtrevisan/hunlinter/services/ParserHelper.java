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
package io.github.mtrevisan.hunlinter.services;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import io.github.mtrevisan.hunlinter.services.system.FileHelper;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.List;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public final class ParserHelper{

	private static final MessageFormat WRONG_FILE_FORMAT = new MessageFormat("Malformed file, the first line is not a number, was `{0}`");

	public static final char COMMENT_MARK_SHARP = '#';
	public static final char COMMENT_MARK_SLASH = '/';
	public static final char COMMENT_MARK_PERCENT = '%';


	private ParserHelper(){}

	public static boolean isComment(final CharSequence line, final char... comment){
		return (StringUtils.isBlank(line) || StringUtils.indexOfAny(line, comment) == 0);
	}

	public static void assertNotEOF(final Scanner scanner) throws EOFException{
		if(!scanner.hasNextLine())
			throw new EOFException("Unexpected EOF while reading file");
	}

	public static void forEachLine(final File file, final Charset charset, final BiConsumer<Integer, String> fun,
			final Consumer<Integer> progressCallback, final char... comment){
		final int totalLines = FileHelper.getLinesCount(file, charset);
		int progress = 0;
		int progressIndex = 0;
		final int progressStep = (int)Math.ceil(totalLines / 100.f);
		try(final Scanner scanner = FileHelper.createScanner(file.toPath(), charset)){
			assertLinesCount(scanner);

			while(scanner.hasNextLine()){
				final String line = scanner.nextLine();

				if(!isComment(line, comment))
					fun.accept(progress, line);

				if(progressCallback != null && ++ progress % progressStep == 0)
					progressCallback.accept(++ progressIndex);
			}

			if(progressCallback != null)
				progressCallback.accept(100);
		}
		catch(final Exception e){
			throw new RuntimeException(e);
		}
	}

	public static void assertLinesCount(final List<String> lines) throws IOException{
		if(lines.isEmpty())
			throw new EOFException("Unexpected EOF while reading file");
		final String line = lines.get(0);
		if(!NumberUtils.isCreatable(line))
			throw new LinterException(WRONG_FILE_FORMAT.format(new Object[]{line}));
	}

	public static String assertLinesCount(final Scanner scanner){
		final String line = scanner.nextLine();
		if(!NumberUtils.isCreatable(line))
			throw new LinterException(WRONG_FILE_FORMAT.format(new Object[]{line}));

		return line;
	}

}
