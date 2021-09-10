/**
 * Copyright (c) 2019-2021 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.parsers.affix;

import io.github.mtrevisan.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import io.github.mtrevisan.hunlinter.parsers.affix.strategies.ParsingStrategyFactory;
import io.github.mtrevisan.hunlinter.parsers.enums.AffixOption;
import io.github.mtrevisan.hunlinter.parsers.vos.AffixEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.RuleEntry;
import io.github.mtrevisan.hunlinter.services.Packager;
import io.github.mtrevisan.hunlinter.services.system.Memoizer;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;
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


public class AffixData{

	private static final String REPEATED_FLAG = "Same flags present in multiple options";
	private static final String CONTAINER_CLOSED = "Cannot add data, container is closed";
	private static final String DUPLICATED_FLAG = "Flag already present: `{}`";


	private static final Function<String, FlagParsingStrategy> FLAG_PARSING_STRATEGY
		= Memoizer.memoize(ParsingStrategyFactory::createFromFlag);

	private static final List<AffixOption> SINGLE_FLAG_TAGS = Arrays.asList(AffixOption.NO_SUGGEST_FLAG, AffixOption.COMPOUND_FLAG,
		AffixOption.COMPOUND_BEGIN_FLAG, AffixOption.COMPOUND_MIDDLE_FLAG, AffixOption.COMPOUND_END_FLAG,
		AffixOption.ONLY_IN_COMPOUND_FLAG, AffixOption.PERMIT_COMPOUND_FLAG, AffixOption.FORBID_COMPOUND_FLAG,
		/*AffixOption.COMPOUND_ROOT,*/ AffixOption.FORCE_COMPOUND_UPPERCASE_FLAG, AffixOption.CIRCUMFIX_FLAG, AffixOption.FORBIDDEN_WORD_FLAG,
		AffixOption.KEEP_CASE_FLAG, AffixOption.NEED_AFFIX_FLAG/*, AffixOption.SUB_STANDARD_FLAG*/);


	private final Map<String, Object> data = new HashMap<>(0);
	private final Collection<String> terminalAffixes = new HashSet<>(0);
	private final Set<String> productableFlags = new HashSet<>(0);
	private boolean closed;


	final void close(){
		terminalAffixes.addAll(getStringData(SINGLE_FLAG_TAGS));

		productableFlags.addAll(data.keySet());
		Arrays.asList(AffixOption.CHARACTER_SET.getCode(), AffixOption.FLAG.getCode(),
			AffixOption.COMPLEX_PREFIXES.getCode(), AffixOption.LANGUAGE.getCode(), AffixOption.ALIASES_FLAG.getCode(),
			AffixOption.ALIASES_MORPHOLOGICAL_FIELD.getCode(), AffixOption.TRY.getCode(), AffixOption.NO_SUGGEST_FLAG.getCode(),
			AffixOption.REPLACEMENT_TABLE.getCode(), AffixOption.RELATION_TABLE.getCode(), AffixOption.WORD_BREAK_CHARACTERS.getCode(),
			AffixOption.COMPOUND_RULE.getCode(), AffixOption.COMPOUND_MINIMUM_LENGTH.getCode(),
			AffixOption.ALLOW_TWOFOLD_AFFIXES_IN_COMPOUND.getCode(), AffixOption.COMPOUND_MAX_WORD_COUNT.getCode(),
			AffixOption.FORBID_DUPLICATES_IN_COMPOUND.getCode(), AffixOption.CHECK_COMPOUND_REPLACEMENT.getCode(),
			AffixOption.FORBID_DIFFERENT_CASES_IN_COMPOUND.getCode(), AffixOption.FORBID_TRIPLES_IN_COMPOUND.getCode(),
			AffixOption.SIMPLIFIED_TRIPLES_IN_COMPOUND.getCode(), AffixOption.FORCE_COMPOUND_UPPERCASE_FLAG.getCode(),
			AffixOption.FULLSTRIP.getCode(), AffixOption.KEEP_CASE_FLAG.getCode(), AffixOption.NEED_AFFIX_FLAG.getCode(),
			AffixOption.INPUT_CONVERSION_TABLE.getCode(), AffixOption.OUTPUT_CONVERSION_TABLE.getCode()
		).forEach(productableFlags::remove);

		closed = true;
	}

	final void clear(){
		data.clear();
		terminalAffixes.clear();
		productableFlags.clear();
		closed = false;
	}

	/** Check that the same flag doesn't belong to different tags. */
	final void verify(){
		final Map<AffixOption, Object> extractSingleFlags = extractSingleFlags();
		final Collection<Object> flaggedData = extractSingleFlags.values();
		final Collection<Object> uniqueValues = new HashSet<>(flaggedData);
		if(uniqueValues.size() != flaggedData.size())
			throw new LinterException(REPEATED_FLAG);
	}

	private Map<AffixOption, Object> extractSingleFlags(){
		final Map<AffixOption, Object> singleFlags = new EnumMap<>(AffixOption.class);
		for(int i = 0; i < SINGLE_FLAG_TAGS.size(); i ++){
			final AffixOption option = SINGLE_FLAG_TAGS.get(i);
			final Object entry = getData(option);
			if(entry != null)
				singleFlags.put(option, entry);
		}
		return singleFlags;
	}

	final boolean containsData(final AffixOption key){
		return containsData(key.getCode());
	}

	private boolean containsData(final String key){
		return data.containsKey(key);
	}

	public final <T> T getData(final AffixOption key){
		return getData(key.getCode());
	}

	public final <T> T getDataOrDefault(final AffixOption key, final T defaultValue){
		return getDataOrDefault(key.getCode(), defaultValue);
	}

	@SuppressWarnings("unchecked")
	public final <T> T getData(final String key){
		return (T)data.get(key);
	}

	@SuppressWarnings("unchecked")
	public final <T> T getDataOrDefault(final String key, final T defaultValue){
		return (T)data.getOrDefault(key, defaultValue);
	}

	private List<String> getStringData(final List<AffixOption> keys){
		final List<String> strings = new ArrayList<>(keys.size());
		for(int i = 0; i < keys.size(); i ++)
			strings.add(getData(keys.get(i)));
		return strings;
	}

	public final <T> void addData(final AffixOption key, final T value){
		addData(key.getCode(), value);
	}

	public final <T> void addData(final String key, final T value){
		if(closed)
			throw new LinterException(CONTAINER_CLOSED);
		if(data.containsKey(key))
			throw new LinterException(DUPLICATED_FLAG, key);

		if(value != null)
			data.put(key, value);
	}


	public final String getLanguage(){
		return getData(AffixOption.LANGUAGE);
	}

	public final void setLanguage(final String language){
		data.put(AffixOption.LANGUAGE.getCode(), language);
	}

	public final FlagParsingStrategy getFlagParsingStrategy(){
		final String flag = getFlag();
		return (flag != null? FLAG_PARSING_STRATEGY.apply(flag): ParsingStrategyFactory.createASCIIParsingStrategy());
	}

	public final String getNeedAffixFlag(){
		return getData(AffixOption.NEED_AFFIX_FLAG);
	}

	public final boolean isTerminalAffix(final String flag){
		return terminalAffixes.contains(flag);
	}

	public final Set<String> getCompoundRules(){
		return getDataOrDefault(AffixOption.COMPOUND_RULE, Collections.emptySet());
	}

	public final boolean isManagedByCompoundRule(final String flag){
		for(final String rule : getCompoundRules())
			if(isManagedByCompoundRule(rule, flag))
				return true;
		return false;
	}

	public final boolean isManagedByCompoundRule(final String compoundRule, final String flag){
		final FlagParsingStrategy strategy = getFlagParsingStrategy();
		final String[] flags = strategy.extractCompoundRule(compoundRule);
		return ArrayUtils.contains(flags, flag);
	}

	public final Charset getCharset(){
		return Charset.forName(getData(AffixOption.CHARACTER_SET));
	}

	public final boolean isFullstrip(){
		return containsData(AffixOption.FULLSTRIP);
	}

	/**
	 * 2-stage prefix plus 1-stage suffix instead of 2-stage suffix plus 1-stage prefix
	 *
	 * @return Whether the prefix is complex
	 */
	public final boolean isComplexPrefixes(){
		return containsData(AffixOption.COMPLEX_PREFIXES);
	}

	public final boolean isForbidDifferentCasesInCompound(){
		return containsData(AffixOption.FORBID_DIFFERENT_CASES_IN_COMPOUND);
	}

	public final boolean isForbidTriplesInCompound(){
		return containsData(AffixOption.FORBID_TRIPLES_IN_COMPOUND);
	}

	public final boolean isSimplifyTriplesInCompound(){
		return containsData(AffixOption.SIMPLIFIED_TRIPLES_IN_COMPOUND);
	}

	public final String getFlag(){
		return getData(AffixOption.FLAG);
	}

	public final boolean isAffixProductive(final String affix, final String word){
		final String convertedWord = applyInputConversionTable(word);

		final boolean productive;
		final Object affixData = getData(affix);
		if(affixData != null && RuleEntry.class.isAssignableFrom(affixData.getClass()))
			productive = ((RuleEntry)affixData).isProductiveFor(convertedWord);
		else
			productive = isManagedByCompoundRule(affix);
		return productive;
	}

	public static AffixEntry[] extractListOfApplicableAffixes(final String word, final List<AffixEntry> entries){
		int limit = 0;
		final int size = (entries != null? entries.size(): 0);
		final AffixEntry[] list = new AffixEntry[size];
		for(int i = 0; i < size; i ++){
			final AffixEntry entry = entries.get(i);
			if(entry.canApplyTo(word))
				list[limit ++] = entry;
		}
		return Arrays.copyOf(list, limit);
	}

	public final String getReplacementPairs(){
		return ((ConversionTable)getData(AffixOption.REPLACEMENT_TABLE))
			.extractAsList();
	}

	public final String getEquivalentChars(){
		return ((RelationTable)getData(AffixOption.RELATION_TABLE))
			.extractAsList();
	}

	public final String getInputConversions(){
		return ((ConversionTable)getData(AffixOption.INPUT_CONVERSION_TABLE))
			.extractAsList();
	}

	public final String applyReplacementTable(final String word){
		final ConversionTable table = getData(AffixOption.REPLACEMENT_TABLE);
		return (table != null? table.applyConversionTable(word): word);
	}

	public final String applyInputConversionTable(final String word){
		final ConversionTable table = getData(AffixOption.INPUT_CONVERSION_TABLE);
		return applyConversionTable(word, table);
	}

	public final String applyOutputConversionTable(final String word){
		final ConversionTable table = getData(AffixOption.OUTPUT_CONVERSION_TABLE);
		return applyConversionTable(word, table);
	}

	private static String applyConversionTable(final String word, final ConversionTable table){
		return (table != null? table.applyConversionTable(word): word);
	}

	/**
	 * Extracts all the characters from each rule
	 *
	 * @return	A sample text of the underlying dictionary
	 *
	 * @see Packager#getSampleText()
	 */
	public final String getSampleText(){
		final List<String> sortedSample;
		final String sample = getData(AffixOption.TRY);
		if(sample != null)
			sortedSample = Arrays.asList(StringUtils.split(sample, StringUtils.EMPTY));
		else
			sortedSample = getRuleEntries().parallelStream()
				.flatMap(entry -> entry.getEntries().stream())
				.flatMap(entry -> Arrays.stream(StringUtils.split(entry.getAppending(), StringUtils.EMPTY)))
				.distinct()
				.collect(Collectors.toList());
		Collections.sort(sortedSample);
		//NOTE: a space should be used because of the presence of characters that are only modifiers
		return StringUtils.join(sortedSample, StringUtils.SPACE);
	}

	public final String getCompoundBeginFlag(){
		return getData(AffixOption.COMPOUND_BEGIN_FLAG);
	}

	public final String getCompoundMiddleFlag(){
		return getData(AffixOption.COMPOUND_MIDDLE_FLAG);
	}

	public final String getCompoundEndFlag(){
		return getData(AffixOption.COMPOUND_END_FLAG);
	}

	public final String getOnlyInCompoundFlag(){
		return getData(AffixOption.ONLY_IN_COMPOUND_FLAG);
	}

	public final boolean isTwofoldAffixesInCompound(){
		return containsData(AffixOption.ALLOW_TWOFOLD_AFFIXES_IN_COMPOUND);
	}

	public final String getPermitCompoundFlag(){
		return getData(AffixOption.PERMIT_COMPOUND_FLAG);
	}

	public final String getForbidCompoundFlag(){
		return getData(AffixOption.FORBID_COMPOUND_FLAG);
	}

	public final Integer getCompoundMaxWordCount(){
		return getData(AffixOption.COMPOUND_MAX_WORD_COUNT);
	}

	public final boolean isForbidDuplicatesInCompound(){
		return containsData(AffixOption.FORBID_DUPLICATES_IN_COMPOUND);
	}

	public final boolean isCheckCompoundReplacement(){
		return containsData(AffixOption.CHECK_COMPOUND_REPLACEMENT);
	}

	public final Integer getCompoundMinimumLength(){
		return getData(AffixOption.COMPOUND_MINIMUM_LENGTH);
	}

	public final String getCompoundFlag(){
		return getData(AffixOption.COMPOUND_FLAG);
	}

	public final String getCircumfixFlag(){
		return getData(AffixOption.CIRCUMFIX_FLAG);
	}

	public final String getForbiddenWordFlag(){
		return getData(AffixOption.FORBIDDEN_WORD_FLAG);
	}

	public final String getForceCompoundUppercaseFlag(){
		return getData(AffixOption.FORCE_COMPOUND_UPPERCASE_FLAG);
	}

	public final Set<String> getProductableFlags(){
		return productableFlags;
	}

	public final List<RuleEntry> getRuleEntries(){
		final List<RuleEntry> list = new ArrayList<>(data.size());
		for(final Object entry : data.values())
			if(RuleEntry.class.isAssignableFrom(entry.getClass()))
				list.add((RuleEntry)entry);
		return list;
	}

}
