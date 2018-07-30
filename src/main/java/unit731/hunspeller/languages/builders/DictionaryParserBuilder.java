package unit731.hunspeller.languages.builders;

import java.io.File;
import java.nio.charset.Charset;
import unit731.hunspeller.languages.vec.DictionaryParserVEC;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.hyphenation.hyphenators.AbstractHyphenator;


public class DictionaryParserBuilder{

	private static final String LANGUAGE_VENETAN = "vec";


	public static DictionaryParser getParser(String language, File dicFile, WordGenerator wordGenerator, AbstractHyphenator hyphenator, Charset charset){
		DictionaryParser parser;
		switch(language){
			case LANGUAGE_VENETAN:
				parser = new DictionaryParserVEC(dicFile, wordGenerator, hyphenator, charset);
				break;

			default:
				parser = new DictionaryParser(dicFile, wordGenerator, hyphenator, charset);
		}
		return parser;
	}

}
