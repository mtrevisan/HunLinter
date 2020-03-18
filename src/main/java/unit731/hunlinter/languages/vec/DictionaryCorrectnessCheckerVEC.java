package unit731.hunlinter.languages.vec;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.languages.DictionaryCorrectnessChecker;
import unit731.hunlinter.languages.Orthography;
import unit731.hunlinter.languages.RulesLoader;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.enums.MorphologicalTag;
import unit731.hunlinter.parsers.hyphenation.HyphenationParser;
import unit731.hunlinter.parsers.vos.AffixEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.parsers.hyphenation.HyphenatorInterface;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.PatternHelper;


public class DictionaryCorrectnessCheckerVEC extends DictionaryCorrectnessChecker{

	public static final String LANGUAGE = "vec-IT";

	private static final String POS_PROPER_NOUN = MorphologicalTag.PART_OF_SPEECH.attachValue("proper_noun");
	private static final String POS_NOUN = MorphologicalTag.PART_OF_SPEECH.attachValue("noun");
	private static final String POS_ADJECTIVE = MorphologicalTag.PART_OF_SPEECH.attachValue("adjective");
	private static final String POS_ADJECTIVE_POSSESSIVE = MorphologicalTag.PART_OF_SPEECH.attachValue("adjective_possessive");
	private static final String POS_ADJECTIVE_DEMONSTRATIVE = MorphologicalTag.PART_OF_SPEECH.attachValue("adjective_demonstrative");
	private static final String POS_ADJECTIVE_IDENTIFICATIVE = MorphologicalTag.PART_OF_SPEECH.attachValue("adjective_identificative");
	private static final String POS_ADJECTIVE_INTERROGATIVE = MorphologicalTag.PART_OF_SPEECH.attachValue("adjective_interrogative");
	private static final String POS_QUANTIFIER = MorphologicalTag.PART_OF_SPEECH.attachValue("quantifier");
	private static final String POS_VERB = MorphologicalTag.PART_OF_SPEECH.attachValue("verb");
	private static final String POS_ARTICLE = MorphologicalTag.PART_OF_SPEECH.attachValue("article");
	private static final String POS_PRONOUN = MorphologicalTag.PART_OF_SPEECH.attachValue("pronoun");
	private static final String POS_PREPOSITION = MorphologicalTag.PART_OF_SPEECH.attachValue("preposition");
	private static final String POS_ADVERB = MorphologicalTag.PART_OF_SPEECH.attachValue("adverb");
	private static final String POS_CONJUNCTION = MorphologicalTag.PART_OF_SPEECH.attachValue("conjunction");
	private static final String POS_NUMERAL_LATIN = MorphologicalTag.PART_OF_SPEECH.attachValue("numeral_latin");
	private static final String POS_UNIT_OF_MEASURE = MorphologicalTag.PART_OF_SPEECH.attachValue("unit_of_measure");

	private static final int MINIMAL_PAIR_MINIMUM_LENGTH = 3;

	private static Pattern PATTERN_NON_VANISHING_EL;
	private static Pattern PATTERN_NON_VANISHING_EL_NOT_AT_END;
	private static Pattern PATTERN_VANISHING_EL_NEXT_TO_CONSONANT;
	private static Pattern PATTERN_PHONEME_CIJJHNHIV;
	private static final Pattern PATTERN_V_IU_V = PatternHelper.pattern("[aeiou][iu][aeiou]", Pattern.CASE_INSENSITIVE);
	private static final Pattern PATTERN_NOT_V_IU_DIERESIS_V = PatternHelper.pattern("[aeiou][ïü][^aeiou]|[^aeiou][ïü][aeiou]|[^aeiou][ïü][^aeiou]", Pattern.CASE_INSENSITIVE);
	private static Pattern PATTERN_NORTHERN_PLURAL;
	private static String PLURAL_NOUN_MASCULINE_RULE;
	private static String VARIANT_TRANSFORMATIONS_END_RULE_VANISHING_EL;
	private static String NORTHERN_PLURAL_RULE;
	private static String NORTHERN_PLURAL_STRESSED_RULE;
	private static String NORTHERN_PLURAL_EXCEPTION;

	private static final String SINGLE_POS_NOT_PRESENT = "Part-of-Speech not unique";
	private static final MessageFormat UNNECESSARY_STRESS = new MessageFormat("Word {0} have unnecessary stress");
	private static final MessageFormat WORD_WITH_VAN_EL_CANNOT_CONTAIN_NON_VAN_EL = new MessageFormat("Word with ƚ cannot contain non–ƚ, {0}");
	private static final MessageFormat WORD_WITH_VAN_EL_CANNOT_CONTAIN_RULE = new MessageFormat("Word with ƚ cannot contain rule {0} or {1}, {2}");
	private static final MessageFormat WORD_WITH_VAN_EL_NEAR_CONSONANT = new MessageFormat("Word with ƚ near a consonant, {0}");
	private static final MessageFormat WORD_WITH_MIXED_VARIANTS = new MessageFormat("Word with mixed variants, {0}");
	private static final MessageFormat WORD_WITH_RULE_CANNOT_HAVE_RULES_OTHER_THAN = new MessageFormat("Word with rule {0} cannot have other rules than {1}");
	private static final MessageFormat NORTHERN_PLURAL_MISSING = new MessageFormat("Northern plural missing, add {0}");
	private static final MessageFormat NORTHERN_PLURAL_NOT_NEEDED = new MessageFormat("Northern plural not needed, remove {0} or {1}");
	private static final MessageFormat MISSPELLED = new MessageFormat("{0} is misspelled, should be {1}");
	private static final MessageFormat MULTIPLE_ACCENTS = new MessageFormat("{0} cannot have multiple accents");
	private static final MessageFormat MISSING_ACCENT = new MessageFormat("{0} cannot be generated by the rule {1} because of the missing accent");
	private static final MessageFormat ALREADY_PRESENT_ACCENT = new MessageFormat("{0} cannot be generated by the rule {1} because of the already present accent");
	private static final MessageFormat WORD_CANNOT_HAVE_CIJJHNHIV = new MessageFormat("{0} cannot have [cijɉñ]iV");
	private static final MessageFormat WORD_CANNOT_HAVE_V_IU_V = new MessageFormat("{0} cannot have [aeiou][iu][aeiou], use ï or ü instead");
	private static final MessageFormat WORD_CANNOT_HAVE_NOT_V_IU_DIERESIS_V = new MessageFormat("{0} cannot have [^aeiou][ïü][^aeiou], use i or u instead");


	private final Orthography orthography;

	private String[] pluralFlags;
	private String finalSonorizationFlag;


	public DictionaryCorrectnessCheckerVEC(final AffixData affixData, final HyphenatorInterface hyphenator){
		super(affixData, hyphenator);

		Objects.requireNonNull(hyphenator);

		orthography = OrthographyVEC.getInstance();
	}

	@Override
	public void loadRules(){
		rulesLoader = new RulesLoader(affixData.getLanguage(), affixData.getFlagParsingStrategy());

		final String pluralFlagsValue = rulesLoader.readProperty("pluralFlags");
		final FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();
		pluralFlags = (pluralFlagsValue != null? strategy.parseFlags(pluralFlagsValue): null);
		finalSonorizationFlag = rulesLoader.readProperty("finalSonorizationFlag");

		PATTERN_NON_VANISHING_EL = PatternHelper.pattern(rulesLoader.readProperty("patternNonVanishingEl"),
			Pattern.CASE_INSENSITIVE);
		PATTERN_NON_VANISHING_EL_NOT_AT_END = PatternHelper.pattern(rulesLoader.readProperty("patternNonVanishingElNotAtEnd"),
			Pattern.CASE_INSENSITIVE);
		PATTERN_VANISHING_EL_NEXT_TO_CONSONANT = PatternHelper.pattern(rulesLoader.readProperty("patternVanishingElNextToConsonant"),
			Pattern.CASE_INSENSITIVE);
		PATTERN_PHONEME_CIJJHNHIV = PatternHelper.pattern(rulesLoader.readProperty("patternPhonemeCIJJHNHIV"),
			Pattern.CASE_INSENSITIVE);
		PATTERN_NORTHERN_PLURAL = PatternHelper.pattern(rulesLoader.readProperty("patternNorthernPlural"),
			Pattern.CASE_INSENSITIVE);

		PLURAL_NOUN_MASCULINE_RULE = rulesLoader.readProperty("masculinePluralNoun");
		VARIANT_TRANSFORMATIONS_END_RULE_VANISHING_EL = rulesLoader.readProperty("variantTransformationAtEndVanishingEl");
		NORTHERN_PLURAL_RULE = rulesLoader.readProperty("northernPlural");
		NORTHERN_PLURAL_STRESSED_RULE = rulesLoader.readProperty("northernPluralStressed");
		NORTHERN_PLURAL_EXCEPTION = rulesLoader.readProperty("northernPluralException");
	}

	private boolean hasPluralFlag(final Production production){
		return (pluralFlags != null && production.hasContinuationFlags(pluralFlags));
	}

	@Override
	public void checkProduction(final Production production){
		super.checkProduction(production);

		stressCheck(production);

		variantsCheck(production);

		incompatibilityCheck(production);

		if(production.hasNonTerminalContinuationFlags(affixData::isTerminalAffix)
				&& !production.hasPartOfSpeech(POS_VERB) && !production.hasPartOfSpeech(POS_ADVERB))
			northernPluralCheck(production);

		finalSonorizationCheck(production);

		orthographyCheck(production);
	}

	private void stressCheck(final Production production){
		final String derivedWord = production.getWord();
		final String unmarkedDefaultStressWord = WordVEC.unmarkDefaultStress(derivedWord);
		if(!derivedWord.equals(unmarkedDefaultStressWord))
			throw new LinterException(UNNECESSARY_STRESS.format(new Object[]{derivedWord}));
	}

	private void variantsCheck(final Production production){
		final String derivedWord = production.getWord();
		final String[] subwords = StringUtils.split(derivedWord.toLowerCase(Locale.ROOT), HyphenationParser.EN_DASH);
		final Set<LanguageVariant> variants = new HashSet<>();
		for(final String subword : subwords){
			if(subword.contains(GraphemeVEC.GRAPHEME_L_STROKE)){
				if(PatternHelper.find(subword, PATTERN_NON_VANISHING_EL))
					throw new LinterException(WORD_WITH_VAN_EL_CANNOT_CONTAIN_NON_VAN_EL.format(new Object[]{derivedWord}));
				if(production.hasContinuationFlag(NORTHERN_PLURAL_RULE))
					throw new LinterException(WORD_WITH_VAN_EL_CANNOT_CONTAIN_RULE.format(new Object[]{NORTHERN_PLURAL_RULE,
						NORTHERN_PLURAL_STRESSED_RULE, subword}));
				if(PatternHelper.find(subword, PATTERN_VANISHING_EL_NEXT_TO_CONSONANT))
					throw new LinterException(WORD_WITH_VAN_EL_NEAR_CONSONANT.format(new Object[]{derivedWord}));

				variants.add(LanguageVariant.VENETIAN);
			}
			else if(PatternHelper.find(subword, PATTERN_NON_VANISHING_EL_NOT_AT_END)
					|| subword.contains(GraphemeVEC.GRAPHEME_D_STROKE) || subword.contains(GraphemeVEC.GRAPHEME_T_STROKE))
				variants.add(LanguageVariant.NORTHERN);
		}
		if(variants.contains(LanguageVariant.VENETIAN) && variants.contains(LanguageVariant.NORTHERN))
			throw new LinterException(WORD_WITH_MIXED_VARIANTS.format(new Object[]{derivedWord}));
	}

	private void incompatibilityCheck(final Production production){
		if(production.hasContinuationFlag(VARIANT_TRANSFORMATIONS_END_RULE_VANISHING_EL)
				&& (production.getContinuationFlagCount() != 2 || !production.hasContinuationFlag(PLURAL_NOUN_MASCULINE_RULE)))
			throw new LinterException(WORD_WITH_RULE_CANNOT_HAVE_RULES_OTHER_THAN.format(new Object[]{
				VARIANT_TRANSFORMATIONS_END_RULE_VANISHING_EL, PLURAL_NOUN_MASCULINE_RULE}));

		final String[] pos = production.getMorphologicalFieldPartOfSpeech();
		if(pos.length > 1)
			throw new LinterException(SINGLE_POS_NOT_PRESENT);
	}

	private void northernPluralCheck(final Production production){
		if(hasToCheckForNorthernPlural(production)){
			final String word = production.getWord();
			final String rule = getRuleToCheckNorthernPlural(word);
			final boolean canHaveNorthernPlural = canHaveNorthernPlural(production, rule);
			final boolean hasNorthernPluralFlag = production.hasContinuationFlag(rule);
			if(canHaveNorthernPlural && !hasNorthernPluralFlag)
				throw new LinterException(NORTHERN_PLURAL_MISSING.format(new Object[]{rule}));
			if(!canHaveNorthernPlural && hasNorthernPluralFlag)
				throw new LinterException(NORTHERN_PLURAL_NOT_NEEDED.format(new Object[]{NORTHERN_PLURAL_RULE,
					NORTHERN_PLURAL_STRESSED_RULE}));
		}
	}

	private boolean hasToCheckForNorthernPlural(final Production production){
		return (!production.hasPartOfSpeech(POS_ARTICLE) && !production.hasPartOfSpeech(POS_PRONOUN)
			&& !production.hasPartOfSpeech(POS_PROPER_NOUN) && !production.hasPartOfSpeech(POS_UNIT_OF_MEASURE)
			&& hyphenator.hyphenate(production.getWord()).countSyllabes() > 1);
	}

	private String getRuleToCheckNorthernPlural(final String word){
		final List<String> subwords = hyphenator.splitIntoCompounds(word);
		return (!WordVEC.hasStressedGrapheme(subwords.get(subwords.size() - 1))
			|| PatternHelper.find(word, PATTERN_NORTHERN_PLURAL)?
			NORTHERN_PLURAL_RULE: NORTHERN_PLURAL_STRESSED_RULE);
	}

	private boolean canHaveNorthernPlural(final Production production, final String rule){
		final String word = production.getWord();
		final boolean hasPluralFlag = hasPluralFlag(production);
		return (hasPluralFlag && !word.contains(GraphemeVEC.GRAPHEME_L_STROKE)
			&& !word.endsWith(NORTHERN_PLURAL_EXCEPTION) && affixData.isAffixProductive(rule, word));
	}

	private void orthographyCheck(final Production production){
		if(hasToCheckForOrthographyAndSyllabation(production)){
			String word = production.getWord();
			if(!rulesLoader.containsUnsyllabableWords(word) && !rulesLoader.containsMultipleAccentedWords(word)){
				word = word.toLowerCase(Locale.ROOT);
				orthographyCheck(word);
			}
		}
	}

	private boolean hasToCheckForOrthographyAndSyllabation(final Production production){
		return ((rulesLoader.isEnableVerbSyllabationCheck() || !production.hasPartOfSpeech(POS_VERB))
			&& !production.hasPartOfSpeech(POS_NUMERAL_LATIN) && !production.hasPartOfSpeech(POS_UNIT_OF_MEASURE));
	}

	private void orthographyCheck(final String word){
		final String correctedDerivedWord = orthography.correctOrthography(word);
		if(!correctedDerivedWord.equals(word))
			throw new LinterException(MISSPELLED.format(new Object[]{word, correctedDerivedWord}));
	}

	@Override
	protected void checkCompoundProduction(final String subword, final int subwordIndex, final Production production){
		if(subwordIndex == 0)
			accentCheck(subword, production);

		ciuiCheck(subword, production);
	}

	private void accentCheck(final String subword, final Production production){
		if(!rulesLoader.containsMultipleAccentedWords(subword)){
			final int accents = WordVEC.countAccents(subword);
			if(!rulesLoader.isWordCanHaveMultipleAccents() && accents > 1)
				throw new LinterException(MULTIPLE_ACCENTS.format(new Object[]{production.getWord()}));

			final String appliedRuleFlag = getLastAppliedRule(production);
			if(appliedRuleFlag != null){
				//retrieve last applied rule
				if(accents == 0 && rulesLoader.containsHasToContainAccent(appliedRuleFlag))
					throw new LinterException(MISSING_ACCENT.format(new Object[]{production.getWord(),
						appliedRuleFlag}));
				if(accents > 0 && rulesLoader.containsCannotContainAccent(appliedRuleFlag))
					throw new LinterException(ALREADY_PRESENT_ACCENT.format(new Object[]{production.getWord(),
						appliedRuleFlag}));
			}
		}
	}

	private String getLastAppliedRule(final Production production){
		String appliedRuleFlag = null;
		final AffixEntry[] appliedRules = production.getAppliedRules();
		if(appliedRules != null)
			appliedRuleFlag = appliedRules[appliedRules.length - 1]
				.getFlag();
		return appliedRuleFlag;
	}

	private void ciuiCheck(final String subword, final Production production){
		if(!production.hasPartOfSpeech(POS_NUMERAL_LATIN)){
			final String phonemizedSubword = GraphemeVEC.handleJHJWIUmlautPhonemes(subword);
			if(PatternHelper.find(phonemizedSubword, PATTERN_PHONEME_CIJJHNHIV))
				throw new LinterException(WORD_CANNOT_HAVE_CIJJHNHIV.format(new Object[]{production.getWord()}));
		}

		if(PatternHelper.find(subword, PATTERN_V_IU_V))
			throw new LinterException(WORD_CANNOT_HAVE_V_IU_V.format(new Object[]{production.getWord()}));
		if(PatternHelper.find(subword, PATTERN_NOT_V_IU_DIERESIS_V))
			throw new LinterException(WORD_CANNOT_HAVE_NOT_V_IU_DIERESIS_V.format(new Object[]{production.getWord()}));
	}

//	private void variantIncompatibilityCheck(final RuleProductionEntry production, final Set<MatcherEntry> checks){
//		if(canContainVanishingEl(production.getWord()))
//			for(final MatcherEntry entry : checks)
//				entry.match(production);
//	}
//
//	//(^[ʼ']?l|[aeiouàèéíòóú]l)[aeiouàèéíòóú]
//	private static boolean canContainVanishingEl(final String word){
//		boolean result = false;
//		final int size = word.length();
//		if(size > 1){
//			int index = (WordVEC.isApostrophe(word.charAt(0))? 1: 0);
//			if(index + 1 < size){
//				final char chr = word.charAt(index);
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

	private void finalSonorizationCheck(final Production production){
		//FIXME
//		if(!production.hasProductionRules() && !production.getWord().contains(HyphenationParser.EN_DASH)){
//			final boolean hasFinalSonorizationFlag = production.hasContinuationFlag(finalSonorizationFlag);
//			final boolean canHaveFinalSonorization = (!production.getWord().toLowerCase(Locale.ROOT).contains(GraphemeVEC.GRAPHEME_L_STROKE)
//				&& affixData.isAffixProductive(production.getWord(), finalSonorizationFlag));
//			if(canHaveFinalSonorization ^ hasFinalSonorizationFlag){
//				if(canHaveFinalSonorization)
//					throw new HunLintException("Final sonorization missing for " + production.getWord() + ", add " + finalSonorizationFlag);
//				if(!canHaveFinalSonorization)
//					throw new HunLintException("Final sonorization not needed for " + production.getWord() + ", remove " + finalSonorizationFlag);
//			}
//		}
	}


	@Override
	public boolean isConsonant(final char chr){
		return WordVEC.isConsonant(chr);
	}

	@Override
	public boolean shouldBeProcessedForMinimalPair(final Production production){
		final String word = production.getWord();
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
