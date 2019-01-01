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


	protected WordGeneratorBase(AffixData affixData){
		this.affixData = affixData;
	}

	/**
	 * Generates a list of stems for the provided word
	 * 
	 * @param dicEntry	{@link DictionaryEntry dictionary entry} used to generate the productions for
	 * @param isCompound	Whether the word is-a or belongs-to a compound word
	 * @return	The list of productions for the given word
	 * @throws NoApplicableRuleException	If there is a rule that does not apply to the word
	 */
	protected List<Production> applyAffixRules(DictionaryEntry dicEntry, boolean isCompound) throws IllegalArgumentException,
			NoApplicableRuleException{
		String forbiddenWordFlag = affixData.getForbiddenWordFlag();
		if(dicEntry.hasContinuationFlag(forbiddenWordFlag))
			return Collections.<Production>emptyList();

		//extract base production
		Production baseProduction = getBaseProduction(dicEntry);
		if(LOGGER.isDebugEnabled()){
			LOGGER.debug("Base production:");
			LOGGER.debug("   {}", baseProduction);
		}

		//extract suffixed productions
		List<Production> onefoldProductions = getOnefoldProductions(baseProduction, isCompound, !affixData.isComplexPrefixes());
		printProductions((affixData.isComplexPrefixes()? "Prefix productions:": "Suffix productions:"), onefoldProductions);

		List<Production> twofoldProductions = Collections.<Production>emptyList();
		if(!isCompound || affixData.allowTwofoldAffixesInCompound()){
			//extract prefixed productions
			twofoldProductions = getTwofoldProductions(onefoldProductions, isCompound, !affixData.isComplexPrefixes());
			printProductions((affixData.isComplexPrefixes()? "Suffix productions:": "Prefix productions:"), twofoldProductions);
		}

		//extract lastfold productions
		List<Production> lastfoldProductions = collectProductions(baseProduction, onefoldProductions, twofoldProductions, null);
		lastfoldProductions = getTwofoldProductions(lastfoldProductions, isCompound, affixData.isComplexPrefixes());
		printProductions("Twofold productions:", lastfoldProductions);

		checkTwofoldCorrectness(lastfoldProductions);

		//remove rules that invalidate the circumfix rule
		enforceCircumfix(lastfoldProductions);

		List<Production> productions = collectProductions(baseProduction, onefoldProductions, twofoldProductions, lastfoldProductions);

		//remove rules that invalidate the onlyInCompound rule
		if(isCompound)
			enforceOnlyInCompound(productions);

		//remove rules that invalidate the affix rule
		enforceNeedAffixFlag(productions);

		return productions;
	}

	private List<Production> collectProductions(Production baseProduction, List<Production> onefoldProductions,
			List<Production> twofoldProductions, List<Production> lastfoldProductions){
		List<Production> productions = new ArrayList<>();
		productions.add(baseProduction);
		productions.addAll(onefoldProductions);
		productions.addAll(twofoldProductions);
		if(lastfoldProductions != null)
			productions.addAll(lastfoldProductions);
		return productions;
	}


	private void printProductions(String title, List<Production> productions){
		if(LOGGER.isDebugEnabled() && !productions.isEmpty()){
			LOGGER.debug(title);
			productions.forEach(production -> LOGGER.debug("   {} from {}", production.toString(affixData.getFlagParsingStrategy()),
				production.getRulesSequence()));
		}
	}


	private Production getBaseProduction(DictionaryEntry dicEntry){
		return Production.clone(dicEntry);
	}

	protected List<Production> getOnefoldProductions(DictionaryEntry dicEntry, boolean isCompound, boolean reverse)
			throws NoApplicableRuleException{
		List<String[]> applyAffixes = dicEntry.extractAllAffixes(affixData, reverse);
		return applyAffixRules(dicEntry, applyAffixes, isCompound);
	}

	private List<Production> getTwofoldProductions(List<Production> onefoldProductions, boolean isCompound, boolean reverse)
			throws NoApplicableRuleException{
		List<Production> twofoldProductions = new ArrayList<>();
		for(Production production : onefoldProductions)
			if(production.isCombineable()){
				List<Production> prods = getOnefoldProductions(production, isCompound, reverse);

				List<AffixEntry> appliedRules = production.getAppliedRules();
				for(Production prod : prods)
					//add parent derivations
					prod.prependAppliedRules(appliedRules);

				twofoldProductions.addAll(prods);
			}
		return twofoldProductions;
	}

	private void checkTwofoldCorrectness(List<Production> twofoldProductions) throws IllegalArgumentException{
		boolean complexPrefixes = affixData.isComplexPrefixes();
		for(Production prod : twofoldProductions){
			List<String[]> affixes = prod.extractAllAffixes(affixData, false);
			String[] aff = affixes.get(complexPrefixes? 1: 0);
			if(aff.length > 0){
				String overabundantAffixes = affixData.getFlagParsingStrategy().joinFlags(aff);
				throw new IllegalArgumentException("Twofold rule violated for '" + prod + " from " + prod.getRulesSequence()
					+ "' (" + prod.getRulesSequence() + " still has rules " + overabundantAffixes + ")");
			}
		}
	}

	protected List<Production> enforceOnlyInCompound(List<Production> productions){
		String onlyInCompoundFlag = affixData.getOnlyInCompoundFlag();

		if(onlyInCompoundFlag != null){
			Iterator<Production> itr = productions.iterator();
			while(itr.hasNext()){
				Production production = itr.next();

				if(!production.hasContinuationFlag(onlyInCompoundFlag))
					itr.remove();
			}
		}
		return productions;
	}

	private List<Production> enforceCircumfix(List<Production> lastfoldProductions){
		String circumfixFlag = affixData.getCircumfixFlag();
		if(circumfixFlag != null){
			Iterator<Production> itr = lastfoldProductions.iterator();
			while(itr.hasNext()){
				Production production = itr.next();

				if(affixWithCircumfix(production, circumfixFlag))
					itr.remove();
			}
		}
		return lastfoldProductions;
	}

	private boolean affixWithCircumfix(Production production, String circumfixFlag){
		boolean affixWithCircumfix = false;
		List<AffixEntry> appliedRules = production.getAppliedRules();
		boolean rulesContainsCircumfixFlag = appliedRules.stream()
			.anyMatch(rule -> rule.hasContinuationFlag(circumfixFlag));
		if(rulesContainsCircumfixFlag){
			//check if at least one SFX and one PFX have the circumfix flag
			boolean suffixWithCircumfix = appliedRules.stream()
				.filter(AffixEntry::isSuffix)
				.anyMatch(rule -> rule.hasContinuationFlag(circumfixFlag));
			boolean prefixWithCircumfix = appliedRules.stream()
				.filter(Predicate.not(AffixEntry::isSuffix))
				.anyMatch(rule -> rule.hasContinuationFlag(circumfixFlag));
			affixWithCircumfix = (suffixWithCircumfix ^ prefixWithCircumfix);
		}
		return affixWithCircumfix;
	}

	protected void enforceNeedAffixFlag(List<Production> productions){
		String needAffixFlag = affixData.getNeedAffixFlag();
		if(needAffixFlag != null){
			Iterator<Production> itr = productions.iterator();
			while(itr.hasNext()){
				Production production = itr.next();

				if(hasNeedAffixFlag(production, needAffixFlag))
					itr.remove();
			}
		}
	}

	private boolean hasNeedAffixFlag(Production production, String needAffixFlag){
		boolean hasNeedAffixFlag = false;
		List<AffixEntry> appliedRules = production.getAppliedRules();
		if(appliedRules != null){
			//check that last suffix and last prefix don't have the needaffix flag
			boolean lastSuffix = false;
			boolean lastPrefix = false;
			boolean lastSuffixNeedAffix = false;
			boolean lastPrefixNeedAffix = false;
			for(int i = appliedRules.size() - 1; (!lastSuffix || !lastPrefix) && i >= 0; i --){
				AffixEntry appliedRule = appliedRules.get(i);
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

	private List<Production> applyAffixRules(DictionaryEntry dicEntry, List<String[]> applyAffixes, boolean isCompound)
			throws NoApplicableRuleException{
		String[] appliedAffixes = applyAffixes.get(0);
		String[] postponedAffixes = applyAffixes.get(1);

		String forbiddenWordFlag = affixData.getForbiddenWordFlag();

		List<Production> productions = new ArrayList<>();
		if(appliedAffixes.length > 0 && !dicEntry.hasContinuationFlag(forbiddenWordFlag)){
			String forbidCompoundFlag = affixData.getForbidCompoundFlag();
			String permitCompoundFlag = affixData.getPermitCompoundFlag();

			String word = dicEntry.getWord();

			for(String affix : appliedAffixes){
				RuleEntry rule = affixData.getData(affix);
				if(rule == null){
					if(affixData.isManagedByCompoundRule(affix))
						continue;

					List<AffixEntry> appliedRules = dicEntry.getAppliedRules();
					String parentFlag = (!appliedRules.isEmpty()? appliedRules.get(0).getFlag(): null);
					throw new IllegalArgumentException("Non–existent rule " + affix + " found"
						+ (parentFlag != null? " via " + parentFlag: StringUtils.EMPTY));
				}

				List<AffixEntry> applicableAffixes = AffixData.extractListOfApplicableAffixes(word, rule.getEntries());
				if(applicableAffixes.isEmpty())
					throw new NoApplicableRuleException("No applicable rules found for " + affix + " from " + dicEntry.getWord());

				for(AffixEntry entry : applicableAffixes){
					if(isCompound){
						boolean hasForbidFlag = entry.hasContinuationFlag(forbidCompoundFlag);
						boolean hasPermitFlag = entry.hasContinuationFlag(permitCompoundFlag);
						if(hasForbidFlag || !hasPermitFlag)
							continue;
					}

					//produce the new word
					String newWord = entry.applyRule(word, affixData.isFullstrip());
					Production production = Production.createFromProduction(newWord, entry, dicEntry, postponedAffixes, rule.isCombineable());
					if(!production.hasContinuationFlag(forbiddenWordFlag))
						productions.add(production);
				}
			}
		}

		return productions;
	}

}
