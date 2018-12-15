package unit731.hunspeller.languages.builders;

import java.util.HashMap;
import java.util.Map;
import unit731.hunspeller.languages.DictionaryBaseData;
import unit731.hunspeller.languages.vec.DictionaryCorrectnessCheckerVEC;
import unit731.hunspeller.languages.vec.DictionaryBaseDataVEC;


public class DictionaryBaseDataBuilder{

	private static final Map<String, DictionaryBaseData> CHECKERS = new HashMap<>();
	static{
		CHECKERS.put(DictionaryCorrectnessCheckerVEC.LANGUAGE, DictionaryBaseDataVEC.getInstance());
	}


	private DictionaryBaseDataBuilder(){}

	public static DictionaryBaseData getDictionaryBaseData(String language){
		return CHECKERS.getOrDefault(language, DictionaryBaseData.getInstance());
	}

}
