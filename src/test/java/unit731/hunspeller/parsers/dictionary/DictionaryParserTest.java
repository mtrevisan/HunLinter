package unit731.hunspeller.parsers.dictionary;

import java.io.File;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.parsers.affix.ConversionTable;
import unit731.hunspeller.services.FileHelper;


public class DictionaryParserTest{

	private final AffixParser affParser = new AffixParser();


	@Test
	public void replacementTable() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"REP 4",
			"REP ^b bb",
			"REP e$ ee",
			"REP ij IJ",
			"REP alot a_lot");
		affParser.parse(affFile);

		ConversionTable table = affParser.getData(AffixTag.REPLACEMENT_TABLE);
		Assert.assertEquals("[affixTag=REPLACEMENT_TABLE,table=[(^b,bb), (e$,ee), (ij,IJ), (alot,a lot)]]", table.toString());

		String replaced = affParser.applyReplacementTable("clea");
		Assert.assertEquals("clea", replaced);

		replaced = affParser.applyReplacementTable("bcijde");
		Assert.assertEquals("bbcIJdee", replaced);

		replaced = affParser.applyReplacementTable("alot");
		Assert.assertEquals("a lot", replaced);
	}

	@Test
	public void applyLongest() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"REP 3",
			"REP b 1",
			"REP bac 3",
			"REP ba 2");
		affParser.parse(affFile);

		String replaced = affParser.applyReplacementTable("abacc");
		Assert.assertEquals("a3c", replaced);
	}

	@Test
	public void applyLongestOnStart() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"REP 3",
			"REP ^b 1",
			"REP ^bac 3",
			"REP ^ba 2");
		affParser.parse(affFile);

		String replaced = affParser.applyReplacementTable("bacc");
		Assert.assertEquals("3c", replaced);
	}

	@Test
	public void applyLongestOnEnd() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"REP 3",
			"REP b$ 1",
			"REP cab$ 3",
			"REP ab$ 2");
		affParser.parse(affFile);

		String replaced = affParser.applyReplacementTable("ccab");
		Assert.assertEquals("c3", replaced);
	}

}
