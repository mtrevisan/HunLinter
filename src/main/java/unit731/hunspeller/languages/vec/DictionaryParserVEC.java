package unit731.hunspeller.languages.vec;

import java.io.File;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.AffixEntry;
import unit731.hunspeller.parsers.dictionary.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.RuleProductionEntry;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.hyphenation.Hyphenation;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.PatternService;


public class DictionaryParserVEC extends DictionaryParser{

	private static final String START_TAGS = "(?<!\\\\)\\/";

	private static final String VERB_1ST_RULE_NON_VANISHING_EL = "a1";
	private static final String VERB_1ST_RULE_VANISHING_EL = "a2";
	private static final String VERB_2ND_RULE_NON_VANISHING_EL = "b1";
	private static final String VERB_2ND_RULE_VANISHING_EL = "b2";
	private static final String VERB_3RD_IS_RULE_NON_VANISHING_EL = "c1";
	private static final String VERB_3RD_IS_RULE_VANISHING_EL = "c2";
	private static final String VERB_3RD_NO_IS_RULE_NON_VANISHING_EL = "d1";
	private static final String VERB_3RD_NO_IS_RULE_VANISHING_EL = "d2";
	private static final String VERB_3RD_BOTH_IS_RULE_NON_VANISHING_EL = "e1";
	private static final String VERB_3RD_BOTH_IS_RULE_VANISHING_EL = "e2";
	private static final String VERB_DAR_RULE_NON_VANISHING_EL = "f1";
	private static final String VERB_DAR_RULE_VANISHING_EL = "f2";
	private static final String VERB_TOLER_RULE_NON_VANISHING_EL = "k1";
	private static final String VERB_TOLER_RULE_VANISHING_EL = "k2";
	private static final String VERB_DIXER_RULE_NON_VANISHING_EL = "n1";
	private static final String VERB_DIXER_RULE_VANISHING_EL = "n2";
	private static final String PROCOMPLEMENTAR_VERB_DEFINITE_RULE_NON_VANISHING_EL = "E1";
	private static final String PROCOMPLEMENTAR_VERB_DEFINITE_RULE_VANISHING_EL = "E2";
	private static final String PROCOMPLEMENTAR_VERB_INDEFINITE_RULE_NON_VANISHING_EL = "G1";
	private static final String PROCOMPLEMENTAR_VERB_INDEFINITE_RULE_VANISHING_EL = "G2";
	private static final String PROCOMPLEMENTAR_VERB_IMPERATIVE_RULE_NON_VANISHING_EL = "F1";
	private static final String PROCOMPLEMENTAR_VERB_IMPERATIVE_RULE_VANISHING_EL = "F2";
	private static final String INTERROGATIVES_3RD_PERSON_RULE_NON_VANISHING_EL = "P1";
	private static final String INTERROGATIVES_3RD_PERSON_RULE_VANISHING_EL = "P2";
	private static final String INTERROGATIVES_3RD_PERSON_CONDITIONAL_RULE_NON_VANISHING_EL = "Q1";
	private static final String INTERROGATIVES_3RD_PERSON_CONDITIONAL_RULE_VANISHING_EL = "Q2";
	private static final String ADJECTIVE_FIRST_CLASS_RULE = "B0";
	private static final String ADJECTIVE_SECOND_CLASS_RULE = "C0";
	private static final String ADJECTIVE_THIRD_CLASS_RULE = "D0";
	private static final String PLURAL_NOUN_MASCULINE_RULE = "T0";
	private static final String VARIANT_TRANSFORMATIONS_RULE_1 = "T2";
	private static final String VARIANT_TRANSFORMATIONS_RULE_2 = "T3";
	private static final String VARIANT_TRANSFORMATIONS_RULE_4 = "T4";
	private static final String METAPHONESIS_RULE = "mf";
	private static final String PLANTS_AND_CRAFTS_RULE_NON_VANISHING_EL = "V0";
	private static final String PLANTS_AND_CRAFTS_RULE_VANISHING_EL = "V1";
	private static final String DEVERBAL_NOMINALS_MENTO_RULE_NON_VANISHING_EL = "r0";
	private static final String DEVERBAL_NOMINALS_MENTO_RULE_VANISHING_EL = "r1";
	private static final String DEVERBAL_NOMINALS_THION_RULE_NON_VANISHING_EL = "s0";
	private static final String DEVERBAL_NOMINALS_SION_RULE_NON_VANISHING_EL = "s1";
	private static final String DEVERBAL_NOMINALS_SION_RULE_VANISHING_EL = "s2";
	private static final String DEVERBAL_NOMINALS_IXMO_RULE_NON_VANISHING_EL = "S0";
	private static final String DEVERBAL_NOMINALS_IXMO_RULE_VANISHING_EL = "S1";
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
	private static final String PEJORATIVE_ASO_RULE_VANISHING_EL = "<1";
	private static final String PEJORATIVE_ASO_RULE_NON_VANISHING_EL = "<2";
	private static final String NORTHERN_PLURAL_RULE = "U0";
	private static final String NORTHERN_PLURAL_STRESSED_RULE = "u0";
	private static final String COLLECTIVE_NOUNS_RULE = "Y0";
	private static final String FINAL_SONORIZATION_RULE = "I0";

	private static final Matcher MISMATCHED_VARIANTS = PatternService.matcher("ƚ[^ŧđ]*[ŧđ]|[ŧđ][^ƚ]*ƚ");
	private static final Matcher NON_VANISHING_EL = PatternService.matcher("(^|[aàeèéiíoòóuúAÀEÈÉIÍOÒÓUÚʼ-])l([aàeèéiíoòóuúAÀEÈÉIÍOÒÓUÚʼ-]|$)");
	private static final Matcher VANISHING_EL_NEAR_CONSONANT = PatternService.matcher("[^aàeèéiíoòóuúAÀEÈÉIÍOÒÓUÚʼ-]ƚ|ƚ[^aàeèéiíoòóuúAÀEÈÉIÍOÒÓUÚʼ]");

	private static final Matcher L_BETWEEN_VOWELS = PatternService.matcher("l i l$");
	private static final Matcher CIJJHNHIV = PatternService.matcher("[ci" + GraphemeVEC.JJH_PHONEME + "ɉñ]j[aàeèéiíoòóuú]");

	private static final Pattern REGEX_PATTERN_HYPHEN_MINUS = PatternService.pattern(HyphenationParser.HYPHEN_MINUS);

	private static final String NON_VANISHING_L = "(^l|[aeiouàèéíòóú]l)[aeiouàèéíòóú][^ƚ/]*" + START_TAGS;
	private static final String NON_VANISHING_L_NOT_ENDING_IN_A = "(^l|[aeiouàèéíòóú]l)[aeiouàèéíòóú][^ƚ/]*[^a]" + START_TAGS;
	private static final String VANISHING_L = "ƚ.+?" + START_TAGS;
	private static final String VANISHING_L_NOT_ENDING_IN_A = "ƚ.*[^a]" + START_TAGS;

	private static final class MatcherEntry{

		private static final String CANNOT_USE_RULE_WITH_LH = "Cannot use {0} rule with ƚ";
		private static final String CANNOT_USE_RULE_WITH_LH_USE_INSTEAD = "Cannot use {0} rule with ƚ, use {1}";
		private static final String CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD = "Cannot use {0} rule with non-ƚ, use {1}";
		private static final String CANNOT_USE_RULE_WITH_TH_OR_DH_USE_INSTEAD = "Cannot use {0} rule with đ or ŧ, use {1}";

		private final Matcher matcher;
		private final List<String> ruleFlags;
		private final String error;


		public MatcherEntry(String matcher, String pattern, Object ... arguments){
			this.matcher = PatternService.matcher(matcher);
			ruleFlags = null;
			this.error = MessageFormat.format(pattern, arguments);
		}

		public MatcherEntry(List<String> ruleFlags, String pattern, Object ... arguments){
			matcher = null;
			this.ruleFlags = ruleFlags;
			//take last argument as the concatenation of the ruleFlags
			List<Object> args = new ArrayList<>(arguments.length + 1);
			args.addAll(Arrays.asList(arguments));
			args.add(String.join(" or ", ruleFlags));
			this.error = MessageFormat.format(pattern, args.toArray(new Object[args.size()]));
		}

		public void match(String word) throws IllegalArgumentException{
			if(PatternService.find(word, matcher))
				throw new IllegalArgumentException(error + " for " + word);
		}
	}
	private static final boolean ENABLE_VERB_CHECK = false;
	private static final Set<MatcherEntry> MISMATCH_CHECKS_MUST_CONTAINS_LH = new HashSet<>();
	private static final Set<MatcherEntry> MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH = new HashSet<>();
	private static final Set<MatcherEntry> MISMATCH_CHECKS_MUST_CONTAINS_DH_OR_TH = new HashSet<>();
	static{
		if(ENABLE_VERB_CHECK){
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L + VERB_1ST_RULE_NON_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, VERB_1ST_RULE_NON_VANISHING_EL, VERB_1ST_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(NON_VANISHING_L + VERB_1ST_RULE_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, VERB_1ST_RULE_VANISHING_EL, VERB_1ST_RULE_NON_VANISHING_EL));
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L + VERB_2ND_RULE_NON_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, VERB_2ND_RULE_NON_VANISHING_EL, VERB_2ND_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(NON_VANISHING_L + VERB_2ND_RULE_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, VERB_2ND_RULE_VANISHING_EL, VERB_2ND_RULE_NON_VANISHING_EL));
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L + VERB_3RD_IS_RULE_NON_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, VERB_3RD_IS_RULE_NON_VANISHING_EL, VERB_3RD_IS_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(NON_VANISHING_L + VERB_3RD_IS_RULE_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, VERB_3RD_IS_RULE_VANISHING_EL, VERB_3RD_IS_RULE_NON_VANISHING_EL));
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L + VERB_3RD_NO_IS_RULE_NON_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, VERB_3RD_NO_IS_RULE_NON_VANISHING_EL, VERB_3RD_NO_IS_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(NON_VANISHING_L + VERB_3RD_NO_IS_RULE_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, VERB_3RD_NO_IS_RULE_VANISHING_EL, VERB_3RD_NO_IS_RULE_NON_VANISHING_EL));
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L + VERB_3RD_BOTH_IS_RULE_NON_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, VERB_3RD_BOTH_IS_RULE_NON_VANISHING_EL, VERB_3RD_BOTH_IS_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(NON_VANISHING_L + VERB_3RD_BOTH_IS_RULE_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, VERB_3RD_BOTH_IS_RULE_VANISHING_EL, VERB_3RD_BOTH_IS_RULE_NON_VANISHING_EL));
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L + VERB_DAR_RULE_NON_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, VERB_DAR_RULE_NON_VANISHING_EL, VERB_DAR_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(NON_VANISHING_L + VERB_DAR_RULE_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, VERB_DAR_RULE_VANISHING_EL, VERB_DAR_RULE_NON_VANISHING_EL));
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L + VERB_TOLER_RULE_NON_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, VERB_TOLER_RULE_NON_VANISHING_EL, VERB_TOLER_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(NON_VANISHING_L + VERB_TOLER_RULE_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, VERB_TOLER_RULE_VANISHING_EL, VERB_TOLER_RULE_NON_VANISHING_EL));
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L + VERB_DIXER_RULE_NON_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, VERB_DIXER_RULE_NON_VANISHING_EL, VERB_DIXER_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(NON_VANISHING_L + VERB_DIXER_RULE_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, VERB_DIXER_RULE_VANISHING_EL, VERB_DIXER_RULE_NON_VANISHING_EL));
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L + PROCOMPLEMENTAR_VERB_DEFINITE_RULE_NON_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, PROCOMPLEMENTAR_VERB_DEFINITE_RULE_NON_VANISHING_EL, PROCOMPLEMENTAR_VERB_DEFINITE_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(NON_VANISHING_L + PROCOMPLEMENTAR_VERB_DEFINITE_RULE_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, PROCOMPLEMENTAR_VERB_DEFINITE_RULE_VANISHING_EL, PROCOMPLEMENTAR_VERB_DEFINITE_RULE_NON_VANISHING_EL));
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L + PROCOMPLEMENTAR_VERB_INDEFINITE_RULE_NON_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, PROCOMPLEMENTAR_VERB_INDEFINITE_RULE_NON_VANISHING_EL, PROCOMPLEMENTAR_VERB_INDEFINITE_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(NON_VANISHING_L + PROCOMPLEMENTAR_VERB_INDEFINITE_RULE_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, PROCOMPLEMENTAR_VERB_INDEFINITE_RULE_VANISHING_EL, PROCOMPLEMENTAR_VERB_INDEFINITE_RULE_NON_VANISHING_EL));
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L + PROCOMPLEMENTAR_VERB_IMPERATIVE_RULE_NON_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, PROCOMPLEMENTAR_VERB_IMPERATIVE_RULE_NON_VANISHING_EL, PROCOMPLEMENTAR_VERB_IMPERATIVE_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(NON_VANISHING_L + PROCOMPLEMENTAR_VERB_IMPERATIVE_RULE_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, PROCOMPLEMENTAR_VERB_IMPERATIVE_RULE_VANISHING_EL, PROCOMPLEMENTAR_VERB_IMPERATIVE_RULE_NON_VANISHING_EL));
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L + INTERROGATIVES_3RD_PERSON_RULE_NON_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, INTERROGATIVES_3RD_PERSON_RULE_NON_VANISHING_EL, INTERROGATIVES_3RD_PERSON_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(NON_VANISHING_L + INTERROGATIVES_3RD_PERSON_RULE_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, INTERROGATIVES_3RD_PERSON_RULE_VANISHING_EL, INTERROGATIVES_3RD_PERSON_RULE_NON_VANISHING_EL));
			MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L + INTERROGATIVES_3RD_PERSON_CONDITIONAL_RULE_NON_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, INTERROGATIVES_3RD_PERSON_CONDITIONAL_RULE_NON_VANISHING_EL, INTERROGATIVES_3RD_PERSON_CONDITIONAL_RULE_VANISHING_EL));
			MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(NON_VANISHING_L + INTERROGATIVES_3RD_PERSON_CONDITIONAL_RULE_VANISHING_EL,
				MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, INTERROGATIVES_3RD_PERSON_CONDITIONAL_RULE_VANISHING_EL, INTERROGATIVES_3RD_PERSON_CONDITIONAL_RULE_NON_VANISHING_EL));
		}

		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L + FINAL_SONORIZATION_RULE,
			MatcherEntry.CANNOT_USE_RULE_WITH_LH, FINAL_SONORIZATION_RULE));
		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L + VARIANT_TRANSFORMATIONS_RULE_1,
			MatcherEntry.CANNOT_USE_RULE_WITH_LH, VARIANT_TRANSFORMATIONS_RULE_4));

		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L + DEVERBAL_NOMINALS_MENTO_RULE_NON_VANISHING_EL,
			MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, DEVERBAL_NOMINALS_MENTO_RULE_NON_VANISHING_EL, DEVERBAL_NOMINALS_MENTO_RULE_VANISHING_EL));
		MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(NON_VANISHING_L + DEVERBAL_NOMINALS_MENTO_RULE_VANISHING_EL,
			MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, DEVERBAL_NOMINALS_MENTO_RULE_VANISHING_EL, DEVERBAL_NOMINALS_MENTO_RULE_NON_VANISHING_EL));

		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L + PLANTS_AND_CRAFTS_RULE_NON_VANISHING_EL,
			MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, PLANTS_AND_CRAFTS_RULE_NON_VANISHING_EL, PLANTS_AND_CRAFTS_RULE_VANISHING_EL));
		MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(NON_VANISHING_L + PLANTS_AND_CRAFTS_RULE_VANISHING_EL,
			MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, PLANTS_AND_CRAFTS_RULE_VANISHING_EL, PLANTS_AND_CRAFTS_RULE_NON_VANISHING_EL));

		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L + "[" + DEVERBAL_NOMINALS_THION_RULE_NON_VANISHING_EL + DEVERBAL_NOMINALS_SION_RULE_NON_VANISHING_EL + "]",
			MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, DEVERBAL_NOMINALS_SION_RULE_NON_VANISHING_EL, DEVERBAL_NOMINALS_SION_RULE_VANISHING_EL));
		MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(NON_VANISHING_L + DEVERBAL_NOMINALS_SION_RULE_VANISHING_EL,
			MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, DEVERBAL_NOMINALS_THION_RULE_NON_VANISHING_EL, DEVERBAL_NOMINALS_SION_RULE_NON_VANISHING_EL, DEVERBAL_NOMINALS_SION_RULE_VANISHING_EL));

		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L + DEVERBAL_NOMINALS_IXMO_RULE_NON_VANISHING_EL,
			MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, DEVERBAL_NOMINALS_IXMO_RULE_NON_VANISHING_EL, DEVERBAL_NOMINALS_IXMO_RULE_VANISHING_EL));
		MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(NON_VANISHING_L + DEVERBAL_NOMINALS_IXMO_RULE_VANISHING_EL,
			MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, DEVERBAL_NOMINALS_IXMO_RULE_VANISHING_EL, DEVERBAL_NOMINALS_IXMO_RULE_NON_VANISHING_EL));

		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L + ADVERB_MENTE_RULE_NON_VANISHING_EL,
			MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, ADVERB_MENTE_RULE_NON_VANISHING_EL, ADVERB_MENTE_RULE_VANISHING_EL));
		MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(NON_VANISHING_L + ADVERB_MENTE_RULE_VANISHING_EL,
			MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, ADVERB_MENTE_RULE_VANISHING_EL, ADVERB_MENTE_RULE_NON_VANISHING_EL));

		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L + DIMINUTIVE_ETO_RULE_NON_VANISHING_EL,
			MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, DIMINUTIVE_ETO_RULE_NON_VANISHING_EL, DIMINUTIVE_ETO_RULE_VANISHING_EL));
		MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(NON_VANISHING_L + DIMINUTIVE_ETO_RULE_VANISHING_EL,
			MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, DIMINUTIVE_ETO_RULE_VANISHING_EL, DIMINUTIVE_ETO_RULE_NON_VANISHING_EL));
		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L + DIMINUTIVE_EL_RULE_NON_VANISHING_EL,
			MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, DIMINUTIVE_EL_RULE_NON_VANISHING_EL, DIMINUTIVE_EL_RULE_VANISHING_EL));
		MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(NON_VANISHING_L + DIMINUTIVE_EL_RULE_VANISHING_EL,
			MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, DIMINUTIVE_EL_RULE_VANISHING_EL, DIMINUTIVE_EL_RULE_NON_VANISHING_EL));

		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L_NOT_ENDING_IN_A + Pattern.quote(AUGMENTATIVE_OTO_RULE_NON_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, AUGMENTATIVE_OTO_RULE_NON_VANISHING_EL, AUGMENTATIVE_OTO_RULE_VANISHING_EL));
		MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(NON_VANISHING_L_NOT_ENDING_IN_A + Pattern.quote(AUGMENTATIVE_OTO_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, AUGMENTATIVE_OTO_RULE_VANISHING_EL, AUGMENTATIVE_OTO_RULE_NON_VANISHING_EL));
		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L_NOT_ENDING_IN_A + Pattern.quote(AUGMENTATIVE_ON_RULE_NON_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, AUGMENTATIVE_ON_RULE_NON_VANISHING_EL, AUGMENTATIVE_ON_RULE_VANISHING_EL));
		MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(NON_VANISHING_L_NOT_ENDING_IN_A + Pattern.quote(AUGMENTATIVE_ON_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, AUGMENTATIVE_ON_RULE_VANISHING_EL, AUGMENTATIVE_ON_RULE_NON_VANISHING_EL));

		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L_NOT_ENDING_IN_A + PEJORATIVE_ATHO_RULE_NON_VANISHING_EL,
			MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, PEJORATIVE_ATHO_RULE_NON_VANISHING_EL, PEJORATIVE_ASO_RULE_VANISHING_EL));
		MISMATCH_CHECKS_MUST_CONTAINS_DH_OR_TH.add(new MatcherEntry("[đŧ].*[^a]" + START_TAGS + PEJORATIVE_ASO_RULE_VANISHING_EL,
			MatcherEntry.CANNOT_USE_RULE_WITH_TH_OR_DH_USE_INSTEAD, PEJORATIVE_ASO_RULE_VANISHING_EL, PEJORATIVE_ASO_RULE_NON_VANISHING_EL));

		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(VANISHING_L_NOT_ENDING_IN_A + PEJORATIVE_ATO_RULE_NON_VANISHING_EL,
			MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, PEJORATIVE_ATO_RULE_NON_VANISHING_EL, PEJORATIVE_ATO_RULE_VANISHING_EL));
		MISMATCH_CHECKS_MUST_CONTAINS_DH_OR_TH.add(new MatcherEntry("[đŧ].*[^a]" + START_TAGS + PEJORATIVE_ATO_RULE_VANISHING_EL,
			MatcherEntry.CANNOT_USE_RULE_WITH_TH_OR_DH_USE_INSTEAD, PEJORATIVE_ATO_RULE_VANISHING_EL, PEJORATIVE_ATO_RULE_NON_VANISHING_EL));
	}

	private static final Matcher MISSING_PLURAL_AFTER_N_OR_L = PatternService.matcher("^[^ƚ]*[eaouèàòéóú][ln]\\/[^ZUu\\t]+\\t");
	private static final Matcher ENDS_IN_MAN = PatternService.matcher("man\\/");

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
		ADJECTIVE_FIRST_CLASS_MISMATCH_CHECKS.add(new MatcherEntry(Arrays.asList(COLLECTIVE_NOUNS_RULE),
			WORD_WITH_RULE_CANNOT_HAVE, ADJECTIVE_FIRST_CLASS_RULE));
		ADJECTIVE_FIRST_CLASS_MISMATCH_CHECKS.add(new MatcherEntry(Arrays.asList(PEJORATIVE_ATHO_RULE_NON_VANISHING_EL, PEJORATIVE_ASO_RULE_VANISHING_EL),
			WORD_WITH_RULE_CANNOT_HAVE, ADJECTIVE_FIRST_CLASS_RULE));
	}

	//also V0/)0 - &0/&1
	private static final Set<MatcherEntry> VARIANT_TRANSFORMATION_1_MISMATCH_CHECKS = new HashSet<>();
	static{
		VARIANT_TRANSFORMATION_1_MISMATCH_CHECKS.add(new MatcherEntry(Arrays.asList("V0"),
			WORD_WITH_RULE_CANNOT_HAVE, VARIANT_TRANSFORMATIONS_RULE_1));
		VARIANT_TRANSFORMATION_1_MISMATCH_CHECKS.add(new MatcherEntry(Arrays.asList("v0"),
			WORD_WITH_RULE_CANNOT_HAVE, VARIANT_TRANSFORMATIONS_RULE_1));
		VARIANT_TRANSFORMATION_1_MISMATCH_CHECKS.add(new MatcherEntry(Arrays.asList("T1"),
			WORD_WITH_RULE_CANNOT_HAVE, VARIANT_TRANSFORMATIONS_RULE_1));
		VARIANT_TRANSFORMATION_1_MISMATCH_CHECKS.add(new MatcherEntry(Arrays.asList(DIMINUTIVE_ETO_RULE_NON_VANISHING_EL, DIMINUTIVE_ETO_RULE_VANISHING_EL),
			WORD_WITH_RULE_CANNOT_HAVE, VARIANT_TRANSFORMATIONS_RULE_1));
		VARIANT_TRANSFORMATION_1_MISMATCH_CHECKS.add(new MatcherEntry(Arrays.asList(AUGMENTATIVE_ON_RULE_NON_VANISHING_EL, AUGMENTATIVE_ON_RULE_VANISHING_EL),
			WORD_WITH_RULE_CANNOT_HAVE, VARIANT_TRANSFORMATIONS_RULE_1));
	}

	private static final Set<MatcherEntry> VARIANT_TRANSFORMATION_2_MISMATCH_CHECKS = new HashSet<>();
	static{
		VARIANT_TRANSFORMATION_2_MISMATCH_CHECKS.add(new MatcherEntry(Arrays.asList(PEJORATIVE_ATHO_RULE_NON_VANISHING_EL, PEJORATIVE_ASO_RULE_VANISHING_EL),
			WORD_WITH_RULE_CANNOT_HAVE, VARIANT_TRANSFORMATIONS_RULE_2));
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

	private static final List<String> UNSYLLABABLE_INTERJECTIONS = Arrays.asList("brr", "ehh", "mh", "ohh", "ssh", "iu");

	private static final int MINIMAL_PAIR_MINIMUM_LENGTH = 3;


	private final Orthography orthography = OrthographyVEC.getInstance();


	public DictionaryParserVEC(File dicFile, WordGenerator wordGenerator, Charset charset){
		super(dicFile, wordGenerator, charset);
	}

	@Override
	public void checkProduction(RuleProductionEntry production, FlagParsingStrategy strategy) throws IllegalArgumentException{
		try{
			if(!production.hasDataFields())
				throw new IllegalArgumentException("Line does not contains any data fields");

			dataFieldCheck(production);

			vanishingElCheck(production);

			incompatibilityCheck(production);

			String derivedWord = production.getWord();

			String derivedWordWithoutDataFields = derivedWord + strategy.joinRuleFlags(production.getRuleFlags());
			if(production.hasRuleFlags() && !production.isPartOfSpeech(POS_VERB) && !production.isPartOfSpeech(POS_ADVERB)){
				metaphonesisCheck(production, derivedWordWithoutDataFields);

				northernPluralCheck(production, derivedWordWithoutDataFields);
			}

			mismatchCheck(derivedWordWithoutDataFields);

			finalSonorizationCheck(production);

			String[] splittedWords = PatternService.split(derivedWord, REGEX_PATTERN_HYPHEN_MINUS);
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

	private void dataFieldCheck(RuleProductionEntry production) throws IllegalArgumentException{
		String[] dataFields = production.getDataFields();
		if(Objects.nonNull(dataFields))
			for(String dataField : dataFields){
				if(dataField.length() < 4)
					throw new IllegalArgumentException("Word " + production.getWord() + " has an invalid data field prefix: " + dataField);

				String dataFieldPrefix = dataField.substring(0, 3);
				if(!DATA_FIELDS.containsKey(dataFieldPrefix))
					throw new IllegalArgumentException("Word " + production.getWord() + " has an unknown data field prefix: " + dataField);

				Set<String> dataFieldTypes = DATA_FIELDS.get(dataFieldPrefix);
				if(Objects.nonNull(dataFieldTypes) && !dataFieldTypes.contains(dataField.substring(3)))
					throw new IllegalArgumentException("Word " + production.getWord() + " has an unknown data field value: " + dataField);
			}
	}

	private void vanishingElCheck(RuleProductionEntry production) throws IllegalArgumentException{
		String derivedWord = production.getWord();
		if(derivedWord.contains(GraphemeVEC.L_STROKE_GRAPHEME) && PatternService.find(derivedWord, NON_VANISHING_EL))
			throw new IllegalArgumentException("Word with ƚ cannot contain non-ƚ, " + derivedWord);
		if(PatternService.find(derivedWord, MISMATCHED_VARIANTS))
			throw new IllegalArgumentException("Word with ƚ cannot contain characters from another variant, " + derivedWord);
		if(PatternService.find(derivedWord, VANISHING_EL_NEAR_CONSONANT))
			throw new IllegalArgumentException("Word with ƚ near a consonant, " + derivedWord);
		if(derivedWord.contains(GraphemeVEC.L_STROKE_GRAPHEME) && production.containsRuleFlag(NORTHERN_PLURAL_RULE))
			throw new IllegalArgumentException("Word with ƚ cannot contain rule " + NORTHERN_PLURAL_RULE + " or "
				+ NORTHERN_PLURAL_STRESSED_RULE + ", " + derivedWord);
	}

	private void incompatibilityCheck(RuleProductionEntry production) throws IllegalArgumentException{
		commonIncompatibilityCheck(production, ADJECTIVE_FIRST_CLASS_RULE, ADJECTIVE_FIRST_CLASS_MISMATCH_CHECKS);
		commonIncompatibilityCheck(production, VARIANT_TRANSFORMATIONS_RULE_1, VARIANT_TRANSFORMATION_1_MISMATCH_CHECKS);
		commonIncompatibilityCheck(production, VARIANT_TRANSFORMATIONS_RULE_2, VARIANT_TRANSFORMATION_2_MISMATCH_CHECKS);
	}

	private void commonIncompatibilityCheck(RuleProductionEntry production, String ruleFlag, Set<MatcherEntry> checks)
			throws IllegalArgumentException{
		if(production.containsRuleFlag(ruleFlag))
			for(MatcherEntry entry : checks)
				for(String flag : entry.ruleFlags)
					if(production.containsRuleFlag(flag))
						throw new IllegalArgumentException(entry.error + " for " + production.getWord());
	}

	private void metaphonesisCheck(RuleProductionEntry production, String line) throws IllegalArgumentException{
		if(!production.isPartOfSpeech(POS_PROPER_NOUN) && !production.isPartOfSpeech(POS_ARTICLE)){
			boolean hasMetaphonesisFlag = production.containsRuleFlag(METAPHONESIS_RULE);
			boolean hasPluralFlag = production.containsRuleFlag(PLURAL_NOUN_MASCULINE_RULE, ADJECTIVE_FIRST_CLASS_RULE, ADJECTIVE_SECOND_CLASS_RULE, ADJECTIVE_THIRD_CLASS_RULE);
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

	private void northernPluralCheck(RuleProductionEntry production, String line) throws IllegalArgumentException{
		if(!production.isPartOfSpeech(POS_ARTICLE) && !production.isPartOfSpeech(POS_PRONOUN)
				&& !PatternService.find(line, ENDS_IN_MAN)
				&& PatternService.find(line, MISSING_PLURAL_AFTER_N_OR_L))
			throw new IllegalArgumentException("Plural missing after n or l for " + line + ", add "
				+ (WordVEC.isStressed(PatternService.clear(line, PatternService.matcher(START_TAGS)))? NORTHERN_PLURAL_STRESSED_RULE: NORTHERN_PLURAL_RULE));
	}

	private void mismatchCheck(String line) throws IllegalArgumentException{
		if(line.contains(GraphemeVEC.L_STROKE_GRAPHEME))
			for(MatcherEntry check : MISMATCH_CHECKS_MUST_CONTAINS_LH)
				check.match(line);
		else{
			for(MatcherEntry check : MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH)
				check.match(line);
			if(line.contains(GraphemeVEC.D_STROKE_GRAPHEME) || line.contains(GraphemeVEC.T_STROKE_GRAPHEME))
				for(MatcherEntry check : MISMATCH_CHECKS_MUST_CONTAINS_DH_OR_TH)
					check.match(line);
		}
	}

	private void finalSonorizationCheck(RuleProductionEntry production) throws IllegalArgumentException{
//		List<String> appliedRules = Optional.ofNullable(production.getAppliedRules())
//			.map(AffixEntry::getFlag)
//			.collect(Collectors.toSet());
//FIXME
		List<AffixEntry> appliedRules = production.getAppliedRules();
		if(false && !production.isPartOfSpeech(POS_VERB) && (Objects.isNull(appliedRules) || appliedRules.size() < 2)){
			boolean hasFinalSonorizationFlag = production.containsRuleFlag(FINAL_SONORIZATION_RULE);
			boolean canHaveFinalSonorization = wordGenerator.isAffixProductive(production.getWord(), FINAL_SONORIZATION_RULE);
			if(canHaveFinalSonorization ^ hasFinalSonorizationFlag){
				if(canHaveFinalSonorization)
					throw new IllegalArgumentException("Final sonorization missing for " + production.getWord() + ", add " + FINAL_SONORIZATION_RULE);
				else if(!canHaveFinalSonorization)
					throw new IllegalArgumentException("Final sonorization not needed for " + production.getWord() + ", remove " + FINAL_SONORIZATION_RULE);
			}
		}



//		String word = production.getWord();
//		List<AffixEntry> appliedRules = production.getAppliedRules();
//		if(true || word.length() > 2 && (Objects.isNull(appliedRules) || appliedRules.size() < 2) && !production.hasProductionRule(AffixEntry.Type.PREFIX)
//				&& !production.hasProductionRule("G0") && !production.hasProductionRule("E0")
//				&& !production.hasProductionRule("G1") && !production.hasProductionRule("E1")
//				&& !word.contains(GraphemeVEC.L_STROKE_GRAPHEME)
//				&& !production.isPartOfSpeech(POS_PROPER_NOUN) && !production.isPartOfSpeech(POS_ARTICLE) && !production.isPartOfSpeech(POS_VERB)
//				&& !production.hasProductionRule(ADJECTIVE_FIRST_CLASS_RULE)&& !production.hasProductionRule(PLURAL_NOUN_MASCULINE_RULE)
//				&& !production.hasProductionRule(ADJECTIVE_THIRD_CLASS_RULE) && !production.hasProductionRule(ADJECTIVE_SECOND_CLASS_RULE)
//				&& !production.hasProductionRule(VARIANT_TRANSFORMATIONS_RULE)){
//			DictionaryEntry entry = new DictionaryEntry(production, FINAL_SONORIZATION_RULE, wordGenerator.getFlagParsingStrategy());
//			List<RuleProductionEntry> productions = Collections.<RuleProductionEntry>emptyList();
//			try{
//				productions = wordGenerator.applyRules(entry);
//			}
//			catch(IllegalArgumentException e){
//				//no productions result from the application of the rule
//			}
//			int numberOfProductions = productions.size();
//
//			boolean hasRule = production.containsRuleFlag(FINAL_SONORIZATION_RULE);
//			if(hasRule && numberOfProductions == 0)
//				throw new IllegalArgumentException("Superfluous rule for " + production.getWord() + ", remove " + FINAL_SONORIZATION_RULE);
//			else if(!hasRule && numberOfProductions > 1)
//				throw new IllegalArgumentException("Missing rule for " + production.getWord() + ", add " + FINAL_SONORIZATION_RULE);
//		}
	}

	private void accentCheck(String subword, RuleProductionEntry production) throws IllegalArgumentException{
		int acents = WordVEC.countAccents(subword);
		if(acents > 1)
			throw new IllegalArgumentException("Word " + production.getWord() + " cannot have multiple accents");

		if(acents == 1 && !subword.equals(WordVEC.unmarkDefaultStress(subword))){
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
		if(!production.isPartOfSpeech(POS_VERB) && !production.isPartOfSpeech(POS_NUMERAL_LATIN) && !production.isPartOfSpeech(POS_UNIT_OF_MEASURE)){
			derivedWord = derivedWord.toLowerCase(Locale.ROOT);
			if(!UNSYLLABABLE_INTERJECTIONS.contains(derivedWord)){
				String correctedDerivedWord = hyphenationParser.correctOrthography(derivedWord);
				if(!correctedDerivedWord.equals(derivedWord))
					throw new IllegalArgumentException("Word " + derivedWord + " is mispelled (should be " + correctedDerivedWord + ")");
			}

			if(Objects.nonNull(hyphenationParser) && derivedWord.length() > 1 && !derivedWord.contains(HyphenationParser.HYPHEN_MINUS)
					&& !production.isPartOfSpeech(POS_NUMERAL_LATIN)
					&& !production.isPartOfSpeech(POS_UNIT_OF_MEASURE)
					&& (!production.isPartOfSpeech(POS_INTERJECTION) || !UNSYLLABABLE_INTERJECTIONS.contains(derivedWord))){
				Hyphenation hyphenation = hyphenationParser.hyphenate(derivedWord);
				if(hyphenation.hasErrors())
					throw new IllegalArgumentException("Word " + String.join(HyphenationParser.HYPHEN, hyphenation.getSyllabes())
						+ " is not syllabable");
			}
		}
	}


	@Override
	public boolean isConsonant(char chr){
		return (WordVEC.CONSONANTS.indexOf(chr) >= 0);
	}

	@Override
	public boolean shouldBeProcessedForMinimalPair(RuleProductionEntry production){
		String word = production.getWord();
		return (word.length() >= MINIMAL_PAIR_MINIMUM_LENGTH
			&& word.indexOf('ƚ') < 0
			&& word.indexOf('ɉ') < 0
			&& (production.containsDataField(WordGenerator.TAG_PART_OF_SPEECH + POS_NOUN)
			|| production.containsDataField(WordGenerator.TAG_PART_OF_SPEECH + POS_ADJECTIVE)
			|| production.containsDataField(WordGenerator.TAG_PART_OF_SPEECH + POS_ADJECTIVE_POSSESSIVE)
			|| production.containsDataField(WordGenerator.TAG_PART_OF_SPEECH + POS_ADJECTIVE_DEMONSTRATIVE)
			|| production.containsDataField(WordGenerator.TAG_PART_OF_SPEECH + POS_ADJECTIVE_IDENTIFICATIVE)
			|| production.containsDataField(WordGenerator.TAG_PART_OF_SPEECH + POS_ADJECTIVE_INTERROGATIVE)
			|| production.containsDataField(WordGenerator.TAG_PART_OF_SPEECH + POS_QUANTIFIER)
			|| production.containsDataField(WordGenerator.TAG_PART_OF_SPEECH + POS_PRONOUN)
			|| production.containsDataField(WordGenerator.TAG_PART_OF_SPEECH + POS_PREPOSITION)
			|| production.containsDataField(WordGenerator.TAG_PART_OF_SPEECH + POS_ADVERB)
			|| production.containsDataField(WordGenerator.TAG_PART_OF_SPEECH + POS_CONJUNCTION)));
	}

	@Override
	public String prepareTextForFilter(String text){
		text = super.prepareTextForFilter(text);

		return orthography.correctOrthography(text);
	}

}
