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
package io.github.mtrevisan.hunlinter.parsers.enums;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;


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


	private static final Map<String, PartOfSpeechTag> VALUES = new HashMap<>(PartOfSpeechTag.values().length);
	static{
		for(final PartOfSpeechTag tag : EnumSet.allOf(PartOfSpeechTag.class))
			VALUES.put(MorphologicalTag.PART_OF_SPEECH.getCode() + tag.code, tag);
	}

	private final String code;
	private final String tag;


	PartOfSpeechTag(final String code, final String tag){
		this.code = code;
		this.tag = tag;
	}

	public static PartOfSpeechTag createFromCodeAndValue(final String codeAndValue){
		return VALUES.get(codeAndValue);
	}

	public String getCode(){
		return code;
	}

	public String getTag(){
		return tag;
	}

}
