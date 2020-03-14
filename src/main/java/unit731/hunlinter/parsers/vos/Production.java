package unit731.hunlinter.parsers.vos;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.enums.AffixType;
import unit731.hunlinter.parsers.enums.InflectionTag;
import unit731.hunlinter.parsers.enums.MorphologicalTag;
import unit731.hunlinter.parsers.enums.PartOfSpeechTag;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.system.JavaHelper;


public class Production extends DictionaryEntry{

	private static final MessageFormat SINGLE_POS_NOT_PRESENT = new MessageFormat("Part-of-Speech not unique, found ''{0}''");

	private static final String TAB = "\t";
	private static final String FROM = "from";
	private static final String LEADS_TO = " > ";
	private static final String POS_FIELD_PREFIX = ":";

	public static final String POS_FSA_SEPARATOR = ",";
	private static final String POS_FSA_TAG_SEPARATOR = "+";


	private AffixEntry[] appliedRules;

	private final DictionaryEntry[] compoundEntries;


	public static Production createFromCompound(final String word, final String continuationFlags,
			final DictionaryEntry[] compoundEntries, final FlagParsingStrategy strategy){
		final String[] cfs = (strategy != null? strategy.parseFlags(continuationFlags): null);
		final String[] morphologicalFields = AffixEntry.extractMorphologicalFields(compoundEntries);
		return new Production(word, cfs, morphologicalFields, true, null, compoundEntries);
	}

	public static Production createFromProduction(final String word, final AffixEntry appliedEntry, final boolean combinable){
		return new Production(word, appliedEntry.continuationFlags, appliedEntry.morphologicalFields, combinable,
			new AffixEntry[]{appliedEntry}, null);
	}

	public static Production createFromProduction(final String word, final AffixEntry appliedEntry, final DictionaryEntry dicEntry,
			final List<String> remainingContinuationFlags, final boolean combinable){
		final String[] continuationFlags = appliedEntry.combineContinuationFlags(remainingContinuationFlags);
		final String[] morphologicalFields = appliedEntry.combineMorphologicalFields(dicEntry);
		final AffixEntry[] appliedRules = new AffixEntry[]{appliedEntry};
		final DictionaryEntry[] compoundEntries = extractCompoundEntries(dicEntry);
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

	private Production(final String word, final String[] continuationFlags, final String[] morphologicalFields,
			final boolean combinable, final AffixEntry[] appliedRules, final DictionaryEntry[] compoundEntries){
		super(word, continuationFlags, morphologicalFields, combinable);

		this.appliedRules = appliedRules;
		this.compoundEntries = compoundEntries;
	}

	/* NOTE: used for testing purposes */
	public Production(final String word, final String continuationFlags, final String morphologicalFields,
			final DictionaryEntry[] compoundEntries, final FlagParsingStrategy strategy){
		super(word, (strategy != null? strategy.parseFlags(continuationFlags): null),
			(morphologicalFields != null? StringUtils.split(morphologicalFields): null), true);

		this.compoundEntries = compoundEntries;
	}

	private static DictionaryEntry[] extractCompoundEntries(final DictionaryEntry dicEntry){
		return (dicEntry instanceof Production? ((Production)dicEntry).compoundEntries: null);
	}

	@Override
	public AffixEntry[] getAppliedRules(){
		return appliedRules;
	}

	public AffixEntry getAppliedRule(final int index){
		return (appliedRules != null && index < appliedRules.length? appliedRules[index]: null);
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
		return (appliedRules != null? appliedRules[appliedRules.length - 1]: null);
	}

	public void capitalizeIfContainsFlag(final String forceCompoundUppercaseFlag){
		if(compoundEntries != null && compoundEntries.length > 0
				&& compoundEntries[compoundEntries.length - 1].hasContinuationFlag(forceCompoundUppercaseFlag))
			word = StringUtils.capitalize(word);
	}

	public boolean hasMorphologicalFields(){
		return (morphologicalFields != null && morphologicalFields.length > 0);
	}

	public void prependAppliedRules(final AffixEntry[] appliedRules){
		if(appliedRules != null)
			this.appliedRules = ArrayUtils.insert(0, (this.appliedRules != null? this.appliedRules: new AffixEntry[1]),
				appliedRules);
	}

	public boolean hasProductionRules(){
		return (appliedRules != null && appliedRules.length > 0);
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
		return (morphologicalFields != null? StringUtils.join(morphologicalFields, StringUtils.SPACE): StringUtils.EMPTY);
	}

	@Override
	public boolean isCompound(){
		return (compoundEntries != null && compoundEntries.length > 0);
	}

	public String toStringWithPartOfSpeechFields(){
		final List<String> fields = getMorphologicalFields(MorphologicalTag.TAG_PART_OF_SPEECH);
		if(!fields.isEmpty()){
			fields.sort(Comparator.naturalOrder());
			return word + POS_FIELD_PREFIX + StringUtils.join(fields, StringUtils.SPACE);
		}
		return word;
	}

	public List<String> toStringPoSFSA(){
		//extract Part-of-Speech
		final List<String> pos = getMorphologicalFields(MorphologicalTag.TAG_PART_OF_SPEECH);
		if(pos.size() != 1)
			throw new LinterException(SINGLE_POS_NOT_PRESENT.format(new Object[]{String.join(", ", pos)}));

		//extract Inflection
		final List<String> suffixInflection = getMorphologicalFields(MorphologicalTag.TAG_INFLECTIONAL_SUFFIX);
		final List<String> prefixInflection = getMorphologicalFields(MorphologicalTag.TAG_INFLECTIONAL_PREFIX);
		final List<String> inflections = new ArrayList<>(suffixInflection);
		inflections.addAll(prefixInflection);
		for(int i = 0; i < inflections.size(); i ++){
			final String code = inflections.get(i);
			final String[] tags = InflectionTag.createFromCode(code).getTags();
			inflections.set(i, StringUtils.join(tags, POS_FSA_TAG_SEPARATOR));
		}
		inflections.add(0, PartOfSpeechTag.createFromCode(pos.get(0)).getTag());

		final String suffix = POS_FSA_SEPARATOR + word + POS_FSA_SEPARATOR + StringUtils.join(inflections, POS_FSA_TAG_SEPARATOR);
		//extract stem
		final List<String> stem = getMorphologicalFields(MorphologicalTag.TAG_STEM);
		return stem.stream()
			.map(st -> st + suffix)
			.collect(Collectors.toList());
	}

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
			sj.add(Arrays.stream(appliedRules)
				.map(AffixEntry::getFlag)
				.collect(Collectors.joining(LEADS_TO)));
		}
		return sj.toString();
	}

}
