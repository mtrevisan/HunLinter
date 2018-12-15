package unit731.hunspeller.languages.builders;

import unit731.hunspeller.languages.DictionaryCorrectnessChecker;
import unit731.hunspeller.languages.vec.DictionaryCorrectnessCheckerVEC;


public class BaseClassBuilder{

	private BaseClassBuilder(){}

	public static Class<? extends DictionaryCorrectnessChecker> getBaseClass(String language){
		Class<? extends DictionaryCorrectnessChecker> klazz;
		switch(language){
			case DictionaryCorrectnessCheckerVEC.LANGUAGE:
				klazz = DictionaryCorrectnessCheckerVEC.class;
				break;

			default:
				klazz = DictionaryCorrectnessChecker.class;
		}
		return klazz;
	}

}
