package unit731.hunlinter.parsers.vos;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.enums.AffixType;
import unit731.hunlinter.parsers.enums.MorphologicalTag;
import unit731.hunlinter.parsers.workers.exceptions.LinterException;
import unit731.hunlinter.services.system.JavaHelper;
import unit731.hunlinter.services.PatternHelper;


public class AffixEntry{

	private static final MessageFormat AFFIX_EXPECTED = new MessageFormat("Expected an affix entry, found something else{0}");
	private static final MessageFormat WRONG_FORMAT = new MessageFormat("Cannot parse affix line ''{0}''");
	private static final MessageFormat WRONG_CONDITION_END = new MessageFormat("Condition part doesn''t ends with removal part: ''{0}''");
	private static final MessageFormat WRONG_CONDITION_START = new MessageFormat("Condition part doesn''t starts with removal part: ''{0}''");
	private static final MessageFormat POS_PRESENT = new MessageFormat("Part-of-Speech detected: ''{0}''");
	//warning
	private static final MessageFormat CHARACTERS_IN_COMMON = new MessageFormat("Characters in common between removed and added part: ''{0}''");
	private static final MessageFormat CANNOT_FULL_STRIP = new MessageFormat("Cannot strip full word ''{0}'' without the FULLSTRIP option");

	private static final int PARAM_CONDITION = 1;
	private static final int PARAM_CONTINUATION_CLASSES = 2;
	private static final Pattern PATTERN_LINE = PatternHelper.pattern("^(?<condition>[^\\s]+?)(?:(?<!\\\\)\\/(?<continuationClasses>[^\\s]+))?$");

	private static final String TAB = "\t";
	private static final String SLASH = "/";
	private static final String SLASH_ESCAPED = "\\/";

	private static final String DOT = ".";
	private static final String ZERO = "0";


	private final AffixType affixType;
	/** ID used to represent the affix */
	private final String flag;
	final String[] continuationFlags;
	/** condition that must be met before the affix can be applied */
	private final Pattern condition;
	/** string to strip */
	private final String removing;
	private final int removingLength;
	/** string to append */
	private final String appending;
	private final int appendingLength;
	final String[] morphologicalFields;

	private final String entry;


	public AffixEntry(final String line, final FlagParsingStrategy strategy, final List<String> aliasesFlag,
			final List<String> aliasesMorphologicalField){
		Objects.requireNonNull(line);
		Objects.requireNonNull(strategy);

		final String[] lineParts = StringUtils.split(line, null, 6);
		if(lineParts.length < 4 || lineParts.length > 6)
			throw new LinterException(AFFIX_EXPECTED.format(new Object[]{(lineParts.length > 0? ": '" + line + "'": StringUtils.EMPTY)}));

		final String ruleType = lineParts[0];
		this.flag = lineParts[1];
		final String removal = StringUtils.replace(lineParts[2], SLASH_ESCAPED, SLASH);
		final Matcher m = PATTERN_LINE.matcher(lineParts[3]);
		if(!m.find())
			throw new LinterException(WRONG_FORMAT.format(new Object[]{line}));
		final String addition = StringUtils.replace(m.group(PARAM_CONDITION), SLASH_ESCAPED, SLASH);
		final String continuationClasses = m.group(PARAM_CONTINUATION_CLASSES);
		final String cond = (lineParts.length > 4? StringUtils.replace(lineParts[4], SLASH_ESCAPED, SLASH): DOT);
		morphologicalFields = (lineParts.length > 5? StringUtils.split(expandAliases(lineParts[5], aliasesMorphologicalField)): null);

		affixType = AffixType.createFromCode(ruleType);
		final String[] classes = strategy.parseFlags((continuationClasses != null? expandAliases(continuationClasses, aliasesFlag): null));
		continuationFlags = (classes != null && classes.length > 0? classes: null);
		final String matcherCondition = (affixType == AffixType.PREFIX? "^": StringUtils.EMPTY)
			+ cond
			+ (affixType == AffixType.SUFFIX? "$": StringUtils.EMPTY);
		condition = PatternHelper.pattern(matcherCondition);
		removing = (!ZERO.equals(removal)? removal: StringUtils.EMPTY);
		removingLength = removing.length();
		appending = (!ZERO.equals(addition)? addition: StringUtils.EMPTY);
		appendingLength = appending.length();

		if(continuationFlags != null)
			Arrays.sort(continuationFlags);

		checkValidity(cond, removal, line);


		entry = line;
	}

	private void checkValidity(final String cond, final String removal, final String line){
		if(removingLength > 0){
			if(isSuffix()){
				if(!cond.endsWith(removal))
					throw new LinterException(WRONG_CONDITION_END.format(new Object[]{line}));
				if(appending.length() > 1 && removal.charAt(0) == appending.charAt(0))
					throw new LinterException(CHARACTERS_IN_COMMON.format(new Object[]{line}));
			}
			else{
				if(!cond.startsWith(removal))
					throw new LinterException(WRONG_CONDITION_START.format(new Object[]{line}));
				if(appending.length() > 1 && removal.charAt(removal.length() - 1) == appending.charAt(appending.length() - 1))
					throw new LinterException(CHARACTERS_IN_COMMON.format(new Object[]{line}));
			}
		}
	}

	public AffixType getType(){
		return affixType;
	}

	public String getFlag(){
		return flag;
	}

	public String getAppending(){
		return appending;
	}

	private String expandAliases(final String part, final List<String> aliases){
		return (aliases != null && !aliases.isEmpty() && NumberUtils.isCreatable(part)? aliases.get(Integer.parseInt(part) - 1): part);
	}

	public boolean hasContinuationFlags(){
		return (continuationFlags != null && continuationFlags.length > 0);
	}

	public boolean hasContinuationFlag(final String flag){
		return (hasContinuationFlags() && flag != null && Arrays.binarySearch(continuationFlags, flag) >= 0);
	}

	public String[] combineContinuationFlags(final String[] otherContinuationFlags){
		final Set<String> flags = new HashSet<>();
		if(continuationFlags != null)
			flags.addAll(Arrays.asList(continuationFlags));
		if(otherContinuationFlags != null && otherContinuationFlags.length > 0)
			flags.addAll(Arrays.asList(otherContinuationFlags));
		final int size = flags.size();
		return (size > 0? flags.toArray(String[]::new): null);
	}

	//FIXME is this documentation updated/true?
	/**
	 *
	 * Derivational Suffix: stemming doesn't remove derivational suffixes (morphological generation depends on the order of the
	 * 	suffix fields)
	 * Inflectional Suffix: all inflectional suffixes are removed by stemming (morphological generation depends on the order of the
	 * 	suffix fields)
	 * Terminal Suffix: inflectional suffix fields removed by additional (not terminal) suffixes, useful for zero morphemes and
	 * 	affixes removed by splitting rules
	 *
	 * @param dicEntry	The dictionary entry to combine from
	 * @return	The list of new morphological fields
	 */
	public String[] combineMorphologicalFields(final DictionaryEntry dicEntry){
		List<String> mf = (dicEntry.morphologicalFields != null? new ArrayList<>(Arrays.asList(dicEntry.morphologicalFields)):
			new ArrayList<>());
		final List<String> amf = (morphologicalFields != null? Arrays.asList(morphologicalFields): Collections.emptyList());

		//NOTE: part of speech is NOT overwritten, both in simple application of an affix rule and of a compound rule
		final boolean containsInflectionalAffix = amf.stream()
			.anyMatch(field -> field.startsWith(MorphologicalTag.TAG_INFLECTIONAL_SUFFIX.getCode())
				|| field.startsWith(MorphologicalTag.TAG_INFLECTIONAL_PREFIX.getCode()));
		final boolean containsNonTerminalAffixes = containsInflectionalAffix;
		//remove inflectional and terminal suffixes
		mf = mf.stream()
			.filter(field -> !field.startsWith(MorphologicalTag.TAG_INFLECTIONAL_SUFFIX.getCode())
				&& !field.startsWith(MorphologicalTag.TAG_INFLECTIONAL_PREFIX.getCode())
				|| !containsInflectionalAffix)
			.filter(field -> !field.startsWith(MorphologicalTag.TAG_TERMINAL_SUFFIX.getCode()) || !containsNonTerminalAffixes)
			.collect(Collectors.toList());

		//add morphological fields from the applied affix
		mf.addAll((isSuffix()? mf.size(): 0), amf);

		return mf.toArray(String[]::new);
	}

	public static String[] extractMorphologicalFields(final List<DictionaryEntry> compoundEntries){
		final List<String[]> mf = new ArrayList<>();
		JavaHelper.nullableToStream(compoundEntries)
			.forEach(compoundEntry -> {
				final String compound = compoundEntry.getWord();
				mf.add(ArrayUtils.addAll(new String[]{MorphologicalTag.TAG_PART.attachValue(compound)}, compoundEntry.morphologicalFields));
			});
		return mf.stream()
			.flatMap(Arrays::stream)
			.toArray(String[]::new);
	}

	public final boolean isSuffix(){
		return (affixType == AffixType.SUFFIX);
	}

	public void validate(){
		final List<String> filteredFields = getMorphologicalFields(MorphologicalTag.TAG_PART_OF_SPEECH);
		if(!filteredFields.isEmpty())
			throw new LinterException(POS_PRESENT.format(new Object[]{String.join(", ", filteredFields)}));
	}

	private List<String> getMorphologicalFields(final MorphologicalTag morphologicalTag){
		final String tag = morphologicalTag.getCode();
		final int purgeTag = tag.length();
		return JavaHelper.nullableToStream(morphologicalFields)
			.filter(df -> df.startsWith(tag))
			.map(df -> df.substring(purgeTag))
			.collect(Collectors.toList());
	}

	public boolean canApplyTo(final String word){
		return PatternHelper.find(word, condition);
	}

	public boolean canInverseApplyTo(final String word){
		return (affixType == AffixType.PREFIX? word.startsWith(appending): word.endsWith(appending));
	}

	public String applyRule(final String word, final boolean isFullstrip){
		if(!isFullstrip && word.length() == removingLength)
			throw new LinterException(CANNOT_FULL_STRIP.format(new Object[]{word}));

		return (isSuffix()?
			word.substring(0, word.length() - removingLength) + appending:
			appending + word.substring(removingLength));
	}

	//NOTE: {#canInverseApplyTo} should be called to verify applicability
	public String undoRule(final String word){
		return (isSuffix()?
			word.substring(0, word.length() - appendingLength) + removing:
			removing + word.substring(appendingLength));
	}

	public String toStringWithMorphologicalFields(final FlagParsingStrategy strategy){
		Objects.requireNonNull(strategy);

		final StringBuffer sb = new StringBuffer();
		if(continuationFlags != null && continuationFlags.length > 0){
			sb.append(SLASH);
			sb.append(strategy.joinFlags(continuationFlags));
		}
		if(morphologicalFields != null && morphologicalFields.length > 0)
			sb.append(TAB).append(StringUtils.join(morphologicalFields, StringUtils.SPACE));
		return sb.toString();
	}

	@Override
	public String toString(){
		return StringUtils.replaceChars(entry, TAB, StringUtils.SPACE);
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final AffixEntry rhs = (AffixEntry)obj;
		return new EqualsBuilder()
			.append(affixType, rhs.affixType)
			.append(flag, rhs.flag)
			.append(continuationFlags, rhs.continuationFlags)
			.append(condition, rhs.condition)
			.append(removing, rhs.removing)
			.append(appending, rhs.appending)
			.append(morphologicalFields, rhs.morphologicalFields)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(affixType)
			.append(flag)
			.append(continuationFlags)
			.append(condition)
			.append(removing)
			.append(appending)
			.append(morphologicalFields)
			.toHashCode();
	}

}
