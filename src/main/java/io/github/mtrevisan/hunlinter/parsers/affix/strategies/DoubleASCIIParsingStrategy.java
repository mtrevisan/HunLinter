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
import java.text.MessageFormat;
import java.util.regex.Pattern;


/**
 * Implementation of {@link FlagParsingStrategy} that assumes each flag is encoded as two ASCII characters whose codes
 * must be combined into a single character.
 */
final class DoubleASCIIParsingStrategy extends FlagParsingStrategy{

	private static final MessageFormat BAD_FORMAT = new MessageFormat("Each flag should be in {0} encoding: `{1}`");
	private static final MessageFormat FLAG_MUST_BE_EVEN_IN_LENGTH = new MessageFormat("Flag must be even number of characters: `{0}`");
	private static final MessageFormat FLAG_MUST_BE_OF_LENGTH_TWO = new MessageFormat("Flag must be of length two: `{0}`");
	private static final MessageFormat BAD_FORMAT_COMPOUND_RULE = new MessageFormat("Compound rule must be composed by double-characters flags in {0} encoding, or the optional operators '*' or '?': `{1}`");

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
	public String[] parseFlags(final String rawFlags){
		if(StringUtils.isBlank(rawFlags))
			return null;

		if(rawFlags.length() % 2 != 0)
			throw new LinterException(FLAG_MUST_BE_EVEN_IN_LENGTH.format(new Object[]{rawFlags}));

		if(!canEncode(rawFlags))
			throw new LinterException(BAD_FORMAT.format(new Object[]{StandardCharsets.US_ASCII.displayName(), rawFlags}));

		final String[] flags = extractFlags(rawFlags);

		checkForDuplicates(flags);

		return flags;
	}

	private String[] extractFlags(final CharSequence rawFlags){
		return RegexHelper.split(rawFlags, PATTERN);
	}

	@Override
	public void validate(final String flag){
		if(flag == null || flag.length() != 2)
			throw new LinterException(FLAG_MUST_BE_OF_LENGTH_TWO.format(new Object[]{flag}));
		if(!canEncode(flag))
			throw new LinterException(BAD_FORMAT.format(new Object[]{StandardCharsets.US_ASCII.displayName(), flag}));
	}

	@Override
	public String[] extractCompoundRule(final String compoundRule){
		final String[] parts = RegexHelper.extract(compoundRule, COMPOUND_RULE_SPLITTER);

		checkCompoundValidity(parts, compoundRule);

		return parts;
	}

	private void checkCompoundValidity(final String[] parts, final CharSequence compoundRule){
		for(final String part : parts){
			final int size = part.length();
			final boolean isFlag = (size != 1
				|| !FlagParsingStrategy.FLAG_OPTIONAL.equals(part) && !FlagParsingStrategy.FLAG_ANY.equals(part));
			if(size != 2 && isFlag || !canEncode(compoundRule))
				throw new LinterException(BAD_FORMAT_COMPOUND_RULE.format(new Object[]{StandardCharsets.US_ASCII.displayName(), compoundRule}));
		}
	}

	private boolean canEncode(final CharSequence cs){
		final CharsetEncoder encoder = StandardCharsets.US_ASCII.newEncoder();
		//NOTE: encoder.canEncode is not thread-safe!
		return encoder.canEncode(cs);
	}

}
