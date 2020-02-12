package unit731.hunlinter.parsers.dictionary.generators;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.affix.AffixParser;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.services.FileHelper;
import unit731.hunlinter.services.PermutationsWithRepetitions;


/** @see <a href="https://github.com/hunspell/hunspell/tree/master/tests/v1cmdline">Hunspell tests</a> */
class WordGeneratorCompoundFlagTest{

	private AffixData affixData;
	private WordGenerator wordGenerator;


	@Test
	void simple() throws IOException, SAXException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
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
		List<Production> words = wordGenerator.applyCompoundFlag(inputCompounds, 10, PermutationsWithRepetitions.MAX_COMPOUNDS_INFINITY);

		List<Production> expected = Arrays.asList(
			createProduction("foofoo", null, "pa:foo st:foo pa:foo st:foo"),
			createProduction("foobar", null, "pa:foo st:foo pa:bar st:bar"),
			createProduction("fooxy", null, "pa:foo st:foo pa:xy st:xy"),
			createProduction("fooyz", null, "pa:foo st:foo pa:yz st:yz"),
			createProduction("barfoo", null, "pa:bar st:bar pa:foo st:foo"),
			createProduction("barbar", null, "pa:bar st:bar pa:bar st:bar"),
			createProduction("barxy", null, "pa:bar st:bar pa:xy st:xy"),
			createProduction("baryz", null, "pa:bar st:bar pa:yz st:yz"),
			createProduction("xyfoo", null, "pa:xy st:xy pa:foo st:foo"),
			createProduction("xybar", null, "pa:xy st:xy pa:bar st:bar")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	void compoundMinLength() throws IOException, SAXException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPOUNDMIN 3",
			"COMPOUNDFLAG A");
		loadData(affFile, language);

		String[] inputCompounds = new String[]{
			"foo/A",
			"bar/A",
			"yz/A"
		};
		List<Production> words = wordGenerator.applyCompoundFlag(inputCompounds, 100, 2);

		List<Production> expected = Arrays.asList(
			createProduction("foofoo", null, "pa:foo st:foo pa:foo st:foo"),
			createProduction("foobar", null, "pa:foo st:foo pa:bar st:bar"),
			createProduction("barfoo", null, "pa:bar st:bar pa:foo st:foo"),
			createProduction("barbar", null, "pa:bar st:bar pa:bar st:bar")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	void checkCompoundTriple() throws IOException, SAXException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
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
		List<Production> words = wordGenerator.applyCompoundFlag(inputCompounds, 12, PermutationsWithRepetitions.MAX_COMPOUNDS_INFINITY);

		List<Production> expected = Arrays.asList(
			createProduction("foofoo", null, "pa:foo st:foo pa:foo st:foo"),
			createProduction("fooeel", null, "pa:foo st:foo pa:eel st:eel"),
			createProduction("foobare", null, "pa:foo st:foo pa:bare st:bare"),
			createProduction("operafoo", null, "pa:opera st:opera pa:foo st:foo"),
			createProduction("operaopera", null, "pa:opera st:opera pa:opera st:opera"),
			createProduction("operaeel", null, "pa:opera st:opera pa:eel st:eel"),
			createProduction("operabare", null, "pa:opera st:opera pa:bare st:bare"),
			createProduction("eelfoo", null, "pa:eel st:eel pa:foo st:foo"),
			createProduction("eelopera", null, "pa:eel st:eel pa:opera st:opera"),
			createProduction("eeleel", null, "pa:eel st:eel pa:eel st:eel"),
			createProduction("eelbare", null, "pa:eel st:eel pa:bare st:bare")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	void simplifiedTriple() throws IOException, SAXException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
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
		List<Production> words = wordGenerator.applyCompoundFlag(inputCompounds, 3, PermutationsWithRepetitions.MAX_COMPOUNDS_INFINITY);

		List<Production> expected = Arrays.asList(
			createProduction("glassglass", null, "pa:glass st:glass pa:glass st:glass"),
			createProduction("glassko", null, "pa:glass st:glass pa:sko st:sko"),
			createProduction("skoglass", null, "pa:sko st:sko pa:glass st:glass")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	void forbidWordDuplication() throws IOException, SAXException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
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
		List<Production> words = wordGenerator.applyCompoundFlag(inputCompounds, 100, 2);

		List<Production> expected = Arrays.asList(
			createProduction("foobar", null, "pa:foo st:foo pa:bar st:bar"),
			createProduction("fooyz", null, "pa:foo st:foo pa:yz st:yz"),
			createProduction("barfoo", null, "pa:bar st:bar pa:foo st:foo"),
			createProduction("baryz", null, "pa:bar st:bar pa:yz st:yz"),
			createProduction("yzfoo", null, "pa:yz st:yz pa:foo st:foo"),
			createProduction("yzbar", null, "pa:yz st:yz pa:bar st:bar")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	void withAffixes() throws IOException, SAXException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPOUNDFLAG X",
			"PFX P Y 1",
			"PFX P 0 pre .",
			"SFX S Y 1",
			"SFX S 0 suf .");
		loadData(affFile, language);


		String line = "foo/XPS";
		final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(4, words.size());
		//base production
		Assertions.assertEquals(createProduction("foo", "XPS", "st:foo"), words.get(0));
		//onefold productions
		Assertions.assertEquals(createProduction("foosuf", "P", "st:foo"), words.get(1));
		//twofold productions
		Assertions.assertEquals(createProduction("prefoo", "S", "st:foo"), words.get(2));
		Assertions.assertEquals(createProduction("prefoosuf", null, "st:foo"), words.get(3));
		//lastfold productions


		String[] inputCompounds = new String[]{
			"foo/XPS",
			"bar/XPS"
		};
		words = wordGenerator.applyCompoundFlag(inputCompounds, 20, 2);

		List<Production> expected = Arrays.asList(
			createProduction("foofoo", "PS", "pa:foo st:foo pa:foo st:foo"),
			createProduction("foofoosuf", "P", "pa:foo st:foo pa:foo st:foo"),
			createProduction("prefoofoo", "S", "pa:foo st:foo pa:foo st:foo"),
			createProduction("prefoofoosuf", null, "pa:foo st:foo pa:foo st:foo"),
			createProduction("foobar", "PS", "pa:foo st:foo pa:bar st:bar"),
			createProduction("foobarsuf", "P", "pa:foo st:foo pa:bar st:bar"),
			createProduction("prefoobar", "S", "pa:foo st:foo pa:bar st:bar"),
			createProduction("prefoobarsuf", null, "pa:foo st:foo pa:bar st:bar"),
			createProduction("barfoo", "PS", "pa:bar st:bar pa:foo st:foo"),
			createProduction("barfoosuf", "P", "pa:bar st:bar pa:foo st:foo"),
			createProduction("prebarfoo", "S", "pa:bar st:bar pa:foo st:foo"),
			createProduction("prebarfoosuf", null, "pa:bar st:bar pa:foo st:foo"),
			createProduction("barbar", "PS", "pa:bar st:bar pa:bar st:bar"),
			createProduction("barbarsuf", "P", "pa:bar st:bar pa:bar st:bar"),
			createProduction("prebarbar", "S", "pa:bar st:bar pa:bar st:bar"),
			createProduction("prebarbarsuf", null, "pa:bar st:bar pa:bar st:bar")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	void withAffixesOnefold() throws IOException, SAXException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
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
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(6, words.size());
		//base production
		Assertions.assertEquals(createProduction("foo", "XPS", "st:foo"), words.get(0));
		//onefold productions
		Assertions.assertEquals(createProduction("foosuf", "PT", "st:foo"), words.get(1));
		//twofold productions
		Assertions.assertEquals(createProduction("foosufsff", "P", "st:foo"), words.get(2));
		//lastfold productions
		Assertions.assertEquals(createProduction("prefoo", "S", "st:foo"), words.get(3));
		Assertions.assertEquals(createProduction("prefoosuf", "T", "st:foo"), words.get(4));
		Assertions.assertEquals(createProduction("prefoosufsff", null, "st:foo"), words.get(5));


		String[] inputCompounds = new String[]{
			"foo/XPS",
			"bar/XPS"
		};
		words = wordGenerator.applyCompoundFlag(inputCompounds, 20, 2);

		List<Production> expected = Arrays.asList(
			createProduction("foofoo", "PS", "pa:foo st:foo pa:foo st:foo"),
			createProduction("foofoosuf", "PT", "pa:foo st:foo pa:foo st:foo"),
			createProduction("prefoofoo", "S", "pa:foo st:foo pa:foo st:foo"),
			createProduction("prefoofoosuf", "T", "pa:foo st:foo pa:foo st:foo"),
			createProduction("foobar", "PS", "pa:foo st:foo pa:bar st:bar"),
			createProduction("foobarsuf", "PT", "pa:foo st:foo pa:bar st:bar"),
			createProduction("prefoobar", "S", "pa:foo st:foo pa:bar st:bar"),
			createProduction("prefoobarsuf", "T", "pa:foo st:foo pa:bar st:bar"),
			createProduction("barfoo", "PS", "pa:bar st:bar pa:foo st:foo"),
			createProduction("barfoosuf", "PT", "pa:bar st:bar pa:foo st:foo"),
			createProduction("prebarfoo", "S", "pa:bar st:bar pa:foo st:foo"),
			createProduction("prebarfoosuf", "T", "pa:bar st:bar pa:foo st:foo"),
			createProduction("barbar", "PS", "pa:bar st:bar pa:bar st:bar"),
			createProduction("barbarsuf", "PT", "pa:bar st:bar pa:bar st:bar"),
			createProduction("prebarbar", "S", "pa:bar st:bar pa:bar st:bar"),
			createProduction("prebarbarsuf", "T", "pa:bar st:bar pa:bar st:bar")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	void withAffixesTwofold() throws IOException, SAXException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
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
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(6, words.size());
		//base production
		Assertions.assertEquals(createProduction("foo", "XPS", "st:foo"), words.get(0));
		//onefold productions
		Assertions.assertEquals(createProduction("foosuf", "PT", "st:foo"), words.get(1));
		//twofold productions
		Assertions.assertEquals(createProduction("foosufsff", "P", "st:foo"), words.get(2));
		//lastfold productions
		Assertions.assertEquals(createProduction("prefoo", "S", "st:foo"), words.get(3));
		Assertions.assertEquals(createProduction("prefoosuf", "T", "st:foo"), words.get(4));
		Assertions.assertEquals(createProduction("prefoosufsff", null, "st:foo"), words.get(5));


		String[] inputCompounds = new String[]{
			"foo/XPS",
			"bar/XPS"
		};
		words = wordGenerator.applyCompoundFlag(inputCompounds, 30, 2);

		List<Production> expected = Arrays.asList(
			createProduction("foofoo", "PS", "pa:foo st:foo pa:foo st:foo"),
			createProduction("foofoosuf", "PT", "pa:foo st:foo pa:foo st:foo"),
			createProduction("foofoosufsff", "P", "pa:foo st:foo pa:foo st:foo"),
			createProduction("prefoofoo", "S", "pa:foo st:foo pa:foo st:foo"),
			createProduction("prefoofoosuf", "T", "pa:foo st:foo pa:foo st:foo"),
			createProduction("prefoofoosufsff", null, "pa:foo st:foo pa:foo st:foo"),
			createProduction("foobar", "PS", "pa:foo st:foo pa:bar st:bar"),
			createProduction("foobarsuf", "PT", "pa:foo st:foo pa:bar st:bar"),
			createProduction("foobarsufsff", "P", "pa:foo st:foo pa:bar st:bar"),
			createProduction("prefoobar", "S", "pa:foo st:foo pa:bar st:bar"),
			createProduction("prefoobarsuf", "T", "pa:foo st:foo pa:bar st:bar"),
			createProduction("prefoobarsufsff", null, "pa:foo st:foo pa:bar st:bar"),
			createProduction("barfoo", "PS", "pa:bar st:bar pa:foo st:foo"),
			createProduction("barfoosuf", "PT", "pa:bar st:bar pa:foo st:foo"),
			createProduction("barfoosufsff", "P", "pa:bar st:bar pa:foo st:foo"),
			createProduction("prebarfoo", "S", "pa:bar st:bar pa:foo st:foo"),
			createProduction("prebarfoosuf", "T", "pa:bar st:bar pa:foo st:foo"),
			createProduction("prebarfoosufsff", null, "pa:bar st:bar pa:foo st:foo"),
			createProduction("barbar", "PS", "pa:bar st:bar pa:bar st:bar"),
			createProduction("barbarsuf", "PT", "pa:bar st:bar pa:bar st:bar"),
			createProduction("barbarsufsff", "P", "pa:bar st:bar pa:bar st:bar"),
			createProduction("prebarbar", "S", "pa:bar st:bar pa:bar st:bar"),
			createProduction("prebarbarsuf", "T", "pa:bar st:bar pa:bar st:bar"),
			createProduction("prebarbarsufsff", null, "pa:bar st:bar pa:bar st:bar")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	void permitFlag() throws IOException, SAXException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
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
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(4, words.size());
		//base production
		Assertions.assertEquals(createProduction("foo", "XPS", "st:foo"), words.get(0));
		//onefold productions
		Assertions.assertEquals(createProduction("foosuf", "PY", "st:foo"), words.get(1));
		//twofold productions
		Assertions.assertEquals(createProduction("prefoo", "SY", "st:foo"), words.get(2));
		Assertions.assertEquals(createProduction("prefoosuf", "Y", "st:foo"), words.get(3));
		//lastfold productions


		String[] inputCompounds = new String[]{
			"foo/XPS",
			"bar/XPS"
		};
		words = wordGenerator.applyCompoundFlag(inputCompounds, 70, 2);

		List<Production> expected = Arrays.asList(
			createProduction("foofoo", "PS", "pa:foo st:foo pa:foo st:foo"),
			createProduction("foofoosuf", "PY", "pa:foo st:foo pa:foosuf st:foo"),
			createProduction("fooprefoo", "PSY", "pa:foo st:foo pa:prefoo st:foo"),
			createProduction("fooprefoosuf", "PY", "pa:foo st:foo pa:prefoosuf st:foo"),
			createProduction("foosuffoo", "PSY", "pa:foosuf st:foo pa:foo st:foo"),
			createProduction("foosuffoosuf", "PY", "pa:foosuf st:foo pa:foosuf st:foo"),
			createProduction("foosufprefoo", "PSY", "pa:foosuf st:foo pa:prefoo st:foo"),
			createProduction("foosufprefoosuf", "PY", "pa:foosuf st:foo pa:prefoosuf st:foo"),
			createProduction("prefoofoo", "SY", "pa:prefoo st:foo pa:foo st:foo"),
			createProduction("prefoofoosuf", "Y", "pa:prefoo st:foo pa:foosuf st:foo"),
			createProduction("prefooprefoo", "SY", "pa:prefoo st:foo pa:prefoo st:foo"),
			createProduction("prefooprefoosuf", "Y", "pa:prefoo st:foo pa:prefoosuf st:foo"),
			createProduction("prefoosuffoo", "SY", "pa:prefoosuf st:foo pa:foo st:foo"),
			createProduction("prefoosuffoosuf", "Y", "pa:prefoosuf st:foo pa:foosuf st:foo"),
			createProduction("prefoosufprefoo", "SY", "pa:prefoosuf st:foo pa:prefoo st:foo"),
			createProduction("prefoosufprefoosuf", "Y", "pa:prefoosuf st:foo pa:prefoosuf st:foo"),
			createProduction("foobar", "PS", "pa:foo st:foo pa:bar st:bar"),
			createProduction("foobarsuf", "PY", "pa:foo st:foo pa:barsuf st:bar"),
			createProduction("fooprebar", "PSY", "pa:foo st:foo pa:prebar st:bar"),
			createProduction("fooprebarsuf", "PY", "pa:foo st:foo pa:prebarsuf st:bar"),
			createProduction("foosufbar", "PSY", "pa:foosuf st:foo pa:bar st:bar"),
			createProduction("foosufbarsuf", "PY", "pa:foosuf st:foo pa:barsuf st:bar"),
			createProduction("foosufprebar", "PSY", "pa:foosuf st:foo pa:prebar st:bar"),
			createProduction("foosufprebarsuf", "PY", "pa:foosuf st:foo pa:prebarsuf st:bar"),
			createProduction("prefoobar", "SY", "pa:prefoo st:foo pa:bar st:bar"),
			createProduction("prefoobarsuf", "Y", "pa:prefoo st:foo pa:barsuf st:bar"),
			createProduction("prefooprebar", "SY", "pa:prefoo st:foo pa:prebar st:bar"),
			createProduction("prefooprebarsuf", "Y", "pa:prefoo st:foo pa:prebarsuf st:bar"),
			createProduction("prefoosufbar", "SY", "pa:prefoosuf st:foo pa:bar st:bar"),
			createProduction("prefoosufbarsuf", "Y", "pa:prefoosuf st:foo pa:barsuf st:bar"),
			createProduction("prefoosufprebar", "SY", "pa:prefoosuf st:foo pa:prebar st:bar"),
			createProduction("prefoosufprebarsuf", "Y", "pa:prefoosuf st:foo pa:prebarsuf st:bar"),
			createProduction("barfoo", "PS", "pa:bar st:bar pa:foo st:foo"),
			createProduction("barfoosuf", "PY", "pa:bar st:bar pa:foosuf st:foo"),
			createProduction("barprefoo", "PSY", "pa:bar st:bar pa:prefoo st:foo"),
			createProduction("barprefoosuf", "PY", "pa:bar st:bar pa:prefoosuf st:foo"),
			createProduction("barsuffoo", "PSY", "pa:barsuf st:bar pa:foo st:foo"),
			createProduction("barsuffoosuf", "PY", "pa:barsuf st:bar pa:foosuf st:foo"),
			createProduction("barsufprefoo", "PSY", "pa:barsuf st:bar pa:prefoo st:foo"),
			createProduction("barsufprefoosuf", "PY", "pa:barsuf st:bar pa:prefoosuf st:foo"),
			createProduction("prebarfoo", "SY", "pa:prebar st:bar pa:foo st:foo"),
			createProduction("prebarfoosuf", "Y", "pa:prebar st:bar pa:foosuf st:foo"),
			createProduction("prebarprefoo", "SY", "pa:prebar st:bar pa:prefoo st:foo"),
			createProduction("prebarprefoosuf", "Y", "pa:prebar st:bar pa:prefoosuf st:foo"),
			createProduction("prebarsuffoo", "SY", "pa:prebarsuf st:bar pa:foo st:foo"),
			createProduction("prebarsuffoosuf", "Y", "pa:prebarsuf st:bar pa:foosuf st:foo"),
			createProduction("prebarsufprefoo", "SY", "pa:prebarsuf st:bar pa:prefoo st:foo"),
			createProduction("prebarsufprefoosuf", "Y", "pa:prebarsuf st:bar pa:prefoosuf st:foo"),
			createProduction("barbar", "PS", "pa:bar st:bar pa:bar st:bar"),
			createProduction("barbarsuf", "PY", "pa:bar st:bar pa:barsuf st:bar"),
			createProduction("barprebar", "PSY", "pa:bar st:bar pa:prebar st:bar"),
			createProduction("barprebarsuf", "PY", "pa:bar st:bar pa:prebarsuf st:bar"),
			createProduction("barsufbar", "PSY", "pa:barsuf st:bar pa:bar st:bar"),
			createProduction("barsufbarsuf", "PY", "pa:barsuf st:bar pa:barsuf st:bar"),
			createProduction("barsufprebar", "PSY", "pa:barsuf st:bar pa:prebar st:bar"),
			createProduction("barsufprebarsuf", "PY", "pa:barsuf st:bar pa:prebarsuf st:bar"),
			createProduction("prebarbar", "SY", "pa:prebar st:bar pa:bar st:bar"),
			createProduction("prebarbarsuf", "Y", "pa:prebar st:bar pa:barsuf st:bar"),
			createProduction("prebarprebar", "SY", "pa:prebar st:bar pa:prebar st:bar"),
			createProduction("prebarprebarsuf", "Y", "pa:prebar st:bar pa:prebarsuf st:bar"),
			createProduction("prebarsufbar", "SY", "pa:prebarsuf st:bar pa:bar st:bar"),
			createProduction("prebarsufbarsuf", "Y", "pa:prebarsuf st:bar pa:barsuf st:bar"),
			createProduction("prebarsufprebar", "SY", "pa:prebarsuf st:bar pa:prebar st:bar"),
			createProduction("prebarsufprebarsuf", "Y", "pa:prebarsuf st:bar pa:prebarsuf st:bar")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	void forbidFlag() throws IOException, SAXException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
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
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(4, words.size());
		//base production
		Assertions.assertEquals(createProduction("foo", "XPS", "st:foo"), words.get(0));
		//onefold productions
		Assertions.assertEquals(createProduction("foosuf", "PZ", "st:foo"), words.get(1));
		//twofold productions
		Assertions.assertEquals(createProduction("prefoo", "SZ", "st:foo"), words.get(2));
		Assertions.assertEquals(createProduction("prefoosuf", "Z", "st:foo"), words.get(3));
		//lastfold productions


		String[] inputCompounds = new String[]{
			"foo/XPS",
			"bar/XPS"
		};
		words = wordGenerator.applyCompoundFlag(inputCompounds, 4, 2);

		List<Production> expected = Arrays.asList(
			createProduction("foofoo", "PS", "pa:foo st:foo pa:foo st:foo"),
			createProduction("foobar", "PS", "pa:foo st:foo pa:bar st:bar"),
			createProduction("barfoo", "PS", "pa:bar st:bar pa:foo st:foo"),
			createProduction("barbar", "PS", "pa:bar st:bar pa:bar st:bar")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	void checkCompoundCase() throws IOException, SAXException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
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

		List<Production> words = wordGenerator.applyCompoundFlag(inputCompounds, 100, 3);

		List<Production> expected = Arrays.asList(
			createProduction("foofoo", null, "pa:foo st:foo pa:foo st:foo"),
			createProduction("foobar", null, "pa:foo st:foo pa:Bar st:Bar"),
			createProduction("foobAZ", null, "pa:foo st:foo pa:BAZ st:BAZ"),
			createProduction("foo-", null, "pa:foo st:foo pa:- st:-"),
			createProduction("Barfoo", null, "pa:Bar st:Bar pa:foo st:foo"),
			createProduction("Barbar", null, "pa:Bar st:Bar pa:Bar st:Bar"),
			createProduction("BarbAZ", null, "pa:Bar st:Bar pa:BAZ st:BAZ"),
			createProduction("Bar-", null, "pa:Bar st:Bar pa:- st:-"),
			createProduction("BAZFoo", null, "pa:BAZ st:BAZ pa:foo st:foo"),
			createProduction("BAZBar", null, "pa:BAZ st:BAZ pa:Bar st:Bar"),
			createProduction("BAZBAZ", null, "pa:BAZ st:BAZ pa:BAZ st:BAZ"),
			createProduction("BAZ-", null, "pa:BAZ st:BAZ pa:- st:-"),
			createProduction("-foo", null, "pa:- st:- pa:foo st:foo"),
			createProduction("-Bar", null, "pa:- st:- pa:Bar st:Bar"),
			createProduction("-BAZ", null, "pa:- st:- pa:BAZ st:BAZ"),
			createProduction("--", null, "pa:- st:- pa:- st:-"),
			createProduction("foofoofoo", null, "pa:foo st:foo pa:foo st:foo pa:foo st:foo"),
			createProduction("foofoobar", null, "pa:foo st:foo pa:foo st:foo pa:Bar st:Bar"),
			createProduction("foofoobAZ", null, "pa:foo st:foo pa:foo st:foo pa:BAZ st:BAZ"),
			createProduction("foofoo-", null, "pa:foo st:foo pa:foo st:foo pa:- st:-"),
			createProduction("foobarfoo", null, "pa:foo st:foo pa:Bar st:Bar pa:foo st:foo"),
			createProduction("foobarbar", null, "pa:foo st:foo pa:Bar st:Bar pa:Bar st:Bar"),
			createProduction("foobarbAZ", null, "pa:foo st:foo pa:Bar st:Bar pa:BAZ st:BAZ"),
			createProduction("foobar-", null, "pa:foo st:foo pa:Bar st:Bar pa:- st:-"),
			createProduction("foobAZFoo", null, "pa:foo st:foo pa:BAZ st:BAZ pa:foo st:foo"),
			createProduction("foobAZBar", null, "pa:foo st:foo pa:BAZ st:BAZ pa:Bar st:Bar"),
			createProduction("foobAZBAZ", null, "pa:foo st:foo pa:BAZ st:BAZ pa:BAZ st:BAZ"),
			createProduction("foobAZ-", null, "pa:foo st:foo pa:BAZ st:BAZ pa:- st:-"),
			createProduction("foo-foo", null, "pa:foo st:foo pa:- st:- pa:foo st:foo"),
			createProduction("foo-Bar", null, "pa:foo st:foo pa:- st:- pa:Bar st:Bar"),
			createProduction("foo-BAZ", null, "pa:foo st:foo pa:- st:- pa:BAZ st:BAZ"),
			createProduction("foo--", null, "pa:foo st:foo pa:- st:- pa:- st:-"),
			createProduction("Barfoofoo", null, "pa:Bar st:Bar pa:foo st:foo pa:foo st:foo"),
			createProduction("Barfoobar", null, "pa:Bar st:Bar pa:foo st:foo pa:Bar st:Bar"),
			createProduction("BarfoobAZ", null, "pa:Bar st:Bar pa:foo st:foo pa:BAZ st:BAZ"),
			createProduction("Barfoo-", null, "pa:Bar st:Bar pa:foo st:foo pa:- st:-"),
			createProduction("Barbarfoo", null, "pa:Bar st:Bar pa:Bar st:Bar pa:foo st:foo"),
			createProduction("Barbarbar", null, "pa:Bar st:Bar pa:Bar st:Bar pa:Bar st:Bar"),
			createProduction("BarbarbAZ", null, "pa:Bar st:Bar pa:Bar st:Bar pa:BAZ st:BAZ"),
			createProduction("Barbar-", null, "pa:Bar st:Bar pa:Bar st:Bar pa:- st:-"),
			createProduction("BarbAZFoo", null, "pa:Bar st:Bar pa:BAZ st:BAZ pa:foo st:foo"),
			createProduction("BarbAZBar", null, "pa:Bar st:Bar pa:BAZ st:BAZ pa:Bar st:Bar"),
			createProduction("BarbAZBAZ", null, "pa:Bar st:Bar pa:BAZ st:BAZ pa:BAZ st:BAZ"),
			createProduction("BarbAZ-", null, "pa:Bar st:Bar pa:BAZ st:BAZ pa:- st:-"),
			createProduction("Bar-foo", null, "pa:Bar st:Bar pa:- st:- pa:foo st:foo"),
			createProduction("Bar-Bar", null, "pa:Bar st:Bar pa:- st:- pa:Bar st:Bar"),
			createProduction("Bar-BAZ", null, "pa:Bar st:Bar pa:- st:- pa:BAZ st:BAZ"),
			createProduction("Bar--", null, "pa:Bar st:Bar pa:- st:- pa:- st:-"),
			createProduction("BAZFoofoo", null, "pa:BAZ st:BAZ pa:foo st:foo pa:foo st:foo"),
			createProduction("BAZFoobar", null, "pa:BAZ st:BAZ pa:foo st:foo pa:Bar st:Bar"),
			createProduction("BAZFoobAZ", null, "pa:BAZ st:BAZ pa:foo st:foo pa:BAZ st:BAZ"),
			createProduction("BAZFoo-", null, "pa:BAZ st:BAZ pa:foo st:foo pa:- st:-"),
			createProduction("BAZBarfoo", null, "pa:BAZ st:BAZ pa:Bar st:Bar pa:foo st:foo"),
			createProduction("BAZBarbar", null, "pa:BAZ st:BAZ pa:Bar st:Bar pa:Bar st:Bar"),
			createProduction("BAZBarbAZ", null, "pa:BAZ st:BAZ pa:Bar st:Bar pa:BAZ st:BAZ"),
			createProduction("BAZBar-", null, "pa:BAZ st:BAZ pa:Bar st:Bar pa:- st:-"),
			createProduction("BAZBAZFoo", null, "pa:BAZ st:BAZ pa:BAZ st:BAZ pa:foo st:foo"),
			createProduction("BAZBAZBar", null, "pa:BAZ st:BAZ pa:BAZ st:BAZ pa:Bar st:Bar"),
			createProduction("BAZBAZBAZ", null, "pa:BAZ st:BAZ pa:BAZ st:BAZ pa:BAZ st:BAZ"),
			createProduction("BAZBAZ-", null, "pa:BAZ st:BAZ pa:BAZ st:BAZ pa:- st:-"),
			createProduction("BAZ-foo", null, "pa:BAZ st:BAZ pa:- st:- pa:foo st:foo"),
			createProduction("BAZ-Bar", null, "pa:BAZ st:BAZ pa:- st:- pa:Bar st:Bar"),
			createProduction("BAZ-BAZ", null, "pa:BAZ st:BAZ pa:- st:- pa:BAZ st:BAZ"),
			createProduction("BAZ--", null, "pa:BAZ st:BAZ pa:- st:- pa:- st:-"),
			createProduction("-foofoo", null, "pa:- st:- pa:foo st:foo pa:foo st:foo"),
			createProduction("-foobar", null, "pa:- st:- pa:foo st:foo pa:Bar st:Bar"),
			createProduction("-foobAZ", null, "pa:- st:- pa:foo st:foo pa:BAZ st:BAZ"),
			createProduction("-foo-", null, "pa:- st:- pa:foo st:foo pa:- st:-"),
			createProduction("-Barfoo", null, "pa:- st:- pa:Bar st:Bar pa:foo st:foo"),
			createProduction("-Barbar", null, "pa:- st:- pa:Bar st:Bar pa:Bar st:Bar"),
			createProduction("-BarbAZ", null, "pa:- st:- pa:Bar st:Bar pa:BAZ st:BAZ"),
			createProduction("-Bar-", null, "pa:- st:- pa:Bar st:Bar pa:- st:-"),
			createProduction("-BAZFoo", null, "pa:- st:- pa:BAZ st:BAZ pa:foo st:foo"),
			createProduction("-BAZBar", null, "pa:- st:- pa:BAZ st:BAZ pa:Bar st:Bar"),
			createProduction("-BAZBAZ", null, "pa:- st:- pa:BAZ st:BAZ pa:BAZ st:BAZ"),
			createProduction("-BAZ-", null, "pa:- st:- pa:BAZ st:BAZ pa:- st:-"),
			createProduction("--foo", null, "pa:- st:- pa:- st:- pa:foo st:foo"),
			createProduction("--Bar", null, "pa:- st:- pa:- st:- pa:Bar st:Bar"),
			createProduction("--BAZ", null, "pa:- st:- pa:- st:- pa:BAZ st:BAZ"),
			createProduction("---", null, "pa:- st:- pa:- st:- pa:- st:-")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	void compoundReplacement() throws IOException, SAXException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"CHECKCOMPOUNDREP",
			"COMPOUNDFLAG A",
			"REP 1",
			"REP í i");
		File dicFile = FileHelper.getTemporaryUTF8File(language, ".dic",
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
		List<Production> words = wordGenerator.applyCompoundFlag(inputCompounds, 40, 3);

		List<Production> expected = Arrays.asList(
			createProduction("szerszer", null, "pa:szer st:szer pa:szer st:szer"),
			createProduction("szerkocsi", null, "pa:szer st:szer pa:kocsi st:kocsi"),
			createProduction("szerszerviz", null, "pa:szer st:szer pa:szerviz st:szerviz"),
			createProduction("vízszer", null, "pa:víz st:víz pa:szer st:szer"),
			createProduction("vízvíz", null, "pa:víz st:víz pa:víz st:víz"),
			createProduction("vízkocsi", null, "pa:víz st:víz pa:kocsi st:kocsi"),
			createProduction("vízszerviz", null, "pa:víz st:víz pa:szerviz st:szerviz"),
			createProduction("kocsiszer", null, "pa:kocsi st:kocsi pa:szer st:szer"),
			createProduction("kocsivíz", null, "pa:kocsi st:kocsi pa:víz st:víz"),
			createProduction("kocsikocsi", null, "pa:kocsi st:kocsi pa:kocsi st:kocsi"),
			createProduction("kocsiszerviz", null, "pa:kocsi st:kocsi pa:szerviz st:szerviz"),
			createProduction("szervizszer", null, "pa:szerviz st:szerviz pa:szer st:szer"),
			createProduction("szervizvíz", null, "pa:szerviz st:szerviz pa:víz st:víz"),
			createProduction("szervizkocsi", null, "pa:szerviz st:szerviz pa:kocsi st:kocsi"),
			createProduction("szervizszerviz", null, "pa:szerviz st:szerviz pa:szerviz st:szerviz"),
			createProduction("szerszerszer", null, "pa:szer st:szer pa:szer st:szer pa:szer st:szer"),
			createProduction("szerszerkocsi", null, "pa:szer st:szer pa:szer st:szer pa:kocsi st:kocsi"),
			createProduction("szerszerszerviz", null, "pa:szer st:szer pa:szer st:szer pa:szerviz st:szerviz"),
			createProduction("szerkocsiszer", null, "pa:szer st:szer pa:kocsi st:kocsi pa:szer st:szer"),
			createProduction("szerkocsivíz", null, "pa:szer st:szer pa:kocsi st:kocsi pa:víz st:víz"),
			createProduction("szerkocsikocsi", null, "pa:szer st:szer pa:kocsi st:kocsi pa:kocsi st:kocsi"),
			createProduction("szerkocsiszerviz", null, "pa:szer st:szer pa:kocsi st:kocsi pa:szerviz st:szerviz"),
			createProduction("szerszervizszer", null, "pa:szer st:szer pa:szerviz st:szerviz pa:szer st:szer"),
			createProduction("szerszervizvíz", null, "pa:szer st:szer pa:szerviz st:szerviz pa:víz st:víz"),
			createProduction("szerszervizkocsi", null, "pa:szer st:szer pa:szerviz st:szerviz pa:kocsi st:kocsi"),
			createProduction("szerszervizszerviz", null, "pa:szer st:szer pa:szerviz st:szerviz pa:szerviz st:szerviz"),
			createProduction("vízszerszer", null, "pa:víz st:víz pa:szer st:szer pa:szer st:szer"),
			createProduction("vízszerkocsi", null, "pa:víz st:víz pa:szer st:szer pa:kocsi st:kocsi"),
			createProduction("vízszerszerviz", null, "pa:víz st:víz pa:szer st:szer pa:szerviz st:szerviz"),
			createProduction("vízvízszer", null, "pa:víz st:víz pa:víz st:víz pa:szer st:szer"),
			createProduction("vízvízvíz", null, "pa:víz st:víz pa:víz st:víz pa:víz st:víz"),
			createProduction("vízvízkocsi", null, "pa:víz st:víz pa:víz st:víz pa:kocsi st:kocsi"),
			createProduction("vízvízszerviz", null, "pa:víz st:víz pa:víz st:víz pa:szerviz st:szerviz")
		);
		Assertions.assertEquals(expected, words);
	}


	@Test
	void forbiddenWord() throws IOException, SAXException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
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
		List<Production> words = wordGenerator.applyCompoundFlag(inputCompounds, 100, 2);

		List<Production> expected = Arrays.asList(
			createProduction("foofoo", "S", "pa:foo st:foo po:1 pa:foo st:foo po:1"),
			createProduction("foofoos", null, "pa:foo st:foo po:1 pa:foo st:foo po:1"),
			createProduction("foofoo", null, "pa:foo st:foo po:1 pa:foo st:foo po:3"),
			createProduction("foobar", "S", "pa:foo st:foo po:1 pa:bar st:bar po:4"),
			createProduction("foobars", null, "pa:foo st:foo po:1 pa:bar st:bar po:4"),
			createProduction("foofoo", "S", "pa:foo st:foo po:3 pa:foo st:foo po:1"),
			createProduction("foofoos", null, "pa:foo st:foo po:3 pa:foo st:foo po:1"),
			createProduction("foofoo", null, "pa:foo st:foo po:3 pa:foo st:foo po:3"),
			createProduction("foobar", "S", "pa:foo st:foo po:3 pa:bar st:bar po:4"),
			createProduction("foobars", null, "pa:foo st:foo po:3 pa:bar st:bar po:4"),
			createProduction("barfoo", "S", "pa:bar st:bar po:4 pa:foo st:foo po:1"),
			createProduction("barfoos", null, "pa:bar st:bar po:4 pa:foo st:foo po:1"),
			createProduction("barfoo", null, "pa:bar st:bar po:4 pa:foo st:foo po:3"),
			createProduction("barbar", "S", "pa:bar st:bar po:4 pa:bar st:bar po:4"),
			createProduction("barbars", null, "pa:bar st:bar po:4 pa:bar st:bar po:4")
		);
		Assertions.assertEquals(expected, words);
	}


	@Test
	void forceUppercase() throws IOException, SAXException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"FORCEUCASE A",
			"COMPOUNDFLAG C");
		loadData(affFile, language);

		String[] inputCompounds = new String[]{
			"foo/C",
			"bar/C",
			"baz/CA"
		};
		List<Production> words = wordGenerator.applyCompoundFlag(inputCompounds, 100, 3);
//words.forEach(System.out::println);

		List<Production> expected = Arrays.asList(
			createProduction("foofoo", null, "pa:foo st:foo pa:foo st:foo"),
			createProduction("foobar", null, "pa:foo st:foo pa:bar st:bar"),
			createProduction("Foobaz", null, "pa:foo st:foo pa:baz st:baz"),
			createProduction("barfoo", null, "pa:bar st:bar pa:foo st:foo"),
			createProduction("barbar", null, "pa:bar st:bar pa:bar st:bar"),
			createProduction("Barbaz", null, "pa:bar st:bar pa:baz st:baz"),
			createProduction("bazfoo", null, "pa:baz st:baz pa:foo st:foo"),
			createProduction("bazbar", null, "pa:baz st:baz pa:bar st:bar"),
			createProduction("Bazbaz", null, "pa:baz st:baz pa:baz st:baz"),
			createProduction("foofoofoo", null, "pa:foo st:foo pa:foo st:foo pa:foo st:foo"),
			createProduction("foofoobar", null, "pa:foo st:foo pa:foo st:foo pa:bar st:bar"),
			createProduction("Foofoobaz", null, "pa:foo st:foo pa:foo st:foo pa:baz st:baz"),
			createProduction("foobarfoo", null, "pa:foo st:foo pa:bar st:bar pa:foo st:foo"),
			createProduction("foobarbar", null, "pa:foo st:foo pa:bar st:bar pa:bar st:bar"),
			createProduction("Foobarbaz", null, "pa:foo st:foo pa:bar st:bar pa:baz st:baz"),
			createProduction("foobazfoo", null, "pa:foo st:foo pa:baz st:baz pa:foo st:foo"),
			createProduction("foobazbar", null, "pa:foo st:foo pa:baz st:baz pa:bar st:bar"),
			createProduction("Foobazbaz", null, "pa:foo st:foo pa:baz st:baz pa:baz st:baz"),
			createProduction("barfoofoo", null, "pa:bar st:bar pa:foo st:foo pa:foo st:foo"),
			createProduction("barfoobar", null, "pa:bar st:bar pa:foo st:foo pa:bar st:bar"),
			createProduction("Barfoobaz", null, "pa:bar st:bar pa:foo st:foo pa:baz st:baz"),
			createProduction("barbarfoo", null, "pa:bar st:bar pa:bar st:bar pa:foo st:foo"),
			createProduction("barbarbar", null, "pa:bar st:bar pa:bar st:bar pa:bar st:bar"),
			createProduction("Barbarbaz", null, "pa:bar st:bar pa:bar st:bar pa:baz st:baz"),
			createProduction("barbazfoo", null, "pa:bar st:bar pa:baz st:baz pa:foo st:foo"),
			createProduction("barbazbar", null, "pa:bar st:bar pa:baz st:baz pa:bar st:bar"),
			createProduction("Barbazbaz", null, "pa:bar st:bar pa:baz st:baz pa:baz st:baz"),
			createProduction("bazfoofoo", null, "pa:baz st:baz pa:foo st:foo pa:foo st:foo"),
			createProduction("bazfoobar", null, "pa:baz st:baz pa:foo st:foo pa:bar st:bar"),
			createProduction("Bazfoobaz", null, "pa:baz st:baz pa:foo st:foo pa:baz st:baz"),
			createProduction("bazbarfoo", null, "pa:baz st:baz pa:bar st:bar pa:foo st:foo"),
			createProduction("bazbarbar", null, "pa:baz st:baz pa:bar st:bar pa:bar st:bar"),
			createProduction("Bazbarbaz", null, "pa:baz st:baz pa:bar st:bar pa:baz st:baz"),
			createProduction("bazbazfoo", null, "pa:baz st:baz pa:baz st:baz pa:foo st:foo"),
			createProduction("bazbazbar", null, "pa:baz st:baz pa:baz st:baz pa:bar st:bar"),
			createProduction("Bazbazbaz", null, "pa:baz st:baz pa:baz st:baz pa:baz st:baz")
		);
		Assertions.assertEquals(expected, words);
	}

//	@Test
//	void onlyInCompound() throws IOException, SAXException{
//		String language = "en-GB";
//		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
//			"SET UTF-8",
//			"ONLYINCOMPOUND O",
//			"COMPOUNDFLAG A",
//			"SFX B Y 1",
//			"SFX B 0 s .");
//		loadData(affFile, language);
//
//		String[] inputCompounds = new String[]{
//			"foo/A",
//			"pseudo/OAB"
//		};
//		List<Production> words = wordGenerator.applyCompoundFlag(inputCompounds, 100, 3);
//
//		List<Production> expected = Arrays.asList(
//			createProduction("foo", null, "pa:0 pa:{"),
//			createProduction("pseudofoo", null, "pa:0 pa:{"),
//			createProduction("foopseudo", null, "pa:0 pa:{"),
//			createProduction("foopseudos", null, "pa:0 pa:{")
//		);
//		Assertions.assertEquals(expected, words);
//	}

//	@Test
//	void onlyInCompound2() throws IOException, SAXException{
//		String language = "en-GB";
//		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
//			"SET UTF-8",
//			"ONLYINCOMPOUND O",
//			"COMPOUNDFLAG A",
//			"COMPOUNDPERMITFLAG P",
//			"SFX B Y 1",
//			"SFX B 0 s/OP .",
//			"# obligate fogemorpheme by forbidding the stem (0) in compounds",
//			"CHECKCOMPOUNDPATTERN 1",
//			"CHECKCOMPOUNDPATTERN 0/B /A");
//		loadData(affFile, language);
//
//		String[] inputCompounds = new String[]{
//			"foo/A",
//			"pseudo/AB"
//		};
//		List<Production> words = wordGenerator.applyCompoundFlag(inputCompounds, 100, 3);
//
//		List<Production> expected = Arrays.asList(
//			createProduction("foo", null, "pa:0 pa:{"),
//			createProduction("foopseudo", null, "pa:0 pa:{"),
//			createProduction("pseudosfoo", null, "pa:0 pa:{")
//		);
//		Assertions.assertEquals(expected, words);
//	}

//FIXME
	@Test
	void germanCompounding() throws IOException, SAXException{
		String language = "ger";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			//compound flags
			"COMPOUNDBEGIN U",
			"COMPOUNDMIDDLE V",
			"COMPOUNDEND W",
			//Prefixes are allowed at the beginning of compounds, suffixes are allowed at the end of compounds by default:
			//(prefix)?(root)+(affix)?
			//Affixes with COMPOUNDPERMITFLAG may be inside of compounds
			"COMPOUNDPERMITFLAG P",
			//for fogemorphemes (Fuge-element)
			//Hint: ONLYINCOMPOUND is not required everywhere, but the checking will be a little faster with it
			"ONLYINCOMPOUND X",
			//forbid uppercase characters at compound word bounds
			"CHECKCOMPOUNDCASE",
			//for handling Fuge-elements with dashes (Arbeits-) dash will be a special word
			"COMPOUNDMIN 1",
			"WORDCHARS -",
			//for forbid exceptions (*Arbeitsnehmer)
			"FORBIDDENWORD Z",
			//compound settings and fogemorpheme for `Arbeit'
			"SFX A Y 3",
			"SFX A 0 s/UPX .",
			"SFX A 0 s/VPDX .",
			"SFX A 0 0/WXD .",
			"SFX B Y 2",
			"SFX B 0 0/UPX .",
			"SFX B 0 0/VWXDP .",
			//a suffix for `Computer`
			"SFX C Y 1",
			"SFX C 0 n/WD .",
			//dash prefix for compounds with dash (Arbeits-Computer)
			"PFX - Y 1",
			"PFX - 0 -/P .",
			//decapitalizing prefix
			"PFX D Y 2",
			"PFX D A a/P A",
			"PFX D C c/P C");
		loadData(affFile, language);


		String line = "Arbeit/A-";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(7, words.size());
		//base production
		Assertions.assertEquals(createProduction("Arbeit", "A-", "st:Arbeit"), words.get(0));
		//suffix productions
		//prefix productions
		//twofold productions
		Assertions.assertEquals(createProduction("-Arbeit", "PA", "st:Arbeit"), words.get(1));
		Assertions.assertEquals(createProduction("-Arbeits", "P", "st:Arbeit"), words.get(2));
		Assertions.assertEquals(createProduction("-Arbeits", "P", "st:Arbeit"), words.get(3));
		Assertions.assertEquals(createProduction("-Arbeit", "P", "st:Arbeit"), words.get(4));
		Assertions.assertEquals(createProduction("arbeits", "P", "st:Arbeit"), words.get(5));
		Assertions.assertEquals(createProduction("arbeit", "P", "st:Arbeit"), words.get(6));


		line = "Computer/BC-";
		dicEntry = wordGenerator.createFromDictionaryLine(line);
		words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(8, words.size());
		//base production
		Assertions.assertEquals(createProduction("Computer", "BC-", "st:Computer"), words.get(0));
		//suffix productions
		Assertions.assertEquals(createProduction("Computern", "DW-", "st:Computer"), words.get(1));
		//prefix productions
		//twofold productions
		Assertions.assertEquals(createProduction("-Computer", "PBC", "st:Computer"), words.get(2));
		Assertions.assertEquals(createProduction("-Computer", "P", "st:Computer"), words.get(3));
		Assertions.assertEquals(createProduction("-Computer", "P", "st:Computer"), words.get(4));
		Assertions.assertEquals(createProduction("-Computern", "P", "st:Computer"), words.get(5));
		Assertions.assertEquals(createProduction("computer", "P", "st:Computer"), words.get(6));
		Assertions.assertEquals(createProduction("computern", "P", "st:Computer"), words.get(7));


		line = "-/W";
		dicEntry = wordGenerator.createFromDictionaryLine(line);
		words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(1, words.size());
		//base production
		Assertions.assertEquals(createProduction("-", "W", "st:-"), words.get(0));
		//suffix productions
		//prefix productions
		//twofold productions


		String[] inputCompounds = new String[]{
			"Arbeit/A-",
			"Computer/BC-",
			"-/W",
			"Arbeitsnehmer/Z"
		};
		words = wordGenerator.applyCompoundFlag(inputCompounds, 154, PermutationsWithRepetitions.MAX_COMPOUNDS_INFINITY);

		List<Production> expected = Arrays.asList(
			createProduction("Arbeitarbeit", "-A", "pa:Arbeit st:Arbeit pa:Arbeit st:Arbeit"),
			createProduction("Arbeitarbeits", "-PUX", "pa:Arbeit st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("Arbeitarbeits", "-PVX", "pa:Arbeit st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("Arbeit-Arbeit", "-AP", "pa:Arbeit st:Arbeit pa:-Arbeit st:Arbeit"),
			createProduction("Arbeit-Arbeits", "-P", "pa:Arbeit st:Arbeit pa:-Arbeits st:Arbeit"),
			createProduction("Arbeitarbeits", "-P", "pa:Arbeit st:Arbeit pa:arbeits st:Arbeit"),
			createProduction("Arbeitsarbeit", "-APUX", "pa:Arbeits st:Arbeit pa:Arbeit st:Arbeit"),
			createProduction("Arbeitsarbeits", "-PUX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("Arbeitsarbeits", "-PUVX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("Arbeits-Arbeit", "-APUX", "pa:Arbeits st:Arbeit pa:-Arbeit st:Arbeit"),
			createProduction("Arbeits-Arbeits", "-PUX", "pa:Arbeits st:Arbeit pa:-Arbeits st:Arbeit"),
			createProduction("Arbeitsarbeits", "-PUX", "pa:Arbeits st:Arbeit pa:arbeits st:Arbeit"),
			createProduction("Arbeitsarbeit", "D-APVX", "pa:Arbeits st:Arbeit pa:Arbeit st:Arbeit"),
			createProduction("Arbeitsarbeits", "D-PUVX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("Arbeitsarbeits", "D-PVX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("Arbeits-Arbeit", "D-APVX", "pa:Arbeits st:Arbeit pa:-Arbeit st:Arbeit"),
			createProduction("Arbeits-Arbeits", "D-PVX", "pa:Arbeits st:Arbeit pa:-Arbeits st:Arbeit"),
			createProduction("Arbeitsarbeits", "D-PVX", "pa:Arbeits st:Arbeit pa:arbeits st:Arbeit"),
			createProduction("-ArbeitArbeit", "AP", "pa:-Arbeit st:Arbeit pa:Arbeit st:Arbeit"),
			createProduction("-ArbeitArbeits", "PUX", "pa:-Arbeit st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("-ArbeitArbeits", "PVX", "pa:-Arbeit st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("-Arbeit-Arbeit", "AP", "pa:-Arbeit st:Arbeit pa:-Arbeit st:Arbeit"),
			createProduction("-Arbeit-Arbeits", "P", "pa:-Arbeit st:Arbeit pa:-Arbeits st:Arbeit"),
			createProduction("-Arbeitarbeits", "P", "pa:-Arbeit st:Arbeit pa:arbeits st:Arbeit"),
			createProduction("-ArbeitsArbeit", "AP", "pa:-Arbeits st:Arbeit pa:Arbeit st:Arbeit"),
			createProduction("-ArbeitsArbeits", "PUX", "pa:-Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("-ArbeitsArbeits", "PVX", "pa:-Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("-Arbeits-Arbeit", "AP", "pa:-Arbeits st:Arbeit pa:-Arbeit st:Arbeit"),
			createProduction("-Arbeits-Arbeits", "P", "pa:-Arbeits st:Arbeit pa:-Arbeits st:Arbeit"),
			createProduction("-Arbeitsarbeits", "P", "pa:-Arbeits st:Arbeit pa:arbeits st:Arbeit"),
			createProduction("arbeitsarbeit", "AP", "pa:arbeits st:Arbeit pa:Arbeit st:Arbeit"),
			createProduction("arbeitsarbeits", "PUX", "pa:arbeits st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("arbeitsarbeits", "PVX", "pa:arbeits st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("arbeits-Arbeit", "AP", "pa:arbeits st:Arbeit pa:-Arbeit st:Arbeit"),
			createProduction("arbeits-Arbeits", "P", "pa:arbeits st:Arbeit pa:-Arbeits st:Arbeit"),
			createProduction("arbeitsarbeits", "P", "pa:arbeits st:Arbeit pa:arbeits st:Arbeit"),
			createProduction("Arbeitcomputer", "-BC", "pa:Arbeit st:Arbeit pa:Computer st:Computer"),
			createProduction("Arbeitcomputer", "-PUX", "pa:Arbeit st:Arbeit pa:Computer st:Computer"),
			createProduction("Arbeitcomputer", "-PVWX", "pa:Arbeit st:Arbeit pa:Computer st:Computer"),
			createProduction("Arbeit-Computer", "-BCP", "pa:Arbeit st:Arbeit pa:-Computer st:Computer"),
			createProduction("Arbeit-Computer", "-P", "pa:Arbeit st:Arbeit pa:-Computer st:Computer"),
			createProduction("Arbeitcomputer", "-P", "pa:Arbeit st:Arbeit pa:computer st:Computer"),
			createProduction("Arbeitscomputer", "-BCPUX", "pa:Arbeits st:Arbeit pa:Computer st:Computer"),
			createProduction("Arbeitscomputer", "-PUX", "pa:Arbeits st:Arbeit pa:Computer st:Computer"),
			createProduction("Arbeitscomputer", "-PUVWX", "pa:Arbeits st:Arbeit pa:Computer st:Computer"),
			createProduction("Arbeits-Computer", "-BCPUX", "pa:Arbeits st:Arbeit pa:-Computer st:Computer"),
			createProduction("Arbeits-Computer", "-PUX", "pa:Arbeits st:Arbeit pa:-Computer st:Computer"),
			createProduction("Arbeitscomputer", "-PUX", "pa:Arbeits st:Arbeit pa:computer st:Computer"),
			createProduction("Arbeitscomputer", "D-BCPVX", "pa:Arbeits st:Arbeit pa:Computer st:Computer"),
			createProduction("Arbeitscomputer", "D-PUVX", "pa:Arbeits st:Arbeit pa:Computer st:Computer"),
			createProduction("Arbeitscomputer", "D-PVWX", "pa:Arbeits st:Arbeit pa:Computer st:Computer"),
			createProduction("Arbeits-Computer", "D-BCPVX", "pa:Arbeits st:Arbeit pa:-Computer st:Computer"),
			createProduction("Arbeits-Computer", "D-PVX", "pa:Arbeits st:Arbeit pa:-Computer st:Computer"),
			createProduction("Arbeitscomputer", "D-PVX", "pa:Arbeits st:Arbeit pa:computer st:Computer"),
			createProduction("-ArbeitComputer", "BCP", "pa:-Arbeit st:Arbeit pa:Computer st:Computer"),
			createProduction("-ArbeitComputer", "PUX", "pa:-Arbeit st:Arbeit pa:Computer st:Computer"),
			createProduction("-ArbeitComputer", "PVWX", "pa:-Arbeit st:Arbeit pa:Computer st:Computer"),
			createProduction("-Arbeit-Computer", "BCP", "pa:-Arbeit st:Arbeit pa:-Computer st:Computer"),
			createProduction("-Arbeit-Computer", "P", "pa:-Arbeit st:Arbeit pa:-Computer st:Computer"),
			createProduction("-Arbeitcomputer", "P", "pa:-Arbeit st:Arbeit pa:computer st:Computer"),
			createProduction("-ArbeitsComputer", "BCP", "pa:-Arbeits st:Arbeit pa:Computer st:Computer"),
			createProduction("-ArbeitsComputer", "PUX", "pa:-Arbeits st:Arbeit pa:Computer st:Computer"),
			createProduction("-ArbeitsComputer", "PVWX", "pa:-Arbeits st:Arbeit pa:Computer st:Computer"),
			createProduction("-Arbeits-Computer", "BCP", "pa:-Arbeits st:Arbeit pa:-Computer st:Computer"),
			createProduction("-Arbeits-Computer", "P", "pa:-Arbeits st:Arbeit pa:-Computer st:Computer"),
			createProduction("-Arbeitscomputer", "P", "pa:-Arbeits st:Arbeit pa:computer st:Computer"),
			createProduction("arbeitscomputer", "BCP", "pa:arbeits st:Arbeit pa:Computer st:Computer"),
			createProduction("arbeitscomputer", "PUX", "pa:arbeits st:Arbeit pa:Computer st:Computer"),
			createProduction("arbeitscomputer", "PVWX", "pa:arbeits st:Arbeit pa:Computer st:Computer"),
			createProduction("arbeits-Computer", "BCP", "pa:arbeits st:Arbeit pa:-Computer st:Computer"),
			createProduction("arbeits-Computer", "P", "pa:arbeits st:Arbeit pa:-Computer st:Computer"),
			createProduction("arbeitscomputer", "P", "pa:arbeits st:Arbeit pa:computer st:Computer"),
			createProduction("Arbeit-", "-W", "pa:Arbeit st:Arbeit pa:- st:-"),
			createProduction("Arbeits-", "-PUWX", "pa:Arbeits st:Arbeit pa:- st:-"),
			createProduction("Arbeits-", "D-PVWX", "pa:Arbeits st:Arbeit pa:- st:-"),
			createProduction("-Arbeit-", "PW", "pa:-Arbeit st:Arbeit pa:- st:-"),
			createProduction("-Arbeits-", "PW", "pa:-Arbeits st:Arbeit pa:- st:-"),
			createProduction("arbeits-", "PW", "pa:arbeits st:Arbeit pa:- st:-"),
			createProduction("Computerarbeit", "-A", "pa:Computer st:Computer pa:Arbeit st:Arbeit"),
			createProduction("Computerarbeits", "-PUX", "pa:Computer st:Computer pa:Arbeits st:Arbeit"),
			createProduction("Computerarbeits", "-PVX", "pa:Computer st:Computer pa:Arbeits st:Arbeit"),
			createProduction("Computer-Arbeit", "-AP", "pa:Computer st:Computer pa:-Arbeit st:Arbeit"),
			createProduction("Computer-Arbeits", "-P", "pa:Computer st:Computer pa:-Arbeits st:Arbeit"),
			createProduction("Computerarbeits", "-P", "pa:Computer st:Computer pa:arbeits st:Arbeit"),
			createProduction("Computerarbeit", "-APUX", "pa:Computer st:Computer pa:Arbeit st:Arbeit"),
			createProduction("Computerarbeits", "-PUVX", "pa:Computer st:Computer pa:Arbeits st:Arbeit"),
			createProduction("Computer-Arbeit", "-APUX", "pa:Computer st:Computer pa:-Arbeit st:Arbeit"),
			createProduction("Computer-Arbeits", "-PUX", "pa:Computer st:Computer pa:-Arbeits st:Arbeit"),
			createProduction("Computerarbeits", "-PUX", "pa:Computer st:Computer pa:arbeits st:Arbeit"),
			createProduction("Computerarbeit", "D-APVWX", "pa:Computer st:Computer pa:Arbeit st:Arbeit"),
			createProduction("Computerarbeits", "D-PUVWX", "pa:Computer st:Computer pa:Arbeits st:Arbeit"),
			createProduction("Computerarbeits", "D-PVWX", "pa:Computer st:Computer pa:Arbeits st:Arbeit"),
			createProduction("Computer-Arbeit", "D-APVWX", "pa:Computer st:Computer pa:-Arbeit st:Arbeit"),
			createProduction("Computer-Arbeits", "D-PVWX", "pa:Computer st:Computer pa:-Arbeits st:Arbeit"),
			createProduction("Computerarbeits", "D-PVWX", "pa:Computer st:Computer pa:arbeits st:Arbeit"),
			createProduction("-ComputerArbeit", "AP", "pa:-Computer st:Computer pa:Arbeit st:Arbeit"),
			createProduction("-ComputerArbeits", "PUX", "pa:-Computer st:Computer pa:Arbeits st:Arbeit"),
			createProduction("-ComputerArbeits", "PVX", "pa:-Computer st:Computer pa:Arbeits st:Arbeit"),
			createProduction("-Computer-Arbeit", "AP", "pa:-Computer st:Computer pa:-Arbeit st:Arbeit"),
			createProduction("-Computer-Arbeits", "P", "pa:-Computer st:Computer pa:-Arbeits st:Arbeit"),
			createProduction("-Computerarbeits", "P", "pa:-Computer st:Computer pa:arbeits st:Arbeit"),
			createProduction("computerarbeit", "AP", "pa:computer st:Computer pa:Arbeit st:Arbeit"),
			createProduction("computerarbeits", "PUX", "pa:computer st:Computer pa:Arbeits st:Arbeit"),
			createProduction("computerarbeits", "PVX", "pa:computer st:Computer pa:Arbeits st:Arbeit"),
			createProduction("computer-Arbeit", "AP", "pa:computer st:Computer pa:-Arbeit st:Arbeit"),
			createProduction("computer-Arbeits", "P", "pa:computer st:Computer pa:-Arbeits st:Arbeit"),
			createProduction("computerarbeits", "P", "pa:computer st:Computer pa:arbeits st:Arbeit"),
			createProduction("Computercomputer", "-BC", "pa:Computer st:Computer pa:Computer st:Computer"),
			createProduction("Computercomputer", "-PUX", "pa:Computer st:Computer pa:Computer st:Computer"),
			createProduction("Computercomputer", "-PVWX", "pa:Computer st:Computer pa:Computer st:Computer"),
			createProduction("Computer-Computer", "-BCP", "pa:Computer st:Computer pa:-Computer st:Computer"),
			createProduction("Computer-Computer", "-P", "pa:Computer st:Computer pa:-Computer st:Computer"),
			createProduction("Computercomputer", "-P", "pa:Computer st:Computer pa:computer st:Computer"),
			createProduction("Computercomputer", "-BCPUX", "pa:Computer st:Computer pa:Computer st:Computer"),
			createProduction("Computercomputer", "-PUVWX", "pa:Computer st:Computer pa:Computer st:Computer"),
			createProduction("Computer-Computer", "-BCPUX", "pa:Computer st:Computer pa:-Computer st:Computer"),
			createProduction("Computer-Computer", "-PUX", "pa:Computer st:Computer pa:-Computer st:Computer"),
			createProduction("Computercomputer", "-PUX", "pa:Computer st:Computer pa:computer st:Computer"),
			createProduction("Computercomputer", "D-BCPVWX", "pa:Computer st:Computer pa:Computer st:Computer"),
			createProduction("Computercomputer", "D-PUVWX", "pa:Computer st:Computer pa:Computer st:Computer"),
			createProduction("Computercomputer", "D-PVWX", "pa:Computer st:Computer pa:Computer st:Computer"),
			createProduction("Computer-Computer", "D-BCPVWX", "pa:Computer st:Computer pa:-Computer st:Computer"),
			createProduction("Computer-Computer", "D-PVWX", "pa:Computer st:Computer pa:-Computer st:Computer"),
			createProduction("Computercomputer", "D-PVWX", "pa:Computer st:Computer pa:computer st:Computer"),
			createProduction("-ComputerComputer", "BCP", "pa:-Computer st:Computer pa:Computer st:Computer"),
			createProduction("-ComputerComputer", "PUX", "pa:-Computer st:Computer pa:Computer st:Computer"),
			createProduction("-ComputerComputer", "PVWX", "pa:-Computer st:Computer pa:Computer st:Computer"),
			createProduction("-Computer-Computer", "BCP", "pa:-Computer st:Computer pa:-Computer st:Computer"),
			createProduction("-Computer-Computer", "P", "pa:-Computer st:Computer pa:-Computer st:Computer"),
			createProduction("-Computercomputer", "P", "pa:-Computer st:Computer pa:computer st:Computer"),
			createProduction("computercomputer", "BCP", "pa:computer st:Computer pa:Computer st:Computer"),
			createProduction("computercomputer", "PUX", "pa:computer st:Computer pa:Computer st:Computer"),
			createProduction("computercomputer", "PVWX", "pa:computer st:Computer pa:Computer st:Computer"),
			createProduction("computer-Computer", "BCP", "pa:computer st:Computer pa:-Computer st:Computer"),
			createProduction("computer-Computer", "P", "pa:computer st:Computer pa:-Computer st:Computer"),
			createProduction("computercomputer", "P", "pa:computer st:Computer pa:computer st:Computer"),
			createProduction("Computer-", "-W", "pa:Computer st:Computer pa:- st:-"),
			createProduction("Computer-", "-PUWX", "pa:Computer st:Computer pa:- st:-"),
			createProduction("Computer-", "D-PVWX", "pa:Computer st:Computer pa:- st:-"),
			createProduction("-Computer-", "PW", "pa:-Computer st:Computer pa:- st:-"),
			createProduction("computer-", "PW", "pa:computer st:Computer pa:- st:-"),
			createProduction("-Arbeit", "AW", "pa:- st:- pa:Arbeit st:Arbeit"),
			createProduction("-Arbeits", "PUWX", "pa:- st:- pa:Arbeits st:Arbeit"),
			createProduction("-Arbeits", "PVWX", "pa:- st:- pa:Arbeits st:Arbeit"),
			createProduction("--Arbeit", "APW", "pa:- st:- pa:-Arbeit st:Arbeit"),
			createProduction("--Arbeits", "PW", "pa:- st:- pa:-Arbeits st:Arbeit"),
			createProduction("-arbeits", "PW", "pa:- st:- pa:arbeits st:Arbeit"),
			createProduction("-Computer", "BCW", "pa:- st:- pa:Computer st:Computer"),
			createProduction("-Computer", "PUWX", "pa:- st:- pa:Computer st:Computer"),
			createProduction("-Computer", "PVWX", "pa:- st:- pa:Computer st:Computer"),
			createProduction("--Computer", "BCPW", "pa:- st:- pa:-Computer st:Computer"),
			createProduction("--Computer", "PW", "pa:- st:- pa:-Computer st:Computer"),
			createProduction("-computer", "PW", "pa:- st:- pa:computer st:Computer"),
			createProduction("--", "W", "pa:- st:- pa:- st:-")
		);
		Assertions.assertEquals(expected, words);
	}


	private void loadData(File affFile, String language) throws IOException, SAXException{
		AffixParser affParser = new AffixParser();
		affParser.parse(affFile, language);
		affixData = affParser.getAffixData();
		wordGenerator = new WordGenerator(affixData, null);
	}

	private void loadData(File affFile, File dicFile, String language) throws IOException, SAXException{
		AffixParser affParser = new AffixParser();
		affParser.parse(affFile, language);
		affixData = affParser.getAffixData();
		Charset charset = affixData.getCharset();
		DictionaryParser dicParser = new DictionaryParser(dicFile, language, charset);
		wordGenerator = new WordGenerator(affixData, dicParser);
	}

	private Production createProduction(String word, String continuationFlags, String morphologicalFields){
		FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();
		return new Production(word, continuationFlags, morphologicalFields, null, strategy);
	}

}
