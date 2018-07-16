package unit731.hunspeller.languages.vec;

import java.io.File;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.valueobjects.AffixEntry;
import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.hyphenation.hyphenators.HyphenatorInterface;
import unit731.hunspeller.parsers.hyphenation.dtos.HyphenationInterface;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.PatternService;


public class DictionaryParserVEC extends DictionaryParser{

	private static final String VERB_1ST_RULE_NON_VANISHING_EL = "a1";
	private static final String VERB_1ST_RULE_VANISHING_EL = "a2";
	private static final String VERB_DAR_RULE_NON_VANISHING_EL = "a4";
	private static final String VERB_DAR_RULE_VANISHING_EL = "a5";
	private static final String VERB_2ND_RULE_NON_VANISHING_EL = "b1";
	private static final String VERB_2ND_RULE_VANISHING_EL = "b2";
	private static final String VERB_DIXER_RULE_NON_VANISHING_EL = "b6";
	private static final String VERB_DIXER_RULE_VANISHING_EL = "b7";
	private static final String VERB_TOLER_RULE_NON_VANISHING_EL = "c3";
	private static final String VERB_TOLER_RULE_VANISHING_EL = "c4";
	private static final String VERB_3RD_IS_RULE_NON_VANISHING_EL = "d1";
	private static final String VERB_3RD_IS_RULE_VANISHING_EL = "d2";
	private static final String VERB_3RD_NO_IS_RULE_NON_VANISHING_EL = "e1";
	private static final String VERB_3RD_NO_IS_RULE_VANISHING_EL = "e2";
	private static final String VERB_3RD_BOTH_IS_RULE_NON_VANISHING_EL = "f1";
	private static final String VERB_3RD_BOTH_IS_RULE_VANISHING_EL = "f2";
	private static final String PROCOMPLEMENTAR_VERB_DEFINITE_RULE_NON_VANISHING_EL = "P1";
	private static final String PROCOMPLEMENTAR_VERB_DEFINITE_RULE_VANISHING_EL = "P2";
	private static final String PROCOMPLEMENTAR_VERB_IMPERATIVE_RULE_NON_VANISHING_EL = "P4";
	private static final String PROCOMPLEMENTAR_VERB_IMPERATIVE_RULE_VANISHING_EL = "P4";
	private static final String PROCOMPLEMENTAR_VERB_INDEFINITE_RULE_NON_VANISHING_EL = "P7";
	private static final String PROCOMPLEMENTAR_VERB_INDEFINITE_RULE_VANISHING_EL = "P8";
	private static final String INTERROGATIVES_3RD_PERSON_RULE_NON_VANISHING_EL = "I6";
	private static final String INTERROGATIVES_3RD_PERSON_RULE_VANISHING_EL = "I7";
	private static final String INTERROGATIVES_3RD_PERSON_CONDITIONAL_RULE_NON_VANISHING_EL = "I8";
	private static final String INTERROGATIVES_3RD_PERSON_CONDITIONAL_RULE_VANISHING_EL = "I9";
	private static final String ADJECTIVE_FIRST_CLASS_RULE = "A1";
	private static final String ADJECTIVE_SECOND_CLASS_RULE = "A2";
	private static final String ADJECTIVE_THIRD_CLASS_RULE = "F0";
	private static final String PLURAL_NOUN_MASCULINE_RULE = "M0";
	private static final String PLURAL_NOUN_MASCULINE_IO_RULE = "M1";
	private static final String VARIANT_TRANSFORMATIONS_BEGIN_RULE = "TB";
	private static final String VARIANT_TRANSFORMATIONS_END_RULE = "TE";
	private static final String VARIANT_TRANSFORMATIONS_FEMININE_RULE = "TF";
	private static final String METAPHONESIS_RULE = "mf";
	private static final String PLANTS_AND_CRAFTS_RULE_NON_VANISHING_EL = "V0";
	private static final String PLANTS_AND_CRAFTS_RULE_VANISHING_EL = "V1";
	private static final String DEVERBAL_NOMINALS_MENTO_RULE_NON_VANISHING_EL = "r0";
	private static final String DEVERBAL_NOMINALS_MENTO_RULE_VANISHING_EL = "r1";
	private static final String DEVERBAL_NOMINALS_THION_RULE_NON_VANISHING_EL = "r2";
	private static final String DEVERBAL_NOMINALS_SION_RULE_NON_VANISHING_EL = "r3";
	private static final String DEVERBAL_NOMINALS_SION_RULE_VANISHING_EL = "r4";
	private static final String DEVERBAL_NOMINALS_IXMO_RULE_NON_VANISHING_EL = "s0";
	private static final String DEVERBAL_NOMINALS_IXMO_RULE_VANISHING_EL = "s1";
	private static final String ADVERB_MENTE_RULE_NON_VANISHING_EL = "W0";
	private static final String ADVERB_MENTE_RULE_VANISHING_EL = "W1";
	private static final String DIMINUTIVE_ETO_RULE_NON_VANISHING_EL = "&0";
	private static final String DIMINUTIVE_ETO_RULE_VANISHING_EL = "&1";
	private static final String DIMINUTIVE_EL_RULE_NON_VANISHING_EL = "&2";
	private static final String DIMINUTIVE_EL_RULE_VANISHING_EL = "&3";
	private static final String AUGMENTATIVE_OTO_RULE_NON_VANISHING_EL = "(0";
	private static final String AUGMENTATIVE_OTO_RULE_VANISHING_EL = "(1";
	private static final String AUGMENTATIVE_ON_RULE_NON_VANISHING_EL = "(2";
	private static final String AUGMENTATIVE_ON_RULE_VANISHING_EL = "(3";
	private static final String PEJORATIVE_ATO_RULE_NON_VANISHING_EL = "§0";
	private static final String PEJORATIVE_ATO_RULE_VANISHING_EL = "§1";
	private static final String PEJORATIVE_ATHO_RULE_NON_VANISHING_EL = "<0";
	private static final String PEJORATIVE_ASO_RULE_NON_VANISHING_EL = "<1";
	private static final String PEJORATIVE_ASO_RULE_VANISHING_EL = "<2";
	private static final String NORTHERN_PLURAL_RULE = "U0";
	private static final String NORTHERN_PLURAL_STRESSED_RULE = "U1";
	private static final String COLLECTIVE_NOUNS_RULE = "Y0";
	private static final String FINAL_SONORIZATION_RULE = "FS";
	private static final String GUA_TO_VA_RULE = "gv";

	private static final Matcher NON_VANISHING_EL = PatternService.matcher("(^|[aàeèéiíoòóuúAÀEÈÉIÍOÒÓUÚʼ–-])l([aàeèéiíoòóuúAÀEÈÉIÍOÒÓUÚʼ–-]|$)");
	private static final Matcher VANISHING_EL_NEAR_CONSONANT = PatternService.matcher("[^aàeèéiíoòóuúAÀEÈÉIÍOÒÓUÚʼ–-]ƚ|ƚ[^aàeèéiíoòóuúAÀEÈÉIÍOÒÓUÚʼ]");

	private static final Matcher L_BETWEEN_VOWELS = PatternService.matcher("l i l$");
	private static final Matcher CIJJHNHIV = PatternService.matcher("[ci" + GraphemeVEC.JJH_PHONEME + "ɉñ]j[aàeèéiíoòóuú]");

//	private static final String START_TAGS = "(?<!\\\\)\\/.*?";
//	private static final String NON_VANISHING_L = "(^[ʼ']?l|[aeiouàèéíòóú]l)[aeiouàèéíòóú][^ƚ]+?" + START_TAGS;

	private static final String SLASH = "/";
	private static final String ASTERISK = "*";


	private static final class MatcherEntry{

		private static final String CANNOT_USE_RULE_WITH_LH = "Cannot use {0} rule with ƚ";
		private static final String CANNOT_USE_RULE_WITH_LH_USE_INSTEAD = "Cannot use {0} rule with ƚ, use {1}";
		private static final String CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD = "Cannot use {0} rule with non–ƚ, use {1}";
		private static final String CANNOT_USE_RULE_WITH_TH_OR_DH_USE_INSTEAD = "Cannot use {0} rule with đ or ŧ, use {1}";

		private final List<String> continuationFlags;
		private final String error;


		MatcherEntry(List<String> continuationFlags, String pattern, Object ... arguments){
			this.continuationFlags = continuationFlags;
			//take last argument as the concatenation of the continuationFlags
			List<Object> args = new ArrayList<>(arguments.length + 1);
			args.addAll(Arrays.asList(arguments));
			args.add(String.join(" or ", continuationFlags));
			this.error = MessageFormat.format(pattern, args.toArray(new Object[args.size()]));
		}

		public void match(RuleProductionEntry production) throws IllegalArgumentException{
			for(String flag : continuationFlags)
				if(production.containsContinuationFlag(flag))
					throw new IllegalArgumentException(error + " for " + production.getWord());
		}
	}
	private static final boolean ENABLE_VERB_CHECK = false;
	private static final Set<MatcherEntry> MISMATCH_CHECKS_MUST_CONTAINS_LH = new HashSet<>();
	private static final Set<MatcherEntry> MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH = new HashSet<>();
	private static final Set<MatcherEntry> MISMATCH_CHECKS_MUST_CONTAINS_DH_OR_TH = new HashSet<>();
	static{
		if(ENABLE_VERB_CHECK){
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(VERB_1ST_RULE_NON_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, VERB_1ST_RULE_NON_VANISHING_EL, VERB_1ST_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(VERB_1ST_RULE_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, VERB_1ST_RULE_VANISHING_EL, VERB_1ST_RULE_NON_VANISHING_EL));
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(VERB_2ND_RULE_NON_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, VERB_2ND_RULE_NON_VANISHING_EL, VERB_2ND_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(VERB_2ND_RULE_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, VERB_2ND_RULE_VANISHING_EL, VERB_2ND_RULE_NON_VANISHING_EL));
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(VERB_3RD_IS_RULE_NON_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, VERB_3RD_IS_RULE_NON_VANISHING_EL, VERB_3RD_IS_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(VERB_3RD_IS_RULE_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, VERB_3RD_IS_RULE_VANISHING_EL, VERB_3RD_IS_RULE_NON_VANISHING_EL));
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(VERB_3RD_NO_IS_RULE_NON_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, VERB_3RD_NO_IS_RULE_NON_VANISHING_EL, VERB_3RD_NO_IS_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(VERB_3RD_NO_IS_RULE_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, VERB_3RD_NO_IS_RULE_VANISHING_EL, VERB_3RD_NO_IS_RULE_NON_VANISHING_EL));
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(VERB_3RD_BOTH_IS_RULE_NON_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, VERB_3RD_BOTH_IS_RULE_NON_VANISHING_EL, VERB_3RD_BOTH_IS_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(VERB_3RD_BOTH_IS_RULE_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, VERB_3RD_BOTH_IS_RULE_VANISHING_EL, VERB_3RD_BOTH_IS_RULE_NON_VANISHING_EL));
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(VERB_DAR_RULE_NON_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, VERB_DAR_RULE_NON_VANISHING_EL, VERB_DAR_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(VERB_DAR_RULE_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, VERB_DAR_RULE_VANISHING_EL, VERB_DAR_RULE_NON_VANISHING_EL));
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(VERB_TOLER_RULE_NON_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, VERB_TOLER_RULE_NON_VANISHING_EL, VERB_TOLER_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(VERB_TOLER_RULE_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, VERB_TOLER_RULE_VANISHING_EL, VERB_TOLER_RULE_NON_VANISHING_EL));
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(VERB_DIXER_RULE_NON_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, VERB_DIXER_RULE_NON_VANISHING_EL, VERB_DIXER_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(VERB_DIXER_RULE_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, VERB_DIXER_RULE_VANISHING_EL, VERB_DIXER_RULE_NON_VANISHING_EL));
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(PROCOMPLEMENTAR_VERB_DEFINITE_RULE_NON_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, PROCOMPLEMENTAR_VERB_DEFINITE_RULE_NON_VANISHING_EL, PROCOMPLEMENTAR_VERB_DEFINITE_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(PROCOMPLEMENTAR_VERB_DEFINITE_RULE_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, PROCOMPLEMENTAR_VERB_DEFINITE_RULE_VANISHING_EL, PROCOMPLEMENTAR_VERB_DEFINITE_RULE_NON_VANISHING_EL));
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(PROCOMPLEMENTAR_VERB_INDEFINITE_RULE_NON_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, PROCOMPLEMENTAR_VERB_INDEFINITE_RULE_NON_VANISHING_EL, PROCOMPLEMENTAR_VERB_INDEFINITE_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(PROCOMPLEMENTAR_VERB_INDEFINITE_RULE_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, PROCOMPLEMENTAR_VERB_INDEFINITE_RULE_VANISHING_EL, PROCOMPLEMENTAR_VERB_INDEFINITE_RULE_NON_VANISHING_EL));
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(PROCOMPLEMENTAR_VERB_IMPERATIVE_RULE_NON_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, PROCOMPLEMENTAR_VERB_IMPERATIVE_RULE_NON_VANISHING_EL, PROCOMPLEMENTAR_VERB_IMPERATIVE_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(PROCOMPLEMENTAR_VERB_IMPERATIVE_RULE_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, PROCOMPLEMENTAR_VERB_IMPERATIVE_RULE_VANISHING_EL, PROCOMPLEMENTAR_VERB_IMPERATIVE_RULE_NON_VANISHING_EL));
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(INTERROGATIVES_3RD_PERSON_RULE_NON_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, INTERROGATIVES_3RD_PERSON_RULE_NON_VANISHING_EL, INTERROGATIVES_3RD_PERSON_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(INTERROGATIVES_3RD_PERSON_RULE_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, INTERROGATIVES_3RD_PERSON_RULE_VANISHING_EL, INTERROGATIVES_3RD_PERSON_RULE_NON_VANISHING_EL));
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(INTERROGATIVES_3RD_PERSON_CONDITIONAL_RULE_NON_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, INTERROGATIVES_3RD_PERSON_CONDITIONAL_RULE_NON_VANISHING_EL, INTERROGATIVES_3RD_PERSON_CONDITIONAL_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(INTERROGATIVES_3RD_PERSON_CONDITIONAL_RULE_VANISHING_EL),
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, INTERROGATIVES_3RD_PERSON_CONDITIONAL_RULE_VANISHING_EL, INTERROGATIVES_3RD_PERSON_CONDITIONAL_RULE_NON_VANISHING_EL));
		}

		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(FINAL_SONORIZATION_RULE),
			MatcherEntry.CANNOT_USE_RULE_WITH_LH, FINAL_SONORIZATION_RULE));

		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(DEVERBAL_NOMINALS_MENTO_RULE_NON_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, DEVERBAL_NOMINALS_MENTO_RULE_NON_VANISHING_EL, DEVERBAL_NOMINALS_MENTO_RULE_VANISHING_EL));
		MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(DEVERBAL_NOMINALS_MENTO_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, DEVERBAL_NOMINALS_MENTO_RULE_VANISHING_EL, DEVERBAL_NOMINALS_MENTO_RULE_NON_VANISHING_EL));

		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(PLANTS_AND_CRAFTS_RULE_NON_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, PLANTS_AND_CRAFTS_RULE_NON_VANISHING_EL, PLANTS_AND_CRAFTS_RULE_VANISHING_EL));
		MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(PLANTS_AND_CRAFTS_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, PLANTS_AND_CRAFTS_RULE_VANISHING_EL, PLANTS_AND_CRAFTS_RULE_NON_VANISHING_EL));

		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(DEVERBAL_NOMINALS_THION_RULE_NON_VANISHING_EL, DEVERBAL_NOMINALS_SION_RULE_NON_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, DEVERBAL_NOMINALS_SION_RULE_NON_VANISHING_EL, DEVERBAL_NOMINALS_SION_RULE_VANISHING_EL));
		MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(DEVERBAL_NOMINALS_SION_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, DEVERBAL_NOMINALS_THION_RULE_NON_VANISHING_EL, DEVERBAL_NOMINALS_SION_RULE_NON_VANISHING_EL, DEVERBAL_NOMINALS_SION_RULE_VANISHING_EL));

		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(DEVERBAL_NOMINALS_IXMO_RULE_NON_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, DEVERBAL_NOMINALS_IXMO_RULE_NON_VANISHING_EL, DEVERBAL_NOMINALS_IXMO_RULE_VANISHING_EL));
		MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(DEVERBAL_NOMINALS_IXMO_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, DEVERBAL_NOMINALS_IXMO_RULE_VANISHING_EL, DEVERBAL_NOMINALS_IXMO_RULE_NON_VANISHING_EL));

		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(ADVERB_MENTE_RULE_NON_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, ADVERB_MENTE_RULE_NON_VANISHING_EL, ADVERB_MENTE_RULE_VANISHING_EL));
		MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(ADVERB_MENTE_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, ADVERB_MENTE_RULE_VANISHING_EL, ADVERB_MENTE_RULE_NON_VANISHING_EL));

		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(DIMINUTIVE_ETO_RULE_NON_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, DIMINUTIVE_ETO_RULE_NON_VANISHING_EL, DIMINUTIVE_ETO_RULE_VANISHING_EL));
		MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(DIMINUTIVE_ETO_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, DIMINUTIVE_ETO_RULE_VANISHING_EL, DIMINUTIVE_ETO_RULE_NON_VANISHING_EL));
		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(DIMINUTIVE_EL_RULE_NON_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, DIMINUTIVE_EL_RULE_NON_VANISHING_EL, DIMINUTIVE_EL_RULE_VANISHING_EL));
		MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(DIMINUTIVE_EL_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, DIMINUTIVE_EL_RULE_VANISHING_EL, DIMINUTIVE_EL_RULE_NON_VANISHING_EL));

		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(AUGMENTATIVE_OTO_RULE_NON_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, AUGMENTATIVE_OTO_RULE_NON_VANISHING_EL, AUGMENTATIVE_OTO_RULE_VANISHING_EL));
		MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(AUGMENTATIVE_OTO_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, AUGMENTATIVE_OTO_RULE_VANISHING_EL, AUGMENTATIVE_OTO_RULE_NON_VANISHING_EL));
		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(AUGMENTATIVE_ON_RULE_NON_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, AUGMENTATIVE_ON_RULE_NON_VANISHING_EL, AUGMENTATIVE_ON_RULE_VANISHING_EL));
		MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(AUGMENTATIVE_ON_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, AUGMENTATIVE_ON_RULE_VANISHING_EL, AUGMENTATIVE_ON_RULE_NON_VANISHING_EL));

		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(PEJORATIVE_ATHO_RULE_NON_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, PEJORATIVE_ATHO_RULE_NON_VANISHING_EL, PEJORATIVE_ASO_RULE_VANISHING_EL));
		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(PEJORATIVE_ATO_RULE_NON_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, PEJORATIVE_ATO_RULE_NON_VANISHING_EL, PEJORATIVE_ATO_RULE_VANISHING_EL));


		MISMATCH_CHECKS_MUST_CONTAINS_DH_OR_TH.add(new MatcherEntry(Arrays.asList(PLANTS_AND_CRAFTS_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_TH_OR_DH_USE_INSTEAD, PLANTS_AND_CRAFTS_RULE_VANISHING_EL, PLANTS_AND_CRAFTS_RULE_NON_VANISHING_EL));
		MISMATCH_CHECKS_MUST_CONTAINS_DH_OR_TH.add(new MatcherEntry(Arrays.asList(DEVERBAL_NOMINALS_MENTO_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_TH_OR_DH_USE_INSTEAD, DEVERBAL_NOMINALS_MENTO_RULE_VANISHING_EL, DEVERBAL_NOMINALS_MENTO_RULE_NON_VANISHING_EL));

		MISMATCH_CHECKS_MUST_CONTAINS_DH_OR_TH.add(new MatcherEntry(Arrays.asList(DEVERBAL_NOMINALS_SION_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_TH_OR_DH_USE_INSTEAD, DEVERBAL_NOMINALS_SION_RULE_VANISHING_EL, DEVERBAL_NOMINALS_THION_RULE_NON_VANISHING_EL));
		MISMATCH_CHECKS_MUST_CONTAINS_DH_OR_TH.add(new MatcherEntry(Arrays.asList(DEVERBAL_NOMINALS_IXMO_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_TH_OR_DH_USE_INSTEAD, DEVERBAL_NOMINALS_IXMO_RULE_VANISHING_EL, DEVERBAL_NOMINALS_IXMO_RULE_NON_VANISHING_EL));
		MISMATCH_CHECKS_MUST_CONTAINS_DH_OR_TH.add(new MatcherEntry(Arrays.asList(ADVERB_MENTE_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_TH_OR_DH_USE_INSTEAD, ADVERB_MENTE_RULE_VANISHING_EL, ADVERB_MENTE_RULE_NON_VANISHING_EL));
		MISMATCH_CHECKS_MUST_CONTAINS_DH_OR_TH.add(new MatcherEntry(Arrays.asList(DIMINUTIVE_ETO_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_TH_OR_DH_USE_INSTEAD, DIMINUTIVE_ETO_RULE_VANISHING_EL, DIMINUTIVE_ETO_RULE_NON_VANISHING_EL));
		MISMATCH_CHECKS_MUST_CONTAINS_DH_OR_TH.add(new MatcherEntry(Arrays.asList(DIMINUTIVE_EL_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_TH_OR_DH_USE_INSTEAD, DIMINUTIVE_EL_RULE_VANISHING_EL, DIMINUTIVE_EL_RULE_NON_VANISHING_EL));
		MISMATCH_CHECKS_MUST_CONTAINS_DH_OR_TH.add(new MatcherEntry(Arrays.asList(AUGMENTATIVE_OTO_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_TH_OR_DH_USE_INSTEAD, AUGMENTATIVE_OTO_RULE_VANISHING_EL, AUGMENTATIVE_OTO_RULE_NON_VANISHING_EL));
		MISMATCH_CHECKS_MUST_CONTAINS_DH_OR_TH.add(new MatcherEntry(Arrays.asList(AUGMENTATIVE_ON_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_TH_OR_DH_USE_INSTEAD, AUGMENTATIVE_ON_RULE_VANISHING_EL, AUGMENTATIVE_ON_RULE_NON_VANISHING_EL));
		MISMATCH_CHECKS_MUST_CONTAINS_DH_OR_TH.add(new MatcherEntry(Arrays.asList(PEJORATIVE_ATO_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_TH_OR_DH_USE_INSTEAD, PEJORATIVE_ATO_RULE_VANISHING_EL, PEJORATIVE_ATO_RULE_NON_VANISHING_EL));
		MISMATCH_CHECKS_MUST_CONTAINS_DH_OR_TH.add(new MatcherEntry(Arrays.asList(PEJORATIVE_ASO_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_TH_OR_DH_USE_INSTEAD, PEJORATIVE_ASO_RULE_VANISHING_EL, PEJORATIVE_ASO_RULE_NON_VANISHING_EL));
	}

	private static final Matcher MATCHER_NORTHERN_PLURAL = PatternService.matcher("[èò][ln]$");
	private static final String MAN = "man";

	private static final Set<MatcherEntry> ADJECTIVE_FIRST_CLASS_MISMATCH_CHECKS = new HashSet<>();
	private static final String WORD_WITH_RULE_CANNOT_HAVE = "Word with rule {0} cannot have rule {1}";
	static{
		ADJECTIVE_FIRST_CLASS_MISMATCH_CHECKS.add(new MatcherEntry(Arrays.asList(DIMINUTIVE_ETO_RULE_NON_VANISHING_EL, DIMINUTIVE_ETO_RULE_VANISHING_EL),
			WORD_WITH_RULE_CANNOT_HAVE, ADJECTIVE_FIRST_CLASS_RULE));
		ADJECTIVE_FIRST_CLASS_MISMATCH_CHECKS.add(new MatcherEntry(Arrays.asList(DIMINUTIVE_EL_RULE_NON_VANISHING_EL, DIMINUTIVE_EL_RULE_VANISHING_EL),
			WORD_WITH_RULE_CANNOT_HAVE, ADJECTIVE_FIRST_CLASS_RULE));
		ADJECTIVE_FIRST_CLASS_MISMATCH_CHECKS.add(new MatcherEntry(Arrays.asList(AUGMENTATIVE_OTO_RULE_NON_VANISHING_EL, AUGMENTATIVE_OTO_RULE_VANISHING_EL),
			WORD_WITH_RULE_CANNOT_HAVE, ADJECTIVE_FIRST_CLASS_RULE));
		ADJECTIVE_FIRST_CLASS_MISMATCH_CHECKS.add(new MatcherEntry(Arrays.asList(AUGMENTATIVE_ON_RULE_NON_VANISHING_EL, AUGMENTATIVE_ON_RULE_VANISHING_EL),
			WORD_WITH_RULE_CANNOT_HAVE, ADJECTIVE_FIRST_CLASS_RULE));
		ADJECTIVE_FIRST_CLASS_MISMATCH_CHECKS.add(new MatcherEntry(Arrays.asList(PEJORATIVE_ATO_RULE_NON_VANISHING_EL, PEJORATIVE_ATO_RULE_VANISHING_EL),
			WORD_WITH_RULE_CANNOT_HAVE, ADJECTIVE_FIRST_CLASS_RULE));
		ADJECTIVE_FIRST_CLASS_MISMATCH_CHECKS.add(new MatcherEntry(Arrays.asList(PEJORATIVE_ATHO_RULE_NON_VANISHING_EL, PEJORATIVE_ASO_RULE_VANISHING_EL),
			WORD_WITH_RULE_CANNOT_HAVE, ADJECTIVE_FIRST_CLASS_RULE));
		ADJECTIVE_FIRST_CLASS_MISMATCH_CHECKS.add(new MatcherEntry(Arrays.asList(COLLECTIVE_NOUNS_RULE),
			WORD_WITH_RULE_CANNOT_HAVE, ADJECTIVE_FIRST_CLASS_RULE));
	}

	private static final Set<MatcherEntry> VARIANT_TRANSFORMATION_END_MISMATCH_CHECKS = new HashSet<>();
	static{
		VARIANT_TRANSFORMATION_END_MISMATCH_CHECKS.add(new MatcherEntry(Arrays.asList(DIMINUTIVE_ETO_RULE_NON_VANISHING_EL, DIMINUTIVE_ETO_RULE_VANISHING_EL, DIMINUTIVE_EL_RULE_NON_VANISHING_EL, DIMINUTIVE_EL_RULE_VANISHING_EL),
			WORD_WITH_RULE_CANNOT_HAVE, VARIANT_TRANSFORMATIONS_END_RULE));
		VARIANT_TRANSFORMATION_END_MISMATCH_CHECKS.add(new MatcherEntry(Arrays.asList(PLURAL_NOUN_MASCULINE_IO_RULE),
			WORD_WITH_RULE_CANNOT_HAVE, VARIANT_TRANSFORMATIONS_END_RULE));
		VARIANT_TRANSFORMATION_END_MISMATCH_CHECKS.add(new MatcherEntry(Arrays.asList(DIMINUTIVE_ETO_RULE_NON_VANISHING_EL, DIMINUTIVE_ETO_RULE_VANISHING_EL),
			WORD_WITH_RULE_CANNOT_HAVE, VARIANT_TRANSFORMATIONS_END_RULE));
		VARIANT_TRANSFORMATION_END_MISMATCH_CHECKS.add(new MatcherEntry(Arrays.asList(AUGMENTATIVE_ON_RULE_NON_VANISHING_EL, AUGMENTATIVE_ON_RULE_VANISHING_EL),
			WORD_WITH_RULE_CANNOT_HAVE, VARIANT_TRANSFORMATIONS_END_RULE));
	}

	private static final Set<MatcherEntry> VARIANT_TRANSFORMATION_FEMININE_MISMATCH_CHECKS = new HashSet<>();
	static{
		VARIANT_TRANSFORMATION_FEMININE_MISMATCH_CHECKS.add(new MatcherEntry(Arrays.asList(PEJORATIVE_ATHO_RULE_NON_VANISHING_EL, PEJORATIVE_ASO_RULE_VANISHING_EL),
			WORD_WITH_RULE_CANNOT_HAVE, VARIANT_TRANSFORMATIONS_FEMININE_RULE));
	}

	private static final Set<MatcherEntry> GUA_TO_VA_CHECKS = new HashSet<>();
	static{
		GUA_TO_VA_CHECKS.add(new MatcherEntry(Arrays.asList(":0", "@0", ",0", ".0", "*0", "-0", "+0", "^6", "^5", "^4", "^3", "^2", "^1", "^0", VARIANT_TRANSFORMATIONS_BEGIN_RULE),
			WORD_WITH_RULE_CANNOT_HAVE, GUA_TO_VA_RULE));
	}


	public static final String POS_PROPER_NOUN = "proper_noun";
	public static final String POS_NOUN = "noun";
	public static final String POS_ADJECTIVE = "adjective";
	public static final String POS_ADJECTIVE_POSSESSIVE = "adjective_possessive";
	public static final String POS_ADJECTIVE_DEMONSTRATIVE = "adjective_demonstrative";
	public static final String POS_ADJECTIVE_IDENTIFICATIVE = "adjective_identificative";
	public static final String POS_ADJECTIVE_INTERROGATIVE = "adjective_interrogative";
	public static final String POS_QUANTIFIER = "quantifier";
	public static final String POS_VERB = "verb";
	public static final String POS_ARTICLE = "article";
	public static final String POS_PRONOUN = "pronoun";
	public static final String POS_PREPOSITION = "preposition";
	public static final String POS_ADVERB = "adverb";
	public static final String POS_CONJUNCTION = "conjunction";
	public static final String POS_NUMERAL_LATIN = "numeral_latin";
	public static final String POS_INTERJECTION = "interjection";
	public static final String POS_UNIT_OF_MEASURE = "unit_of_measure";

	private static final Set<String> PART_OF_SPEECH = new HashSet<>(Arrays.asList(POS_NOUN, POS_PROPER_NOUN, POS_VERB, POS_ADJECTIVE,
		POS_ADJECTIVE_POSSESSIVE, POS_ADJECTIVE_DEMONSTRATIVE, POS_ADJECTIVE_IDENTIFICATIVE, POS_ADJECTIVE_INTERROGATIVE, POS_QUANTIFIER,
		POS_NUMERAL_LATIN, "numeral_cardenal", "numeral_ordenal", "numeral_collective", "numeral_fractional", "numeral_multiplicative",
		POS_ARTICLE, POS_PRONOUN, POS_PREPOSITION, POS_ADVERB, POS_CONJUNCTION, "prefix", POS_INTERJECTION, POS_UNIT_OF_MEASURE));
	private static final Set<String> INFLECTIONAL_SUFFIX = new HashSet<>(Arrays.asList("singular_masculine", "singular_femenine", "plural",
		"plural_masculine", "plural_femenine", "procomplementar", "interrogative", "second_singular", "second_plural"));
	private static final Set<String> TERMINAL_SUFFIX = new HashSet<>(Arrays.asList("indicative_present", "indicative_imperfect",
		"indicative_future", "subjunctive_present", "subjunctive_imperfect", "conditional_present", "imperative_present", "infinitive_simple",
		"gerund_simple", "participle_active", "participle_passive", "participle_perfect", "participle_perfect_strong", "participle_imperfect"));

	private static final Map<String, Set<String>> DATA_FIELDS = new HashMap<>();
	static{
		DATA_FIELDS.put(WordGenerator.TAG_PART_OF_SPEECH, PART_OF_SPEECH);
		DATA_FIELDS.put(WordGenerator.TAG_INFLECTIONAL_SUFFIX, INFLECTIONAL_SUFFIX);
		DATA_FIELDS.put(WordGenerator.TAG_TERMINAL_SUFFIX, TERMINAL_SUFFIX);
		DATA_FIELDS.put(WordGenerator.TAG_STEM, null);
		DATA_FIELDS.put(WordGenerator.TAG_ALLOMORPH, null);
	}

	private static final List<String> UNSYLLABABLE_INTERJECTIONS = Arrays.asList("brr", "ii", "iii", "mm", "mmm", "ss", "sss", "iu");
	private static final List<String> MULTIPLE_ACCENTED_INTERJECTIONS = Arrays.asList("àà", "ààà", "èè", "èèè", "íí", "ííí", "òò", "òòò");

	private static final int MINIMAL_PAIR_MINIMUM_LENGTH = 3;


	private final Orthography orthography = OrthographyVEC.getInstance();


	public DictionaryParserVEC(File dicFile, HyphenatorInterface hyphenator, WordGenerator wordGenerator, Charset charset){
		super(dicFile, hyphenator, wordGenerator, charset);
	}

	@Override
	public void checkProduction(RuleProductionEntry production, FlagParsingStrategy strategy) throws IllegalArgumentException{
//		if(!ENABLE_VERB_CHECK && production.isPartOfSpeech(POS_VERB))
//			return;

		try{
			if(!production.hasMorphologicalFields())
				throw new IllegalArgumentException("Line does not contains any morphological fields");

			morphologicalFieldCheck(production);

			vanishingElCheck(production);

			incompatibilityCheck(production);

			String derivedWordWithoutMorphologicalFields = production.toString();
			if(production.hasContinuationFlags() && !production.isPartOfSpeech(POS_VERB) && !production.isPartOfSpeech(POS_ADVERB)){
				metaphonesisCheck(production, derivedWordWithoutMorphologicalFields);

				northernPluralCheck(production);
			}

			mismatchCheck(production);

			finalSonorizationCheck(production);

			String derivedWord = production.getWord();
			List<String> splittedWords = hyphenator.splitIntoCompounds(derivedWord);
			for(String subword : splittedWords){
				accentCheck(subword, production);

				ciuiCheck(subword, production);
			}

			syllabationCheck(production, derivedWord);
		}
		catch(IllegalArgumentException e){
			String message = e.getMessage();
			String rulesSequence = production.getRulesSequence();
			if(rulesSequence.length() > 0)
				message += " (via " + rulesSequence + ")";
			throw new IllegalArgumentException(message);
		}
	}

	private void morphologicalFieldCheck(RuleProductionEntry production) throws IllegalArgumentException{
		String[] morphologicalFields = production.getMorphologicalFields();
		if(Objects.nonNull(morphologicalFields))
			for(String morphologicalField : morphologicalFields){
				if(morphologicalField.length() < 4)
					throw new IllegalArgumentException("Word " + production.getWord() + " has an invalid morphological field prefix: " + morphologicalField);

				String morphologicalFieldPrefix = morphologicalField.substring(0, 3);
				if(!DATA_FIELDS.containsKey(morphologicalFieldPrefix))
					throw new IllegalArgumentException("Word " + production.getWord() + " has an unknown morphological field prefix: " + morphologicalField);

				Set<String> morphologicalFieldTypes = DATA_FIELDS.get(morphologicalFieldPrefix);
				if(Objects.nonNull(morphologicalFieldTypes) && !morphologicalFieldTypes.contains(morphologicalField.substring(3)))
					throw new IllegalArgumentException("Word " + production.getWord() + " has an unknown morphological field value: " + morphologicalField);
			}
	}

	private void vanishingElCheck(RuleProductionEntry production) throws IllegalArgumentException{
		String derivedWord = production.getWord();
		if(derivedWord.contains(GraphemeVEC.L_STROKE_GRAPHEME) && PatternService.find(derivedWord, NON_VANISHING_EL))
			throw new IllegalArgumentException("Word with ƚ cannot contain non–ƚ, " + derivedWord);
		if(StringUtils.contains(production.getWord(), GraphemeVEC.L_STROKE_GRAPHEME)
				&& (StringUtils.contains(production.getWord(), GraphemeVEC.D_STROKE_GRAPHEME) || StringUtils.contains(production.getWord(), GraphemeVEC.T_STROKE_GRAPHEME)))
			throw new IllegalArgumentException("Word with ƚ cannot contain đ or ŧ, " + derivedWord);
		if(PatternService.find(derivedWord, VANISHING_EL_NEAR_CONSONANT))
			throw new IllegalArgumentException("Word with ƚ near a consonant, " + derivedWord);
		if(derivedWord.contains(GraphemeVEC.L_STROKE_GRAPHEME) && production.containsContinuationFlag(NORTHERN_PLURAL_RULE))
			throw new IllegalArgumentException("Word with ƚ cannot contain rule " + NORTHERN_PLURAL_RULE + " or "
				+ NORTHERN_PLURAL_STRESSED_RULE + ", " + derivedWord);
	}

	private void incompatibilityCheck(RuleProductionEntry production) throws IllegalArgumentException{
		variantIncompatibilityCheck(production, MISMATCH_CHECKS_MUST_CONTAINS_LH, GraphemeVEC.L_STROKE_GRAPHEME);
		variantIncompatibilityCheck(production, MISMATCH_CHECKS_MUST_CONTAINS_DH_OR_TH, GraphemeVEC.D_STROKE_GRAPHEME, GraphemeVEC.T_STROKE_GRAPHEME);

		continuationFlagIncompatibilityCheck(production, ADJECTIVE_FIRST_CLASS_RULE, ADJECTIVE_FIRST_CLASS_MISMATCH_CHECKS);
		continuationFlagIncompatibilityCheck(production, VARIANT_TRANSFORMATIONS_END_RULE, VARIANT_TRANSFORMATION_END_MISMATCH_CHECKS);
		continuationFlagIncompatibilityCheck(production, VARIANT_TRANSFORMATIONS_FEMININE_RULE, VARIANT_TRANSFORMATION_FEMININE_MISMATCH_CHECKS);
		continuationFlagIncompatibilityCheck(production, GUA_TO_VA_RULE, GUA_TO_VA_CHECKS);
	}

	private void variantIncompatibilityCheck(RuleProductionEntry production, Set<MatcherEntry> checks, String... contains)
			throws IllegalArgumentException{
		if(StringUtils.containsAny(production.getWord(), contains))
			for(MatcherEntry entry : checks)
				entry.match(production);
	}

	private void continuationFlagIncompatibilityCheck(RuleProductionEntry production, String continuationFlag, Set<MatcherEntry> checks)
			throws IllegalArgumentException{
		if(production.containsContinuationFlag(continuationFlag))
			for(MatcherEntry entry : checks)
				entry.match(production);
	}

	private void metaphonesisCheck(RuleProductionEntry production, String line) throws IllegalArgumentException{
		if(!production.isPartOfSpeech(POS_PROPER_NOUN) && !production.isPartOfSpeech(POS_ARTICLE)){
			boolean hasMetaphonesisFlag = production.containsContinuationFlag(METAPHONESIS_RULE);
			boolean hasPluralFlag = production.containsContinuationFlag(PLURAL_NOUN_MASCULINE_RULE, ADJECTIVE_FIRST_CLASS_RULE, ADJECTIVE_SECOND_CLASS_RULE,
				ADJECTIVE_THIRD_CLASS_RULE);
			if(hasMetaphonesisFlag && !hasPluralFlag)
				throw new IllegalArgumentException("Metaphonesis not needed for " + line + " (missing plural flag), handle " + METAPHONESIS_RULE);
			else{
				boolean canHaveMetaphonesis = wordGenerator.isAffixProductive(production.getWord(), METAPHONESIS_RULE);
				if(canHaveMetaphonesis ^ hasMetaphonesisFlag){
					if(canHaveMetaphonesis && hasPluralFlag)
						throw new IllegalArgumentException("Metaphonesis missing for " + line + ", add " + METAPHONESIS_RULE);
					else if(!canHaveMetaphonesis && !hasPluralFlag)
						throw new IllegalArgumentException("Metaphonesis not needed for " + line + ", remove " + METAPHONESIS_RULE);
				}
			}
		}
	}

	private void northernPluralCheck(RuleProductionEntry production) throws IllegalArgumentException{
		String word = production.getWord();
		if(!production.isPartOfSpeech(POS_ARTICLE) && !production.isPartOfSpeech(POS_PRONOUN) && !production.isPartOfSpeech(POS_PROPER_NOUN)
				&& hyphenator.hyphenate(word).countSyllabes() > 1){
			List<String> subwords = hyphenator.splitIntoCompounds(word);
			String rule = (!WordVEC.hasStressedGrapheme(subwords.get(subwords.size() - 1)) || PatternService.find(word, MATCHER_NORTHERN_PLURAL)? NORTHERN_PLURAL_RULE: NORTHERN_PLURAL_STRESSED_RULE);
			boolean hasNorthernPluralFlag = production.containsContinuationFlag(rule);
			boolean canHaveNorthernPlural = (production.containsContinuationFlag(PLURAL_NOUN_MASCULINE_RULE, ADJECTIVE_FIRST_CLASS_RULE, ADJECTIVE_SECOND_CLASS_RULE, ADJECTIVE_THIRD_CLASS_RULE)
				&& !word.contains(GraphemeVEC.L_STROKE_GRAPHEME) && !word.endsWith(MAN) && wordGenerator.isAffixProductive(word, rule));
			if(canHaveNorthernPlural ^ hasNorthernPluralFlag){
				if(canHaveNorthernPlural)
					throw new IllegalArgumentException("Northern plural missing for " + word + ", add " + rule);
				else if(!canHaveNorthernPlural)
					throw new IllegalArgumentException("Northern plural not needed for " + word + ", remove " + NORTHERN_PLURAL_RULE + " or " + NORTHERN_PLURAL_STRESSED_RULE);
			}
		}
	}

	private void mismatchCheck(RuleProductionEntry production) throws IllegalArgumentException{
		variantIncompatibilityCheck(production, MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH);
	}

	private void variantIncompatibilityCheck(RuleProductionEntry production, Set<MatcherEntry> checks)
			throws IllegalArgumentException{
		if(canContainsVanishingEl(production.getWord()))
			for(MatcherEntry entry : checks)
				entry.match(production);
	}

	//(^[ʼ']?l|[aeiouàèéíòóú]l)[aeiouàèéíòóú]
	private static boolean canContainsVanishingEl(String word){
		boolean result = false;
		int size = word.length();
		if(size > 1){
			int index = (WordVEC.isApostrophe(word.charAt(0))? 1: 0);
			if(index + 1 < size){
				char chr = word.charAt(index);
				result = (chr == 'l' && WordVEC.isVowel(word.charAt(index + 1)));
				while(!result){
					index = WordVEC.getFirstVowelIndex(word, index);
					if(index < 0 || index + 2 >= size)
						break;

					if(word.charAt(index + 1) == 'l' && WordVEC.isVowel(word.charAt(index + 2)))
						result = true;

					index ++;
				}
			}
		}
		return result;
	}

	private void finalSonorizationCheck(RuleProductionEntry production) throws IllegalArgumentException{
//		if(!production.hasProductionRules()&& !production.isPartOfSpeech(POS_VERB) && !production.isPartOfSpeech(POS_PROPER_NOUN)){
//			boolean hasFinalSonorizationFlag = production.containsContinuationFlag(FINAL_SONORIZATION_RULE);
//			boolean canHaveFinalSonorization = (!production.getWord().toLowerCase(Locale.ROOT).contains(GraphemeVEC.L_STROKE_GRAPHEME) && wordGenerator.isAffixProductive(production.getWord(), FINAL_SONORIZATION_RULE));
//			if(canHaveFinalSonorization ^ hasFinalSonorizationFlag){
//				if(canHaveFinalSonorization)
//					throw new IllegalArgumentException("Final sonorization missing for " + production.getWord() + ", add " + FINAL_SONORIZATION_RULE);
//				else if(!canHaveFinalSonorization)
//					throw new IllegalArgumentException("Final sonorization not needed for " + production.getWord() + ", remove " + FINAL_SONORIZATION_RULE);
//			}
//		}
	}

	private void accentCheck(String subword, RuleProductionEntry production) throws IllegalArgumentException{
		int accents = WordVEC.countAccents(subword);
		if(accents > 1 && !MULTIPLE_ACCENTED_INTERJECTIONS.contains(subword))
			throw new IllegalArgumentException("Word " + production.getWord() + " cannot have multiple accents");

		if(accents == 1 && !subword.equals(WordVEC.unmarkDefaultStress(subword))){
			boolean elBetweenVowelsRemoval = false;
			List<AffixEntry> appliedRules = production.getAppliedRules();
			if(Objects.nonNull(appliedRules))
				for(AffixEntry appliedRule : appliedRules)
					if(PatternService.find(appliedRule.toString(), L_BETWEEN_VOWELS)){
						elBetweenVowelsRemoval = true;
						break;
					}
			if(!elBetweenVowelsRemoval)
				throw new IllegalArgumentException("Word " + production.getWord() + " cannot have an accent here");
		}
	}

	private void ciuiCheck(String subword, RuleProductionEntry production) throws IllegalArgumentException{
		if(!production.isPartOfSpeech(POS_NUMERAL_LATIN)){
			String phonemizedSubword = GraphemeVEC.handleJHJWIUmlautPhonemes(subword);
			if(PatternService.find(phonemizedSubword, CIJJHNHIV))
				throw new IllegalArgumentException("Word " + production.getWord() + " cannot have [cijɉñ]iV");
		}
	}

	private void syllabationCheck(RuleProductionEntry production, String derivedWord) throws IllegalArgumentException{
		if((ENABLE_VERB_CHECK || !production.isPartOfSpeech(POS_VERB)) && !production.isPartOfSpeech(POS_NUMERAL_LATIN) && !production.isPartOfSpeech(POS_UNIT_OF_MEASURE)){
			derivedWord = derivedWord.toLowerCase(Locale.ROOT);
			if(!UNSYLLABABLE_INTERJECTIONS.contains(derivedWord) && !MULTIPLE_ACCENTED_INTERJECTIONS.contains(derivedWord)){
				String correctedDerivedWord = correctOrthography(derivedWord);
				if(!correctedDerivedWord.equals(derivedWord))
					throw new IllegalArgumentException("Word " + derivedWord + " is mispelled (should be " + correctedDerivedWord + ")");

				if(derivedWord.length() > 1){
					HyphenationInterface hyphenation = hyphenator.hyphenate(derivedWord);
					if(hyphenation.hasErrors())
						throw new IllegalArgumentException("Word " + derivedWord + " (" + hyphenation.formatHyphenation(new StringJoiner(SLASH), syllabe -> ASTERISK + syllabe + ASTERISK)
							+ ") is not syllabable");
				}
			}
		}
	}


	@Override
	public boolean isConsonant(char chr){
		return WordVEC.isConsonant(chr);
	}

	@Override
	public boolean shouldBeProcessedForMinimalPair(RuleProductionEntry production){
		String word = production.getWord();
		return (word.length() >= MINIMAL_PAIR_MINIMUM_LENGTH
			&& word.indexOf('ƚ') < 0
			&& word.indexOf('ɉ') < 0
			&& (production.containsMorphologicalField(WordGenerator.TAG_PART_OF_SPEECH + POS_NOUN)
			|| production.containsMorphologicalField(WordGenerator.TAG_PART_OF_SPEECH + POS_ADJECTIVE)
			|| production.containsMorphologicalField(WordGenerator.TAG_PART_OF_SPEECH + POS_ADJECTIVE_POSSESSIVE)
			|| production.containsMorphologicalField(WordGenerator.TAG_PART_OF_SPEECH + POS_ADJECTIVE_DEMONSTRATIVE)
			|| production.containsMorphologicalField(WordGenerator.TAG_PART_OF_SPEECH + POS_ADJECTIVE_IDENTIFICATIVE)
			|| production.containsMorphologicalField(WordGenerator.TAG_PART_OF_SPEECH + POS_ADJECTIVE_INTERROGATIVE)
			|| production.containsMorphologicalField(WordGenerator.TAG_PART_OF_SPEECH + POS_QUANTIFIER)
			|| production.containsMorphologicalField(WordGenerator.TAG_PART_OF_SPEECH + POS_PRONOUN)
			|| production.containsMorphologicalField(WordGenerator.TAG_PART_OF_SPEECH + POS_PREPOSITION)
			|| production.containsMorphologicalField(WordGenerator.TAG_PART_OF_SPEECH + POS_ADVERB)
			|| production.containsMorphologicalField(WordGenerator.TAG_PART_OF_SPEECH + POS_CONJUNCTION)));
	}

	@Override
	public String prepareTextForFilter(String text){
		text = super.prepareTextForFilter(text);

		return correctOrthography(text);
	}

	@Override
	public String correctOrthography(String text){
		return orthography.correctOrthography(text);
	}

}
