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
package io.github.mtrevisan.hunlinter.languages;

import io.github.mtrevisan.hunlinter.datastructures.bloomfilter.BloomFilterParameters;
import io.github.mtrevisan.hunlinter.languages.vec.DictionaryCorrectnessCheckerVEC;
import io.github.mtrevisan.hunlinter.languages.vec.OrthographyVEC;
import io.github.mtrevisan.hunlinter.languages.vec.WordTokenizerVEC;
import io.github.mtrevisan.hunlinter.languages.vec.WordVEC;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.languages.vec.DictionaryBaseDataVEC;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.HyphenatorInterface;
import io.github.mtrevisan.hunlinter.services.system.PropertiesUTF8;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;


public final class BaseBuilder{

	public static final Comparator<String> COMPARATOR_LENGTH = Comparator.comparingInt(String::length);
	public static final Comparator<String> COMPARATOR_DEFAULT = Comparator.naturalOrder();

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
	private static final Map<String, LanguageData> DATA = new HashMap<>();
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

	public static Comparator<String> getComparator(final String language){
		return DATA.getOrDefault(language, LANGUAGE_DATA_DEFAULT)
			.comparator;
	}

	public static BloomFilterParameters getDictionaryBaseData(final String language){
		return DATA.getOrDefault(language, LANGUAGE_DATA_DEFAULT)
			.dictionaryBaseData;
	}

	public static DictionaryCorrectnessChecker getCorrectnessChecker(final AffixData affixData,
			final HyphenatorInterface hyphenator){
		final DictionaryCorrectnessChecker checker = DATA.getOrDefault(affixData.getLanguage(), LANGUAGE_DATA_DEFAULT)
			.checker.apply(affixData, hyphenator);
		checker.loadRules();
		return checker;
	}

	public static Orthography getOrthography(final String language){
		return DATA.getOrDefault(language, LANGUAGE_DATA_DEFAULT)
			.orthography;
	}

	public static WordTokenizer getWordTokenizer(final String language){
		return DATA.getOrDefault(language, LANGUAGE_DATA_DEFAULT)
			.wordTokenizer;
	}

	public static PropertiesUTF8 getRulesProperties(final String language){
		final PropertiesUTF8 rulesProperties = new PropertiesUTF8();
		final Class<? extends DictionaryCorrectnessChecker> cl = DATA.getOrDefault(language, LANGUAGE_DATA_DEFAULT)
			.baseClass;
		final InputStream is = cl.getResourceAsStream("rules.properties");
		if(is != null){
			try(final InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)){
				rulesProperties.load(isr);
			}
			catch(final IOException ignored){}
		}
		return rulesProperties;
	}

}
