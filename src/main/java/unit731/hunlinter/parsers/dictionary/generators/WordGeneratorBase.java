package unit731.hunlinter.parsers.dictionary.generators;

import java.text.MessageFormat;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.vos.Affixes;
import unit731.hunlinter.parsers.vos.RuleEntry;
import unit731.hunlinter.parsers.vos.AffixEntry;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.services.system.LoopHelper;
import unit731.hunlinter.workers.exceptions.LinterException;


class WordGeneratorBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordGeneratorBase.class);

	private static final MessageFormat TWOFOLD_RULE_VIOLATED = new MessageFormat("Twofold rule violated for ''{0} from {1}'' ({2} still has rules {3})");
	private static final MessageFormat NON_EXISTENT_RULE = new MessageFormat("Non–existent rule ''{0}''{1}");


	protected final AffixData affixData;


	protected WordGeneratorBase(final AffixData affixData){
		this.affixData = affixData;
	}

	/**
	 * Generates a list of stems for the provided word
	 *
	 * @param dicEntry	{@link DictionaryEntry dictionary entry} used to generate the productions for
	 * @param isCompound	Whether the word is-a or belongs-to a compound word
	 * @param overriddenRule	Overridden set of rule entries, optional
	 * @return	The list of productions for the given word
	 * @throws NoApplicableRuleException	If there is a rule that doesn't apply to the word
	 */
	protected Production[] applyAffixRules(final DictionaryEntry dicEntry, final boolean isCompound,
			final RuleEntry overriddenRule){
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();
		if(dicEntry.hasContinuationFlag(forbiddenWordFlag))
			return new Production[0];

		//extract base production
		final Production baseProduction = getBaseProduction(dicEntry);
		if(LOGGER.isDebugEnabled()){
			LOGGER.debug("Base production:");
			LOGGER.debug("   {}", baseProduction);
		}

		//extract suffixed productions
		final Production[] suffixedProductions = getOnefoldProductions(baseProduction, isCompound, !affixData.isComplexPrefixes(),
			overriddenRule);
//		Arrays.sort(suffixedProductions, PRODUCTION_COMPARATOR);
		printProductions((affixData.isComplexPrefixes()? "Prefix productions:": "Suffix productions:"), suffixedProductions);

		Production[] prefixedProductions = new Production[0];
		if(!isCompound || affixData.allowTwofoldAffixesInCompound()){
			//extract prefixed productions
			prefixedProductions = getTwofoldProductions(suffixedProductions, isCompound, !affixData.isComplexPrefixes(),
				overriddenRule);
//			Arrays.sort(prefixedProductions, PRODUCTION_COMPARATOR);
			printProductions((affixData.isComplexPrefixes()? "Suffix productions:": "Prefix productions:"), prefixedProductions);
		}

		//extract lastfold productions
		Production[] twofoldProductions = collectProductions(baseProduction, suffixedProductions, prefixedProductions, null);
		twofoldProductions = getTwofoldProductions(twofoldProductions, isCompound, affixData.isComplexPrefixes(), overriddenRule);
		checkTwofoldCorrectness(twofoldProductions);
//		Arrays.sort(twofoldProductions, PRODUCTION_COMPARATOR);
		printProductions("Twofold productions:", twofoldProductions);

		final Production[] productions = collectProductions(baseProduction, suffixedProductions, prefixedProductions,
			twofoldProductions);
		return filterProductions(productions);
	}

	private Production[] collectProductions(final Production baseProduction, final Production[] onefoldProductions,
			final Production[] twofoldProductions, final Production[] lastfoldProductions){
		final int size = 1 + onefoldProductions.length + twofoldProductions.length
			+ (lastfoldProductions != null? lastfoldProductions.length: 0);
		final Production[] productions = new Production[size];
		productions[0] = baseProduction;
		System.arraycopy(onefoldProductions, 0, productions, 1, onefoldProductions.length);
		System.arraycopy(twofoldProductions, 0, productions, 1 + onefoldProductions.length, twofoldProductions.length);
		if(lastfoldProductions != null)
			System.arraycopy(lastfoldProductions, 0, productions, 1 + onefoldProductions.length + twofoldProductions.length,
				lastfoldProductions.length);
		return productions;
	}


	private void printProductions(final String title, final Production[] productions){
		if(LOGGER.isDebugEnabled() && productions.length > 0){
			LOGGER.debug(title);
			LoopHelper.forEach(productions,
				production -> LOGGER.debug("   {} from {}", production.toString(affixData.getFlagParsingStrategy()),
				production.getRulesSequence()));
		}
	}


	private Production getBaseProduction(final DictionaryEntry dicEntry){
		return Production.clone(dicEntry);
	}

	protected Production[] getOnefoldProductions(final DictionaryEntry dicEntry, final boolean isCompound, final boolean reverse,
			final RuleEntry overriddenRule) throws NoApplicableRuleException{
		final String[][] allAffixes = dicEntry.extractAllAffixes(affixData, reverse);
		return applyAffixRules(dicEntry, allAffixes, isCompound, overriddenRule);
	}

	private Production[] getTwofoldProductions(final Production[] onefoldProductions, final boolean isCompound,
			final boolean reverse, final RuleEntry overriddenRule) throws NoApplicableRuleException{
		Production[] twofoldProductions = new Production[0];
		for(final Production production : onefoldProductions)
			if(production.isCombinable()){
				final Production[] prods = getOnefoldProductions(production, isCompound, reverse, overriddenRule);

				final AffixEntry[] appliedRules = production.getAppliedRules();
				//add parent derivations
				for(final Production prod : prods)
					prod.prependAppliedRules(appliedRules);

				twofoldProductions = ArrayUtils.addAll(twofoldProductions, prods);
			}
		return twofoldProductions;
	}

	private void checkTwofoldCorrectness(final Production[] twofoldProductions){
		final boolean complexPrefixes = affixData.isComplexPrefixes();
		for(final Production prod : twofoldProductions){
			final String[][] affixes = prod.extractAllAffixes(affixData, false);
			final String[] aff = affixes[complexPrefixes? Affixes.INDEX_SUFFIXES: Affixes.INDEX_PREFIXES];
			if(aff.length > 0){
				final String overabundantAffixes = affixData.getFlagParsingStrategy().joinFlags(aff);
				throw new LinterException(TWOFOLD_RULE_VIOLATED.format(new Object[]{prod, prod.getRulesSequence(),
					prod.getRulesSequence(), overabundantAffixes}));
			}
		}
	}

	private Production[] filterProductions(Production[] productions){
		productions = enforceCircumfix(productions);

		return enforceNeedAffixFlag(productions);
	}

	/** Remove rules that invalidate the circumfix rule */
	private Production[] enforceCircumfix(Production[] productions){
		final String circumfixFlag = affixData.getCircumfixFlag();
		if(circumfixFlag != null)
			productions = LoopHelper.removeIf(productions,
				production -> production.hasContinuationFlag(circumfixFlag) && !production.isTwofolded(circumfixFlag));
		return productions;
	}

	/** Remove rules that invalidate the affix rule */
	private Production[] enforceNeedAffixFlag(Production[] productions){
		final String needAffixFlag = affixData.getNeedAffixFlag();
		if(needAffixFlag != null)
			productions = LoopHelper.removeIf(productions, production -> hasNeedAffixFlag(production, needAffixFlag));
		return productions;
	}

	private boolean hasNeedAffixFlag(final Production production, final String needAffixFlag){
		boolean hasNeedAffixFlag = false;
		final AffixEntry[] appliedRules = production.getAppliedRules();
		if(appliedRules != null){
			//check that last suffix and last prefix don't have the needaffix flag
			boolean lastSuffix = false;
			boolean lastPrefix = false;
			boolean lastSuffixNeedAffix = false;
			boolean lastPrefixNeedAffix = false;
			for(int i = appliedRules.length - 1; (!lastSuffix || !lastPrefix) && i >= 0; i --){
				final AffixEntry appliedRule = appliedRules[i];
				if(appliedRule.isSuffix() && !lastSuffix){
					lastSuffix = true;
					lastSuffixNeedAffix = appliedRule.hasContinuationFlag(needAffixFlag);
				}
				if(!appliedRule.isSuffix() && !lastPrefix){
					lastPrefix = true;
					lastPrefixNeedAffix = appliedRule.hasContinuationFlag(needAffixFlag);
				}
			}
			hasNeedAffixFlag = (!lastSuffix || lastSuffixNeedAffix) && (!lastPrefix || lastPrefixNeedAffix);
		}
		return (hasNeedAffixFlag || production.hasContinuationFlag(needAffixFlag));
	}

	private Production[] applyAffixRules(final DictionaryEntry dicEntry, final String[][] allAffixes,
			final boolean isCompound, final RuleEntry overriddenRule) throws NoApplicableRuleException{
		final String circumfixFlag = affixData.getCircumfixFlag();
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();

		final String[] appliedAffixes = allAffixes[Affixes.INDEX_PREFIXES];
		String[] postponedAffixes = allAffixes[Affixes.INDEX_SUFFIXES];
		if(circumfixFlag != null && ArrayUtils.contains(allAffixes[Affixes.INDEX_TERMINALS], circumfixFlag))
			postponedAffixes = ArrayUtils.add(postponedAffixes, circumfixFlag);

		Production[] productions = new Production[0];
		if(hasToBeExpanded(dicEntry, appliedAffixes, forbiddenWordFlag))
			for(final String affix : appliedAffixes){
				//extract current rule
				RuleEntry rule = affixData.getData(affix);
				//override with the given rule
				if(overriddenRule != null && affix.equals(overriddenRule.getEntries().get(0).getFlag()))
					rule = overriddenRule;

				String[] currentPostponedAffixes = ArrayUtils.clone(postponedAffixes);
				if(dicEntry.getLastAppliedRule() != null && dicEntry.getLastAppliedRule().isSuffix() ^ rule.isSuffix())
					currentPostponedAffixes = ArrayUtils.removeElement(currentPostponedAffixes, circumfixFlag);
				final Production[] prods = applyAffixRule(dicEntry, affix, currentPostponedAffixes, isCompound, overriddenRule);
				productions = ArrayUtils.addAll(productions, prods);
			}
		return productions;
	}

	private Production[] applyAffixRule(final DictionaryEntry dicEntry, final String affix, final String[] postponedAffixes,
			final boolean isCompound, final RuleEntry overriddenRule) throws NoApplicableRuleException{
		final AffixEntry[] appliedRules = dicEntry.getAppliedRules();

		RuleEntry rule = affixData.getData(affix);
		//override with the given rule
		if(overriddenRule != null && affix.equals(overriddenRule.getEntries().get(0).getFlag()))
			rule = overriddenRule;
		if(rule == null){
			if(affixData.isManagedByCompoundRule(affix))
				return new Production[0];

			final String parentFlag = (appliedRules.length > 0? appliedRules[0].getFlag(): null);
			throw new LinterException(NON_EXISTENT_RULE.format(new Object[]{affix,
				(parentFlag != null? " via " + parentFlag: StringUtils.EMPTY)}));
		}

		final String forbidCompoundFlag = affixData.getForbidCompoundFlag();
		final String permitCompoundFlag = affixData.getPermitCompoundFlag();
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();
		final String circumfixFlag = affixData.getCircumfixFlag();

		final String word = dicEntry.getWord();
		final List<AffixEntry> applicableAffixes = AffixData.extractListOfApplicableAffixes(word, rule.getEntries());
		if(applicableAffixes.isEmpty())
			throw new NoApplicableRuleException("No applicable rules found for flag '" + affix + "' and word '" + word + "'");

		Production[] productions = new Production[0];
		for(final AffixEntry entry : applicableAffixes)
			if(shouldApplyEntry(entry, forbidCompoundFlag, permitCompoundFlag, isCompound)){
				//if entry has circumfix constraint and production has the same contraint then remove it from postponedAffixes
				boolean removeCircumfixFlag = false;
				if(circumfixFlag != null && appliedRules != null){
					final boolean entryContainsCircumfix = entry.hasContinuationFlag(circumfixFlag);
					final boolean appliedRuleContainsCircumfix = LoopHelper.anyMatch(appliedRules,
						appliedRule -> (entry.isSuffix() ^ appliedRule.isSuffix()) && appliedRule.hasContinuationFlag(circumfixFlag));
					removeCircumfixFlag = (entryContainsCircumfix && (entry.isSuffix() ^ appliedRuleContainsCircumfix));
				}

				//produce the new word
				final String newWord = entry.applyRule(word, affixData.isFullstrip());
				final Production production = Production.createFromProduction(newWord, entry, dicEntry, postponedAffixes,
					rule.isCombinable());
				if(removeCircumfixFlag)
					production.removeContinuationFlag(circumfixFlag);
				if(!production.hasContinuationFlag(forbiddenWordFlag))
					productions = ArrayUtils.add(productions, production);
			}
		return productions;
	}

	private boolean hasToBeExpanded(final DictionaryEntry dicEntry, final String[] appliedAffixes, final String forbiddenWordFlag){
		return (appliedAffixes.length > 0 && !dicEntry.hasContinuationFlag(forbiddenWordFlag));
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
