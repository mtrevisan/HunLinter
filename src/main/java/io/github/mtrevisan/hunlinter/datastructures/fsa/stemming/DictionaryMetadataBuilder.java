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
package io.github.mtrevisan.hunlinter.datastructures.fsa.stemming;

import io.github.mtrevisan.hunlinter.datastructures.fsa.builders.DictionaryAttribute;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Helper class to build {@link DictionaryMetadata} instances.
 *
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public class DictionaryMetadataBuilder{

	private final EnumMap<DictionaryAttribute, String> attrs = new EnumMap<>(DictionaryAttribute.class);

	public final DictionaryMetadataBuilder separator(final char c){
		attrs.put(DictionaryAttribute.SEPARATOR, Character.toString(c));
		return this;
	}

	public final DictionaryMetadataBuilder encoding(final Charset charset){
		return encoding(charset.name());
	}

	public final DictionaryMetadataBuilder encoding(final String charsetName){
		attrs.put(DictionaryAttribute.ENCODING, charsetName);
		return this;
	}

	public final DictionaryMetadataBuilder frequencyIncluded(){ return frequencyIncluded(true); }

	public final DictionaryMetadataBuilder frequencyIncluded(final boolean v){
		attrs.put(DictionaryAttribute.FREQUENCY_INCLUDED, Boolean.toString(v));
		return this;
	}

	public final DictionaryMetadataBuilder ignorePunctuation(){ return ignorePunctuation(true); }

	public final DictionaryMetadataBuilder ignorePunctuation(final boolean v){
		attrs.put(DictionaryAttribute.IGNORE_PUNCTUATION, Boolean.toString(v));
		return this;
	}

	public final DictionaryMetadataBuilder ignoreNumbers(){ return ignoreNumbers(true); }

	public final DictionaryMetadataBuilder ignoreNumbers(final boolean v){
		attrs.put(DictionaryAttribute.IGNORE_NUMBERS, Boolean.toString(v));
		return this;
	}

	public final DictionaryMetadataBuilder ignoreCamelCase(){ return ignoreCamelCase(true); }

	public final DictionaryMetadataBuilder ignoreCamelCase(final boolean v){
		attrs.put(DictionaryAttribute.IGNORE_CAMEL_CASE, Boolean.toString(v));
		return this;
	}

	public final DictionaryMetadataBuilder ignoreAllUppercase(){ return ignoreAllUppercase(true); }

	public final DictionaryMetadataBuilder ignoreAllUppercase(final boolean v){
		attrs.put(DictionaryAttribute.IGNORE_ALL_UPPERCASE, Boolean.toString(v));
		return this;
	}

	public final DictionaryMetadataBuilder ignoreDiacritics(){ return ignoreDiacritics(true); }

	public final DictionaryMetadataBuilder ignoreDiacritics(final boolean v){
		attrs.put(DictionaryAttribute.IGNORE_DIACRITICS, Boolean.toString(v));
		return this;
	}

	public final DictionaryMetadataBuilder convertCase(){ return convertCase(true); }

	public final DictionaryMetadataBuilder convertCase(final boolean v){
		attrs.put(DictionaryAttribute.CONVERT_CASE, Boolean.toString(v));
		return this;
	}

	public final DictionaryMetadataBuilder supportRunOnWords(){ return supportRunOnWords(true); }

	public final DictionaryMetadataBuilder supportRunOnWords(final boolean v){
		attrs.put(DictionaryAttribute.RUN_ON_WORDS, Boolean.toString(v));
		return this;
	}

	public final DictionaryMetadataBuilder encoder(final EncoderType type){
		attrs.put(DictionaryAttribute.ENCODER, type.name());
		return this;
	}

	public final DictionaryMetadataBuilder locale(final Locale locale){
		return locale(locale.toString());
	}

	public final DictionaryMetadataBuilder locale(final String localeName){
		attrs.put(DictionaryAttribute.LOCALE, localeName);
		return this;
	}

	public final DictionaryMetadataBuilder withReplacementPairs(final Map<String, List<String>> replacementPairs){
		final StringBuilder builder = new StringBuilder();
		for(final Map.Entry<String, List<String>> e : replacementPairs.entrySet()){
			final String k = e.getKey();
			for(final String v : e.getValue()){
				if(!builder.isEmpty())
					builder.append(", ");
				builder.append(k).append(StringUtils.SPACE).append(v);
			}
		}
		attrs.put(DictionaryAttribute.REPLACEMENT_PAIRS, builder.toString());
		return this;
	}

	public final DictionaryMetadataBuilder withEquivalentChars(final Map<Character, List<Character>> equivalentChars){
		final StringBuilder builder = new StringBuilder();
		for(final Map.Entry<Character, List<Character>> e : equivalentChars.entrySet()){
			final Character k = e.getKey();
			for(final Character v : e.getValue()){
				if(!builder.isEmpty())
					builder.append(", ");
				builder.append(k).append(StringUtils.SPACE).append(v);
			}
		}
		attrs.put(DictionaryAttribute.EQUIVALENT_CHARS, builder.toString());
		return this;
	}

	public final DictionaryMetadataBuilder withInputConversionPairs(final Map<String, String> conversionPairs){
		attrs.put(DictionaryAttribute.INPUT_CONVERSION, getConversionPairs(conversionPairs));
		return this;
	}

	public final DictionaryMetadataBuilder withOutputConversionPairs(final Map<String, String> conversionPairs){
		attrs.put(DictionaryAttribute.OUTPUT_CONVERSION, getConversionPairs(conversionPairs));
		return this;
	}

	private static String getConversionPairs(final Map<String, String> conversionPairs){
		final StringBuilder builder = new StringBuilder();
		for(final Map.Entry<String, String> e : conversionPairs.entrySet()){
			final String k = e.getKey();
			if(!builder.isEmpty())
				builder.append(", ");
			builder.append(k).append(StringUtils.SPACE).append(conversionPairs.get(k));
		}
		return builder.toString();
	}


	public final DictionaryMetadataBuilder author(final String author){
		attrs.put(DictionaryAttribute.AUTHOR, author);
		return this;
	}

	public final DictionaryMetadataBuilder creationDate(final String creationDate){
		attrs.put(DictionaryAttribute.CREATION_DATE, creationDate);
		return this;
	}

	public final DictionaryMetadataBuilder license(final String license){
		attrs.put(DictionaryAttribute.LICENSE, license);
		return this;
	}

	public final DictionaryMetadata build(){
		return new DictionaryMetadata(attrs);
	}

	public final Map<DictionaryAttribute, String> toMap(){
		return new EnumMap<>(attrs);
	}

}
