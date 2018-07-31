package unit731.hunspeller.parsers.dictionary.valueobjects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.interfaces.Productable;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;


@Getter
@EqualsAndHashCode(of = {"word", "continuationFlags", "morphologicalFields"})
public class RuleProductionEntry implements Productable{

	@Setter
	private String word;
	private final String[] continuationFlags;
	private final String[] morphologicalFields;
	private List<AffixEntry> appliedRules;
	private final boolean combineable;

	private final FlagParsingStrategy strategy;


	public RuleProductionEntry(Productable productable, FlagParsingStrategy strategy){
		Objects.requireNonNull(productable);

		word = productable.getWord();
		continuationFlags = productable.getContinuationFlags();
		morphologicalFields = productable.getMorphologicalFields();
		combineable = true;

		this.strategy = strategy;
	}

	public RuleProductionEntry(String word, String[] originalMorphologicalFields, AffixEntry appliedEntry, Set<String> remainingContinuationFlags,
			boolean combineable, FlagParsingStrategy strategy){
		Objects.requireNonNull(word);
		Objects.requireNonNull(appliedEntry);

		this.word = word;
		continuationFlags = combineContinuationFlags(appliedEntry.getContinuationFlags(), remainingContinuationFlags);
		this.morphologicalFields = combineMorphologicalFields(originalMorphologicalFields, appliedEntry.getMorphologicalFields());
		appliedRules = new ArrayList<>(3);
		appliedRules.add(appliedEntry);
		this.combineable = combineable;

		this.strategy = strategy;
	}

	/** NOTE: used for testing purposes */
	public RuleProductionEntry(String word, String continuationFlags, FlagParsingStrategy strategy){
		this.word = word;
		this.continuationFlags = strategy.parseFlags(continuationFlags);
		morphologicalFields = new String[0];
		combineable = false;

		this.strategy = strategy;
	}

	private String[] combineContinuationFlags(String[] continuationFlags1, Set<String> continuationFlags2){
		Set<String> flags = new HashSet<>();
		if(continuationFlags1 != null)
			flags.addAll(Arrays.asList(continuationFlags1));
		if(continuationFlags2 != null && !continuationFlags2.isEmpty())
			flags.addAll(continuationFlags2);
		return flags.toArray(new String[flags.size()]);
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
		return newMorphologicalFields.toArray(new String[0]);
	}

	public int getContinuationFlagsCount(){
		return (continuationFlags != null? continuationFlags.length: 0);
	}

	public boolean hasContinuationFlags(){
		return (getContinuationFlagsCount() > 0);
	}

	@Override
	public boolean containsContinuationFlag(String ... continuationFlags){
		if(this.continuationFlags != null)
			for(String flag : continuationFlags)
				if(ArrayUtils.contains(this.continuationFlags, flag))
					return true;
		return false;
	}

	public boolean hasMorphologicalFields(){
		return (morphologicalFields != null && morphologicalFields.length > 0);
	}

	@Override
	public boolean containsMorphologicalField(String morphologicalField){
		return (morphologicalFields != null && ArrayUtils.contains(morphologicalFields, morphologicalField));
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

	public boolean hasProductionRule(String continuationFlag){
		return (appliedRules != null && appliedRules.stream().map(AffixEntry::getFlag).anyMatch(flag -> flag.equals(continuationFlag)));
	}

	public boolean hasProductionRule(AffixEntry.Type type){
		return (appliedRules != null && appliedRules.stream().map(AffixEntry::getType).anyMatch(t -> t == type));
	}

	public boolean hasDoublefoldAffixRule(){
		int prefixRules = 0;
		int suffixRules = 0;
		if(appliedRules != null && appliedRules.size() > 1)
			for(AffixEntry appliedRule : appliedRules){
				if(appliedRule.getType() == AffixEntry.Type.PREFIX)
					prefixRules ++;
				else
					suffixRules ++;
			}
		return (prefixRules > 1 || suffixRules > 1);
	}

	public String getRulesSequence(){
		StringJoiner sj = new StringJoiner(" > ");
		if(appliedRules != null)
			appliedRules.stream()
				.map(AffixEntry::getFlag)
				.forEach(sj::add);
		return sj.toString();
	}

//	Comparator<String> comparator = ComparatorBuilder.getComparator("vec");
	public List<String> getSignificantMorphologicalFields(){
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
			.sorted()
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

	@Override
	public String toString(){
		return word + strategy.joinFlags(continuationFlags);
	}

	public String toStringWithSignificantMorphologicalFields(){
		StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		sj.add(word);
		List<String> significantMorphologicalFields = getSignificantMorphologicalFields();
		if(!significantMorphologicalFields.isEmpty())
			sj.add(String.join(StringUtils.SPACE, significantMorphologicalFields));
		return sj.toString();
	}

}
