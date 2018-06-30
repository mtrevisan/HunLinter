package unit731.hunspeller.parsers.dictionary;

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
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;


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
	RuleProductionEntry(String word, String continuationFlags, FlagParsingStrategy strategy){
		this.word = word;
		this.continuationFlags = strategy.parseFlags(continuationFlags);
		morphologicalFields = new String[0];
		combineable = false;

		this.strategy = strategy;
	}

	private String[] combineContinuationFlags(String[] continuationFlags1, Set<String> continuationFlags2){
		Set<String> flags = new HashSet<>();
		if(Objects.nonNull(continuationFlags1))
			flags.addAll(Arrays.asList(continuationFlags1));
		if(Objects.nonNull(continuationFlags2) && !continuationFlags2.isEmpty())
			flags.addAll(continuationFlags2);
		return flags.toArray(new String[flags.size()]);
	}

	private String[] combineMorphologicalFields(String[] morphologicalFields, String[] affixEntryMorphologicalFields){
		List<String> newMorphologicalFields = new ArrayList<>();
		//Derivational Suffix: stemming doesn't remove derivational suffixes (morphological generation depends on the order of the suffix fields)
		//Inflectional Suffix: all inflectional suffixes are removed by stemming (morphological generation depends on the order of the suffix fields)
		//Terminal Suffix: inflectional suffix fields "removed" by additional (not terminal) suffixes, useful for zero morphemes and affixes
		//	removed by splitting rules
		if(Objects.nonNull(morphologicalFields))
			for(String morphologicalField : morphologicalFields)
				if(!morphologicalField.startsWith(WordGenerator.TAG_INFLECTIONAL_SUFFIX) && !morphologicalField.startsWith(WordGenerator.TAG_INFLECTIONAL_PREFIX)
						&& (!morphologicalField.startsWith(WordGenerator.TAG_PART_OF_SPEECH) || Objects.isNull(affixEntryMorphologicalFields)
							|| !Arrays.stream(affixEntryMorphologicalFields).anyMatch(field -> field.startsWith(WordGenerator.TAG_PART_OF_SPEECH)))
						&& (!morphologicalField.startsWith(WordGenerator.TAG_TERMINAL_SUFFIX) || Objects.isNull(affixEntryMorphologicalFields)
							|| !Arrays.stream(affixEntryMorphologicalFields).allMatch(field -> !field.startsWith(WordGenerator.TAG_TERMINAL_SUFFIX))))
					newMorphologicalFields.add(morphologicalField);
		if(Objects.nonNull(affixEntryMorphologicalFields))
			newMorphologicalFields.addAll(Arrays.asList(affixEntryMorphologicalFields));
		return newMorphologicalFields.toArray(new String[0]);
	}

	public boolean hasContinuationFlags(){
		return (Objects.nonNull(continuationFlags) && continuationFlags.length > 0);
	}

	@Override
	public boolean containsContinuationFlag(String ... continuationFlags){
		if(Objects.nonNull(this.continuationFlags))
			for(String flag : continuationFlags)
				if(ArrayUtils.contains(this.continuationFlags, flag))
					return true;
		return false;
	}

	public boolean hasMorphologicalFields(){
		return (Objects.nonNull(morphologicalFields) && morphologicalFields.length > 0);
	}

	@Override
	public boolean containsMorphologicalField(String morphologicalField){
		return (Objects.nonNull(morphologicalFields) && ArrayUtils.contains(morphologicalFields, morphologicalField));
	}

	public void prependAppliedRules(List<AffixEntry> appliedRules){
		if(Objects.nonNull(appliedRules)){
			this.appliedRules = ObjectUtils.defaultIfNull(this.appliedRules, new ArrayList<>(3));
			this.appliedRules.addAll(0, appliedRules);
		}
	}

	public boolean hasProductionRules(){
		return (Objects.nonNull(appliedRules) && !appliedRules.isEmpty());
	}

	public boolean hasProductionRule(String continuationFlag){
		return (Objects.nonNull(appliedRules) && appliedRules.stream().map(AffixEntry::getFlag).anyMatch(flag -> flag.equals(continuationFlag)));
	}

	public boolean hasProductionRule(AffixEntry.Type type){
		return (Objects.nonNull(appliedRules) && appliedRules.stream().map(AffixEntry::getType).anyMatch(t -> t == type));
	}

	public boolean hasDoublefoldAffixRule(){
		int prefixRules = 0;
		int suffixRules = 0;
		if(Objects.nonNull(appliedRules) && appliedRules.size() > 1)
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
		if(Objects.nonNull(appliedRules))
			appliedRules.stream()
				.map(AffixEntry::getFlag)
				.forEach(sj::add);
		return sj.toString();
	}

//	Comparator<String> comparator = ComparatorBuilder.getComparator("vec");
	public List<String> getSignificantMorphologicalFields(){
//		List<String> significant = new ArrayList<>();
//		if(Objects.nonNull(morphologicalFields))
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
//		if(Objects.nonNull(morphologicalFields) && morphologicalFields.length > 0)
//			sj.add("\t")
//				.add(String.join(StringUtils.SPACE, morphologicalFields));
//		if(Objects.nonNull(rules) && rules.size() > 0)
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
