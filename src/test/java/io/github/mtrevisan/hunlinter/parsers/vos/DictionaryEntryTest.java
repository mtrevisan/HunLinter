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
package io.github.mtrevisan.hunlinter.parsers.vos;

import io.github.mtrevisan.hunlinter.parsers.affix.AffixParser;
import io.github.mtrevisan.hunlinter.services.system.FileHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;


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
		DictionaryEntryFactory dictionaryEntryFactory = new DictionaryEntryFactory(affParser.getAffixData());
		DictionaryEntry entry = dictionaryEntryFactory.createFromDictionaryLine(line);

		Assertions.assertEquals("abcdef", entry.getWord());
		Assertions.assertNull(entry.continuationFlags);
		Assertions.assertArrayEquals(new String[]{"st:abcdef"}, entry.morphologicalFields);


		line = "abcdef/ABC";
		entry = dictionaryEntryFactory.createFromDictionaryLine(line);

		Assertions.assertEquals("abcdef", entry.word);
		Assertions.assertArrayEquals(new String[]{"A", "B", "C"}, entry.continuationFlags.toArray(String[]::new));
		Assertions.assertArrayEquals(new String[]{"st:abcdef"}, entry.morphologicalFields);


		line = "abcdef	po:noun";
		entry = dictionaryEntryFactory.createFromDictionaryLine(line);

		Assertions.assertEquals("abcdef", entry.word);
		Assertions.assertNull(entry.continuationFlags);
		Assertions.assertArrayEquals(new String[]{"st:abcdef", "po:noun"}, entry.morphologicalFields);


		line = "abcdef/ABC	po:noun";
		entry = dictionaryEntryFactory.createFromDictionaryLine(line);

		Assertions.assertEquals("abcdef", entry.word);
		Assertions.assertArrayEquals(new String[]{"A", "B", "C"}, entry.continuationFlags.toArray(String[]::new));
		Assertions.assertArrayEquals(new String[]{"st:abcdef", "po:noun"}, entry.morphologicalFields);


		line = "abc\\/def";
		entry = dictionaryEntryFactory.createFromDictionaryLine(line);

		Assertions.assertEquals("abc/def", entry.getWord());
		Assertions.assertNull(entry.continuationFlags);
		Assertions.assertArrayEquals(new String[]{"st:abc/def"}, entry.morphologicalFields);


		line = "abc\\/def/ABC";
		entry = dictionaryEntryFactory.createFromDictionaryLine(line);

		Assertions.assertEquals("abc/def", entry.word);
		Assertions.assertArrayEquals(new String[]{"A", "B", "C"}, entry.continuationFlags.toArray(String[]::new));
		Assertions.assertArrayEquals(new String[]{"st:abc/def"}, entry.morphologicalFields);


		line = "abc\\/def	po:noun";
		entry = dictionaryEntryFactory.createFromDictionaryLine(line);

		Assertions.assertEquals("abc/def", entry.word);
		Assertions.assertNull(entry.continuationFlags);
		Assertions.assertArrayEquals(new String[]{"st:abc/def", "po:noun"}, entry.morphologicalFields);


		line = "abc\\/def/ABC	po:noun";
		entry = dictionaryEntryFactory.createFromDictionaryLine(line);

		Assertions.assertEquals("abc/def", entry.word);
		Assertions.assertArrayEquals(new String[]{"A", "B", "C"}, entry.continuationFlags.toArray(String[]::new));
		Assertions.assertArrayEquals(new String[]{"st:abc/def", "po:noun"}, entry.morphologicalFields);
	}

}
