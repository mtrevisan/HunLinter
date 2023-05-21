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
package io.github.mtrevisan.hunlinter.parsers.affix.handlers;

import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.affix.ParsingContext;
import io.github.mtrevisan.hunlinter.services.ParserHelper;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class AliasesHandler implements Handler{

	private static final String BAD_FIRST_PARAMETER = "Error reading line `{}`: the first parameter is not a number";
	private static final String BAD_NUMBER_OF_ENTRIES = "Error reading line `{}`: bad number of entries, `{}` must be a positive integer less or equal than " + Short.MAX_VALUE;
	private static final String WRONG_FORMAT = "Error reading line `{}`: bad number of entries, it must be '<option> <flag/morphological field>'";
	private static final String BAD_OPTION = "Error reading line `{}`: bad option, it must be {}";


	@Override
	public final int parse(final ParsingContext context, final AffixData affixData) throws EOFException{
		final Scanner scanner = context.getScanner();
		if(!NumberUtils.isCreatable(context.getFirstParameter()))
			throw new LinterException(BAD_FIRST_PARAMETER, context);
		final int numEntries = Integer.parseInt(context.getFirstParameter());
		if(numEntries <= 0 || numEntries > Short.MAX_VALUE)
			throw new LinterException(BAD_NUMBER_OF_ENTRIES, context, context.getFirstParameter());

		final List<String> aliases = new ArrayList<>(numEntries);
		for(int i = 0; i < numEntries; i ++){
			ParserHelper.assertNotEOF(scanner);

			final String line = scanner.nextLine();
			final String[] parts = StringUtils.split(line);

			checkValidity(parts, context);

			aliases.add(parts[1]);
		}

		affixData.addData(context.getRuleType(), aliases);

		return numEntries;
	}

	private static void checkValidity(final String[] parts, final ParsingContext context){
		if(parts.length != 2)
			throw new LinterException(WRONG_FORMAT, context);
		if(!context.getRuleType().equals(parts[0]))
			throw new LinterException(BAD_OPTION, context, context.getRuleType());
	}

}
