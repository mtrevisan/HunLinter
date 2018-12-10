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
import unit731.hunspeller.parsers.dictionary.valueobjects.AffixEntry;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import unit731.hunspeller.parsers.hyphenation.dtos.Hyphenation;
import unit731.hunspeller.parsers.hyphenation.hyphenators.AbstractHyphenator;
import unit731.hunspeller.services.PatternHelper;


public class CorrectnessCheckerVEC extends CorrectnessChecker{

	public static final String LANGUAGE = "vec";

	private static final String ADJECTIVE_FIRST_CLASS_RULE = "A1";
	private static final String ADJECTIVE_SECOND_CLASS_RULE = "A2";
	private static final String ADJECTIVE_THIRD_CLASS_RULE = "F0";
	private static final String PLURAL_NOUN_MASCULINE_RULE = "M0";
	private static final String VARIANT_TRANSFORMATIONS_END_RULE_VANISHING_EL = "Te";
	private static final String METAPHONESIS_RULE = "mf";
	private static final String NORTHERN_PLURAL_RULE = "U0";
	private static final String NORTHERN_PLURAL_STRESSED_RULE = "U1";

	private static final Pattern NON_VANISHING_EL = PatternHelper.pattern("(^|[aàeèéiíoòóuúAÀEÈÉIÍOÒÓUÚʼ'–-])l([aàeèéiíoòóuúAÀEÈÉIÍOÒÓUÚʼ'–-]|$)");
	private static final Pattern VANISHING_EL_NEAR_CONSONANT = PatternHelper.pattern("[^aàeèéiíoòóuúAÀEÈÉIÍOÒÓUÚʼ'–-]ƚ|ƚ[^aàeèéiíoòóuúAÀEÈÉIÍOÒÓUÚʼ']");

	private static final Pattern CIJJHNHIV = PatternHelper.pattern("[ci" + GraphemeVEC.JJH_PHONEME + "ɉñ]j[aeiou]");

//	private static final String START_TAGS = "(?<!\\\\)\\/.*?";
//	private static final String NON_VANISHING_L = "(^[ʼ']?l|[aeiouàèéíòóú]l)[aeiouàèéíòóú][^ƚ]+?" + START_TAGS;

	private static final String SLASH = "/";
	private static final String ASTERISK = "*";


	private static final Pattern PATTERN_NORTHERN_PLURAL = PatternHelper.pattern("[èò][ln]$");
	private static final String MAN = "man";

	private static final String WORD_WITH_RULE_CANNOT_HAVE_RULES_OTHER_THAN = "Word with rule {0} cannot have otehr rules than {1}";


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


	public CorrectnessCheckerVEC(AffixParser affParser, AbstractHyphenator hyphenator) throws IOException{
		super(affParser, hyphenator);

		Objects.requireNonNull(hyphenator);


		Properties rulesProperties = new Properties();
		rulesProperties.load(getClass().getResourceAsStream("rules.properties"));
		loadRules(rulesProperties);
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
			String message = e.getMessage();
			if(production.hasProductionRules())
				message += " (via " + production.getRulesSequence() + ")";
			throw new IllegalArgumentException(message);
		}
	}

	private void vanishingElCheck(Production production) throws IllegalArgumentException{
		String derivedWord = production.getWord();
		if(derivedWord.contains(GraphemeVEC.L_STROKE_GRAPHEME)){
			if(PatternHelper.find(derivedWord, NON_VANISHING_EL))
				throw new IllegalArgumentException("Word with ƚ cannot contain non–ƚ, " + derivedWord);
			if(production.hasContinuationFlag(NORTHERN_PLURAL_RULE))
				throw new IllegalArgumentException("Word with ƚ cannot contain rule " + NORTHERN_PLURAL_RULE + " or "
					+ NORTHERN_PLURAL_STRESSED_RULE + ", " + derivedWord);
			if(derivedWord.contains(GraphemeVEC.D_STROKE_GRAPHEME) || derivedWord.contains(GraphemeVEC.T_STROKE_GRAPHEME))
				throw new IllegalArgumentException("Word with ƚ cannot contain đ or ŧ, " + derivedWord);
		}
		if(PatternHelper.find(derivedWord, VANISHING_EL_NEAR_CONSONANT))
			throw new IllegalArgumentException("Word with ƚ near a consonant, " + derivedWord);
	}

	private void incompatibilityCheck(Production production) throws IllegalArgumentException{
		if(production.hasContinuationFlag(VARIANT_TRANSFORMATIONS_END_RULE_VANISHING_EL)
				&& (production.getContinuationFlagCount() != 2 || !production.hasContinuationFlag(PLURAL_NOUN_MASCULINE_RULE)))
			throw new IllegalArgumentException(MessageFormat.format(WORD_WITH_RULE_CANNOT_HAVE_RULES_OTHER_THAN,
				VARIANT_TRANSFORMATIONS_END_RULE_VANISHING_EL, PLURAL_NOUN_MASCULINE_RULE) + " for " + production.getWord());
	}

	private void metaphonesisCheck(Production production) throws IllegalArgumentException{
		if(!production.hasPartOfSpeech(POS_PROPER_NOUN) && !production.hasPartOfSpeech(POS_ARTICLE)){
			boolean hasMetaphonesisFlag = production.hasContinuationFlag(METAPHONESIS_RULE);
			boolean hasPluralFlag = production.hasContinuationFlag(PLURAL_NOUN_MASCULINE_RULE, ADJECTIVE_FIRST_CLASS_RULE,
				ADJECTIVE_SECOND_CLASS_RULE, ADJECTIVE_THIRD_CLASS_RULE);
			if(hasMetaphonesisFlag && !hasPluralFlag)
				throw new IllegalArgumentException("Metaphonesis not needed for " + production.getWord() + " (missing plural flag), handle "
					+ METAPHONESIS_RULE);
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
			boolean canHaveNorthernPlural = (production.hasContinuationFlag(PLURAL_NOUN_MASCULINE_RULE, ADJECTIVE_FIRST_CLASS_RULE,
				ADJECTIVE_SECOND_CLASS_RULE, ADJECTIVE_THIRD_CLASS_RULE)
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

	private void syllabationCheck(Production production) throws IllegalArgumentException{
		if((enableVerbSyllabationCheck || !production.hasPartOfSpeech(POS_VERB)) && !production.hasPartOfSpeech(POS_NUMERAL_LATIN)
				&& !production.hasPartOfSpeech(POS_UNIT_OF_MEASURE)){
			String word = production.getWord();
			if(!unsyllabableWords.contains(word) && !multipleAccentedWords.contains(word)){
				word = word.toLowerCase(Locale.ROOT);
				String correctedDerivedWord = orthography.correctOrthography(word);
				if(!correctedDerivedWord.equals(word))
					throw new IllegalArgumentException("Word " + word + " is mispelled, should be " + correctedDerivedWord);

				if(word.length() > 1){
					Hyphenation hyphenation = hyphenator.hyphenate(word);
					if(hyphenation.hasErrors())
						throw new IllegalArgumentException("Word " + word + " (" + hyphenation.formatHyphenation(new StringJoiner(SLASH),
							syllabe -> ASTERISK + syllabe + ASTERISK) + ") is not syllabable");
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
			if(PatternHelper.find(phonemizedSubword, CIJJHNHIV))
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
