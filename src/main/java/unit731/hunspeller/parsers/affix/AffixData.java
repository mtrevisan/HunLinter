package unit731.hunspeller.parsers.affix;

import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.affix.strategies.ParsingStrategyFactory;
import unit731.hunspeller.parsers.enums.AffixOption;
import unit731.hunspeller.parsers.vos.RuleEntry;
import unit731.hunspeller.parsers.vos.AffixEntry;
import unit731.hunspeller.services.Memoizer;


public class AffixData{

	private static final MessageFormat REPEATED_FLAG = new MessageFormat("Repeated flags in multiple options");
	private static final MessageFormat CONTAINER_CLOSED = new MessageFormat("Cannot add data, container is closed");
	private static final MessageFormat DUPLICATED_FLAG = new MessageFormat("Duplicated flag: ''{0}''");
	private static final MessageFormat TOO_MANY_APPLICABLE_RULES = new MessageFormat("Cannot {0} convert word ''{1}'', too many applicable rules");


	private static final Function<String, FlagParsingStrategy> FLAG_PARSING_STRATEGY
		= Memoizer.memoize(ParsingStrategyFactory::createFromFlag);

	private static final List<AffixOption> SINGLE_FLAG_TAGS = Arrays.asList(AffixOption.NO_SUGGEST_FLAG, AffixOption.COMPOUND_FLAG,
		AffixOption.COMPOUND_BEGIN_FLAG, AffixOption.COMPOUND_MIDDLE_FLAG, AffixOption.COMPOUND_END_FLAG,
		AffixOption.ONLY_IN_COMPOUND_FLAG, AffixOption.PERMIT_COMPOUND_FLAG, AffixOption.FORBID_COMPOUND_FLAG,
		/*AffixOption.COMPOUND_ROOT,*/ AffixOption.CIRCUMFIX_FLAG, AffixOption.FORBIDDEN_WORD_FLAG, AffixOption.KEEP_CASE_FLAG,
		AffixOption.NEED_AFFIX_FLAG/*, AffixOption.SUB_STANDARD_FLAG*/);


	private final Map<String, Object> data = new HashMap<>();
	private final Set<String> terminalAffixes = new HashSet<>();
	private boolean closed;


	void close(){
		terminalAffixes.addAll(getStringData(AffixOption.NO_SUGGEST_FLAG, AffixOption.COMPOUND_FLAG, AffixOption.FORBIDDEN_WORD_FLAG,
			AffixOption.COMPOUND_BEGIN_FLAG, AffixOption.COMPOUND_MIDDLE_FLAG, AffixOption.COMPOUND_END_FLAG,
			AffixOption.ONLY_IN_COMPOUND_FLAG, AffixOption.PERMIT_COMPOUND_FLAG, AffixOption.FORBID_COMPOUND_FLAG,
			AffixOption.FORCE_COMPOUND_UPPERCASE_FLAG, AffixOption.CIRCUMFIX_FLAG, AffixOption.KEEP_CASE_FLAG,
			AffixOption.NEED_AFFIX_FLAG));

		closed = true;
	}

	void clear(){
		data.clear();
		terminalAffixes.clear();
		closed = false;
	}

	/** Check that the same flag does not belongs to different tags */
	void verify(){
		final Map<AffixOption, Object> extractSingleFlags = extractSingleFlags();
		final Collection<Object> flaggedData = extractSingleFlags.values();
		final Set<Object> uniqueValues = new HashSet<>(flaggedData);
		if(uniqueValues.size() != flaggedData.size())
			throw new IllegalArgumentException(REPEATED_FLAG.format(new Object[0]));
	}

	private Map<AffixOption, Object> extractSingleFlags(){
		final Map<AffixOption, Object> singleFlags = new EnumMap<>(AffixOption.class);
		for(final AffixOption option : SINGLE_FLAG_TAGS){
			final Object entry = getData(option);
			if(entry != null)
				singleFlags.put(option, entry);
		}
		return singleFlags;
	}

	boolean containsData(final AffixOption key){
		return containsData(key.getCode());
	}

	private boolean containsData(final String key){
		return data.containsKey(key);
	}

	public <T> T getData(final AffixOption key){
		return getData(key.getCode());
	}

	public <T> T getDataOrDefault(final AffixOption key, final T defaultValue){
		return getDataOrDefault(key.getCode(), defaultValue);
	}

	@SuppressWarnings("unchecked")
	public <T> T getData(final String key){
		return (T)data.get(key);
	}

	@SuppressWarnings("unchecked")
	public <T> T getDataOrDefault(final String key, final T defaultValue){
		return (T)data.getOrDefault(key, defaultValue);
	}

	private List<String> getStringData(final AffixOption... keys){
		final List<String> result = new ArrayList<>(keys.length);
		for(final AffixOption key : keys)
			result.add(getData(key));
		return result;
	}

	<T> void addData(final AffixOption key, final T value){
		addData(key.getCode(), value);
	}

	<T> void addData(final String key, final T value){
		if(closed)
			throw new IllegalArgumentException(CONTAINER_CLOSED.format(new Object[0]));
		if(data.containsKey(key))
			throw new IllegalArgumentException(DUPLICATED_FLAG.format(new Object[]{key}));

		if(value != null)
			data.put(key, value);
	}


	public String getLanguage(){
		return getData(AffixOption.LANGUAGE);
	}

	public FlagParsingStrategy getFlagParsingStrategy(){
		final String flag = getFlag();
		return (flag != null? FLAG_PARSING_STRATEGY.apply(flag): ParsingStrategyFactory.createASCIIParsingStrategy());
	}

	public String getNeedAffixFlag(){
		return getData(AffixOption.NEED_AFFIX_FLAG);
	}

	public boolean isTerminalAffix(final String flag){
		return terminalAffixes.contains(flag);
	}

	public Set<String> getCompoundRules(){
		return getDataOrDefault(AffixOption.COMPOUND_RULE, Collections.emptySet());
	}

	public boolean isManagedByCompoundRule(final String flag){
		final Set<String> compoundRules = getCompoundRules();
		for(final String rule : compoundRules)
			if(isManagedByCompoundRule(rule, flag))
				return true;
		return false;
	}

	public boolean isManagedByCompoundRule(final String compoundRule, final String flag){
		final FlagParsingStrategy strategy = getFlagParsingStrategy();
		final String[] flags = strategy.extractCompoundRule(compoundRule);
		return ArrayUtils.contains(flags, flag);
	}

	public Charset getCharset(){
		return Charset.forName(getData(AffixOption.CHARACTER_SET));
	}

	public boolean isFullstrip(){
		return containsData(AffixOption.FULLSTRIP);
	}

	/**
	 * 2-stage prefix plus 1-stage suffix instead of 2-stage suffix plus 1-stage prefix
	 *
	 * @return Whether the prefix is complex
	 */
	public boolean isComplexPrefixes(){
		return containsData(AffixOption.COMPLEX_PREFIXES);
	}

	public boolean isForbidDifferentCasesInCompound(){
		return containsData(AffixOption.FORBID_DIFFERENT_CASES_IN_COMPOUND);
	}

	public boolean isForbidTriplesInCompound(){
		return containsData(AffixOption.FORBID_TRIPLES_IN_COMPOUND);
	}

	public boolean isSimplifyTriplesInCompound(){
		return containsData(AffixOption.SIMPLIFIED_TRIPLES_IN_COMPOUND);
	}

	public String getFlag(){
		return getData(AffixOption.FLAG);
	}

	public boolean isAffixProductive(final String word, final String affix){
		final String convertedWord = applyInputConversionTable(word);

		boolean productive;
		final Object data = getData(affix);
		if(data != null && RuleEntry.class.isAssignableFrom(data.getClass()))
			productive = ((RuleEntry)data).getEntries().stream()
				.anyMatch(entry -> entry.canApplyTo(convertedWord));
		else
			productive = isManagedByCompoundRule(affix);
		return productive;
	}

	public static List<AffixEntry> extractListOfApplicableAffixes(final String word, final List<AffixEntry> entries){
		//extract the list of applicable affixes...
		return entries.stream()
			.filter(entry -> entry.canApplyTo(word))
			.collect(Collectors.toList());
	}

	public List<String> applyReplacementTable(final String word){
		final ConversionTable table = getData(AffixOption.REPLACEMENT_TABLE);
		return (table != null? table.applyConversionTable(word): Collections.emptyList());
	}

	public String applyInputConversionTable(final String word){
		final ConversionTable table = getData(AffixOption.INPUT_CONVERSION_TABLE);
		return applyConversionTable(word, table, "input");
	}

	public String applyOutputConversionTable(final String word){
		final ConversionTable table = getData(AffixOption.OUTPUT_CONVERSION_TABLE);
		return applyConversionTable(word, table, "output");
	}

	private String applyConversionTable(String word, final ConversionTable table, final String type){
		if(table != null){
			try{
				word = table.applySingleConversionTable(word);
			}
			catch(final IllegalArgumentException e){
				throw new IllegalArgumentException(TOO_MANY_APPLICABLE_RULES.format(new Object[]{type, word}));
			}
		}
		return word;
	}

	/** Extracts all the characters from each rule */
	public String getSampleText(){
		final List<String> sortedSample;
		String sample = getData(AffixOption.TRY);
		if(sample != null)
			sortedSample = Arrays.asList(sample.split(StringUtils.EMPTY));
		else
			sortedSample = getRuleEntries().parallelStream()
				.flatMap(entry -> entry.getEntries().stream())
				.flatMap(entry -> Arrays.stream(entry.getAppending().split(StringUtils.EMPTY)))
				.distinct()
				.collect(Collectors.toList());
		Collections.sort(sortedSample);
		//NOTE: a space should be used because of the presence of characters that are only modifiers
		return String.join(StringUtils.SPACE, sortedSample);
	}

	public String getCompoundBeginFlag(){
		return getData(AffixOption.COMPOUND_BEGIN_FLAG);
	}

	public String getCompoundMiddleFlag(){
		return getData(AffixOption.COMPOUND_MIDDLE_FLAG);
	}

	public String getCompoundEndFlag(){
		return getData(AffixOption.COMPOUND_END_FLAG);
	}

	public String getOnlyInCompoundFlag(){
		return getData(AffixOption.ONLY_IN_COMPOUND_FLAG);
	}

	public boolean allowTwofoldAffixesInCompound(){
		return containsData(AffixOption.ALLOW_TWOFOLD_AFFIXES_IN_COMPOUND);
	}

	public String getPermitCompoundFlag(){
		return getData(AffixOption.PERMIT_COMPOUND_FLAG);
	}

	public String getForbidCompoundFlag(){
		return getData(AffixOption.FORBID_COMPOUND_FLAG);
	}

	public int getCompoundMaxWordCount(){
		return getData(AffixOption.COMPOUND_MAX_WORD_COUNT);
	}

	public boolean isForbidDuplicatesInCompound(){
		return containsData(AffixOption.FORBID_DUPLICATES_IN_COMPOUND);
	}

	public boolean isCheckCompoundReplacement(){
		return containsData(AffixOption.CHECK_COMPOUND_REPLACEMENT);
	}

	public int getCompoundMinimumLength(){
		return getData(AffixOption.COMPOUND_MINIMUM_LENGTH);
	}

	public String getCompoundFlag(){
		return getData(AffixOption.COMPOUND_FLAG);
	}

	public String getCircumfixFlag(){
		return getData(AffixOption.CIRCUMFIX_FLAG);
	}

	public String getForbiddenWordFlag(){
		return getData(AffixOption.FORBIDDEN_WORD_FLAG);
	}

	public String getForceCompoundUppercaseFlag(){
		return getData(AffixOption.FORCE_COMPOUND_UPPERCASE_FLAG);
	}

	public List<RuleEntry> getRuleEntries(){
		return data.values().stream()
			.filter(entry -> RuleEntry.class.isAssignableFrom(entry.getClass()))
			.map(RuleEntry.class::cast)
			.collect(Collectors.toList());
	}

}
