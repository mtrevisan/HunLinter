package unit731.hunspeller.parsers.dictionary.valueobjects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.services.PatternService;


@EqualsAndHashCode(of = "entry")
public class AffixEntry{

	public static final String SLASH = "/";
	private static final Matcher MATCHER_ENTRY = PatternService.matcher("\t.*$");

	public static final String DOT = ".";
	private static final String ZERO = "0";


	@AllArgsConstructor
	public static enum Type{
		SUFFIX(AffixTag.SUFFIX),
		PREFIX(AffixTag.PREFIX);


		@Getter
		private final AffixTag flag;

		public static Type toEnum(String flag){
			Type[] types = Type.values();
			for(Type type : types)
				if(type.getFlag().getCode().equals(flag))
					return type;
			return null;
		}
	}


	@Getter
	private final Type type;
	/** ID used to represent the affix */
	@Getter
	private final String flag;
	private final String[] continuationFlags;
	/** condition that must be met before the affix can be applied */
	@Getter
	private final AffixCondition condition;
	/** string to strip */
	private final String removing;
	private final int removingLength;
	/** string to append */
	private final String appending;
	private final int appendingLength;
	private final String[] morphologicalFields;

	private final String entry;


	public AffixEntry(String line, List<String> aliasesFlag, List<String> aliasesMorphologicaField, FlagParsingStrategy strategy){
		Objects.requireNonNull(line);
		Objects.requireNonNull(strategy);

		String[] lineParts = StringUtils.split(line, null, 6);
		String ruleType = lineParts[0];
		this.flag = lineParts[1];
		String removal = lineParts[2];
		String[] additionParts = StringUtils.split(lineParts[3], SLASH);
		String addition = additionParts[0];
		String cond = (lineParts.length > 4? lineParts[4]: DOT);
		morphologicalFields = (lineParts.length > 5? StringUtils.split(expandAliases(lineParts[5], aliasesMorphologicaField)): null);

		type = Type.toEnum(ruleType);
		String[] classes = strategy.parseFlags((additionParts.length > 1? expandAliases(additionParts[1], aliasesFlag): null));
		continuationFlags = (classes.length > 0? classes: null);
		condition = new AffixCondition(cond, type);
		removing = (!ZERO.equals(removal)? removal: StringUtils.EMPTY);
		removingLength = removing.length();
		appending = (!ZERO.equals(addition)? addition: StringUtils.EMPTY);
		appendingLength = appending.length();

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

		entry = PatternService.clear(line, MATCHER_ENTRY);
	}

	private String expandAliases(String part, List<String> aliases) throws IllegalArgumentException{
		return (aliases != null && !aliases.isEmpty() && NumberUtils.isCreatable(part)? aliases.get(Integer.valueOf(part) - 1): part);
	}

	public boolean containsContinuationFlag(String flag){
		return ArrayUtils.contains(continuationFlags, flag);
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
		return flags.toArray(new String[flags.size()]);
	}

	/**
	 * FIXME is this documentation updated/true?
	 * 
	 * Derivational Suffix: stemming doesn't remove derivational suffixes (morphological generation depends on the order of the suffix fields)
	 * Inflectional Suffix: all inflectional suffixes are removed by stemming (morphological generation depends on the order of the suffix fields)
	 * Terminal Suffix: inflectional suffix fields "removed" by additional (not terminal) suffixes, useful for zero morphemes and affixes
	 * 	removed by splitting rules
	 */
	public String[] combineMorphologicalFields(DictionaryEntry productable){
		List<String> mf = (productable.morphologicalFields != null? new ArrayList<>(Arrays.asList(productable.morphologicalFields)): new ArrayList<>());
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
			if(field.startsWith(WordGenerator.TAG_STEM)){
				stemFound = true;
				break;
			}
		//add stem only if not present
		if(!stemFound)
			mf.add(0, WordGenerator.TAG_STEM + productable.getWord());

		//add morphological fields from the applied affix
		mf.addAll((isSuffix()? mf.size(): 0), amf);

		return mf.toArray(new String[mf.size()]);
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

}
