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
package io.github.mtrevisan.hunlinter.parsers.affix.strategies;

import io.github.mtrevisan.hunlinter.services.RegexHelper;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;


/**
 * Implementation of {@link FlagParsingStrategy} that assumes each flag is encoded as two ASCII characters whose codes
 * must be combined into a single character.
 */
final class DoubleASCIIParsingStrategy extends FlagParsingStrategy{

	private static final String BAD_FORMAT = "Each flag should be in {} encoding: `{}`";
	private static final String FLAG_MUST_BE_EVEN_IN_LENGTH = "Flag must be even number of characters: `{}`";
	private static final String FLAG_MUST_BE_OF_LENGTH_TWO = "Flag must be of length two: `{}`";
	private static final String BAD_FORMAT_COMPOUND_RULE = "Compound rule must be composed by double-characters flags in {} encoding, or the optional operators '*' or '?': `{}`";

	private static final Pattern PATTERN = RegexHelper.pattern("(?<=\\G.{2})");

	private static final Pattern COMPOUND_RULE_SPLITTER = RegexHelper.pattern("\\((..)\\)|([?*])");

	private static class SingletonHelper{
		private static final DoubleASCIIParsingStrategy INSTANCE = new DoubleASCIIParsingStrategy();
	}


	public static DoubleASCIIParsingStrategy getInstance(){
		return SingletonHelper.INSTANCE;
	}

	private DoubleASCIIParsingStrategy(){}

	@Override
	public Character[] parseFlags(final String rawFlags){
		if(StringUtils.isBlank(rawFlags))
			return null;

		if(rawFlags.length() % 2 != 0)
			throw new LinterException(FLAG_MUST_BE_EVEN_IN_LENGTH, rawFlags);

		if(!canEncode(rawFlags))
			throw new LinterException(BAD_FORMAT, StandardCharsets.US_ASCII.displayName(), rawFlags);

		final Character[] flags = extractFlags(rawFlags);

		checkForDuplicates(flags);

		return flags;
	}

	private static Character[] extractFlags(final CharSequence rawFlags){
		return RegexHelper.split(rawFlags, PATTERN);
	}

	@Override
	public void validate(final Character flag){
		if(flag == null || ((flag & 0xFF00) != 0? 2: 1) != 2)
			throw new LinterException(FLAG_MUST_BE_OF_LENGTH_TWO, flag);
		if(!canEncode(Character.toString(flag)))
			throw new LinterException(BAD_FORMAT, StandardCharsets.US_ASCII.displayName(), flag);
	}

	@Override
	public Character[] extractCompoundRule(final String compoundRule){
		final Character[] parts = RegexHelper.extract(compoundRule, COMPOUND_RULE_SPLITTER);

		checkCompoundValidity(parts, compoundRule);

		return parts;
	}

	private static void checkCompoundValidity(final Character[] parts, final CharSequence compoundRule){
		for(int i = 0; i < parts.length; i ++){
			final Character part = parts[i];
			final int size = ((part & 0xFF00) != 0? 2: 1);
			final boolean isFlag = (size != 1
				|| !FlagParsingStrategy.FLAG_OPTIONAL.equals(part) && !FlagParsingStrategy.FLAG_ANY.equals(part));
			if(size != 2 && isFlag || !canEncode(compoundRule))
				throw new LinterException(BAD_FORMAT_COMPOUND_RULE, StandardCharsets.US_ASCII.displayName(), compoundRule);
		}
	}

	private static boolean canEncode(final CharSequence cs){
		final CharsetEncoder encoder = StandardCharsets.US_ASCII.newEncoder();
		//NOTE: encoder.canEncode is not thread-safe!
		return encoder.canEncode(cs);
	}

}
