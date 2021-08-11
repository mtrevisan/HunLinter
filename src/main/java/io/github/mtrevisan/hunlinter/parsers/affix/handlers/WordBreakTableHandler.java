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
package io.github.mtrevisan.hunlinter.parsers.affix.handlers;

import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.affix.ParsingContext;
import io.github.mtrevisan.hunlinter.parsers.enums.AffixOption;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.HyphenationParser;
import io.github.mtrevisan.hunlinter.services.ParserHelper;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.EOFException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;


public class WordBreakTableHandler implements Handler{

	private static final MessageFormat BAD_FIRST_PARAMETER = new MessageFormat("Error reading line `{0}`: the first parameter is not a number");
	private static final MessageFormat BAD_NUMBER_OF_ENTRIES = new MessageFormat("Error reading line `{0}`: bad number of entries, `{1}` must be a positive integer less or equal than " + Short.MAX_VALUE);
	private static final MessageFormat MISMATCHED_TYPE = new MessageFormat("Error reading line `{0}`: mismatched type (expected {1})");
	private static final MessageFormat EMPTY_BREAK_CHARACTER = new MessageFormat("Error reading line `{0}`: break character cannot be empty");
	private static final MessageFormat DUPLICATED_LINE = new MessageFormat("Error reading line `{0}`: duplicated line");

	private static final String DOUBLE_MINUS_SIGN = HyphenationParser.MINUS_SIGN + HyphenationParser.MINUS_SIGN;


	@Override
	public int parse(final ParsingContext context, final AffixData affixData){
		try{
			final Scanner scanner = context.getScanner();
			if(!NumberUtils.isCreatable(context.getFirstParameter()))
				throw new LinterException(BAD_FIRST_PARAMETER.format(new Object[]{context}));
			final int numEntries = Integer.parseInt(context.getFirstParameter());
			if(numEntries <= 0 || numEntries > Short.MAX_VALUE)
				throw new LinterException(BAD_NUMBER_OF_ENTRIES.format(new Object[]{context, context.getFirstParameter()}));

			final Set<String> wordBreakCharacters = readCharacters(scanner, numEntries);

			affixData.addData(AffixOption.WORD_BREAK_CHARACTERS.getCode(), wordBreakCharacters);

			return numEntries;
		}
		catch(final IOException e){
			throw new RuntimeException(e.getMessage());
		}
	}

	private Set<String> readCharacters(final Scanner scanner, final int numEntries) throws EOFException{
		final Set<String> wordBreakCharacters = new HashSet<>(numEntries);
		for(int i = 0; i < numEntries; i ++){
			ParserHelper.assertNotEOF(scanner);

			final String line = scanner.nextLine();
			final String[] lineParts = StringUtils.split(line);

			final AffixOption option = AffixOption.createFromCode(lineParts[0]);
			if(option != AffixOption.WORD_BREAK_CHARACTERS)
				throw new LinterException(MISMATCHED_TYPE.format(new Object[]{line, AffixOption.WORD_BREAK_CHARACTERS}));

			final String breakCharacter = (DOUBLE_MINUS_SIGN.equals(lineParts[1])? HyphenationParser.EN_DASH: lineParts[1]);
			if(StringUtils.isBlank(breakCharacter))
				throw new LinterException(EMPTY_BREAK_CHARACTER.format(new Object[]{line}));

			final boolean inserted = wordBreakCharacters.add(breakCharacter);
			if(!inserted && !HyphenationParser.EN_DASH.equals(breakCharacter))
				throw new LinterException(DUPLICATED_LINE.format(new Object[]{line}));
		}
		return wordBreakCharacters;
	}

}
