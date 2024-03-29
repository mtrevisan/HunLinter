/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.parsers.vos;

import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import io.github.mtrevisan.hunlinter.parsers.enums.AffixOption;
import io.github.mtrevisan.hunlinter.parsers.enums.MorphologicalTag;
import io.github.mtrevisan.hunlinter.services.RegexHelper;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DictionaryEntryFactory{

	private static final String WRONG_FORMAT = "Cannot parse dictionary line `{}`";

	private static final int PARAM_WORD = 1;
	private static final int PARAM_FLAGS = 2;
	private static final int PARAM_MORPHOLOGICAL_FIELDS = 3;
	private static final Pattern PATTERN_ENTRY = RegexHelper.pattern("^(?<word>[^\\s]+?)(?:(?<!\\\\)\\/(?<flags>[^\\s]+))?(?:[\\s]+(?<morphologicalFields>.+))?$");

	private static final String SLASH = "/";
	private static final String SLASH_ESCAPED = "\\/";


	private final AffixData affixData;
	private final FlagParsingStrategy strategy;
	private final List<String> aliasesFlag;
	private final List<String> aliasesMorphologicalField;


	public DictionaryEntryFactory(final AffixData affixData){
		Objects.requireNonNull(affixData, "Affix data cannot be null");

		this.affixData = affixData;
		strategy = affixData.getFlagParsingStrategy();
		aliasesFlag = affixData.getData(AffixOption.ALIASES_FLAG);
		aliasesMorphologicalField = affixData.getData(AffixOption.ALIASES_MORPHOLOGICAL_FIELD);

		Objects.requireNonNull(strategy, "Strategy cannot be null");
	}

	public final DictionaryEntry createFromDictionaryLine(final String line){
		return createFromDictionaryLine(line, true);
	}

	public final DictionaryEntry createFromDictionaryLineNoStemTag(final String line){
		return createFromDictionaryLine(line, false);
	}

	private DictionaryEntry createFromDictionaryLine(final String line, final boolean addStemTag){
		Objects.requireNonNull(line, "Line cannot be null");

		final Matcher m = RegexHelper.matcher(line, PATTERN_ENTRY);
		if(!m.find())
			throw new LinterException(WRONG_FORMAT, line);

		final String word = extractWord(m.group(PARAM_WORD));
		final List<String> continuationFlags = extractContinuationFlags(m.group(PARAM_FLAGS));
		final List<String> morphologicalFields = extractMorphologicalFields(m.group(PARAM_MORPHOLOGICAL_FIELDS), addStemTag, word);

		final String convertedWord = affixData.applyInputConversionTable(word);
		final boolean combinable = true;
		return new DictionaryEntry(convertedWord, continuationFlags, morphologicalFields, combinable);
	}

	private static String extractWord(final String word){
		return StringUtils.replace(word, SLASH_ESCAPED, SLASH);
	}

	private List<String> extractContinuationFlags(final String flagsGroup){
		final String rawFlags = expandAliases(flagsGroup, aliasesFlag);
		final String[] result = strategy.parseFlags(rawFlags);
		return (result != null? Arrays.asList(result): null);
	}

	private List<String> extractMorphologicalFields(final String dicMorphologicalFields, final boolean addStemTag, final String word){
		final String[] split = StringUtils.split(expandAliases(dicMorphologicalFields, aliasesMorphologicalField));
		final List<String> mfs = (split != null? new ArrayList<>(Arrays.asList(split)): new ArrayList<>(0));
		if(addStemTag && !containsStem(mfs))
			mfs.add(0, MorphologicalTag.STEM.attachValue(word));
		return mfs;
	}

	private static String expandAliases(final String part, final List<String> aliases){
		return (aliases != null && !aliases.isEmpty() && NumberUtils.isCreatable(part)
			? aliases.get(Integer.parseInt(part) - 1)
			: part);
	}

	private static boolean containsStem(final List<String> mfs){
		final int size = (mfs != null? mfs.size(): 0);
		for(int i = 0; i < size; i ++)
			if(mfs.get(i).startsWith(MorphologicalTag.STEM.getCode()))
				return true;
		return false;
	}

}
