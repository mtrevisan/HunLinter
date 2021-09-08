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
package io.github.mtrevisan.hunlinter.parsers.affix;

import io.github.mtrevisan.hunlinter.parsers.enums.AffixOption;
import io.github.mtrevisan.hunlinter.services.ParserHelper;
import io.github.mtrevisan.hunlinter.services.RegexHelper;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringJoiner;
import java.util.regex.Pattern;


public class RelationTable{

	private static final String BAD_FIRST_PARAMETER = "Error reading line `{}`: the first parameter is not a number";
	private static final String BAD_NUMBER_OF_ENTRIES = "Error reading line `{}`: bad number of entries, `{}` must be a positive integer less or equal than " + Short.MAX_VALUE;
	private static final String WRONG_FORMAT = "Error reading line `{}`: bad number of entries, it must be '<option> <substitutions>'";
	private static final String BAD_OPTION = "Error reading line `{}`: bad option, it must be {}";

	//aß(ss) > a, ß, ss
	private static final Pattern PATTERN = RegexHelper.pattern("(?<!\\()(?![^\\(]*\\))");


	private final AffixOption affixOption;
	private List<String[]> table;


	public RelationTable(final AffixOption affixOption){
		this.affixOption = affixOption;
	}

	public final void parse(final ParsingContext context){
		try{
			final Scanner scanner = context.getScanner();
			if(!NumberUtils.isCreatable(context.getFirstParameter()))
				throw new LinterException(BAD_FIRST_PARAMETER, context);
			final int numEntries = Integer.parseInt(context.getFirstParameter());
			if(numEntries <= 0 || numEntries > Short.MAX_VALUE)
				throw new LinterException(BAD_NUMBER_OF_ENTRIES, context, context.getFirstParameter());

			table = new ArrayList<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				ParserHelper.assertNotEOF(scanner);

				final String line = scanner.nextLine();
				final String[] parts = StringUtils.split(line);

				checkValidity(parts, context);

				final String[] substitutions = RegexHelper.split(parts[1], PATTERN);
				for(int j = 0; j < substitutions.length; j ++){
					final String substitution = substitutions[j];
					if(substitution.length() > 1)
						substitutions[j] = substitution.substring(1, substitution.length() - 1);
				}
				table.add(substitutions);
			}
		}
		catch(final IOException e){
			throw new RuntimeException(e.getMessage());
		}
	}

	private void checkValidity(final String[] parts, final ParsingContext context){
		if(parts.length != 2)
			throw new LinterException(WRONG_FORMAT, context);
		if(!affixOption.is(parts[0]))
			throw new LinterException(BAD_OPTION, context, affixOption.getCode());
	}

	public final String extractAsList(){
		final StringJoiner sj = new StringJoiner(", ");
		for(final String[] list : table){
			final String base = list[0];
			for(int i = 1; i < list.length; i ++)
				sj.add(base + StringUtils.SPACE + list[i]);
		}
		return sj.toString();
	}

	@Override
	public final String toString(){
		return "[affixOption=" + affixOption + ',' + "table=" + table + ']';
	}

}
