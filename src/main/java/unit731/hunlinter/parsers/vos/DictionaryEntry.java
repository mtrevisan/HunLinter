/**
 * Copyright (c) 2019-2020 Mauro Trevisan
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
package unit731.hunlinter.parsers.vos;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunlinter.datastructures.SetHelper;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.enums.AffixOption;
import unit731.hunlinter.parsers.enums.AffixType;
import unit731.hunlinter.parsers.enums.MorphologicalTag;
import unit731.hunlinter.datastructures.FixedArray;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.RegexHelper;

import static unit731.hunlinter.services.system.LoopHelper.forEach;
import static unit731.hunlinter.services.system.LoopHelper.match;


public class DictionaryEntry{

	private static final MessageFormat WRONG_FORMAT = new MessageFormat("Cannot parse dictionary line ''{0}''");
	private static final MessageFormat NON_EXISTENT_RULE = new MessageFormat("Non-existent rule ''{0}''{1}");

	private static final int PARAM_WORD = 1;
	private static final int PARAM_FLAGS = 2;
	private static final int PARAM_MORPHOLOGICAL_FIELDS = 3;
	private static final Pattern PATTERN_ENTRY = RegexHelper.pattern("^(?<word>[^\\s]+?)(?:(?<!\\\\)\\/(?<flags>[^\\s]+))?(?:[\\s]+(?<morphologicalFields>.+))?$");

	private static final String SLASH = "/";
	private static final String SLASH_ESCAPED = "\\/";
	private static final String TAB = "\t";
	private static final String COMMA = ",";


	protected String word;
	protected String[] continuationFlags;
	protected final String[] morphologicalFields;
	private final boolean combinable;


	public static DictionaryEntry createFromDictionaryLine(final String line, final AffixData affixData){
		return createFromDictionaryLine(line, affixData, true);
	}

	public static DictionaryEntry createFromDictionaryLineNoStemTag(final String line, final AffixData affixData){
		return createFromDictionaryLine(line, affixData, false);
	}

	private static DictionaryEntry createFromDictionaryLine(final String line, final AffixData affixData,
			final boolean addStemTag){
		final FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();
		final List<String> aliasesFlag = affixData.getData(AffixOption.ALIASES_FLAG);
		final List<String> aliasesMorphologicalField = affixData.getData(AffixOption.ALIASES_MORPHOLOGICAL_FIELD);

		Objects.requireNonNull(line);
		Objects.requireNonNull(strategy);

		final Matcher m = RegexHelper.matcher(line, PATTERN_ENTRY);
		if(!m.find())
			throw new LinterException(WRONG_FORMAT.format(new Object[]{line}));

		final String word = StringUtils.replace(m.group(PARAM_WORD), SLASH_ESCAPED, SLASH);
		final String[] continuationFlags = strategy.parseFlags(expandAliases(m.group(PARAM_FLAGS), aliasesFlag));
		final String dicMorphologicalFields = m.group(PARAM_MORPHOLOGICAL_FIELDS);
		final String[] mfs = StringUtils.split(expandAliases(dicMorphologicalFields, aliasesMorphologicalField));
		final String[] morphologicalFields = (!addStemTag || containsStem(mfs)? mfs:
			ArrayUtils.addAll(new String[]{MorphologicalTag.STEM.attachValue(word)}, mfs));
		final boolean combinable = true;
		final String convertedWord = affixData.applyInputConversionTable(word);
		return new DictionaryEntry(convertedWord, continuationFlags, morphologicalFields, combinable);
	}

	protected DictionaryEntry(final DictionaryEntry dicEntry){
		Objects.requireNonNull(dicEntry);

		word = dicEntry.word;
		continuationFlags = dicEntry.continuationFlags;
		morphologicalFields = dicEntry.morphologicalFields;
		combinable = dicEntry.combinable;
	}

	protected DictionaryEntry(final String word, final String[] continuationFlags, final String[] morphologicalFields,
			final boolean combinable){
		Objects.requireNonNull(word);

		this.word = word;
		this.continuationFlags = continuationFlags;
		this.morphologicalFields = morphologicalFields;
		this.combinable = combinable;
	}

	private static String expandAliases(final String part, final List<String> aliases){
		return (aliases != null && !aliases.isEmpty() && NumberUtils.isCreatable(part)?
			aliases.get(Integer.parseInt(part) - 1): part);
	}

	private static boolean containsStem(final String[] mfs){
		return (match(mfs, mf -> mf.startsWith(MorphologicalTag.STEM.getCode())) != null);
	}

//	public static String extractWord(final String line){
//		Objects.requireNonNull(line);
//
//		final Matcher m = RegexHelper.matcher(line, PATTERN_ENTRY);
//		if(!m.find())
//			throw new HunLintException("Cannot parse dictionary line '" + line + "'");
//
//		return StringUtils.replace(m.group(PARAM_WORD), SLASH_ESCAPED, SLASH);
//	}

	public String getWord(){
		return word;
	}

	public boolean isCombinable(){
		return combinable;
	}

	public boolean removeContinuationFlag(final String continuationFlagToRemove){
		boolean removed = false;
		if(continuationFlagToRemove != null && continuationFlags != null){
			final int previousSize = continuationFlags.length;
			continuationFlags = ArrayUtils.removeElement(ArrayUtils.clone(continuationFlags), continuationFlagToRemove);

			removed = (continuationFlags.length != previousSize);

			if(continuationFlags.length == 0)
				continuationFlags = null;
		}
		return removed;
	}

	/**
	 * @param isTerminalAffix	The method used to determine if a flag is a terminal
	 * @return	Whether there are continuation flags that are not terminal affixes
	 */
	public boolean hasNonTerminalContinuationFlags(final Predicate<String> isTerminalAffix){
		return (match(continuationFlags, Predicate.not(isTerminalAffix)) != null);
	}

	public int getContinuationFlagCount(){
		return (continuationFlags != null? continuationFlags.length: 0);
	}

	public boolean hasContinuationFlag(final String flag){
		return (continuationFlags != null && ArrayUtils.contains(continuationFlags, flag));
	}

	public boolean hasContinuationFlags(final String[] flags){
		if(continuationFlags != null && flags != null){
			final Set<String> list = SetHelper.setOf(continuationFlags);
			return (match(flags, Predicate.not(list::add)) != null);
		}
		return false;
	}

	public AffixEntry[] getAppliedRules(){
		return new AffixEntry[0];
	}

	/**
	 * Get last applied rule of type {@code type}
	 *
	 * @param type    The type used to filter the last applied rule
	 * @return    The last applied rule of the specified type
	 */
	public AffixEntry getLastAppliedRule(final AffixType type){
		return null;
	}

	/**
	 * Get last applied rule
	 *
	 * @return    The last applied rule of the specified type
	 */
	public AffixEntry getLastAppliedRule(){
		return null;
	}

	public Map<String, DictionaryEntry[]> distributeByCompoundRule(final AffixData affixData){
		final Map<String, DictionaryEntry[]> result = new HashMap<>();
		final int size = (continuationFlags != null? continuationFlags.length: 0);
		for(int i = 0; i < size; i ++){
			final String cf = continuationFlags[i];
			if(affixData.isManagedByCompoundRule(cf)){
				final DictionaryEntry[] v = result.get(cf);
				result.put(cf, (v != null? ArrayUtils.add(v, this): new DictionaryEntry[]{this}));
			}
		}
		return result;
	}

	public Map<String, DictionaryEntry[]> distributeByCompoundBeginMiddleEnd(final String compoundBeginFlag,
			final String compoundMiddleFlag, final String compoundEndFlag){
		final Map<String, DictionaryEntry[]> distribution = new HashMap<>(3);
		distribution.put(compoundBeginFlag, new DictionaryEntry[0]);
		distribution.put(compoundMiddleFlag, new DictionaryEntry[0]);
		distribution.put(compoundEndFlag, new DictionaryEntry[0]);
		forEach(continuationFlags, flag -> {
			final DictionaryEntry[] entries = distribution.get(flag);
			if(entries != null)
				distribution.put(flag, ArrayUtils.add(entries, this));
		});
		return distribution;
	}

	public boolean hasPartOfSpeech(){
		return (match(morphologicalFields, MorphologicalTag.PART_OF_SPEECH::isSupertypeOf) != null);
	}

	/**
	 * @param partOfSpeech	Part-of-Speech WITH the MorphologicalTag.PART_OF_SPEECH prefix
	 * @return	Whether this entry has the given Part-of-Speech
	 */
	public boolean hasPartOfSpeech(final String partOfSpeech){
		return hasMorphologicalField(partOfSpeech);
	}

	private boolean hasMorphologicalField(final String morphologicalField){
		return (morphologicalFields != null && ArrayUtils.contains(morphologicalFields, morphologicalField));
	}

	public String getMorphologicalFieldStem(){
		if(morphologicalFields != null){
			final String tag = MorphologicalTag.STEM.getCode();
			for(final String mf : morphologicalFields)
				if(mf.startsWith(tag))
					return mf;
		}
		return word;
	}

	public String[] getMorphologicalFieldPartOfSpeech(){
		if(morphologicalFields == null)
			return new String[0];

		final String tag = MorphologicalTag.PART_OF_SPEECH.getCode();
		String[] list = new String[0];
		for(final String mf : morphologicalFields)
			if(mf.startsWith(tag))
				list = ArrayUtils.add(list, mf);
		return list;
	}

	public void forEachMorphologicalField(final Consumer<String> fun){
		forEach(morphologicalFields, fun);
	}

	/**
	 * @param affixData   Affix data
	 * @param reverse   Whether the complex prefixes is used
	 * @return	A list of prefixes, suffixes, and terminal affixes (the first two may be exchanged if
	 * 			COMPLEXPREFIXES is defined)
	 */
	@SuppressWarnings("rawtypes")
	public FixedArray[] extractAllAffixes(final AffixData affixData, final boolean reverse){
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
		final int maxSize = (continuationFlags != null? continuationFlags.length: 0);
		final FixedArray<String> terminals = new FixedArray<>(String.class, maxSize);
		final FixedArray<String> prefixes = new FixedArray<>(String.class, maxSize);
		final FixedArray<String> suffixes = new FixedArray<>(String.class, maxSize);
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
					throw new LinterException(NON_EXISTENT_RULE.format(new Object[]{affix,
						(parentFlag != null? " via " + parentFlag: StringUtils.EMPTY)}));
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

	public boolean isCompound(){
		return false;
	}

	@Override
	public String toString(){
		return toString(null);
	}

	public String toString(final FlagParsingStrategy strategy){
		final StringBuffer sb = new StringBuffer(word);
		if(continuationFlags != null && continuationFlags.length > 0){
			sb.append(SLASH);
			sb.append(strategy != null? strategy.joinFlags(continuationFlags): StringUtils.join(continuationFlags, COMMA));
		}
		if(morphologicalFields != null && morphologicalFields.length > 0)
			sb.append(TAB).append(StringUtils.join(morphologicalFields, StringUtils.SPACE));
		return sb.toString();
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final DictionaryEntry rhs = (DictionaryEntry)obj;
		return new EqualsBuilder()
			.append(word, rhs.word)
			.append(continuationFlags, rhs.continuationFlags)
			.append(morphologicalFields, rhs.morphologicalFields)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(word)
			.append(continuationFlags)
			.append(morphologicalFields)
			.toHashCode();
	}

}
