package unit731.hunlinter.parsers.vos;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.enums.AffixOption;
import unit731.hunlinter.parsers.enums.AffixType;
import unit731.hunlinter.parsers.enums.MorphologicalTag;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.RegexHelper;

import static unit731.hunlinter.services.system.LoopHelper.applyIf;
import static unit731.hunlinter.services.system.LoopHelper.forEach;
import static unit731.hunlinter.services.system.LoopHelper.match;


public class DictionaryEntry{

	private static final MessageFormat WRONG_FORMAT = new MessageFormat("Cannot parse dictionary line ''{0}''");
	private static final MessageFormat NON_EXISTENT_RULE = new MessageFormat("Non–existent rule ''{0}''{1}");

	private static final int PARAM_WORD = 1;
	private static final int PARAM_FLAGS = 2;
	private static final int PARAM_MORPHOLOGICAL_FIELDS = 3;
	private static final Pattern PATTERN_ENTRY = RegexHelper.pattern("^(?<word>[^\\s]+?)(?:(?<!\\\\)\\/(?<flags>[^\\s]+))?(?:[\\s]+(?<morphologicalFields>.+))?$");

	private static final String SLASH = "/";
	private static final String SLASH_ESCAPED = "\\/";
	private static final String TAB = "\t";
	private static final String COMMA = ",";


	protected String word;
	protected String[] continuationFlags;
	protected final String[] morphologicalFields;
	private final boolean combinable;


	public static DictionaryEntry createFromDictionaryLine(final String line, final AffixData affixData){
		return createFromDictionaryLine(line, affixData, true);
	}

	public static DictionaryEntry createFromDictionaryLineNoStemTag(final String line, final AffixData affixData){
		return createFromDictionaryLine(line, affixData, false);
	}

	private static DictionaryEntry createFromDictionaryLine(final String line, final AffixData affixData,
			final boolean addStemTag){
		final FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();
		final List<String> aliasesFlag = affixData.getData(AffixOption.ALIASES_FLAG);
		final List<String> aliasesMorphologicalField = affixData.getData(AffixOption.ALIASES_MORPHOLOGICAL_FIELD);

		Objects.requireNonNull(line);
		Objects.requireNonNull(strategy);

		final Matcher m = PATTERN_ENTRY.matcher(line);
		if(!m.find())
			throw new LinterException(WRONG_FORMAT.format(new Object[]{line}));

		final String word = StringUtils.replace(m.group(PARAM_WORD), SLASH_ESCAPED, SLASH);
		final String[] continuationFlags = strategy.parseFlags(expandAliases(m.group(PARAM_FLAGS), aliasesFlag));
		final String dicMorphologicalFields = m.group(PARAM_MORPHOLOGICAL_FIELDS);
		final String[] mfs = StringUtils.split(expandAliases(dicMorphologicalFields, aliasesMorphologicalField));
		final String[] morphologicalFields = (!addStemTag || containsStem(mfs)? mfs:
			ArrayUtils.addAll(new String[]{MorphologicalTag.STEM.attachValue(word)}, mfs));
		final boolean combinable = true;
		final String convertedWord = affixData.applyInputConversionTable(word);
		return new DictionaryEntry(convertedWord, continuationFlags, morphologicalFields, combinable);
	}

	protected DictionaryEntry(final DictionaryEntry dicEntry){
		Objects.requireNonNull(dicEntry);

		word = dicEntry.word;
		continuationFlags = dicEntry.continuationFlags;
		morphologicalFields = dicEntry.morphologicalFields;
		combinable = dicEntry.combinable;
	}

	protected DictionaryEntry(final String word, final String[] continuationFlags, final String[] morphologicalFields,
			final boolean combinable){
		Objects.requireNonNull(word);

		this.word = word;
		this.continuationFlags = continuationFlags;
		this.morphologicalFields = morphologicalFields;
		this.combinable = combinable;
	}

	private static String expandAliases(final String part, final List<String> aliases){
		return (aliases != null && !aliases.isEmpty() && NumberUtils.isCreatable(part)?
			aliases.get(Integer.parseInt(part) - 1): part);
	}

	private static boolean containsStem(final String[] mfs){
		return (match(mfs, mf -> mf.startsWith(MorphologicalTag.STEM.getCode())) != null);
	}

//	public static String extractWord(final String line){
//		Objects.requireNonNull(line);
//
//		final Matcher m = PATTERN_ENTRY.matcher(line);
//		if(!m.find())
//			throw new HunLintException("Cannot parse dictionary line '" + line + "'");
//
//		return StringUtils.replace(m.group(PARAM_WORD), SLASH_ESCAPED, SLASH);
//	}

	public String getWord(){
		return word;
	}

	public boolean isCombinable(){
		return combinable;
	}

	public boolean removeContinuationFlag(final String continuationFlagToRemove){
		boolean removed = false;
		if(continuationFlagToRemove != null && continuationFlags != null){
			final int previousSize = continuationFlags.length;
			continuationFlags = ArrayUtils.removeElement(ArrayUtils.clone(continuationFlags), continuationFlagToRemove);

			removed = (continuationFlags.length != previousSize);

			if(continuationFlags.length == 0)
				continuationFlags = null;
		}
		return removed;
	}

	/**
	 * @param isTerminalAffix	The method used to determine if a flag is a terminal
	 * @return	Whether there are continuation flags that are not terminal affixes
	 */
	public boolean hasNonTerminalContinuationFlags(final Function<String, Boolean> isTerminalAffix){
		return (match(continuationFlags, Predicate.not(isTerminalAffix::apply)) != null);
	}

	public int getContinuationFlagCount(){
		return (continuationFlags != null? continuationFlags.length: 0);
	}

	public boolean hasContinuationFlag(final String flag){
		return (continuationFlags != null && ArrayUtils.contains(continuationFlags, flag));
	}

	public boolean hasContinuationFlags(final String[] flags){
		if(continuationFlags != null && flags != null){
			final Set<String> list = new HashSet<>(Arrays.asList(continuationFlags));
			return (match(flags, Predicate.not(list::add)) != null);
		}
		return false;
	}

	public AffixEntry[] getAppliedRules(){
		return new AffixEntry[0];
	}

	/**
	 * Get last applied rule of type {@code type}
	 *
	 * @param type    The type used to filter the last applied rule
	 * @return    The last applied rule of the specified type
	 */
	public AffixEntry getLastAppliedRule(final AffixType type){
		return null;
	}

	/**
	 * Get last applied rule
	 *
	 * @return    The last applied rule of the specified type
	 */
	public AffixEntry getLastAppliedRule(){
		return null;
	}

	public Map<String, List<DictionaryEntry>> distributeByCompoundRule(final AffixData affixData){
		final Map<String, List<DictionaryEntry>> result = new HashMap<>();
		applyIf(continuationFlags,
			affixData::isManagedByCompoundRule,
			flag -> result.computeIfAbsent(flag, k -> new ArrayList<>(1)).add(this));
		return result;
	}

	public Map<String, List<DictionaryEntry>> distributeByCompoundBeginMiddleEnd(final String compoundBeginFlag,
			final String compoundMiddleFlag, final String compoundEndFlag){
		final Map<String, List<DictionaryEntry>> distribution = new HashMap<>(3);
		distribution.put(compoundBeginFlag, new ArrayList<>());
		distribution.put(compoundMiddleFlag, new ArrayList<>());
		distribution.put(compoundEndFlag, new ArrayList<>());
		forEach(continuationFlags, flag -> {
			final List<DictionaryEntry> entries = distribution.get(flag);
			if(entries != null)
				entries.add(this);
		});
		return distribution;
	}

	public boolean hasPartOfSpeech(){
		return (match(morphologicalFields, MorphologicalTag.PART_OF_SPEECH::isSupertypeOf) != null);
	}

	/**
	 * @param partOfSpeech	Part–of–Speech WITH the MorphologicalTag.PART_OF_SPEECH prefix
	 * @return	Whether this entry has the given Part–of–Speech
	 */
	public boolean hasPartOfSpeech(final String partOfSpeech){
		return hasMorphologicalField(partOfSpeech);
	}

	private boolean hasMorphologicalField(final String morphologicalField){
		return (morphologicalFields != null && ArrayUtils.contains(morphologicalFields, morphologicalField));
	}

	public String[] getMorphologicalFieldPartOfSpeech(){
		if(morphologicalFields == null)
			return new String[0];

		final String tag = MorphologicalTag.PART_OF_SPEECH.getCode();
		final List<String> list = new ArrayList<>(morphologicalFields.length);
		applyIf(morphologicalFields,
			mf -> mf.startsWith(tag),
			list::add);
		return list.toArray(String[]::new);
	}

	public void forEachMorphologicalField(final Consumer<String> fun){
		forEach(morphologicalFields, fun);
	}

	/**
	 * @param affixData	Affix data
	 * @param reverse	Whether the complex prefixes is used
	 * @return	A list of prefixes, suffixes, and terminal affixes (the first two may be exchanged if
	 * 			COMPLEXPREFIXES is defined)
	 */
	public String[][] extractAllAffixes(final AffixData affixData, final boolean reverse){
		final Affixes affixes = separateAffixes(affixData);
		return affixes.extractAllAffixes(reverse);
	}

	/**
	 * Separate the prefixes from the suffixes and from the terminals
	 *
	 * @param affixData	The {@link AffixData}
	 * @return	An object with separated flags, one for each group (prefixes, suffixes, terminals)
	 */
	private Affixes separateAffixes(final AffixData affixData){
		final int maxSize = (continuationFlags != null? continuationFlags.length: 0);
		String[] terminals = new String[maxSize];
		String[] prefixes = new String[maxSize];
		String[] suffixes = new String[maxSize];
		if(continuationFlags != null){
			int indexTerminal = 0;
			int indexPrefix = 0;
			int indexSuffix = 0;
			for(final String affix : continuationFlags){
				if(affixData.isTerminalAffix(affix)){
					terminals[indexTerminal ++] = affix;
					continue;
				}

				final Object rule = affixData.getData(affix);
				if(rule == null){
					if(affixData.isManagedByCompoundRule(affix))
						continue;

					final AffixEntry[] appliedRules = getAppliedRules();
					final String parentFlag = (appliedRules != null && appliedRules.length > 0? appliedRules[0].getFlag(): null);
					throw new LinterException(NON_EXISTENT_RULE.format(new Object[]{affix,
						(parentFlag != null? " via " + parentFlag: StringUtils.EMPTY)}));
				}

				if(rule instanceof RuleEntry){
					if(((RuleEntry) rule).isSuffix())
						suffixes[indexSuffix ++] = affix;
					else
						prefixes[indexPrefix ++] = affix;
				}
				else
					terminals[indexTerminal ++] = affix;
			}

			//trim arrays
			terminals = Arrays.copyOf(terminals, indexTerminal);
			prefixes = Arrays.copyOf(prefixes, indexPrefix);
			suffixes = Arrays.copyOf(suffixes, indexSuffix);
		}

		return new Affixes(prefixes, suffixes, terminals);
	}

	public boolean isCompound(){
		return false;
	}

	@Override
	public String toString(){
		return toString(null);
	}

	public String toString(final FlagParsingStrategy strategy){
		final StringBuffer sb = new StringBuffer(word);
		if(continuationFlags != null && continuationFlags.length > 0){
			sb.append(SLASH);
			sb.append(strategy != null? strategy.joinFlags(continuationFlags): StringUtils.join(continuationFlags, COMMA));
		}
		if(morphologicalFields != null && morphologicalFields.length > 0)
			sb.append(TAB).append(StringUtils.join(morphologicalFields, StringUtils.SPACE));
		return sb.toString();
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final DictionaryEntry rhs = (DictionaryEntry)obj;
		return new EqualsBuilder()
			.append(word, rhs.word)
			.append(continuationFlags, rhs.continuationFlags)
			.append(morphologicalFields, rhs.morphologicalFields)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(word)
			.append(continuationFlags)
			.append(morphologicalFields)
			.toHashCode();
	}

}
