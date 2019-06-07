package unit731.hunspeller.parsers.dictionary.vos;

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
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.dtos.MorphologicalTag;
import unit731.hunspeller.services.PatternHelper;


public class AffixEntry{

	private static final int PARAM_CONDITION = 1;
	private static final int PARAM_CONTINUATION_CLASSES = 2;
	private static final Pattern PATTERN_LINE = PatternHelper.pattern("^(?<condition>[^\\s]+?)(?:(?<!\\\\)\\/(?<continuationClasses>[^\\s]+))?$");

	private static final String TAB = "\t";
	private static final String SLASH = "/";
	private static final String SLASH_ESCAPED = "\\/";
	private static final Pattern PATTERN_ENTRY = PatternHelper.pattern("\t.*$");

	public static final String DOT = ".";
	public static final String ZERO = "0";


	public enum Type{
		SUFFIX(AffixTag.SUFFIX),
		PREFIX(AffixTag.PREFIX);


		private final AffixTag tag;

		Type(final AffixTag tag){
			this.tag = tag;
		}

		public static Type createFromCode(final String code){
			return Arrays.stream(values())
				.filter(t -> t.tag.getCode().equals(code))
				.findFirst()
				.orElse(null);
		}

		public boolean is(final String flag){
			return this.tag.getCode().equals(flag);
		}

		public AffixTag getTag(){
			return tag;
		}

	}


	private final Type type;
	/** ID used to represent the affix */
	private final String flag;
	private final String[] continuationFlags;
	/** condition that must be met before the affix can be applied */
	private final AffixCondition condition;
	/** string to strip */
	private final String removing;
	private final int removingLength;
	/** string to append */
	private final String appending;
	private final int appendingLength;
	private final String[] morphologicalFields;

	private final String entry;


	public AffixEntry(final String line, final FlagParsingStrategy strategy, final List<String> aliasesFlag,
			final List<String> aliasesMorphologicalField){
		Objects.requireNonNull(line);
		Objects.requireNonNull(strategy);

		final String[] lineParts = StringUtils.split(line, null, 6);
		if(lineParts.length < 4 || lineParts.length > 6)
			throw new IllegalArgumentException("Expected an affix entry, found something else"
				+ (lineParts.length > 0? ": '" + line + "'": StringUtils.EMPTY));

		final String ruleType = lineParts[0];
		this.flag = lineParts[1];
		final String removal = StringUtils.replace(lineParts[2], SLASH_ESCAPED, SLASH);
		final Matcher m = PATTERN_LINE.matcher(lineParts[3]);
		if(!m.find())
			throw new IllegalArgumentException("Cannot parse affix line '" + line + "'");
		final String addition = StringUtils.replace(m.group(PARAM_CONDITION), SLASH_ESCAPED, SLASH);
		final String continuationClasses = m.group(PARAM_CONTINUATION_CLASSES);
		final String cond = (lineParts.length > 4? StringUtils.replace(lineParts[4], SLASH_ESCAPED, SLASH): DOT);
		morphologicalFields = (lineParts.length > 5? StringUtils.split(expandAliases(lineParts[5], aliasesMorphologicalField)): null);

		type = Type.createFromCode(ruleType);
		final String[] classes = strategy.parseFlags((continuationClasses != null? expandAliases(continuationClasses, aliasesFlag): null));
		continuationFlags = (classes != null && classes.length > 0? classes: null);
		condition = new AffixCondition(cond, type);
		removing = (!ZERO.equals(removal)? removal: StringUtils.EMPTY);
		removingLength = removing.length();
		appending = (!ZERO.equals(addition)? addition: StringUtils.EMPTY);
		appendingLength = appending.length();

		if(continuationFlags != null)
			Arrays.sort(continuationFlags);

		checkValidity(cond, removal, line);


		entry = line;
	}

	private void checkValidity(final String cond, final String removal, final String line) throws IllegalArgumentException{
		if(removingLength > 0){
			if(isSuffix()){
				if(!cond.endsWith(removal))
					throw new IllegalArgumentException("Condition part does not ends with removal part: '" + line + "'");
				if(appending.length() > 1 && removal.charAt(0) == appending.charAt(0))
					throw new IllegalArgumentException("Characters in common between removed and added part: '" + line + "'");
			}
			else{
				if(!cond.startsWith(removal))
					throw new IllegalArgumentException("Condition part does not starts with removal part: '" + line + "'");
				if(appending.length() > 1 && removal.charAt(removal.length() - 1) == appending.charAt(appending.length() - 1))
					throw new IllegalArgumentException("Characters in common between removed and added part: '" + line + "'");
			}
		}
	}

	public Type getType(){
		return type;
	}

	public String getFlag(){
		return flag;
	}

	public AffixCondition getCondition(){
		return condition;
	}

	private String expandAliases(final String part, final List<String> aliases) throws IllegalArgumentException{
		return (aliases != null && !aliases.isEmpty() && NumberUtils.isCreatable(part)? aliases.get(Integer.parseInt(part) - 1): part);
	}

	public boolean hasContinuationFlag(final String flag){
		return (continuationFlags != null && flag != null && Arrays.binarySearch(continuationFlags, flag) >= 0);
	}

	public String[] combineContinuationFlags(final String[] otherContinuationFlags){
		final Set<String> flags = new HashSet<>();
		if(continuationFlags != null)
			flags.addAll(Arrays.asList(continuationFlags));
		if(otherContinuationFlags != null && otherContinuationFlags.length > 0)
			flags.addAll(Arrays.asList(otherContinuationFlags));
		final int size = flags.size();
		return (size > 0? flags.toArray(new String[size]): null);
	}

	/**
	 * FIXME is this documentation updated/true?
	 * 
	 * Derivational Suffix: stemming doesn't remove derivational suffixes (morphological generation depends on the order of the suffix fields)
	 * Inflectional Suffix: all inflectional suffixes are removed by stemming (morphological generation depends on the order of the suffix fields)
	 * Terminal Suffix: inflectional suffix fields removed by additional (not terminal) suffixes, useful for zero morphemes and affixes
	 * 	removed by splitting rules
	 * 
	 * @param dicEntry	The dictionary entry to combine from
	 * @return	The list of new morphological fields
	 */
	public String[] combineMorphologicalFields(final DictionaryEntry dicEntry){
		List<String> mf = (dicEntry.morphologicalFields != null? new ArrayList<>(Arrays.asList(dicEntry.morphologicalFields)): new ArrayList<>());
		final List<String> amf = (morphologicalFields != null? Arrays.asList(morphologicalFields): Collections.emptyList());

//		final boolean containsPartOfSpeech = amf.stream()
//			.anyMatch(field -> field.startsWith(MorphologicalTag.TAG_PART_OF_SPEECH));
		final boolean containsTerminalSuffixes = amf.stream()
			.anyMatch(field -> field.startsWith(MorphologicalTag.TAG_TERMINAL_SUFFIX));
		//remove inflectional and terminal suffixes
		mf = mf.stream()
			.filter(field -> !field.startsWith(MorphologicalTag.TAG_INFLECTIONAL_SUFFIX))
//			.filter(field -> !field.startsWith(MorphologicalTag.TAG_PART_OF_SPEECH) || !containsPartOfSpeech)
			.filter(field -> !field.startsWith(MorphologicalTag.TAG_TERMINAL_SUFFIX) || !containsTerminalSuffixes)
			.collect(Collectors.toList());

		//find stem
/*		String stem = null;
		final Iterator<String> itr = mf.iterator();
		while(itr.hasNext()){
			final String field = itr.next();
			if(field.startsWith(MorphologicalTag.TAG_STEM)){
				stem = field;
				itr.remove();
				break;
			}
		}
		if(stem != null){
			itr = amf.iterator();
			while(itr.hasNext()){
				final String field = itr.next();
				if(field.startsWith(MorphologicalTag.TAG_STEM)){
					stem = field;
					itr.remove();
					break;
				}
			}
		}
		//add stem as first element
		mf.add(0, (stem != null? stem: MorphologicalTag.TAG_STEM + dicEntry.getWord()));*/

		//add morphological fields from the applied affix
		mf.addAll((isSuffix()? mf.size(): 0), amf);

		return mf.toArray(new String[mf.size()]);
	}

	public static String[] extractMorphologicalFields(final List<DictionaryEntry> compoundEntries){
		final List<String[]> mf = new ArrayList<>();
		if(compoundEntries != null)
			for(final DictionaryEntry compoundEntry : compoundEntries){
				final String compound = compoundEntry.getWord();
				mf.add(ArrayUtils.addAll(new String[]{MorphologicalTag.TAG_PART + compound}, compoundEntry.morphologicalFields));
			}
		return mf.stream()
			.flatMap(Arrays::stream)
			.toArray(String[]::new);
	}

	public final boolean isSuffix(){
		return (type == Type.SUFFIX);
	}

	public boolean match(final String word){
		return condition.match(word, type);
	}

	public String applyRule(final String word, final boolean isFullstrip) throws IllegalArgumentException{
		if(!isFullstrip && word.length() == removingLength)
			throw new IllegalArgumentException("Cannot strip full words without the FULLSTRIP tag");

		return (isSuffix()?
			word.substring(0, word.length() - removingLength) + appending:
			appending + word.substring(removingLength));
	}

	public String undoRule(final String word) throws IllegalArgumentException{
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
		return entry;
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final AffixEntry rhs = (AffixEntry)obj;
		return new EqualsBuilder()
			.append(entry, rhs.entry)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(entry)
			.toHashCode();
	}

}
