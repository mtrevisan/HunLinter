package unit731.hunspeller.parsers.dictionary.generators;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.vos.DictionaryEntry;
import unit731.hunspeller.parsers.vos.Production;
import unit731.hunspeller.services.FileHelper;

import java.io.File;
import java.io.IOException;
import java.util.List;


class WordMuncherTest{

	@Test
	void simpleOriginator() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"SFX a Y 1",
			"SFX a 0 a .",
			"SFX b Y 1",
			"SFX b 0 b ."
		);
		String line = "ab\tpo:noun";
		AffixParser affParser = new AffixParser();
		affParser.parse(affFile);
		AffixData affixData = affParser.getAffixData();
		WordMuncher muncher = new WordMuncher(affixData, null, null);
		final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(line, affixData);
		final List<Production> originators = muncher.extractAllAffixes(dicEntry);

		Assertions.assertEquals(1, originators.size());
		final Production originator = originators.get(0);
		Assertions.assertEquals("a	from	b", originator.toString());
	}

	//	@Test
	void simpleOriginatorWithCompatiblePartOfSpeech() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"SFX a Y 1",
			"SFX a 0 a .	po:noun",
			"SFX b Y 1",
			"SFX b 0 b .	po:noun"
		);
		String line = "ab\tpo:noun";
		AffixParser affParser = new AffixParser();
		affParser.parse(affFile);
		AffixData affixData = affParser.getAffixData();
		WordMuncher muncher = new WordMuncher(affixData, null, null);
		final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(line, affixData);
		final List<Production> originators = muncher.extractAllAffixes(dicEntry);

		Assertions.assertEquals(1, originators.size());
		final Production originator = originators.get(0);
		Assertions.assertEquals("a	po:noun	from	b", originator.toString());
	}

	@Test
	void simpleOriginatorWithNonCompatiblePartOfSpeech() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"SFX a Y 1",
			"SFX a 0 a .	po:adjective",
			"SFX b Y 1",
			"SFX b 0 b .	po:adjective"
		);
		String line = "ab\tpo:noun";
		AffixParser affParser = new AffixParser();
		affParser.parse(affFile);
		AffixData affixData = affParser.getAffixData();
		WordMuncher muncher = new WordMuncher(affixData, null, null);
		final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(line, affixData);
		final List<Production> originators = muncher.extractAllAffixes(dicEntry);

		Assertions.assertEquals(0, originators.size());
	}

}
