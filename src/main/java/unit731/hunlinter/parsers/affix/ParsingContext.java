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
package unit731.hunlinter.parsers.affix;

import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.services.ParserHelper;


public class ParsingContext{

	private final String line;
	private final int index;
	private final Scanner scanner;

	private final String[] lineParts;


	public ParsingContext(final String line, final int index, final Scanner scanner){
		Objects.requireNonNull(line);
		Objects.requireNonNull(scanner);

		this.line = line;
		this.index = index;
		this.scanner = scanner;

		lineParts = StringUtils.split(line);
	}

	public String getLine(){
		final int commentIndex = line.indexOf(ParserHelper.COMMENT_MARK_SHARP);
		return (commentIndex >= 0? line.substring(0, commentIndex).trim(): line);
	}

	public int getIndex(){
		return index;
	}

	public Scanner getScanner(){
		return scanner;
	}

	public String getRuleType(){
		return lineParts[0];
	}

	public String getFirstParameter(){
		return lineParts[1];
	}

	public String getSecondParameter(){
		return lineParts[2];
	}

	public String getThirdParameter(){
		return lineParts[3];
	}

	public String getAllButFirstParameter(){
		return StringUtils.join(Arrays.asList(lineParts).subList(1, lineParts.length), StringUtils.SPACE);
	}

	@Override
	public String toString(){
		return line;
	}

}
