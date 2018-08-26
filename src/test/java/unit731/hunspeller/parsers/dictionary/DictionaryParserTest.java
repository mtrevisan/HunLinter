package unit731.hunspeller.parsers.dictionary;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.services.FileHelper;


public class DictionaryParserTest{

	private final AffixParser affParser = new AffixParser();


	@Test
	public void replacementTable() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"REP 4",
			"REP ^a aa",
			"REP e$ ee",
			"REP ij IJ",
			"REP alot a_lot");
		affParser.parse(affFile);

		Map<String, String> replacementTable = affParser.getData(AffixTag.REPLACEMENT_TABLE);

		Map<String, String> expected = new HashMap<>();
		expected.put("^a", "aa");
		expected.put("e$", "ee");
		expected.put("ij", "IJ");
		expected.put("alot", "a lot");
		Assert.assertEquals(expected, replacementTable);

		String replaced = affParser.applyReplacementTable("blea");
		Assert.assertEquals("blea", replaced);

		replaced = affParser.applyReplacementTable("abcijde");
		Assert.assertEquals("aabcIJdee", replaced);

		replaced = affParser.applyReplacementTable("alot");
		Assert.assertEquals("aa lot", replaced);
	}

}
