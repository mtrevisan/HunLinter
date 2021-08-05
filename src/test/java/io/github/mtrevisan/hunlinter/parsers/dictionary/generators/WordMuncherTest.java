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
package io.github.mtrevisan.hunlinter.parsers.dictionary.generators;

import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixParser;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.services.system.FileHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;

import java.io.File;
import java.io.IOException;
import java.util.List;


class WordMuncherTest{

//	@Test
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

//	@Test
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

//	@Test
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

//	@Test
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

//	@Test
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
		WordGenerator wordGenerator = new WordGenerator(affixData, dicParser, null);
		WordMuncher muncher = new WordMuncher(affixData, dicParser, wordGenerator);
		final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(line, affixData);
		return Pair.of(muncher, dicEntry);
	}

}
