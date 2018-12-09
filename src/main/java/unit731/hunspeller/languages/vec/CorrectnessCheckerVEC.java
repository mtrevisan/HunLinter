package unit731.hunspeller.languages.vec;

import java.io.IOException;
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
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.languages.CorrectnessChecker;
import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.valueobjects.AffixEntry;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import unit731.hunspeller.parsers.dictionary.dtos.MorphologicalTag;
import unit731.hunspeller.parsers.hyphenation.dtos.Hyphenation;
import unit731.hunspeller.parsers.hyphenation.hyphenators.AbstractHyphenator;
import unit731.hunspeller.services.PatternHelper;


public class CorrectnessCheckerVEC extends CorrectnessChecker{

	private static final char COMMA = ',';

	public static final String LANGUAGE = "vec";

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
	private static final String VARIANT_TRANSFORMATIONS_END_RULE_VANISHING_EL = "Te";
	private static final String VARIANT_TRANSFORMATIONS_FEMININE_RULE = "TF";
	private static final String METAPHONESIS_RULE = "mf";
	private static final String PLANTS_AND_CRAFTS_RULE_NON_VANISHING_EL = "V0";
	private static final String PLANTS_AND_CRAFTS_RULE_VANISHING_EL = "V1";
	private static final String DEVERBAL_NOMINALS_ISTA_RULE = "v1";
	private static final String ETHA_RULE_NON_VANISHING_EL = "q0";
	private static final String ESA_RULE_VANISHING_EL = "q1";
	private static final String ITHIA_RULE_NON_VANISHING_EL = "q2";
	private static final String ISIA_RULE_VANISHING_EL = "q3";
	private static final String DEVERBAL_NOMINALS_MENTO_RULE_NON_VANISHING_EL = "r0";
	private static final String DEVERBAL_NOMINALS_MENTO_RULE_VANISHING_EL = "r1";
	private static final String DEVERBAL_NOMINALS_THION_RULE_NON_VANISHING_EL = "r2";
	private static final String DEVERBAL_NOMINALS_SION_RULE_NON_VANISHING_EL = "r3";
	private static final String DEVERBAL_NOMINALS_SION_RULE_VANISHING_EL = "r4";
	private static final String DEVERBAL_NOMINALS_DURA_RULE_NON_VANISHING_EL = "r5";
	private static final String DEVERBAL_NOMINALS_DURA_RULE_VANISHING_EL = "r6";
	private static final String DEVERBAL_NOMINALS_DOR_RULE_NON_VANISHING_EL = "r7";
	private static final String DEVERBAL_NOMINALS_DOR_RULE_VANISHING_EL = "r8";
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
	private static final String AUGMENTATIVE_ON_A_RULE_NON_VANISHING_EL = "(4";
	private static final String AUGMENTATIVE_ON_A_RULE_VANISHING_EL = "(5";
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

	private static final Pattern NON_VANISHING_EL = PatternHelper.pattern("(^|[aàeèéiíoòóuúAÀEÈÉIÍOÒÓUÚʼ'–-])l([aàeèéiíoòóuúAÀEÈÉIÍOÒÓUÚʼ'–-]|$)");
	private static final Pattern VANISHING_EL_NEAR_CONSONANT = PatternHelper.pattern("[^aàeèéiíoòóuúAÀEÈÉIÍOÒÓUÚʼ'–-]ƚ|ƚ[^aàeèéiíoòóuúAÀEÈÉIÍOÒÓUÚʼ']");

	private static final Pattern L_BETWEEN_VOWELS = PatternHelper.pattern("l i l$");
	private static final Pattern CIJJHNHIV = PatternHelper.pattern("[ci" + GraphemeVEC.JJH_PHONEME + "ɉñ]j[aeiou]");

//	private static final String START_TAGS = "(?<!\\\\)\\/.*?";
//	private static final String NON_VANISHING_L = "(^[ʼ']?l|[aeiouàèéíòóú]l)[aeiouàèéíòóú][^ƚ]+?" + START_TAGS;

	private static final String SLASH = "/";
	private static final String ASTERISK = "*";


	private static final Set<String> CANNOT_CONTAINS_ACCENT = new HashSet<>();
	static{
//		CANNOT_CONTAINS_ACCENT.add(PLANTS_AND_CRAFTS_RULE_NON_VANISHING_EL);
//		CANNOT_CONTAINS_ACCENT.add(PLANTS_AND_CRAFTS_RULE_VANISHING_EL);
		CANNOT_CONTAINS_ACCENT.add(DEVERBAL_NOMINALS_ISTA_RULE);
		CANNOT_CONTAINS_ACCENT.add(DEVERBAL_NOMINALS_MENTO_RULE_NON_VANISHING_EL);
		CANNOT_CONTAINS_ACCENT.add(DEVERBAL_NOMINALS_MENTO_RULE_VANISHING_EL);
		CANNOT_CONTAINS_ACCENT.add(DEVERBAL_NOMINALS_THION_RULE_NON_VANISHING_EL);
		CANNOT_CONTAINS_ACCENT.add(DEVERBAL_NOMINALS_SION_RULE_NON_VANISHING_EL);
		CANNOT_CONTAINS_ACCENT.add(DEVERBAL_NOMINALS_SION_RULE_VANISHING_EL);
		CANNOT_CONTAINS_ACCENT.add(DEVERBAL_NOMINALS_DURA_RULE_NON_VANISHING_EL);
		CANNOT_CONTAINS_ACCENT.add(DEVERBAL_NOMINALS_DURA_RULE_VANISHING_EL);
		CANNOT_CONTAINS_ACCENT.add(DEVERBAL_NOMINALS_DOR_RULE_NON_VANISHING_EL);
		CANNOT_CONTAINS_ACCENT.add(DEVERBAL_NOMINALS_DOR_RULE_VANISHING_EL);
		CANNOT_CONTAINS_ACCENT.add(DEVERBAL_NOMINALS_IXMO_RULE_NON_VANISHING_EL);
		CANNOT_CONTAINS_ACCENT.add(DEVERBAL_NOMINALS_IXMO_RULE_VANISHING_EL);
		CANNOT_CONTAINS_ACCENT.add(ADVERB_MENTE_RULE_NON_VANISHING_EL);
		CANNOT_CONTAINS_ACCENT.add(ADVERB_MENTE_RULE_VANISHING_EL);
//		CANNOT_CONTAINS_ACCENT.add(COLLECTIVE_SUBSTANTIVES_RULE_NON_VANISHING_EL);
//		CANNOT_CONTAINS_ACCENT.add(COLLECTIVE_SUBSTANTIVES_RULE_VANISHING_EL);
		CANNOT_CONTAINS_ACCENT.add(DIMINUTIVE_ETO_RULE_NON_VANISHING_EL);
		CANNOT_CONTAINS_ACCENT.add(DIMINUTIVE_ETO_RULE_VANISHING_EL);
		CANNOT_CONTAINS_ACCENT.add(AUGMENTATIVE_ON_RULE_NON_VANISHING_EL);
		CANNOT_CONTAINS_ACCENT.add(AUGMENTATIVE_ON_RULE_VANISHING_EL);
		CANNOT_CONTAINS_ACCENT.add(AUGMENTATIVE_ON_A_RULE_NON_VANISHING_EL);
		CANNOT_CONTAINS_ACCENT.add(AUGMENTATIVE_ON_A_RULE_VANISHING_EL);
		CANNOT_CONTAINS_ACCENT.add(PEJORATIVE_ATO_RULE_NON_VANISHING_EL);
		CANNOT_CONTAINS_ACCENT.add(PEJORATIVE_ATO_RULE_VANISHING_EL);
		CANNOT_CONTAINS_ACCENT.add(PEJORATIVE_ATHO_RULE_NON_VANISHING_EL);
		CANNOT_CONTAINS_ACCENT.add(PEJORATIVE_ASO_RULE_NON_VANISHING_EL);
		CANNOT_CONTAINS_ACCENT.add(PEJORATIVE_ASO_RULE_VANISHING_EL);
	}

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

		public void match(Production production) throws IllegalArgumentException{
			for(String flag : continuationFlags)
				if(production.hasContinuationFlag(flag))
					throw new IllegalArgumentException(error + " for " + production.getWord());
		}
	}
	private static final Set<MatcherEntry> MISMATCH_CHECKS_MUST_CONTAINS_LH = new HashSet<>();
	private static final Set<MatcherEntry> MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH = new HashSet<>();
	private static final Set<MatcherEntry> MISMATCH_CHECKS_MUST_CONTAINS_DH_OR_TH = new HashSet<>();
	static{
		if(false){
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
		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(AUGMENTATIVE_ON_A_RULE_NON_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, AUGMENTATIVE_ON_A_RULE_NON_VANISHING_EL, AUGMENTATIVE_ON_A_RULE_VANISHING_EL));
		MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(AUGMENTATIVE_ON_A_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, AUGMENTATIVE_ON_A_RULE_VANISHING_EL, AUGMENTATIVE_ON_A_RULE_NON_VANISHING_EL));

		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(ETHA_RULE_NON_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, ETHA_RULE_NON_VANISHING_EL, ESA_RULE_VANISHING_EL));
		MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(ESA_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, ESA_RULE_VANISHING_EL, ETHA_RULE_NON_VANISHING_EL));
		MISMATCH_CHECKS_MUST_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(ITHIA_RULE_NON_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_LH_USE_INSTEAD, ITHIA_RULE_NON_VANISHING_EL, ISIA_RULE_VANISHING_EL));
		MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH.add(new MatcherEntry(Arrays.asList(ISIA_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_NON_LH_USE_INSTEAD, ISIA_RULE_VANISHING_EL, ITHIA_RULE_NON_VANISHING_EL));

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
		MISMATCH_CHECKS_MUST_CONTAINS_DH_OR_TH.add(new MatcherEntry(Arrays.asList(AUGMENTATIVE_ON_A_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_TH_OR_DH_USE_INSTEAD, AUGMENTATIVE_ON_A_RULE_VANISHING_EL, AUGMENTATIVE_ON_A_RULE_NON_VANISHING_EL));
		MISMATCH_CHECKS_MUST_CONTAINS_DH_OR_TH.add(new MatcherEntry(Arrays.asList(PEJORATIVE_ATO_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_TH_OR_DH_USE_INSTEAD, PEJORATIVE_ATO_RULE_VANISHING_EL, PEJORATIVE_ATO_RULE_NON_VANISHING_EL));
		MISMATCH_CHECKS_MUST_CONTAINS_DH_OR_TH.add(new MatcherEntry(Arrays.asList(PEJORATIVE_ASO_RULE_VANISHING_EL),
			MatcherEntry.CANNOT_USE_RULE_WITH_TH_OR_DH_USE_INSTEAD, PEJORATIVE_ASO_RULE_VANISHING_EL, PEJORATIVE_ASO_RULE_NON_VANISHING_EL));
	}

	private static final Pattern PATTERN_NORTHERN_PLURAL = PatternHelper.pattern("[èò][ln]$");
	private static final String MAN = "man";

	private static final Set<MatcherEntry> ADJECTIVE_FIRST_CLASS_MISMATCH_CHECKS = new HashSet<>();
	private static final String WORD_WITH_RULE_CANNOT_HAVE = "Word with rule {0} cannot have rule {1}";
	private static final String WORD_WITH_RULE_CANNOT_HAVE_RULES_OTHER_THAN = "Word with rule {0} cannot have toehr rules than {1}";
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
		VARIANT_TRANSFORMATION_END_MISMATCH_CHECKS.add(new MatcherEntry(Arrays.asList(AUGMENTATIVE_ON_RULE_NON_VANISHING_EL, AUGMENTATIVE_ON_RULE_VANISHING_EL),
			WORD_WITH_RULE_CANNOT_HAVE, VARIANT_TRANSFORMATIONS_END_RULE));
		VARIANT_TRANSFORMATION_END_MISMATCH_CHECKS.add(new MatcherEntry(Arrays.asList(AUGMENTATIVE_ON_A_RULE_NON_VANISHING_EL, AUGMENTATIVE_ON_A_RULE_VANISHING_EL),
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


	private static final String POS_PROPER_NOUN = "proper_noun";
	private static final String POS_NOUN = "noun";
	private static final String POS_ADJECTIVE = "adjective";
	private static final String POS_ADJECTIVE_POSSESSIVE = "adjective_possessive";
	private static final String POS_ADJECTIVE_DEMONSTRATIVE = "adjective_demonstrative";
	private static final String POS_ADJECTIVE_IDENTIFICATIVE = "adjective_identificative";
	private static final String POS_ADJECTIVE_INTERROGATIVE = "adjective_interrogative";
	private static final String POS_QUANTIFIER = "quantifier";
	private static final String POS_VERB = "verb";
	private static final String POS_ARTICLE = "article";
	private static final String POS_PRONOUN = "pronoun";
	private static final String POS_PREPOSITION = "preposition";
	private static final String POS_ADVERB = "adverb";
	private static final String POS_CONJUNCTION = "conjunction";
	private static final String POS_NUMERAL_LATIN = "numeral_latin";
	private static final String POS_INTERJECTION = "interjection";
	private static final String POS_UNIT_OF_MEASURE = "unit_of_measure";

	private static final int MINIMAL_PAIR_MINIMUM_LENGTH = 3;


	private final Properties rules = new Properties();
	private boolean enableVerbCheck;
	private Set<String> unsyllabableWords;
	private Set<String> multipleAccentedWords;
	private final Map<String, Set<String>> dataFields = new HashMap<>();

	private final Orthography orthography = OrthographyVEC.getInstance();


	public CorrectnessCheckerVEC(AffixParser affParser, AbstractHyphenator hyphenator) throws IOException{
		super(affParser, hyphenator);

		Objects.requireNonNull(hyphenator);


		loadRules();
	}

	private void loadRules() throws IOException{
		rules.load(CorrectnessCheckerVEC.class.getResourceAsStream("rules.properties"));

		enableVerbCheck = Boolean.getBoolean((String)rules.get("verbCheck"));

		dataFields.put(MorphologicalTag.TAG_PART_OF_SPEECH, readPropertyAsSet("partOfSpeeches", COMMA));
		dataFields.put(MorphologicalTag.TAG_INFLECTIONAL_SUFFIX, readPropertyAsSet("inflectionalSuffixes", COMMA));
		dataFields.put(MorphologicalTag.TAG_TERMINAL_SUFFIX, readPropertyAsSet("terminalSuffixes", COMMA));
		dataFields.put(MorphologicalTag.TAG_STEM, null);
		dataFields.put(MorphologicalTag.TAG_ALLOMORPH, null);

		unsyllabableWords = readPropertyAsSet("unsyllabableInterjections", COMMA);
		multipleAccentedWords = readPropertyAsSet("multipleAccentedInterjections", COMMA);

//		Iterator<String> mustNotAdmits = readPropertyAsList("mustNotAdmit", SLASH_CHAR)
//			.iterator();
//		while(mustNotAdmits.hasNext()){
//			String masterRule = mustNotAdmits.next();
//			String[] otherRules = strategy.parseFlags(mustNotAdmits.next());
//			mustNotAdmit.computeIfAbsent(masterRule, k -> new HashSet<>())
//				.add(new MatcherEntry(WORD_WITH_RULE_CANNOT_HAVE, masterRule, otherRules));
//		}
//
//		Iterator<String> mustAdmits = readPropertyAsList("mustAdmit", SLASH_CHAR)
//			.iterator();
//		while(mustAdmits.hasNext()){
//			String masterRule = mustAdmits.next();
//			boolean converse = (masterRule.charAt(0) == '^');
//			if(converse)
//				masterRule = masterRule.substring(1);
//			String[] wrongRightRules = strategy.parseFlags(mustAdmits.next());
//			mustAdmit.computeIfAbsent(masterRule, k -> new HashSet<>())
//				.add(new MismatcherEntry(CANNOT_USE_RULE_WITH_USE_INSTEAD, masterRule, wrongRightRules[0], wrongRightRules[1]));
//			if(converse)
//				converseMustAdmit.computeIfAbsent(masterRule, k -> new HashSet<>())
//					.add(new MismatcherEntry(CANNOT_USE_RULE_WITH_NON_USE_INSTEAD, masterRule, wrongRightRules[1], wrongRightRules[0]));
//		}
	}

	private Set<String> readPropertyAsSet(String key, char separator){
		List<String> properties = readPropertyAsList(key, separator);
		return (!properties.isEmpty()? new HashSet<>(properties): Collections.<String>emptySet());
	}

	private List<String> readPropertyAsList(String key, char separator){
		String line = rules.getProperty(key, StringUtils.EMPTY);
		return (StringUtils.isNotEmpty(line)? Arrays.asList(StringUtils.split(line, separator)): Collections.<String>emptyList());
	}

	@Override
	public void checkProduction(Production production) throws IllegalArgumentException{
//		if(!ENABLE_VERB_CHECK && production.isPartOfSpeech(POS_VERB))
//			return;

		try{
			if(!production.hasMorphologicalFields())
				throw new IllegalArgumentException("Line does not contains any morphological fields");

			String forbidCompoundFlag = affParser.getForbidCompoundFlag();
			if(!production.hasProductionRules() && production.hasContinuationFlag(forbidCompoundFlag))
				throw new IllegalArgumentException("Non-affix entry contains COMPOUNDFORBIDFLAG");

			morphologicalFieldCheck(production);

			vanishingElCheck(production);

			incompatibilityCheck(production);

//FIXME move this in super?
			String derivedWordWithoutMorphologicalFields = production.toString();
			if(production.hasNonTerminalContinuationFlags(affParser) && !production.hasPartOfSpeech(POS_VERB) && !production.hasPartOfSpeech(POS_ADVERB)){
				metaphonesisCheck(production, derivedWordWithoutMorphologicalFields);

				northernPluralCheck(production);
			}

			mismatchCheck(production);

			finalSonorizationCheck(production);

			List<String> splittedWords = hyphenator.splitIntoCompounds(production.getWord());
			for(String subword : splittedWords){
				accentCheck(subword, production);

//				ciuiCheck(subword, production);
			}

			syllabationCheck(production);
		}
		catch(IllegalArgumentException e){
			String message = e.getMessage();
			String rulesSequence = production.getRulesSequence();
			if(rulesSequence.length() > 0)
				message += " (via " + rulesSequence + ")";
			throw new IllegalArgumentException(message);
		}
	}

	private void morphologicalFieldCheck(Production production) throws IllegalArgumentException{
		production.forEachMorphologicalField(morphologicalField -> {
			if(morphologicalField.length() < 4)
				throw new IllegalArgumentException("Word " + production.getWord() + " has an invalid morphological field prefix: " + morphologicalField);

			String morphologicalFieldPrefix = morphologicalField.substring(0, 3);
			if(!dataFields.containsKey(morphologicalFieldPrefix))
				throw new IllegalArgumentException("Word " + production.getWord() + " has an unknown morphological field prefix: " + morphologicalField);

			Set<String> morphologicalFieldTypes = dataFields.get(morphologicalFieldPrefix);
			if(morphologicalFieldTypes != null && !morphologicalFieldTypes.contains(morphologicalField.substring(3)))
				throw new IllegalArgumentException("Word " + production.getWord() + " has an unknown morphological field value: " + morphologicalField);
		});
	}

	private void vanishingElCheck(Production production) throws IllegalArgumentException{
		String derivedWord = production.getWord();
		if(derivedWord.contains(GraphemeVEC.L_STROKE_GRAPHEME) && PatternHelper.find(derivedWord, NON_VANISHING_EL))
			throw new IllegalArgumentException("Word with ƚ cannot contain non–ƚ, " + derivedWord);
		if(StringUtils.contains(production.getWord(), GraphemeVEC.L_STROKE_GRAPHEME)
				&& (StringUtils.contains(production.getWord(), GraphemeVEC.D_STROKE_GRAPHEME) || StringUtils.contains(production.getWord(), GraphemeVEC.T_STROKE_GRAPHEME)))
			throw new IllegalArgumentException("Word with ƚ cannot contain đ or ŧ, " + derivedWord);
		if(PatternHelper.find(derivedWord, VANISHING_EL_NEAR_CONSONANT))
			throw new IllegalArgumentException("Word with ƚ near a consonant, " + derivedWord);
		if(derivedWord.contains(GraphemeVEC.L_STROKE_GRAPHEME) && production.hasContinuationFlag(NORTHERN_PLURAL_RULE))
			throw new IllegalArgumentException("Word with ƚ cannot contain rule " + NORTHERN_PLURAL_RULE + " or "
				+ NORTHERN_PLURAL_STRESSED_RULE + ", " + derivedWord);
	}

	private void incompatibilityCheck(Production production) throws IllegalArgumentException{
		variantIncompatibilityCheck(production, MISMATCH_CHECKS_MUST_CONTAINS_LH, GraphemeVEC.L_STROKE_GRAPHEME);
		variantIncompatibilityCheck(production, MISMATCH_CHECKS_MUST_CONTAINS_DH_OR_TH, GraphemeVEC.D_STROKE_GRAPHEME, GraphemeVEC.T_STROKE_GRAPHEME);

		continuationFlagIncompatibilityCheck(production, ADJECTIVE_FIRST_CLASS_RULE, ADJECTIVE_FIRST_CLASS_MISMATCH_CHECKS);
		continuationFlagIncompatibilityCheck(production, VARIANT_TRANSFORMATIONS_END_RULE, VARIANT_TRANSFORMATION_END_MISMATCH_CHECKS);
		if(production.hasContinuationFlag(VARIANT_TRANSFORMATIONS_END_RULE_VANISHING_EL)
				&& (production.getContinuationFlagCount() != 2 || !production.hasContinuationFlag(PLURAL_NOUN_MASCULINE_RULE)))
			throw new IllegalArgumentException(MessageFormat.format(WORD_WITH_RULE_CANNOT_HAVE_RULES_OTHER_THAN, VARIANT_TRANSFORMATIONS_END_RULE_VANISHING_EL, PLURAL_NOUN_MASCULINE_RULE) + " for " + production.getWord());
		continuationFlagIncompatibilityCheck(production, VARIANT_TRANSFORMATIONS_FEMININE_RULE, VARIANT_TRANSFORMATION_FEMININE_MISMATCH_CHECKS);
		continuationFlagIncompatibilityCheck(production, GUA_TO_VA_RULE, GUA_TO_VA_CHECKS);
	}

	private void continuationFlagIncompatibilityCheck(Production production, String continuationFlag, Set<MatcherEntry> checks)
			throws IllegalArgumentException{
		if(production.hasContinuationFlag(continuationFlag))
			for(MatcherEntry entry : checks)
				entry.match(production);
	}

	private void metaphonesisCheck(Production production, String line) throws IllegalArgumentException{
		if(!production.hasPartOfSpeech(POS_PROPER_NOUN) && !production.hasPartOfSpeech(POS_ARTICLE)){
			boolean hasMetaphonesisFlag = production.hasContinuationFlag(METAPHONESIS_RULE);
			boolean hasPluralFlag = production.hasContinuationFlag(PLURAL_NOUN_MASCULINE_RULE, ADJECTIVE_FIRST_CLASS_RULE, ADJECTIVE_SECOND_CLASS_RULE,
				ADJECTIVE_THIRD_CLASS_RULE);
			if(hasMetaphonesisFlag && !hasPluralFlag)
				throw new IllegalArgumentException("Metaphonesis not needed for " + production.getWord() + " (missing plural flag), handle " + METAPHONESIS_RULE);
			else{
				boolean canHaveMetaphonesis = affParser.isAffixProductive(production.getWord(), METAPHONESIS_RULE);
				if(canHaveMetaphonesis ^ hasMetaphonesisFlag){
					if(canHaveMetaphonesis && hasPluralFlag)
						throw new IllegalArgumentException("Metaphonesis missing for " + production.getWord() + ", add " + METAPHONESIS_RULE);
					else if(!canHaveMetaphonesis && !hasPluralFlag)
						throw new IllegalArgumentException("Metaphonesis not needed for " + production.getWord() + ", remove " + METAPHONESIS_RULE);
				}
			}
		}
	}

	private void northernPluralCheck(Production production) throws IllegalArgumentException{
		String word = production.getWord();
		if(!production.hasPartOfSpeech(POS_ARTICLE) && !production.hasPartOfSpeech(POS_PRONOUN) && !production.hasPartOfSpeech(POS_PROPER_NOUN)
				&& hyphenator.hyphenate(word).countSyllabes() > 1){
			List<String> subwords = hyphenator.splitIntoCompounds(word);
			String rule = (!WordVEC.hasStressedGrapheme(subwords.get(subwords.size() - 1)) || PatternHelper.find(word, PATTERN_NORTHERN_PLURAL)?
				NORTHERN_PLURAL_RULE: NORTHERN_PLURAL_STRESSED_RULE);
			boolean hasNorthernPluralFlag = production.hasContinuationFlag(rule);
			boolean canHaveNorthernPlural = (production.hasContinuationFlag(PLURAL_NOUN_MASCULINE_RULE, ADJECTIVE_FIRST_CLASS_RULE, ADJECTIVE_SECOND_CLASS_RULE, ADJECTIVE_THIRD_CLASS_RULE)
				&& !word.contains(GraphemeVEC.L_STROKE_GRAPHEME) && !word.endsWith(MAN) && affParser.isAffixProductive(word, rule));
			if(canHaveNorthernPlural ^ hasNorthernPluralFlag){
				if(canHaveNorthernPlural)
					throw new IllegalArgumentException("Northern plural missing for " + word + ", add " + rule);
				else if(!canHaveNorthernPlural)
					throw new IllegalArgumentException("Northern plural not needed for " + word + ", remove " + NORTHERN_PLURAL_RULE
						+ " or " + NORTHERN_PLURAL_STRESSED_RULE);
			}
		}
	}

	private void mismatchCheck(Production production) throws IllegalArgumentException{
		variantIncompatibilityCheck(production, MISMATCH_CHECKS_MUST_NOT_CONTAINS_LH);
	}

	private void variantIncompatibilityCheck(Production production, Set<MatcherEntry> checks, String... contains)
			throws IllegalArgumentException{
		if(StringUtils.containsAny(production.getWord(), contains))
			for(MatcherEntry entry : checks)
				entry.match(production);
	}

	private void accentCheck(String subword, Production production) throws IllegalArgumentException{
		int accents = WordVEC.countAccents(subword);
		if(!multipleAccentedWords.contains(subword)){
			if(accents > 1)
				throw new IllegalArgumentException("Word " + production.getWord() + " cannot have multiple accents");

			List<AffixEntry> appliedRules = production.getAppliedRules();
			if(appliedRules != null && WordVEC.countAccents(subword) > 0){
				String appliedRuleFlag = appliedRules.get(appliedRules.size() - 1)
					.getFlag();
				if(CANNOT_CONTAINS_ACCENT.contains(appliedRuleFlag))
					throw new IllegalArgumentException("Word " + production.getWord() + " cannot be generated by the rule " + appliedRuleFlag + " because of the accent");
			}

			//if word contains accent and appliedRule is one of those commented
			if(accents == 1 && !subword.equals(WordVEC.unmarkDefaultStress(subword))){
				boolean elBetweenVowelsRemoval = false;
				if(appliedRules != null)
					for(AffixEntry appliedRule : appliedRules)
						if(PatternHelper.find(appliedRule.toString(), L_BETWEEN_VOWELS)){
							elBetweenVowelsRemoval = true;
							break;
						}
				if(!elBetweenVowelsRemoval)
					throw new IllegalArgumentException("Word " + production.getWord() + " cannot have an accent there");
			}
		}
	}

	private void ciuiCheck(String subword, Production production) throws IllegalArgumentException{
		if(!production.hasPartOfSpeech(POS_NUMERAL_LATIN)){
			String phonemizedSubword = GraphemeVEC.handleJHJWIUmlautPhonemes(subword);
			if(PatternHelper.find(phonemizedSubword, CIJJHNHIV))
				throw new IllegalArgumentException("Word " + production.getWord() + " cannot have [cijɉñ]iV");
		}
	}

	private void syllabationCheck(Production production) throws IllegalArgumentException{
		if((enableVerbCheck || !production.hasPartOfSpeech(POS_VERB)) && !production.hasPartOfSpeech(POS_NUMERAL_LATIN) && !production.hasPartOfSpeech(POS_UNIT_OF_MEASURE)){
			String word = production.getWord();
			if(!unsyllabableWords.contains(word) && !multipleAccentedWords.contains(word)){
				word = word.toLowerCase(Locale.ROOT);
				String correctedDerivedWord = orthography.correctOrthography(word);
				if(!correctedDerivedWord.equals(word))
					throw new IllegalArgumentException("Word " + word + " is mispelled, should be " + correctedDerivedWord);

				if(word.length() > 1){
					Hyphenation hyphenation = hyphenator.hyphenate(word);
					if(hyphenation.hasErrors())
						throw new IllegalArgumentException("Word " + word + " (" + hyphenation.formatHyphenation(new StringJoiner(SLASH), syllabe -> ASTERISK + syllabe + ASTERISK)
							+ ") is not syllabable");
				}
			}
		}
	}

//	private void variantIncompatibilityCheck(RuleProductionEntry production, Set<MatcherEntry> checks)
//			throws IllegalArgumentException{
//		if(canContainsVanishingEl(production.getWord()))
//			for(MatcherEntry entry : checks)
//				entry.match(production);
//	}
//
//	//(^[ʼ']?l|[aeiouàèéíòóú]l)[aeiouàèéíòóú]
//	private static boolean canContainsVanishingEl(String word){
//		boolean result = false;
//		int size = word.length();
//		if(size > 1){
//			int index = (WordVEC.isApostrophe(word.charAt(0))? 1: 0);
//			if(index + 1 < size){
//				char chr = word.charAt(index);
//				result = (chr == 'l' && WordVEC.isVowel(word.charAt(index + 1)));
//				while(!result){
//					index = WordVEC.getFirstVowelIndex(word, index);
//					if(index < 0 || index + 2 >= size)
//						break;
//
//					if(word.charAt(index + 1) == 'l' && WordVEC.isVowel(word.charAt(index + 2)))
//						result = true;
//
//					index ++;
//				}
//			}
//		}
//		return result;
//	}

	private void finalSonorizationCheck(Production production) throws IllegalArgumentException{
//		if(!production.hasProductionRules()&& !production.isPartOfSpeech(POS_VERB) && !production.isPartOfSpeech(POS_PROPER_NOUN)){
//			boolean hasFinalSonorizationFlag = production.hasContinuationFlag(FINAL_SONORIZATION_RULE);
//			boolean canHaveFinalSonorization = (!production.getWord().toLowerCase(Locale.ROOT).contains(GraphemeVEC.L_STROKE_GRAPHEME) && wordGenerator.isAffixProductive(production.getWord(), FINAL_SONORIZATION_RULE));
//			if(canHaveFinalSonorization ^ hasFinalSonorizationFlag){
//				if(canHaveFinalSonorization)
//					throw new IllegalArgumentException("Final sonorization missing for " + production.getWord() + ", add " + FINAL_SONORIZATION_RULE);
//				else if(!canHaveFinalSonorization)
//					throw new IllegalArgumentException("Final sonorization not needed for " + production.getWord() + ", remove " + FINAL_SONORIZATION_RULE);
//			}
//		}
	}


	@Override
	public boolean isConsonant(char chr){
		return WordVEC.isConsonant(chr);
	}

	@Override
	public boolean shouldBeProcessedForMinimalPair(Production production){
		String word = production.getWord();
		return (word.length() >= MINIMAL_PAIR_MINIMUM_LENGTH
			&& word.indexOf('ƚ') < 0
			&& word.indexOf('ɉ') < 0
			&& (production.hasPartOfSpeech(POS_NOUN)
			|| production.hasPartOfSpeech(POS_ADJECTIVE)
			|| production.hasPartOfSpeech(POS_ADJECTIVE_POSSESSIVE)
			|| production.hasPartOfSpeech(POS_ADJECTIVE_DEMONSTRATIVE)
			|| production.hasPartOfSpeech(POS_ADJECTIVE_IDENTIFICATIVE)
			|| production.hasPartOfSpeech(POS_ADJECTIVE_INTERROGATIVE)
			|| production.hasPartOfSpeech(POS_QUANTIFIER)
			|| production.hasPartOfSpeech(POS_PRONOUN)
			|| production.hasPartOfSpeech(POS_PREPOSITION)
			|| production.hasPartOfSpeech(POS_ADVERB)
			|| production.hasPartOfSpeech(POS_CONJUNCTION)));
	}

}
