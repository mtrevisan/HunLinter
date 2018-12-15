package unit731.hunspeller.languages.builders;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import unit731.hunspeller.languages.DictionaryCorrectnessChecker;
import unit731.hunspeller.languages.vec.DictionaryCorrectnessCheckerVEC;


public class RulesPropertiesBuilder{

	private static final Map<String, Class<? extends DictionaryCorrectnessChecker>> BASE_CLASSES = new HashMap<>();
	static{
		BASE_CLASSES.put(DictionaryCorrectnessCheckerVEC.LANGUAGE, DictionaryCorrectnessCheckerVEC.class);
	}


	private RulesPropertiesBuilder(){}

	public static Properties getProperties(String language) throws IOException{
		Class<? extends DictionaryCorrectnessChecker> cl = BASE_CLASSES.getOrDefault(language, DictionaryCorrectnessChecker.class);
		InputStream is = cl.getResourceAsStream("rules.properties");

		Properties rulesProperties = new Properties();
		rulesProperties.load(is);
		return rulesProperties;
	}

}
