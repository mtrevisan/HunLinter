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
package io.github.mtrevisan.hunlinter.parsers.dictionary.generators;

import io.github.mtrevisan.hunlinter.datastructures.FixedArray;
import io.github.mtrevisan.hunlinter.datastructures.SimpleDynamicArray;
import io.github.mtrevisan.hunlinter.languages.DictionaryCorrectnessChecker;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.services.system.LoopHelper;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.mtrevisan.hunlinter.parsers.enums.AffixType;
import io.github.mtrevisan.hunlinter.parsers.vos.AffixEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.Affixes;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.parsers.vos.RuleEntry;

import java.text.MessageFormat;
import java.util.Objects;

import static io.github.mtrevisan.hunlinter.services.system.LoopHelper.forEach;


class WordGeneratorBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordGeneratorBase.class);

	private static final MessageFormat TWOFOLD_RULE_VIOLATED = new MessageFormat("Twofold rule violated for `{0} from {1}` ({2} still has rules {3})");
	private static final MessageFormat NON_EXISTENT_RULE = new MessageFormat("Non-existent rule `{0}`{1}");


	protected final AffixData affixData;
	private final DictionaryCorrectnessChecker checker;


	protected WordGeneratorBase(final AffixData affixData, final DictionaryCorrectnessChecker checker){
		Objects.requireNonNull(affixData);

		this.affixData = affixData;
		this.checker = checker;
	}

	/**
	 * Generates a list of stems for the provided word
	 *
	 * @param dicEntry	{@link DictionaryEntry dictionary entry} used to generate the inflections for
	 * @param isCompound	Whether the word is-a or belongs-to a compound word
	 * @param overriddenRule	Overridden set of rule entries, optional
	 * @return	The list of inflections for the given word
	 * @throws NoApplicableRuleException	If there is a rule that doesn't apply to the word
	 */
	protected Inflection[] applyAffixRules(final DictionaryEntry dicEntry, final boolean isCompound,
			final RuleEntry overriddenRule){
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();
		if(dicEntry.hasContinuationFlag(forbiddenWordFlag))
			return new Inflection[0];

		//extract base inflection
		final Inflection baseInflection = getBaseInflection(dicEntry);
		if(LOGGER.isDebugEnabled()){
			LOGGER.debug("Base inflection:");
			LOGGER.debug("   {}", baseInflection);
		}

		//extract suffixed inflections
		final Inflection[] suffixedInflections = getOnefoldInflections(baseInflection, isCompound, !affixData.isComplexPrefixes(),
			overriddenRule);
		printInflections((affixData.isComplexPrefixes()? "Prefix inflections:": "Suffix inflections:"), suffixedInflections);

		Inflection[] prefixedInflections = new Inflection[0];
		if(!isCompound || affixData.allowTwofoldAffixesInCompound()){
			//extract prefixed inflections
			prefixedInflections = getTwofoldInflections(suffixedInflections, isCompound, !affixData.isComplexPrefixes(),
				overriddenRule);
			printInflections((affixData.isComplexPrefixes()? "Suffix inflections:": "Prefix inflections:"), prefixedInflections);
		}

		//extract lastfold inflections
		Inflection[] twofoldInflections = collectInflections(baseInflection, suffixedInflections, prefixedInflections, null);
		twofoldInflections = getTwofoldInflections(twofoldInflections, isCompound, affixData.isComplexPrefixes(), overriddenRule);
		checkTwofoldCorrectness(twofoldInflections);
		printInflections("Twofold inflections:", twofoldInflections);

		final Inflection[] inflections = collectInflections(baseInflection, suffixedInflections, prefixedInflections, twofoldInflections);
		return filterInflections(inflections);
	}

	private Inflection[] collectInflections(final Inflection baseInflection, final Inflection[] onefoldInflections,
			final Inflection[] twofoldInflections, final Inflection[] lastfoldInflections){
		final int size = 1 + onefoldInflections.length + twofoldInflections.length
			+ (lastfoldInflections != null? lastfoldInflections.length: 0);
		final Inflection[] inflections = new Inflection[size];
		inflections[0] = baseInflection;
		System.arraycopy(onefoldInflections, 0, inflections, 1, onefoldInflections.length);
		System.arraycopy(twofoldInflections, 0, inflections, 1 + onefoldInflections.length, twofoldInflections.length);
		if(lastfoldInflections != null)
			System.arraycopy(lastfoldInflections, 0, inflections, 1 + onefoldInflections.length + twofoldInflections.length,
				lastfoldInflections.length);
		return inflections;
	}


	private void printInflections(final String title, final Inflection[] inflections){
		if(LOGGER.isDebugEnabled() && inflections.length > 0){
			LOGGER.debug(title);
			forEach(inflections,
				inflection -> LOGGER.debug("   {} from {}", inflection.toString(affixData.getFlagParsingStrategy()),
				inflection.getRulesSequence()));
		}
	}


	private Inflection getBaseInflection(final DictionaryEntry dicEntry){
		return Inflection.createFromDictionaryEntry(dicEntry);
	}

	@SuppressWarnings("unchecked")
	protected Inflection[] getOnefoldInflections(final DictionaryEntry dicEntry, final boolean isCompound, final boolean reverse,
			final RuleEntry overriddenRule) throws NoApplicableRuleException{
		@SuppressWarnings("rawtypes")
		final FixedArray[] allAffixes = dicEntry.extractAllAffixes(affixData, reverse);
		return applyAffixRules(dicEntry, allAffixes, isCompound, overriddenRule);
	}

	private Inflection[] getTwofoldInflections(final Inflection[] onefoldInflections, final boolean isCompound,
			final boolean reverse, final RuleEntry overriddenRule) throws NoApplicableRuleException{
		Inflection[] twofoldInflections = new Inflection[0];
		for(final Inflection inflection : onefoldInflections)
			if(inflection.isCombinable()){
				final Inflection[] prods = getOnefoldInflections(inflection, isCompound, reverse, overriddenRule);

				final AffixEntry[] appliedRules = inflection.getAppliedRules();
				//add parent derivations
				for(final Inflection prod : prods)
					prod.prependAppliedRules(appliedRules);

				twofoldInflections = ArrayUtils.addAll(twofoldInflections, prods);
			}
		return twofoldInflections;
	}

	private void checkTwofoldCorrectness(final Inflection[] twofoldInflections){
		final boolean complexPrefixes = affixData.isComplexPrefixes();
		for(final Inflection prod : twofoldInflections){
			@SuppressWarnings("rawtypes")
			final FixedArray[] affixes = prod.extractAllAffixes(affixData, false);
			@SuppressWarnings("unchecked")
			final FixedArray<String> aff = affixes[complexPrefixes? Affixes.INDEX_SUFFIXES: Affixes.INDEX_PREFIXES];
			if(!aff.isEmpty()){
				final String overabundantAffixes = affixData.getFlagParsingStrategy().joinFlags(aff.data, aff.limit);
				throw new LinterException(TWOFOLD_RULE_VIOLATED.format(new Object[]{prod, prod.getRulesSequence(),
					prod.getRulesSequence(), overabundantAffixes}));
			}
		}
	}

	private Inflection[] filterInflections(Inflection[] inflections){
		inflections = enforceCircumfix(inflections);

		return enforceNeedAffixFlag(inflections);
	}

	/**
	 * Remove rules that invalidate the circumfix rule
	 * <p />
	 * <code><pre>
	 * (rule.flag is the flag associated to the rule)
	 * (rule.type is either `PREFIX` or `AFFIX`)
	 * (rule.flags represents the set of the continuation classes)
	 * (rule.has(flag) returns whether the rule contains the given flag in its continuation classes)
	 * (the operation ⋃ does NOT involve uniqueness, so it's kind of a `+`)
	 *
	 * def currentRule = rule | nextRule.flag ∈ dictionary-word.flags
	 * def nextRule = rule | nextRule.flag ∈ (dictionary-word.flags ⋃ currentRule.flags) \ currentRule.flag
	 * if ¬ currentRule.has(circumfix) ∨ ¬ nextRule.has(circumfix) ∧ currentRule.type ≠ nextRule.type then
	 *    accept inflection
	 * else
	 *    discard inflection
	 * </pre></code>
	 */
	private Inflection[] enforceCircumfix(Inflection[] inflections){
		final String circumfixFlag = affixData.getCircumfixFlag();
		if(circumfixFlag != null)
			inflections = LoopHelper.removeIf(inflections,
				inflection -> inflection.hasContinuationFlag(circumfixFlag) && !inflection.isTwofolded(circumfixFlag));
		return inflections;
	}

	/** Remove rules that invalidate the affix rule */
	private Inflection[] enforceNeedAffixFlag(Inflection[] inflections){
		final String needAffixFlag = affixData.getNeedAffixFlag();
		if(needAffixFlag != null)
			inflections = LoopHelper.removeIf(inflections, inflection -> hasNeedAffixFlag(inflection, needAffixFlag));
		return inflections;
	}

	private boolean hasNeedAffixFlag(final Inflection inflection, final String needAffixFlag){
		boolean hasNeedAffixFlag = false;
		final AffixEntry[] appliedRules = inflection.getAppliedRules();
		if(appliedRules != null){
			//check that last suffix and last prefix don't have the needaffix flag
			boolean lastSuffix = false;
			boolean lastPrefix = false;
			boolean lastSuffixNeedAffix = false;
			boolean lastPrefixNeedAffix = false;
			for(int i = appliedRules.length - 1; (!lastSuffix || !lastPrefix) && i >= 0; i --){
				final AffixEntry appliedRule = appliedRules[i];
				switch(appliedRule.getType()){
					case SUFFIX -> {
						if(!lastSuffix){
							lastSuffix = true;
							lastSuffixNeedAffix = appliedRule.hasContinuationFlag(needAffixFlag);
						}
					}
					case PREFIX -> {
						if(!lastPrefix){
							lastPrefix = true;
							lastPrefixNeedAffix = appliedRule.hasContinuationFlag(needAffixFlag);
						}
					}
				}
			}
			hasNeedAffixFlag = (!lastSuffix || lastSuffixNeedAffix) && (!lastPrefix || lastPrefixNeedAffix);
		}
		return (hasNeedAffixFlag || inflection.hasContinuationFlag(needAffixFlag));
	}

	private Inflection[] applyAffixRules(final DictionaryEntry dicEntry, final FixedArray<String>[] allAffixes,
			final boolean isCompound, final RuleEntry overriddenRule) throws NoApplicableRuleException{
		final String circumfixFlag = affixData.getCircumfixFlag();
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();

		final FixedArray<String> appliedAffixes = allAffixes[Affixes.INDEX_PREFIXES];
		final FixedArray<String> postponedAffixes = allAffixes[Affixes.INDEX_SUFFIXES];
		if(circumfixFlag != null && ArrayUtils.contains(allAffixes[Affixes.INDEX_TERMINALS].data, circumfixFlag))
			postponedAffixes.add(circumfixFlag);

		Inflection[] inflections = new Inflection[0];
		if(hasToBeExpanded(dicEntry, appliedAffixes, forbiddenWordFlag))
			for(int i = 0; i < appliedAffixes.limit; i ++){
				final String affix = appliedAffixes.data[i];
				//extract current rule
				RuleEntry rule = affixData.getData(affix);
				//override with the given rule
				if(overriddenRule != null && affix.equals(overriddenRule.getEntries()[0].getFlag()))
					rule = overriddenRule;

				String[] currentPostponedAffixes = postponedAffixes.extractCopyOrNull();
				if(dicEntry.getLastAppliedRule() != null
						&& dicEntry.getLastAppliedRule().getType() == AffixType.SUFFIX ^ rule.getType() == AffixType.SUFFIX)
					currentPostponedAffixes = ArrayUtils.removeElement(currentPostponedAffixes, circumfixFlag);
				final Inflection[] prods = applyAffixRule(dicEntry, affix, currentPostponedAffixes, isCompound, overriddenRule);
				inflections = ArrayUtils.addAll(inflections, prods);
			}
		return inflections;
	}

	private Inflection[] applyAffixRule(final DictionaryEntry dicEntry, final String affix, final String[] postponedAffixes,
			final boolean isCompound, final RuleEntry overriddenRule) throws NoApplicableRuleException{
		final AffixEntry[] appliedRules = dicEntry.getAppliedRules();

		RuleEntry rule = affixData.getData(affix);
		//override with the given rule
		if(overriddenRule != null && affix.equals(overriddenRule.getEntries()[0].getFlag()))
			rule = overriddenRule;
		if(rule == null){
			if(affixData.isManagedByCompoundRule(affix))
				return new Inflection[0];

			final String parentFlag = (appliedRules.length > 0? appliedRules[0].getFlag(): null);
			throw new LinterException(NON_EXISTENT_RULE.format(new Object[]{affix,
				(parentFlag != null? " via " + parentFlag: StringUtils.EMPTY)}));
		}

		final String forbidCompoundFlag = affixData.getForbidCompoundFlag();
		final String permitCompoundFlag = affixData.getPermitCompoundFlag();
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();
		final String circumfixFlag = affixData.getCircumfixFlag();

		final String word = dicEntry.getWord();
		final AffixEntry[] applicableAffixes = AffixData.extractListOfApplicableAffixes(word, rule.getEntries());
		if(applicableAffixes.length == 0 && (checker == null || !checker.shouldNotCheckProductiveness(affix)))
			throw new NoApplicableRuleException("No applicable rules found for flag `" + affix + "` via `"
				+ (dicEntry.getAppliedRules() != null && dicEntry.getAppliedRules().length > 0? dicEntry.toString(): word) + "`");

		final SimpleDynamicArray<Inflection> inflections = new SimpleDynamicArray<>(Inflection.class, applicableAffixes.length);
		for(final AffixEntry entry : applicableAffixes){
			if(shouldApplyEntry(entry, forbidCompoundFlag, permitCompoundFlag, isCompound)){
				//if entry has circumfix constraint and inflection has the same contraint then remove it from postponedAffixes
				boolean removeCircumfixFlag = false;
				if(circumfixFlag != null && appliedRules != null){
					final boolean entryContainsCircumfix = entry.hasContinuationFlag(circumfixFlag);
					final boolean appliedRuleContainsCircumfix = match(appliedRules, entry, circumfixFlag);
					removeCircumfixFlag = (entryContainsCircumfix && (entry.getType() == AffixType.SUFFIX ^ appliedRuleContainsCircumfix));
				}

				//produce the new word
				final String newWord = entry.applyRule(word, affixData.isFullstrip());
				final Inflection inflection = Inflection.createFromInflection(newWord, entry, dicEntry, postponedAffixes,
					rule.isCombinable());
				if(removeCircumfixFlag)
					inflection.removeContinuationFlag(circumfixFlag);
				if(!inflection.hasContinuationFlag(forbiddenWordFlag))
					inflections.add(inflection);
			}
		}
		return inflections.extractCopy();
	}

	private static boolean match(final AffixEntry[] appliedRules, final AffixEntry entry, final String circumfixFlag){
		final AffixType entryType = entry.getType();
		final int size = (appliedRules != null? appliedRules.length: 0);
		for(int i = 0; i < size; i ++){
			final AffixEntry appliedRule = appliedRules[i];
			if((entryType == AffixType.SUFFIX ^ appliedRule.getType() == AffixType.SUFFIX) && appliedRule.hasContinuationFlag(circumfixFlag))
				return true;
		}
		return false;
	}

	private boolean hasToBeExpanded(final DictionaryEntry dicEntry, final FixedArray<String> appliedAffixes,
			final String forbiddenWordFlag){
		return (!appliedAffixes.isEmpty() && !dicEntry.hasContinuationFlag(forbiddenWordFlag));
	}

	private boolean shouldApplyEntry(final AffixEntry entry, final String forbidCompoundFlag, final String permitCompoundFlag,
			final boolean isCompound){
		boolean shouldApply = true;
		if(isCompound){
			final boolean hasForbidFlag = entry.hasContinuationFlag(forbidCompoundFlag);
			final boolean hasPermitFlag = entry.hasContinuationFlag(permitCompoundFlag);
			if(hasForbidFlag || !hasPermitFlag)
				shouldApply = false;
		}
		return shouldApply;
	}

}
