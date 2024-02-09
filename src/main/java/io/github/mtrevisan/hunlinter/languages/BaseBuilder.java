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
package io.github.mtrevisan.hunlinter.languages;

import io.github.mtrevisan.hunlinter.datastructures.bloomfilter.BloomFilterParameters;
import io.github.mtrevisan.hunlinter.languages.vec.DictionaryBaseDataVEC;
import io.github.mtrevisan.hunlinter.languages.vec.DictionaryCorrectnessCheckerVEC;
import io.github.mtrevisan.hunlinter.languages.vec.OrthographyVEC;
import io.github.mtrevisan.hunlinter.languages.vec.WordTokenizerVEC;
import io.github.mtrevisan.hunlinter.languages.vec.WordVEC;
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.HyphenatorInterface;
import io.github.mtrevisan.hunlinter.services.RegexHelper;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import io.github.mtrevisan.hunlinter.services.system.PropertiesUTF8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Pattern;


public final class BaseBuilder{

	private static final Logger LOGGER = LoggerFactory.getLogger(BaseBuilder.class);

	private static final String COMPARATOR_WITHOUT_COUNTRY_CODE = "Cannot find comparator for language `{}`, use comparator for `{}` instead";
	private static final String COMPARATOR_DEFAULT_MESSAGE = "Cannot find comparator for language `{}`, use default comparator";

	public static final Comparator<String> COMPARATOR_LENGTH = Comparator.comparingInt(String::length);
	public static final Comparator<String> COMPARATOR_DEFAULT = Comparator.naturalOrder();

	private static final Pattern PATTERN_REPLACEMENT = RegexHelper.pattern("<'_'");

	private static class LanguageData{
		private Class<? extends DictionaryCorrectnessChecker> baseClass;
		private Comparator<String> comparator;
		private BloomFilterParameters dictionaryBaseData;
		private BiFunction<AffixData, HyphenatorInterface, DictionaryCorrectnessChecker> checker;
		private Orthography orthography;
		private WordTokenizer wordTokenizer;
	}

	private static final LanguageData LANGUAGE_DATA_DEFAULT = new LanguageData();
	static{
		LANGUAGE_DATA_DEFAULT.baseClass = DictionaryCorrectnessChecker.class;
		LANGUAGE_DATA_DEFAULT.comparator = COMPARATOR_DEFAULT;
		LANGUAGE_DATA_DEFAULT.dictionaryBaseData = DictionaryBaseData.getInstance();
		LANGUAGE_DATA_DEFAULT.checker = DictionaryCorrectnessChecker::new;
		LANGUAGE_DATA_DEFAULT.orthography = Orthography.getInstance();
		LANGUAGE_DATA_DEFAULT.wordTokenizer = new WordTokenizer();
	}
	private static final Map<String, LanguageData> DATA = new HashMap<>(1);
	static{
		final LanguageData langData = new LanguageData();
		langData.baseClass = DictionaryCorrectnessCheckerVEC.class;
		langData.comparator = WordVEC.sorterComparator();
		langData.dictionaryBaseData = DictionaryBaseDataVEC.getInstance();
		langData.checker = DictionaryCorrectnessCheckerVEC::new;
		langData.orthography = OrthographyVEC.getInstance();
		langData.wordTokenizer = new WordTokenizerVEC();
		DATA.put(DictionaryCorrectnessCheckerVEC.LANGUAGE, langData);
	}


	private BaseBuilder(){}

	/**
	 * Retrieves the comparator for sorting strings in a specified language.
	 *
	 * @param language	The language code.
	 * @return	The comparator for the specified language, or a default comparator if the specified language is not supported.
	 */
	public static Comparator<String> getComparator(final String language){
		LanguageData languageData = DATA.get(language);
		if(languageData == null && language != null && language.contains("-")){
			final String realLanguageCode = language.substring(0, language.indexOf('-'));
			languageData = DATA.get(realLanguageCode);
		}
		if(languageData == null){
			languageData = LANGUAGE_DATA_DEFAULT;

			if(language != null){
				Collator collator = Collator.getInstance(Locale.forLanguageTag(language));

				//make ordering per-word
				if(collator instanceof RuleBasedCollator ruleBasedCollator){
					try{
						//insert a collation rule to sort the space character before the underscore
						final String rules = ruleBasedCollator.getRules();
						collator = new RuleBasedCollator(RegexHelper.replaceAll(rules, PATTERN_REPLACEMENT, "<' '='\t'<'_'"));
					}
					catch(final ParseException ignored){}
				}

				languageData.comparator = collator::compare;
			}
		}
		return languageData.comparator;
	}

	/**
	 * Checks the default comparator for sorting strings in a specified language.
	 *
	 * @param language	The language code.
	 */
	public static void checkDefaultComparator(final String language){
		LanguageData languageData = DATA.get(language);
		if(languageData == null && language != null && language.contains("-")){
			final String realLanguageCode = language.substring(0, language.indexOf('-'));
			languageData = DATA.get(realLanguageCode);

			if(languageData != null)
				LOGGER.warn(ParserManager.MARKER_APPLICATION, JavaHelper.textFormat(COMPARATOR_WITHOUT_COUNTRY_CODE, language, realLanguageCode));
		}
		if(languageData == null)
			LOGGER.warn(ParserManager.MARKER_APPLICATION, JavaHelper.textFormat(COMPARATOR_DEFAULT_MESSAGE, language));
	}

	/**
	 * Retrieves the dictionary base data for a specified language.
	 *
	 * @param language	The language code.
	 * @return	The Bloom filter parameters containing the dictionary base data for the specified language.
	 */
	public static BloomFilterParameters getDictionaryBaseData(final String language){
		return DATA.getOrDefault(language, LANGUAGE_DATA_DEFAULT)
			.dictionaryBaseData;
	}

	/**
	 * Retrieves the correctness checker for a given affix data and hyphenator.
	 *
	 * @param affixData	The affix data for which to retrieve the correctness checker.
	 * @param hyphenator	The hyphenator interface used by the correctness checker.
	 * @return	The {@link DictionaryCorrectnessChecker} instance for the specified affixData and hyphenator.
	 */
	public static DictionaryCorrectnessChecker getCorrectnessChecker(final AffixData affixData,
			final HyphenatorInterface hyphenator){
		final DictionaryCorrectnessChecker checker = DATA.getOrDefault(affixData.getLanguage(), LANGUAGE_DATA_DEFAULT)
			.checker.apply(affixData, hyphenator);
		checker.loadRules();
		return checker;
	}

	/**
	 * Retrieves the orthography for a given language.
	 *
	 * @param language	The language code.
	 * @return	The orthography for the specified language, or the default orthography if the specified language is not supported.
	 */
	public static Orthography getOrthography(final String language){
		return DATA.getOrDefault(language, LANGUAGE_DATA_DEFAULT)
			.orthography;
	}

	/**
	 * Retrieves the {@link WordTokenizer} for a specified language.
	 *
	 * @param language	The language code for which to retrieve the {@link WordTokenizer}.
	 * @return	The {@link WordTokenizer} for the specified language, or a default {@link WordTokenizer} if the specified language is not
	 * 	supported.
	 */
	public static WordTokenizer getWordTokenizer(final String language){
		return DATA.getOrDefault(language, LANGUAGE_DATA_DEFAULT)
			.wordTokenizer;
	}

	/**
	 * Retrieves the rules properties for a given language.
	 *
	 * @param language	The language code for which to retrieve the rules properties.
	 * @return	The rules properties for the specified language.
	 */
	public static PropertiesUTF8 getRulesProperties(final String language){
		final PropertiesUTF8 rulesProperties = new PropertiesUTF8();
		final Class<? extends DictionaryCorrectnessChecker> cl = DATA.getOrDefault(language, LANGUAGE_DATA_DEFAULT)
			.baseClass;
		final InputStream is = cl.getResourceAsStream("rules.properties");
		if(is != null){
			try{
				rulesProperties.load(is);
			}
			catch(final IOException ioe){
				LOGGER.error("Error while reading rules", ioe);
			}
		}
		return rulesProperties;
	}

}
