package unit731.hunspeller.parsers.affix;

import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
@Getter
public enum AffixTag{

	//General options
	/**
	 * Set character encoding of words and morphemes in affix and dictionary files. Possible values are UTF-8, ISO8859-1 through ISO8859-10,
	 * ISO8859-13 through ISO8859-15, KOI8-R, KOI8-U, MICROSOFT-CP1251, ISCII-DEVANAGARI
	 */
	CHARACTER_SET("SET"),
	/**
	 * Set flag type. Default type is the extended ASCII (8-bit) character. 'UTF-8' parameter sets UTF-8 encoded Unicode character flags.
	 * The 'long' value sets the double extended ASCII character flag type, the 'num' sets the decimal number flag type. Decimal flags numbered
	 * from 1 to 65000, and in flag fields are separated by comma
	 */
	FLAG("FLAG"),
	/** Set twofold prefix stripping (but single suffix stripping) for agglutinative languages with right-to-left writing system */
	COMPLEX_PREFIXES("COMPLEXPREFIXES"),
	/** Language code */
	LANGUAGE("LANG"),
	/** Sets characters to ignore in dictionary words, affixes and input words */
//	IGNORE("IGNORE"),
	ALIASES_FLAG("AF"),
	ALIASES_MORPHOLOGICAL_FIELD("AM"),

	//Options for suggestions
	/** Search and suggest words with one different character replaced by a neighbor character */
//	KEY("KEY"),
//	TRY("TRY"),
	/** Words signed with this flag are not suggested (but still accepted when typed correctly) */
//	NO_SUGGEST("NOSUGGEST"),
	/** Similar to NOSUGGEST, but it forbids to use the word in n-gram based (more, than 1-character distance) suggestions */
//	NO_NGRAM_SUGGEST("NONGRAMSUGGEST"),
//	MAX_COMPOUND_SUGGEST("MAXCPDSUGS"),
//	MAX_NGRAM_SUGGEST("MAXNGRAMSUGS"),
//	MAX_NGRAM_SIMILARITY_FACTOR("MAXDIFF"),
//	ONLY_MAX_NGRAM_SIMILARITY_FACTOR("ONLYMAXDIFF"),
//	SUGGESTIONS_WITH_DOTS("SUGSWITHDOTS"),
	/** If space is used then all the words must be present in the dictionary */
	REPLACEMENT_TABLE("REP"),
//	MAP_TABLE("MAP"),
//	PHONE_TABLE("PHONE"),
//	WARN("WARN"),
//	FORBID_WARN("FORBIDWARN"),

	//Options for compounding
	/**
	 * Define new break points for breaking words and checking word parts separately (use ^ and $ to delete characters at end
	 * and start of the word)
	 */
	BREAK("BREAK"),
	/** Define custom compound patterns */
	COMPOUND_RULE("COMPOUNDRULE"),
	/** Minimum length of words in compound words */
	COMPOUND_MIN("COMPOUNDMIN"),
	/**
	 * Words with this flag may be in compound words (except when word shorter than COMPOUNDMIN).
	 * Affixes with COMPOUNDFLAG also permits compounding of affixed words.
	 */
	COMPOUND_FLAG("COMPOUNDFLAG"),
	/** Words signed with this flag (or with a signed affix) may be first elements in compound words */
//	COMPOUND_BEGIN("COMPOUNDBEGIN"),
	/** Words signed with this flag (or with a signed affix) may be middle elements in compound words */
//	COMPOUND_MIDDLE("COMPOUNDMIDDLE"),
	/** Words signed with this flag (or with a signed affix) may be last elements in compound words */
//	COMPOUND_END("COMPOUNDEND"),
	/** Suffixes signed this flag may be only inside of compounds (this flag works also with words) */
	ONLY_IN_COMPOUND("ONLYINCOMPOUND"),
	/**
	 * Affixes with this flag may be inside of compounds (normally, prefixes and suffixes are allowed respectively at the beginning and
	 * at the end of compounds only).
	 */
	COMPOUND_PERMIT_FLAG("COMPOUNDPERMITFLAG"),
	/** Allow twofold suffixes within compounds */
	COMPOUND_MORE_SUFFIXES("COMPOUNDMORESUFFIXES"),
	/** Affixes with this flag forbid compounding of the affixed word */
	COMPOUND_FORBID_FLAG("COMPOUNDFORBIDFLAG"),
	/** Set maximum word count in a compound word (default is unlimited) */
	COMPOUND_WORD_MAX("COMPOUNDWORDMAX"),
	/** Forbid word duplication in compounds */
	CHECK_COMPOUND_DUPLICATION("CHECKCOMPOUNDDUP"),
	/**
	 * Forbid compounding if the (usually bad) compound word may be a non-compound word with a REP fault (useful for languages with
	 * 'compound friendly' orthography)
	 */
//	CHECK_COMPOUND_REPLACEMENT("CHECKCOMPOUNDREP"),
	/** Forbid upper case characters at word bound in compounds */
	CHECK_COMPOUND_CASE("CHECKCOMPOUNDCASE"),
	/** Forbid compounding, if compound word contains triple repeating letters (e.g. foo|ox or xo|oof) */
	CHECK_COMPOUND_TRIPLE("CHECKCOMPOUNDTRIPLE"),
	/** Allow simplified 2-letter forms of the compounds forbidden by CHECKCOMPOUNDTRIPLE (Schiff|fahrt -> Schiffahrt) */
	SIMPLIFIED_TRIPLE("SIMPLIFIEDTRIPLE"),
	/** Affixes signed with this flag may be on a word when this word also has a prefix with CIRCUMFIX flag and vice versa */
	CIRCUMFIX("CIRCUMFIX"),
	/**
	 * Signs forbidden word form (because affixed forms are also forbidden, we can subtract a subset from the set of accepted affixed
	 * and compound words)
	 */
//	FORBIDDEN_WORD("FORBIDDENWORD"),

	//Options for affix creation
	PREFIX("PFX"),
	SUFFIX("SFX"),

	//Other options
	/** With this flag the affix rules can strip full words, not only one less characters */
	FULLSTRIP("FULLSTRIP"),
	/** Forbid uppercased and capitalized forms of words signed with this flag */
	KEEP_CASE("KEEPCASE"),
	/**
	 * Signs virtual stems in the dictionary, words are valid only when affixed, except if the dictionary word has a homonym or a zero affix
	 * (it works also with prefixes and prefix + suffix combinations)
	 */
	NEED_AFFIX("NEEDAFFIX"),
	/** Extends tokenizer of Hunspell command line interface with additional word character */
//	WORD_CHARS("WORDCHARS"),
	/** Define input conversion table */
	INPUT_CONVERSION_TABLE("ICONV"),
	/** Define output conversion table */
	OUTPUT_CONVERSION_TABLE("OCONV");


	private final String code;


	public static AffixTag toEnum(String code){
		for(AffixTag tag : values())
			if(tag.getCode().equals(code))
				return tag;
		return null;
	}

}
