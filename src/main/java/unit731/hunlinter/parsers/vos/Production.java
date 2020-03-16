package unit731.hunlinter.parsers.vos;

import java.util.Arrays;
import java.util.Comparator;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

	private static final String SINGLE_POS_NOT_PRESENT = "Part-of-Speech not unique";

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
			final String[] remainingContinuationFlags, final boolean combinable){
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

	public boolean isTwofolded(final String circumfixFlag){
		if(appliedRules != null){
			//find last applied rule with circumfix flag
			int startIndex = appliedRules.length - 1;
			while(startIndex >= 0)
				if(appliedRules[startIndex --].hasContinuationFlag(circumfixFlag))
					break;

			final long[] suffixesAffixesCount = new long[2];
			IntStream.range(startIndex + 1, appliedRules.length)
				.forEach(idx -> suffixesAffixesCount[appliedRules[idx].isSuffix()? 1: 0] ++);
			return (suffixesAffixesCount[0] > 0 && suffixesAffixesCount[1] > 0);
		}
		return false;
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
		final String[] pos = getMorphologicalFieldPartOfSpeech();
		if(pos.length > 0){
			Arrays.sort(pos, Comparator.naturalOrder());
			return word + POS_FIELD_PREFIX + StringUtils.join(pos, StringUtils.SPACE);
		}
		return word;
	}

	public String[] toStringPoSFSA(){
		//extract Part-of-Speech
		final String[] pos = getMorphologicalFields(MorphologicalTag.TAG_PART_OF_SPEECH);
		if(pos.length != 1)
			throw new LinterException(SINGLE_POS_NOT_PRESENT);

		//extract Inflection
		final String[] suffixInflection = getMorphologicalFields(MorphologicalTag.TAG_INFLECTIONAL_SUFFIX);
		final String[] prefixInflection = getMorphologicalFields(MorphologicalTag.TAG_INFLECTIONAL_PREFIX);
		final String[] inflections = new String[1 + suffixInflection.length + prefixInflection.length];
		inflections[0] = PartOfSpeechTag.createFromCode(pos[0]).getTag();
		System.arraycopy(suffixInflection, 0, inflections, 1, suffixInflection.length);
		System.arraycopy(prefixInflection, 0, inflections, 1 + suffixInflection.length, prefixInflection.length);
		for(int i = 1; i < inflections.length; i ++){
			final String code = inflections[i];
			final String[] tags = InflectionTag.createFromCode(code).getTags();
			inflections[i] = StringUtils.join(tags, POS_FSA_TAG_SEPARATOR);
		}

		final String suffix = POS_FSA_SEPARATOR + word + POS_FSA_SEPARATOR + StringUtils.join(inflections, POS_FSA_TAG_SEPARATOR);
		//extract stem
		final String[] stem = getMorphologicalFields(MorphologicalTag.TAG_STEM);
		for(int i = 0; i < stem.length; i ++)
			stem[i] += suffix;
		return stem;
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
			final StringJoiner subsj = new StringJoiner(LEADS_TO);
			for(final AffixEntry appliedRule : appliedRules)
				subsj.add(appliedRule.getFlag());
			sj.add(subsj.toString());
		}
		return sj.toString();
	}

}
