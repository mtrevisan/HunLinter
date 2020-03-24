package unit731.hunlinter.parsers.vos;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.enums.AffixType;
import unit731.hunlinter.parsers.enums.InflectionTag;
import unit731.hunlinter.parsers.enums.MorphologicalTag;
import unit731.hunlinter.parsers.enums.PartOfSpeechTag;
import unit731.hunlinter.services.SetHelper;
import unit731.hunlinter.workers.exceptions.LinterException;

import static unit731.hunlinter.services.system.LoopHelper.forEach;


public class Inflection extends DictionaryEntry{

	private static final String SINGLE_POS_NOT_PRESENT = "Part-of-Speech not unique";

	private static final String TAB = "\t";
	private static final String FROM = "from";
	private static final String LEADS_TO = " > ";
	private static final String POS_FIELD_PREFIX = ":";

	public static final String POS_FSA_SEPARATOR = ",";
	private static final String POS_FSA_TAG_SEPARATOR = "+";


	private AffixEntry[] appliedRules;

	private final DictionaryEntry[] compoundEntries;


	public static Inflection createFromCompound(final String word, final String continuationFlags,
			final DictionaryEntry[] compoundEntries, final FlagParsingStrategy strategy){
		final String[] cfs = (strategy != null? strategy.parseFlags(continuationFlags): null);
		final String[] morphologicalFields = AffixEntry.extractMorphologicalFields(compoundEntries);
		return new Inflection(word, cfs, morphologicalFields, true, null, compoundEntries);
	}

	public static Inflection createFromInflection(final String word, final AffixEntry appliedEntry, final boolean combinable){
		return new Inflection(word, appliedEntry.continuationFlags, appliedEntry.morphologicalFields, combinable,
			new AffixEntry[]{appliedEntry}, null);
	}

	public static Inflection createFromInflection(final String word, final AffixEntry appliedEntry,
			final DictionaryEntry dicEntry, final String[] remainingContinuationFlags, final boolean combinable){
		final String[] continuationFlags = appliedEntry.combineContinuationFlags(remainingContinuationFlags);
		final String[] morphologicalFields = appliedEntry.combineMorphologicalFields(dicEntry);
		final AffixEntry[] appliedRules = new AffixEntry[]{appliedEntry};
		final DictionaryEntry[] compoundEntries = extractCompoundEntries(dicEntry);
		return new Inflection(word, continuationFlags, morphologicalFields, combinable,
			appliedRules, compoundEntries);
	}

	public static Inflection clone(final DictionaryEntry dicEntry){
		return new Inflection(dicEntry);
	}

	private Inflection(final DictionaryEntry dicEntry){
		super(dicEntry);

		compoundEntries = extractCompoundEntries(dicEntry);
	}

	private Inflection(final String word, final String[] continuationFlags, final String[] morphologicalFields,
			final boolean combinable, final AffixEntry[] appliedRules, final DictionaryEntry[] compoundEntries){
		super(word, continuationFlags, morphologicalFields, combinable);

		this.appliedRules = appliedRules;
		this.compoundEntries = compoundEntries;
	}

	/* NOTE: used for testing purposes */
	public Inflection(final String word, final String continuationFlags, final String morphologicalFields,
			final DictionaryEntry[] compoundEntries, final FlagParsingStrategy strategy){
		super(word, (strategy != null? strategy.parseFlags(continuationFlags): null),
			(morphologicalFields != null? StringUtils.split(morphologicalFields): null), true);

		this.compoundEntries = compoundEntries;
	}

	private static DictionaryEntry[] extractCompoundEntries(final DictionaryEntry dicEntry){
		return (dicEntry instanceof Inflection? ((Inflection)dicEntry).compoundEntries: null);
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
		AffixEntry result = null;
		if(appliedRules != null)
			for(final AffixEntry rule : appliedRules)
				result = rule;
		return result;
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

	public boolean hasInflectionRules(){
		return (appliedRules != null && appliedRules.length > 0);
	}

//	public boolean hasInflectionRule(final String continuationFlag){
//		return (appliedRules != null && appliedRules.stream().map(AffixEntry::getFlag).anyMatch(flag -> flag.equals(continuationFlag)));
//	}

//	public boolean hasInflectionRule(final AffixEntry.Type type){
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
			for(int idx = startIndex + 1; idx < appliedRules.length; idx ++)
				suffixesAffixesCount[appliedRules[idx].getType() == AffixType.SUFFIX? 1: 0] ++;
			return (suffixesAffixesCount[0] > 0 && suffixesAffixesCount[1] > 0);
		}
		return false;
	}

	public String getRulesSequence(){
		final StringJoiner sj = new StringJoiner(LEADS_TO);
		forEach(appliedRules, rule -> sj.add(rule.getFlag()));
		return sj.toString();
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

	public List<String> toStringPoSFSA(){
		//subdivide morphologicalFields into PART_OF_SPEECH, INFLECTIONAL_SUFFIX, INFLECTIONAL_PREFIX, and STEM
		final Map<MorphologicalTag, List<String>> bucket = extractMorphologicalTags();

		//extract Part-of-Speech
		final List<String> pos = bucket.get(MorphologicalTag.PART_OF_SPEECH);
		if(pos.size() != 1)
			throw new LinterException(SINGLE_POS_NOT_PRESENT);

		//extract Inflection
		final List<String> suffixInflection = bucket.get(MorphologicalTag.INFLECTIONAL_SUFFIX);
		final List<String> prefixInflection = bucket.get(MorphologicalTag.INFLECTIONAL_PREFIX);

		final StringJoiner sj = new StringJoiner(POS_FSA_TAG_SEPARATOR);
		sj.add(PartOfSpeechTag.createFromCodeAndValue(pos.get(0)).getTag());
		forEach(suffixInflection,
			code -> forEach(InflectionTag.createFromCodeAndValue(code).getTags(), sj::add));
		forEach(prefixInflection,
			code -> forEach(InflectionTag.createFromCodeAndValue(code).getTags(), sj::add));

		final byte[] suffix = (POS_FSA_SEPARATOR + word + POS_FSA_SEPARATOR + sj).getBytes(StandardCharsets.UTF_8);
		//extract stem
		final List<String> stem = bucket.get(MorphologicalTag.STEM);
		for(int i = 0; i < stem.size(); i ++){
			final byte[] s = stem.get(i).getBytes(StandardCharsets.UTF_8);
			final int offset = s.length - 3;
			final byte[] ss = new byte[offset + suffix.length];
			System.arraycopy(s, 3, ss, 0, offset);
			System.arraycopy(suffix, 0, ss, offset, suffix.length);
			stem.set(i, new String(ss, StandardCharsets.UTF_8));
		}
		return stem;
	}

	//NOTE: the only morphological tags really needed are: PART_OF_SPEECH, INFLECTIONAL_SUFFIX, INFLECTIONAL_PREFIX, and STEM
	private Map<MorphologicalTag, List<String>> extractMorphologicalTags(){
		return SetHelper.bucket(morphologicalFields,
			mf -> MorphologicalTag.createFromCode(mf), MorphologicalTag.class);
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
		if(hasInflectionRules()){
			sj.add(FROM);
			final StringJoiner subsj = new StringJoiner(LEADS_TO);
			for(final AffixEntry appliedRule : appliedRules)
				subsj.add(appliedRule.getFlag());
			sj.add(subsj.toString());
		}
		return sj.toString();
	}

}
