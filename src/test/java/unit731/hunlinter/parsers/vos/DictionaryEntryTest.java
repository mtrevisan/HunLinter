package unit731.hunlinter.parsers.vos;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.parsers.affix.AffixParser;
import unit731.hunlinter.services.FileHelper;


class DictionaryEntryTest{

	@Test
	void parse() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"NEEDAFFIX C",
			"SFX A Y 1",
			"SFX A 0 a .",
			"SFX B Y 1",
			"SFX B 0 b .");
		AffixParser affParser = new AffixParser();
		affParser.parse(affFile, language);


		String line = "abcdef";
		DictionaryEntry entry = DictionaryEntry.createFromDictionaryLine(line, affParser.getAffixData());

		Assertions.assertEquals("abcdef", entry.getWord());
		Assertions.assertNull(entry.continuationFlags);
		Assertions.assertArrayEquals(new String[]{"st:abcdef"}, entry.morphologicalFields);


		line = "abcdef/ABC";
		entry = DictionaryEntry.createFromDictionaryLine(line, affParser.getAffixData());

		Assertions.assertEquals("abcdef", entry.word);
		Assertions.assertArrayEquals(new String[]{"A", "B", "C"}, entry.continuationFlags);
		Assertions.assertArrayEquals(new String[]{"st:abcdef"}, entry.morphologicalFields);


		line = "abcdef	po:noun";
		entry = DictionaryEntry.createFromDictionaryLine(line, affParser.getAffixData());

		Assertions.assertEquals("abcdef", entry.word);
		Assertions.assertNull(entry.continuationFlags);
		Assertions.assertArrayEquals(new String[]{"st:abcdef", "po:noun"}, entry.morphologicalFields);


		line = "abcdef/ABC	po:noun";
		entry = DictionaryEntry.createFromDictionaryLine(line, affParser.getAffixData());

		Assertions.assertEquals("abcdef", entry.word);
		Assertions.assertArrayEquals(new String[]{"A", "B", "C"}, entry.continuationFlags);
		Assertions.assertArrayEquals(new String[]{"st:abcdef", "po:noun"}, entry.morphologicalFields);


		line = "abc\\/def";
		entry = DictionaryEntry.createFromDictionaryLine(line, affParser.getAffixData());

		Assertions.assertEquals("abc/def", entry.getWord());
		Assertions.assertNull(entry.continuationFlags);
		Assertions.assertArrayEquals(new String[]{"st:abc/def"}, entry.morphologicalFields);


		line = "abc\\/def/ABC";
		entry = DictionaryEntry.createFromDictionaryLine(line, affParser.getAffixData());

		Assertions.assertEquals("abc/def", entry.word);
		Assertions.assertArrayEquals(new String[]{"A", "B", "C"}, entry.continuationFlags);
		Assertions.assertArrayEquals(new String[]{"st:abc/def"}, entry.morphologicalFields);


		line = "abc\\/def	po:noun";
		entry = DictionaryEntry.createFromDictionaryLine(line, affParser.getAffixData());

		Assertions.assertEquals("abc/def", entry.word);
		Assertions.assertNull(entry.continuationFlags);
		Assertions.assertArrayEquals(new String[]{"st:abc/def", "po:noun"}, entry.morphologicalFields);


		line = "abc\\/def/ABC	po:noun";
		entry = DictionaryEntry.createFromDictionaryLine(line, affParser.getAffixData());

		Assertions.assertEquals("abc/def", entry.word);
		Assertions.assertArrayEquals(new String[]{"A", "B", "C"}, entry.continuationFlags);
		Assertions.assertArrayEquals(new String[]{"st:abc/def", "po:noun"}, entry.morphologicalFields);
	}

}
