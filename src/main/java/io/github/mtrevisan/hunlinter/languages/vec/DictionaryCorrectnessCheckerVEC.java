/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.languages.vec;

import io.github.mtrevisan.hunlinter.languages.DictionaryCorrectnessChecker;
import io.github.mtrevisan.hunlinter.languages.Orthography;
import io.github.mtrevisan.hunlinter.languages.RulesLoader;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.enums.MorphologicalTag;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.HyphenationParser;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.HyphenatorInterface;
import io.github.mtrevisan.hunlinter.parsers.vos.AffixEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.services.RegexHelper;
import io.github.mtrevisan.hunlinter.services.eventbus.EventBusService;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterWarning;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;


public class DictionaryCorrectnessCheckerVEC extends DictionaryCorrectnessChecker{

	public static final String LANGUAGE = "vec";

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

	private static final String WORD_SEPARATORS = HyphenationParser.MINUS_SIGN + HyphenationParser.EN_DASH;

	private static final int MINIMAL_PAIR_MINIMUM_LENGTH = 3;

	private Pattern patternNonVanishingEl;
	private Pattern patternNonVanishingElNotAtEnd;
	private Pattern patternVanishingElNextToConsonant;
	private Pattern patternPhonemeCijjhnhiv;
	private static final Pattern PATTERN_V_IU_V = RegexHelper.pattern("[aeiou][iu][aeiou]", Pattern.CASE_INSENSITIVE);
	private static final Pattern PATTERN_NOT_V_IU_DIERESIS_V = RegexHelper.pattern("[aeiou][ïü][^aeiou]|[^aeiou][ïü]", Pattern.CASE_INSENSITIVE);
	private String northernPluralRule;
	private String northernPluralStressedRule;
	private Set<String> dontCheckProductivenessRules;
	private Set<String> derivationCanAdminStress;
	private Set<String> stemCanAdmitStress;

	private static final String SINGLE_POS_NOT_PRESENT = "Part-of-Speech not unique";
	private static final String UNNECESSARY_STRESS = "{} have unnecessary stress";
	private static final String WORD_WITH_VAN_EL_CANNOT_CONTAIN_NON_VAN_EL = "Word with ƚ cannot contain non-ƚ, {}";
	private static final String WORD_WITH_VAN_EL_CANNOT_CONTAIN_RULE = "Word with ƚ cannot contain rule {} or {}, {}";
	private static final String WORD_WITH_VAN_EL_NEAR_CONSONANT = "Word with ƚ near a consonant, {}";
	private static final String WORD_WITH_MIXED_VARIANTS = "Word with mixed variants, {}";
	private static final String MISSPELLED = "{} is misspelled, should be {}";
	private static final String MULTIPLE_STRESSES = "{} cannot have multiple stresses";
	private static final String MISSING_STRESS = "{} cannot be generated by the rule {} because of the missing stress";
	private static final String ALREADY_PRESENT_STRESS = "{} cannot be generated by the rule {} because of the already present stress";
	private static final String WORD_CANNOT_HAVE_CIJJHNHIV = "{} cannot have [cijɉñ]iV";
	private static final String WORD_CANNOT_HAVE_V_IU_V = "{} cannot have [aeiou][iu][aeiou], use ï or ü instead";
	private static final String WORD_CANNOT_HAVE_NOT_V_IU_DIERESIS_V = "{} cannot have [^aeiou][ïü][^aeiou], use i or u instead";


	private final Orthography orthography;


	public DictionaryCorrectnessCheckerVEC(final AffixData affixData, final HyphenatorInterface hyphenator){
		super(affixData, hyphenator);

		Objects.requireNonNull(hyphenator, "Hyphenator cannot be null");

		orthography = OrthographyVEC.getInstance();
	}

	/**
	 * Loads the rules required for dictionary correctness checking.
	 */
	@Override
	public final void loadRules(){
		rulesLoader = new RulesLoader(affixData.getLanguage(), affixData.getFlagParsingStrategy());

		patternNonVanishingEl = RegexHelper.pattern(rulesLoader.readProperty("patternNonVanishingEl"),
			Pattern.CASE_INSENSITIVE);
		patternNonVanishingElNotAtEnd = RegexHelper.pattern(rulesLoader.readProperty("patternNonVanishingElNotAtEnd"),
			Pattern.CASE_INSENSITIVE);
		patternVanishingElNextToConsonant = RegexHelper.pattern(rulesLoader.readProperty("patternVanishingElNextToConsonant"),
			Pattern.CASE_INSENSITIVE);
		patternPhonemeCijjhnhiv = RegexHelper.pattern(rulesLoader.readProperty("patternPhonemeCIJJHNHIV"),
			Pattern.CASE_INSENSITIVE);

		northernPluralRule = rulesLoader.readProperty("northernPlural");
		northernPluralStressedRule = rulesLoader.readProperty("northernPluralStressed");

		dontCheckProductivenessRules = rulesLoader.readPropertyAsSet("dontCheckProductiveness", ',');
		derivationCanAdminStress = rulesLoader.readPropertyAsSet("derivationCanAdmitStress", ',');
		final Set<String> canAdmitStress = rulesLoader.readPropertyAsSet("stemCanAdmitStress", ',');
		stemCanAdmitStress = new HashSet<>(canAdmitStress.size());
		for(final String cas : canAdmitStress)
			stemCanAdmitStress.add(MorphologicalTag.STEM.getCode() + cas);
	}

	/**
	 * Checks the inflection for correctness, performing various checks and validations.
	 *
	 * @param inflection	The inflection to be checked.
	 * @param index	The line number of the inflection in the dictionary.
	 */
	@Override
	public final void checkInflection(final Inflection inflection, final int index){
		super.checkInflection(inflection, index);

		if(!rulesLoader.containsHasToContainStressDerivationalSuffix(inflection.getMorphologicalFields(MorphologicalTag.DERIVATIONAL_SUFFIX)))
			stressCheck(inflection);

		variantsCheck(inflection);

		incompatibilityCheck(inflection, index);

		orthographyCheck(inflection);
	}

	/**
	 * Perform stress checks on an inflection.
	 *
	 * @param inflection	The inflection to be checked.
	 */
	private void stressCheck(final Inflection inflection){
		final boolean canAdmitStress = canAdmitStress(inflection.getMorphologicalFieldStem());
		final String derivedWord = inflection.getWord().toLowerCase(Locale.ROOT);
		final String[] subwords = StringUtils.split(derivedWord, WORD_SEPARATORS);
		for(int i = 0; i < subwords.length; i ++){
			final String subword = subwords[i];
			stressCheck(subword, inflection, (i == subwords.length - 1));

			final String markedDefaultStressWord = WordVEC.markDefaultStress(subword);
			if(!subword.equals(markedDefaultStressWord) && !canAdmitStress && !canAdmitStress(inflection))
				throw new LinterException(UNNECESSARY_STRESS, inflection.getWord());
		}
	}

	/**
	 * Checks the given inflection for language variants and throws exceptions if any inconsistencies are found.
	 *
	 * @param inflection	The inflection to be checked.
	 */
	private void variantsCheck(final Inflection inflection){
		String derivedWord = inflection.getWord().toLowerCase(Locale.ROOT);
		for(int i = 0; i < WORD_SEPARATORS.length(); i ++)
			derivedWord = StringUtils.remove(derivedWord, WORD_SEPARATORS.charAt(i));

		final Collection<LanguageVariant> variants = EnumSet.noneOf(LanguageVariant.class);
		if(derivedWord.contains(GraphemeVEC.GRAPHEME_L_STROKE)){
			if(RegexHelper.find(derivedWord, patternNonVanishingEl))
				throw new LinterException(WORD_WITH_VAN_EL_CANNOT_CONTAIN_NON_VAN_EL, inflection.getWord());
			if(inflection.hasContinuationFlag(northernPluralRule))
				throw new LinterException(WORD_WITH_VAN_EL_CANNOT_CONTAIN_RULE, northernPluralRule, northernPluralStressedRule, inflection.getWord());
			if(RegexHelper.find(derivedWord, patternVanishingElNextToConsonant))
				throw new LinterException(WORD_WITH_VAN_EL_NEAR_CONSONANT, inflection.getWord());

			variants.add(LanguageVariant.VENETIAN);
		}
		if(RegexHelper.find(derivedWord, patternNonVanishingElNotAtEnd)
				|| derivedWord.contains(GraphemeVEC.GRAPHEME_D_STROKE) || derivedWord.contains(GraphemeVEC.GRAPHEME_T_STROKE))
			variants.add(LanguageVariant.NORTHERN);

		if(variants.size() > 1)
			throw new LinterException(WORD_WITH_MIXED_VARIANTS, inflection.getWord());
	}

	/**
	 * Performs an incompatibility check on the given inflection.
	 * <p>
	 * If the inflection has more than one part of speech, it publishes a linter warning event with the specified index.
	 * </p>
	 *
	 * @param inflection	The inflection to be checked.
	 * @param index	The line number of the inflection in the dictionary.
	 */
	private static void incompatibilityCheck(final Inflection inflection, final int index){
		final List<String> pos = inflection.getMorphologicalFieldPartOfSpeech();
		if(pos.size() > 1)
			EventBusService.publish(new LinterWarning(SINGLE_POS_NOT_PRESENT)
				.withIndex(index));
	}

	/**
	 * Checks the orthography of the given inflection by performing various checks and validations.
	 *
	 * @param inflection	The inflection to be checked.
	 */
	private void orthographyCheck(final Inflection inflection){
		if(hasToCheckForOrthographyAndSyllabation(inflection)){
			final String word = inflection.getWord()
				.toLowerCase(Locale.ROOT);
			if(!rulesLoader.containsUnsyllabableWords(word) && !rulesLoader.containsValidStressedWords(word))
				orthographyCheck(word);
		}
	}

	/**
	 * Checks whether the given inflection needs to be checked for orthography and syllabation.
	 *
	 * @param inflection	The inflection to be checked.
	 * @return	Whether the inflection needs to be checked for orthography and syllabation.
	 */
	private boolean hasToCheckForOrthographyAndSyllabation(final Inflection inflection){
		return ((rulesLoader.isEnableVerbSyllabationCheck() || !inflection.hasPartOfSpeech(POS_VERB))
			&& !inflection.hasPartOfSpeech(POS_NUMERAL_LATIN) && !inflection.hasPartOfSpeech(POS_UNIT_OF_MEASURE));
	}

	/**
	 * Performs an orthography check on the given word.
	 * <p>
	 * If the word is misspelled, a {@link LinterException} is thrown with the misspelled word and the corrected word.
	 * </p>
	 *
	 * @param word	The word to be checked for orthography.
	 * @throws LinterException	If the word is misspelled.
	 */
	private void orthographyCheck(final String word){
		final String correctedDerivedWord = orthography.correctOrthography(word);
		if(!correctedDerivedWord.equals(word))
			throw new LinterException(MISSPELLED, word, correctedDerivedWord);
	}

	/**
	 * Checks the compound inflection for correctness, performing various checks and validations.
	 *
	 * @param subword	The subword being checked.
	 * @param subwordIndex	The line number of the subword in the dictionary.
	 * @param inflection	The inflection to be checked.
	 */
	@Override
	protected final void checkCompoundInflection(final String subword, final int subwordIndex, final Inflection inflection){
		if(subwordIndex == 0)
			stressCheck(subword, inflection, false);

		ciuiCheck(subword, inflection);
	}

	/**
	 *
	 * Performs stress checks on a given subword in an inflection.
	 *
	 * @param subword	The subword to be checked.
	 * @param inflection	The inflection containing the subword.
	 * @param lastSubword	Flag indicating if the subword is the last in the inflection.
	 */
	private void stressCheck(final String subword, final Inflection inflection, final boolean lastSubword){
		if(!rulesLoader.containsValidStressedWords(subword)){
			final int stresses = WordVEC.countStresses(subword);
			if(!rulesLoader.isWordCanHaveMultipleStresses() && stresses > 1)
				throw new LinterException(MULTIPLE_STRESSES, inflection.getWord());

			final AffixEntry appliedRule = getLastAppliedRule(inflection);
			if(appliedRule != null){
				final String appliedRuleFlag = appliedRule.getFlag();
				//retrieve last applied rule
				if(stresses == 0 && (rulesLoader.containsHasToContainStress(appliedRuleFlag)
						|| rulesLoader.containsHasToContainStressDerivationalSuffix(appliedRule.getMorphologicalFields(
							MorphologicalTag.DERIVATIONAL_SUFFIX))))
					throw new LinterException(MISSING_STRESS, inflection.getWord(), appliedRuleFlag);
				if(lastSubword && rulesLoader.containsCannotContainStress(appliedRuleFlag) && stresses > 0
						&& WordVEC.countStresses(appliedRule.getAppending()) == 0)
					throw new LinterException(ALREADY_PRESENT_STRESS, inflection.getWord(), appliedRuleFlag);
			}
		}
	}

	/**
	 * Retrieves the last applied rule from the given {@link Inflection} object.
	 *
	 * @param inflection	The Inflection object from which to retrieve the last applied rule.
	 * @return	The last applied rule as an {@link AffixEntry} object, or <code>null</code> if no rules have been applied.
	 */
	private static AffixEntry getLastAppliedRule(final Inflection inflection){
		AffixEntry appliedRuleFlag = null;
		final AffixEntry[] appliedRules = inflection.getAppliedRules();
		if(appliedRules.length > 0)
			appliedRuleFlag = appliedRules[appliedRules.length - 1];
		return appliedRuleFlag;
	}

	/**
	 * Checks the given subword of an inflection for specific patterns and throws exceptions if any inconsistencies are found.
	 *
	 * @param subword	The subword to be checked.
	 * @param inflection	The inflection containing the subword.
	 * @throws LinterException	If the subword matches certain patterns.
	 */
	private void ciuiCheck(final String subword, final Inflection inflection){
		if(!inflection.hasPartOfSpeech(POS_NUMERAL_LATIN) && RegexHelper.find(subword, patternPhonemeCijjhnhiv))
			throw new LinterException(WORD_CANNOT_HAVE_CIJJHNHIV, inflection.getWord());

		if(RegexHelper.find(subword, PATTERN_V_IU_V))
			throw new LinterException(WORD_CANNOT_HAVE_V_IU_V, inflection.getWord());
		if(RegexHelper.find(subword, PATTERN_NOT_V_IU_DIERESIS_V))
			throw new LinterException(WORD_CANNOT_HAVE_NOT_V_IU_DIERESIS_V, inflection.getWord());
	}

//	private void variantIncompatibilityCheck(final RuleInflectionEntry inflection, final Set<MatcherEntry> checks){
//		if(canContainVanishingEl(inflection.getWord()))
//			for(final MatcherEntry entry : checks)
//				entry.match(inflection);
//	}
//
//	//(^[‘’']?l|[aeiouàèéíòóú]l)[aeiouàèéíòóú]
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


	/**
	 * Checks if a given flag should not be used to check productiveness.
	 *
	 * @param flag	The flag to check.
	 * @return	Whether the flag should not be used for productiveness check.
	 */
	@Override
	public final boolean shouldNotCheckProductiveness(final String flag){
		return dontCheckProductivenessRules.contains(flag);
	}

	/**
	 * Determines whether the given inflection can admit stress.
	 *
	 * @param inflection	The inflection to be checked.
	 * @return	Whether the inflection can admit stress.
	 */
	@Override
	public final boolean canAdmitStress(final Inflection inflection){
		String derivationalField = null;
		final List<String> morphologicalFields = inflection.getMorphologicalFieldsAsList();
		for(int i = 0; derivationalField == null && i < morphologicalFields.size(); i ++)
			if(morphologicalFields.get(i).startsWith(MorphologicalTag.DERIVATIONAL_SUFFIX.getCode()))
				derivationalField = morphologicalFields.get(i);
		return !derivationCanAdminStress.contains(derivationalField);
	}

	/**
	 * Determines whether the given stem can admit stress.
	 *
	 * @param stem	The stem to be checked.
	 * @return	Whether the stem can admit stress.
	 */
	@Override
	public final boolean canAdmitStress(final String stem){
		//NOTE: starting from index 3 removes the prefix `po:`
		return stemCanAdmitStress.contains(stem);
	}

	/**
	 * Determines whether a character is a consonant.
	 *
	 * @param chr	The character to be checked.
	 * @return	Whether the character is a consonant.
	 */
	@Override
	public final boolean isConsonant(final char chr){
		return WordVEC.isConsonant(chr);
	}

	/**
	 * Determines whether the given inflection should be processed for minimal pair.
	 *
	 * @param inflection	The inflection to be checked.
	 * @return	Whether the inflection meets the conditions to be processed for minimal pair.
	 */
	@Override
	public final boolean shouldBeProcessedForMinimalPair(final Inflection inflection){
		final String word = inflection.getWord();
		return (word.length() >= MINIMAL_PAIR_MINIMUM_LENGTH
			&& word.indexOf('ƚ') < 0
			&& word.indexOf('ɉ') < 0
			&& (inflection.hasPartOfSpeech(POS_NOUN)
			|| inflection.hasPartOfSpeech(POS_ADJECTIVE)
			|| inflection.hasPartOfSpeech(POS_ADJECTIVE_POSSESSIVE)
			|| inflection.hasPartOfSpeech(POS_ADJECTIVE_DEMONSTRATIVE)
			|| inflection.hasPartOfSpeech(POS_ADJECTIVE_IDENTIFICATIVE)
			|| inflection.hasPartOfSpeech(POS_ADJECTIVE_INTERROGATIVE)
			|| inflection.hasPartOfSpeech(POS_QUANTIFIER)
			|| inflection.hasPartOfSpeech(POS_PRONOUN)
			|| inflection.hasPartOfSpeech(POS_PREPOSITION)
			|| inflection.hasPartOfSpeech(POS_ADVERB)
			|| inflection.hasPartOfSpeech(POS_CONJUNCTION)));
	}

}
