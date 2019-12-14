package unit731.hunspeller.parsers.vos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.enums.AffixType;
import unit731.hunspeller.parsers.enums.MorphologicalTag;
import unit731.hunspeller.services.JavaHelper;
import unit731.hunspeller.services.StringHelper;


public class Production extends DictionaryEntry{

	private static final String TAB = "\t";
	private static final String FROM = "from";
	private static final String LEADS_TO = " > ";
	private static final String POS_FIELD_PREFIX = ":";


	private List<AffixEntry> appliedRules;

	private final List<DictionaryEntry> compoundEntries;


	public static Production createFromCompound(final String word, final String continuationFlags,
			final List<DictionaryEntry> compoundEntries, final FlagParsingStrategy strategy){
		final String[] cfs = (strategy != null? strategy.parseFlags(continuationFlags): null);
		final String[] morphologicalFields = AffixEntry.extractMorphologicalFields(compoundEntries);
		return new Production(word, cfs, morphologicalFields, true, null, compoundEntries);
	}

	public static Production createFromProduction(final String word, final AffixEntry appliedEntry, final boolean combinable){
		return new Production(word, appliedEntry.continuationFlags, appliedEntry.morphologicalFields, combinable,
			Collections.singletonList(appliedEntry), null);
	}

	public static Production createFromProduction(final String word, final AffixEntry appliedEntry, final DictionaryEntry dicEntry,
			final String[] remainingContinuationFlags, final boolean combinable){
		final String[] continuationFlags = appliedEntry.combineContinuationFlags(remainingContinuationFlags);
		final String[] morphologicalFields = appliedEntry.combineMorphologicalFields(dicEntry);
		final List<AffixEntry> appliedRules = new ArrayList<>(3);
		appliedRules.add(appliedEntry);
		final List<DictionaryEntry> compoundEntries = extractCompoundEntries(dicEntry);
		return new Production(word, continuationFlags, morphologicalFields, combinable,
			appliedRules, compoundEntries);
	}

	public static Production clone(final DictionaryEntry dicEntry){
		return new Production(dicEntry);
	}

	private Production(final DictionaryEntry dicEntry){
		super(dicEntry);

		compoundEntries = extractCompoundEntries(dicEntry);
	}

	private Production(final String word, final String[] continuationFlags, final String[] morphologicalFields, final boolean combinable,
			final List<AffixEntry> appliedRules, final List<DictionaryEntry> compoundEntries){
		super(word, continuationFlags, morphologicalFields, combinable);

		this.appliedRules = appliedRules;
		this.compoundEntries = compoundEntries;
	}

	/* NOTE: used for testing purposes */
	public Production(final String word, final String continuationFlags, final String morphologicalFields,
			final List<DictionaryEntry> compoundEntries, final FlagParsingStrategy strategy){
		super(word, (strategy != null? strategy.parseFlags(continuationFlags): null),
			(morphologicalFields != null? StringUtils.split(morphologicalFields): null), true);

		this.compoundEntries = compoundEntries;
	}

	private static List<DictionaryEntry> extractCompoundEntries(final DictionaryEntry dicEntry){
		final List<DictionaryEntry> entries = (dicEntry instanceof Production? ((Production)dicEntry).compoundEntries: null);
		return (entries != null? new ArrayList<>(entries): null);
	}

	@Override
	public List<AffixEntry> getAppliedRules(){
		return appliedRules;
	}

	public AffixEntry getAppliedRule(final int index){
		return (appliedRules != null && index < appliedRules.size()? appliedRules.get(index): null);
	}

	@Override
	public AffixEntry getLastAppliedRule(final AffixType type){
		return JavaHelper.nullableToStream(appliedRules)
			.filter(rule -> rule.getType() == type)
			.reduce((first, second) -> second)
			.orElse(null);
	}

	@Override
	public AffixEntry getLastAppliedRule(){
		return JavaHelper.nullableToStream(appliedRules)
			.reduce((first, second) -> second)
			.orElse(null);
	}

	public void capitalizeIfContainsFlag(final String forceCompoundUppercaseFlag){
		if(compoundEntries != null && !compoundEntries.isEmpty()
				&& compoundEntries.get(compoundEntries.size() - 1).hasContinuationFlag(forceCompoundUppercaseFlag))
			word = StringUtils.capitalize(word);
	}

	public boolean hasMorphologicalFields(){
		return (morphologicalFields != null && morphologicalFields.length > 0);
	}

	public void prependAppliedRules(final List<AffixEntry> appliedRules){
		if(appliedRules != null){
			this.appliedRules = ObjectUtils.defaultIfNull(this.appliedRules, new ArrayList<>(3));
			this.appliedRules.addAll(0, appliedRules);
		}
	}

	public boolean hasProductionRules(){
		return (appliedRules != null && !appliedRules.isEmpty());
	}

//	public boolean hasProductionRule(final String continuationFlag){
//		return (appliedRules != null && appliedRules.stream().map(AffixEntry::getFlag).anyMatch(flag -> flag.equals(continuationFlag)));
//	}

//	public boolean hasProductionRule(final AffixEntry.Type type){
//		return (appliedRules != null && appliedRules.stream().map(AffixEntry::getType).anyMatch(t -> t == type));
//	}

	public boolean isTwofolded(){
		final Long affixesMaxCount = JavaHelper.nullableToStream(appliedRules)
			.map(AffixEntry::isSuffix)
			.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
			.values().stream()
			.max(Comparator.naturalOrder())
			.orElse(0L);
		return (affixesMaxCount > 1);
	}

	public String getRulesSequence(){
		return JavaHelper.nullableToStream(appliedRules)
			.map(AffixEntry::getFlag)
			.collect(Collectors.joining(LEADS_TO));
	}

	public String getMorphologicalFields(){
		return (morphologicalFields != null? StringHelper.join(StringUtils.SPACE, morphologicalFields): StringUtils.EMPTY);
	}

	@Override
	public boolean isCompound(){
		return (compoundEntries != null && !compoundEntries.isEmpty());
	}

	public String toStringWithPartOfSpeechFields(){
		final List<String> fields = getMorphologicalFields(MorphologicalTag.TAG_PART_OF_SPEECH);
		if(!fields.isEmpty()){
			fields.sort(Comparator.naturalOrder());
			return word + POS_FIELD_PREFIX + StringHelper.join(StringUtils.SPACE, fields);
		}
		return word;
	}

//	public List<String> toStringWithPartOfSpeechFields(){
//		final List<String> fields = getMorphologicalFields(MorphologicalTag.TAG_PART_OF_SPEECH);
//		if(!fields.isEmpty())
//			return fields.stream()
//				.map(field -> word + POS_FIELD_PREFIX + field)
//				.collect(Collectors.toList());
//		else
//			return Collections.singletonList(word);
//	}

	public void applyOutputConversionTable(final Function<String, String> outputConversionTable){
		word = outputConversionTable.apply(word);
	}

	@Override
	public String toString(){
		return toString(null);
	}

	@Override
	public String toString(final FlagParsingStrategy strategy){
		final StringJoiner sj = new StringJoiner(TAB);
		sj.add(super.toString(strategy));
		if(hasProductionRules()){
			sj.add(FROM);
			sj.add(appliedRules.stream()
				.map(AffixEntry::getFlag)
				.collect(Collectors.joining(LEADS_TO)));
		}
		return sj.toString();
	}

}
