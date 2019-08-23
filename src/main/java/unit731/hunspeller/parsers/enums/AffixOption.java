package unit731.hunspeller.parsers.enums;

import java.util.Arrays;


public enum AffixOption{

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
	/* Search and suggest words with one different character replaced by a neighbor character */
//	KEY("KEY"),
//	TRY("TRY"),
	/** Words signed with this flag are not suggested (but still accepted when typed correctly) */
	NO_SUGGEST_FLAG("NOSUGGEST"),
//	MAX_COMPOUND_SUGGEST("MAXCPDSUGS"),
//	MAX_NGRAM_SUGGEST("MAXNGRAMSUGS"),
//	MAX_NGRAM_SIMILARITY_FACTOR("MAXDIFF"),
//	ONLY_MAX_NGRAM_SIMILARITY_FACTOR("ONLYMAXDIFF"),
	/* Sable word suggestions with spaces */
//	NO_SPLIT_SUGGEST("NOSPLITSUGS"),
	/** Similar to NOSUGGEST, but it forbids to use the word in n-gram based (more, than 1-character distance) suggestions */
//	NO_NGRAM_SUGGEST("NONGRAMSUGGEST"),
//	SUGGESTIONS_WITH_DOTS("SUGSWITHDOTS"),
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
	WORD_BREAK_CHARACTERS("BREAK"),
	/** Define custom compound patterns */
	COMPOUND_RULE("COMPOUNDRULE"),
	/** Minimum length of words in compound words */
	COMPOUND_MINIMUM_LENGTH("COMPOUNDMIN"),
	/**
	 * Words with this flag may be in compound words (except when word shorter than COMPOUNDMIN).
	 * Affixes with COMPOUNDFLAG also permits compounding of affixed words.
	 */
	COMPOUND_FLAG("COMPOUNDFLAG"),
	/** Word signed with this flag (or with a signed affix) may be the first element in compound words */
	COMPOUND_BEGIN_FLAG("COMPOUNDBEGIN"),
	/** Word signed with this flag (or with a signed affix) may be the middle element in compound words */
	COMPOUND_MIDDLE_FLAG("COMPOUNDMIDDLE"),
	/** Word signed with this flag (or with a signed affix) may be the last element in compound words */
	COMPOUND_END_FLAG("COMPOUNDEND"),
	/** Suffixes signed this flag may be only inside of compounds (this flag works also with words) */
	ONLY_IN_COMPOUND_FLAG("ONLYINCOMPOUND"),
	/**
	 * Affixes with this flag may be present inside of compounds (normally, prefixes and suffixes are allowed respectively at the beginning and
	 * at the end of compounds only).
	 * <code>(prefix)?(root)+(affix)?</code>
	 */
	PERMIT_COMPOUND_FLAG("COMPOUNDPERMITFLAG"),
	/** Affixes with this flag forbid compounding of the affixed word */
	FORBID_COMPOUND_FLAG("COMPOUNDFORBIDFLAG"),
	/** Allow twofold suffixes within compounds */
	ALLOW_TWOFOLD_AFFIXES_IN_COMPOUND("COMPOUNDMORESUFFIXES"),
	/* Signs the compounds in the dictionary */
	//FIXME if implemented, remember to decomment from {@link AffixData#SINGLE_FLAG_TAGS}
//	COMPOUND_ROOT("COMPOUNDROOT"),
	/** Set maximum word count in a compound word (default is unlimited) */
	COMPOUND_MAX_WORD_COUNT("COMPOUNDWORDMAX"),
	/** Forbid word duplicates in compounds */
	FORBID_DUPLICATES_IN_COMPOUND("CHECKCOMPOUNDDUP"),
	/**
	 * Forbid compounding if the (usually bad) compound word may be a non-compound word with a REP substitution (useful for languages with
	 * 'compound friendly' orthography)
	 */
	CHECK_COMPOUND_REPLACEMENT("CHECKCOMPOUNDREP"),
	/** Forbid upper case characters at word bound in compounds */
	FORBID_DIFFERENT_CASES_IN_COMPOUND("CHECKCOMPOUNDCASE"),
	/** Forbid compounding, if compound word contains triple repeating letters (e.g. foo|ox or xo|oof) */
	FORBID_TRIPLES_IN_COMPOUND("CHECKCOMPOUNDTRIPLE"),
	/** Allow simplified 2-letter forms of the compounds forbidden by CHECKCOMPOUNDTRIPLE (Schiff|fahrt -&gt; Schiffahrt) */
	SIMPLIFIED_TRIPLES_IN_COMPOUND("SIMPLIFIEDTRIPLE"),
	/*
	  Forbid compounding, if the first word in the compound ends with <endchars>, and next word begins with <beginchars> and (optionally)
	  they have the requested flags. The optional replacement parameter allows simplified compound form.
	  The special <endchars> pattern 0 (zero) limits the rule to the unmodified stems (stems and stems with zero affixes)
	  Note: COMPOUNDMIN doesn't work correctly with the compound word alternation, so it may need to set COMPOUNDMIN to lower value.
	 */
//	CHECK_COMPOUND_PATTERN("CHECKCOMPOUNDPATTERN"),
	/** The last word of a compound with this flag forces capitalization of the whole compound word */
	FORCE_COMPOUND_UPPERCASE_FLAG("FORCEUCASE"),
	/*
	  Needed for special compounding rules in Hungarian (first parameter is the maximum syllable number, that may be in a compound,
	  if words in compounds are more than COMPOUNDWORDMAX; second parameter is the list of vowels -- for calculating syllables)
	 */
//	COMPOUND_SYLLABLE("COMPOUNDSYLLABLE"),
	/** Needed for special compounding rules in Hungarian */
//	SYLLABLE_NUMBER("SYLLABLENUM"),

	//Options for affix creation
	PREFIX("PFX"),
	SUFFIX("SFX"),

	//Other options
	/** Affixes signed with this flag may be on a word when this word also has a prefix with CIRCUMFIX flag and vice versa */
	CIRCUMFIX_FLAG("CIRCUMFIX"),
	/** Signs forbidden word form (affixed forms are also forbidden, excepts root homonyms) */
	FORBIDDEN_WORD_FLAG("FORBIDDENWORD"),
	/** With this flag the affix rules can strip full words, not only one less characters */
	FULLSTRIP("FULLSTRIP"),
	/** Forbid uppercased and capitalized forms of words signed with this flag */
	KEEP_CASE_FLAG("KEEPCASE"),
	/** Define input conversion table */
	INPUT_CONVERSION_TABLE("ICONV"),
	/** Define output conversion table */
	OUTPUT_CONVERSION_TABLE("OCONV"),
	/**
	 * Signs virtual stems in the dictionary, words are accepted only when affixed, except if the dictionary word has a homonym or a zero affix
	 * (it works also with prefixes and prefix + suffix combinations)
	 */
	NEED_AFFIX_FLAG("NEEDAFFIX");
	/* Signs affix rules and dictionary words (allomorphs) not used in morphological generation and root words removed from suggestion */
	//FIXME if implemented, remember to decomment from {@link AffixData#SINGLE_FLAG_TAGS}
//	SUB_STANDARD_FLAG("SUBSTANDARD"),
	/* Extends tokenizer of Hunspell command line interface with additional word character */
//	WORD_CHARS("WORDCHARS"),
	/**
	 * SS letter pair in uppercased (German) words may be uppercase sharp s (ß). Hunspell can handle this special casing with the
	 * CHECKSHARPS declaration (see also KEEPCASE flag and tests/germancompounding example) in both spelling and suggestion
	 */
//	CHECK_SHARPS("CHECKSHARPS");


	private final String code;


	AffixOption(final String code){
		this.code = code;
	}

	public static AffixOption createFromCode(final String code){
		return Arrays.stream(values())
			.filter(option -> option.code.equals(code))
			.findFirst()
			.orElse(null);
	}

	public String getCode(){
		return code;
	}

}
