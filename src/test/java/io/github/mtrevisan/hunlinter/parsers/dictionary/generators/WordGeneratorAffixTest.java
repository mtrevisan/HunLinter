/**
 * Copyright (c) 2019-2020 Mauro Trevisan
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

import io.github.mtrevisan.hunlinter.parsers.affix.ConversionTable;
import io.github.mtrevisan.hunlinter.parsers.enums.AffixOption;
import io.github.mtrevisan.hunlinter.services.system.FileHelper;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;

import java.io.File;
import java.io.IOException;


/** @see <a href="https://github.com/hunspell/hunspell/tree/master/tests/v1cmdline">Hunspell tests</a> */
class WordGeneratorAffixTest extends TestBase{

	@Test
	void affFormat() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"# Testing also whitespace and comments.",
			"OCONV 1",
			"OCONV é É 	",
			"",
			" # space",
			"  # 2xspace",
			"	# tab",
			"		# 2xtab",
			" 	# space+tab",
			"	 # tab+space");
		loadData(affFile, language);

		ConversionTable table = affixData.getData(AffixOption.OUTPUT_CONVERSION_TABLE);
		Assertions.assertEquals("[affixOption=OUTPUT_CONVERSION_TABLE,table={  =[(é,É)]}]", table.toString());
	}


	@Test
	void flagUTF8() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"FLAG UTF-8",
			"SFX A Y 1",
			"SFX A 0 s/ÖüÜ .",
			"SFX Ö Y 1",
			"SFX Ö 0 bar .",
			"SFX ü Y 1",
			"SFX ü 0 baz .",
			"PFX Ü Y 1",
			"PFX Ü 0 un .");
		loadData(affFile, language);

		String line = "foo/AÜ";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(8, words.length);
		//base inflection
		Assertions.assertEquals(createInflection("foo", "AÜ", "st:foo"), words[0]);
		//suffix inflections
		Assertions.assertEquals(createInflection("foos", "ÜÖü", "st:foo"), words[1]);
		//prefix inflections
		Assertions.assertEquals(createInflection("foosbar", "Ü", "st:foo"), words[2]);
		Assertions.assertEquals(createInflection("foosbaz", "Ü", "st:foo"), words[3]);
		//twofold inflectionss
		Assertions.assertEquals(createInflection("unfoo", "A", "st:foo"), words[4]);
		Assertions.assertEquals(createInflection("unfoos", "Öü", "st:foo"), words[5]);
		Assertions.assertEquals(createInflection("unfoosbar", null, "st:foo"), words[6]);
		Assertions.assertEquals(createInflection("unfoosbaz", null, "st:foo"), words[7]);
	}

	@Test
	void flagNumerical() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"FLAG num",
			"SFX 999 Y 1",
			"SFX 999 0 s/214,216,54321 .",
			"SFX 214 Y 1",
			"SFX 214 0 bar .",
			"SFX 216 Y 1",
			"SFX 216 0 baz .",
			"PFX 54321 Y 1",
			"PFX 54321 0 un .");
		loadData(affFile, language);

		String line = "foo/999,54321";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(8, words.length);
		//base inflections
		Assertions.assertEquals(createInflection("foo", "999,54321", "st:foo"), words[0]);
		//suffix inflectionss
		Assertions.assertEquals(createInflection("foos", "54321,214,216", "st:foo"), words[1]);
		//prefix inflectionss
		Assertions.assertEquals(createInflection("foosbar", "54321", "st:foo"), words[2]);
		Assertions.assertEquals(createInflection("foosbaz", "54321", "st:foo"), words[3]);
		//twofold inflectionss
		Assertions.assertEquals(createInflection("unfoo", "999", "st:foo"), words[4]);
		Assertions.assertEquals(createInflection("unfoos", "214,216", "st:foo"), words[5]);
		Assertions.assertEquals(createInflection("unfoosbar", null, "st:foo"), words[6]);
		Assertions.assertEquals(createInflection("unfoosbaz", null, "st:foo"), words[7]);
	}

	@Test
	void flagASCII() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"SFX A Y 1",
			"SFX A 0 s/123 .",
			"SFX 1 Y 1",
			"SFX 1 0 bar .",
			"SFX 2 Y 1",
			"SFX 2 0 baz .",
			"PFX 3 Y 1",
			"PFX 3 0 un .");
		loadData(affFile, language);

		String line = "foo/A3";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(8, words.length);
		//base inflections
		Assertions.assertEquals(createInflection("foo", "A3", "st:foo"), words[0]);
		//suffix inflectionss
		Assertions.assertEquals(createInflection("foos", "312", "st:foo"), words[1]);
		//prefix inflectionss
		Assertions.assertEquals(createInflection("foosbar", "3", "st:foo"), words[2]);
		Assertions.assertEquals(createInflection("foosbaz", "3", "st:foo"), words[3]);
		//twofold inflectionss
		Assertions.assertEquals(createInflection("unfoo", "A", "st:foo"), words[4]);
		Assertions.assertEquals(createInflection("unfoos", "12", "st:foo"), words[5]);
		Assertions.assertEquals(createInflection("unfoosbar", null, "st:foo"), words[6]);
		Assertions.assertEquals(createInflection("unfoosbaz", null, "st:foo"), words[7]);
	}

	@Test
	void flagDoubleASCII() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"FLAG long",
			"SFX zx Y 1",
			"SFX zx 0 s/g?1G09 .",
			"SFX g? Y 1",
			"SFX g? 0 bar .",
			"SFX 1G Y 1",
			"SFX 1G 0 baz .",
			"PFX 09 Y 1",
			"PFX 09 0 un .");
		loadData(affFile, language);

		String line = "foo/zx09";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(8, words.length);
		//base inflections
		Assertions.assertEquals(createInflection("foo", "zx09", "st:foo"), words[0]);
		//suffix inflectionss
		Assertions.assertEquals(createInflection("foos", "09g?1G", "st:foo"), words[1]);
		//prefix inflectionss
		Assertions.assertEquals(createInflection("foosbar", "09", "st:foo"), words[2]);
		Assertions.assertEquals(createInflection("foosbaz", "09", "st:foo"), words[3]);
		//twofold inflectionss
		Assertions.assertEquals(createInflection("unfoo", "zx", "st:foo"), words[4]);
		Assertions.assertEquals(createInflection("unfoos", "g?1G", "st:foo"), words[5]);
		Assertions.assertEquals(createInflection("unfoosbar", null, "st:foo"), words[6]);
		Assertions.assertEquals(createInflection("unfoosbaz", null, "st:foo"), words[7]);
	}


	@Test
	void conditions() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"SFX A Y 6",
			"SFX A 0 a .",
			"SFX A 0 b b",
			"SFX A 0 c [ab]",
			"SFX A 0 d [^ab]",
			"SFX A 0 e [^c]",
			"SFX A 0 f a[^ab]b");
		loadData(affFile, language);

		String line = "a/A";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(4, words.length);
		//base inflections
		Assertions.assertEquals(createInflection("a", "A", "st:a"), words[0]);
		//suffix inflectionss
		Assertions.assertEquals(createInflection("aa", null, "st:a"), words[1]);
		Assertions.assertEquals(createInflection("ac", null, "st:a"), words[2]);
		Assertions.assertEquals(createInflection("ae", null, "st:a"), words[3]);
	}


	@Test
	void stems1() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"FLAG long",
			"SFX S1 Y 1",
			"SFX S1 0 s1/S2P1",
			"SFX S2 Y 1",
			"SFX S2 0 s2",
			"PFX P1 Y 1",
			"PFX P1 0 p1");
		loadData(affFile, language);

		String line = "aa/S1";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(5, words.length);
		//base inflections
		Assertions.assertEquals(createInflection("aa", "S1", "st:aa"), words[0]);
		//suffix inflectionss
		Assertions.assertEquals(createInflection("aas1", "S2P1", "st:aa"), words[1]);
		//prefix inflectionss
		Assertions.assertEquals(createInflection("aas1s2", "P1", "st:aa"), words[2]);
		//twofold inflectionss
		Assertions.assertEquals(createInflection("p1aas1", "S2", "st:aa"), words[3]);
		Assertions.assertEquals(createInflection("p1aas1s2", null, "st:aa"), words[4]);
	}

	@Test
	void stems2() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"FLAG long",
			"SFX S1 Y 1",
			"SFX S1 0 s1/S2",
			"SFX S2 Y 1",
			"SFX S2 0 s2/P1",
			"PFX P1 Y 1",
			"PFX P1 0 p1");
		loadData(affFile, language);

		String line = "aa/S1";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(4, words.length);
		//base inflections
		Assertions.assertEquals(createInflection("aa", "S1", "st:aa"), words[0]);
		//suffix inflectionss
		Assertions.assertEquals(createInflection("aas1", "S2", "st:aa"), words[1]);
		//prefix inflectionss
		//twofold inflectionss
		Assertions.assertEquals(createInflection("aas1s2", "P1", "st:aa"), words[2]);
		Assertions.assertEquals(createInflection("p1aas1s2", null, "st:aa"), words[3]);
	}

	@Test
	void stems3() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"FLAG long",
			"SFX S1 Y 1",
			"SFX S1 0 s1/S2",
			"SFX S2 Y 1",
			"SFX S2 0 s2",
			"PFX P1 Y 1",
			"PFX P1 0 p1");
		loadData(affFile, language);

		String line = "aa/S1P1";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(6, words.length);
		//base inflections
		Assertions.assertEquals(createInflection("aa", "S1P1", "st:aa"), words[0]);
		//suffix inflectionss
		Assertions.assertEquals(createInflection("aas1", "P1S2", "st:aa"), words[1]);
		//prefix inflectionss
		Assertions.assertEquals(createInflection("aas1s2", "P1", "st:aa"), words[2]);
		//twofold inflectionss
		Assertions.assertEquals(createInflection("p1aa", "S1", "st:aa"), words[3]);
		Assertions.assertEquals(createInflection("p1aas1", "S2", "st:aa"), words[4]);
		Assertions.assertEquals(createInflection("p1aas1s2", null, "st:aa"), words[5]);
	}

	@Test
	void stems4() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"FLAG long",
			"SFX S1 Y 1",
			"SFX S1 0 s1/S2",
			"SFX S2 Y 1",
			"SFX S2 0 s2",
			"PFX P1 Y 1",
			"PFX P1 0 p1");
		loadData(affFile, language);

		String line = "aa/P1S1";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(6, words.length);
		//base inflections
		Assertions.assertEquals(createInflection("aa", "P1S1", "st:aa"), words[0]);
		//suffix inflectionss
		Assertions.assertEquals(createInflection("aas1", "P1S2", "st:aa"), words[1]);
		//prefix inflectionss
		Assertions.assertEquals(createInflection("aas1s2", "P1", "st:aa"), words[2]);
		//twofold inflectionss
		Assertions.assertEquals(createInflection("p1aa", "S1", "st:aa"), words[3]);
		Assertions.assertEquals(createInflection("p1aas1", "S2", "st:aa"), words[4]);
		Assertions.assertEquals(createInflection("p1aas1s2", null, "st:aa"), words[5]);
	}

	@Test
	void stems5() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"SFX A Y 1",
			"SFX A 0 a",
			"SFX B Y 1",
			"SFX B 0 b/A",
			"SFX C Y 1",
			"SFX C 0 c/E",
			"SFX D Y 1",
			"SFX D 0 d/AE",
			"PFX E Y 1",
			"PFX E 0 e");
		loadData(affFile, language);

		String line = "a/ABCDE";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(14, words.length);
		//base inflections
		Assertions.assertEquals(createInflection("a", "ABCDE", "st:a"), words[0]);
		//suffix inflectionss
		Assertions.assertEquals(createInflection("aa", "E", "st:a"), words[1]);
		Assertions.assertEquals(createInflection("ab", "EA", "st:a"), words[2]);
		Assertions.assertEquals(createInflection("ac", "E", "st:a"), words[3]);
		Assertions.assertEquals(createInflection("ad", "EA", "st:a"), words[4]);
		//prefix inflectionss
		Assertions.assertEquals(createInflection("aba", "E", "st:a"), words[5]);
		Assertions.assertEquals(createInflection("ada", "E", "st:a"), words[6]);
		//twofold inflectionss
		Assertions.assertEquals(createInflection("ea", "ABCD", "st:a"), words[7]);
		Assertions.assertEquals(createInflection("eaa", null, "st:a"), words[8]);
		Assertions.assertEquals(createInflection("eab", "A", "st:a"), words[9]);
		Assertions.assertEquals(createInflection("eac", null, "st:a"), words[10]);
		Assertions.assertEquals(createInflection("ead", "A", "st:a"), words[11]);
		Assertions.assertEquals(createInflection("eaba", null, "st:a"), words[12]);
		Assertions.assertEquals(createInflection("eada", null, "st:a"), words[13]);
	}


	@Test
	void stemsInvalidFullstrip(){
		Throwable exception = Assertions.assertThrows(LinterException.class, () -> {
			String language = "xxx";
			File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
				"SET UTF-8",
				"SFX A Y 1",
				"SFX A a b a");
			loadData(affFile, language);

			String line = "a/A";
			DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
			wordGenerator.applyAffixRules(dicEntry);
		});
		Assertions.assertEquals("Cannot strip full word `a` without the FULLSTRIP option", exception.getMessage());
	}

	@Test
	void stemsValidFullstrip() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"FULLSTRIP",
			"SFX A Y 1",
			"SFX A a b a");
		loadData(affFile, language);

		String line = "a/A";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(2, words.length);
		//base inflection
		Assertions.assertEquals(createInflection("a", "A", "st:a"), words[0]);
		//suffix inflections
		Assertions.assertEquals(createInflection("b", null, "st:a"), words[1]);
	}


	@Test
	void stemsInvalidTwofold1(){
		Throwable exception = Assertions.assertThrows(LinterException.class, () -> {
			String language = "xxx";
			File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
				"SET UTF-8",
				"FLAG long",
				"SFX S1 Y 1",
				"SFX S1 0 s1/S2P1",
				"SFX S2 Y 1",
				"SFX S2 0 s2/S3",
				"SFX S3 Y 1",
				"SFX S3 0 s3",
				"PFX P1 Y 1",
				"PFX P1 0 p1/P2",
				"PFX P2 Y 1",
				"PFX P2 0 p2");
			loadData(affFile, language);

			String line = "aa/S1";
			DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
			wordGenerator.applyAffixRules(dicEntry);
		});
		Assertions.assertEquals("Twofold rule violated for `p1aas1/S2,P2\tst:aa\tfrom\tSFX S1 0 s1/S2P1 . > PFX P1 0 p1/P2 . from S1 > P1` (S1 > P1 still has rules P2)", exception.getMessage());
	}

	@Test
	void stemsInvalidTwofold2(){
		Throwable exception = Assertions.assertThrows(LinterException.class, () -> {
			String language = "xxx";
			File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
				"SET UTF-8",
				"SFX A Y 1",
				"SFX A 0 a",
				"SFX B Y 1",
				"SFX B 0 b/A",
				"SFX C Y 1",
				"SFX C 0 c/E",
				"SFX D Y 1",
				"SFX D 0 d/AE",
				"PFX E Y 1",
				"PFX E 0 e",
				"PFX F Y 1",
				"PFX F 0 f/A",
				"PFX G Y 1",
				"PFX G 0 g/E",
				"PFX H Y 1",
				"PFX H 0 h/AE");
			loadData(affFile, language);

			String line = "a/ABCDEFGH";
			DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
			wordGenerator.applyAffixRules(dicEntry);
		});
		Assertions.assertEquals("Twofold rule violated for `ga/A,B,C,D,E\tst:a\tfrom\tPFX G 0 g/E . from G` (G still has rules E)", exception.getMessage());
	}


	@Test
	void complexPrefixes1() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"COMPLEXPREFIXES",
			"PFX A Y 1",
			"PFX A 0 a",
			"PFX B Y 1",
			"PFX B 0 b/A",
			"PFX C Y 1",
			"PFX C 0 c/E",
			"PFX D Y 1",
			"PFX D 0 d/AE",
			"SFX E Y 1",
			"SFX E 0 e");
		loadData(affFile, language);

		String line = "a/ABCDE";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(14, words.length);
		//base inflection
		Assertions.assertEquals(createInflection("a", "ABCDE", "st:a"), words[0]);
		//suffix inflections
		Assertions.assertEquals(createInflection("aa", "E", "st:a"), words[1]);
		Assertions.assertEquals(createInflection("ba", "EA", "st:a"), words[2]);
		Assertions.assertEquals(createInflection("ca", "E", "st:a"), words[3]);
		Assertions.assertEquals(createInflection("da", "EA", "st:a"), words[4]);
		//prefix inflections
		Assertions.assertEquals(createInflection("aba", "E", "st:a"), words[5]);
		Assertions.assertEquals(createInflection("ada", "E", "st:a"), words[6]);
		//twofold inflections
		Assertions.assertEquals(createInflection("ae", "ABCD", "st:a"), words[7]);
		Assertions.assertEquals(createInflection("aae", null, "st:a"), words[8]);
		Assertions.assertEquals(createInflection("bae", "A", "st:a"), words[9]);
		Assertions.assertEquals(createInflection("cae", null, "st:a"), words[10]);
		Assertions.assertEquals(createInflection("dae", "A", "st:a"), words[11]);
		Assertions.assertEquals(createInflection("abae", null, "st:a"), words[12]);
		Assertions.assertEquals(createInflection("adae", null, "st:a"), words[13]);
	}

	@Test
	void complexPrefixes2() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"COMPLEXPREFIXES",
			"PFX A Y 1",
			"PFX A 0 tek .",
			"PFX B Y 1",
			"PFX B 0 met/A .");
		loadData(affFile, language);

		String line = "ouro/B";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(3, words.length);
		//base inflection
		Assertions.assertEquals(createInflection("ouro", "B", "st:ouro"), words[0]);
		//suffix inflections
		Assertions.assertEquals(createInflection("metouro", "A", "st:ouro"), words[1]);
		//prefix inflections
		Assertions.assertEquals(createInflection("tekmetouro", null, "st:ouro"), words[2]);
		//twofold inflections
	}

	@Test
	void complexPrefixesUTF8() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"COMPLEXPREFIXES",
			"PFX A Y 1",
			"PFX A 0 ⲧⲉⲕ .",
			"PFX B Y 1",
			"PFX B 0 ⲙⲉⲧ/A .");
		loadData(affFile, language);

		String line = "ⲟⲩⲣⲟ/B";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(3, words.length);
		//base inflection
		Assertions.assertEquals(createInflection("ⲟⲩⲣⲟ", "B", "st:ⲟⲩⲣⲟ"), words[0]);
		//suffix inflections
		Assertions.assertEquals(createInflection("ⲙⲉⲧⲟⲩⲣⲟ", "A", "st:ⲟⲩⲣⲟ"), words[1]);
		//prefix inflections
		Assertions.assertEquals(createInflection("ⲧⲉⲕⲙⲉⲧⲟⲩⲣⲟ", null, "st:ⲟⲩⲣⲟ"), words[2]);
		//twofold inflections
	}

	@Test
	void complexPrefixesInvalidTwofold(){
		Throwable exception = Assertions.assertThrows(LinterException.class, () -> {
			String language = "xxx";
			File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
				"SET UTF-8",
				"COMPLEXPREFIXES",
				"PFX A Y 1",
				"PFX A 0 a",
				"PFX B Y 1",
				"PFX B 0 b/A",
				"PFX C Y 1",
				"PFX C 0 c/E",
				"PFX D Y 1",
				"PFX D 0 d/AE",
				"SFX E Y 1",
				"SFX E 0 e",
				"SFX F Y 1",
				"SFX F 0 f/A",
				"SFX G Y 1",
				"SFX G 0 g/E",
				"SFX H Y 1",
				"SFX H 0 h/AE");
			loadData(affFile, language);

			String line = "a/ABCDEFGH";
			DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
			wordGenerator.applyAffixRules(dicEntry);
		});
		Assertions.assertEquals("Twofold rule violated for `ag/A,B,C,D,E\tst:a\tfrom\tSFX G 0 g/E . from G` (G still has rules E)", exception.getMessage());
	}


	@Test
	void needAffix3() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"NEEDAFFIX X",
			"SFX A Y 1",
			"SFX A 0 s/XB .",
			"SFX B Y 1",
			"SFX B 0 baz .");
		loadData(affFile, language);

		String line = "foo/A";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(2, words.length);
		//base inflection
		Assertions.assertEquals(createInflection("foo", "A", "st:foo"), words[0]);
		//suffix inflections
		//prefix inflections
		Assertions.assertEquals(createInflection("foosbaz", null, "st:foo"), words[1]);
		//twofold inflections
	}

	@Test
	void needAffix5() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"NEEDAFFIX X",
			"SFX A Y 2",
			"SFX A 0 -suf/B .",
			"SFX A 0 -pseudosuf/XB .",
			"SFX B Y 1",
			"SFX B 0 -bar .",
			"PFX C Y 2",
			"PFX C 0 pre- .",
			"PFX C 0 pseudopre-/X .");
		loadData(affFile, language);

		String line = "foo/AC";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(9, words.length);
		//base inflection
		Assertions.assertEquals(createInflection("foo", "AC", "st:foo"), words[0]);
		//suffix inflections
		Assertions.assertEquals(createInflection("foo-suf", "CB", "st:foo"), words[1]);
		//prefix inflections
		Assertions.assertEquals(createInflection("foo-suf-bar", "C", "st:foo"), words[2]);
		Assertions.assertEquals(createInflection("foo-pseudosuf-bar", "C", "st:foo"), words[3]);
		//twofold inflections
		Assertions.assertEquals(createInflection("pre-foo", "A", "st:foo"), words[4]);
		Assertions.assertEquals(createInflection("pre-foo-suf", "B", "st:foo"), words[5]);
		Assertions.assertEquals(createInflection("pre-foo-pseudosuf", "B", "st:foo"), words[6]);
		Assertions.assertEquals(createInflection("pre-foo-suf-bar", null, "st:foo"), words[7]);
		Assertions.assertEquals(createInflection("pre-foo-pseudosuf-bar", null, "st:foo"), words[8]);
	}


	@Test
	void circumfix1() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"CIRCUMFIX X",
			"PFX A Y 1",
			"PFX A 0 leg/X .",
			"PFX B Y 1",
			"PFX B 0 legesleg/X .",
			"SFX C Y 3",
			"SFX C 0 obb .",
			"SFX C 0 obb/AX .",
			"SFX C 0 obb/BX .");
		loadData(affFile, language);

		String line = "nagy/C";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(4, words.length);
		//base inflection
		Assertions.assertEquals(createInflection("nagy", "C", "st:nagy"), words[0]);
		//suffix inflections
		Assertions.assertEquals(createInflection("nagyobb", null, "st:nagy"), words[1]);
		//prefix inflections
		//twofold inflections
		Assertions.assertEquals(createInflection("legnagyobb", null, "st:nagy"), words[2]);
		Assertions.assertEquals(createInflection("legeslegnagyobb", null, "st:nagy"), words[3]);
	}

	@Test
	void circumfix2() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"CIRCUMFIX X",
			"PFX A Y 1",
			"PFX A 0 leg/X .",
			"PFX B Y 1",
			"PFX B 0 legesleg/X .",
			"SFX C Y 3",
			"SFX C 0 obb .",
			"SFX C 0 obb/AX .",
			"SFX C 0 obb/BX .");
		loadData(affFile, language);

		String line = "nagy/CX";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(2, words.length);
		//base inflection
		//suffix inflections
		//prefix inflections
		//twofold inflections
		Assertions.assertEquals(createInflection("legnagyobb", null, "st:nagy"), words[0]);
		Assertions.assertEquals(createInflection("legeslegnagyobb", null, "st:nagy"), words[1]);
	}

	@Test
	void circumfix3() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"CIRCUMFIX X",
			"PFX a Y 4",
			"PFX a 0 a .",
			"PFX a 0 n .",
			"PFX a 0 t .",
			"PFX a 0 y .",
			"PFX c Y 5",
			"PFX c a g/X a[^y]",
			"PFX c a f/X a[^y]",
			"PFX c 0 t/X [^a]",
			"PFX c 0 lt/X [^a]",
			"PFX c 0 wlt/X [^a]",
			"SFX b Y 1",
			"SFX b 0 i/cX .",
			"PFX d Y 1",
			"PFX d 0 y/X .",
			"SFX e Y 2",
			"SFX e 0 un/cdX .",
			"SFX e 0 n/cdX .");
		loadData(affFile, language);

		String line = "bark/abe";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(17, words.length);
		//base inflection
		Assertions.assertEquals(createInflection("bark", "abe", "st:bark"), words[0]);
		//suffix inflections
		//twofold inflections
		Assertions.assertEquals(createInflection("abark", "be", "st:bark"), words[1]);
		Assertions.assertEquals(createInflection("nbark", "be", "st:bark"), words[2]);
		Assertions.assertEquals(createInflection("tbark", "be", "st:bark"), words[3]);
		Assertions.assertEquals(createInflection("ybark", "be", "st:bark"), words[4]);
		Assertions.assertEquals(createInflection("abarki", null, "st:bark"), words[5]);
		Assertions.assertEquals(createInflection("nbarki", null, "st:bark"), words[6]);
		Assertions.assertEquals(createInflection("tbarki", null, "st:bark"), words[7]);
		Assertions.assertEquals(createInflection("ybarki", null, "st:bark"), words[8]);
		Assertions.assertEquals(createInflection("abarkun", null, "st:bark"), words[9]);
		Assertions.assertEquals(createInflection("nbarkun", null, "st:bark"), words[10]);
		Assertions.assertEquals(createInflection("tbarkun", null, "st:bark"), words[11]);
		Assertions.assertEquals(createInflection("ybarkun", null, "st:bark"), words[12]);
		Assertions.assertEquals(createInflection("abarkn", null, "st:bark"), words[13]);
		Assertions.assertEquals(createInflection("nbarkn", null, "st:bark"), words[14]);
		Assertions.assertEquals(createInflection("tbarkn", null, "st:bark"), words[15]);
		Assertions.assertEquals(createInflection("ybarkn", null, "st:bark"), words[16]);
	}


	@Test
	void morphologicalAnalisys() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"PFX P Y 1",
			"PFX P 0 un . dp:pfx_un sp:un",
			"SFX S Y 1",
			"SFX S 0 s . is:plur",
			"SFX Q Y 1",
			"SFX Q 0 s . is:sg_3",
			"SFX R Y 1",
			"SFX R 0 able/PS . ds:der_able");
		loadData(affFile, language);

		String line = "drink/S	po:noun";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(2, words.length);
		//base inflection
		Assertions.assertEquals(createInflection("drink", "S", "st:drink po:noun"), words[0]);
		//suffix inflections
		Assertions.assertEquals(createInflection("drinks", null, "st:drink po:noun is:plur"), words[1]);
		//prefix inflections
		//twofold inflections


		line = "drink/RQ	po:verb	al:drank	al:drunk	ts:present";
		dicEntry = wordGenerator.createFromDictionaryLine(line);
		words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(6, words.length);
		//base inflection
		Assertions.assertEquals(createInflection("drink", "RQ", "st:drink po:verb al:drank al:drunk ts:present"), words[0]);
		//suffix inflections
		Assertions.assertEquals(createInflection("drinkable", "PS", "st:drink po:verb al:drank al:drunk ds:der_able"), words[1]);
		Assertions.assertEquals(createInflection("drinks", null, "st:drink po:verb al:drank al:drunk ts:present is:sg_3"), words[2]);
		//prefix inflections
		Assertions.assertEquals(createInflection("drinkables", "P", "st:drink po:verb al:drank al:drunk ds:der_able is:plur"),
			words[3]);
		//twofold inflections
		Assertions.assertEquals(createInflection("undrinkable", "S", "dp:pfx_un sp:un st:drink po:verb al:drank al:drunk ds:der_able"),
			words[4]);
		Assertions.assertEquals(createInflection("undrinkables", null, "dp:pfx_un sp:un st:drink po:verb al:drank al:drunk ds:der_able is:plur"),
			words[5]);
	}


	@Test
	void alias1() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"AF 2",
			"AF AB",
			"AF A",
			"SFX A Y 1",
			"SFX A 0 x .",
			"SFX B Y 1",
			"SFX B 0 y/2 .");
		loadData(affFile, language);


		String line = "foo/1";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(4, words.length);
		//base inflection
		Assertions.assertEquals(createInflection("foo", "AB", "st:foo"), words[0]);
		//suffix inflections
		Assertions.assertEquals(createInflection("foox", null, "st:foo"), words[1]);
		Assertions.assertEquals(createInflection("fooy", "A", "st:foo"), words[2]);
		//prefix inflections
		Assertions.assertEquals(createInflection("fooyx", null, "st:foo"), words[3]);
		//twofold inflections
	}


	@Test
	void escapeSlash() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"SFX A Y 1",
			"SFX A 0 x .",
			"SFX B Y 1",
			"SFX B 0 y\\/z .");
		loadData(affFile, language);


		String line = "foo\\/bar/AB";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(3, words.length);
		//base inflection
		Assertions.assertEquals(createInflection("foo/bar", "AB", "st:foo/bar"), words[0]);
		//suffix inflections
		Assertions.assertEquals(createInflection("foo/barx", null, "st:foo/bar"), words[1]);
		Assertions.assertEquals(createInflection("foo/bary/z", null, "st:foo/bar"), words[2]);
		//prefix inflections
		//twofold inflections
	}


	@Test
	void forbiddenWord() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"FORBIDDENWORD !",
			"SFX s N 1",
			"SFX s 0 os .");
		loadData(affFile, language);

		String line = "forbidden/!s";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertTrue(words.length == 0);
	}

}
