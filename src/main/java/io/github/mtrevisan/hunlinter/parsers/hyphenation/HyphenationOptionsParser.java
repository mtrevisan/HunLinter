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
package io.github.mtrevisan.hunlinter.parsers.hyphenation;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class HyphenationOptionsParser{

	/** minimal hyphenation distance from the left word end. */
	private static final String MIN_LEFT_HYPHENATION = "LEFTHYPHENMIN";
	/** minimal hyphenation distance from the right word end. */
	private static final String MIN_RIGHT_HYPHENATION = "RIGHTHYPHENMIN";
	/** minimal hyphenation distance from the left compound word boundary. */
	private static final String MIN_COMPOUND_LEFT_HYPHENATION = "COMPOUNDLEFTHYPHENMIN";
	/** minimal hyphenation distance from the right compound word boundary. */
	private static final String MIN_COMPOUND_RIGHT_HYPHENATION = "COMPOUNDRIGHTHYPHENMIN";
	/** comma separated list of characters or character sequences with forbidden hyphenation. */
	private static final String NO_HYPHEN = "NOHYPHEN";

	private static final String NO_HYPHEN_SEPARATOR = ",";

	private static final char[] NEW_LINE = {'\n'};


	private final HyphenationOptions nonCompoundOptions = new HyphenationOptions(2);
	private final HyphenationOptions compoundOptions = new HyphenationOptions(0);
	private final Set<String> noHyphen = new HashSet<>(0);


	public final HyphenationOptions getNonCompoundOptions(){
		return nonCompoundOptions;
	}

	public final HyphenationOptions getCompoundOptions(){
		return compoundOptions;
	}

	public final Set<String> getNoHyphen(){
		return noHyphen;
	}

	public final void clear(){
		nonCompoundOptions.clear();
		compoundOptions.clear();
		noHyphen.clear();
	}

	public final boolean parseLine(final String line){
		boolean managed = true;
		if(line.startsWith(MIN_LEFT_HYPHENATION))
			nonCompoundOptions.setLeftMin(Integer.parseInt(extractValue(line)));
		else if(line.startsWith(MIN_RIGHT_HYPHENATION))
			nonCompoundOptions.setRightMin(Integer.parseInt(extractValue(line)));
		else if(line.startsWith(MIN_COMPOUND_LEFT_HYPHENATION))
			compoundOptions.setLeftMin(Integer.parseInt(extractValue(line)));
		else if(line.startsWith(MIN_COMPOUND_RIGHT_HYPHENATION))
			compoundOptions.setRightMin(Integer.parseInt(extractValue(line)));
		else if(line.startsWith(NO_HYPHEN))
			noHyphen.addAll(Arrays.asList(StringUtils.split(extractValue(line), NO_HYPHEN_SEPARATOR)));
		else
			managed = false;
		return managed;
	}

	private String extractValue(final String line){
		final String[] components = StringUtils.split(line);
		return components[1].trim();
	}

	public final void write(final BufferedWriter writer) throws IOException{
		if(nonCompoundOptions.getLeftMin() != nonCompoundOptions.getMinDefault())
			writeValue(writer, MIN_LEFT_HYPHENATION, nonCompoundOptions.getLeftMin());
		if(nonCompoundOptions.getRightMin() != nonCompoundOptions.getMinDefault())
			writeValue(writer, MIN_RIGHT_HYPHENATION, nonCompoundOptions.getRightMin());
		if(compoundOptions.getLeftMin() != compoundOptions.getMinDefault())
			writeValue(writer, MIN_COMPOUND_LEFT_HYPHENATION, compoundOptions.getLeftMin());
		if(compoundOptions.getRightMin() != compoundOptions.getMinDefault())
			writeValue(writer, MIN_COMPOUND_RIGHT_HYPHENATION, compoundOptions.getRightMin());
		if(!noHyphen.isEmpty())
			writeValue(writer, NO_HYPHEN, StringUtils.join(noHyphen, NO_HYPHEN_SEPARATOR));
	}

	private void writeValue(final BufferedWriter writer, final String key, final int value) throws IOException{
		writeValue(writer, key, Integer.toString(value));
	}

	private void writeValue(final BufferedWriter writer, final String key, final String value) throws IOException{
		writer.write(key);
		writer.write(StringUtils.SPACE);
		writer.write(value);
		writer.write(NEW_LINE);
	}

}
