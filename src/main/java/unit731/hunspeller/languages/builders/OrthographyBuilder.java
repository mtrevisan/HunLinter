package unit731.hunspeller.languages.builders;

import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.languages.vec.DictionaryCorrectnessCheckerVEC;
import unit731.hunspeller.languages.vec.OrthographyVEC;


public class OrthographyBuilder{

	private OrthographyBuilder(){}

	public static Orthography getOrthography(String language){
		Orthography ortho;
		switch(language){
			case DictionaryCorrectnessCheckerVEC.LANGUAGE:
				ortho = OrthographyVEC.getInstance();
				break;

			default:
				ortho = Orthography.getInstance();
		}
		return ortho;
	}

}
