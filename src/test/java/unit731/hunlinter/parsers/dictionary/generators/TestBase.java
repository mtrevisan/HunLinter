package unit731.hunlinter.parsers.dictionary.generators;

import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.affix.AffixParser;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.vos.Production;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;


class TestBase{

	protected AffixData affixData;
	protected WordGenerator wordGenerator;


	protected void loadData(File affFile, String language) throws IOException{
		AffixParser affParser = new AffixParser();
		affParser.parse(affFile, language);
		affixData = affParser.getAffixData();
		wordGenerator = new WordGenerator(affixData, null);
	}

	protected void loadData(File affFile, File dicFile, String language) throws IOException{
		AffixParser affParser = new AffixParser();
		affParser.parse(affFile, language);
		affixData = affParser.getAffixData();
		Charset charset = affixData.getCharset();
		DictionaryParser dicParser = new DictionaryParser(dicFile, language, charset);
		wordGenerator = new WordGenerator(affixData, dicParser);
	}

	protected Production createProduction(String word, String continuationFlags, String morphologicalFields){
		FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();
		return new Production(word, continuationFlags, morphologicalFields, null, strategy);
	}

}
