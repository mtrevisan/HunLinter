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

import io.github.mtrevisan.hunlinter.services.system.FileHelper;
import io.github.mtrevisan.hunlinter.services.text.PermutationsWithRepetitions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;

import java.io.File;
import java.io.IOException;


/** @see <a href="https://github.com/hunspell/hunspell/tree/master/tests/v1cmdline">Hunspell tests</a> */
class WordGeneratorCompoundFlagTest extends TestBase{

	@Test
	void simple() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"COMPOUNDMIN 1",
			"COMPOUNDFLAG A");
		loadData(affFile, language);

		String[] inputCompounds = new String[]{
			"foo/A",
			"bar/A",
			"xy/A",
			"yz/A"
		};
		Inflection[] words = wordGenerator.applyCompoundFlag(inputCompounds, 10, PermutationsWithRepetitions.MAX_COMPOUNDS_INFINITY);

		Inflection[] expected = new Inflection[]{
			createInflection("foofoo", null, "pa:foo st:foo pa:foo st:foo"),
			createInflection("foobar", null, "pa:foo st:foo pa:bar st:bar"),
			createInflection("fooxy", null, "pa:foo st:foo pa:xy st:xy"),
			createInflection("fooyz", null, "pa:foo st:foo pa:yz st:yz"),
			createInflection("barfoo", null, "pa:bar st:bar pa:foo st:foo"),
			createInflection("barbar", null, "pa:bar st:bar pa:bar st:bar"),
			createInflection("barxy", null, "pa:bar st:bar pa:xy st:xy"),
			createInflection("baryz", null, "pa:bar st:bar pa:yz st:yz"),
			createInflection("xyfoo", null, "pa:xy st:xy pa:foo st:foo"),
			createInflection("xybar", null, "pa:xy st:xy pa:bar st:bar")
		};
		Assertions.assertArrayEquals(expected, words);
	}

	@Test
	void compoundMinLength() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"COMPOUNDMIN 3",
			"COMPOUNDFLAG A");
		loadData(affFile, language);

		String[] inputCompounds = new String[]{
			"foo/A",
			"bar/A",
			"yz/A"
		};
		Inflection[] words = wordGenerator.applyCompoundFlag(inputCompounds, 100, 2);

		Inflection[] expected = new Inflection[]{
			createInflection("foofoo", null, "pa:foo st:foo pa:foo st:foo"),
			createInflection("foobar", null, "pa:foo st:foo pa:bar st:bar"),
			createInflection("barfoo", null, "pa:bar st:bar pa:foo st:foo"),
			createInflection("barbar", null, "pa:bar st:bar pa:bar st:bar")
		};
		Assertions.assertArrayEquals(expected, words);
	}

	@Test
	void checkCompoundTriple() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"CHECKCOMPOUNDTRIPLE",
			"COMPOUNDFLAG A");
		loadData(affFile, language);

		String[] inputCompounds = new String[]{
			"foo/A",
			"opera/A",
			"eel/A",
			"bare/A"
		};
		Inflection[] words = wordGenerator.applyCompoundFlag(inputCompounds, 12, PermutationsWithRepetitions.MAX_COMPOUNDS_INFINITY);

		Inflection[] expected = new Inflection[]{
			createInflection("foofoo", null, "pa:foo st:foo pa:foo st:foo"),
			createInflection("fooeel", null, "pa:foo st:foo pa:eel st:eel"),
			createInflection("foobare", null, "pa:foo st:foo pa:bare st:bare"),
			createInflection("operafoo", null, "pa:opera st:opera pa:foo st:foo"),
			createInflection("operaopera", null, "pa:opera st:opera pa:opera st:opera"),
			createInflection("operaeel", null, "pa:opera st:opera pa:eel st:eel"),
			createInflection("operabare", null, "pa:opera st:opera pa:bare st:bare"),
			createInflection("eelfoo", null, "pa:eel st:eel pa:foo st:foo"),
			createInflection("eelopera", null, "pa:eel st:eel pa:opera st:opera"),
			createInflection("eeleel", null, "pa:eel st:eel pa:eel st:eel"),
			createInflection("eelbare", null, "pa:eel st:eel pa:bare st:bare")
		};
		Assertions.assertArrayEquals(expected, words);
	}

	@Test
	void simplifiedTriple() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"CHECKCOMPOUNDTRIPLE",
			"SIMPLIFIEDTRIPLE",
			"COMPOUNDMIN 2",
			"COMPOUNDFLAG A");
		loadData(affFile, language);

		String[] inputCompounds = new String[]{
			"glass/A",
			"sko/A"
		};
		Inflection[] words = wordGenerator.applyCompoundFlag(inputCompounds, 3, PermutationsWithRepetitions.MAX_COMPOUNDS_INFINITY);

		Inflection[] expected = new Inflection[]{
			createInflection("glassglass", null, "pa:glass st:glass pa:glass st:glass"),
			createInflection("glassko", null, "pa:glass st:glass pa:sko st:sko"),
			createInflection("skoglass", null, "pa:sko st:sko pa:glass st:glass")
		};
		Assertions.assertArrayEquals(expected, words);
	}

	@Test
	void forbidWordDuplication() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"CHECKCOMPOUNDDUP",
			"COMPOUNDMIN 2",
			"COMPOUNDFLAG A");
		loadData(affFile, language);

		String[] inputCompounds = new String[]{
			"foo/A",
			"bar/A",
			"yz/A"
		};
		Inflection[] words = wordGenerator.applyCompoundFlag(inputCompounds, 100, 2);

		Inflection[] expected = new Inflection[]{
			createInflection("foobar", null, "pa:foo st:foo pa:bar st:bar"),
			createInflection("fooyz", null, "pa:foo st:foo pa:yz st:yz"),
			createInflection("barfoo", null, "pa:bar st:bar pa:foo st:foo"),
			createInflection("baryz", null, "pa:bar st:bar pa:yz st:yz"),
			createInflection("yzfoo", null, "pa:yz st:yz pa:foo st:foo"),
			createInflection("yzbar", null, "pa:yz st:yz pa:bar st:bar")
		};
		Assertions.assertArrayEquals(expected, words);
	}

	@Test
	void withAffixes() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"COMPOUNDFLAG X",
			"PFX P Y 1",
			"PFX P 0 pre .",
			"SFX S Y 1",
			"SFX S 0 suf .");
		loadData(affFile, language);


		String line = "foo/XPS";
		final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(4, words.length);
		//base inflection
		Assertions.assertEquals(createInflection("foo", "XPS", "st:foo"), words[0]);
		//onefold inflections
		Assertions.assertEquals(createInflection("foosuf", "P", "st:foo"), words[1]);
		//twofold inflections
		Assertions.assertEquals(createInflection("prefoo", "S", "st:foo"), words[2]);
		Assertions.assertEquals(createInflection("prefoosuf", null, "st:foo"), words[3]);
		//lastfold inflections


		String[] inputCompounds = new String[]{
			"foo/XPS",
			"bar/XPS"
		};
		words = wordGenerator.applyCompoundFlag(inputCompounds, 20, 2);

		Inflection[] expected = new Inflection[]{
			createInflection("foofoo", "PS", "pa:foo st:foo pa:foo st:foo"),
			createInflection("foofoosuf", "P", "pa:foo st:foo pa:foo st:foo"),
			createInflection("prefoofoo", "S", "pa:foo st:foo pa:foo st:foo"),
			createInflection("prefoofoosuf", null, "pa:foo st:foo pa:foo st:foo"),
			createInflection("foobar", "PS", "pa:foo st:foo pa:bar st:bar"),
			createInflection("foobarsuf", "P", "pa:foo st:foo pa:bar st:bar"),
			createInflection("prefoobar", "S", "pa:foo st:foo pa:bar st:bar"),
			createInflection("prefoobarsuf", null, "pa:foo st:foo pa:bar st:bar"),
			createInflection("barfoo", "PS", "pa:bar st:bar pa:foo st:foo"),
			createInflection("barfoosuf", "P", "pa:bar st:bar pa:foo st:foo"),
			createInflection("prebarfoo", "S", "pa:bar st:bar pa:foo st:foo"),
			createInflection("prebarfoosuf", null, "pa:bar st:bar pa:foo st:foo"),
			createInflection("barbar", "PS", "pa:bar st:bar pa:bar st:bar"),
			createInflection("barbarsuf", "P", "pa:bar st:bar pa:bar st:bar"),
			createInflection("prebarbar", "S", "pa:bar st:bar pa:bar st:bar"),
			createInflection("prebarbarsuf", null, "pa:bar st:bar pa:bar st:bar")
		};
		Assertions.assertArrayEquals(expected, words);
	}

	@Test
	void withAffixesOnefold() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"COMPOUNDFLAG X",
			"PFX P Y 1",
			"PFX P 0 pre .",
			"SFX S Y 1",
			"SFX S 0 suf/T .",
			"SFX T Y 1",
			"SFX T 0 sff .");
		loadData(affFile, language);


		String line = "foo/XPS";
		final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(6, words.length);
		//base inflection
		Assertions.assertEquals(createInflection("foo", "XPS", "st:foo"), words[0]);
		//onefold inflections
		Assertions.assertEquals(createInflection("foosuf", "PT", "st:foo"), words[1]);
		//twofold inflections
		Assertions.assertEquals(createInflection("foosufsff", "P", "st:foo"), words[2]);
		//lastfold inflections
		Assertions.assertEquals(createInflection("prefoo", "S", "st:foo"), words[3]);
		Assertions.assertEquals(createInflection("prefoosuf", "T", "st:foo"), words[4]);
		Assertions.assertEquals(createInflection("prefoosufsff", null, "st:foo"), words[5]);


		String[] inputCompounds = new String[]{
			"foo/XPS",
			"bar/XPS"
		};
		words = wordGenerator.applyCompoundFlag(inputCompounds, 20, 2);

		Inflection[] expected = new Inflection[]{
			createInflection("foofoo", "PS", "pa:foo st:foo pa:foo st:foo"),
			createInflection("foofoosuf", "PT", "pa:foo st:foo pa:foo st:foo"),
			createInflection("foofoosufsff", "P", "pa:foo st:foo pa:foo st:foo"),
			createInflection("prefoofoo", "S", "pa:foo st:foo pa:foo st:foo"),
			createInflection("prefoofoosuf", "T", "pa:foo st:foo pa:foo st:foo"),
			createInflection("prefoofoosufsff", null, "pa:foo st:foo pa:foo st:foo"),
			createInflection("foobar", "PS", "pa:foo st:foo pa:bar st:bar"),
			createInflection("foobarsuf", "PT", "pa:foo st:foo pa:bar st:bar"),
			createInflection("foobarsufsff", "P", "pa:foo st:foo pa:bar st:bar"),
			createInflection("prefoobar", "S", "pa:foo st:foo pa:bar st:bar"),
			createInflection("prefoobarsuf", "T", "pa:foo st:foo pa:bar st:bar"),
			createInflection("prefoobarsufsff", null, "pa:foo st:foo pa:bar st:bar"),
			createInflection("barfoo", "PS", "pa:bar st:bar pa:foo st:foo"),
			createInflection("barfoosuf", "PT", "pa:bar st:bar pa:foo st:foo"),
			createInflection("barfoosufsff", "P", "pa:bar st:bar pa:foo st:foo"),
			createInflection("prebarfoo", "S", "pa:bar st:bar pa:foo st:foo"),
			createInflection("prebarfoosuf", "T", "pa:bar st:bar pa:foo st:foo"),
			createInflection("prebarfoosufsff", null, "pa:bar st:bar pa:foo st:foo"),
			createInflection("barbar", "PS", "pa:bar st:bar pa:bar st:bar"),
			createInflection("barbarsuf", "PT", "pa:bar st:bar pa:bar st:bar")
		};
		Assertions.assertArrayEquals(expected, words);
	}

	@Test
	void withAffixesTwofold() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"COMPOUNDFLAG X",
			"COMPOUNDMORESUFFIXES",
			"PFX P Y 1",
			"PFX P 0 pre .",
			"SFX S Y 1",
			"SFX S 0 suf/T .",
			"SFX T Y 1",
			"SFX T 0 sff .");
		loadData(affFile, language);


		String line = "foo/XPS";
		final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(6, words.length);
		//base inflection
		Assertions.assertEquals(createInflection("foo", "XPS", "st:foo"), words[0]);
		//onefold inflections
		Assertions.assertEquals(createInflection("foosuf", "PT", "st:foo"), words[1]);
		//twofold inflections
		Assertions.assertEquals(createInflection("foosufsff", "P", "st:foo"), words[2]);
		//lastfold inflections
		Assertions.assertEquals(createInflection("prefoo", "S", "st:foo"), words[3]);
		Assertions.assertEquals(createInflection("prefoosuf", "T", "st:foo"), words[4]);
		Assertions.assertEquals(createInflection("prefoosufsff", null, "st:foo"), words[5]);


		String[] inputCompounds = new String[]{
			"foo/XPS",
			"bar/XPS"
		};
		words = wordGenerator.applyCompoundFlag(inputCompounds, 30, 2);

		Inflection[] expected = new Inflection[]{
			createInflection("foofoo", "PS", "pa:foo st:foo pa:foo st:foo"),
			createInflection("foofoosuf", "PT", "pa:foo st:foo pa:foo st:foo"),
			createInflection("foofoosufsff", "P", "pa:foo st:foo pa:foo st:foo"),
			createInflection("prefoofoo", "S", "pa:foo st:foo pa:foo st:foo"),
			createInflection("prefoofoosuf", "T", "pa:foo st:foo pa:foo st:foo"),
			createInflection("prefoofoosufsff", null, "pa:foo st:foo pa:foo st:foo"),
			createInflection("foobar", "PS", "pa:foo st:foo pa:bar st:bar"),
			createInflection("foobarsuf", "PT", "pa:foo st:foo pa:bar st:bar"),
			createInflection("foobarsufsff", "P", "pa:foo st:foo pa:bar st:bar"),
			createInflection("prefoobar", "S", "pa:foo st:foo pa:bar st:bar"),
			createInflection("prefoobarsuf", "T", "pa:foo st:foo pa:bar st:bar"),
			createInflection("prefoobarsufsff", null, "pa:foo st:foo pa:bar st:bar"),
			createInflection("barfoo", "PS", "pa:bar st:bar pa:foo st:foo"),
			createInflection("barfoosuf", "PT", "pa:bar st:bar pa:foo st:foo"),
			createInflection("barfoosufsff", "P", "pa:bar st:bar pa:foo st:foo"),
			createInflection("prebarfoo", "S", "pa:bar st:bar pa:foo st:foo"),
			createInflection("prebarfoosuf", "T", "pa:bar st:bar pa:foo st:foo"),
			createInflection("prebarfoosufsff", null, "pa:bar st:bar pa:foo st:foo"),
			createInflection("barbar", "PS", "pa:bar st:bar pa:bar st:bar"),
			createInflection("barbarsuf", "PT", "pa:bar st:bar pa:bar st:bar"),
			createInflection("barbarsufsff", "P", "pa:bar st:bar pa:bar st:bar"),
			createInflection("prebarbar", "S", "pa:bar st:bar pa:bar st:bar"),
			createInflection("prebarbarsuf", "T", "pa:bar st:bar pa:bar st:bar"),
			createInflection("prebarbarsufsff", null, "pa:bar st:bar pa:bar st:bar")
		};
		Assertions.assertArrayEquals(expected, words);
	}

	@Test
	void notPermitFlag() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"COMPOUNDFLAG X",
			"PFX P Y 1",
			"PFX P 0 pre .",
			"SFX S Y 1",
			"SFX S 0 suf .");
		loadData(affFile, language);


		String line = "foo/XPS";
		final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(4, words.length);
		//base inflection
		Assertions.assertEquals(createInflection("foo", "XPS", "st:foo"), words[0]);
		//onefold inflections
		Assertions.assertEquals(createInflection("foosuf", "P", "st:foo"), words[1]);
		//twofold inflections
		Assertions.assertEquals(createInflection("prefoo", "S", "st:foo"), words[2]);
		Assertions.assertEquals(createInflection("prefoosuf", "", "st:foo"), words[3]);
		//lastfold inflections


		String[] inputCompounds = new String[]{
			"foo/XPS",
			"bar/XPS"
		};
		words = wordGenerator.applyCompoundFlag(inputCompounds, 70, 2);

		Inflection[] expected = new Inflection[]{
			createInflection("foofoo", "PS", "pa:foo st:foo pa:foo st:foo"),
			createInflection("foofoosuf", "P", "pa:foo st:foo pa:foo st:foo"),
			createInflection("prefoofoo", "S", "pa:foo st:foo pa:foo st:foo"),
			createInflection("prefoofoosuf", null, "pa:foo st:foo pa:foo st:foo"),
			createInflection("foobar", "PS", "pa:foo st:foo pa:bar st:bar"),
			createInflection("foobarsuf", "P", "pa:foo st:foo pa:bar st:bar"),
			createInflection("prefoobar", "S", "pa:foo st:foo pa:bar st:bar"),
			createInflection("prefoobarsuf", null, "pa:foo st:foo pa:bar st:bar"),
			createInflection("barfoo", "PS", "pa:bar st:bar pa:foo st:foo"),
			createInflection("barfoosuf", "P", "pa:bar st:bar pa:foo st:foo"),
			createInflection("prebarfoo", "S", "pa:bar st:bar pa:foo st:foo"),
			createInflection("prebarfoosuf", null, "pa:bar st:bar pa:foo st:foo"),
			createInflection("barbar", "PS", "pa:bar st:bar pa:bar st:bar"),
			createInflection("barbarsuf", "P", "pa:bar st:bar pa:bar st:bar"),
			createInflection("prebarbar", "S", "pa:bar st:bar pa:bar st:bar"),
			createInflection("prebarbarsuf", null, "pa:bar st:bar pa:bar st:bar")
		};
		Assertions.assertArrayEquals(expected, words);
	}

	@Test
	void permitFlag() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"COMPOUNDFLAG X",
			"COMPOUNDPERMITFLAG Y",
			"PFX P Y 1",
			"PFX P 0 pre/Y .",
			"SFX S Y 1",
			"SFX S 0 suf/Y .");
		loadData(affFile, language);


		String line = "foo/XPS";
		final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(4, words.length);
		//base inflection
		Assertions.assertEquals(createInflection("foo", "XPS", "st:foo"), words[0]);
		//onefold inflections
		Assertions.assertEquals(createInflection("foosuf", "PY", "st:foo"), words[1]);
		//twofold inflections
		Assertions.assertEquals(createInflection("prefoo", "SY", "st:foo"), words[2]);
		Assertions.assertEquals(createInflection("prefoosuf", "Y", "st:foo"), words[3]);
		//lastfold inflections


		String[] inputCompounds = new String[]{
			"foo/XPS",
			"bar/XPS"
		};
		words = wordGenerator.applyCompoundFlag(inputCompounds, 70, 2);

		Inflection[] expected = new Inflection[]{
			createInflection("foofoo", "PS", "pa:foo st:foo pa:foo st:foo"),
			createInflection("foofoosuf", "PY", "pa:foo st:foo pa:foosuf st:foo"),
			createInflection("fooprefoo", "PSY", "pa:foo st:foo pa:prefoo st:foo"),
			createInflection("fooprefoosuf", "PY", "pa:foo st:foo pa:prefoosuf st:foo"),
			createInflection("foosuffoo", "PSY", "pa:foosuf st:foo pa:foo st:foo"),
			createInflection("foosuffoosuf", "PY", "pa:foosuf st:foo pa:foosuf st:foo"),
			createInflection("foosufprefoo", "PSY", "pa:foosuf st:foo pa:prefoo st:foo"),
			createInflection("foosufprefoosuf", "PY", "pa:foosuf st:foo pa:prefoosuf st:foo"),
			createInflection("prefoofoo", "SY", "pa:prefoo st:foo pa:foo st:foo"),
			createInflection("prefoofoosuf", "Y", "pa:prefoo st:foo pa:foosuf st:foo"),
			createInflection("prefooprefoo", "SY", "pa:prefoo st:foo pa:prefoo st:foo"),
			createInflection("prefooprefoosuf", "Y", "pa:prefoo st:foo pa:prefoosuf st:foo"),
			createInflection("prefoosuffoo", "SY", "pa:prefoosuf st:foo pa:foo st:foo"),
			createInflection("prefoosuffoosuf", "Y", "pa:prefoosuf st:foo pa:foosuf st:foo"),
			createInflection("prefoosufprefoo", "SY", "pa:prefoosuf st:foo pa:prefoo st:foo"),
			createInflection("prefoosufprefoosuf", "Y", "pa:prefoosuf st:foo pa:prefoosuf st:foo"),
			createInflection("foobar", "PS", "pa:foo st:foo pa:bar st:bar"),
			createInflection("foobarsuf", "PY", "pa:foo st:foo pa:barsuf st:bar"),
			createInflection("fooprebar", "PSY", "pa:foo st:foo pa:prebar st:bar"),
			createInflection("fooprebarsuf", "PY", "pa:foo st:foo pa:prebarsuf st:bar"),
			createInflection("foosufbar", "PSY", "pa:foosuf st:foo pa:bar st:bar"),
			createInflection("foosufbarsuf", "PY", "pa:foosuf st:foo pa:barsuf st:bar"),
			createInflection("foosufprebar", "PSY", "pa:foosuf st:foo pa:prebar st:bar"),
			createInflection("foosufprebarsuf", "PY", "pa:foosuf st:foo pa:prebarsuf st:bar"),
			createInflection("prefoobar", "SY", "pa:prefoo st:foo pa:bar st:bar"),
			createInflection("prefoobarsuf", "Y", "pa:prefoo st:foo pa:barsuf st:bar"),
			createInflection("prefooprebar", "SY", "pa:prefoo st:foo pa:prebar st:bar"),
			createInflection("prefooprebarsuf", "Y", "pa:prefoo st:foo pa:prebarsuf st:bar"),
			createInflection("prefoosufbar", "SY", "pa:prefoosuf st:foo pa:bar st:bar"),
			createInflection("prefoosufbarsuf", "Y", "pa:prefoosuf st:foo pa:barsuf st:bar"),
			createInflection("prefoosufprebar", "SY", "pa:prefoosuf st:foo pa:prebar st:bar"),
			createInflection("prefoosufprebarsuf", "Y", "pa:prefoosuf st:foo pa:prebarsuf st:bar"),
			createInflection("barfoo", "PS", "pa:bar st:bar pa:foo st:foo"),
			createInflection("barfoosuf", "PY", "pa:bar st:bar pa:foosuf st:foo"),
			createInflection("barprefoo", "PSY", "pa:bar st:bar pa:prefoo st:foo"),
			createInflection("barprefoosuf", "PY", "pa:bar st:bar pa:prefoosuf st:foo"),
			createInflection("barsuffoo", "PSY", "pa:barsuf st:bar pa:foo st:foo"),
			createInflection("barsuffoosuf", "PY", "pa:barsuf st:bar pa:foosuf st:foo"),
			createInflection("barsufprefoo", "PSY", "pa:barsuf st:bar pa:prefoo st:foo"),
			createInflection("barsufprefoosuf", "PY", "pa:barsuf st:bar pa:prefoosuf st:foo"),
			createInflection("prebarfoo", "SY", "pa:prebar st:bar pa:foo st:foo"),
			createInflection("prebarfoosuf", "Y", "pa:prebar st:bar pa:foosuf st:foo"),
			createInflection("prebarprefoo", "SY", "pa:prebar st:bar pa:prefoo st:foo"),
			createInflection("prebarprefoosuf", "Y", "pa:prebar st:bar pa:prefoosuf st:foo"),
			createInflection("prebarsuffoo", "SY", "pa:prebarsuf st:bar pa:foo st:foo"),
			createInflection("prebarsuffoosuf", "Y", "pa:prebarsuf st:bar pa:foosuf st:foo"),
			createInflection("prebarsufprefoo", "SY", "pa:prebarsuf st:bar pa:prefoo st:foo"),
			createInflection("prebarsufprefoosuf", "Y", "pa:prebarsuf st:bar pa:prefoosuf st:foo"),
			createInflection("barbar", "PS", "pa:bar st:bar pa:bar st:bar"),
			createInflection("barbarsuf", "PY", "pa:bar st:bar pa:barsuf st:bar"),
			createInflection("barprebar", "PSY", "pa:bar st:bar pa:prebar st:bar"),
			createInflection("barprebarsuf", "PY", "pa:bar st:bar pa:prebarsuf st:bar"),
			createInflection("barsufbar", "PSY", "pa:barsuf st:bar pa:bar st:bar"),
			createInflection("barsufbarsuf", "PY", "pa:barsuf st:bar pa:barsuf st:bar"),
			createInflection("barsufprebar", "PSY", "pa:barsuf st:bar pa:prebar st:bar"),
			createInflection("barsufprebarsuf", "PY", "pa:barsuf st:bar pa:prebarsuf st:bar"),
			createInflection("prebarbar", "SY", "pa:prebar st:bar pa:bar st:bar"),
			createInflection("prebarbarsuf", "Y", "pa:prebar st:bar pa:barsuf st:bar"),
			createInflection("prebarprebar", "SY", "pa:prebar st:bar pa:prebar st:bar"),
			createInflection("prebarprebarsuf", "Y", "pa:prebar st:bar pa:prebarsuf st:bar"),
			createInflection("prebarsufbar", "SY", "pa:prebarsuf st:bar pa:bar st:bar"),
			createInflection("prebarsufbarsuf", "Y", "pa:prebarsuf st:bar pa:barsuf st:bar"),
			createInflection("prebarsufprebar", "SY", "pa:prebarsuf st:bar pa:prebar st:bar"),
			createInflection("prebarsufprebarsuf", "Y", "pa:prebarsuf st:bar pa:prebarsuf st:bar")
		};
		Assertions.assertArrayEquals(expected, words);
	}

	@Test
	void forbidFlag() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"COMPOUNDFLAG X",
			"COMPOUNDFORBIDFLAG Z",
			"PFX P Y 1",
			"PFX P 0 pre/Z .",
			"SFX S Y 1",
			"SFX S 0 suf/Z .");
		loadData(affFile, language);


		String line = "foo/XPS";
		final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(4, words.length);
		//base inflection
		Assertions.assertEquals(createInflection("foo", "XPS", "st:foo"), words[0]);
		//onefold inflections
		Assertions.assertEquals(createInflection("foosuf", "PZ", "st:foo"), words[1]);
		//twofold inflections
		Assertions.assertEquals(createInflection("prefoo", "SZ", "st:foo"), words[2]);
		Assertions.assertEquals(createInflection("prefoosuf", "Z", "st:foo"), words[3]);
		//lastfold inflections


		String[] inputCompounds = new String[]{
			"foo/XPS",
			"bar/XPS"
		};
		words = wordGenerator.applyCompoundFlag(inputCompounds, 4, 2);

		Inflection[] expected = new Inflection[]{
			createInflection("foofoo", "PS", "pa:foo st:foo pa:foo st:foo"),
			createInflection("foobar", "PS", "pa:foo st:foo pa:bar st:bar"),
			createInflection("barfoo", "PS", "pa:bar st:bar pa:foo st:foo"),
			createInflection("barbar", "PS", "pa:bar st:bar pa:bar st:bar")
		};
		Assertions.assertArrayEquals(expected, words);
	}

	@Test
	void checkCompoundCase() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"COMPOUNDMIN 1",
			"CHECKCOMPOUNDCASE",
			"COMPOUNDFLAG A");
		loadData(affFile, language);


		String[] inputCompounds = new String[]{
			"foo/A",
			"Bar/A",
			"BAZ/A",
			"-/A"
		};

		Inflection[] words = wordGenerator.applyCompoundFlag(inputCompounds, 100, 3);

		Inflection[] expected = new Inflection[]{
			createInflection("foofoo", null, "pa:foo st:foo pa:foo st:foo"),
			createInflection("foobar", null, "pa:foo st:foo pa:Bar st:Bar"),
			createInflection("foobAZ", null, "pa:foo st:foo pa:BAZ st:BAZ"),
			createInflection("foo-", null, "pa:foo st:foo pa:- st:-"),
			createInflection("Barfoo", null, "pa:Bar st:Bar pa:foo st:foo"),
			createInflection("Barbar", null, "pa:Bar st:Bar pa:Bar st:Bar"),
			createInflection("BarbAZ", null, "pa:Bar st:Bar pa:BAZ st:BAZ"),
			createInflection("Bar-", null, "pa:Bar st:Bar pa:- st:-"),
			createInflection("BAZFoo", null, "pa:BAZ st:BAZ pa:foo st:foo"),
			createInflection("BAZBar", null, "pa:BAZ st:BAZ pa:Bar st:Bar"),
			createInflection("BAZBAZ", null, "pa:BAZ st:BAZ pa:BAZ st:BAZ"),
			createInflection("BAZ-", null, "pa:BAZ st:BAZ pa:- st:-"),
			createInflection("-foo", null, "pa:- st:- pa:foo st:foo"),
			createInflection("-Bar", null, "pa:- st:- pa:Bar st:Bar"),
			createInflection("-BAZ", null, "pa:- st:- pa:BAZ st:BAZ"),
			createInflection("--", null, "pa:- st:- pa:- st:-"),
			createInflection("foofoofoo", null, "pa:foo st:foo pa:foo st:foo pa:foo st:foo"),
			createInflection("foofoobar", null, "pa:foo st:foo pa:foo st:foo pa:Bar st:Bar"),
			createInflection("foofoobAZ", null, "pa:foo st:foo pa:foo st:foo pa:BAZ st:BAZ"),
			createInflection("foofoo-", null, "pa:foo st:foo pa:foo st:foo pa:- st:-"),
			createInflection("foobarfoo", null, "pa:foo st:foo pa:Bar st:Bar pa:foo st:foo"),
			createInflection("foobarbar", null, "pa:foo st:foo pa:Bar st:Bar pa:Bar st:Bar"),
			createInflection("foobarbAZ", null, "pa:foo st:foo pa:Bar st:Bar pa:BAZ st:BAZ"),
			createInflection("foobar-", null, "pa:foo st:foo pa:Bar st:Bar pa:- st:-"),
			createInflection("foobAZFoo", null, "pa:foo st:foo pa:BAZ st:BAZ pa:foo st:foo"),
			createInflection("foobAZBar", null, "pa:foo st:foo pa:BAZ st:BAZ pa:Bar st:Bar"),
			createInflection("foobAZBAZ", null, "pa:foo st:foo pa:BAZ st:BAZ pa:BAZ st:BAZ"),
			createInflection("foobAZ-", null, "pa:foo st:foo pa:BAZ st:BAZ pa:- st:-"),
			createInflection("foo-foo", null, "pa:foo st:foo pa:- st:- pa:foo st:foo"),
			createInflection("foo-Bar", null, "pa:foo st:foo pa:- st:- pa:Bar st:Bar"),
			createInflection("foo-BAZ", null, "pa:foo st:foo pa:- st:- pa:BAZ st:BAZ"),
			createInflection("foo--", null, "pa:foo st:foo pa:- st:- pa:- st:-"),
			createInflection("Barfoofoo", null, "pa:Bar st:Bar pa:foo st:foo pa:foo st:foo"),
			createInflection("Barfoobar", null, "pa:Bar st:Bar pa:foo st:foo pa:Bar st:Bar"),
			createInflection("BarfoobAZ", null, "pa:Bar st:Bar pa:foo st:foo pa:BAZ st:BAZ"),
			createInflection("Barfoo-", null, "pa:Bar st:Bar pa:foo st:foo pa:- st:-"),
			createInflection("Barbarfoo", null, "pa:Bar st:Bar pa:Bar st:Bar pa:foo st:foo"),
			createInflection("Barbarbar", null, "pa:Bar st:Bar pa:Bar st:Bar pa:Bar st:Bar"),
			createInflection("BarbarbAZ", null, "pa:Bar st:Bar pa:Bar st:Bar pa:BAZ st:BAZ"),
			createInflection("Barbar-", null, "pa:Bar st:Bar pa:Bar st:Bar pa:- st:-"),
			createInflection("BarbAZFoo", null, "pa:Bar st:Bar pa:BAZ st:BAZ pa:foo st:foo"),
			createInflection("BarbAZBar", null, "pa:Bar st:Bar pa:BAZ st:BAZ pa:Bar st:Bar"),
			createInflection("BarbAZBAZ", null, "pa:Bar st:Bar pa:BAZ st:BAZ pa:BAZ st:BAZ"),
			createInflection("BarbAZ-", null, "pa:Bar st:Bar pa:BAZ st:BAZ pa:- st:-"),
			createInflection("Bar-foo", null, "pa:Bar st:Bar pa:- st:- pa:foo st:foo"),
			createInflection("Bar-Bar", null, "pa:Bar st:Bar pa:- st:- pa:Bar st:Bar"),
			createInflection("Bar-BAZ", null, "pa:Bar st:Bar pa:- st:- pa:BAZ st:BAZ"),
			createInflection("Bar--", null, "pa:Bar st:Bar pa:- st:- pa:- st:-"),
			createInflection("BAZFoofoo", null, "pa:BAZ st:BAZ pa:foo st:foo pa:foo st:foo"),
			createInflection("BAZFoobar", null, "pa:BAZ st:BAZ pa:foo st:foo pa:Bar st:Bar"),
			createInflection("BAZFoobAZ", null, "pa:BAZ st:BAZ pa:foo st:foo pa:BAZ st:BAZ"),
			createInflection("BAZFoo-", null, "pa:BAZ st:BAZ pa:foo st:foo pa:- st:-"),
			createInflection("BAZBarfoo", null, "pa:BAZ st:BAZ pa:Bar st:Bar pa:foo st:foo"),
			createInflection("BAZBarbar", null, "pa:BAZ st:BAZ pa:Bar st:Bar pa:Bar st:Bar"),
			createInflection("BAZBarbAZ", null, "pa:BAZ st:BAZ pa:Bar st:Bar pa:BAZ st:BAZ"),
			createInflection("BAZBar-", null, "pa:BAZ st:BAZ pa:Bar st:Bar pa:- st:-"),
			createInflection("BAZBAZFoo", null, "pa:BAZ st:BAZ pa:BAZ st:BAZ pa:foo st:foo"),
			createInflection("BAZBAZBar", null, "pa:BAZ st:BAZ pa:BAZ st:BAZ pa:Bar st:Bar"),
			createInflection("BAZBAZBAZ", null, "pa:BAZ st:BAZ pa:BAZ st:BAZ pa:BAZ st:BAZ"),
			createInflection("BAZBAZ-", null, "pa:BAZ st:BAZ pa:BAZ st:BAZ pa:- st:-"),
			createInflection("BAZ-foo", null, "pa:BAZ st:BAZ pa:- st:- pa:foo st:foo"),
			createInflection("BAZ-Bar", null, "pa:BAZ st:BAZ pa:- st:- pa:Bar st:Bar"),
			createInflection("BAZ-BAZ", null, "pa:BAZ st:BAZ pa:- st:- pa:BAZ st:BAZ"),
			createInflection("BAZ--", null, "pa:BAZ st:BAZ pa:- st:- pa:- st:-"),
			createInflection("-foofoo", null, "pa:- st:- pa:foo st:foo pa:foo st:foo"),
			createInflection("-foobar", null, "pa:- st:- pa:foo st:foo pa:Bar st:Bar"),
			createInflection("-foobAZ", null, "pa:- st:- pa:foo st:foo pa:BAZ st:BAZ"),
			createInflection("-foo-", null, "pa:- st:- pa:foo st:foo pa:- st:-"),
			createInflection("-Barfoo", null, "pa:- st:- pa:Bar st:Bar pa:foo st:foo"),
			createInflection("-Barbar", null, "pa:- st:- pa:Bar st:Bar pa:Bar st:Bar"),
			createInflection("-BarbAZ", null, "pa:- st:- pa:Bar st:Bar pa:BAZ st:BAZ"),
			createInflection("-Bar-", null, "pa:- st:- pa:Bar st:Bar pa:- st:-"),
			createInflection("-BAZFoo", null, "pa:- st:- pa:BAZ st:BAZ pa:foo st:foo"),
			createInflection("-BAZBar", null, "pa:- st:- pa:BAZ st:BAZ pa:Bar st:Bar"),
			createInflection("-BAZBAZ", null, "pa:- st:- pa:BAZ st:BAZ pa:BAZ st:BAZ"),
			createInflection("-BAZ-", null, "pa:- st:- pa:BAZ st:BAZ pa:- st:-"),
			createInflection("--foo", null, "pa:- st:- pa:- st:- pa:foo st:foo"),
			createInflection("--Bar", null, "pa:- st:- pa:- st:- pa:Bar st:Bar"),
			createInflection("--BAZ", null, "pa:- st:- pa:- st:- pa:BAZ st:BAZ"),
			createInflection("---", null, "pa:- st:- pa:- st:- pa:- st:-")
		};
		Assertions.assertArrayEquals(expected, words);
	}

	@Test
	void compoundReplacement() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"CHECKCOMPOUNDREP",
			"COMPOUNDFLAG A",
			"REP 1",
			"REP í i");
		File dicFile = FileHelper.createDeleteOnExitFile(language, ".dic",
			"4",
			"szer/A",
			"víz/A",
			"kocsi/A",
			"szerviz");
		loadData(affFile, dicFile, language);

		String[] inputCompounds = new String[]{
			"szer/A",
			"víz/A",
			"kocsi/A",
			"szerviz"
		};
		Inflection[] words = wordGenerator.applyCompoundFlag(inputCompounds, 40, 3);

		Inflection[] expected = new Inflection[]{
			createInflection("szerszer", null, "pa:szer st:szer pa:szer st:szer"),
			createInflection("szerkocsi", null, "pa:szer st:szer pa:kocsi st:kocsi"),
			createInflection("szerszerviz", null, "pa:szer st:szer pa:szerviz st:szerviz"),
			createInflection("vízszer", null, "pa:víz st:víz pa:szer st:szer"),
			createInflection("vízvíz", null, "pa:víz st:víz pa:víz st:víz"),
			createInflection("vízkocsi", null, "pa:víz st:víz pa:kocsi st:kocsi"),
			createInflection("vízszerviz", null, "pa:víz st:víz pa:szerviz st:szerviz"),
			createInflection("kocsiszer", null, "pa:kocsi st:kocsi pa:szer st:szer"),
			createInflection("kocsivíz", null, "pa:kocsi st:kocsi pa:víz st:víz"),
			createInflection("kocsikocsi", null, "pa:kocsi st:kocsi pa:kocsi st:kocsi"),
			createInflection("kocsiszerviz", null, "pa:kocsi st:kocsi pa:szerviz st:szerviz"),
			createInflection("szervizszer", null, "pa:szerviz st:szerviz pa:szer st:szer"),
			createInflection("szervizvíz", null, "pa:szerviz st:szerviz pa:víz st:víz"),
			createInflection("szervizkocsi", null, "pa:szerviz st:szerviz pa:kocsi st:kocsi"),
			createInflection("szervizszerviz", null, "pa:szerviz st:szerviz pa:szerviz st:szerviz"),
			createInflection("szerszerszer", null, "pa:szer st:szer pa:szer st:szer pa:szer st:szer"),
			createInflection("szerszerkocsi", null, "pa:szer st:szer pa:szer st:szer pa:kocsi st:kocsi"),
			createInflection("szerszerszerviz", null, "pa:szer st:szer pa:szer st:szer pa:szerviz st:szerviz"),
			createInflection("szerkocsiszer", null, "pa:szer st:szer pa:kocsi st:kocsi pa:szer st:szer"),
			createInflection("szerkocsivíz", null, "pa:szer st:szer pa:kocsi st:kocsi pa:víz st:víz"),
			createInflection("szerkocsikocsi", null, "pa:szer st:szer pa:kocsi st:kocsi pa:kocsi st:kocsi"),
			createInflection("szerkocsiszerviz", null, "pa:szer st:szer pa:kocsi st:kocsi pa:szerviz st:szerviz"),
			createInflection("szerszervizszer", null, "pa:szer st:szer pa:szerviz st:szerviz pa:szer st:szer"),
			createInflection("szerszervizvíz", null, "pa:szer st:szer pa:szerviz st:szerviz pa:víz st:víz"),
			createInflection("szerszervizkocsi", null, "pa:szer st:szer pa:szerviz st:szerviz pa:kocsi st:kocsi"),
			createInflection("szerszervizszerviz", null, "pa:szer st:szer pa:szerviz st:szerviz pa:szerviz st:szerviz"),
			createInflection("vízszerszer", null, "pa:víz st:víz pa:szer st:szer pa:szer st:szer"),
			createInflection("vízszerkocsi", null, "pa:víz st:víz pa:szer st:szer pa:kocsi st:kocsi"),
			createInflection("vízszerszerviz", null, "pa:víz st:víz pa:szer st:szer pa:szerviz st:szerviz"),
			createInflection("vízvízszer", null, "pa:víz st:víz pa:víz st:víz pa:szer st:szer"),
			createInflection("vízvízvíz", null, "pa:víz st:víz pa:víz st:víz pa:víz st:víz"),
			createInflection("vízvízkocsi", null, "pa:víz st:víz pa:víz st:víz pa:kocsi st:kocsi"),
			createInflection("vízvízszerviz", null, "pa:víz st:víz pa:víz st:víz pa:szerviz st:szerviz")
		};
		Assertions.assertArrayEquals(expected, words);
	}


	@Test
	void forbiddenWord() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"FORBIDDENWORD X",
			"COMPOUNDFLAG Y",
			"SFX S Y 1",
			"SFX S 0 s .");
		loadData(affFile, language);

		String[] inputCompounds = new String[]{
			"foo/S	po:1",
			"foo/YX	po:2",
			"foo/Y	po:3",
			"bar/YS	po:4",
			"bars/X",
			"foos/X"
		};
		Inflection[] words = wordGenerator.applyCompoundFlag(inputCompounds, 100, 2);

		Inflection[] expected = new Inflection[]{
			createInflection("foofoo", "S", "pa:foo st:foo po:1 pa:foo st:foo po:1"),
			createInflection("foofoos", null, "pa:foo st:foo po:1 pa:foo st:foo po:1"),
			createInflection("foofoo", null, "pa:foo st:foo po:1 pa:foo st:foo po:3"),
			createInflection("foobar", "S", "pa:foo st:foo po:1 pa:bar st:bar po:4"),
			createInflection("foobars", null, "pa:foo st:foo po:1 pa:bar st:bar po:4"),
			createInflection("foofoo", "S", "pa:foo st:foo po:3 pa:foo st:foo po:1"),
			createInflection("foofoos", null, "pa:foo st:foo po:3 pa:foo st:foo po:1"),
			createInflection("foofoo", null, "pa:foo st:foo po:3 pa:foo st:foo po:3"),
			createInflection("foobar", "S", "pa:foo st:foo po:3 pa:bar st:bar po:4"),
			createInflection("foobars", null, "pa:foo st:foo po:3 pa:bar st:bar po:4"),
			createInflection("barfoo", "S", "pa:bar st:bar po:4 pa:foo st:foo po:1"),
			createInflection("barfoos", null, "pa:bar st:bar po:4 pa:foo st:foo po:1"),
			createInflection("barfoo", null, "pa:bar st:bar po:4 pa:foo st:foo po:3"),
			createInflection("barbar", "S", "pa:bar st:bar po:4 pa:bar st:bar po:4"),
			createInflection("barbars", null, "pa:bar st:bar po:4 pa:bar st:bar po:4")
		};
		Assertions.assertArrayEquals(expected, words);
	}


	@Test
	void forceUppercase() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"FORCEUCASE A",
			"COMPOUNDFLAG C");
		loadData(affFile, language);

		String[] inputCompounds = new String[]{
			"foo/C",
			"bar/C",
			"baz/CA"
		};
		Inflection[] words = wordGenerator.applyCompoundFlag(inputCompounds, 100, 3);

		Inflection[] expected = new Inflection[]{
			createInflection("foofoo", null, "pa:foo st:foo pa:foo st:foo"),
			createInflection("foobar", null, "pa:foo st:foo pa:bar st:bar"),
			createInflection("Foobaz", null, "pa:foo st:foo pa:baz st:baz"),
			createInflection("barfoo", null, "pa:bar st:bar pa:foo st:foo"),
			createInflection("barbar", null, "pa:bar st:bar pa:bar st:bar"),
			createInflection("Barbaz", null, "pa:bar st:bar pa:baz st:baz"),
			createInflection("bazfoo", null, "pa:baz st:baz pa:foo st:foo"),
			createInflection("bazbar", null, "pa:baz st:baz pa:bar st:bar"),
			createInflection("Bazbaz", null, "pa:baz st:baz pa:baz st:baz"),
			createInflection("foofoofoo", null, "pa:foo st:foo pa:foo st:foo pa:foo st:foo"),
			createInflection("foofoobar", null, "pa:foo st:foo pa:foo st:foo pa:bar st:bar"),
			createInflection("Foofoobaz", null, "pa:foo st:foo pa:foo st:foo pa:baz st:baz"),
			createInflection("foobarfoo", null, "pa:foo st:foo pa:bar st:bar pa:foo st:foo"),
			createInflection("foobarbar", null, "pa:foo st:foo pa:bar st:bar pa:bar st:bar"),
			createInflection("Foobarbaz", null, "pa:foo st:foo pa:bar st:bar pa:baz st:baz"),
			createInflection("foobazfoo", null, "pa:foo st:foo pa:baz st:baz pa:foo st:foo"),
			createInflection("foobazbar", null, "pa:foo st:foo pa:baz st:baz pa:bar st:bar"),
			createInflection("Foobazbaz", null, "pa:foo st:foo pa:baz st:baz pa:baz st:baz"),
			createInflection("barfoofoo", null, "pa:bar st:bar pa:foo st:foo pa:foo st:foo"),
			createInflection("barfoobar", null, "pa:bar st:bar pa:foo st:foo pa:bar st:bar"),
			createInflection("Barfoobaz", null, "pa:bar st:bar pa:foo st:foo pa:baz st:baz"),
			createInflection("barbarfoo", null, "pa:bar st:bar pa:bar st:bar pa:foo st:foo"),
			createInflection("barbarbar", null, "pa:bar st:bar pa:bar st:bar pa:bar st:bar"),
			createInflection("Barbarbaz", null, "pa:bar st:bar pa:bar st:bar pa:baz st:baz"),
			createInflection("barbazfoo", null, "pa:bar st:bar pa:baz st:baz pa:foo st:foo"),
			createInflection("barbazbar", null, "pa:bar st:bar pa:baz st:baz pa:bar st:bar"),
			createInflection("Barbazbaz", null, "pa:bar st:bar pa:baz st:baz pa:baz st:baz"),
			createInflection("bazfoofoo", null, "pa:baz st:baz pa:foo st:foo pa:foo st:foo"),
			createInflection("bazfoobar", null, "pa:baz st:baz pa:foo st:foo pa:bar st:bar"),
			createInflection("Bazfoobaz", null, "pa:baz st:baz pa:foo st:foo pa:baz st:baz"),
			createInflection("bazbarfoo", null, "pa:baz st:baz pa:bar st:bar pa:foo st:foo"),
			createInflection("bazbarbar", null, "pa:baz st:baz pa:bar st:bar pa:bar st:bar"),
			createInflection("Bazbarbaz", null, "pa:baz st:baz pa:bar st:bar pa:baz st:baz"),
			createInflection("bazbazfoo", null, "pa:baz st:baz pa:baz st:baz pa:foo st:foo"),
			createInflection("bazbazbar", null, "pa:baz st:baz pa:baz st:baz pa:bar st:bar"),
			createInflection("Bazbazbaz", null, "pa:baz st:baz pa:baz st:baz pa:baz st:baz")
		};
		Assertions.assertArrayEquals(expected, words);
	}

	@Test
	void onlyInCompound() throws IOException{
		String language = "en-GB";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
			"SET UTF-8",
			"ONLYINCOMPOUND O",
			"COMPOUNDFLAG A",
			"SFX B Y 1",
			"SFX B 0 s .");
		loadData(affFile, language);

		String line = "foo/A";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		Inflection[] words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(1, words.length);
		//base inflection
		//suffix inflections
		Assertions.assertEquals(createInflection("foo", "A", "st:foo"), words[0]);
		//prefix inflections
		//twofold inflections

		line = "pseudo/OAB";
		dicEntry = wordGenerator.createFromDictionaryLine(line);
		words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(1, words.length);
		//base inflection
		//suffix inflections
		Assertions.assertEquals(createInflection("pseudos", null, "st:pseudo"), words[0]);
		//prefix inflections
		//twofold inflections


		String[] inputCompounds = new String[]{
			"foo/A",
			"pseudo/OAB"
		};
		words = wordGenerator.applyCompoundFlag(inputCompounds, 10, 2);

		Inflection[] expected = new Inflection[]{
			createInflection("foofoo", null, "pa:foo st:foo pa:foo st:foo"),
			createInflection("foopseudo", "BO", "pa:foo st:foo pa:pseudo st:pseudo"),
			createInflection("foopseudos", null, "pa:foo st:foo pa:pseudo st:pseudo"),
			createInflection("pseudofoo", "O", "pa:pseudo st:pseudo pa:foo st:foo"),
			createInflection("pseudopseudo", "BO", "pa:pseudo st:pseudo pa:pseudo st:pseudo"),
			createInflection("pseudopseudos", null, "pa:pseudo st:pseudo pa:pseudo st:pseudo")
		};
		Assertions.assertArrayEquals(expected, words);
	}

	//FIXME manage CHECKCOMPOUNDPATTERN
//	@Test
//	void onlyInCompound2_checkCompoundPattern() throws IOException{
//		String language = "en-GB";
//		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
//			"SET UTF-8",
//			//affixes only in compounds (see also fogemorpheme example)
//			"ONLYINCOMPOUND O",
//			"COMPOUNDFLAG A",
//			"COMPOUNDPERMITFLAG P",
//			"SFX B Y 1",
//			"SFX B 0 s/OP .",
//			//obligate fogemorpheme by forbidding the stem (0) in compounds
//			"CHECKCOMPOUNDPATTERN 1",
//			//avoid generation of `pseudofoo`, but allow `pseudosfoo`
//			"CHECKCOMPOUNDPATTERN 0/B /A");
//		TestHelper.loadData(affFile, language);
//
//		String[] inputCompounds = new String[]{
//			"foo/A",
//			"pseudo/AB"
//		};
//		Inflection[] words = wordGenerator.applyCompoundFlag(inputCompounds, 10, 2);
//
//		Inflection[] expected = new Inflection[]{
//			TestHelper.createInflection("foofoo", null, "pa:foo st:foo pa:foo st:foo"),
//			TestHelper.createInflection("foopseudo", "B", "pa:foo st:foo pa:pseudo st:pseudo"),
//			TestHelper.createInflection("foopseudos", "PO", "pa:foo st:foo pa:pseudos st:pseudo"),
//			TestHelper.createInflection("pseudosfoo", "PO", "pa:pseudos st:pseudo pa:foo st:foo"),
//			TestHelper.createInflection("pseudopseudo", "B", "pa:pseudo st:pseudo pa:pseudo st:pseudo"),
//			TestHelper.createInflection("pseudopseudos", "PO", "pa:pseudo st:pseudo pa:pseudos st:pseudo"),
//			TestHelper.createInflection("pseudospseudo", "BPO", "pa:pseudos st:pseudo pa:pseudo st:pseudo"),
//			TestHelper.createInflection("pseudospseudos", "PO", "pa:pseudos st:pseudo pa:pseudos st:pseudo")
//		};
//		Assertions.assertArrayEquals(expected, words);
//	}

}
