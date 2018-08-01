package unit731.hunspeller.languages.builders;

import java.io.File;
import java.nio.charset.Charset;
import unit731.hunspeller.languages.CorrectnessChecker;
import unit731.hunspeller.languages.vec.CorrectnessCheckerVEC;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.hyphenation.hyphenators.AbstractHyphenator;


public class CorrectnessCheckerBuilder{

	public static CorrectnessChecker getParser(String language, AffixParser affParser, File dicFile, AbstractHyphenator hyphenator, WordGenerator wordGenerator,
			Charset charset){
		CorrectnessChecker checker;
		switch(language){
			case CorrectnessCheckerVEC.LANGUAGE:
				checker = new CorrectnessCheckerVEC(affParser, dicFile, hyphenator, wordGenerator, charset);
				break;

			default:
				checker = new CorrectnessChecker(affParser, dicFile, hyphenator, wordGenerator, charset);
		}
		return checker;
	}

}
