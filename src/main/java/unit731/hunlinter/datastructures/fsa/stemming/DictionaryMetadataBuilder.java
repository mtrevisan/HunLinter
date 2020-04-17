package unit731.hunlinter.datastructures.fsa.stemming;

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

	public DictionaryMetadataBuilder separator(char c){
		attrs.put(DictionaryAttribute.SEPARATOR, Character.toString(c));
		return this;
	}

	public DictionaryMetadataBuilder encoding(Charset charset){
		return encoding(charset.name());
	}

	public DictionaryMetadataBuilder encoding(String charsetName){
		attrs.put(DictionaryAttribute.ENCODING, charsetName);
		return this;
	}

	public DictionaryMetadataBuilder frequencyIncluded(){ return frequencyIncluded(true); }

	public DictionaryMetadataBuilder frequencyIncluded(boolean v){
		attrs.put(DictionaryAttribute.FREQUENCY_INCLUDED, Boolean.toString(v));
		return this;
	}

	public DictionaryMetadataBuilder ignorePunctuation(){ return ignorePunctuation(true); }

	public DictionaryMetadataBuilder ignorePunctuation(boolean v){
		attrs.put(DictionaryAttribute.IGNORE_PUNCTUATION, Boolean.toString(v));
		return this;
	}

	public DictionaryMetadataBuilder ignoreNumbers(){ return ignoreNumbers(true); }

	public DictionaryMetadataBuilder ignoreNumbers(boolean v){
		attrs.put(DictionaryAttribute.IGNORE_NUMBERS, Boolean.toString(v));
		return this;
	}

	public DictionaryMetadataBuilder ignoreCamelCase(){ return ignoreCamelCase(true); }

	public DictionaryMetadataBuilder ignoreCamelCase(boolean v){
		attrs.put(DictionaryAttribute.IGNORE_CAMEL_CASE, Boolean.toString(v));
		return this;
	}

	public DictionaryMetadataBuilder ignoreAllUppercase(){ return ignoreAllUppercase(true); }

	public DictionaryMetadataBuilder ignoreAllUppercase(boolean v){
		attrs.put(DictionaryAttribute.IGNORE_ALL_UPPERCASE, Boolean.toString(v));
		return this;
	}

	public DictionaryMetadataBuilder ignoreDiacritics(){ return ignoreDiacritics(true); }

	public DictionaryMetadataBuilder ignoreDiacritics(boolean v){
		attrs.put(DictionaryAttribute.IGNORE_DIACRITICS, Boolean.toString(v));
		return this;
	}

	public DictionaryMetadataBuilder convertCase(){ return convertCase(true); }

	public DictionaryMetadataBuilder convertCase(boolean v){
		attrs.put(DictionaryAttribute.CONVERT_CASE, Boolean.toString(v));
		return this;
	}

	public DictionaryMetadataBuilder supportRunOnWords(){ return supportRunOnWords(true); }

	public DictionaryMetadataBuilder supportRunOnWords(boolean v){
		attrs.put(DictionaryAttribute.RUN_ON_WORDS, Boolean.toString(v));
		return this;
	}

	public DictionaryMetadataBuilder encoder(EncoderType type){
		attrs.put(DictionaryAttribute.ENCODER, type.name());
		return this;
	}

	public DictionaryMetadataBuilder locale(Locale locale){
		return locale(locale.toString());
	}

	public DictionaryMetadataBuilder locale(String localeName){
		attrs.put(DictionaryAttribute.LOCALE, localeName);
		return this;
	}

	public DictionaryMetadataBuilder withReplacementPairs(Map<String, List<String>> replacementPairs){
		final StringBuffer builder = new StringBuffer();
		for(final Map.Entry<String, List<String>> e : replacementPairs.entrySet()){
			final String k = e.getKey();
			for(final String v : e.getValue()){
				if(builder.length() > 0)
					builder.append(", ");
				builder.append(k).append(StringUtils.SPACE).append(v);
			}
		}
		attrs.put(DictionaryAttribute.REPLACEMENT_PAIRS, builder.toString());
		return this;
	}

	public DictionaryMetadataBuilder withEquivalentChars(Map<Character, List<Character>> equivalentChars){
		final StringBuffer builder = new StringBuffer();
		for(final Map.Entry<Character, List<Character>> e : equivalentChars.entrySet()){
			final Character k = e.getKey();
			for(final Character v : e.getValue()){
				if(builder.length() > 0)
					builder.append(", ");
				builder.append(k).append(StringUtils.SPACE).append(v);
			}
		}
		attrs.put(DictionaryAttribute.EQUIVALENT_CHARS, builder.toString());
		return this;
	}

	public DictionaryMetadataBuilder withInputConversionPairs(Map<String, String> conversionPairs){
		final StringBuffer builder = new StringBuffer();
		for(final Map.Entry<String, String> e : conversionPairs.entrySet()){
			final String k = e.getKey();
			if(builder.length() > 0)
				builder.append(", ");
			builder.append(k).append(StringUtils.SPACE).append(conversionPairs.get(k));
		}
		attrs.put(DictionaryAttribute.INPUT_CONVERSION, builder.toString());
		return this;
	}

	public DictionaryMetadataBuilder withOutputConversionPairs(Map<String, String> conversionPairs){
		final StringBuffer builder = new StringBuffer();
		for(final Map.Entry<String, String> e : conversionPairs.entrySet()){
			final String k = e.getKey();
			if(builder.length() > 0)
				builder.append(", ");
			builder.append(k).append(StringUtils.SPACE).append(conversionPairs.get(k));
		}
		attrs.put(DictionaryAttribute.OUTPUT_CONVERSION, builder.toString());
		return this;
	}


	public DictionaryMetadataBuilder author(String author){
		attrs.put(DictionaryAttribute.AUTHOR, author);
		return this;
	}

	public DictionaryMetadataBuilder creationDate(String creationDate){
		attrs.put(DictionaryAttribute.CREATION_DATE, creationDate);
		return this;
	}

	public DictionaryMetadataBuilder license(String license){
		attrs.put(DictionaryAttribute.LICENSE, license);
		return this;
	}

	public DictionaryMetadata build(){
		return new DictionaryMetadata(attrs);
	}

	public EnumMap<DictionaryAttribute, String> toMap(){
		return new EnumMap<>(attrs);
	}

}
