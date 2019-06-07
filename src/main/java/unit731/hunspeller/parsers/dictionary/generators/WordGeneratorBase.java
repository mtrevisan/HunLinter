package unit731.hunspeller.parsers.dictionary.generators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.dictionary.vos.AffixEntry;
import unit731.hunspeller.parsers.dictionary.vos.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.vos.Production;


class WordGeneratorBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordGeneratorBase.class);


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
	 * @throws NoApplicableRuleException	If there is a rule that does not apply to the word
	 */
	protected List<Production> applyAffixRules(final DictionaryEntry dicEntry, final boolean isCompound, final RuleEntry overriddenRule)
			throws IllegalArgumentException, NoApplicableRuleException{
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();
		if(dicEntry.hasContinuationFlag(forbiddenWordFlag))
			return Collections.<Production>emptyList();

		//extract base production
		final Production baseProduction = getBaseProduction(dicEntry);
		if(LOGGER.isDebugEnabled()){
			LOGGER.debug("Base production:");
			LOGGER.debug("   {}", baseProduction);
		}

		//extract suffixed productions
		final List<Production> onefoldProductions = getOnefoldProductions(baseProduction, isCompound, !affixData.isComplexPrefixes(), overriddenRule);
		printProductions((affixData.isComplexPrefixes()? "Prefix productions:": "Suffix productions:"), onefoldProductions);

		List<Production> twofoldProductions = Collections.<Production>emptyList();
		if(!isCompound || affixData.allowTwofoldAffixesInCompound()){
			//extract prefixed productions
			twofoldProductions = getTwofoldProductions(onefoldProductions, isCompound, !affixData.isComplexPrefixes(), overriddenRule);
			printProductions((affixData.isComplexPrefixes()? "Suffix productions:": "Prefix productions:"), twofoldProductions);
		}

		//extract lastfold productions
		List<Production> lastfoldProductions = collectProductions(baseProduction, onefoldProductions, twofoldProductions, null);
		lastfoldProductions = getTwofoldProductions(lastfoldProductions, isCompound, affixData.isComplexPrefixes(), overriddenRule);
		printProductions("Twofold productions:", lastfoldProductions);

		checkTwofoldCorrectness(lastfoldProductions);

		//remove rules that invalidate the circumfix rule
		enforceCircumfix(lastfoldProductions);

		final List<Production> productions = collectProductions(baseProduction, onefoldProductions, twofoldProductions, lastfoldProductions);

		//remove rules that invalidate the onlyInCompound rule
		if(isCompound)
			enforceOnlyInCompound(productions);

		//remove rules that invalidate the affix rule
		enforceNeedAffixFlag(productions);

		return productions;
	}

	private List<Production> collectProductions(final Production baseProduction, final List<Production> onefoldProductions,
			final List<Production> twofoldProductions, final List<Production> lastfoldProductions){
		final List<Production> productions = new ArrayList<>();
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
		final List<String[]> allAffixes = dicEntry.extractAllAffixes(affixData, reverse);
		return applyAffixRules(dicEntry, allAffixes, isCompound, overriddenRule);
	}

	private List<Production> getTwofoldProductions(final List<Production> onefoldProductions, final boolean isCompound, final boolean reverse,
			final RuleEntry overriddenRule) throws NoApplicableRuleException{
		final List<Production> twofoldProductions = new ArrayList<>();
		for(final Production production : onefoldProductions)
			if(production.isCombinable()){
				final List<Production> prods = getOnefoldProductions(production, isCompound, reverse, overriddenRule);

				final List<AffixEntry> appliedRules = production.getAppliedRules();
				for(final Production prod : prods)
					//add parent derivations
					prod.prependAppliedRules(appliedRules);

				twofoldProductions.addAll(prods);
			}
		return twofoldProductions;
	}

	private void checkTwofoldCorrectness(final List<Production> twofoldProductions) throws IllegalArgumentException{
		final boolean complexPrefixes = affixData.isComplexPrefixes();
		for(final Production prod : twofoldProductions){
			final List<String[]> affixes = prod.extractAllAffixes(affixData, false);
			final String[] aff = affixes.get(complexPrefixes? 1: 0);
			if(aff.length > 0){
				final String overabundantAffixes = affixData.getFlagParsingStrategy().joinFlags(aff);
				throw new IllegalArgumentException("Twofold rule violated for '" + prod + " from " + prod.getRulesSequence()
					+ "' (" + prod.getRulesSequence() + " still has rules " + overabundantAffixes + ")");
			}
		}
	}

	protected List<Production> enforceOnlyInCompound(final List<Production> productions){
		final String onlyInCompoundFlag = affixData.getOnlyInCompoundFlag();
		if(onlyInCompoundFlag != null){
			final Iterator<Production> itr = productions.iterator();
			while(itr.hasNext()){
				final Production production = itr.next();

				if(!production.hasContinuationFlag(onlyInCompoundFlag))
					itr.remove();
			}
		}
		return productions;
	}

	private List<Production> enforceCircumfix(final List<Production> lastfoldProductions){
		final String circumfixFlag = affixData.getCircumfixFlag();
		if(circumfixFlag != null){
			final Iterator<Production> itr = lastfoldProductions.iterator();
			while(itr.hasNext()){
				final Production production = itr.next();

				if(affixWithCircumfix(production, circumfixFlag))
					itr.remove();
			}
		}
		return lastfoldProductions;
	}

	private boolean affixWithCircumfix(final Production production, final String circumfixFlag){
		boolean affixWithCircumfix = false;
		final List<AffixEntry> appliedRules = production.getAppliedRules();
		final boolean rulesContainsCircumfixFlag = appliedRules.stream()
			.anyMatch(rule -> rule.hasContinuationFlag(circumfixFlag));
		if(rulesContainsCircumfixFlag){
			//check if at least one SFX and one PFX have the circumfix flag
			final boolean suffixWithCircumfix = appliedRules.stream()
				.filter(AffixEntry::isSuffix)
				.anyMatch(rule -> rule.hasContinuationFlag(circumfixFlag));
			final boolean prefixWithCircumfix = appliedRules.stream()
				.filter(Predicate.not(AffixEntry::isSuffix))
				.anyMatch(rule -> rule.hasContinuationFlag(circumfixFlag));
			affixWithCircumfix = (suffixWithCircumfix ^ prefixWithCircumfix);
		}
		return affixWithCircumfix;
	}

	protected void enforceNeedAffixFlag(final List<Production> productions){
		final String needAffixFlag = affixData.getNeedAffixFlag();
		if(needAffixFlag != null){
			final Iterator<Production> itr = productions.iterator();
			while(itr.hasNext()){
				final Production production = itr.next();

				if(hasNeedAffixFlag(production, needAffixFlag))
					itr.remove();
			}
		}
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
		return hasNeedAffixFlag;
	}

	private List<Production> applyAffixRules(final DictionaryEntry dicEntry, final List<String[]> allAffixes, final boolean isCompound,
			final RuleEntry overriddenRule) throws NoApplicableRuleException{
		final String[] appliedAffixes = allAffixes.get(0);
		final String[] postponedAffixes = allAffixes.get(1);

		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();

		final List<Production> productions = new ArrayList<>();
		if(hasToBeExpanded(dicEntry, appliedAffixes, forbiddenWordFlag))
			for(final String affix : appliedAffixes){
				final List<Production> subProductions = applyAffixRule(dicEntry, affix, postponedAffixes, isCompound, overriddenRule);
				productions.addAll(subProductions);
			}

		return productions;
	}

	private List<Production> applyAffixRule(final DictionaryEntry dicEntry, final String affix, final String[] postponedAffixes,
			final boolean isCompound, final RuleEntry overriddenRule) throws NoApplicableRuleException{
		RuleEntry rule = affixData.getData(affix);
		//override with the given rule
		if(overriddenRule != null && affix.equals(overriddenRule.getEntries().get(0).getFlag()))
			rule = overriddenRule;
		if(rule == null){
			if(affixData.isManagedByCompoundRule(affix))
				return Collections.<Production>emptyList();

			final List<AffixEntry> appliedRules = dicEntry.getAppliedRules();
			final String parentFlag = (!appliedRules.isEmpty()? appliedRules.get(0).getFlag(): null);
			throw new IllegalArgumentException("Non–existent rule " + affix + " found"
				+ (parentFlag != null? " via " + parentFlag: StringUtils.EMPTY));
		}

		final String forbidCompoundFlag = affixData.getForbidCompoundFlag();
		final String permitCompoundFlag = affixData.getPermitCompoundFlag();
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();

		final String word = dicEntry.getWord();
		final List<AffixEntry> applicableAffixes = AffixData.extractListOfApplicableAffixes(word, rule.getEntries());
		if(applicableAffixes.isEmpty())
			throw new NoApplicableRuleException("No applicable rules found for tag '" + affix + "' and word '" + word + "'");

		final List<Production> productions = new ArrayList<>();
		for(final AffixEntry entry : applicableAffixes)
			if(shouldApplyEntry(entry, forbidCompoundFlag, permitCompoundFlag, isCompound)){
				//produce the new word
				final String newWord = entry.applyRule(word, affixData.isFullstrip());
				final Production production = Production.createFromProduction(newWord, entry, dicEntry, postponedAffixes, rule.isCombinable());
				if(!production.hasContinuationFlag(forbiddenWordFlag))
					productions.add(production);
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
