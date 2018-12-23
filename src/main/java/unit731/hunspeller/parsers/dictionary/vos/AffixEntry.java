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

	private static final String SLASH = "/";
	private static final String SLASH_ESCAPED = "\\/";
	private static final Pattern PATTERN_ENTRY = PatternHelper.pattern("\t.*$");

	public static final String DOT = ".";
	public static final String ZERO = "0";


	public static enum Type{
		SUFFIX(AffixTag.SUFFIX),
		PREFIX(AffixTag.PREFIX);


		private final AffixTag flag;

		Type(AffixTag flag){
			this.flag = flag;
		}

		public static Type toEnum(String flag){
			Type[] types = Type.values();
			for(Type type : types)
				if(type.getFlag().getCode().equals(flag))
					return type;
			return null;
		}

		public boolean is(String flag){
			return this.flag.getCode().equals(flag);
		}

		public AffixTag getFlag(){
			return flag;
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


	public AffixEntry(String line, FlagParsingStrategy strategy, List<String> aliasesFlag, List<String> aliasesMorphologicaField){
		Objects.requireNonNull(line);
		Objects.requireNonNull(strategy);

		String[] lineParts = StringUtils.split(line, null, 6);
		if(lineParts.length < 4 || lineParts.length > 6)
			throw new IllegalArgumentException("Expected an affix entry, found something else"
				+ (lineParts.length > 0? ": " + line: StringUtils.EMPTY));

		String ruleType = lineParts[0];
		this.flag = lineParts[1];
		String removal = StringUtils.replace(lineParts[2], SLASH_ESCAPED, SLASH);
		Matcher m = PATTERN_LINE.matcher(lineParts[3]);
		if(!m.find())
			throw new IllegalArgumentException("Cannot parse affix line " + line);
		String addition = StringUtils.replace(m.group(PARAM_CONDITION), SLASH_ESCAPED, SLASH);
		String continuationClasses = m.group(PARAM_CONTINUATION_CLASSES);
		String cond = (lineParts.length > 4? StringUtils.replace(lineParts[4], SLASH_ESCAPED, SLASH): DOT);
		morphologicalFields = (lineParts.length > 5? StringUtils.split(expandAliases(lineParts[5], aliasesMorphologicaField)): null);

		type = Type.toEnum(ruleType);
		String[] classes = strategy.parseFlags((continuationClasses != null? expandAliases(continuationClasses, aliasesFlag): null));
		continuationFlags = (classes != null && classes.length > 0? classes: null);
		condition = new AffixCondition(cond, type);
		removing = (!ZERO.equals(removal)? removal: StringUtils.EMPTY);
		removingLength = removing.length();
		appending = (!ZERO.equals(addition)? addition: StringUtils.EMPTY);
		appendingLength = appending.length();

		if(continuationFlags != null)
			Arrays.sort(continuationFlags);
		if(removingLength > 0){
			if(isSuffix()){
				if(!cond.endsWith(removal))
					throw new IllegalArgumentException("This line has the condition part that not ends with the removal part: " + line);
				if(appending.length() > 1 && removal.charAt(0) == appending.charAt(0))
					throw new IllegalArgumentException("This line has characters in common between removed and added part: " + line);
			}
			else{
				if(!cond.startsWith(removal))
					throw new IllegalArgumentException("This line has the condition part that not starts with the removal part: " + line);
				if(appending.length() > 1 && removal.charAt(removal.length() - 1) == appending.charAt(appending.length() - 1))
					throw new IllegalArgumentException("This line has characters in common between removed and added part: " + line);
			}
		}

		entry = PatternHelper.clear(line, PATTERN_ENTRY);
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

	private String expandAliases(String part, List<String> aliases) throws IllegalArgumentException{
		return (aliases != null && !aliases.isEmpty() && NumberUtils.isCreatable(part)? aliases.get(Integer.parseInt(part) - 1): part);
	}

	public boolean hasContinuationFlag(String flag){
		return (continuationFlags != null && flag != null && Arrays.binarySearch(continuationFlags, flag) >= 0);
	}

	public boolean containsUniqueContinuationFlags(){
		if(continuationFlags == null)
			return true;

		Set<String> set = new HashSet<>();
		return Arrays.stream(continuationFlags)
			.allMatch(set::add);
	}

	public String[] combineContinuationFlags(String[] otherContinuationFlags){
		Set<String> flags = new HashSet<>();
		if(continuationFlags != null)
			flags.addAll(Arrays.asList(continuationFlags));
		if(otherContinuationFlags != null && otherContinuationFlags.length > 0)
			flags.addAll(Arrays.asList(otherContinuationFlags));
		int size = flags.size();
		return (size > 0? flags.toArray(new String[size]): null);
	}

	/**
	 * FIXME is this documentation updated/true?
	 * 
	 * Derivational Suffix: stemming doesn't remove derivational suffixes (morphological generation depends on the order of the suffix fields)
	 * Inflectional Suffix: all inflectional suffixes are removed by stemming (morphological generation depends on the order of the suffix fields)
	 * Terminal Suffix: inflectional suffix fields "removed" by additional (not terminal) suffixes, useful for zero morphemes and affixes
	 * 	removed by splitting rules
	 * 
	 * @param dicEntry	The dictionary entry to combine from
	 * @return	The list of new morphological fields
	 */
	public String[] combineMorphologicalFields(DictionaryEntry dicEntry){
		List<String> mf = (dicEntry.morphologicalFields != null? new ArrayList<>(Arrays.asList(dicEntry.morphologicalFields)): new ArrayList<>());
		List<String> amf = (morphologicalFields != null? Arrays.asList(morphologicalFields): Collections.<String>emptyList());

//		boolean containsNonTerminalSuffixes = amf.stream()
//			.anyMatch(field -> field.startsWith(WordGenerator.TAG_INFLECTIONAL_SUFFIX));
//		//remove inflectional and terminal suffixes
//		mf = mf.stream()
//			.filter(field -> !field.startsWith(WordGenerator.TAG_INFLECTIONAL_SUFFIX))
//			.filter(field -> !containsNonTerminalSuffixes || !field.startsWith(WordGenerator.TAG_TERMINAL_SUFFIX))
//			.collect(Collectors.toList());

		//find stem
		boolean stemFound = false;
		for(String field : mf)
			if(field.startsWith(MorphologicalTag.TAG_STEM)){
				stemFound = true;
				break;
			}
		if(!stemFound)
			for(String field : amf)
				if(field.startsWith(MorphologicalTag.TAG_STEM)){
					stemFound = true;
					break;
				}
		//add stem only if not present
		if(!stemFound)
			mf.add(0, MorphologicalTag.TAG_STEM + dicEntry.getWord());

		//add morphological fields from the applied affix
		mf.addAll((isSuffix()? mf.size(): 0), amf);

		return mf.toArray(new String[mf.size()]);
	}

	public static String[] extractMorphologicalFields(List<DictionaryEntry> compoundEntries){
		List<String[]> mf = new ArrayList<>();
		if(compoundEntries != null)
			for(DictionaryEntry compoundEntry : compoundEntries){
				String compound = compoundEntry.getWord();
				mf.add(ArrayUtils.addAll(new String[]{MorphologicalTag.TAG_PART + compound}, compoundEntry.morphologicalFields));
			}
		return mf.stream()
			.flatMap(Arrays::stream)
			.toArray(String[]::new);
	}

	public final boolean isSuffix(){
		return (type == Type.SUFFIX);
	}

	public boolean match(String word){
		return condition.match(word, type);
	}

	public String applyRule(String word, boolean isFullstrip) throws IllegalArgumentException{
		if(!isFullstrip && word.length() == removingLength)
			throw new IllegalArgumentException("Cannot strip full words without the FULLSTRIP tag");

		return (isSuffix()? word.substring(0, word.length() - removingLength) + appending: appending + word.substring(removingLength));
	}

	public String undoRule(String word) throws IllegalArgumentException{
		return (isSuffix()? word.substring(0, word.length() - appendingLength) + removing: removing + word.substring(appendingLength));
	}

	@Override
	public String toString(){
		return entry;
	}

	@Override
	public boolean equals(Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		AffixEntry rhs = (AffixEntry)obj;
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
