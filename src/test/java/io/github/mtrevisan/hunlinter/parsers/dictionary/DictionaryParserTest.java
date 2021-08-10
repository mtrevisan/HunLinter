/**
 * Copyright (c) 2019-2021 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.parsers.dictionary;

import io.github.mtrevisan.hunlinter.parsers.affix.AffixParser;
import io.github.mtrevisan.hunlinter.parsers.affix.ConversionTable;
import io.github.mtrevisan.hunlinter.parsers.enums.AffixOption;
import io.github.mtrevisan.hunlinter.services.system.FileHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;


class DictionaryParserTest{

	private final AffixParser affParser = new AffixParser();


	@Test
	void replacementTable() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"REP 4",
			"REP ^b bb",
			"REP e$ ee",
			"REP ij IJ",
			"REP alot a_lot");
		affParser.parse(affFile, language);

		ConversionTable table = affParser.getAffixData().getData(AffixOption.REPLACEMENT_TABLE);
		Assertions.assertEquals("[affixOption=REPLACEMENT_TABLE,table={  =[(alot,a_lot), (ij,IJ)],  $=[(e$,ee)], ^ =[(^b,bb)]}]", table.toString());

		String replaced = affParser.getAffixData().applyReplacementTable("clea");
		Assertions.assertEquals("clea", replaced);

		replaced = affParser.getAffixData().applyReplacementTable("bcijde");
		Assertions.assertEquals("bcIJde", replaced);

		replaced = affParser.getAffixData().applyReplacementTable("alot");
		Assertions.assertEquals("a lot", replaced);
	}

	@Test
	void applyLongest() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"REP 3",
			"REP b 1",
			"REP bac 3",
			"REP ba 2");
		affParser.parse(affFile, language);

		String replaced = affParser.getAffixData().applyReplacementTable("abacc");
		Assertions.assertEquals("a3c", replaced);
	}

	@Test
	void applyLongestOnStart() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"REP 3",
			"REP ^b 1",
			"REP ^bac 3",
			"REP ^ba 2");
		affParser.parse(affFile, language);

		String replaced = affParser.getAffixData().applyReplacementTable("bacc");
		Assertions.assertEquals("3c", replaced);
	}

	@Test
	void applyLongestOnEnd() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"REP 3",
			"REP b$ 1",
			"REP cab$ 3",
			"REP ab$ 2");
		affParser.parse(affFile, language);

		String replaced = affParser.getAffixData().applyReplacementTable("ccab");
		Assertions.assertEquals("c3", replaced);
	}

}
