package unit731.hunspeller.parsers.dictionary.valueobjects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.dtos.Affixes;
import unit731.hunspeller.parsers.dictionary.dtos.MorphologicalTag;
import unit731.hunspeller.services.PatternHelper;


public class DictionaryEntry{

	private static final int PARAM_WORD = 1;
	private static final int PARAM_FLAGS = 2;
	private static final int PARAM_MORPHOLOGICAL_FIELDS = 3;
	private static final Matcher ENTRY_PATTERN = PatternHelper.matcher("^(?<word>[^\\s]+?)(?:(?<!\\\\)\\/(?<flags>[^\\s]+))?(?:[\\s]+(?<morphologicalFields>.+))?$");

	private static final String SLASH = "/";
	private static final String SLASH_ESCAPED = "\\/";


	protected String word;
	protected String[] continuationFlags;
	protected final String[] morphologicalFields;
	private final boolean combineable;


	protected DictionaryEntry(String word, String[] continuationFlags, String[] morphologicalFields, boolean combineable){
		Objects.requireNonNull(word);

		this.word = word;
		this.continuationFlags = continuationFlags;
		this.morphologicalFields = morphologicalFields;
		this.combineable = combineable;
	}

	public DictionaryEntry(String line, FlagParsingStrategy strategy){
		this(line, strategy, null, null);
	}

	public DictionaryEntry(String line, FlagParsingStrategy strategy, List<String> aliasesFlag, List<String> aliasesMorphologicaField){
		Objects.requireNonNull(line);
		Objects.requireNonNull(strategy);

		Matcher m = ENTRY_PATTERN.reset(line);
		if(!m.find())
			throw new IllegalArgumentException("Cannot parse dictionary line " + line);

		word = StringUtils.replace(m.group(PARAM_WORD), SLASH_ESCAPED, SLASH);
		String dicFlags = m.group(PARAM_FLAGS);
		continuationFlags = strategy.parseFlags(expandAliases(dicFlags, aliasesFlag));
		String dicMorphologicalFields = m.group(PARAM_MORPHOLOGICAL_FIELDS);
		morphologicalFields = ArrayUtils.addAll(new String[]{MorphologicalTag.TAG_STEM + word},
			(dicMorphologicalFields != null? StringUtils.split(expandAliases(dicMorphologicalFields, aliasesMorphologicaField)): null));
		combineable = true;
	}

	public String getWord(){
		return word;
	}

	public boolean isCombineable(){
		return combineable;
	}

	public static String extractWord(String line){
		Objects.requireNonNull(line);

		Matcher m = ENTRY_PATTERN.reset(line);
		if(!m.find())
			throw new IllegalArgumentException("Cannot parse dictionary line " + line);

		return StringUtils.replace(m.group(PARAM_WORD), SLASH_ESCAPED, SLASH);
	}

	private String expandAliases(String part, List<String> aliases) throws IllegalArgumentException{
		return (aliases != null && !aliases.isEmpty() && NumberUtils.isCreatable(part)? aliases.get(Integer.parseInt(part) - 1): part);
	}

	public void applyConversionTable(Function<String, String> inputConversion){
		if(inputConversion != null)
			//convert using input table
			word = inputConversion.apply(word);
	}

	public boolean removeContinuationFlag(String continuationFlagToRemove){
		boolean removed = false;
		if(continuationFlagToRemove != null && continuationFlags != null){
			int previousSize = continuationFlags.length;
			continuationFlags = ArrayUtils.removeElement(ArrayUtils.clone(continuationFlags), continuationFlagToRemove);

			removed = (continuationFlags.length != previousSize);

			if(continuationFlags.length == 0)
				continuationFlags = null;
		}
		return removed;
	}

	/* Clone constructor */
	public DictionaryEntry(DictionaryEntry dicEntry){
		Objects.requireNonNull(dicEntry);

		word = dicEntry.word;
		continuationFlags = ArrayUtils.clone(dicEntry.continuationFlags);
		morphologicalFields = ArrayUtils.clone(dicEntry.morphologicalFields);
		combineable = dicEntry.combineable;
	}

	/**
	 * @param affParser	The Affix Parser used to determine if a flag is a terminal
	 * @return	Whether there are continuation flags that are not terminal affixes
	 */
	public boolean hasContinuationFlags(AffixParser affParser){
		int continuationFlagsCount = 0;
		if(continuationFlags != null)
			for(String flag : continuationFlags)
				if(!affParser.isTerminalAffix(flag))
					continuationFlagsCount ++;
		return (continuationFlagsCount > 0);
	}

	public boolean hasContinuationFlag(String ... continuationFlags){
		if(this.continuationFlags != null && continuationFlags != null)
			for(String flag : this.continuationFlags)
				if(ArrayUtils.contains(continuationFlags, flag))
					return true;
		return false;
	}

	public Map<String, Set<DictionaryEntry>> distributeByCompoundRule(AffixParser affParser){
		return Arrays.stream(continuationFlags != null? continuationFlags: new String[0])
			.filter(affParser::isManagedByCompoundRule)
			.collect(Collectors.groupingBy(flag -> flag, Collectors.mapping(x -> this, Collectors.toSet())));
	}

	public Map<String, Set<DictionaryEntry>> distributeByCompoundBeginMiddleEnd(AffixParser affParser, String compoundBeginFlag, String compoundMiddleFlag, String compoundEndFlag){
		Map<String, Set<DictionaryEntry>> distribution = new HashMap<>(3);
		distribution.put(compoundBeginFlag, new HashSet<>());
		distribution.put(compoundMiddleFlag, new HashSet<>());
		distribution.put(compoundEndFlag, new HashSet<>());
		if(continuationFlags != null)
			for(String flag : continuationFlags){
				Set<DictionaryEntry> value = distribution.get(flag);
				if(value != null)
					value.add(this);
			}
		return distribution;
	}

	public boolean hasPartOfSpeech(String partOfSpeech){
		return hasMorphologicalField(MorphologicalTag.TAG_PART_OF_SPEECH + partOfSpeech);
	}

	private boolean hasMorphologicalField(String morphologicalField){
		return (morphologicalFields != null && ArrayUtils.contains(morphologicalFields, morphologicalField));
	}

	public void forEachMorphologicalField(Consumer<String> fun){
		if(morphologicalFields != null)
			for(String morphologicalField : morphologicalFields)
				fun.accept(morphologicalField);
	}

	public void removeAffixes(AffixParser affParser){
		Affixes affixes = separateAffixes(affParser);
		continuationFlags = affixes.getTerminalAffixes();
	}

	public List<String[]> extractAllAffixes(AffixParser affParser, boolean reverse){
		Affixes affixes = separateAffixes(affParser);
		return affixes.extractAllAffixes(reverse);
	}

	/**
	 * Separate the prefixes from the suffixes and from the terminals
	 * 
	 * @param affParser	The {@link AffixParser}
	 * @return	An object with separated flags, one for each group (prefixes, suffixes, terminals)
	 */
	private Affixes separateAffixes(AffixParser affParser) throws IllegalArgumentException{
		List<String> terminalAffixes = new ArrayList<>();
		List<String> prefixes = new ArrayList<>();
		List<String> suffixes = new ArrayList<>();
		if(continuationFlags != null)
			for(String affix : continuationFlags){
				if(affParser.isTerminalAffix(affix)){
					terminalAffixes.add(affix);
					continue;
				}

				Object rule = affParser.getData(affix);
				if(rule == null){
					if(affParser.isManagedByCompoundRule(affix))
						continue;

					String parentFlag = null;
					if(this instanceof Production){
						List<AffixEntry> appliedRules = ((Production)this).getAppliedRules();
						if(appliedRules != null && !appliedRules.isEmpty())
							parentFlag = appliedRules.get(0).getFlag();
					}
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

	public String toString(FlagParsingStrategy strategy){
		StringBuilder sb = new StringBuilder(word);
		if(continuationFlags != null && continuationFlags.length > 0){
			sb.append(SLASH);
			if(strategy != null)
				sb.append(strategy.joinFlags(continuationFlags));
			else
				sb.append(StringUtils.join(continuationFlags, ","));
		}
		if(morphologicalFields != null && morphologicalFields.length > 0)
			sb.append("\t").append(StringUtils.join(morphologicalFields, " "));
		return sb.toString();
	}

	@Override
	public boolean equals(Object obj){
		if(obj == null)
			return false;
		if(obj == this)
			return true;
		if(obj.getClass() != getClass())
			return false;

		DictionaryEntry rhs = (DictionaryEntry)obj;
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
