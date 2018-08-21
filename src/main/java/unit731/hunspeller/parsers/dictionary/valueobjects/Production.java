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
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;


@Getter
@EqualsAndHashCode(of = {}, callSuper = true)
public class Production extends DictionaryEntry{

	private List<AffixEntry> appliedRules;

	private final List<DictionaryEntry> compoundEntries;


	public Production(DictionaryEntry productable, FlagParsingStrategy strategy){
		super(productable, strategy);

		compoundEntries = null;
	}

	public Production(String word, AffixEntry appliedEntry, DictionaryEntry productable, String[] remainingContinuationFlags,
			boolean combineable, FlagParsingStrategy strategy){
		super(word, appliedEntry, productable, remainingContinuationFlags, combineable, strategy);

		appliedRules = new ArrayList<>(3);
		appliedRules.add(appliedEntry);

		compoundEntries = null;
	}

	public Production(String word, List<DictionaryEntry> compoundEntries){
		super(word, compoundEntries);

		this.compoundEntries = compoundEntries;
	}

	/** NOTE: used for testing purposes */
	public Production(String word, String continuationFlags, String morphologicalFields, FlagParsingStrategy strategy){
		super(word, continuationFlags, morphologicalFields, strategy);

		compoundEntries = null;
	}

	public int getContinuationFlagsCount(AffixParser affParser){
		int continuationFlagsCount = 0;
		if(continuationFlags != null){
			continuationFlagsCount = continuationFlags.length;
			for(String flag : continuationFlags)
				if(affParser.isTerminalAffix(flag))
					continuationFlagsCount --;
		}
		return continuationFlagsCount;
	}

	public boolean hasContinuationFlags(AffixParser affParser){
		return (getContinuationFlagsCount(affParser) > 0);
	}

	public void clearContinuationFlags(){
		continuationFlags = null;
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

	public String getRulesSequence(){
		StringJoiner sj = new StringJoiner(" > ");
		if(appliedRules != null)
			appliedRules.stream()
				.map(AffixEntry::getFlag)
				.forEach(sj::add);
		return sj.toString();
	}

	public String getMorphologicalFields(){
		return (morphologicalFields != null? String.join(StringUtils.SPACE, morphologicalFields): StringUtils.EMPTY);
	}

	public String[] getCompoundPrefixes(AffixParser affParser){
		String[] prefixes = null;
		if(compoundEntries != null){
			DictionaryEntry entry = compoundEntries.get(0);
			prefixes = entry.extractAffixes(affParser, false).get(0);
		}
		return prefixes;
	}

	public String[] getCompoundSuffixes(AffixParser affParser){
		String[] suffixes = null;
		if(compoundEntries != null){
			DictionaryEntry entry = compoundEntries.get(compoundEntries.size() - 1);
			suffixes = entry.extractAffixes(affParser, false).get(1);
		}
		return suffixes;
	}

	public String toStringWithSignificantMorphologicalFields(){
		StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		sj.add(word);
		List<String> significantMorphologicalFields = getSignificantMorphologicalFields();
		if(!significantMorphologicalFields.isEmpty())
			sj.add(String.join(StringUtils.SPACE, significantMorphologicalFields));
		return sj.toString();
	}

//	Comparator<String> comparator = ComparatorBuilder.getComparator("vec");
	private List<String> getSignificantMorphologicalFields(){
//		List<String> significant = new ArrayList<>();
//		if(morphologicalFields != null)
//			for(String morphologicalField : morphologicalFields)
//				if(!morphologicalField.startsWith(WordGenerator.TAG_PHONETIC) && !morphologicalField.startsWith(WordGenerator.TAG_STEM)
//						&& !morphologicalField.startsWith(WordGenerator.TAG_ALLOMORPH))
//					significant.add(morphologicalField);
//		Collections.sort(significant, comparator);
//		return significant.toArray(new String[0]);
		return Arrays.stream(morphologicalFields)
			.filter(df -> df.startsWith(WordGenerator.TAG_PART_OF_SPEECH))
			.collect(Collectors.toList());
	}

//	@Override
//	public String toString(){
//		StringJoiner sj = (new StringJoiner(StringUtils.EMPTY))
//			.add(word)
//			.add(AffixEntry.joinFlags(continuationFlags, flag));
//		if(morphologicalFields != null && morphologicalFields.length > 0)
//			sj.add("\t")
//				.add(String.join(StringUtils.SPACE, morphologicalFields));
//		if(rules != null && rules.size() > 0)
//			sj.add(" from ")
//				.add(String.join(" > ", rules));
//		return sj.toString();
//	}

}
