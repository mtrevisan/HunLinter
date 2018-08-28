package unit731.hunspeller.languages.builders;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import unit731.hunspeller.languages.DictionaryBaseData;
import unit731.hunspeller.languages.vec.CorrectnessCheckerVEC;
import unit731.hunspeller.languages.vec.DictionaryBaseDataVEC;


@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DictionaryBaseDataBuilder{

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
