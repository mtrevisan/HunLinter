package unit731.hunspeller.languages.builders;

import unit731.hunspeller.languages.DictionaryBaseData;
import unit731.hunspeller.languages.vec.CorrectnessCheckerVEC;
import unit731.hunspeller.languages.vec.DictionaryBaseDataVEC;


public class DictionaryBaseDataBuilder{

	private DictionaryBaseDataBuilder(){}

	public static DictionaryBaseData getDictionaryBaseData(String language){
		DictionaryBaseData checker;
		switch(language){
			case CorrectnessCheckerVEC.LANGUAGE:
				checker = DictionaryBaseDataVEC.getInstance();
				break;

			default:
				checker = DictionaryBaseData.getInstance();
		}
		return checker;
	}

}
