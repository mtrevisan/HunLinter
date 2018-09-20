package unit731.hunspeller.languages.builders;

import unit731.hunspeller.languages.CorrectnessChecker;
import unit731.hunspeller.languages.vec.CorrectnessCheckerVEC;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.hyphenation.hyphenators.AbstractHyphenator;


public class CorrectnessCheckerBuilder{

	private CorrectnessCheckerBuilder(){}

	public static CorrectnessChecker getParser(AffixParser affParser, AbstractHyphenator hyphenator){
		CorrectnessChecker checker;
		switch(affParser.getLanguage()){
			case CorrectnessCheckerVEC.LANGUAGE:
				checker = new CorrectnessCheckerVEC(affParser, hyphenator);
				break;

			default:
				checker = new CorrectnessChecker(affParser, hyphenator);
		}
		return checker;
	}

}
