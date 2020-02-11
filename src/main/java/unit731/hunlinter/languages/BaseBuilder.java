package unit731.hunlinter.languages;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;
import unit731.hunlinter.collections.bloomfilter.BloomFilterParameters;
import unit731.hunlinter.languages.vec.DictionaryBaseDataVEC;
import unit731.hunlinter.languages.vec.DictionaryCorrectnessCheckerVEC;
import unit731.hunlinter.languages.vec.OrthographyVEC;
import unit731.hunlinter.languages.vec.WordVEC;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.hyphenation.HyphenatorInterface;


public class BaseBuilder{

	public static final Comparator<String> COMPARATOR_LENGTH = Comparator.comparingInt(String::length);
	public static final Comparator<String> COMPARATOR_DEFAULT = Comparator.naturalOrder();

	private static class LanguageData{
		private Class<? extends DictionaryCorrectnessChecker> baseClass;
		private Comparator<String> comparator;
		private BloomFilterParameters dictionaryBaseData;
		private BiFunction<AffixData, HyphenatorInterface, DictionaryCorrectnessChecker> checker;
		private Orthography orthography;
	}

	private static final LanguageData LANGUAGE_DATA_DEFAULT = new LanguageData();
	static{
		LANGUAGE_DATA_DEFAULT.baseClass = DictionaryCorrectnessChecker.class;
		LANGUAGE_DATA_DEFAULT.comparator = COMPARATOR_DEFAULT;
		LANGUAGE_DATA_DEFAULT.dictionaryBaseData = DictionaryBaseData.getInstance();
		LANGUAGE_DATA_DEFAULT.checker = DictionaryCorrectnessChecker::new;
		LANGUAGE_DATA_DEFAULT.orthography = Orthography.getInstance();
	}
	private static final Map<String, LanguageData> DATA = new HashMap<>();
	static{
		LanguageData langData = new LanguageData();
		langData.baseClass = DictionaryCorrectnessCheckerVEC.class;
		langData.comparator = WordVEC.sorterComparator();
		langData.dictionaryBaseData = DictionaryBaseDataVEC.getInstance();
		langData.checker = DictionaryCorrectnessCheckerVEC::new;
		langData.orthography = OrthographyVEC.getInstance();
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

	public static Properties getRulesProperties(final String language){
		final Properties rulesProperties = new Properties();
		final Class<? extends DictionaryCorrectnessChecker> cl = DATA.getOrDefault(language, LANGUAGE_DATA_DEFAULT)
			.baseClass;
		final InputStream is = cl.getResourceAsStream("rules.properties");
		if(is != null){
			try(final InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)){
				if(isr != null)
					rulesProperties.load(isr);
			}
			catch(final IOException ignored){}
		}
		return rulesProperties;
	}

}
