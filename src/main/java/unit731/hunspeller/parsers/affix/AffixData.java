package unit731.hunspeller.parsers.affix;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.affix.strategies.ParsingStrategyFactory;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.dictionary.vos.AffixEntry;
import unit731.hunspeller.services.Memoizer;


public class AffixData{

	private static final Function<String, FlagParsingStrategy> FLAG_PARSING_STRATEGY = Memoizer.memoize(ParsingStrategyFactory::createFromFlag);


	private final Map<String, Object> data = new HashMap<>();
	private final Set<String> terminalAffixes = new HashSet<>();
	private boolean closed;


	void close(){
		terminalAffixes.addAll(getStringData(AffixTag.NO_SUGGEST_FLAG, AffixTag.COMPOUND_FLAG, AffixTag.FORBIDDEN_WORD_FLAG,
			AffixTag.COMPOUND_BEGIN_FLAG, AffixTag.COMPOUND_MIDDLE_FLAG, AffixTag.COMPOUND_END_FLAG, AffixTag.ONLY_IN_COMPOUND_FLAG,
			AffixTag.PERMIT_COMPOUND_FLAG, AffixTag.FORBID_COMPOUND_FLAG, AffixTag.FORCE_COMPOUND_UPPERCASE_FLAG, AffixTag.CIRCUMFIX_FLAG,
			AffixTag.KEEP_CASE_FLAG, AffixTag.NEED_AFFIX_FLAG));

		closed = true;
	}

	public boolean isClosed(){
		return closed;
	}

	void clear(){
		data.clear();
		terminalAffixes.clear();
		closed = false;
	}

	boolean containsData(final AffixTag key){
		return containsData(key.getCode());
	}

	private boolean containsData(final String key){
		return data.containsKey(key);
	}

	public <T> T getData(final AffixTag key){
		return getData(key.getCode());
	}

	public <T> T getDataOrDefault(final AffixTag key, final T defaultValue){
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

	private List<String> getStringData(final AffixTag... keys){
		final List<String> result = new ArrayList<>(keys.length);
		for(final AffixTag key : keys)
			result.add(getData(key));
		return result;
	}

	<T> void addData(final AffixTag key, final T value){
		addData(key.getCode(), value);
	}

	@SuppressWarnings("unchecked")
	<T> void addData(final String key, final T value){
		if(closed)
			throw new IllegalArgumentException("Cannot add data, container is closed");

		final T prevValue = (T)data.put(key, value);

		if(prevValue != null)
			throw new IllegalArgumentException("Duplicated flag: " + key);
	}


	public String getLanguage(){
		return getData(AffixTag.LANGUAGE);
	}

	public FlagParsingStrategy getFlagParsingStrategy(){
		final String flag = getFlag();
		return (flag != null? FLAG_PARSING_STRATEGY.apply(flag): ParsingStrategyFactory.createASCIIParsingStrategy());
	}

	public String getKeepCaseFlag(){
		return getData(AffixTag.KEEP_CASE_FLAG);
	}

	public String getNeedAffixFlag(){
		return getData(AffixTag.NEED_AFFIX_FLAG);
	}

	public boolean isTerminalAffix(final String flag){
		return terminalAffixes.contains(flag);
	}

	public Set<String> getCompoundRules(){
		return getDataOrDefault(AffixTag.COMPOUND_RULE, Collections.<String>emptySet());
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
		return Charset.forName(getData(AffixTag.CHARACTER_SET));
	}

	public boolean isFullstrip(){
		return containsData(AffixTag.FULLSTRIP);
	}

	/**
	 * 2-stage prefix plus 1-stage suffix instead of 2-stage suffix plus 1-stage prefix
	 * 
	 * @return Whether the prefix is complex
	 */
	public boolean isComplexPrefixes(){
		return containsData(AffixTag.COMPLEX_PREFIXES);
	}

	public Boolean isSuffix(final String affixCode){
		Boolean isSuffix = null;
		final Object affix = getData(affixCode);
		if(affix != null && RuleEntry.class.isAssignableFrom(affix.getClass()))
			isSuffix = ((RuleEntry)affix).isSuffix();
		return isSuffix;
	}

	public boolean isForbidDifferentCasesInCompound(){
		return containsData(AffixTag.FORBID_DIFFERENT_CASES_IN_COMPOUND);
	}

	public boolean isForbidTriplesInCompound(){
		return containsData(AffixTag.FORBIT_TRIPLES_IN_COMPOUND);
	}

	public boolean isSimplifyTriplesInCompound(){
		return containsData(AffixTag.SIMPLIFIED_TRIPLES_IN_COMPOUND);
	}

	public Set<String> getProductiveAffixes(){
		//keeps only items with RuleEntry as value
		final Set<String> affixes = new HashSet<>();
		final Set<String> keys = data.keySet();
		for(final String key : keys){
			final Object affix = getData(key);
			if(RuleEntry.class.isAssignableFrom(affix.getClass()))
				affixes.add(key);
		}
		return affixes;
	}

	public String getFlag(){
		return getData(AffixTag.FLAG);
	}

	public boolean isAffixProductive(String word, final String affix){
		word = applyInputConversionTable(word);

		boolean productive;
		final RuleEntry rule = getData(affix);
		if(rule != null){
			final List<AffixEntry> applicableAffixes = extractListOfApplicableAffixes(word, rule.getEntries());
			productive = !applicableAffixes.isEmpty();
		}
		else
			productive = isManagedByCompoundRule(affix);
		return productive;
	}

	public static List<AffixEntry> extractListOfApplicableAffixes(final String word, final List<AffixEntry> entries){
		//extract the list of applicable affixes...
		final List<AffixEntry> applicableAffixes = new ArrayList<>();
		for(final AffixEntry entry : entries)
			if(entry.match(word))
				applicableAffixes.add(entry);
		return applicableAffixes;
	}

	public String getNoSuggestFlag(){
		return getData(AffixTag.NO_SUGGEST_FLAG);
	}

	public List<String> applyReplacementTable(final String word){
		final ConversionTable table = getData(AffixTag.REPLACEMENT_TABLE);
		return (table != null? table.applyConversionTable(word): Collections.<String>emptyList());
	}

	public String applyInputConversionTable(final String word){
		final ConversionTable table = getData(AffixTag.INPUT_CONVERSION_TABLE);
		return applyConversionTable(word, table, "input");
	}

	public String applyOutputConversionTable(final String word){
		final ConversionTable table = getData(AffixTag.OUTPUT_CONVERSION_TABLE);
		return applyConversionTable(word, table, "output");
	}

	private String applyConversionTable(String word, final ConversionTable table, final String type){
		if(table != null){
			try{
				word = table.applySingleConversionTable(word);
			}
			catch(final IllegalArgumentException e){
				throw new IllegalArgumentException("Cannot " + type + " convert word " + word + ", too much appliable rules");
			}
		}
		return word;
	}

	public Set<String> getWordBreakCharacters(){
		return getData(AffixTag.WORD_BREAK_CHARACTERS);
	}

	public String getCompoundBeginFlag(){
		return getData(AffixTag.COMPOUND_BEGIN_FLAG);
	}

	public String getCompoundMiddleFlag(){
		return getData(AffixTag.COMPOUND_MIDDLE_FLAG);
	}

	public String getCompoundEndFlag(){
		return getData(AffixTag.COMPOUND_END_FLAG);
	}

	public String getOnlyInCompoundFlag(){
		return getData(AffixTag.ONLY_IN_COMPOUND_FLAG);
	}

	public boolean allowTwofoldAffixesInCompound(){
		return containsData(AffixTag.ALLOW_TWOFOLD_AFFIXES_IN_COMPOUND);
	}

	public String getPermitCompoundFlag(){
		return getData(AffixTag.PERMIT_COMPOUND_FLAG);
	}

	public String getForbidCompoundFlag(){
		return getData(AffixTag.FORBID_COMPOUND_FLAG);
	}

	public int getCompoundMaxWordCount(){
		return getData(AffixTag.COMPOUND_MAX_WORD_COUNT);
	}

	public boolean isForbidDuplicationsInCompound(){
		return containsData(AffixTag.FORBID_DUPLICATIONS_IN_COMPOUND);
	}

	public boolean isCheckCompoundReplacement(){
		return containsData(AffixTag.CHECK_COMPOUND_REPLACEMENT);
	}

	public int getCompoundMinimumLength(){
		return getData(AffixTag.COMPOUND_MINIMUM_LENGTH);
	}

	public String getCompoundFlag(){
		return getData(AffixTag.COMPOUND_FLAG);
	}

	public String getCircumfixFlag(){
		return getData(AffixTag.CIRCUMFIX_FLAG);
	}

	public String getForbiddenWordFlag(){
		return getData(AffixTag.FORBIDDEN_WORD_FLAG);
	}

	public String getForceCompoundUppercaseFlag(){
		return getData(AffixTag.FORCE_COMPOUND_UPPERCASE_FLAG);
	}

	public List<RuleEntry> getRuleEntries(){
		return data.values().stream()
			.filter(entry -> entry instanceof RuleEntry)
			.map(RuleEntry.class::cast)
			.collect(Collectors.toList());
	}

}
