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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.interfaces.Productable;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;


@Getter
@EqualsAndHashCode(of = {"word", "ruleFlags", "dataFields"})
public class RuleProductionEntry implements Productable{

	private final String word;
	private final String[] ruleFlags;
	private final String[] dataFields;
	private List<AffixEntry> appliedRules;
	private final boolean combineable;

	private FlagParsingStrategy strategy;


	public RuleProductionEntry(Productable productable, FlagParsingStrategy strategy){
		Objects.requireNonNull(productable);

		word = productable.getWord();
		ruleFlags = productable.getRuleFlags();
		dataFields = productable.getDataFields();
		combineable = true;

		this.strategy = strategy;
	}

	public RuleProductionEntry(String word, String[] originalDataFields, AffixEntry appliedEntry, Set<String> remainingRuleFlags, boolean combineable, FlagParsingStrategy strategy){
		Objects.requireNonNull(word);
		Objects.requireNonNull(appliedEntry);

		this.word = word;
		ruleFlags = combineRuleFlags(appliedEntry.getRuleFlags(), remainingRuleFlags);
		this.dataFields = combineDataFields(originalDataFields, appliedEntry.getDataFields());
		appliedRules = new ArrayList<>(3);
		appliedRules.add(appliedEntry);
		this.combineable = combineable;

		this.strategy = strategy;
	}

	/** NOTE: used for testing purposes */
	RuleProductionEntry(String word, String ruleFlags, FlagParsingStrategy strategy){
		this.word = word;
		this.ruleFlags = strategy.parseRuleFlags(ruleFlags);
		dataFields = new String[0];
		combineable = false;
	}

	private String[] combineRuleFlags(String[] ruleFlags1, Set<String> ruleFlags2){
		Set<String> flags = new HashSet<>();
		if(Objects.nonNull(ruleFlags1))
			flags.addAll(Arrays.asList(ruleFlags1));
		if(Objects.nonNull(ruleFlags2) && !ruleFlags2.isEmpty())
			flags.addAll(ruleFlags2);
		return flags.toArray(new String[flags.size()]);
	}

	private String[] combineDataFields(String[] dataFields, String[] affixEntryDataFields){
		List<String> newDataFields = new ArrayList<>();
		//Derivational Suffix: stemming doesn't remove derivational suffixes (morphological generation depends on the order of the suffix fields)
		//Inflectional Suffix: all inflectional suffixes are removed by stemming (morphological generation depends on the order of the suffix fields)
		//Terminal Suffix: inflectional suffix fields "removed" by additional (not terminal) suffixes, useful for zero morphemes and affixes
		//	removed by splitting rules
		if(Objects.nonNull(dataFields))
			for(String dataField : dataFields)
				if(!dataField.startsWith(WordGenerator.TAG_INFLECTIONAL_SUFFIX) && !dataField.startsWith(WordGenerator.TAG_INFLECTIONAL_PREFIX)
						&& (!dataField.startsWith(WordGenerator.TAG_PART_OF_SPEECH) || Objects.isNull(affixEntryDataFields)
							|| !Arrays.stream(affixEntryDataFields).anyMatch(field -> field.startsWith(WordGenerator.TAG_PART_OF_SPEECH)))
						&& (!dataField.startsWith(WordGenerator.TAG_TERMINAL_SUFFIX) || Objects.isNull(affixEntryDataFields)
							|| !Arrays.stream(affixEntryDataFields).allMatch(field -> !field.startsWith(WordGenerator.TAG_TERMINAL_SUFFIX))))
					newDataFields.add(dataField);
		if(Objects.nonNull(affixEntryDataFields))
			newDataFields.addAll(Arrays.asList(affixEntryDataFields));
		return newDataFields.toArray(new String[0]);
	}

	public boolean hasRuleFlags(){
		return (Objects.nonNull(ruleFlags) && ruleFlags.length > 0);
	}

	@Override
	public boolean containsRuleFlag(String ... ruleFlags){
		if(Objects.nonNull(this.ruleFlags))
			for(String flag : ruleFlags)
				if(ArrayUtils.contains(this.ruleFlags, flag))
					return true;
		return false;
	}

	public boolean hasDataFields(){
		return (Objects.nonNull(dataFields) && dataFields.length > 0);
	}

	@Override
	public boolean containsDataField(String dataField){
		return (Objects.nonNull(dataFields) && ArrayUtils.contains(dataFields, dataField));
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

	public boolean hasProductionRule(String ruleFlag){
		return (Objects.nonNull(appliedRules) && appliedRules.stream().map(AffixEntry::getFlag).anyMatch(flag -> flag.equals(ruleFlag)));
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
	public List<String> getSignificantDataFields(){
//		List<String> significant = new ArrayList<>();
//		if(Objects.nonNull(dataFields))
//			for(String dataField : dataFields)
//				if(!dataField.startsWith(WordGenerator.TAG_PHONETIC) && !dataField.startsWith(WordGenerator.TAG_STEM)
//						&& !dataField.startsWith(WordGenerator.TAG_ALLOMORPH))
//					significant.add(dataField);
//		Collections.sort(significant, comparator);
//		return significant.toArray(new String[0]);
		return Arrays.stream(dataFields)
			.filter(df -> df.startsWith(WordGenerator.TAG_PART_OF_SPEECH))
			.sorted()
			.collect(Collectors.toList());
	}

//	@Override
//	public String toString(){
//		StringJoiner sj = (new StringJoiner(StringUtils.EMPTY))
//			.add(word)
//			.add(AffixEntry.joinRuleFlags(ruleFlags, flag));
//		if(Objects.nonNull(dataFields) && dataFields.length > 0)
//			sj.add("\t")
//				.add(String.join(StringUtils.SPACE, dataFields));
//		if(Objects.nonNull(rules) && rules.size() > 0)
//			sj.add(" from ")
//				.add(String.join(" > ", rules));
//		return sj.toString();
//	}

	@Override
	public String toString(){
		return word + strategy.joinRuleFlags(ruleFlags);
	}

	public String toStringWithSignificantDataFields(){
		StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		sj.add(word);
		List<String> significantDataFields = getSignificantDataFields();
		if(!significantDataFields.isEmpty())
			sj.add(String.join(StringUtils.SPACE, significantDataFields));
		return sj.toString();
	}

}
