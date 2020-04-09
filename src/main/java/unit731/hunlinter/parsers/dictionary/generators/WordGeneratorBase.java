package unit731.hunlinter.parsers.dictionary.generators;

import java.text.MessageFormat;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.enums.AffixType;
import unit731.hunlinter.parsers.vos.Affixes;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.parsers.vos.RuleEntry;
import unit731.hunlinter.parsers.vos.AffixEntry;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.services.GrowableArray;
import unit731.hunlinter.workers.exceptions.LinterException;

import static unit731.hunlinter.services.system.LoopHelper.forEach;
import static unit731.hunlinter.services.system.LoopHelper.match;
import static unit731.hunlinter.services.system.LoopHelper.removeIf;


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
//		Arrays.sort(suffixedInflections, INFLECTION_COMPARATOR);
		printInflections((affixData.isComplexPrefixes()? "Prefix inflections:": "Suffix inflections:"), suffixedInflections);

		Inflection[] prefixedInflections = new Inflection[0];
		if(!isCompound || affixData.allowTwofoldAffixesInCompound()){
			//extract prefixed inflections
			prefixedInflections = getTwofoldInflections(suffixedInflections, isCompound, !affixData.isComplexPrefixes(),
				overriddenRule);
//			Arrays.sort(prefixedInflections, INFLECTION_COMPARATOR);
			printInflections((affixData.isComplexPrefixes()? "Suffix inflections:": "Prefix inflections:"), prefixedInflections);
		}

		//extract lastfold inflections
		Inflection[] twofoldInflections = collectInflections(baseInflection, suffixedInflections, prefixedInflections, null);
		twofoldInflections = getTwofoldInflections(twofoldInflections, isCompound, affixData.isComplexPrefixes(), overriddenRule);
		checkTwofoldCorrectness(twofoldInflections);
//		Arrays.sort(twofoldInflections, INFLECTION_COMPARATOR);
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
		return Inflection.clone(dicEntry);
	}

	protected Inflection[] getOnefoldInflections(final DictionaryEntry dicEntry, final boolean isCompound, final boolean reverse,
			final RuleEntry overriddenRule) throws NoApplicableRuleException{
		final GrowableArray<String>[] allAffixes = dicEntry.extractAllAffixes(affixData, reverse);
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
			final GrowableArray<String>[] affixes = prod.extractAllAffixes(affixData, false);
			final GrowableArray<String> aff = affixes[complexPrefixes? Affixes.INDEX_SUFFIXES: Affixes.INDEX_PREFIXES];
			if(!aff.isEmpty()){
				final String overabundantAffixes = affixData.getFlagParsingStrategy().joinFlags(aff);
				throw new LinterException(TWOFOLD_RULE_VIOLATED.format(new Object[]{prod, prod.getRulesSequence(),
					prod.getRulesSequence(), overabundantAffixes}));
			}
		}
	}

	private Inflection[] filterInflections(Inflection[] inflections){
		inflections = enforceCircumfix(inflections);

		return enforceNeedAffixFlag(inflections);
	}

	/** Remove rules that invalidate the circumfix rule */
	private Inflection[] enforceCircumfix(Inflection[] inflections){
		final String circumfixFlag = affixData.getCircumfixFlag();
		if(circumfixFlag != null)
			inflections = removeIf(inflections,
				inflection -> inflection.hasContinuationFlag(circumfixFlag) && !inflection.isTwofolded(circumfixFlag));
		return inflections;
	}

	/** Remove rules that invalidate the affix rule */
	private Inflection[] enforceNeedAffixFlag(Inflection[] inflections){
		final String needAffixFlag = affixData.getNeedAffixFlag();
		if(needAffixFlag != null)
			inflections = removeIf(inflections, inflection -> hasNeedAffixFlag(inflection, needAffixFlag));
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
				if(appliedRule.getType() == AffixType.SUFFIX && !lastSuffix){
					lastSuffix = true;
					lastSuffixNeedAffix = appliedRule.hasContinuationFlag(needAffixFlag);
				}
				if(appliedRule.getType() != AffixType.SUFFIX && !lastPrefix){
					lastPrefix = true;
					lastPrefixNeedAffix = appliedRule.hasContinuationFlag(needAffixFlag);
				}
			}
			hasNeedAffixFlag = (!lastSuffix || lastSuffixNeedAffix) && (!lastPrefix || lastPrefixNeedAffix);
		}
		return (hasNeedAffixFlag || inflection.hasContinuationFlag(needAffixFlag));
	}

	private Inflection[] applyAffixRules(final DictionaryEntry dicEntry, final GrowableArray<String>[] allAffixes,
			final boolean isCompound, final RuleEntry overriddenRule) throws NoApplicableRuleException{
		final String circumfixFlag = affixData.getCircumfixFlag();
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();

		final GrowableArray<String> appliedAffixes = allAffixes[Affixes.INDEX_PREFIXES];
		GrowableArray<String> postponedAffixes = allAffixes[Affixes.INDEX_SUFFIXES];
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
		final GrowableArray<AffixEntry> applicableAffixes = AffixData.extractListOfApplicableAffixes(word, rule.getEntries());
		if(applicableAffixes.isEmpty())
			throw new NoApplicableRuleException("No applicable rules found for flag '" + affix + "' and word '" + word + "'");

		Inflection[] inflections = new Inflection[0];
		for(int i = 0; i < applicableAffixes.limit; i ++){
			final AffixEntry entry = applicableAffixes.data[i];
			if(shouldApplyEntry(entry, forbidCompoundFlag, permitCompoundFlag, isCompound)){
				//if entry has circumfix constraint and inflection has the same contraint then remove it from postponedAffixes
				boolean removeCircumfixFlag = false;
				if(circumfixFlag != null && appliedRules != null){
					final boolean entryContainsCircumfix = entry.hasContinuationFlag(circumfixFlag);
					final boolean appliedRuleContainsCircumfix = (match(appliedRules,
						appliedRule -> (entry.getType() == AffixType.SUFFIX ^ appliedRule.getType() == AffixType.SUFFIX) && appliedRule.hasContinuationFlag(circumfixFlag)) != null);
					removeCircumfixFlag = (entryContainsCircumfix && (entry.getType() == AffixType.SUFFIX ^ appliedRuleContainsCircumfix));
				}

				//produce the new word
				final String newWord = entry.applyRule(word, affixData.isFullstrip());
				final Inflection inflection = Inflection.createFromInflection(newWord, entry, dicEntry, postponedAffixes,
					rule.isCombinable());
				if(removeCircumfixFlag)
					inflection.removeContinuationFlag(circumfixFlag);
				if(!inflection.hasContinuationFlag(forbiddenWordFlag))
					inflections = ArrayUtils.add(inflections, inflection);
			}
		}
		return inflections;
	}

	private boolean hasToBeExpanded(final DictionaryEntry dicEntry, final GrowableArray<String> appliedAffixes,
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
