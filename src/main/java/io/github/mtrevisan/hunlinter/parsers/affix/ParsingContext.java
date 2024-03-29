/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.parsers.affix;

import io.github.mtrevisan.hunlinter.services.ParserHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Scanner;
import java.util.StringJoiner;


public class ParsingContext{

	private String line;
	private int index;
	private Scanner scanner;

	private String[] lineParts;


	public final void update(final String line, final int index, final Scanner scanner){
		Objects.requireNonNull(line, "Line cannot be null");
		Objects.requireNonNull(scanner, "Scanner cannot be null");

		this.line = line;
		this.index = index;
		this.scanner = scanner;

		lineParts = StringUtils.split(line);
	}

	public final String getLine(){
		final int commentIndex = line.indexOf(ParserHelper.COMMENT_MARK_SHARP);
		return (commentIndex >= 0? line.substring(0, commentIndex).trim(): line);
	}

	public final int getIndex(){
		return index;
	}

	public final Scanner getScanner(){
		return scanner;
	}

	public final String getRuleType(){
		return lineParts[0];
	}

	public final String getFirstParameter(){
		return lineParts[1];
	}

	public final String getSecondParameter(){
		return lineParts[2];
	}

	public final String getThirdParameter(){
		return lineParts[3];
	}

	public final String getAllButFirstParameter(){
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		for(int i = 1; i < lineParts.length; i ++)
			sj.add(lineParts[i]);
		return sj.toString();
	}

	@Override
	public final String toString(){
		return line;
	}

}
