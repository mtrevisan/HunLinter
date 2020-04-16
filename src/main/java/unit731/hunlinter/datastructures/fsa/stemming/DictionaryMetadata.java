package unit731.hunlinter.datastructures.fsa.stemming;

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
import java.util.Properties;


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
	protected byte separator;

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
	public final static String METADATA_FILE_EXTENSION = "info";


	/**
	 * Read dictionary metadata from a property file (stream).
	 *
	 * @param metadataStream The stream with metadata.
	 * @return Returns {@link DictionaryMetadata} read from a the stream (property file).
	 * @throws IOException Thrown if an I/O exception occurs.
	 */
	public static DictionaryMetadata read(final InputStream metadataStream) throws IOException{
		final Map<DictionaryAttribute, String> map = new EnumMap<>(DictionaryAttribute.class);
		final Properties properties = new Properties();
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
	public DictionaryMetadata(final Map<DictionaryAttribute, String> attributes){
		this.attributes = attributes;

		final EnumMap<DictionaryAttribute, String> allAttributes = new EnumMap<>(DEFAULT_ATTRIBUTES);
		allAttributes.putAll(attributes);

		for(final DictionaryAttribute attr : REQUIRED_ATTRIBUTES)
			if(!allAttributes.containsKey(attr))
				throw new IllegalArgumentException("At least one required attributes was not provided: " + attr.propertyName);

		//convert some attributes from the map to local fields for performance reasons:
		readEncoding(allAttributes);

		for(final Map.Entry<DictionaryAttribute, String> e : allAttributes.entrySet()){
			//run validation and conversion on all of them
			final Object value = e.getKey().fromString(e.getValue());
			//just run validation
			switch(e.getKey()){
				case SEPARATOR -> {
					final char separatorChar = (Character)value;
					final byte[] separatorBytes = String.valueOf(separatorChar).getBytes(charset);
					if(separatorBytes.length > 1)
						throw new IllegalArgumentException("Separator character is not a single byte in encoding " + charset.name()
							+ ": " + separatorChar);

					this.separator = separatorBytes[0];
				}
				case LOCALE -> this.locale = (Locale)value;
				case ENCODING -> {}
				case ENCODER -> this.encoderType = (EncoderType)value;
				case INPUT_CONVERSION -> //noinspection unchecked
					this.inputConversion = (Map<String, String>)value;
				case OUTPUT_CONVERSION -> //noinspection unchecked
					this.outputConversion = (Map<String, String>)value;
				case REPLACEMENT_PAIRS -> //noinspection unchecked
					this.replacementPairs = (Map<String, List<String>>)value;
				case EQUIVALENT_CHARS -> //noinspection unchecked
					this.equivalentChars = (Map<Character, List<Character>>)value;
				case IGNORE_PUNCTUATION, IGNORE_NUMBERS, IGNORE_CAMEL_CASE, IGNORE_ALL_UPPERCASE, IGNORE_DIACRITICS, CONVERT_CASE, RUN_ON_WORDS, FREQUENCY_INCLUDED -> this.boolAttributes.put(e.getKey(), (Boolean)value);
				case AUTHOR, LICENSE, CREATION_DATE -> e.getKey().fromString(e.getValue());
				default -> throw new RuntimeException("Unexpected code path (attribute should be handled but is not): " + e.getKey());
			}
		}
	}

	private void readEncoding(final Map<DictionaryAttribute, String> attributes){
		final String encoding = attributes.get(DictionaryAttribute.ENCODING);
		if(!Charset.isSupported(encoding))
			throw new IllegalArgumentException("Encoding not supported on this JVM: " + encoding);

		charset = (Charset)DictionaryAttribute.ENCODING.fromString(encoding);
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

	/**
	 * @return Return sequence encoder type.
	 */
	public EncoderType getSequenceEncoderType(){
		return encoderType;
	}

	/**
	 * @return A shortcut returning {@link DictionaryMetadataBuilder}.
	 */
	public static DictionaryMetadataBuilder builder(){
		return new DictionaryMetadataBuilder();
	}

	/**
	 * Returns the expected name of the metadata file, based on the name of the
	 * dictionary file. The expected name is resolved by truncating any
	 * file extension of <code>name</code> and appending
	 * {@link DictionaryMetadata#METADATA_FILE_EXTENSION}.
	 *
	 * @param dictionaryFile The name of the dictionary (<code>*.dict</code>) file.
	 * @return Returns the expected name of the metadata file.
	 */
	public static String getExpectedMetadataFileName(final String dictionaryFile){
		final int dotIndex = dictionaryFile.lastIndexOf('.');
		final String featuresName;
		if(dotIndex >= 0)
			featuresName = dictionaryFile.substring(0, dotIndex) + "." + METADATA_FILE_EXTENSION;
		else
			featuresName = dictionaryFile + "." + METADATA_FILE_EXTENSION;
		return featuresName;
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
	 * @param writer The writer to write to.
	 * @throws IOException Thrown when an I/O error occurs.
	 */
	public void write(final Writer writer) throws IOException{
		final Properties properties = new Properties();
		for(final Map.Entry<DictionaryAttribute, String> e : getAttributes().entrySet())
			properties.setProperty(e.getKey().propertyName, e.getValue());
		properties.store(writer, "# " + getClass().getName());
	}

}
