package unit731.hunspeller.languages.builders;

import unit731.hunspeller.languages.CorrectnessChecker;
import unit731.hunspeller.languages.vec.CorrectnessCheckerVEC;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.hyphenation.hyphenators.AbstractHyphenator;


public class CorrectnessCheckerBuilder{

	public static CorrectnessChecker getParser(String language, AffixParser affParser, AbstractHyphenator hyphenator){
		CorrectnessChecker checker;
		switch(language){
			case CorrectnessCheckerVEC.LANGUAGE:
				checker = new CorrectnessCheckerVEC(affParser, hyphenator);
				break;

			default:
				checker = new CorrectnessChecker(affParser, hyphenator);
		}
		return checker;
	}

}
