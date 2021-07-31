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
package io.github.mtrevisan.hunlinter.datastructures.fsa.stemming;

import io.github.mtrevisan.hunlinter.services.system.PropertiesUTF8;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Description of attributes, their types and default values
 *
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public class DictionaryMetadata{

	/** Default attribute values */
	private static final Map<DictionaryAttribute, String> DEFAULT_ATTRIBUTES = new DictionaryMetadataBuilder()
		.frequencyIncluded(false)
		.ignorePunctuation()
		.ignoreNumbers()
		.ignoreCamelCase()
		.ignoreAllUppercase()
		.ignoreDiacritics()
		.convertCase()
		.supportRunOnWords()
		.toMap();

	/** Required attributes */
	private static final EnumSet<DictionaryAttribute> REQUIRED_ATTRIBUTES =
		EnumSet.of(DictionaryAttribute.SEPARATOR, DictionaryAttribute.ENCODER, DictionaryAttribute.ENCODING);


	/**
	 * A separator character between fields (stem, lemma, form).
	 * The character must be within byte range (FSA uses bytes internally).
	 */
	private byte separator;

	private Locale locale = Locale.ROOT;

	private Charset charset;

	/** All attributes */
	private final Map<DictionaryAttribute, String> attributes;

	/** Replacement pairs for non-obvious candidate search in a speller dictionary */
	private Map<String, List<String>> replacementPairs = new HashMap<>();
	/** Conversion pairs for input conversion, for example to replace ligatures */
	private Map<String, String> inputConversion = new HashMap<>();
	/** Conversion pairs for output conversion, for example to replace ligatures */
	private Map<String, String> outputConversion = new HashMap<>();
	/**
	 * Equivalent characters (treated similarly as equivalent chars with and without
	 * diacritics). For example, Polish <tt>ł</tt> can be specified as equivalent to <tt>l</tt>.
	 * <p>
	 * This implements a feature similar to hunspell MAP in the affix file.
	 */
	private Map<Character, List<Character>> equivalentChars = new HashMap<>();

	/** All "enabled" boolean attributes */
	private final EnumMap<DictionaryAttribute, Boolean> boolAttributes = new EnumMap<>(DictionaryAttribute.class);

	/** Sequence encoder */
	private EncoderType encoderType;

	/** Expected metadata file extension */
	private static final String METADATA_FILE_EXTENSION = "info";


	/**
	 * Read dictionary metadata from a property file (stream).
	 *
	 * @param metadataStream The stream with metadata.
	 * @return Returns {@link DictionaryMetadata} read from a the stream (property file).
	 * @throws IOException Thrown if an I/O exception occurs.
	 */
	public static DictionaryMetadata read(final InputStream metadataStream) throws IOException{
		final Map<DictionaryAttribute, String> map = new EnumMap<>(DictionaryAttribute.class);
		final PropertiesUTF8 properties = new PropertiesUTF8();
		properties.load(new InputStreamReader(metadataStream, StandardCharsets.UTF_8));

		//handle back-compatibility for encoder specification
		if(!properties.containsKey(DictionaryAttribute.ENCODER.propertyName)){
			final boolean hasDeprecated = properties.containsKey("fsa.dict.uses-suffixes")
				|| properties.containsKey("fsa.dict.uses-infixes") || properties.containsKey("fsa.dict.uses-prefixes");
			final boolean usesSuffixes = Boolean.parseBoolean(properties.getProperty("fsa.dict.uses-suffixes", "true"));
			final boolean usesPrefixes = Boolean.parseBoolean(properties.getProperty("fsa.dict.uses-prefixes", "false"));
			final boolean usesInfixes = Boolean.parseBoolean(properties.getProperty("fsa.dict.uses-infixes", "false"));

			EncoderType encoder = EncoderType.NONE;
			if(usesInfixes)
				encoder = EncoderType.INFIX;
			else if(usesPrefixes)
				encoder = EncoderType.PREFIX;
			else if(usesSuffixes)
				encoder = EncoderType.SUFFIX;
			if(!hasDeprecated)
				throw new IOException("Use an explicit " + DictionaryAttribute.ENCODER.propertyName + "=" + encoder.name()
					+ " metadata key: ");
			throw new IOException("Deprecated encoder keys in metadata. Use " + DictionaryAttribute.ENCODER.propertyName + "="
				+ encoder.name());
		}

		for(final Enumeration<?> e = properties.propertyNames(); e.hasMoreElements(); ){
			final String key = (String)e.nextElement();
			map.put(DictionaryAttribute.fromPropertyName(key), properties.getProperty(key));
		}

		return new DictionaryMetadata(map);
	}

	/**
	 * Create an instance from an attribute map.
	 *
	 * @param attributes	A set of {@link DictionaryAttribute} keys and their associated values.
	 * @see DictionaryMetadataBuilder
	 */
	@SuppressWarnings("unchecked")
	public DictionaryMetadata(final Map<DictionaryAttribute, String> attributes){
		this.attributes = attributes;

		final EnumMap<DictionaryAttribute, String> allAttributes = new EnumMap<>(DEFAULT_ATTRIBUTES);
		allAttributes.putAll(attributes);

		for(final DictionaryAttribute attr : REQUIRED_ATTRIBUTES)
			if(!allAttributes.containsKey(attr))
				throw new IllegalArgumentException("At least one required attributes was not provided: " + attr.propertyName);

		//convert some attributes from the map to local fields for performance reasons:
		for(final Map.Entry<DictionaryAttribute, String> e : allAttributes.entrySet()){
			//run validation and conversion on all of them
			final Object value = e.getKey().fromString(e.getValue());
			//just run validation
			switch(e.getKey()){
				case SEPARATOR -> separator = (Byte)value;
				case LOCALE -> locale = (Locale)value;
				case ENCODING -> charset = (Charset)value;
				case ENCODER -> encoderType = (EncoderType)value;
				case INPUT_CONVERSION -> inputConversion = (Map<String, String>)value;
				case OUTPUT_CONVERSION -> outputConversion = (Map<String, String>)value;
				case REPLACEMENT_PAIRS -> replacementPairs = (Map<String, List<String>>)value;
				case EQUIVALENT_CHARS -> equivalentChars = (Map<Character, List<Character>>)value;
				case IGNORE_PUNCTUATION, IGNORE_NUMBERS, IGNORE_CAMEL_CASE, IGNORE_ALL_UPPERCASE, IGNORE_DIACRITICS, CONVERT_CASE, RUN_ON_WORDS, FREQUENCY_INCLUDED -> boolAttributes.put(e.getKey(), (Boolean)value);
				case AUTHOR, LICENSE, CREATION_DATE -> e.getKey().fromString(e.getValue());
				default -> throw new RuntimeException("Unexpected code path (attribute should be handled but is not): " + e.getKey());
			}
		}
	}

	/**
	 * @return Return all metadata attributes.
	 */
	public Map<DictionaryAttribute, String> getAttributes(){
		return Collections.unmodifiableMap(attributes);
	}

	public String getEncoding(){ return charset.name(); }

	public byte getSeparator(){ return separator; }

	public Locale getLocale(){ return locale; }

	public Map<String, String> getInputConversionPairs(){ return inputConversion; }

	public Map<String, String> getOutputConversionPairs(){ return outputConversion; }

	public Map<String, List<String>> getReplacementPairs(){ return replacementPairs; }

	public Map<Character, List<Character>> getEquivalentChars(){ return equivalentChars; }

	public boolean isFrequencyIncluded(){ return boolAttributes.get(DictionaryAttribute.FREQUENCY_INCLUDED); }

	public boolean isIgnoringPunctuation(){ return boolAttributes.get(DictionaryAttribute.IGNORE_PUNCTUATION); }

	public boolean isIgnoringNumbers(){ return boolAttributes.get(DictionaryAttribute.IGNORE_NUMBERS); }

	public boolean isIgnoringCamelCase(){ return boolAttributes.get(DictionaryAttribute.IGNORE_CAMEL_CASE); }

	public boolean isIgnoringAllUppercase(){ return boolAttributes.get(DictionaryAttribute.IGNORE_ALL_UPPERCASE); }

	public boolean isIgnoringDiacritics(){ return boolAttributes.get(DictionaryAttribute.IGNORE_DIACRITICS); }

	public boolean isConvertingCase(){ return boolAttributes.get(DictionaryAttribute.CONVERT_CASE); }

	public boolean isSupportingRunOnWords(){ return boolAttributes.get(DictionaryAttribute.RUN_ON_WORDS); }

	public Charset getCharset(){
		return charset;
	}

	public EncoderType getSequenceEncoderType(){
		return encoderType;
	}


	/**
	 * Returns the expected name of the metadata file, based on the name of the
	 * dictionary file. The expected name is resolved by truncating any
	 * file extension of <code>name</code> and appending
	 * {@link DictionaryMetadata#METADATA_FILE_EXTENSION}.
	 *
	 * @param dictionaryFile	The name of the dictionary (<code>*.dict</code>) file.
	 * @return	The expected name of the metadata file.
	 */
	public static String getExpectedMetadataFileName(final String dictionaryFile){
		final int dotIndex = dictionaryFile.lastIndexOf('.');
		return (dotIndex >= 0? dictionaryFile.substring(0, dotIndex): dictionaryFile) + "." + METADATA_FILE_EXTENSION;
	}

	/**
	 * @param dictionary The location of the dictionary file.
	 * @return Returns the expected location of a metadata file.
	 */
	public static Path getExpectedMetadataLocation(final Path dictionary){
		return dictionary.resolveSibling(getExpectedMetadataFileName(dictionary.getFileName().toString()));
	}

	/**
	 * Write dictionary attributes (metadata).
	 *
	 * @param writer	The writer to write to.
	 * @throws IOException	Thrown when an I/O error occurs.
	 */
	public void write(final Writer writer) throws IOException{
		final PropertiesUTF8 properties = new PropertiesUTF8();
		for(final Map.Entry<DictionaryAttribute, String> e : getAttributes().entrySet())
			properties.setProperty(e.getKey().propertyName, e.getValue());
		properties.store(writer, "# " + getClass().getName());
	}

}
