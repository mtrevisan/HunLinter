package unit731.hunspeller.parsers.dictionary.generators;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.vos.DictionaryEntry;
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
		String line = "ab";
		Pair<AffixData, WordGenerator> things = createThings(affFile);
		AffixData affixData = things.getLeft();
		WordGenerator wordGenerator = things.getRight();
		WordMuncher muncher = new WordMuncher(affixData, null, wordGenerator);
		final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(line, affixData);
		final List<DictionaryEntry> originators = muncher.extractAllAffixes(dicEntry);

		Assertions.assertEquals(1, originators.size());
		Assertions.assertEquals("a/b", originators.get(0).toString());
	}

	@Test
	void simpleOriginatorWithCompatiblePartOfSpeech() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"SFX a Y 1",
			"SFX a 0 a .	po:noun",
			"SFX b Y 1",
			"SFX b 0 b .	po:noun"
		);
		String line = "ab	po:noun";
		Pair<AffixData, WordGenerator> things = createThings(affFile);
		AffixData affixData = things.getLeft();
		WordGenerator wordGenerator = things.getRight();
		WordMuncher muncher = new WordMuncher(affixData, null, wordGenerator);
		final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(line, affixData);
		final List<DictionaryEntry> originators = muncher.extractAllAffixes(dicEntry);

		Assertions.assertEquals(1, originators.size());
		final DictionaryEntry originator = originators.get(0);
		Assertions.assertEquals("a/b", originator.toString());
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
		String line = "ab	po:noun";
		Pair<AffixData, WordGenerator> things = createThings(affFile);
		AffixData affixData = things.getLeft();
		WordGenerator wordGenerator = things.getRight();
		WordMuncher muncher = new WordMuncher(affixData, null, wordGenerator);
		final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(line, affixData);
		final List<DictionaryEntry> originators = muncher.extractAllAffixes(dicEntry);

		Assertions.assertTrue(originators.isEmpty());
	}

	@Test
	void multipleOriginator() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"SFX a Y 1",
			"SFX a 0 b .",
			"SFX b Y 1",
			"SFX b 0 b ."
		);
		String line = "ab";
		Pair<AffixData, WordGenerator> things = createThings(affFile);
		AffixData affixData = things.getLeft();
		WordGenerator wordGenerator = things.getRight();
		WordMuncher muncher = new WordMuncher(affixData, null, wordGenerator);
		final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(line, affixData);
		final List<DictionaryEntry> originators = muncher.extractAllAffixes(dicEntry);

		Assertions.assertEquals(2, originators.size());
		Assertions.assertEquals("a/a", originators.get(0).toString());
		Assertions.assertEquals("a/b", originators.get(1).toString());
	}

	@Test
	void multipleOriginatorPrefixAndSuffix() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"SFX a Y 1",
			"SFX a 0 b .",
			"PFX b Y 1",
			"PFX b 0 b ."
		);
		String line = "bab";
		Pair<AffixData, WordGenerator> things = createThings(affFile);
		AffixData affixData = things.getLeft();
		WordGenerator wordGenerator = things.getRight();
		WordMuncher muncher = new WordMuncher(affixData, null, wordGenerator);
		final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(line, affixData);
		final List<DictionaryEntry> originators = muncher.extractAllAffixes(dicEntry);

		Assertions.assertEquals(2, originators.size());
		Assertions.assertEquals("ba/a", originators.get(0).toString());
		Assertions.assertEquals("ab/b", originators.get(1).toString());
	}

	@Test
	void multipleOriginators() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"SFX a Y 2",
			"SFX a 0 b .",
			"SFX a 0 bb ."
		);
		String line = "abb";
		Pair<AffixData, WordGenerator> things = createThings(affFile);
		AffixData affixData = things.getLeft();
		WordGenerator wordGenerator = things.getRight();
		WordMuncher muncher = new WordMuncher(affixData, null, wordGenerator);
		final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(line, affixData);
		final List<DictionaryEntry> originators = muncher.extractAllAffixes(dicEntry);

		Assertions.assertTrue(originators.isEmpty());
	}


	private Pair<AffixData, WordGenerator> createThings(File affFile) throws IOException{
		AffixParser affParser = new AffixParser();
		affParser.parse(affFile);
		AffixData affixData = affParser.getAffixData();
		File dicFile = FileHelper.getTemporaryUTF8File("xxx", ".dic",
			"0");
		DictionaryParser dicParser = new DictionaryParser(dicFile, affixData.getLanguage(), affixData.getCharset());
		WordGenerator wordGenerator = new WordGenerator(affixData, dicParser);
		return Pair.of(affixData, wordGenerator);
	}

}
