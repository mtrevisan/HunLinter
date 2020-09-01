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
package unit731.hunlinter.parsers.affix.strategies;

import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.workers.exceptions.LinterException;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;


final class CharsetParsingStrategy extends FlagParsingStrategy{

	private static final MessageFormat BAD_FORMAT = new MessageFormat("Each flag should be in {0} encoding: ''{1}''");
	private static final MessageFormat BAD_FORMAT_COMPOUND_RULE = new MessageFormat("Compound rule should be in {0} encoding: was ''{1}''");
	private static final MessageFormat FLAG_MUST_BE_OF_LENGTH_ONE = new MessageFormat("Flag should be of length one and in {0} encoding: was ''{1}''");


	private final Charset charset;


	private static class SingletonHelperASCII{
		private static final CharsetParsingStrategy INSTANCE = new CharsetParsingStrategy(StandardCharsets.US_ASCII);
	}

	private static class SingletonHelperUTF8{
		private static final CharsetParsingStrategy INSTANCE = new CharsetParsingStrategy(StandardCharsets.UTF_8);
	}


	public static CharsetParsingStrategy getASCIIInstance(){
		return SingletonHelperASCII.INSTANCE;
	}

	public static CharsetParsingStrategy getUTF8Instance(){
		return SingletonHelperUTF8.INSTANCE;
	}

	private CharsetParsingStrategy(final Charset charset){
		this.charset = charset;
	}

	@Override
	public String[] parseFlags(final String flags){
		if(StringUtils.isBlank(flags))
			return null;

		if(!canEncode(flags))
			throw new LinterException(BAD_FORMAT.format(new Object[]{charset.displayName(), flags}));

		final String[] singleFlags = extractFlags(flags);

		checkForDuplicates(singleFlags);

		return singleFlags;
	}

	private String[] extractFlags(final CharSequence flags){
		final int size = flags.length();
		final String[] list = new String[size];
		for(int i = 0; i < size; i ++)
			list[i] = String.valueOf(flags.charAt(i));
		return list;
	}

	@Override
	public void validate(final String flag){
		if(flag == null || flag.length() != 1)
			throw new LinterException(FLAG_MUST_BE_OF_LENGTH_ONE.format(new Object[]{charset.displayName(), flag}));
		if(!canEncode(flag))
			throw new LinterException(BAD_FORMAT.format(new Object[]{charset.displayName(), flag}));
	}

	@Override
	public String[] extractCompoundRule(final String compoundRule){
		checkCompoundValidity(compoundRule);

		//NOTE: same as compoundRule.split(StringUtils.EMPTY) but faster
		return extractFlags(compoundRule);
	}

	private void checkCompoundValidity(final CharSequence compoundRule){
		if(!canEncode(compoundRule))
			throw new LinterException(BAD_FORMAT_COMPOUND_RULE.format(new Object[]{charset.displayName(), compoundRule}));
	}

	private boolean canEncode(final CharSequence cs){
		final CharsetEncoder encoder = charset.newEncoder();
		//NOTE: encoder.canEncode is not thread-safe!
		return encoder.canEncode(cs);
	}

}
