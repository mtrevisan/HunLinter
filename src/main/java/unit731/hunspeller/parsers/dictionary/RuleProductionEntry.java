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
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.interfaces.Productable;


@Getter
@EqualsAndHashCode(of = {"word", "ruleFlags", "dataFields"})
public class RuleProductionEntry implements Productable{

	private final String word;
	private final String[] ruleFlags;
	private final String[] dataFields;
	@Setter private List<AffixEntry> rules;
	private final boolean combineable;


	public RuleProductionEntry(DictionaryEntry dicEntry, Set<String> ruleFlags, boolean combineable){
		this(dicEntry.getWord(), ruleFlags, null, dicEntry.getDataFields(), combineable);
	}

	public RuleProductionEntry(String word, Set<String> otherRuleFlags, String[] currentContinuationClasses, String[] dataFields, boolean combineable){
		Objects.requireNonNull(word);
		Objects.requireNonNull(otherRuleFlags);
		Objects.requireNonNull(currentContinuationClasses);
		Objects.requireNonNull(dataFields);

		String[] newContinuationClasses = mergeContinuationClasses(otherRuleFlags, currentContinuationClasses);

		this.word = word;
		ruleFlags = newContinuationClasses;
		this.dataFields = dataFields;
		rules = new ArrayList<>();
		this.combineable = combineable;
	}

	public boolean containsRuleFlag(String ruleFlag){
		return Arrays.stream(ruleFlags)
			.anyMatch(flag -> flag.equals(ruleFlag));
	}

	public boolean containsDataField(String dataField){
		return Arrays.stream(dataFields)
			.anyMatch(field -> field.equals(dataField));
	}

	/** Merge previous unproductive continuation classes with the continuation classes of the current rule */
	private String[] mergeContinuationClasses(Set<String> otherRuleFlags, String[] currentContinuationClasses){
		HashSet<String> newContinuationClasses = new HashSet<>(otherRuleFlags);
		if(currentContinuationClasses != null)
			newContinuationClasses.addAll(Arrays.asList(currentContinuationClasses));
		return newContinuationClasses.toArray(new String[0]);
	}

	public String[] getSignificantDataFields(){
		return (dataFields != null? Arrays.stream(dataFields)
			.sorted()
			.filter(df -> !df.startsWith(WordGenerator.TAG_PHONETIC) && !df.startsWith(WordGenerator.TAG_STEM) && !df.startsWith(WordGenerator.TAG_ALLOMORPH))
			.collect(Collectors.toList())
			.toArray(new String[0]): null);
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
		StringJoiner sj = (new StringJoiner(StringUtils.SPACE))
			.add(word);
		String[] significantDataFields = getSignificantDataFields();
		if(significantDataFields != null && significantDataFields.length > 0)
			sj.add(String.join(StringUtils.SPACE, significantDataFields));
		return sj.toString();
	}

}
