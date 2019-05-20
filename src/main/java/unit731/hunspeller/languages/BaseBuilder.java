package unit731.hunspeller.languages;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;
import unit731.hunspeller.collections.bloomfilter.BloomFilterParameters;
import unit731.hunspeller.languages.vec.DictionaryBaseDataVEC;
import unit731.hunspeller.languages.vec.DictionaryCorrectnessCheckerVEC;
import unit731.hunspeller.languages.vec.OrthographyVEC;
import unit731.hunspeller.languages.vec.WordVEC;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.hyphenation.hyphenators.HyphenatorInterface;


public class BaseBuilder{

	public static final Comparator<String> COMPARATOR_LENGTH = (r1, r2) -> Integer.compare(r1.length(), r2.length());
	public static final Comparator<String> COMPARATOR_DEFAULT = (r1, r2) -> r1.compareTo(r2);

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
		LANGUAGE_DATA_DEFAULT.checker = (affixData, hyphenator) -> new DictionaryCorrectnessChecker(affixData, hyphenator);
		LANGUAGE_DATA_DEFAULT.orthography = Orthography.getInstance();
	}
	private static final Map<String, LanguageData> DATAS = new HashMap<>();
	static{
		LanguageData langData = new LanguageData();
		langData.baseClass = DictionaryCorrectnessCheckerVEC.class;
		langData.comparator = WordVEC.sorterComparator();
		langData.dictionaryBaseData = DictionaryBaseDataVEC.getInstance();
		langData.checker = (affixData, hyphenator) -> new DictionaryCorrectnessCheckerVEC(affixData, hyphenator);
		langData.orthography = OrthographyVEC.getInstance();
		DATAS.put(DictionaryCorrectnessCheckerVEC.LANGUAGE, langData);
	}


	private BaseBuilder(){}

	public static Comparator<String> getComparator(final String language){
		return DATAS.getOrDefault(language, LANGUAGE_DATA_DEFAULT)
			.comparator;
	}

	public static BloomFilterParameters getDictionaryBaseData(final String language){
		return DATAS.getOrDefault(language, LANGUAGE_DATA_DEFAULT)
			.dictionaryBaseData;
	}

	public static DictionaryCorrectnessChecker getCorrectnessChecker(final AffixData affixData, final HyphenatorInterface hyphenator)
			throws IOException{
		final DictionaryCorrectnessChecker checker = DATAS.getOrDefault(affixData.getLanguage(), LANGUAGE_DATA_DEFAULT)
			.checker.apply(affixData, hyphenator);
		checker.loadRules();
		return checker;
	}

	public static Orthography getOrthography(final String language){
		return DATAS.getOrDefault(language, LANGUAGE_DATA_DEFAULT)
			.orthography;
	}

	public static Properties getRulesProperties(final String language) throws IOException{
		final Properties rulesProperties = new Properties();
		final Class<? extends DictionaryCorrectnessChecker> cl = DATAS.getOrDefault(language, LANGUAGE_DATA_DEFAULT)
			.baseClass;
		try(final InputStream is = cl.getResourceAsStream("rules.properties")){
			if(is != null)
				rulesProperties.load(is);
		}
		return rulesProperties;
	}

}
