package unit731.hunlinter.datastructures.fsa.stemming;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Attributes applying to {@link Dictionary} and {@link DictionaryMetadata}.
 *
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public enum DictionaryAttribute{

	/**
	 * Logical fields separator inside the FSA.
	 */
	SEPARATOR("fsa.dict.separator"){
		@Override
		public Byte fromString(final String separator){
			if(separator == null || separator.length() != 1)
				throw new IllegalArgumentException("Attribute " + propertyName + " must be a single character: "
					+ StringEscapeUtils.escapeJava(separator));

			final char charValue = separator.charAt(0);
			if(Character.isHighSurrogate(charValue) || Character.isLowSurrogate(charValue))
				throw new IllegalArgumentException("Field separator character cannot be part of a surrogate pair: "
					+ StringEscapeUtils.escapeJava(separator));

			return (byte)charValue;
		}
	},

	/**
	 * Character to byte encoding used for strings inside the FSA.
	 */
	ENCODING("fsa.dict.encoding"){
		@Override
		public Charset fromString(final String charsetName){
			if(!Charset.isSupported(charsetName))
				throw new IllegalArgumentException("Encoding not supported on this JVM: " + charsetName);

			return Charset.forName(charsetName);
		}
	},

	/**
	 * If the FSA dictionary includes frequency data.
	 */
	FREQUENCY_INCLUDED("fsa.dict.frequency-included"){
		@Override
		public Boolean fromString(final String value){
			return booleanValue(value);
		}
	},

	/**
	 * If the spelling dictionary is supposed to ignore words containing digits
	 */
	IGNORE_NUMBERS("fsa.dict.speller.ignore-numbers"){
		@Override
		public Boolean fromString(final String value){
			return booleanValue(value);
		}
	},

	/**
	 * If the spelling dictionary is supposed to ignore punctuation.
	 */
	IGNORE_PUNCTUATION("fsa.dict.speller.ignore-punctuation"){
		@Override
		public Boolean fromString(final String value){
			return booleanValue(value);
		}
	},

	/**
	 * If the spelling dictionary is supposed to ignore CamelCase words.
	 */
	IGNORE_CAMEL_CASE("fsa.dict.speller.ignore-camel-case"){
		@Override
		public Boolean fromString(final String value){
			return booleanValue(value);
		}
	},

	/**
	 * If the spelling dictionary is supposed to ignore ALL UPPERCASE words.
	 */
	IGNORE_ALL_UPPERCASE("fsa.dict.speller.ignore-all-uppercase"){
		@Override
		public Boolean fromString(final String value){
			return booleanValue(value);
		}
	},

	/**
	 * If the spelling dictionary is supposed to ignore diacritics, so that
	 * 'a' would be treated as equivalent to 'ą'.
	 */
	IGNORE_DIACRITICS("fsa.dict.speller.ignore-diacritics"){
		@Override
		public Boolean fromString(final String value){
			return booleanValue(value);
		}
	},

	/**
	 * if the spelling dictionary is supposed to treat upper and lower case
	 * as equivalent.
	 */
	CONVERT_CASE("fsa.dict.speller.convert-case"){
		@Override
		public Boolean fromString(final String value){
			return booleanValue(value);
		}
	},

	/**
	 * If the spelling dictionary is supposed to split runOnWords.
	 */
	RUN_ON_WORDS("fsa.dict.speller.runon-words"){
		@Override
		public Boolean fromString(final String value){
			return booleanValue(value);
		}
	},

	/**
	 * Locale associated with the dictionary.
	 */
	LOCALE("fsa.dict.speller.locale"){
		@Override
		public Locale fromString(final String value){
			return new Locale(value);
		}
	},

	/**
	 * Locale associated with the dictionary.
	 */
	ENCODER("fsa.dict.encoder"){
		@Override
		public EncoderType fromString(final String value){
			try{
				return EncoderType.valueOf(value.trim().toUpperCase(Locale.ROOT));
			}catch(final IllegalArgumentException e){
				throw new IllegalArgumentException("Invalid encoder name '" + value.trim() + "', only these coders are valid: " + Arrays.toString(EncoderType.values()));
			}
		}
	},

	/**
	 * Input conversion pairs to replace non-standard characters before search in a speller dictionary.
	 * For example, common ligatures can be replaced here.
	 */
	INPUT_CONVERSION("fsa.dict.input-conversion"){
		@Override
		public Map<String, String> fromString(final String value) throws IllegalArgumentException{
			final Map<String, String> conversionPairs = new HashMap<>();
			final String[] replacements = value.split(",\\s*");
			for(final String stringPair : replacements){
				final String[] twoStrings = stringPair.trim().split(StringUtils.SPACE);
				if(twoStrings.length != 2)
					throw new IllegalArgumentException("Attribute " + propertyName + " is not in the proper format: " + value);

				if(!conversionPairs.containsKey(twoStrings[0]))
					conversionPairs.put(twoStrings[0], twoStrings[1]);
				else
					throw new IllegalArgumentException("Input conversion cannot specify different values for the same input string: " + twoStrings[0]);
			}
			return conversionPairs;
		}
	},

	/**
	 * Output conversion pairs to replace non-standard characters before search in a speller dictionary.
	 * For example, standard characters can be replaced here into ligatures.
	 * <p>
	 * Useful for dictionaries that do have certain standards imposed.
	 */
	OUTPUT_CONVERSION("fsa.dict.output-conversion"){
		@Override
		public Map<String, String> fromString(final String value) throws IllegalArgumentException{
			final Map<String, String> conversionPairs = new HashMap<>();
			final String[] replacements = value.split(",\\s*");
			for(final String stringPair : replacements){
				final String[] twoStrings = stringPair.trim().split(StringUtils.SPACE);
				if(twoStrings.length != 2)
					throw new IllegalArgumentException("Attribute " + propertyName + " is not in the proper format: " + value);

				if(!conversionPairs.containsKey(twoStrings[0]))
					conversionPairs.put(twoStrings[0], twoStrings[1]);
				else
					throw new IllegalArgumentException("Input conversion cannot specify different values for the same input string: " + twoStrings[0]);
			}
			return conversionPairs;
		}
	},

	/**
	 * Replacement pairs for non-obvious candidate search in a speller dictionary.
	 * For example, Polish {@code rz} is phonetically equivalent to {@code ż},
	 * and this may be specified here to allow looking for replacements of {@code rz} with {@code ż}
	 * and vice versa.
	 */
	REPLACEMENT_PAIRS("fsa.dict.speller.replacement-pairs"){
		@Override
		public Map<String, List<String>> fromString(final String value) throws IllegalArgumentException{
			final Map<String, List<String>> replacementPairs = new HashMap<>();
			final String[] replacements = value.split(",\\s*");
			for(final String stringPair : replacements){
				final String[] twoStrings = stringPair.trim().split(StringUtils.SPACE);
				if(twoStrings.length != 2)
					throw new IllegalArgumentException("Attribute " + propertyName + " is not in the proper format: " + value);

				if(!replacementPairs.containsKey(twoStrings[0])){
					final List<String> strList = new ArrayList<>(1);
					strList.add(twoStrings[1]);
					replacementPairs.put(twoStrings[0], strList);
				}
				else
					replacementPairs.get(twoStrings[0]).add(twoStrings[1]);
			}
			return replacementPairs;
		}
	},

	/**
	 * Equivalent characters (treated similarly as equivalent chars with and without
	 * diacritics). For example, Polish {@code ł} can be specified as equivalent to {@code l}.
	 *
	 * <p>This implements a feature similar to hunspell MAP in the affix file.
	 */
	EQUIVALENT_CHARS("fsa.dict.speller.equivalent-chars"){
		@Override
		public Map<Character, List<Character>> fromString(final String value) throws IllegalArgumentException{
			final Map<Character, List<Character>> equivalentCharacters = new HashMap<>();
			final String[] eqChars = value.split(",\\s*");
			for(final String characterPair : eqChars){
				final String[] twoChars = characterPair.trim().split(StringUtils.SPACE);
				if(twoChars.length != 2 || twoChars[0].length() != 1 || twoChars[1].length() != 1)
					throw new IllegalArgumentException("Attribute " + propertyName + " is not in the proper format: " + value);

				final char fromChar = twoChars[0].charAt(0);
				final char toChar = twoChars[1].charAt(0);
				equivalentCharacters.computeIfAbsent(fromChar, k -> new ArrayList<>(1))
					.add(toChar);
			}
			return equivalentCharacters;
		}
	},

	/** Dictionary license attribute */
	LICENSE("fsa.dict.license"),
	/** Dictionary author */
	AUTHOR("fsa.dict.author"),
	/** Dictionary creation date */
	CREATION_DATE("fsa.dict.created");

	/** Property name for this attribute */
	public final String propertyName;

	/**
	 * Converts a string to the given attribute's value.
	 *
	 * @param value	The value to convert to an attribute value.
	 * @return	The attribute's value converted from a string.
	 * @throws IllegalArgumentException	If the input string cannot be converted to the attribute's value.
	 */
	public Object fromString(final String value) throws IllegalArgumentException{
		return value;
	}

	/**
	 * @param propertyName The property of a {@link DictionaryAttribute}.
	 * @return Return a {@link DictionaryAttribute} associated with
	 * a given {@link #propertyName}.
	 */
	public static DictionaryAttribute fromPropertyName(final String propertyName){
		final DictionaryAttribute value = attrsByPropertyName.get(propertyName);
		if(value == null)
			throw new IllegalArgumentException("No attribute for property: " + propertyName);

		return value;
	}

	private static final Map<String, DictionaryAttribute> attrsByPropertyName;

	static{
		attrsByPropertyName = new HashMap<>();
		for(final DictionaryAttribute attr : DictionaryAttribute.values())
			if(attrsByPropertyName.put(attr.propertyName, attr) != null)
				throw new RuntimeException("Duplicate property key for: " + attr);
	}

	/** Private enum instance constructor */
	DictionaryAttribute(final String propertyName){
		this.propertyName = propertyName;
	}

	private static Boolean booleanValue(String value){
		value = value.toLowerCase(Locale.ROOT);
		if("true".equals(value) || "yes".equals(value) || "on".equals(value))
			return Boolean.TRUE;

		if("false".equals(value) || "no".equals(value) || "off".equals(value))
			return Boolean.FALSE;

		throw new IllegalArgumentException("Not a boolean value: " + value);
	}

}
