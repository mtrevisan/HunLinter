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
package io.github.mtrevisan.hunlinter.parsers.vos;

import io.github.mtrevisan.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import io.github.mtrevisan.hunlinter.parsers.enums.AffixType;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;


public class Inflection extends DictionaryEntry{

	private static final String TAB = "\t";
	private static final String FROM = "from";
	private static final String LEADS_TO = " > ";
	private static final String POS_FIELD_PREFIX = ":";

	public static final String POS_FSA_SEPARATOR = ",";
	private static final String COMMA = ",";


	private AffixEntry[] appliedRules;

	private final List<DictionaryEntry> compoundEntries;


	public static Inflection createFromCompound(final String word, final List<String> continuationFlags,
			final List<DictionaryEntry> compoundEntries){
		final String[] morphologicalFields = AffixEntry.extractMorphologicalFields(compoundEntries);
		return new Inflection(word, continuationFlags, morphologicalFields, true, null, compoundEntries);
	}

	public static Inflection createFromInflection(final String word, final AffixEntry appliedEntry, final boolean combinable){
		return new Inflection(word, appliedEntry.continuationFlags, appliedEntry.morphologicalFields, combinable,
			new AffixEntry[]{appliedEntry}, null);
	}

	public static Inflection createFromInflection(final String word, final AffixEntry appliedEntry,
			final DictionaryEntry dicEntry, final Collection<String> remainingContinuationFlags, final boolean combinable){
		final List<String> continuationFlags = appliedEntry.combineContinuationFlags(remainingContinuationFlags);
		final String[] morphologicalFields = appliedEntry.combineMorphologicalFields(dicEntry);
		final AffixEntry[] appliedRules = {appliedEntry};
		final List<DictionaryEntry> compoundEntries = extractCompoundEntries(dicEntry);
		return new Inflection(word, continuationFlags, morphologicalFields, combinable, appliedRules, compoundEntries);
	}

	public static Inflection createFromDictionaryEntry(final DictionaryEntry dicEntry){
		return new Inflection(dicEntry);
	}

	private Inflection(final DictionaryEntry dicEntry){
		super(dicEntry);

		compoundEntries = extractCompoundEntries(dicEntry);
	}

	private Inflection(final String word, final List<String> continuationFlags, final String[] morphologicalFields,
			final boolean combinable, final AffixEntry[] appliedRules, final List<DictionaryEntry> compoundEntries){
		super(word, continuationFlags, morphologicalFields, combinable);

		this.appliedRules = appliedRules;
		this.compoundEntries = compoundEntries;
	}

	/* NOTE: used for testing purposes. */
	public Inflection(final String word, final String continuationFlags, final String morphologicalFields,
			final List<DictionaryEntry> compoundEntries, final FlagParsingStrategy strategy){
		super(word, (strategy != null && StringUtils.isNotBlank(continuationFlags)
				? Arrays.asList(strategy.parseFlags(continuationFlags))
				: null),
			(morphologicalFields != null? StringUtils.split(morphologicalFields): null), true);

		this.compoundEntries = compoundEntries;
	}

	private static List<DictionaryEntry> extractCompoundEntries(final DictionaryEntry dicEntry){
		return (dicEntry instanceof Inflection inflection? inflection.compoundEntries: null);
	}

	@Override
	public final AffixEntry[] getAppliedRules(){
		return appliedRules;
	}

	public final AffixEntry getAppliedRule(final int index){
		return (appliedRules != null && index < appliedRules.length? appliedRules[index]: null);
	}

	public final boolean hasAppliedRule(final String flag){
		if(appliedRules != null)
			for(final AffixEntry appliedRule : appliedRules)
				if(appliedRule.getFlag().equals(flag))
					return true;
		return false;
	}

	@Override
	public final AffixEntry getLastAppliedRule(final AffixType type){
		AffixEntry result = null;
		if(appliedRules != null)
			for(final AffixEntry rule : appliedRules)
				result = rule;
		return result;
	}

	@Override
	public final AffixEntry getLastAppliedRule(){
		return (appliedRules != null? appliedRules[appliedRules.length - 1]: null);
	}

	public final void capitalizeIfContainsFlag(final String forceCompoundUppercaseFlag){
		if(compoundEntries != null && !compoundEntries.isEmpty()
				&& compoundEntries.get(compoundEntries.size() - 1).hasContinuationFlag(forceCompoundUppercaseFlag))
			word = StringUtils.capitalize(word);
	}

	public final boolean hasMorphologicalFields(){
		return (morphologicalFields != null && morphologicalFields.length > 0);
	}

	public final void prependAppliedRules(final AffixEntry[] appliedRules){
		if(appliedRules != null)
			this.appliedRules = ArrayUtils.insert(0, (this.appliedRules != null? this.appliedRules: new AffixEntry[1]),
				appliedRules);
	}

	public final boolean hasInflectionRules(){
		return (appliedRules != null && appliedRules.length > 0);
	}

//	public boolean hasInflectionRule(final String continuationFlag){
//		return (appliedRules != null && Arrays.stream(appliedRules).map(AffixEntry::getFlag).anyMatch(flag -> flag.equals(continuationFlag)));
//	}

//	public boolean hasInflectionRule(final AffixEntry.Type type){
//		return (appliedRules != null && appliedRules.stream().map(AffixEntry::getType).anyMatch(t -> t == type));
//	}

	@SuppressWarnings("ConstantConditions")
	public final boolean isTwofolded(final String circumfixFlag){
		if(appliedRules != null){
			//find last applied rule with circumfix flag
			int startIndex = appliedRules.length - 1;
			while(startIndex >= 0)
				if(appliedRules[startIndex --].hasContinuationFlag(circumfixFlag))
					break;

			final int[] suffixesAffixesCount = new int[2];
			for(int idx = startIndex + 1; idx < appliedRules.length; idx ++)
				suffixesAffixesCount[appliedRules[idx].getType() == AffixType.SUFFIX? 1: 0] ++;
			return (suffixesAffixesCount[0] > 0 && suffixesAffixesCount[1] > 0);
		}
		return false;
	}

	public final String getRulesSequence(){
		final StringJoiner sj = new StringJoiner(LEADS_TO);
		if(appliedRules != null)
			for(final AffixEntry rule : appliedRules)
				sj.add(rule.getFlag());
		return sj.toString();
	}

	public final String getMorphologicalFields(){
		return (morphologicalFields != null? StringUtils.join(morphologicalFields, StringUtils.SPACE): StringUtils.EMPTY);
	}

	public final String[] getMorphologicalFieldsAsArray(){
		return morphologicalFields;
	}

	@Override
	public final boolean isCompound(){
		return (compoundEntries != null && !compoundEntries.isEmpty());
	}

	public final String toStringWithPartOfSpeechAndStem(){
		final List<String> pos = getMorphologicalFieldPartOfSpeech();
		final String stem = getMorphologicalFieldStem();
		if(!pos.isEmpty()){
			pos.sort(Comparator.naturalOrder());
			return word + POS_FIELD_PREFIX + StringUtils.join(pos, COMMA)
				+ StringUtils.SPACE + stem;
		}
		return word;
	}

	public final void applyOutputConversionTable(final Function<String, String> outputConversionTable){
		word = outputConversionTable.apply(word);
	}

	@Override
	public final String toString(){
		return toString(null);
	}

	@Override
	public final String toString(final FlagParsingStrategy strategy){
		final StringJoiner sj = new StringJoiner(TAB);
		sj.add(super.toString(strategy));
		if(hasInflectionRules()){
			sj.add(FROM);
			final StringJoiner subsj = new StringJoiner(LEADS_TO);
			for(final AffixEntry appliedRule : appliedRules)
				subsj.add(appliedRule.toString());
			sj.add(subsj.toString());
		}
		return sj.toString();
	}

}
