package unit731.hunspeller.parsers.dictionary.vos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.dtos.MorphologicalTag;


public class Production extends DictionaryEntry{

	private static final String TAB = "\t";
	private static final String FROM = "from";
	private static final String LEADS_TO = " > ";
	private static final String POS_OPEN_BRACKET = "[";
	private static final String POS_CLOSE_BRACKET = "]";


	private List<AffixEntry> appliedRules;

	private final List<DictionaryEntry> compoundEntries;


	public static Production createFromCompound(String word, String continuationFlags, List<DictionaryEntry> compoundEntries,
			FlagParsingStrategy strategy){
		String[] cfs = (strategy != null? strategy.parseFlags(continuationFlags): null);
		String[] morphologicalFields = AffixEntry.extractMorphologicalFields(compoundEntries);
		boolean combineable = true;
		List<AffixEntry> appliedRules = null;
		return new Production(word, cfs, morphologicalFields, combineable,
			appliedRules, compoundEntries);
	}

	public static Production createFromProduction(String word, AffixEntry appliedEntry, DictionaryEntry dicEntry,
			String[] remainingContinuationFlags, boolean combineable){
		String[] continuationFlags = appliedEntry.combineContinuationFlags(remainingContinuationFlags);
		String[] morphologicalFields = appliedEntry.combineMorphologicalFields(dicEntry);
		List<AffixEntry> appliedRules = new ArrayList<>(3);
		appliedRules.add(appliedEntry);
		List<DictionaryEntry> compoundEntries = extractCompoundEntries(dicEntry);
		return new Production(word, continuationFlags, morphologicalFields, combineable,
			appliedRules, compoundEntries);
	}

	public static Production clone(DictionaryEntry dicEntry){
		return new Production(dicEntry);
	}

	private Production(DictionaryEntry dicEntry){
		super(dicEntry);

		compoundEntries = extractCompoundEntries(dicEntry);
	}

	private Production(String word, String[] continuationFlags, String[] morphologicalFields, boolean combineable,
			List<AffixEntry> appliedRules, List<DictionaryEntry> compoundEntries){
		super(word, continuationFlags, morphologicalFields, combineable);

		this.appliedRules = appliedRules;
		this.compoundEntries = compoundEntries;
	}

	/* NOTE: used for testing purposes */
	public Production(String word, String continuationFlags, String morphologicalFields, List<DictionaryEntry> compoundEntries,
			FlagParsingStrategy strategy){
		super(word, (strategy != null? strategy.parseFlags(continuationFlags): null),
			(morphologicalFields != null? StringUtils.split(morphologicalFields): null), true);

		this.compoundEntries = compoundEntries;
	}

	private static List<DictionaryEntry> extractCompoundEntries(DictionaryEntry dicEntry){
		List<DictionaryEntry> entries = (dicEntry instanceof Production? ((Production)dicEntry).compoundEntries: null);
		return (entries != null? new ArrayList<>(entries): null);
	}

	@Override
	public List<AffixEntry> getAppliedRules(){
		return appliedRules;
	}

	@Override
	public AffixEntry getLastAppliedRule(){
		return (hasProductionRules()? appliedRules.get(appliedRules.size() - 1): null);
	}

	public List<DictionaryEntry> getCompoundEntries(){
		return compoundEntries;
	}

	public void capitalizeIfContainsFlag(String forceCompoundUppercaseFlag){
		if(compoundEntries != null && !compoundEntries.isEmpty()
				&& compoundEntries.get(compoundEntries.size() - 1).hasContinuationFlag(forceCompoundUppercaseFlag))
			word = StringUtils.capitalize(word);
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
			.collect(Collectors.joining(LEADS_TO)):
			StringUtils.EMPTY);
	}

	public String getMorphologicalFields(){
		return (morphologicalFields != null? String.join(StringUtils.SPACE, morphologicalFields): StringUtils.EMPTY);
	}

	public List<String> getMorphologicalFields(String morphologicalTag){
		int purgeTag = morphologicalTag.length();
		return Arrays.stream(morphologicalFields != null? morphologicalFields: new String[0])
			.filter(df -> df.startsWith(morphologicalTag))
			.map(df -> df.substring(purgeTag))
			.collect(Collectors.toList());
	}

	@Override
	public boolean isCompound(){
		return (compoundEntries != null && !compoundEntries.isEmpty());
	}

	public String toStringWithPartOfSpeechFields(){
		StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		sj.add(word);
		List<String> fields = getMorphologicalFields(MorphologicalTag.TAG_PART_OF_SPEECH);
		if(!fields.isEmpty())
			sj.add(POS_OPEN_BRACKET + String.join(StringUtils.SPACE, fields) + POS_CLOSE_BRACKET);
		return sj.toString();
	}

	public void applyOutputConversionTable(AffixData affixData){
		word = affixData.applyOutputConversionTable(word);
	}

	@Override
	public String toString(){
		StringJoiner sj = new StringJoiner(TAB);
		sj.add(super.toString());
		if(hasProductionRules()){
			sj.add(FROM);
			sj.add(appliedRules.stream()
				.map(AffixEntry::getFlag)
				.collect(Collectors.joining(LEADS_TO)));
		}
		return sj.toString();
	}

	@Override
	public String toString(FlagParsingStrategy strategy){
		Objects.requireNonNull(strategy);

		StringJoiner sj = new StringJoiner(TAB);
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
