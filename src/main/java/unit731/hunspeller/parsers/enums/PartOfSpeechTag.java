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

	//pronouns
	TAG_PRONOUN_FREE_SUBJECT("pronoun_free_subject", "PRFS"),
	TAG_PRONOUN_FREE_INDIRECT("pronoun_free_indirect", "PRFI"),
	TAG_PRONOUN_FREE_REFLEXIVE("pronoun_free_reflexive", "PRFR"),
	TAG_PRONOUN_CLITIC_COMPLEMENT_DIRECT("pronoun_clitic_complement_direct", "PRKCD"),
	TAG_PRONOUN_CLITIC_COMPLEMENT_INDIRECT("pronoun_clitic_complement_indirect", "PRKCI"),
	TAG_PRONOUN_CLITIC_REFLEXIVE("pronoun_clitic_reflexive", "PRKX"),
	TAG_PRONOUN_CLITIC_SUBJECT("pronoun_clitic_subject", "PRKS"),
	TAG_PRONOUN_IMPERSONAL("pronoun_impersonal", "PRI"),
	TAG_PRONOUN_PASSIVATING("pronoun_passivating", "PRV"),
	TAG_PRONOUN_PARTITIVE("pronoun_partitive", "PRT"),
	TAG_PRONOUN_LOCATIVE_EXISTENTIAL("pronoun_locative_existential", "PRLE"),
	TAG_PRONOUN_LOCATIVE_REFERENTIAL("pronoun_locative_existential", "PRLR"),
	TAG_PRONOUN_RELATIVE("pronoun_relative", "PRR"),
	TAG_PRONOUN_POSSESSIVE("pronoun_possessive", "PRP"),
	TAG_PRONOUN_DEMONSTRATIVE("pronoun_demonstrative", "PRD"),
	TAG_PRONOUN_IDENTIFICATIVE("pronoun_identificative", "PRF"),
	TAG_PRONOUN_INTERROGATIVE("pronoun_interrogative", "PRG"),

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
