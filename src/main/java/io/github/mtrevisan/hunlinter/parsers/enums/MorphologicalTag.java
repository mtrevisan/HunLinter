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
package io.github.mtrevisan.hunlinter.parsers.enums;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;


/** Default morphological fields. */
public enum MorphologicalTag{

	STEM("st:"),
	ALLOMORPH("al:"),
	PART_OF_SPEECH("po:"),

	DERIVATIONAL_PREFIX("dp:"),
	INFLECTIONAL_PREFIX("ip:"),
	TERMINAL_PREFIX("tp:"),

	DERIVATIONAL_SUFFIX("ds:"),
	INFLECTIONAL_SUFFIX("is:"),
	TERMINAL_SUFFIX("ts:"),

	SURFACE_PREFIX("sp:"),

	FREQUENCY("fr:"),
	PHONETIC("ph:"),
	HYPHENATION("hy:"),
	PART("pa:"),
	FLAG("fl:");


	private static final Map<String, MorphologicalTag> VALUES = new HashMap<>(values().length);
	static{
		for(final MorphologicalTag tag : EnumSet.allOf(MorphologicalTag.class))
			VALUES.put(tag.code, tag);
	}

	private final String code;


	MorphologicalTag(final String code){
		this.code = code;
	}

	public static MorphologicalTag createFromCode(final String code){
		return VALUES.get(code.substring(0, 3));
	}

	public String getCode(){
		return code;
	}

	public boolean isSupertypeOf(final String codeAndValue){
		return codeAndValue.startsWith(code);
	}

	public String attachValue(final String value){
		return code + value;
	}

}
