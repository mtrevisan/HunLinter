package unit731.hunlinter.parsers.enums;


import unit731.hunlinter.services.system.LoopHelper;


public enum PartOfSpeechTag{

	//noun
	NOUN("noun", "NN"),
	NOUN_PROPER("noun_proper", "NNP"),

	//article
	ARTICLE_DEFINITE("article_definite", "AD"),
	ARTICLE_INDEFINITE("article_indefinite", "AI"),
	ARTICLE_PERSONAL("article_personal", "AP"),
	ARTICLE_PARTITIVE("article_partitive", "AT"),

	//adjective
//FIXME to be replaced
ADJECTIVE("adjective", "JJ"),
ADJECTIVE_QUALIFICATIVE("adjective_qualificative", "JQ"),
//	ADJECTIVE_QUALIFICATIVE_PRIMITIVE("adjective_primitive", "JP"),
//	ADJECTIVE_QUALIFICATIVE_DERIVED("adjective_derived", "JD"),
//	ADJECTIVE_QUALIFICATIVE_ALTERED("adjective_altered", "JA"),
//	ADJECTIVE_QUALIFICATIVE_COMPOUNDED("adjective_compounded", "JC"),
	ADJECTIVE_DETERMINATIVE_POSSESSIVE_STRONG("adjective_possessive_strong", "JPS"),
	ADJECTIVE_DETERMINATIVE_POSSESSIVE_WEAK("adjective_possessive_weak", "JPW"),
//FIXME to be replaced
ADJECTIVE_DETERMINATIVE_DEMONSTRATIVE("adjective_demonstrative", "JDEM"),
	ADJECTIVE_DETERMINATIVE_DEMONSTRATIVE_NEAR("adjective_demonstrative_near", "JDN"),
	ADJECTIVE_DETERMINATIVE_DEMONSTRATIVE_FAR("adjective_demonstrative_far", "JDF"),
	ADJECTIVE_DETERMINATIVE_DEMONSTRATIVE_WEAK("adjective_demonstrative_far_weak", "JDFW"),
	ADJECTIVE_DETERMINATIVE_DEMONSTRATIVE_STRONG("adjective_demonstrative_far_strong", "JDFS"),
	ADJECTIVE_DETERMINATIVE_DEMONSTRATIVE_NEUTRAL("adjective_demonstrative_neutral", "JDU"),
	ADJECTIVE_DETERMINATIVE_DEMONSTRATIVE_BEFORE_NOUN("adjective_demonstrative_before_noun", "PDBN"),
	ADJECTIVE_DETERMINATIVE_IDENTIFICATIVE("adjective_identificative", "JN"),
	ADJECTIVE_DETERMINATIVE_INTERROGATIVE_EXCLAMATORY("adjective_interrogative_exclamatory", "JIE"),

	//pronoun
	PRONOUN_FREE_SUBJECT("pronoun_free_subject", "PFS"),
	PRONOUN_FREE_INDIRECT("pronoun_free_indirect", "PFI"),
	PRONOUN_FREE_REFLEXIVE("pronoun_free_reflexive", "PFR"),
	PRONOUN_CLITIC_COMPLEMENT_DIRECT("pronoun_clitic_complement_direct", "PKCD"),
	PRONOUN_CLITIC_COMPLEMENT_INDIRECT("pronoun_clitic_complement_indirect", "PKCI"),
	PRONOUN_CLITIC_REFLEXIVE("pronoun_clitic_reflexive", "PKR"),
	PRONOUN_CLITIC_SUBJECT("pronoun_clitic_subject", "PKS"),
	PRONOUN_IMPERSONAL("pronoun_impersonal", "PM"),
	PRONOUN_PASSIVATING("pronoun_passivating", "PS"),
	PRONOUN_PARTITIVE("pronoun_partitive", "PT"),
	PRONOUN_LOCATIVE_EXISTENTIAL("pronoun_locative_existential", "PLE"),
	PRONOUN_LOCATIVE_REFERENTIAL("pronoun_locative_referential", "PLR"),
	PRONOUN_RELATIVE("pronoun_relative", "PR"),
	PRONOUN_POSSESSIVE("pronoun_possessive", "PP"),
	PRONOUN_DEMONSTRATIVE("pronoun_demonstrative", "PD"),
	PRONOUN_DEMONSTRATIVE_NEAR("pronoun_demonstrative_near", "PDN"),
	PRONOUN_DEMONSTRATIVE_FAR("pronoun_demonstrative_far", "PDF"),
	PRONOUN_DEMONSTRATIVE_FAR_WEAK("pronoun_demonstrative_far_weak", "PDFW"),
	PRONOUN_DEMONSTRATIVE_FAR_STRONG("pronoun_demonstrative_far_strong", "PDFS"),
	PRONOUN_DEMONSTRATIVE_NEUTRAL("pronoun_demonstrative_neutral", "PDU"),
	PRONOUN_DEMONSTRATIVE_BEFORE_NOUN("pronoun_demonstrative_before_noun", "PDBN"),
	PRONOUN_IDENTIFICATIVE("pronoun_identificative", "PN"),
	PRONOUN_INTERROGATIVE_EXCLAMATORY("pronoun_interrogative_exclamatory", "PIE"),

	//quantifier
	QUANTIFIER_EXISTENTIAL("quantifier_existential", "QE"),
	QUANTIFIER_DISTRIBUTIVE("quantifier_distributive", "QD"),
	QUANTIFIER_UNIVERSAL_POSSIBLE("quantifier_universal_possible", "QUP"),
	QUANTIFIER_UNIVERSAL_EFFECTIVE("quantifier_universal_effective", "QUE"),

	//verb
	VERB("verb", "VB"),

	//numeral
	NUMERAL_LATIN("numeral_latin", "NL"),
	NUMERAL_CARDENAL("numeral_cardenal", "NC"),
	NUMERAL_ORDENAL("numeral_ordenal", "NO"),
	NUMERAL_COLLECTIVE("numeral_collective", "NC"),
	NUMERAL_FRACTIONAL("numeral_fractional", "NF"),
	NUMERAL_MULTIPLICATIVE("numeral_multiplicative", "NM"),

	PREPOSITION("preposition", "PR"),

	ADVERB("adverb", "AD"),

	CONJUNCTION("conjunction", "CN"),

	PREFIX("prefix", "PX"),

	INTERJECTION("interjection", "IN"),

	UNIT_OF_MEASURE("unit_of_measure", "UOM");


	private final String code;
	private final String tag;


	PartOfSpeechTag(final String code, final String tag){
		this.code = code;
		this.tag = tag;
	}

	public static PartOfSpeechTag createFromCode(final String code){
		return LoopHelper.match(values(), tag -> tag.code.equals(code));
	}

	public String getCode(){
		return code;
	}

	public String getTag(){
		return tag;
	}

}
