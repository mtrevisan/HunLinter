package unit731.hunlinter.parsers.dictionary.generators;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.vos.RuleEntry;
import unit731.hunlinter.parsers.vos.AffixEntry;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.workers.exceptions.LinterException;


class WordGeneratorBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordGeneratorBase.class);

	private static final Comparator<Production> PRODUCTION_COMPARATOR = Comparator.comparing(prod -> prod.getLastAppliedRule().getFlag());

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
	protected List<Production> applyAffixRules(final DictionaryEntry dicEntry, final boolean isCompound,
			final RuleEntry overriddenRule){
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();
		if(dicEntry.hasContinuationFlag(forbiddenWordFlag))
			return Collections.emptyList();

		//extract base production
		final Production baseProduction = getBaseProduction(dicEntry);
		if(LOGGER.isDebugEnabled()){
			LOGGER.debug("Base production:");
			LOGGER.debug("   {}", baseProduction);
		}

		//extract suffixed productions
		final List<Production> suffixedProductions = getOnefoldProductions(baseProduction, isCompound, !affixData.isComplexPrefixes(),
			overriddenRule);
		suffixedProductions.sort(PRODUCTION_COMPARATOR);
		printProductions((affixData.isComplexPrefixes()? "Prefix productions:": "Suffix productions:"), suffixedProductions);

		List<Production> prefixedProductions = Collections.emptyList();
		if(!isCompound || affixData.allowTwofoldAffixesInCompound()){
			//extract prefixed productions
			prefixedProductions = getTwofoldProductions(suffixedProductions, isCompound, !affixData.isComplexPrefixes(), overriddenRule);
			prefixedProductions.sort(PRODUCTION_COMPARATOR);
			printProductions((affixData.isComplexPrefixes()? "Suffix productions:": "Prefix productions:"), prefixedProductions);
		}

		//extract lastfold productions
		List<Production> twofoldProductions = collectProductions(baseProduction, suffixedProductions, prefixedProductions, null);
		twofoldProductions = getTwofoldProductions(twofoldProductions, isCompound, affixData.isComplexPrefixes(), overriddenRule);
		twofoldProductions.sort(PRODUCTION_COMPARATOR);
		printProductions("Twofold productions:", twofoldProductions);

		checkTwofoldCorrectness(twofoldProductions);

		final List<Production> productions = collectProductions(baseProduction, suffixedProductions, prefixedProductions,
			twofoldProductions);
		filterProductions(productions);
		return productions;
	}

	private List<Production> collectProductions(final Production baseProduction, final List<Production> onefoldProductions,
			final List<Production> twofoldProductions, final List<Production> lastfoldProductions){
		final List<Production> productions = new ArrayList<>(1);
		productions.add(baseProduction);
		productions.addAll(onefoldProductions);
		productions.addAll(twofoldProductions);
		if(lastfoldProductions != null)
			productions.addAll(lastfoldProductions);
		return productions;
	}


	private void printProductions(final String title, final List<Production> productions){
		if(LOGGER.isDebugEnabled() && !productions.isEmpty()){
			LOGGER.debug(title);
			productions.forEach(production -> LOGGER.debug("   {} from {}", production.toString(affixData.getFlagParsingStrategy()),
				production.getRulesSequence()));
		}
	}


	private Production getBaseProduction(final DictionaryEntry dicEntry){
		return Production.clone(dicEntry);
	}

	protected List<Production> getOnefoldProductions(final DictionaryEntry dicEntry, final boolean isCompound, final boolean reverse,
			final RuleEntry overriddenRule) throws NoApplicableRuleException{
		final List<List<String>> allAffixes = dicEntry.extractAllAffixes(affixData, reverse);
		return applyAffixRules(dicEntry, allAffixes, isCompound, overriddenRule);
	}

	private List<Production> getTwofoldProductions(final List<Production> onefoldProductions, final boolean isCompound,
			final boolean reverse, final RuleEntry overriddenRule) throws NoApplicableRuleException{
		final List<Production> twofoldProductions = new ArrayList<>();
		for(final Production production : onefoldProductions)
			if(production.isCombinable()){
				final List<Production> prods = getOnefoldProductions(production, isCompound, reverse, overriddenRule);

				final List<AffixEntry> appliedRules = production.getAppliedRules();
				//add parent derivations
				prods.forEach(prod -> prod.prependAppliedRules(appliedRules));

				twofoldProductions.addAll(prods);
			}
		return twofoldProductions;
	}

	private void checkTwofoldCorrectness(final List<Production> twofoldProductions){
		final boolean complexPrefixes = affixData.isComplexPrefixes();
		for(final Production prod : twofoldProductions){
			final List<List<String>> affixes = prod.extractAllAffixes(affixData, false);
			final List<String> aff = affixes.get(complexPrefixes? 1: 0);
			if(!aff.isEmpty()){
				final String overabundantAffixes = affixData.getFlagParsingStrategy().joinFlags(aff.toArray(String[]::new));
				throw new LinterException(TWOFOLD_RULE_VIOLATED.format(new Object[]{prod, prod.getRulesSequence(), prod.getRulesSequence(),
					overabundantAffixes}));
			}
		}
	}

	private void filterProductions(final List<Production> productions){
		enforceCircumfix(productions);

		enforceNeedAffixFlag(productions);
	}

	/** Remove rules that invalidate the circumfix rule */
	private void enforceCircumfix(final List<Production> productions){
		final String circumfixFlag = affixData.getCircumfixFlag();
		if(circumfixFlag != null)
			productions.removeIf(production -> production.hasContinuationFlag(circumfixFlag));
	}

	/** Remove rules that invalidate the affix rule */
	private void enforceNeedAffixFlag(final List<Production> productions){
		final String needAffixFlag = affixData.getNeedAffixFlag();
		if(needAffixFlag != null)
			productions.removeIf(production -> hasNeedAffixFlag(production, needAffixFlag));
	}

	private boolean hasNeedAffixFlag(final Production production, final String needAffixFlag){
		boolean hasNeedAffixFlag = false;
		final List<AffixEntry> appliedRules = production.getAppliedRules();
		if(appliedRules != null){
			//check that last suffix and last prefix don't have the needaffix flag
			boolean lastSuffix = false;
			boolean lastPrefix = false;
			boolean lastSuffixNeedAffix = false;
			boolean lastPrefixNeedAffix = false;
			for(int i = appliedRules.size() - 1; (!lastSuffix || !lastPrefix) && i >= 0; i --){
				final AffixEntry appliedRule = appliedRules.get(i);
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

	private List<Production> applyAffixRules(final DictionaryEntry dicEntry, final List<List<String>> allAffixes,
			final boolean isCompound, final RuleEntry overriddenRule) throws NoApplicableRuleException{
		final String circumfixFlag = affixData.getCircumfixFlag();
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();

		final List<String> appliedAffixes = allAffixes.get(0);
		final List<String> postponedAffixes = allAffixes.get(1);
		if(circumfixFlag != null && allAffixes.get(2).contains(circumfixFlag))
			postponedAffixes.add(circumfixFlag);

		final List<Production> productions = new ArrayList<>();
		if(hasToBeExpanded(dicEntry, appliedAffixes, forbiddenWordFlag))
			appliedAffixes.stream()
				.map(affix -> applyAffixRule(dicEntry, affix, postponedAffixes, isCompound, overriddenRule))
				.forEach(productions::addAll);

		return productions;
	}

	private List<Production> applyAffixRule(final DictionaryEntry dicEntry, final String affix, final List<String> postponedAffixes,
			final boolean isCompound, final RuleEntry overriddenRule) throws NoApplicableRuleException{
		final List<AffixEntry> appliedRules = dicEntry.getAppliedRules();

		RuleEntry rule = affixData.getData(affix);
		//override with the given rule
		if(overriddenRule != null && affix.equals(overriddenRule.getEntries().get(0).getFlag()))
			rule = overriddenRule;
		if(rule == null){
			if(affixData.isManagedByCompoundRule(affix))
				return Collections.emptyList();

			final String parentFlag = (!appliedRules.isEmpty()? appliedRules.get(0).getFlag(): null);
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

		final List<Production> productions = new ArrayList<>();
		for(final AffixEntry entry : applicableAffixes)
			if(shouldApplyEntry(entry, forbidCompoundFlag, permitCompoundFlag, isCompound)){
				//if entry has circumfix constraint and production has the same contraint then remove it from postponedAffixes
				boolean removeCircumfixFlag = false;
				if(circumfixFlag != null && appliedRules != null){
					final boolean entryContainsCircumfix = entry.hasContinuationFlag(circumfixFlag);
					final boolean appliedRuleContainsCircumfix = appliedRules.stream()
						.anyMatch(r -> (entry.isSuffix() ^ r.isSuffix()) && r.hasContinuationFlag(circumfixFlag));
					removeCircumfixFlag = (entryContainsCircumfix && (entry.isSuffix() ^ appliedRuleContainsCircumfix));
				}

				//produce the new word
				final String newWord = entry.applyRule(word, affixData.isFullstrip());
				final Production production = Production.createFromProduction(newWord, entry, dicEntry, postponedAffixes,
					rule.isCombinable());
				if(removeCircumfixFlag)
					production.removeContinuationFlag(circumfixFlag);
				if(!production.hasContinuationFlag(forbiddenWordFlag))
					productions.add(production);
			}
		return productions;
	}

	private boolean hasToBeExpanded(final DictionaryEntry dicEntry, final List<String> appliedAffixes, final String forbiddenWordFlag){
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
