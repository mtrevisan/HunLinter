package unit731.hunlinter.parsers.dictionary.generators;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.affix.AffixParser;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.services.FileHelper;

import java.io.File;
import java.io.IOException;
import java.util.List;


class WordMuncherTest{

	@Test
	void simpleOriginator() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"SFX a Y 1",
			"SFX a 0 a .",
			"SFX b Y 1",
			"SFX b 0 b ."
		);
		File dicFile = FileHelper.createDeleteOnExitFile(language, ".dic",
			"1",
			"a");
		String line = "ab";
		Pair<WordMuncher, DictionaryEntry> pair = createMuncher(affFile, dicFile, language, line);
		WordMuncher muncher = pair.getLeft();
		DictionaryEntry dicEntry = pair.getRight();
		final List<DictionaryEntry> originators = muncher.inferAffixRules(dicEntry);

		Assertions.assertEquals(1, originators.size());
		Assertions.assertEquals("a/b", originators.get(0).toString());
	}

	@Test
	void multipleOriginator() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"SFX a Y 1",
			"SFX a 0 b .",
			"SFX b Y 1",
			"SFX b 0 b ."
		);
		File dicFile = FileHelper.createDeleteOnExitFile(language, ".dic",
			"1",
			"a");
		String line = "ab";
		Pair<WordMuncher, DictionaryEntry> pair = createMuncher(affFile, dicFile, language, line);
		WordMuncher muncher = pair.getLeft();
		DictionaryEntry dicEntry = pair.getRight();
		final List<DictionaryEntry> originators = muncher.inferAffixRules(dicEntry);

		Assertions.assertEquals(2, originators.size());
		Assertions.assertEquals("a/a", originators.get(0).toString());
		Assertions.assertEquals("a/b", originators.get(1).toString());
	}

	@Test
	void multipleOriginatorPrefixAndSuffix() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"SFX a Y 1",
			"SFX a 0 b .",
			"PFX b Y 1",
			"PFX b 0 b ."
		);
		File dicFile = FileHelper.createDeleteOnExitFile(language, ".dic",
			"2",
			"ab",
			"ba");
		String line = "bab";
		Pair<WordMuncher, DictionaryEntry> pair = createMuncher(affFile, dicFile, language, line);
		WordMuncher muncher = pair.getLeft();
		DictionaryEntry dicEntry = pair.getRight();
		final List<DictionaryEntry> originators = muncher.inferAffixRules(dicEntry);

		Assertions.assertEquals(2, originators.size());
		Assertions.assertEquals("ba/a", originators.get(0).toString());
		Assertions.assertEquals("ab/b", originators.get(1).toString());
	}

	@Test
	void multipleOriginators() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"SFX a Y 2",
			"SFX a 0 b .",
			"SFX a 0 bb ."
		);
		File dicFile = FileHelper.createDeleteOnExitFile(language, ".dic",
			"0");
		String line = "abb";
		Pair<WordMuncher, DictionaryEntry> pair = createMuncher(affFile, dicFile, language, line);
		WordMuncher muncher = pair.getLeft();
		DictionaryEntry dicEntry = pair.getRight();
		final List<DictionaryEntry> originators = muncher.inferAffixRules(dicEntry);

		Assertions.assertTrue(originators.isEmpty());
	}

	@Test
	void notContainedIntoDictionary() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"SFX a Y 1",
			"SFX a 0 b .",
			"PFX b Y 1",
			"PFX b 0 b ."
		);
		File dicFile = FileHelper.createDeleteOnExitFile(language, ".dic",
			"1",
			"ba");
		String line = "bab";
		Pair<WordMuncher, DictionaryEntry> pair = createMuncher(affFile, dicFile, language, line);
		WordMuncher muncher = pair.getLeft();
		DictionaryEntry dicEntry = pair.getRight();
		final List<DictionaryEntry> originators = muncher.inferAffixRules(dicEntry);

		Assertions.assertEquals(1, originators.size());
		Assertions.assertEquals("ba/a", originators.get(0).toString());
	}


	private Pair<WordMuncher, DictionaryEntry> createMuncher(final File affFile, final File dicFile, final String language, final String line) throws IOException{
		AffixParser affParser = new AffixParser();
		affParser.parse(affFile, language);
		AffixData affixData = affParser.getAffixData();
		DictionaryParser dicParser = new DictionaryParser(dicFile, affixData.getLanguage(), affixData.getCharset());
		WordGenerator wordGenerator = new WordGenerator(affixData, dicParser);
		WordMuncher muncher = new WordMuncher(affixData, dicParser, wordGenerator);
		final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(line, affixData);
		return Pair.of(muncher, dicEntry);
	}

}
