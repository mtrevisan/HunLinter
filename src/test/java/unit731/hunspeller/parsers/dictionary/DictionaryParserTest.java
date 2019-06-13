package unit731.hunspeller.parsers.dictionary;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.parsers.affix.ConversionTable;
import unit731.hunspeller.services.FileHelper;


class DictionaryParserTest{

	private final AffixParser affParser = new AffixParser();


	@Test
	void replacementTable() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"REP 4",
			"REP ^b bb",
			"REP e$ ee",
			"REP ij IJ",
			"REP alot a_lot");
		affParser.parse(affFile);

		ConversionTable table = affParser.getAffixData().getData(AffixTag.REPLACEMENT_TABLE);
		Assertions.assertEquals("[affixTag=REPLACEMENT_TABLE,table={  =[(ij,IJ), (alot,a lot)],  $=[(e$,ee)], ^ =[(^b,bb)]}]", table.toString());

		List<String> replaced = affParser.getAffixData().applyReplacementTable("clea");
		Assertions.assertTrue(replaced.isEmpty());

		replaced = affParser.getAffixData().applyReplacementTable("bcijde");
		Assertions.assertEquals(Arrays.asList("bbcijde", "bcijdee", "bcIJde"), replaced);

		replaced = affParser.getAffixData().applyReplacementTable("alot");
		Assertions.assertEquals(Collections.singletonList("a lot"), replaced);
	}

	@Test
	void applyLongest() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"REP 3",
			"REP b 1",
			"REP bac 3",
			"REP ba 2");
		affParser.parse(affFile);

		List<String> replaced = affParser.getAffixData().applyReplacementTable("abacc");
		Assertions.assertEquals(Arrays.asList("a1acc", "a3c", "a2cc"), replaced);
	}

	@Test
	void applyLongestOnStart() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"REP 3",
			"REP ^b 1",
			"REP ^bac 3",
			"REP ^ba 2");
		affParser.parse(affFile);

		List<String> replaced = affParser.getAffixData().applyReplacementTable("bacc");
		Assertions.assertEquals(Arrays.asList("1acc", "3c", "2cc"), replaced);
	}

	@Test
	void applyLongestOnEnd() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"REP 3",
			"REP b$ 1",
			"REP cab$ 3",
			"REP ab$ 2");
		affParser.parse(affFile);

		List<String> replaced = affParser.getAffixData().applyReplacementTable("ccab");
		Assertions.assertEquals(Arrays.asList("cca1", "c3", "cc2"), replaced);
	}

}
