package unit731.hunspeller.parsers.enums;

import java.util.Arrays;


public enum PartOfSpeechTag{

	TAG_NOUN("noun", "NN"),
	TAG_PROPER_NOUN("proper_noun", "NP"),
	TAG_VERB("verb", "VB"),
	TAG_ADJECTIVE("adjective", "JJ"),
	TAG_ADJECTIVE_POSSESSIVE("adjective_possessive", "JJP"),
	TAG_ADJECTIVE_DEMONSTRATIVE("adjective_demonstrative", "JJD"),
	TAG_ADJECTIVE_IDENTIFICATIVE("adjective_identificative", "JJI"),
	TAG_ADJECTIVE_INTERROGATIVE("adjective_interrogative", "JJR"),
	TAG_QUANTIFIER("quantifier", "QQ"),
	TAG_NUMERAL_LATIN("numeral_latin", "NL"),
	TAG_NUMERAL_CARDENAL("numeral_cardenal", "NC"),
	TAG_NUMERAL_ORDENAL("numeral_ordenal", "NO"),
	TAG_NUMERAL_COLLECTIVE("numeral_collective", "NC"),
	TAG_NUMERAL_FRACTIONAL("numeral_fractional", "NF"),
	TAG_NUMERAL_MULTIPLICATIVE("numeral_multiplicative", "NM"),
	TAG_ARTICLE("article", "AA"),
	TAG_DETERMINER("determiner", "DT"),
	TAG_PRONOUN_PERSONAL("pronoun_personal", "PRP"),
	TAG_PRONOUN_POSSESSIVE("pronoun_possessive", "PRP$"),
	TAG_PREPOSITION("preposition", ""),
	TAG_ADVERB("adverb", ""),
	TAG_CONJUNCTION("conjunction", ""),
	TAG_PREFIX("prefix", ""),
	TAG_INTERJECTION("interjection", ""),
	TAG_UNIT_OF_MEASURE("unit_of_measure", "");


	private final String code;
	private final String tag;


	PartOfSpeechTag(final String code, final String tag){
		this.code = code;
		this.tag = tag;
	}

	public static PartOfSpeechTag createFromCode(final String code){
		return Arrays.stream(values())
			.filter(tag -> tag.code.equals(code))
			.findFirst()
			.orElse(null);
	}

	public String getCode(){
		return code;
	}

	public String getTag(){
		return tag;
	}

}
