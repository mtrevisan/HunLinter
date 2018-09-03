package unit731.hunspeller.parsers.dictionary.valueobjects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.dtos.MorphologicalTag;


@Getter
@EqualsAndHashCode(of = {}, callSuper = true)
public class Production extends DictionaryEntry{

	private List<AffixEntry> appliedRules;

	private final List<DictionaryEntry> compoundEntries;


	/* Clone constructor */
	public Production(String word, DictionaryEntry dicEntry, String continuationFlagToRemove){
		super(word, dicEntry, continuationFlagToRemove);

		compoundEntries = extractCompoundEntries(dicEntry);
	}

	public Production(String word, AffixEntry appliedEntry, DictionaryEntry dicEntry, String[] remainingContinuationFlags,
			boolean combineable, FlagParsingStrategy strategy){
		super(word, appliedEntry.combineContinuationFlags(remainingContinuationFlags), appliedEntry.combineMorphologicalFields(dicEntry),
			combineable);

		appliedRules = new ArrayList<>(3);
		appliedRules.add(appliedEntry);

		compoundEntries = extractCompoundEntries(dicEntry);
	}

	private List<DictionaryEntry> extractCompoundEntries(DictionaryEntry dicEntry){
		List<DictionaryEntry> entries = (dicEntry instanceof Production? ((Production)dicEntry).compoundEntries: null);
		return (entries != null? new ArrayList<>(entries): null);
	}

	public Production(String word, String continuationFlags, List<DictionaryEntry> compoundEntries, FlagParsingStrategy strategy){
		super(word, (strategy != null? strategy.parseFlags(continuationFlags): null), AffixEntry.extractMorphologicalFields(compoundEntries), true);

		this.compoundEntries = compoundEntries;
	}

	/** NOTE: used for testing purposes */
	public Production(String word, String continuationFlags, String morphologicalFields, List<DictionaryEntry> compoundEntries, FlagParsingStrategy strategy){
		super(word, (strategy != null? strategy.parseFlags(continuationFlags): null), (morphologicalFields != null? StringUtils.split(morphologicalFields): null), true);

		this.compoundEntries = compoundEntries;
	}

	public boolean hasMorphologicalFields(){
		return (morphologicalFields != null && morphologicalFields.length > 0);
	}

	public void prependAppliedRules(List<AffixEntry> appliedRules){
		if(appliedRules != null){
			this.appliedRules = ObjectUtils.defaultIfNull(this.appliedRules, new ArrayList<>(3));
			this.appliedRules.addAll(0, appliedRules);
		}
	}

	public boolean hasProductionRules(){
		return (appliedRules != null && !appliedRules.isEmpty());
	}

//	public boolean hasProductionRule(String continuationFlag){
//		return (appliedRules != null && appliedRules.stream().map(AffixEntry::getFlag).anyMatch(flag -> flag.equals(continuationFlag)));
//	}
//
//	public boolean hasProductionRule(AffixEntry.Type type){
//		return (appliedRules != null && appliedRules.stream().map(AffixEntry::getType).anyMatch(t -> t == type));
//	}

	public boolean isTwofolded(){
		boolean twofolded = false;
		if(hasProductionRules()){
			int suffixes = 0;
			int prefixes = 0;
			for(AffixEntry appliedRule : appliedRules){
				if(appliedRule.isSuffix())
					suffixes ++;
				else
					prefixes ++;

				if(suffixes > 1 || prefixes > 1){
					twofolded = true;
					break;
				}
			}
		}
		return twofolded;
	}

	public String getRulesSequence(){
		return (appliedRules != null? appliedRules.stream()
			.map(AffixEntry::getFlag)
			.collect(Collectors.joining(" > ")):
			StringUtils.EMPTY);
	}

	public String getMorphologicalFields(){
		return (morphologicalFields != null? String.join(StringUtils.SPACE, morphologicalFields): StringUtils.EMPTY);
	}

	@Override
	public boolean isCompound(){
		return (compoundEntries != null && !compoundEntries.isEmpty());
	}

	public String toStringWithPartOfSpeechFields(){
		StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		sj.add(word);
		String partOfSpeechFields = getPartOfSpeechFields();
		if(StringUtils.isNotBlank(partOfSpeechFields))
			sj.add(partOfSpeechFields);
		return sj.toString();
	}

	private String getPartOfSpeechFields(){
		return Arrays.stream(morphologicalFields)
			.filter(df -> df.startsWith(MorphologicalTag.TAG_PART_OF_SPEECH))
			.sorted()
			.collect(Collectors.joining(StringUtils.SPACE));
	}

	@Override
	public String toString(){
		return toString(null);
	}

	@Override
	public String toString(FlagParsingStrategy strategy){
		StringJoiner sj = new StringJoiner("\t");
		sj.add(super.toString(strategy));
		if(hasProductionRules()){
			sj.add(" from ");
			sj.add(appliedRules.stream()
				.map(AffixEntry::getFlag)
				.collect(Collectors.joining(" > ")));
		}
		return sj.toString();
	}

}
