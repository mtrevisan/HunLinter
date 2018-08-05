package unit731.hunspeller.parsers.dictionary.valueobjects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.dtos.Affixes;
import unit731.hunspeller.services.PatternService;


public class DictionaryEntry{

	private static final int PARAM_WORD = 1;
	private static final int PARAM_FLAGS = 2;
	private static final int PARAM_MORPHOLOGICAL_FIELDS = 3;
	private static final Matcher ENTRY_PATTERN = PatternService.matcher("^(?<word>[^\\t\\s\\/]+)(?:\\/(?<flags>[^\\t\\s]+))?(?:[\\t\\s]+(?<morphologicalFields>.+))?$");


	@Getter
	@Setter
	protected String word;
	protected final String[] continuationFlags;
	protected final String[] morphologicalFields;
	@Getter
	private final boolean combineable;


	public DictionaryEntry(String line, FlagParsingStrategy strategy){
		Objects.requireNonNull(line);
		Objects.requireNonNull(strategy);

		Matcher m = ENTRY_PATTERN.reset(line);
		if(!m.find())
			throw new IllegalArgumentException("Cannot parse dictionary line " + line);

		word = m.group(PARAM_WORD);
		String dicFlags = m.group(PARAM_FLAGS);
		continuationFlags = strategy.parseFlags(dicFlags);
		String dicMorphologicalFields = m.group(PARAM_MORPHOLOGICAL_FIELDS);
		morphologicalFields = (dicMorphologicalFields != null? StringUtils.split(dicMorphologicalFields): new String[0]);
		combineable = true;
	}

	protected DictionaryEntry(DictionaryEntry productable, FlagParsingStrategy strategy){
		Objects.requireNonNull(productable);

		word = productable.getWord();
		continuationFlags = productable.continuationFlags;
		morphologicalFields = productable.morphologicalFields;
		combineable = true;
	}

	protected DictionaryEntry(String word, AffixEntry appliedEntry, DictionaryEntry productable, Set<String> remainingContinuationFlags,
			boolean combineable, FlagParsingStrategy strategy){
		Objects.requireNonNull(word);
		Objects.requireNonNull(appliedEntry);

		this.word = word;
		continuationFlags = appliedEntry.combineContinuationFlags(remainingContinuationFlags);
		this.morphologicalFields = combineMorphologicalFields(productable.morphologicalFields, appliedEntry.getMorphologicalFields());
		this.combineable = combineable;
	}

	/** NOTE: used for testing purposes */
	protected DictionaryEntry(DictionaryEntry productable, String continuationFlags, FlagParsingStrategy strategy){
		Objects.requireNonNull(productable);
		Objects.requireNonNull(continuationFlags);
		Objects.requireNonNull(strategy);

		word = productable.getWord();
		this.continuationFlags = strategy.parseFlags(continuationFlags);
		morphologicalFields = productable.morphologicalFields;
		combineable = productable.isCombineable();
	}

	public void forEachMorphologicalField(Consumer<String> fun){
		if(morphologicalFields != null)
			for(String morphologicalField : morphologicalFields)
				fun.accept(morphologicalField);
	}

	private String[] combineMorphologicalFields(String[] morphologicalFields, String[] affixEntryMorphologicalFields){
		List<String> newMorphologicalFields = new ArrayList<>();
		//Derivational Suffix: stemming doesn't remove derivational suffixes (morphological generation depends on the order of the suffix fields)
		//Inflectional Suffix: all inflectional suffixes are removed by stemming (morphological generation depends on the order of the suffix fields)
		//Terminal Suffix: inflectional suffix fields "removed" by additional (not terminal) suffixes, useful for zero morphemes and affixes
		//	removed by splitting rules
		if(morphologicalFields != null)
			for(String morphologicalField : morphologicalFields)
				if(!morphologicalField.startsWith(WordGenerator.TAG_INFLECTIONAL_SUFFIX) && !morphologicalField.startsWith(WordGenerator.TAG_INFLECTIONAL_PREFIX)
						&& (!morphologicalField.startsWith(WordGenerator.TAG_PART_OF_SPEECH) || affixEntryMorphologicalFields == null
							|| !Arrays.stream(affixEntryMorphologicalFields).anyMatch(field -> field.startsWith(WordGenerator.TAG_PART_OF_SPEECH)))
						&& (!morphologicalField.startsWith(WordGenerator.TAG_TERMINAL_SUFFIX) || affixEntryMorphologicalFields == null
							|| !Arrays.stream(affixEntryMorphologicalFields).allMatch(field -> !field.startsWith(WordGenerator.TAG_TERMINAL_SUFFIX))))
					newMorphologicalFields.add(morphologicalField);
		if(affixEntryMorphologicalFields != null)
			newMorphologicalFields.addAll(Arrays.asList(affixEntryMorphologicalFields));
		return newMorphologicalFields.toArray(new String[newMorphologicalFields.size()]);
	}

	/** NOTE: used for testing purposes */
	protected DictionaryEntry(String word, String continuationFlags, FlagParsingStrategy strategy){
		this.word = word;
		this.continuationFlags = strategy.parseFlags(continuationFlags);
		morphologicalFields = new String[0];
		combineable = false;
	}

	public List<String> getPrefixes(Function<String, RuleEntry> ruleEntryExtractor){
		return Arrays.stream(continuationFlags)
			.filter(rf -> {
				RuleEntry r = ruleEntryExtractor.apply(rf);
				return (r != null && !r.isSuffix());
			})
			.collect(Collectors.toList());
	}

	public boolean containsContinuationFlag(String ... continuationFlags){
		if(this.continuationFlags != null)
			for(String flag : this.continuationFlags)
				if(ArrayUtils.contains(continuationFlags, flag))
					return true;
		return false;
	}

	public String getContinuationFlags(){
		return Arrays.stream(continuationFlags)
			.collect(Collectors.joining(", "));
	}

	public boolean containsMorphologicalField(String morphologicalField){
		return (morphologicalFields != null && ArrayUtils.contains(morphologicalFields, morphologicalField));
	}

	public String getMorphologicalFieldPrefixedBy(String typePrefix){
		if(morphologicalFields != null)
			for(String field : morphologicalFields)
				if(field.startsWith(typePrefix))
					return field;
		return null;
	}

	public boolean isPartOfSpeech(String partOfSpeech){
		return containsMorphologicalField(WordGenerator.TAG_PART_OF_SPEECH + partOfSpeech);
	}

	/**
	 * Separate the prefixes from the suffixes
	 * 
	 * @param affParser	The {@link AffixParser}
	 * @return	An object with separated flags, one for each group
	 */
	public Affixes separateAffixes(AffixParser affParser) throws IllegalArgumentException{
		Set<String> terminalAffixes = new HashSet<>();
		Set<String> prefixes = new HashSet<>();
		Set<String> suffixes = new HashSet<>();
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
						if(!appliedRules.isEmpty())
							parentFlag = appliedRules.get(0).getFlag();
					}
					throw new IllegalArgumentException("Non–existent rule " + affix + " found" + (parentFlag != null? " via " + parentFlag: StringUtils.EMPTY));
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

		return new Affixes(terminalAffixes, prefixes, suffixes);
	}

	@Override
	public String toString(){
		return word + AffixEntry.SLASH + StringUtils.join(continuationFlags, ", ");
	}

}
