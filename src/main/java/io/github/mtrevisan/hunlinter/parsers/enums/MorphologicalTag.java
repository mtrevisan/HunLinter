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
package io.github.mtrevisan.hunlinter.parsers.enums;


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


	private final String code;
	private final int hash;


	MorphologicalTag(final String code){
		this.code = code;
		hash = partialHash(code);
	}

	public static MorphologicalTag createFromCode(final String code){
		final int hash = partialHash(code);
		for(final MorphologicalTag tag : values())
			if(tag.hash == hash)
				return tag;
		return null;
	}

	public String getCode(){
		return code;
	}

	public boolean isSupertypeOf(final String codeAndValue){
		return (partialHash(codeAndValue) == hash);
	}

	public String attachValue(final String value){
		return code + value;
	}

	private static int partialHash(final String key){
		return (key.codePointAt(0) << 8) | key.codePointAt(1);
	}

}
