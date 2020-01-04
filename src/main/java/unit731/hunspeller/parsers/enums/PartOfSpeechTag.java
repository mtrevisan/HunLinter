package unit731.hunspeller.parsers.enums;

import java.util.Arrays;


public enum PartOfSpeechTag{

	//noun
	TAG_NOUN("noun", "NN"),
	TAG_NOUN_PROPER("noun_proper", "NNP"),

	//article
	TAG_ARTICLE_DEFINITE("article_definite", "AD"),
	TAG_ARTICLE_UNDEFINITE("article_undefinite", "AU"),
	TAG_ARTICLE_PERSONAL("article_personal", "AP"),
	TAG_ARTICLE_PARTITIVE("article_partitive", "AT"),

	//adjective
//FIXME to be replaced
TAG_ADJECTIVE("adjective", "JJ"),
//	TAG_ADJECTIVE_QUALIFICATIVE_PRIMITIVE("adjective_primitive", "JP"),
//	TAG_ADJECTIVE_QUALIFICATIVE_DERIVED("adjective_derived", "JD"),
//	TAG_ADJECTIVE_QUALIFICATIVE_ALTERED("adjective_altered", "JA"),
//	TAG_ADJECTIVE_QUALIFICATIVE_COMPOUNDED("adjective_compounded", "JC"),
	TAG_ADJECTIVE_DETERMINATIVE_POSSESSIVE_STRONG("adjective_possessive_strong", "JPS"),
	TAG_ADJECTIVE_DETERMINATIVE_POSSESSIVE_WEAK("adjective_possessive_weak", "JPW"),
//FIXME to be replaced
TAG_ADJECTIVE_DETERMINATIVE_DEMONSTRATIVE("adjective_demonstrative", "JDEM"),
	TAG_ADJECTIVE_DETERMINATIVE_DEMONSTRATIVE_NEAR("adjective_demonstrative_near", "JDN"),
	TAG_ADJECTIVE_DETERMINATIVE_DEMONSTRATIVE_FAR("adjective_demonstrative_far", "JDF"),
	TAG_ADJECTIVE_DETERMINATIVE_DEMONSTRATIVE_WEAK("adjective_demonstrative_far_weak", "JDFW"),
	TAG_ADJECTIVE_DETERMINATIVE_DEMONSTRATIVE_STRONG("adjective_demonstrative_far_strong", "JDFS"),
	TAG_ADJECTIVE_DETERMINATIVE_DEMONSTRATIVE_NEUTRAL("adjective_demonstrative_neutral", "JDU"),
	TAG_ADJECTIVE_DETERMINATIVE_DEMONSTRATIVE_BEFORE_NOUN("adjective_demonstrative_before_noun", "PDBN"),
	TAG_ADJECTIVE_DETERMINATIVE_IDENTIFICATIVE("adjective_identificative", "JN"),
	TAG_ADJECTIVE_DETERMINATIVE_INTERROGATIVE_ESCLAMATIVE("adjective_interrogative_esclamative", "JIE"),

	//pronoun
	TAG_PRONOUN_FREE_SUBJECT("pronoun_free_subject", "PFS"),
	TAG_PRONOUN_FREE_INDIRECT("pronoun_free_indirect", "PFI"),
	TAG_PRONOUN_FREE_REFLEXIVE("pronoun_free_reflexive", "PFR"),
	TAG_PRONOUN_CLITIC_COMPLEMENT_DIRECT("pronoun_clitic_complement_direct", "PKCD"),
	TAG_PRONOUN_CLITIC_COMPLEMENT_INDIRECT("pronoun_clitic_complement_indirect", "PKCI"),
	TAG_PRONOUN_CLITIC_REFLEXIVE("pronoun_clitic_reflexive", "PKR"),
	TAG_PRONOUN_CLITIC_SUBJECT("pronoun_clitic_subject", "PKS"),
	TAG_PRONOUN_IMPERSONAL("pronoun_impersonal", "PM"),
	TAG_PRONOUN_PASSIVATING("pronoun_passivating", "PS"),
	TAG_PRONOUN_PARTITIVE("pronoun_partitive", "PT"),
	TAG_PRONOUN_LOCATIVE_EXISTENTIAL("pronoun_locative_existential", "PLE"),
	TAG_PRONOUN_LOCATIVE_REFERENTIAL("pronoun_locative_referential", "PLR"),
	TAG_PRONOUN_RELATIVE("pronoun_relative", "PR"),
	TAG_PRONOUN_POSSESSIVE("pronoun_possessive", "PP"),
	TAG_PRONOUN_DEMONSTRATIVE("pronoun_demonstrative", "PD"),
	TAG_PRONOUN_DEMONSTRATIVE_NEAR("pronoun_demonstrative_near", "PDN"),
	TAG_PRONOUN_DEMONSTRATIVE_FAR("pronoun_demonstrative_far", "PDF"),
	TAG_PRONOUN_DEMONSTRATIVE_FAR_WEAK("pronoun_demonstrative_far_weak", "PDFW"),
	TAG_PRONOUN_DEMONSTRATIVE_FAR_STRONG("pronoun_demonstrative_far_strong", "PDFS"),
	TAG_PRONOUN_DEMONSTRATIVE_NEUTRAL("pronoun_demonstrative_neutral", "PDU"),
	TAG_PRONOUN_DEMONSTRATIVE_BEFORE_NOUN("pronoun_demonstrative_before_noun", "PDBN"),
	TAG_PRONOUN_IDENTIFICATIVE("pronoun_identificative", "PN"),
	TAG_PRONOUN_INTERROGATIVE_ESCLAMATIVE("pronoun_interrogative_esclamative", "PIE"),

	TAG_VERB("verb", "VB"),
	TAG_QUANTIFIER("quantifier", "QQ"),
	TAG_NUMERAL_LATIN("numeral_latin", "NL"),
	TAG_NUMERAL_CARDENAL("numeral_cardenal", "NC"),
	TAG_NUMERAL_ORDENAL("numeral_ordenal", "NO"),
	TAG_NUMERAL_COLLECTIVE("numeral_collective", "NC"),
	TAG_NUMERAL_FRACTIONAL("numeral_fractional", "NF"),
	TAG_NUMERAL_MULTIPLICATIVE("numeral_multiplicative", "NM"),

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
