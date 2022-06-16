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

import io.github.mtrevisan.hunlinter.datastructures.SetHelper;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/** Abstraction of the process of parsing flags taken from the affix and dic files. */
public abstract class FlagParsingStrategy{

	/** Represents a '?' character in a compound rule. */
	public static final String FLAG_OPTIONAL = "?";
	/** Represents a '*' character in a compound rule. */
	public static final String FLAG_ANY = "*";

	private static final String DUPLICATED_FLAG = "Flags must not be duplicated: {}";


	public abstract void validate(final String flag);

	/**
	 * Parses the given String into multiple flags
	 *
	 * @param rawFlags	String to parse into flags
	 * @return Parsed flags
	 */
	public abstract String[] parseFlags(final String rawFlags);

	protected static void checkForDuplicates(final String[] flags){
		final Set<String> notDuplicatedFlags = new HashSet<>(Arrays.asList(flags));
		if(notDuplicatedFlags.size() < flags.length){
			final Set<String> duplicates = SetHelper.getDuplicates(flags);
			if(!duplicates.isEmpty())
				throw new LinterException(DUPLICATED_FLAG, String.join(", ", duplicates));
		}
	}


	public final String joinFlags(final List<String> flags){
		if(flags == null || flags.isEmpty())
			return StringUtils.EMPTY;

		final int size = flags.size();
		for(int i = 0; i < size; i ++)
			validate(flags.get(i));

		final StringBuilder sb = new StringBuilder(flags.get(0).length() * size);
		for(int i = 0; i < size; i ++)
			sb.append(flags.get(i));
		return sb.toString();
	}

	/**
	 * Compose the given array of String into one flag stream
	 *
	 * @param flags	Array of String to compose into flags
	 * @return Composed flags
	 */
	public final String joinFlags(final String[] flags){
		return joinFlags(flags, (flags != null? flags.length: 0));
	}

	@SuppressWarnings("DesignForExtension")
	public String joinFlags(final String[] flags, final int size){
		return joinFlags(flags, size, StringUtils.EMPTY);
	}

	protected final String joinFlags(final String[] flags, final int size, final String flagSeparator){
		if(flags == null || size == 0)
			return StringUtils.EMPTY;

		for(int i = 0; i < size; i ++)
			validate(flags[i]);

		return StringUtils.join(flags, flagSeparator);
	}

	/**
	 * Extract each rule from a compound rule ("a*bc?" into ["a", "*", "b", "c", "?"])
	 *
	 * @param compoundRule	String to parse into flags
	 * @return Parsed flags
	 */
	public abstract String[] extractCompoundRule(final String compoundRule);

}
