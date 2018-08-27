package unit731.hunspeller.parsers.dictionary;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
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
			"REP ^b bb",
			"REP e$ ee",
			"REP ij IJ",
			"REP alot a_lot");
		affParser.parse(affFile);

		List<Pair<String, String>> replacementTable = affParser.getData(AffixTag.REPLACEMENT_TABLE);

		List<Pair<String, String>> expected = new ArrayList<>();
		expected.add(Pair.of("^b", "bb"));
		expected.add(Pair.of("e$", "ee"));
		expected.add(Pair.of("ij", "IJ"));
		expected.add(Pair.of("alot", "a lot"));
		Assert.assertEquals(expected, replacementTable);

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
			"REP bbb 3",
			"REP bb 2");
		affParser.parse(affFile);

		String replaced = affParser.applyReplacementTable("abbbc");
		Assert.assertEquals("a3c", replaced);
	}

	@Test
	public void applyLongestOnStart() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"REP 3",
			"REP ^b 1",
			"REP ^bbb 3",
			"REP ^bb 2");
		affParser.parse(affFile);

		String replaced = affParser.applyReplacementTable("bbbc");
		Assert.assertEquals("3c", replaced);
	}

	@Test
	public void applyLongestOnEnd() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"REP 3",
			"REP b$ 1",
			"REP bbb$ 3",
			"REP bb$ 2");
		affParser.parse(affFile);

		String replaced = affParser.applyReplacementTable("cbbb");
		Assert.assertEquals("c3", replaced);
	}

}
