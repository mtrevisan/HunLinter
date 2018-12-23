package unit731.hunspeller.parsers.dictionary.vos;

import java.io.File;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.services.FileHelper;


public class DictionaryEntryTest{

	@Test
	public void parse() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"NEEDAFFIX C",
			"SFX A Y 1",
			"SFX A 0 a .",
			"SFX B Y 1",
			"SFX B 0 b .");
		AffixParser affParser = new AffixParser();
		affParser.parse(affFile);


		String line = "abcdef";
		DictionaryEntry entry = DictionaryEntry.createFromDictionaryLine(line, affParser.getFlagParsingStrategy());

		Assert.assertEquals("abcdef", entry.getWord());
		Assert.assertNull(entry.continuationFlags);
		Assert.assertArrayEquals(new String[]{"st:abcdef"}, entry.morphologicalFields);


		line = "abcdef/ABC";
		entry = DictionaryEntry.createFromDictionaryLine(line, affParser.getFlagParsingStrategy());

		Assert.assertEquals("abcdef", entry.word);
		Assert.assertArrayEquals(new String[]{"A", "B", "C"}, entry.continuationFlags);
		Assert.assertArrayEquals(new String[]{"st:abcdef"}, entry.morphologicalFields);


		line = "abcdef	po:noun";
		entry = DictionaryEntry.createFromDictionaryLine(line, affParser.getFlagParsingStrategy());

		Assert.assertEquals("abcdef", entry.word);
		Assert.assertNull(entry.continuationFlags);
		Assert.assertArrayEquals(new String[]{"st:abcdef", "po:noun"}, entry.morphologicalFields);


		line = "abcdef/ABC	po:noun";
		entry = DictionaryEntry.createFromDictionaryLine(line, affParser.getFlagParsingStrategy());

		Assert.assertEquals("abcdef", entry.word);
		Assert.assertArrayEquals(new String[]{"A", "B", "C"}, entry.continuationFlags);
		Assert.assertArrayEquals(new String[]{"st:abcdef", "po:noun"}, entry.morphologicalFields);


		line = "abc\\/def";
		entry = DictionaryEntry.createFromDictionaryLine(line, affParser.getFlagParsingStrategy());

		Assert.assertEquals("abc/def", entry.getWord());
		Assert.assertNull(entry.continuationFlags);
		Assert.assertArrayEquals(new String[]{"st:abc/def"}, entry.morphologicalFields);


		line = "abc\\/def/ABC";
		entry = DictionaryEntry.createFromDictionaryLine(line, affParser.getFlagParsingStrategy());

		Assert.assertEquals("abc/def", entry.word);
		Assert.assertArrayEquals(new String[]{"A", "B", "C"}, entry.continuationFlags);
		Assert.assertArrayEquals(new String[]{"st:abc/def"}, entry.morphologicalFields);


		line = "abc\\/def	po:noun";
		entry = DictionaryEntry.createFromDictionaryLine(line, affParser.getFlagParsingStrategy());

		Assert.assertEquals("abc/def", entry.word);
		Assert.assertNull(entry.continuationFlags);
		Assert.assertArrayEquals(new String[]{"st:abc/def", "po:noun"}, entry.morphologicalFields);


		line = "abc\\/def/ABC	po:noun";
		entry = DictionaryEntry.createFromDictionaryLine(line, affParser.getFlagParsingStrategy());

		Assert.assertEquals("abc/def", entry.word);
		Assert.assertArrayEquals(new String[]{"A", "B", "C"}, entry.continuationFlags);
		Assert.assertArrayEquals(new String[]{"st:abc/def", "po:noun"}, entry.morphologicalFields);
	}

}
