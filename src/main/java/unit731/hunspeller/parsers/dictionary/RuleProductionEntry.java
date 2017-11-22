package unit731.hunspeller.parsers.dictionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.interfaces.Productable;
import unit731.hunspeller.languages.builders.ComparatorBuilder;
import unit731.hunspeller.services.externalsorter.ExternalSorterOptions;


@Getter
@EqualsAndHashCode(of = {"word", "ruleFlags", "dataFields"})
public class RuleProductionEntry implements Productable{

	private final String word;
	private final String[] ruleFlags;
	private final String[] dataFields;
	private List<AffixEntry> appliedRules;
	private final boolean combineable;


	public RuleProductionEntry(Productable productable){
		Objects.requireNonNull(productable);

		word = productable.getWord();
		ruleFlags = productable.getRuleFlags();
		dataFields = productable.getDataFields();
		combineable = false;
	}

	public RuleProductionEntry(String word, String[] originalDataFields, AffixEntry entry, boolean combineable){
		Objects.requireNonNull(word);
		Objects.requireNonNull(entry);

		this.word = word;
		ruleFlags = entry.getRuleFlags();
		this.dataFields = combineDataFields(originalDataFields, entry.getDataFields());
		appliedRules = new ArrayList<>();
		appliedRules.add(entry);
		this.combineable = combineable;
	}

	private String[] combineDataFields(String[] dataFields, String[] affixEntryDataFields){
		List<String> newDataFields = new ArrayList<>();
		//Derivational Suffix: stemming doesn't remove derivational suffixes (morphological generation depends on the order of the suffix fields)
		//Inflectional Suffix: all inflectional suffixes are removed by stemming (morphological generation depends on the order of the suffix fields)
		//Terminal Suffix: inflectional suffix fields "removed" by additional (not terminal) suffixes, useful for zero morphemes and affixes
		//	removed by splitting rules
		if(dataFields != null)
			for(String dataField : dataFields)
				if(!dataField.startsWith(WordGenerator.TAG_INFLECTIONAL_SUFFIX) && !dataField.startsWith(WordGenerator.TAG_INFLECTIONAL_PREFIX)
						&& (!dataField.startsWith(WordGenerator.TAG_PART_OF_SPEECH) || affixEntryDataFields == null
							|| !Arrays.stream(affixEntryDataFields).anyMatch(field -> field.startsWith(WordGenerator.TAG_PART_OF_SPEECH)))
						&& (!dataField.startsWith(WordGenerator.TAG_TERMINAL_SUFFIX) || affixEntryDataFields == null
							|| !Arrays.stream(affixEntryDataFields).allMatch(field -> !field.startsWith(WordGenerator.TAG_TERMINAL_SUFFIX))))
					newDataFields.add(dataField);
		if(affixEntryDataFields != null)
			newDataFields.addAll(Arrays.asList(affixEntryDataFields));
		return newDataFields.toArray(new String[0]);
	}

	public boolean hasRuleFlags(){
		return (ruleFlags != null && ruleFlags.length > 0);
	}

	@Override
	public boolean containsRuleFlag(String ruleFlag){
		if(ruleFlags != null)
			for(String flag : ruleFlags)
				if(flag.equals(ruleFlag))
					return true;
		return false;
	}

	public boolean hasDataFields(){
		return (dataFields != null && dataFields.length > 0);
	}

	@Override
	public boolean containsDataField(String dataField){
		if(dataFields != null)
			for(String field : dataFields)
				if(field.equals(dataField))
					return true;
		return false;
	}

	public boolean hasProductionRule(String ruleFlag){
		if(appliedRules != null)
			for(AffixEntry appliedRule : appliedRules)
				if(appliedRule.getFlag().equals(ruleFlag))
					return true;
		return false;
	}

	public boolean hasProductionRule(AffixEntry.TYPE type){
		if(appliedRules != null)
			for(AffixEntry appliedRule : appliedRules)
				if(appliedRule.getType() == type)
					return true;
		return false;
	}

	public String getRulesSequence(){
		return (appliedRules != null? appliedRules.stream()
			.map(AffixEntry::getFlag)
			.collect(Collectors.joining(" > ")): StringUtils.EMPTY);
	}

//	Comparator<String> comparator = ComparatorBuilder.getComparator("vec");
	public String[] getSignificantDataFields(){
//		List<String> significant = new ArrayList<>();
//		if(dataFields != null)
//			for(String dataField : dataFields)
//				if(!dataField.startsWith(WordGenerator.TAG_PHONETIC) && !dataField.startsWith(WordGenerator.TAG_STEM)
//						&& !dataField.startsWith(WordGenerator.TAG_ALLOMORPH))
//					significant.add(dataField);
//		Collections.sort(significant, comparator);
//		return significant.toArray(new String[0]);
		return Arrays.stream(dataFields)
			.filter(df -> !df.startsWith(WordGenerator.TAG_PHONETIC) && !df.startsWith(WordGenerator.TAG_STEM)
				&& !df.startsWith(WordGenerator.TAG_ALLOMORPH))
			.sorted()
			.collect(Collectors.toList())
			.toArray(new String[0]);
	}

//	@Override
//	public String toString(){
//		StringJoiner sj = (new StringJoiner(StringUtils.EMPTY))
//			.add(word)
//			.add(AffixEntry.joinRuleFlags(ruleFlags, flag));
//		if(dataFields != null && dataFields.length > 0)
//			sj.add("\t")
//				.add(String.join(StringUtils.SPACE, dataFields));
//		if(rules != null && rules.size() > 0)
//			sj.add(" from ")
//				.add(String.join(" > ", rules));
//		return sj.toString();
//	}

	public String toStringWithSignificantDataFields(){
		StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		sj.add(word);
		String[] significantDataFields = getSignificantDataFields();
		if(significantDataFields.length > 0)
			sj.add(String.join(StringUtils.SPACE, significantDataFields));
		return sj.toString();
	}

}
