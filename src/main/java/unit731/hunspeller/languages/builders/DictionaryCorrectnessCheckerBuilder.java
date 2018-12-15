package unit731.hunspeller.languages.builders;

import java.io.IOException;
import unit731.hunspeller.languages.DictionaryCorrectnessChecker;
import unit731.hunspeller.languages.vec.DictionaryCorrectnessCheckerVEC;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.hyphenation.hyphenators.HyphenatorInterface;


public class DictionaryCorrectnessCheckerBuilder{

	private DictionaryCorrectnessCheckerBuilder(){}

	public static DictionaryCorrectnessChecker getCorrectnessChecker(AffixParser affParser, HyphenatorInterface hyphenator) throws IOException{
		DictionaryCorrectnessChecker checker;
		switch(affParser.getLanguage()){
			case DictionaryCorrectnessCheckerVEC.LANGUAGE:
				checker = new DictionaryCorrectnessCheckerVEC(affParser, hyphenator);
				break;

			default:
				checker = new DictionaryCorrectnessChecker(affParser, hyphenator);
		}
		checker.loadRules();
		return checker;
	}

}
