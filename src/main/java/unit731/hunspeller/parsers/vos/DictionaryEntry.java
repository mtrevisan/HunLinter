package unit731.hunspeller.parsers.vos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.dtos.Affixes;
import unit731.hunspeller.parsers.dictionary.dtos.MorphologicalTag;
import unit731.hunspeller.services.PatternHelper;


public class DictionaryEntry{

	private static final int PARAM_WORD = 1;
	private static final int PARAM_FLAGS = 2;
	private static final int PARAM_MORPHOLOGICAL_FIELDS = 3;
	private static final Pattern PATTERN_ENTRY = PatternHelper.pattern("^(?<word>[^\\s]+?)(?:(?<!\\\\)\\/(?<flags>[^\\s]+))?(?:[\\s]+(?<morphologicalFields>.+))?$");

	private static final String SLASH = "/";
	private static final String SLASH_ESCAPED = "\\/";
	private static final String TAB = "\t";
	private static final String COMMA = ",";


	protected String word;
	protected String[] continuationFlags;
	protected final String[] morphologicalFields;
	private final boolean combinable;


	public static DictionaryEntry createFromDictionaryLine(final String line, final FlagParsingStrategy strategy){
		return createFromDictionaryLineWithAliases(line, strategy, null, null);
	}

	public static DictionaryEntry createFromDictionaryLineWithAliases(final String line, final FlagParsingStrategy strategy,
			final List<String> aliasesFlag, final List<String> aliasesMorphologicalField){
		Objects.requireNonNull(line);
		Objects.requireNonNull(strategy);

		final Matcher m = PATTERN_ENTRY.matcher(line);
		if(!m.find())
			throw new IllegalArgumentException("Cannot parse dictionary line '" + line + "'");

		final String word = StringUtils.replace(m.group(PARAM_WORD), SLASH_ESCAPED, SLASH);
		final String dicFlags = m.group(PARAM_FLAGS);
		final String[] continuationFlags = strategy.parseFlags(expandAliases(dicFlags, aliasesFlag));
		final String dicMorphologicalFields = m.group(PARAM_MORPHOLOGICAL_FIELDS);
		final String[] mfs = StringUtils.split(expandAliases(dicMorphologicalFields, aliasesMorphologicalField));
		final String[] morphologicalFields = (containsStem(mfs)? mfs: ArrayUtils.addAll(new String[]{MorphologicalTag.TAG_STEM + word}, mfs));
		final boolean combinable = true;
		return new DictionaryEntry(word, continuationFlags, morphologicalFields, combinable);
	}

	public static DictionaryEntry clone(final DictionaryEntry dicEntry){
		return new DictionaryEntry(dicEntry);
	}

	protected DictionaryEntry(final DictionaryEntry dicEntry){
		Objects.requireNonNull(dicEntry);

		word = dicEntry.word;
		continuationFlags = ArrayUtils.clone(dicEntry.continuationFlags);
		morphologicalFields = ArrayUtils.clone(dicEntry.morphologicalFields);
		combinable = dicEntry.combinable;
	}

	protected DictionaryEntry(final String word, final String[] continuationFlags, final String[] morphologicalFields, final boolean combinable){
		Objects.requireNonNull(word);

		this.word = word;
		this.continuationFlags = continuationFlags;
		this.morphologicalFields = morphologicalFields;
		this.combinable = combinable;
	}

	private static String expandAliases(final String part, final List<String> aliases) throws IllegalArgumentException{
		return (aliases != null && !aliases.isEmpty() && NumberUtils.isCreatable(part)? aliases.get(Integer.parseInt(part) - 1): part);
	}

	private static boolean containsStem(final String[] mfs){
		boolean containsStem = false;
		if(mfs != null)
			for(final String mf : mfs)
				if(mf.startsWith(MorphologicalTag.TAG_STEM)){
					containsStem = true;
					break;
				}
		return containsStem;
	}

	public static String extractWord(final String line){
		Objects.requireNonNull(line);

		final Matcher m = PATTERN_ENTRY.matcher(line);
		if(!m.find())
			throw new IllegalArgumentException("Cannot parse dictionary line '" + line + "'");

		return StringUtils.replace(m.group(PARAM_WORD), SLASH_ESCAPED, SLASH);
	}

	public String getWord(){
		return word;
	}

	public boolean isCombinable(){
		return combinable;
	}

	public void applyInputConversionTable(final AffixData affixData){
		word = affixData.applyInputConversionTable(word);
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
	 * @param affixData	The Affix Data used to determine if a flag is a terminal
	 * @return	Whether there are continuation flags that are not terminal affixes
	 */
	public boolean hasNonTerminalContinuationFlags(final AffixData affixData){
		if(continuationFlags != null)
			for(final String flag : continuationFlags)
				if(!affixData.isTerminalAffix(flag))
					return true;
		return false;
	}

	public int getContinuationFlagCount(){
		return (continuationFlags != null? continuationFlags.length: 0);
	}

	public boolean hasContinuationFlag(final String ... continuationFlags){
		if(this.continuationFlags != null && continuationFlags != null)
			for(final String flag : this.continuationFlags)
				if(ArrayUtils.contains(continuationFlags, flag))
					return true;
		return false;
	}

	public List<AffixEntry> getAppliedRules(){
		return Collections.emptyList();
	}

	/**
	 * Get last applied rule of type {@code type}
	 * 
	 * @param type	The type used to filter the last applied rule
	 * @return	The last applied rule of the specified type
	 */
	public AffixEntry getLastAppliedRule(final AffixEntry.Type type){
		return null;
	}

	public Map<String, Set<DictionaryEntry>> distributeByCompoundRule(final AffixData affixData){
		return Arrays.stream(continuationFlags != null? continuationFlags: new String[0])
			.filter(affixData::isManagedByCompoundRule)
			.collect(Collectors.groupingBy(flag -> flag, Collectors.mapping(x -> this, Collectors.toSet())));
	}

	public Map<String, Set<DictionaryEntry>> distributeByCompoundBeginMiddleEnd(final String compoundBeginFlag, final String compoundMiddleFlag,
			final String compoundEndFlag){
		final Map<String, Set<DictionaryEntry>> distribution = new HashMap<>(3);
		distribution.put(compoundBeginFlag, new HashSet<>());
		distribution.put(compoundMiddleFlag, new HashSet<>());
		distribution.put(compoundEndFlag, new HashSet<>());
		if(continuationFlags != null)
			for(final String flag : continuationFlags){
				final Set<DictionaryEntry> value = distribution.get(flag);
				if(value != null)
					value.add(this);
			}
		return distribution;
	}

	public boolean hasPartOfSpeech(){
		if(morphologicalFields != null)
			for(final String morphologicalField : morphologicalFields)
				if(morphologicalField.startsWith(MorphologicalTag.TAG_PART_OF_SPEECH))
					return true;
		return false;
	}

	public boolean hasPartOfSpeech(final String partOfSpeech){
		return hasMorphologicalField(MorphologicalTag.TAG_PART_OF_SPEECH + partOfSpeech);
	}

	private boolean hasMorphologicalField(final String morphologicalField){
		return (morphologicalFields != null && ArrayUtils.contains(morphologicalFields, morphologicalField));
	}

	public void forEachMorphologicalField(final Consumer<String> fun){
		if(morphologicalFields != null)
			for(final String morphologicalField : morphologicalFields)
				fun.accept(morphologicalField);
	}

	public void removeAffixes(final AffixData affixData){
		final Affixes affixes = separateAffixes(affixData);
		continuationFlags = affixes.getTerminalAffixes();
	}

	public List<String[]> extractAllAffixes(final AffixData affixData, final boolean reverse){
		final Affixes affixes = separateAffixes(affixData);
		return affixes.extractAllAffixes(reverse);
	}

	/**
	 * Separate the prefixes from the suffixes and from the terminals
	 * 
	 * @param affixData	The {@link AffixData}
	 * @return	An object with separated flags, one for each group (prefixes, suffixes, terminals)
	 */
	private Affixes separateAffixes(final AffixData affixData) throws IllegalArgumentException{
		final List<String> terminalAffixes = new ArrayList<>();
		final List<String> prefixes = new ArrayList<>();
		final List<String> suffixes = new ArrayList<>();
		if(continuationFlags != null)
			for(final String affix : continuationFlags){
				if(affixData.isTerminalAffix(affix)){
					terminalAffixes.add(affix);
					continue;
				}

				final Object rule = affixData.getData(affix);
				if(rule == null){
					if(affixData.isManagedByCompoundRule(affix))
						continue;

					final List<AffixEntry> appliedRules = getAppliedRules();
					final String parentFlag = (appliedRules != null && !appliedRules.isEmpty()? appliedRules.get(0).getFlag(): null);
					throw new IllegalArgumentException("Non–existent rule " + affix + " found" + (parentFlag != null? " via " + parentFlag:
						StringUtils.EMPTY));
				}

				if(rule instanceof RuleEntry){
					if(((RuleEntry)rule).isSuffix())
						suffixes.add(affix);
					else
						prefixes.add(affix);
				}
				else
					terminalAffixes.add(affix);
			}

		return new Affixes(prefixes, suffixes, terminalAffixes);
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
