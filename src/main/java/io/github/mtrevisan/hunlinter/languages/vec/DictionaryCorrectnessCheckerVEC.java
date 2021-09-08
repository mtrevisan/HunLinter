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
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import io.github.mtrevisan.hunlinter.workers.core.IndexDataPair;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterWarning;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;


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

	private static final String SINGLE_POS_NOT_PRESENT = "Part-of-Speech not unique";
	private static final ThreadLocal<MessageFormat> UNNECESSARY_STRESS = JavaHelper.createMessageFormat("{0} have unnecessary stress");
	private static final ThreadLocal<MessageFormat> WORD_WITH_VAN_EL_CANNOT_CONTAIN_NON_VAN_EL = JavaHelper.createMessageFormat("Word with ƚ cannot contain non-ƚ, {0}");
	private static final ThreadLocal<MessageFormat> WORD_WITH_VAN_EL_CANNOT_CONTAIN_RULE = JavaHelper.createMessageFormat("Word with ƚ cannot contain rule {0} or {1}, {2}");
	private static final ThreadLocal<MessageFormat> WORD_WITH_VAN_EL_NEAR_CONSONANT = JavaHelper.createMessageFormat("Word with ƚ near a consonant, {0}");
	private static final ThreadLocal<MessageFormat> WORD_WITH_MIXED_VARIANTS = JavaHelper.createMessageFormat("Word with mixed variants, {0}");
	private static final ThreadLocal<MessageFormat> MISSPELLED = JavaHelper.createMessageFormat("{0} is misspelled, should be {1}");
	private static final ThreadLocal<MessageFormat> MULTIPLE_STRESSES = JavaHelper.createMessageFormat("{0} cannot have multiple stresses");
	private static final ThreadLocal<MessageFormat> MISSING_STRESS = JavaHelper.createMessageFormat("{0} cannot be generated by the rule {1} because of the missing stress");
	private static final ThreadLocal<MessageFormat> ALREADY_PRESENT_STRESS = JavaHelper.createMessageFormat("{0} cannot be generated by the rule {1} because of the already present stress");
	private static final ThreadLocal<MessageFormat> WORD_CANNOT_HAVE_CIJJHNHIV = JavaHelper.createMessageFormat("{0} cannot have [cijɉñ]iV");
	private static final ThreadLocal<MessageFormat> WORD_CANNOT_HAVE_V_IU_V = JavaHelper.createMessageFormat("{0} cannot have [aeiou][iu][aeiou], use ï or ü instead");
	private static final ThreadLocal<MessageFormat> WORD_CANNOT_HAVE_NOT_V_IU_DIERESIS_V = JavaHelper.createMessageFormat("{0} cannot have [^aeiou][ïü][^aeiou], use i or u instead");


	private final Orthography orthography;


	public DictionaryCorrectnessCheckerVEC(final AffixData affixData, final HyphenatorInterface hyphenator){
		super(affixData, hyphenator);

		Objects.requireNonNull(hyphenator, "Hyphenator cannot be null");

		orthography = OrthographyVEC.getInstance();
	}

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
	}

	@Override
	public final void checkInflection(final Inflection inflection, final int index){
		super.checkInflection(inflection, index);

		stressCheck(inflection);

		variantsCheck(inflection);

		incompatibilityCheck(inflection, index);

		orthographyCheck(inflection);
	}

	private void stressCheck(final Inflection inflection){
		final String derivedWord = inflection.getWord().toLowerCase(Locale.ROOT);
		final String[] subwords = StringUtils.split(derivedWord, WORD_SEPARATORS);
		for(final String subword : subwords){
			stressCheck(subword, inflection);

			final String unmarkedDefaultStressWord = WordVEC.unmarkDefaultStress(subword);
			if(!subword.equals(unmarkedDefaultStressWord))
				throw new LinterException(UNNECESSARY_STRESS.get().format(new Object[]{inflection.getWord()}));
		}
	}

	private void variantsCheck(final Inflection inflection){
		String derivedWord = inflection.getWord().toLowerCase(Locale.ROOT);
		for(int i = 0; i < WORD_SEPARATORS.length(); i ++)
			derivedWord = StringUtils.remove(derivedWord, WORD_SEPARATORS.charAt(i));

		final Collection<LanguageVariant> variants = EnumSet.noneOf(LanguageVariant.class);
		if(derivedWord.contains(GraphemeVEC.GRAPHEME_L_STROKE)){
			if(RegexHelper.find(derivedWord, patternNonVanishingEl))
				throw new LinterException(WORD_WITH_VAN_EL_CANNOT_CONTAIN_NON_VAN_EL.get().format(new Object[]{derivedWord}));
			if(inflection.hasContinuationFlag(northernPluralRule))
				throw new LinterException(WORD_WITH_VAN_EL_CANNOT_CONTAIN_RULE.get().format(new Object[]{northernPluralRule,
					northernPluralStressedRule, derivedWord}));
			if(RegexHelper.find(derivedWord, patternVanishingElNextToConsonant))
				throw new LinterException(WORD_WITH_VAN_EL_NEAR_CONSONANT.get().format(new Object[]{derivedWord}));

			variants.add(LanguageVariant.VENETIAN);
		}
		if(RegexHelper.find(derivedWord, patternNonVanishingElNotAtEnd)
				|| derivedWord.contains(GraphemeVEC.GRAPHEME_D_STROKE) || derivedWord.contains(GraphemeVEC.GRAPHEME_T_STROKE))
			variants.add(LanguageVariant.NORTHERN);

		if(variants.size() > 1)
			throw new LinterException(WORD_WITH_MIXED_VARIANTS.get().format(new Object[]{derivedWord}));
	}

	private void incompatibilityCheck(final Inflection inflection, final int index){
		final List<String> pos = inflection.getMorphologicalFieldPartOfSpeech();
		if(pos.size() > 1)
			EventBusService.publish(new LinterWarning(SINGLE_POS_NOT_PRESENT, IndexDataPair.of(index, null)));
	}

	private void orthographyCheck(final Inflection inflection){
		if(hasToCheckForOrthographyAndSyllabation(inflection)){
			String word = inflection.getWord();
			if(!rulesLoader.containsUnsyllabableWords(word) && !rulesLoader.containsMultipleStressedWords(word)){
				word = word.toLowerCase(Locale.ROOT);
				orthographyCheck(word);
			}
		}
	}

	private boolean hasToCheckForOrthographyAndSyllabation(final Inflection inflection){
		return ((rulesLoader.isEnableVerbSyllabationCheck() || !inflection.hasPartOfSpeech(POS_VERB))
			&& !inflection.hasPartOfSpeech(POS_NUMERAL_LATIN) && !inflection.hasPartOfSpeech(POS_UNIT_OF_MEASURE));
	}

	private void orthographyCheck(final String word){
		final String correctedDerivedWord = orthography.correctOrthography(word);
		if(!correctedDerivedWord.equals(word))
			throw new LinterException(MISSPELLED.get().format(new Object[]{word, correctedDerivedWord}));
	}

	@Override
	protected final void checkCompoundInflection(final String subword, final int subwordIndex, final Inflection inflection){
		if(subwordIndex == 0)
			stressCheck(subword, inflection);

		ciuiCheck(subword, inflection);
	}

	private void stressCheck(final String subword, final Inflection inflection){
		if(!rulesLoader.containsMultipleStressedWords(subword)){
			final int stresses = WordVEC.countStresses(subword);
			if(!rulesLoader.isWordCanHaveMultipleStresses() && stresses > 1)
				throw new LinterException(MULTIPLE_STRESSES.get().format(new Object[]{inflection.getWord()}));

			final AffixEntry appliedRule = getLastAppliedRule(inflection);
			if(appliedRule != null){
				final String appliedRuleFlag = appliedRule.getFlag();
				//retrieve last applied rule
				if(stresses == 0 && rulesLoader.containsHasToContainStress(appliedRuleFlag))
					throw new LinterException(MISSING_STRESS.get().format(new Object[]{inflection.getWord(),
						appliedRuleFlag}));
				if(stresses > 0 && WordVEC.countStresses(appliedRule.getAppending()) == 0 && rulesLoader.containsCannotContainStress(appliedRuleFlag))
					throw new LinterException(ALREADY_PRESENT_STRESS.get().format(new Object[]{inflection.getWord(),
						appliedRuleFlag}));
			}
		}
	}

	private AffixEntry getLastAppliedRule(final Inflection inflection){
		AffixEntry appliedRuleFlag = null;
		final AffixEntry[] appliedRules = inflection.getAppliedRules();
		if(appliedRules != null)
			appliedRuleFlag = appliedRules[appliedRules.length - 1];
		return appliedRuleFlag;
	}

	private void ciuiCheck(final String subword, final Inflection inflection){
		if(!inflection.hasPartOfSpeech(POS_NUMERAL_LATIN)){
			final String phonemizedSubword = GraphemeVEC.handleJHJWIUmlautPhonemes(subword);
			if(RegexHelper.find(phonemizedSubword, patternPhonemeCijjhnhiv))
				throw new LinterException(WORD_CANNOT_HAVE_CIJJHNHIV.get().format(new Object[]{inflection.getWord()}));
		}

		if(RegexHelper.find(subword, PATTERN_V_IU_V))
			throw new LinterException(WORD_CANNOT_HAVE_V_IU_V.get().format(new Object[]{inflection.getWord()}));
		if(RegexHelper.find(subword, PATTERN_NOT_V_IU_DIERESIS_V))
			throw new LinterException(WORD_CANNOT_HAVE_NOT_V_IU_DIERESIS_V.get().format(new Object[]{inflection.getWord()}));
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


	@Override
	public final boolean shouldNotCheckProductiveness(final String flag){
		return dontCheckProductivenessRules.contains(flag);
	}

	@Override
	public final boolean isConsonant(final char chr){
		return WordVEC.isConsonant(chr);
	}

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
