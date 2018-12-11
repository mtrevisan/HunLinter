package unit731.hunspeller.languages.vec;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import unit731.hunspeller.languages.CorrectnessChecker;
import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.valueobjects.AffixEntry;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import unit731.hunspeller.parsers.hyphenation.dtos.Hyphenation;
import unit731.hunspeller.parsers.hyphenation.hyphenators.AbstractHyphenator;
import unit731.hunspeller.services.PatternHelper;


public class CorrectnessCheckerVEC extends CorrectnessChecker{

	public static final String LANGUAGE = "vec";

	private static final String PLURAL_NOUN_MASCULINE_RULE = "M0";
	private static final String VARIANT_TRANSFORMATIONS_END_RULE_VANISHING_EL = "Te";
	private static final String METAPHONESIS_RULE = "mf";
	private static final String NORTHERN_PLURAL_RULE = "U0";
	private static final String NORTHERN_PLURAL_STRESSED_RULE = "U1";

	private static Pattern PATTERN_NON_VANISHING_EL;
	private static Pattern PATTERN_VANISHING_EL_NEXT_TO_CONSONANT;

	private static final Pattern PATTERN_CIJJHNHIV = PatternHelper.pattern("[ci" + GraphemeVEC.JJH_PHONEME + "ɉñ]j[aeiou]");

//	private static final String START_TAGS = "(?<!\\\\)\\/.*?";
//	private static final String NON_VANISHING_L = "(^[ʼ']?l|[aeiouàèéíòóú]l)[aeiouàèéíòóú][^ƚ]+?" + START_TAGS;

	private static final String SLASH = "/";
	private static final String ASTERISK = "*";
	private static final String OR = " or ";


	private static Pattern PATTERN_NORTHERN_PLURAL;
	private static final String MAN = "man";

	private static final MessageFormat WORD_WITH_VAN_EL_CANNOT_CONTAIN_NON_VAN_EL = new MessageFormat("Word with ƚ cannot contain non–ƚ, {0}");
	private static final MessageFormat WORD_WITH_VAN_EL_CANNOT_CONTAIN_RULE = new MessageFormat("Word with ƚ cannot contain rule " + NORTHERN_PLURAL_RULE + OR + NORTHERN_PLURAL_STRESSED_RULE + ", {0}");
	private static final MessageFormat WORD_WITH_VAN_EL_CANNOT_CONTAIN_DH_OR_TH = new MessageFormat("Word with ƚ cannot contain đ or ŧ, {0}");
	private static final MessageFormat WORD_WITH_VAN_EL_NEAR_CONSONANT = new MessageFormat("Word with ƚ near a consonant, {0}");
	private static final MessageFormat WORD_WITH_RULE_CANNOT_HAVE_RULES_OTHER_THAN = new MessageFormat("Word with rule {0} cannot have otehr rules than {1}");
	private static final MessageFormat METAPHONESIS_NOT_NEEDED_HANDLE = new MessageFormat("Metaphonesis not needed for {0} (missing plural flag), handle {1}");
	private static final MessageFormat METAPHONESIS_MISSING = new MessageFormat("Metaphonesis missing for {0}, add {1}");
	private static final MessageFormat METAPHONESIS_NOT_NEEDED = new MessageFormat("Metaphonesis not needed for {0}, remove {1}");
	private static final MessageFormat NORTHERN_PLURAL_MISSING = new MessageFormat("Northern plural missing for {0}, add {1}");
	private static final MessageFormat NORTHERN_PLURAL_NOT_NEEDED = new MessageFormat("Northern plural not needed for {0}, remove " + NORTHERN_PLURAL_RULE + OR + NORTHERN_PLURAL_STRESSED_RULE);
	private static final MessageFormat WORD_IS_MISSPELLED = new MessageFormat("Word {0} is misspelled, should be {1}");
	private static final MessageFormat WORD_IS_NOT_SYLLABABLE = new MessageFormat("Word {0} ({1}) is not syllabable");
	private static final MessageFormat WORD_HAS_MULTIPLE_ACCENTS = new MessageFormat("Word {0} cannot have multiple accents");
	private static final MessageFormat WORD_HAS_MISSING_ACCENT = new MessageFormat("Word {0} cannot be generated by the rule {1} because of the missing accent");
	private static final MessageFormat WORD_HAS_PRESENT_ACCENT = new MessageFormat("Word {0} cannot be generated by the rule {1} because of the present accent");


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
	private static final String POS_UNIT_OF_MEASURE = "unit_of_measure";

	private static final int MINIMAL_PAIR_MINIMUM_LENGTH = 3;


	private final Orthography orthography = OrthographyVEC.getInstance();

	private final String[] pluralFlags;


	public CorrectnessCheckerVEC(AffixParser affParser, AbstractHyphenator hyphenator) throws IOException{
		super(affParser, hyphenator);

		Objects.requireNonNull(hyphenator);


		Properties rulesProperties = new Properties();
		rulesProperties.load(getClass().getResourceAsStream("rules.properties"));
		loadRules(rulesProperties);

		String pluralFlagsValue = readProperty(rulesProperties, "pluralFlags");
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		pluralFlags = (pluralFlagsValue != null? strategy.parseFlags(pluralFlagsValue): null);

		PATTERN_NON_VANISHING_EL = PatternHelper.pattern(readProperty(rulesProperties, "patternNonVanishingEl"), Pattern.CASE_INSENSITIVE);
		PATTERN_VANISHING_EL_NEXT_TO_CONSONANT = PatternHelper.pattern(readProperty(rulesProperties, "patternVanishingElNextToConsonant"),
			Pattern.CASE_INSENSITIVE);
		PATTERN_NORTHERN_PLURAL = PatternHelper.pattern(readProperty(rulesProperties, "patternNorthernPlural"), Pattern.CASE_INSENSITIVE);
	}

	private boolean hasPluralFlag(Production production){
		return (pluralFlags != null && production.hasContinuationFlag(pluralFlags));
	}

	@Override
	public void checkProduction(Production production) throws IllegalArgumentException{
		super.checkProduction(production);

		try{
			vanishingElCheck(production);

			incompatibilityCheck(production);

//FIXME move this in super?
			if(production.hasNonTerminalContinuationFlags(affParser) && !production.hasPartOfSpeech(POS_VERB)
					&& !production.hasPartOfSpeech(POS_ADVERB)){
				metaphonesisCheck(production);

				northernPluralCheck(production);
			}

			finalSonorizationCheck(production);

			syllabationCheck(production);
		}
		catch(IllegalArgumentException e){
			StringBuilder sb = new StringBuilder(e.getMessage());
			if(production.hasProductionRules())
				sb.append(" (via ").append(production.getRulesSequence()).append(")");
			sb.append(" for ").append(production.getWord());
			throw new IllegalArgumentException(sb.toString());
		}
	}

	private void vanishingElCheck(Production production) throws IllegalArgumentException{
		String derivedWord = production.getWord();
		if(derivedWord.contains(GraphemeVEC.L_STROKE_GRAPHEME)){
			if(PatternHelper.find(derivedWord, PATTERN_NON_VANISHING_EL))
				throw new IllegalArgumentException(WORD_WITH_VAN_EL_CANNOT_CONTAIN_NON_VAN_EL.format(new Object[]{derivedWord}));
			if(production.hasContinuationFlag(NORTHERN_PLURAL_RULE))
				throw new IllegalArgumentException(WORD_WITH_VAN_EL_CANNOT_CONTAIN_RULE.format(new Object[]{derivedWord}));
			if(derivedWord.contains(GraphemeVEC.D_STROKE_GRAPHEME) || derivedWord.contains(GraphemeVEC.T_STROKE_GRAPHEME))
				throw new IllegalArgumentException(WORD_WITH_VAN_EL_CANNOT_CONTAIN_DH_OR_TH.format(new Object[]{derivedWord}));
		}
		if(PatternHelper.find(derivedWord, PATTERN_VANISHING_EL_NEXT_TO_CONSONANT))
			throw new IllegalArgumentException(WORD_WITH_VAN_EL_NEAR_CONSONANT.format(new Object[]{derivedWord}));
	}

	private void incompatibilityCheck(Production production) throws IllegalArgumentException{
		if(production.hasContinuationFlag(VARIANT_TRANSFORMATIONS_END_RULE_VANISHING_EL)
				&& (production.getContinuationFlagCount() != 2 || !production.hasContinuationFlag(PLURAL_NOUN_MASCULINE_RULE)))
			throw new IllegalArgumentException(WORD_WITH_RULE_CANNOT_HAVE_RULES_OTHER_THAN.format(new Object[]{
				VARIANT_TRANSFORMATIONS_END_RULE_VANISHING_EL, PLURAL_NOUN_MASCULINE_RULE}));
	}

	private void metaphonesisCheck(Production production) throws IllegalArgumentException{
		if(!production.hasPartOfSpeech(POS_PROPER_NOUN) && !production.hasPartOfSpeech(POS_ARTICLE)){
			boolean hasMetaphonesisFlag = production.hasContinuationFlag(METAPHONESIS_RULE);
			boolean hasPluralFlag = hasPluralFlag(production);
			if(hasMetaphonesisFlag && !hasPluralFlag)
				throw new IllegalArgumentException(METAPHONESIS_NOT_NEEDED_HANDLE.format(new Object[]{production.getWord(), METAPHONESIS_RULE}));
			else{
				boolean canHaveMetaphonesis = affParser.isAffixProductive(production.getWord(), METAPHONESIS_RULE);
				if(canHaveMetaphonesis ^ hasMetaphonesisFlag){
					if(canHaveMetaphonesis && hasPluralFlag)
						throw new IllegalArgumentException(METAPHONESIS_MISSING.format(new Object[]{production.getWord(), METAPHONESIS_RULE}));
					else if(!canHaveMetaphonesis && !hasPluralFlag)
						throw new IllegalArgumentException(METAPHONESIS_NOT_NEEDED.format(new Object[]{production.getWord(), METAPHONESIS_RULE}));
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
			boolean hasPluralFlag = hasPluralFlag(production);
			boolean canHaveNorthernPlural = (hasPluralFlag && !word.contains(GraphemeVEC.L_STROKE_GRAPHEME) && !word.endsWith(MAN)
				&& affParser.isAffixProductive(word, rule));
			if(canHaveNorthernPlural ^ hasNorthernPluralFlag){
				if(canHaveNorthernPlural)
					throw new IllegalArgumentException(NORTHERN_PLURAL_MISSING.format(new Object[]{word, rule}));
				else if(!canHaveNorthernPlural)
					throw new IllegalArgumentException(NORTHERN_PLURAL_NOT_NEEDED.format(new Object[]{word}));
			}
		}
	}

	private void syllabationCheck(Production production) throws IllegalArgumentException{
		if((enableVerbSyllabationCheck || !production.hasPartOfSpeech(POS_VERB)) && !production.hasPartOfSpeech(POS_NUMERAL_LATIN)
				&& !production.hasPartOfSpeech(POS_UNIT_OF_MEASURE)){
			String word = production.getWord();
			if(!unsyllabableWords.contains(word) && !multipleAccentedWords.contains(word)){
				word = word.toLowerCase(Locale.ROOT);
				String correctedDerivedWord = orthography.correctOrthography(word);
				if(!correctedDerivedWord.equals(word))
					throw new IllegalArgumentException(WORD_IS_MISSPELLED.format(new Object[]{word, correctedDerivedWord}));

				if(word.length() > 1){
					Hyphenation hyphenation = hyphenator.hyphenate(word);
					if(hyphenation.hasErrors())
						throw new IllegalArgumentException(WORD_IS_NOT_SYLLABABLE.format(new Object[]{word,
							hyphenation.formatHyphenation(new StringJoiner(SLASH), syllabe -> ASTERISK + syllabe + ASTERISK)}));
				}
			}
		}
	}

	@Override
	protected void checkCompoundProduction(String subword, Production production) throws IllegalArgumentException{
		accentCheck(subword, production);

//		ciuiCheck(subword, production);
	}

	private void accentCheck(String subword, Production production) throws IllegalArgumentException{
		int accents = WordVEC.countAccents(subword);
		if(!multipleAccentedWords.contains(subword)){
			if(!wordCanHaveMultipleAccents && accents > 1)
				throw new IllegalArgumentException(WORD_HAS_MULTIPLE_ACCENTS.format(new Object[]{production.getWord()}));

			List<AffixEntry> appliedRules = production.getAppliedRules();
			if(appliedRules != null){
				//retrieve last applied rule
				String appliedRuleFlag = appliedRules.get(appliedRules.size() - 1)
					.getFlag();
				if(accents == 0 && hasToContainAccent.contains(appliedRuleFlag))
					throw new IllegalArgumentException(WORD_HAS_MISSING_ACCENT.format(new Object[]{production.getWord(), appliedRuleFlag}));
				else if(accents > 0 && cannotContainAccent.contains(appliedRuleFlag))
					throw new IllegalArgumentException(WORD_HAS_PRESENT_ACCENT.format(new Object[]{production.getWord(), appliedRuleFlag}));
			}
		}
	}

	private void ciuiCheck(String subword, Production production) throws IllegalArgumentException{
		if(!production.hasPartOfSpeech(POS_NUMERAL_LATIN)){
			String phonemizedSubword = GraphemeVEC.handleJHJWIUmlautPhonemes(subword);
			if(PatternHelper.find(phonemizedSubword, PATTERN_CIJJHNHIV))
				throw new IllegalArgumentException("Word " + production.getWord() + " cannot have [cijɉñ]iV");
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
