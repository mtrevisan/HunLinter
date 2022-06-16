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
package io.github.mtrevisan.hunlinter.parsers.affix.strategies;

import io.github.mtrevisan.hunlinter.services.RegexHelper;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.regex.Pattern;


/**
 * Implementation of {@link FlagParsingStrategy} that assumes each flag is encoded in its numerical form. In the case
 * of multiple flags, each number is separated by a comma.
 */
final class NumericalParsingStrategy extends FlagParsingStrategy{

	private static final String FLAG_MUST_BE_IN_RANGE = "Flag must be in the range [1, {}]: `{}`";
	private static final String BAD_FORMAT = "Flag must be an integer number: `{}`";
	private static final String BAD_FORMAT_COMPOUND_RULE = "Compound rule must be composed by numbers and the optional operators '" + FlagParsingStrategy.FLAG_OPTIONAL + "' or '" + FlagParsingStrategy.FLAG_ANY + "': `{}`";


	private static final int MAX_NUMERICAL_FLAG = 65_535;

	private static final String COMMA = ",";

	private static final Pattern COMPOUND_RULE_SPLITTER = RegexHelper.pattern("\\((\\d+)\\)|([?*])");

	private static class SingletonHelper{
		private static final NumericalParsingStrategy INSTANCE = new NumericalParsingStrategy();
	}


	public static NumericalParsingStrategy getInstance(){
		return SingletonHelper.INSTANCE;
	}

	private NumericalParsingStrategy(){}

	@Override
	public String[] parseFlags(final String rawFlags){
		if(StringUtils.isBlank(rawFlags))
			return null;

		final String[] flags = extractFlags(rawFlags);

		checkForDuplicates(flags);

		final int size = (flags != null? flags.length: 0);
		for(int i = 0; i < size; i ++)
			validate(flags[i]);

		return flags;
	}

	private static String[] extractFlags(final String rawFlags){
		return StringUtils.split(rawFlags, COMMA);
	}

	@Override
	public void validate(final String flag){
		try{
			final int numericalFlag = Integer.parseInt(flag);
			if(numericalFlag <= 0 || numericalFlag > MAX_NUMERICAL_FLAG)
				throw new LinterException(FLAG_MUST_BE_IN_RANGE, MAX_NUMERICAL_FLAG, flag);
		}
		catch(final NumberFormatException nfe){
			throw new LinterException(nfe, BAD_FORMAT, flag);
		}
	}

	@Override
	public String joinFlags(final String[] flags, final int size){
		return joinFlags(flags, size, COMMA);
	}

	@Override
	public String[] extractCompoundRule(final String compoundRule){
		final String[] parts = RegexHelper.extract(compoundRule, COMPOUND_RULE_SPLITTER);

		checkCompoundValidity(parts, compoundRule);

		return parts;
	}

	private static void checkCompoundValidity(final String[] parts, final String compoundRule){
		for(final String part : parts){
			final boolean isNumber = (part.length() != 1
				|| !FlagParsingStrategy.FLAG_OPTIONAL.equals(part) && !FlagParsingStrategy.FLAG_ANY.equals(part));
			if(isNumber && !NumberUtils.isCreatable(part))
				throw new LinterException(BAD_FORMAT_COMPOUND_RULE, compoundRule);
		}
	}

}
