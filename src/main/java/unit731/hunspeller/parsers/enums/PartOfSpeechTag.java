package unit731.hunspeller.parsers.enums;

import java.util.Arrays;


public enum PartOfSpeechTag{

	//pronouns
	TAG_PRONOUN_FREE_SUBJECT("pronoun_free_subject", "PFS"),
	TAG_PRONOUN_FREE_INDIRECT("pronoun_free_indirect", "PFI"),
	TAG_PRONOUN_FREE_REFLEXIVE("pronoun_free_reflexive", "PFR"),
	TAG_PRONOUN_CLITIC_COMPLEMENT_DIRECT("pronoun_clitic_complement_direct", "PKCD"),
	TAG_PRONOUN_CLITIC_COMPLEMENT_INDIRECT("pronoun_clitic_complement_indirect", "PKCI"),
	TAG_PRONOUN_CLITIC_REFLEXIVE("pronoun_clitic_reflexive", "PKR"),
	TAG_PRONOUN_CLITIC_SUBJECT("pronoun_clitic_subject", "PKS"),
	TAG_PRONOUN_IMPERSONAL("pronoun_impersonal", "PM"),
	TAG_PRONOUN_PASSIVATING("pronoun_passivating", "PS"),
	TAG_PRONOUN_PARTITIVE("pronoun_partitive", "PP"),
	TAG_PRONOUN_LOCATIVE_EXISTENTIAL("pronoun_locative_existential", "PLE"),
	TAG_PRONOUN_LOCATIVE_REFERENTIAL("pronoun_locative_referential", "PLR"),
	TAG_PRONOUN_RELATIVE("pronoun_relative", "PR"),
	TAG_PRONOUN_POSSESSIVE_STRONG("pronoun_possessive_strong", "PPS"),
	TAG_PRONOUN_DEMONSTRATIVE("pronoun_demonstrative", "PD"),
	TAG_PRONOUN_DEMONSTRATIVE_NEAR("pronoun_demonstrative_near", "PDN"),
	TAG_PRONOUN_DEMONSTRATIVE_FAR("pronoun_demonstrative_far", "PDF"),
	TAG_PRONOUN_DEMONSTRATIVE_FAR_WEAK("pronoun_demonstrative_far_weak", "PDFW"),
	TAG_PRONOUN_DEMONSTRATIVE_FAR_STRONG("pronoun_demonstrative_far_strong", "PDFS"),
	TAG_PRONOUN_DEMONSTRATIVE_NEUTRAL("pronoun_demonstrative_neutral", "PDU"),
	TAG_PRONOUN_DEMONSTRATIVE_BEFORE_NOUN("pronoun_demonstrative_before_noun", "PDBN"),
	TAG_PRONOUN_IDENTIFICATIVE("pronoun_identificative", "PN"),
	TAG_PRONOUN_INTERROGATIVE("pronoun_interrogative", "PI"),

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

	TAG_PREPOSITION("preposition", "PR"),
	TAG_ADVERB("adverb", "AD"),
	TAG_CONJUNCTION("conjunction", "CN"),
	TAG_PREFIX("prefix", "PX"),
	TAG_INTERJECTION("interjection", "IN"),
	TAG_UNIT_OF_MEASURE("unit_of_measure", "UOM");


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
