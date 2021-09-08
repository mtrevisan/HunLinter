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

import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import io.github.mtrevisan.hunlinter.parsers.enums.AffixType;
import io.github.mtrevisan.hunlinter.parsers.enums.MorphologicalTag;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import io.github.mtrevisan.hunlinter.services.system.LoopHelper;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;


public class DictionaryEntry{

	private static final ThreadLocal<MessageFormat> NON_EXISTENT_RULE = JavaHelper.createMessageFormat("Non-existent rule `{0}`{1}");

	private static final String SLASH = "/";
	private static final String TAB = "\t";
	private static final String COMMA = ",";


	protected String word;
	protected List<String> continuationFlags;
	protected final String[] morphologicalFields;
	private final boolean combinable;


	DictionaryEntry(final DictionaryEntry dicEntry){
		Objects.requireNonNull(dicEntry, "Dictionary entry cannot be null");

		word = dicEntry.word;
		continuationFlags = dicEntry.continuationFlags;
		morphologicalFields = dicEntry.morphologicalFields;
		combinable = dicEntry.combinable;
	}

	DictionaryEntry(final String word, final List<String> continuationFlags, final String[] morphologicalFields, final boolean combinable){
		Objects.requireNonNull(word, "Word cannot be null");

		this.word = word;
		this.continuationFlags = (continuationFlags != null && !continuationFlags.isEmpty()? continuationFlags: null);
		if(this.continuationFlags != null)
			this.continuationFlags.sort(Comparator.naturalOrder());
		this.morphologicalFields = morphologicalFields;
		this.combinable = combinable;
	}

//	public static String extractWord(final String line){
//		Objects.requireNonNull(line, "Line cannot be null");
//
//		final Matcher m = RegexHelper.matcher(line, PATTERN_ENTRY);
//		if(!m.find())
//			throw new HunLintException("Cannot parse dictionary line `" + line + "`");
//
//		return StringUtils.replace(m.group(PARAM_WORD), SLASH_ESCAPED, SLASH);
//	}

	public final String getWord(){
		return word;
	}

	public final boolean isCombinable(){
		return combinable;
	}

	public final boolean removeContinuationFlag(final String continuationFlagToRemove){
		boolean removed = false;
		if(continuationFlagToRemove != null && continuationFlags != null){
			final int previousSize = continuationFlags.size();
			continuationFlags.remove(continuationFlagToRemove);

			removed = (continuationFlags.size() != previousSize);

			if(continuationFlags.isEmpty())
				continuationFlags = null;
		}
		return removed;
	}

	/**
	 * @param isTerminalAffix	The method used to determine if a flag is a terminal
	 * @return	Whether there are continuation flags that are not terminal affixes
	 */
	public final boolean hasNonTerminalContinuationFlags(final Predicate<String> isTerminalAffix){
		return (LoopHelper.match(continuationFlags, Predicate.not(isTerminalAffix)) != null);
	}

	public final List<String> getContinuationFlags(){
		return continuationFlags;
	}

	public final int getContinuationFlagCount(){
		return (continuationFlags != null? continuationFlags.size(): 0);
	}

	public final boolean hasContinuationFlag(final String flag){
		return (continuationFlags != null && flag != null && Collections.binarySearch(continuationFlags, flag) >= 0);
	}

	public final boolean hasContinuationFlags(final String[] flags){
		if(continuationFlags != null && flags != null){
			final Set<String> list = new HashSet<>(continuationFlags);
			return (LoopHelper.match(flags, Predicate.not(list::add)) != null);
		}
		return false;
	}

	@SuppressWarnings("DesignForExtension")
	public AffixEntry[] getAppliedRules(){
		return new AffixEntry[0];
	}

	/**
	 * Get last applied rule of type {@code type}
	 *
	 * @param type    The type used to filter the last applied rule
	 * @return    The last applied rule of the specified type
	 */
	@SuppressWarnings("DesignForExtension")
	public AffixEntry getLastAppliedRule(final AffixType type){
		return null;
	}

	/**
	 * Get last applied rule
	 *
	 * @return    The last applied rule of the specified type
	 */
	@SuppressWarnings("DesignForExtension")
	public AffixEntry getLastAppliedRule(){
		return null;
	}

	public final Map<String, List<DictionaryEntry>> distributeByCompoundRule(final AffixData affixData){
		final int size = (continuationFlags != null? continuationFlags.size(): 0);
		final Map<String, List<DictionaryEntry>> result = new HashMap<>(size);
		for(int i = 0; i < size; i ++){
			final String cf = continuationFlags.get(i);
			if(affixData.isManagedByCompoundRule(cf))
				result.computeIfAbsent(cf, k -> new ArrayList<>(1))
					.add(this);
		}
		return result;
	}

	public final Map<String, List<DictionaryEntry>> distributeByCompoundBeginMiddleEnd(final String compoundBeginFlag, final String compoundMiddleFlag, final String compoundEndFlag){
		final Map<String, List<DictionaryEntry>> distribution = new HashMap<>(3);
		distribution.put(compoundBeginFlag, new ArrayList<>(0));
		distribution.put(compoundMiddleFlag, new ArrayList<>(0));
		distribution.put(compoundEndFlag, new ArrayList<>(0));
		if(continuationFlags != null)
			for(final String flag : continuationFlags)
				distribution.get(flag)
					.add(this);
		return distribution;
	}

	public final boolean hasPartOfSpeech(){
		return (LoopHelper.match(morphologicalFields, MorphologicalTag.PART_OF_SPEECH::isSupertypeOf) != null);
	}

	/**
	 * @param partOfSpeech	Part-of-Speech WITH the MorphologicalTag.PART_OF_SPEECH prefix
	 * @return	Whether this entry has the given Part-of-Speech
	 */
	public final boolean hasPartOfSpeech(final String partOfSpeech){
		return hasMorphologicalField(partOfSpeech);
	}

	private boolean hasMorphologicalField(final String morphologicalField){
		return (morphologicalFields != null && ArrayUtils.contains(morphologicalFields, morphologicalField));
	}

	public final String getMorphologicalFieldStem(){
		if(morphologicalFields != null){
			final String tag = MorphologicalTag.STEM.getCode();
			for(final String mf : morphologicalFields)
				if(mf.startsWith(tag))
					return mf;
		}
		return word;
	}

	public final List<String> getMorphologicalFieldPartOfSpeech(){
		if(morphologicalFields == null)
			return Collections.emptyList();

		final String tag = MorphologicalTag.PART_OF_SPEECH.getCode();
		final List<String> list = new ArrayList<>(morphologicalFields.length);
		for(final String mf : morphologicalFields)
			if(mf.startsWith(tag))
				list.add(mf);
		return list;
	}

	/**
	 * @param affixData   Affix data
	 * @param reverse   Whether the complex prefixes is used
	 * @return	A list of prefixes, suffixes, and terminal affixes (the first two may be exchanged if
	 * 			COMPLEXPREFIXES is defined)
	 */
	public final List<List<String>> extractAllAffixes(final AffixData affixData, final boolean reverse){
		final Affixes affixes = separateAffixes(affixData);
		return affixes.extractAllAffixes(reverse);
	}

	/**
	 * Separate the prefixes from the suffixes and from the terminals
	 *
	 * @param affixData	The {@link AffixData}
	 * @return	An object with separated flags, one for each group (prefixes, suffixes, terminals)
	 */
	private Affixes separateAffixes(final AffixData affixData){
		final int maxSize = (continuationFlags != null? continuationFlags.size(): 0);
		final List<String> terminals = new ArrayList<>(maxSize);
		final List<String> prefixes = new ArrayList<>(maxSize);
		final List<String> suffixes = new ArrayList<>(maxSize);
		if(continuationFlags != null){
			for(final String affix : continuationFlags){
				if(affixData.isTerminalAffix(affix)){
					terminals.add(affix);
					continue;
				}

				final Object rule = affixData.getData(affix);
				if(rule == null){
					if(affixData.isManagedByCompoundRule(affix))
						continue;

					final AffixEntry[] appliedRules = getAppliedRules();
					final String parentFlag = (appliedRules != null && appliedRules.length > 0? appliedRules[0].getFlag(): null);
					throw new LinterException(NON_EXISTENT_RULE, affix, (parentFlag != null? " via " + parentFlag: StringUtils.EMPTY));
				}

				if(rule instanceof RuleEntry){
					if(((RuleEntry)rule).getType() == AffixType.SUFFIX)
						suffixes.add(affix);
					else
						prefixes.add(affix);
				}
				else
					terminals.add(affix);
			}
		}

		return new Affixes(prefixes, suffixes, terminals);
	}

	@SuppressWarnings("DesignForExtension")
	public boolean isCompound(){
		return false;
	}

	@Override
	@SuppressWarnings("DesignForExtension")
	public String toString(){
		return toString(null);
	}

	@SuppressWarnings("DesignForExtension")
	public String toString(final FlagParsingStrategy strategy){
		final StringBuilder sb = new StringBuilder(word);
		if(continuationFlags != null && !continuationFlags.isEmpty()){
			sb.append(SLASH);
			sb.append(strategy != null? strategy.joinFlags(continuationFlags): StringUtils.join(continuationFlags, COMMA));
		}
		if(morphologicalFields != null && morphologicalFields.length > 0)
			sb.append(TAB).append(StringUtils.join(morphologicalFields, StringUtils.SPACE));
		return sb.toString();
	}

	@Override
	public final boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final DictionaryEntry rhs = (DictionaryEntry)obj;
		return (word.equals(rhs.word)
			&& Objects.equals(continuationFlags, rhs.continuationFlags)
			&& Arrays.equals(morphologicalFields, rhs.morphologicalFields));
	}

	@Override
	public final int hashCode(){
		int result = (word == null? 0: word.hashCode());
		result = 31 * result + (continuationFlags == null? 0: continuationFlags.hashCode());
		result = 31 * result + Arrays.hashCode(morphologicalFields);
		return result;
	}

}
