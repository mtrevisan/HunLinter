package unit731.hunspeller.languages.builders;

import java.util.HashMap;
import java.util.Map;
import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.languages.vec.DictionaryCorrectnessCheckerVEC;
import unit731.hunspeller.languages.vec.OrthographyVEC;


public class OrthographyBuilder{

	private static final Map<String, Orthography> ORTHOS = new HashMap<>();
	static{
		ORTHOS.put(DictionaryCorrectnessCheckerVEC.LANGUAGE, OrthographyVEC.getInstance());
	}


	private OrthographyBuilder(){}

	public static Orthography getOrthography(String language){
		return ORTHOS.getOrDefault(language, Orthography.getInstance());
	}

}
