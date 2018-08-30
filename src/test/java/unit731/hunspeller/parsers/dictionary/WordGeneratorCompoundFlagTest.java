package unit731.hunspeller.parsers.dictionary;

import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.junit.Test;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.FileHelper;
import unit731.hunspeller.services.PermutationsWithRepetitions;


/** @see <a href="https://github.com/hunspell/hunspell/tree/master/tests/v1cmdline">Hunspell tests</a> */
public class WordGeneratorCompoundFlagTest{

	private final Backbone backbone = new Backbone(null, null);


	private void loadData(String affixFilePath) throws IOException{
		backbone.loadFile(affixFilePath);
	}

	private void loadData(String affixFilePath, String dictionaryFilePath) throws IOException{
		backbone.loadFile(affixFilePath, dictionaryFilePath);
	}

	private Production createProduction(String word, String continuationFlags, String morphologicalFields){
		FlagParsingStrategy strategy = backbone.getAffParser().getFlagParsingStrategy();
		return new Production(word, continuationFlags, morphologicalFields, strategy);
	}

	@Test
	public void simple() throws IOException, TimeoutException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPOUNDMIN 1",
			"COMPOUNDFLAG A");
		loadData(affFile.getAbsolutePath());

		String[] inputCompounds = new String[]{
			"foo/A",
			"bar/A",
			"xy/A",
			"yz/A"
		};
		List<Production> words = backbone.getWordGenerator().applyCompoundFlag(inputCompounds, 10, PermutationsWithRepetitions.MAX_COMPOUNDS_INFINITY);
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
		Assert.assertEquals(expected, words);
	}

	@Test
	public void compoundMinLength() throws IOException, TimeoutException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPOUNDMIN 3",
			"COMPOUNDFLAG A");
		loadData(affFile.getAbsolutePath());

		String[] inputCompounds = new String[]{
			"foo/A",
			"bar/A",
			"yz/A"
		};
		List<Production> words = backbone.getWordGenerator().applyCompoundFlag(inputCompounds, 100, 2);
		List<Production> expected = Arrays.asList(
			createProduction("foofoo", null, "pa:foo st:foo pa:foo st:foo"),
			createProduction("foobar", null, "pa:foo st:foo pa:bar st:bar"),
			createProduction("barfoo", null, "pa:bar st:bar pa:foo st:foo"),
			createProduction("barbar", null, "pa:bar st:bar pa:bar st:bar")
		);
		Assert.assertEquals(expected, words);
	}

	@Test
	public void checkCompoundTriple() throws IOException, TimeoutException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"CHECKCOMPOUNDTRIPLE",
			"COMPOUNDFLAG A");
		loadData(affFile.getAbsolutePath());

		String[] inputCompounds = new String[]{
			"foo/A",
			"opera/A",
			"eel/A",
			"bare/A"
		};
		List<Production> words = backbone.getWordGenerator().applyCompoundFlag(inputCompounds, 12, PermutationsWithRepetitions.MAX_COMPOUNDS_INFINITY);
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
		Assert.assertEquals(expected, words);
	}

	@Test
	public void simplifiedTriple() throws IOException, TimeoutException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"CHECKCOMPOUNDTRIPLE",
			"SIMPLIFIEDTRIPLE",
			"COMPOUNDMIN 2",
			"COMPOUNDFLAG A");
		loadData(affFile.getAbsolutePath());

		String[] inputCompounds = new String[]{
			"glass/A",
			"sko/A"
		};
		List<Production> words = backbone.getWordGenerator().applyCompoundFlag(inputCompounds, 3, PermutationsWithRepetitions.MAX_COMPOUNDS_INFINITY);
		List<Production> expected = Arrays.asList(
			createProduction("glassglass", null, "pa:glass st:glass pa:glass st:glass"),
			createProduction("glassko", null, "pa:glass st:glass pa:sko st:sko"),
			createProduction("skoglass", null, "pa:sko st:sko pa:glass st:glass")
		);
		Assert.assertEquals(expected, words);
	}

	@Test
	public void forbidWordDuplication() throws IOException, TimeoutException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"CHECKCOMPOUNDDUP",
			"COMPOUNDMIN 2",
			"COMPOUNDFLAG A");
		loadData(affFile.getAbsolutePath());

		String[] inputCompounds = new String[]{
			"foo/A",
			"bar/A",
			"yz/A"
		};
		List<Production> words = backbone.getWordGenerator().applyCompoundFlag(inputCompounds, 100, 2);
		List<Production> expected = Arrays.asList(
			createProduction("foobar", null, "pa:foo st:foo pa:bar st:bar"),
			createProduction("fooyz", null, "pa:foo st:foo pa:yz st:yz"),
			createProduction("barfoo", null, "pa:bar st:bar pa:foo st:foo"),
			createProduction("baryz", null, "pa:bar st:bar pa:yz st:yz"),
			createProduction("yzfoo", null, "pa:yz st:yz pa:foo st:foo"),
			createProduction("yzbar", null, "pa:yz st:yz pa:bar st:bar")
		);
		Assert.assertEquals(expected, words);
	}

	@Test
	public void withAffixes() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"COMPOUNDFLAG X",
			"PFX P Y 1",
			"PFX P 0 pre .	po:pre",
			"SFX S Y 1",
			"SFX S 0 suf .	po:suf");
		loadData(affFile.getAbsolutePath());


		String line = "foo/XPS";
		List<Production> words = backbone.getWordGenerator().applyRules(line);

		Assert.assertEquals(4, words.size());
		//base production
		Assert.assertEquals(createProduction("foo", "XPS", "st:foo"), words.get(0));
		//onefold productions
		Assert.assertEquals(createProduction("foosuf", "P", "st:foo po:suf"), words.get(1));
		//twofold productions
		Assert.assertEquals(createProduction("prefoo", "S", "po:pre st:foo"), words.get(2));
		Assert.assertEquals(createProduction("prefoosuf", null, "po:pre st:foo po:suf"), words.get(3));
		//lastfold productions


		String[] inputCompounds = new String[]{
			"foo/XPS",
			"bar/XPS"
		};
		words = backbone.getWordGenerator().applyCompoundFlag(inputCompounds, 4, 2);
		List<Production> expected = Arrays.asList(
			createProduction("foofoo", "PS", "pa:foo st:foo pa:foo st:foo"),
			createProduction("foofoosuf", "P", "pa:foo st:foo pa:foo st:foo po:suf"),
			createProduction("prefoofoo", "S", "po:pre pa:foo st:foo pa:foo st:foo"),
			createProduction("prefoofoosuf", null, "po:pre pa:foo st:foo pa:foo st:foo po:suf"),
			createProduction("foobar", "PS", "pa:foo st:foo pa:bar st:bar"),
			createProduction("foobarsuf", "P", "pa:foo st:foo pa:bar st:bar po:suf"),
			createProduction("prefoobar", "S", "po:pre pa:foo st:foo pa:bar st:bar"),
			createProduction("prefoobarsuf", null, "po:pre pa:foo st:foo pa:bar st:bar po:suf"),
			createProduction("barfoo", "PS", "pa:bar st:bar pa:foo st:foo"),
			createProduction("barfoosuf", "P", "pa:bar st:bar pa:foo st:foo po:suf"),
			createProduction("prebarfoo", "S", "po:pre pa:bar st:bar pa:foo st:foo"),
			createProduction("prebarfoosuf", null, "po:pre pa:bar st:bar pa:foo st:foo po:suf"),
			createProduction("barbar", "PS", "pa:bar st:bar pa:bar st:bar"),
			createProduction("barbarsuf", "P", "pa:bar st:bar pa:bar st:bar po:suf"),
			createProduction("prebarbar", "S", "po:pre pa:bar st:bar pa:bar st:bar"),
			createProduction("prebarbarsuf", null, "po:pre pa:bar st:bar pa:bar st:bar po:suf")
		);
		Assert.assertEquals(expected, words);
	}

	@Test
	public void withAffixesOnefold() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"COMPOUNDFLAG X",
			"PFX P Y 1",
			"PFX P 0 pre .",
			"SFX S Y 1",
			"SFX S 0 suf/T .",
			"SFX T Y 1",
			"SFX T 0 sff .");
		loadData(affFile.getAbsolutePath());


		String line = "foo/XPS";
		List<Production> words = backbone.getWordGenerator().applyRules(line);

		Assert.assertEquals(6, words.size());
		//base production
		Assert.assertEquals(createProduction("foo", "XPS", "st:foo"), words.get(0));
		//onefold productions
		Assert.assertEquals(createProduction("foosuf", "PT", "st:foo"), words.get(1));
		//twofold productions
		Assert.assertEquals(createProduction("foosufsff", "P", "st:foo"), words.get(2));
		//lastfold productions
		Assert.assertEquals(createProduction("prefoo", "S", "st:foo"), words.get(3));
		Assert.assertEquals(createProduction("prefoosuf", "T", "st:foo"), words.get(4));
		Assert.assertEquals(createProduction("prefoosufsff", null, "st:foo"), words.get(5));


		String[] inputCompounds = new String[]{
			"foo/XPS",
			"bar/XPS"
		};
		words = backbone.getWordGenerator().applyCompoundFlag(inputCompounds, 4, 2);
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
		Assert.assertEquals(expected, words);
	}

	@Test
	public void withAffixesTwofold() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"COMPOUNDFLAG X",
			"COMPOUNDMORESUFFIXES",
			"PFX P Y 1",
			"PFX P 0 pre .",
			"SFX S Y 1",
			"SFX S 0 suf/T .",
			"SFX T Y 1",
			"SFX T 0 sff .");
		loadData(affFile.getAbsolutePath());


		String line = "foo/XPS";
		List<Production> words = backbone.getWordGenerator().applyRules(line);

		Assert.assertEquals(6, words.size());
		//base production
		Assert.assertEquals(createProduction("foo", "XPS", "st:foo"), words.get(0));
		//onefold productions
		Assert.assertEquals(createProduction("foosuf", "PT", "st:foo"), words.get(1));
		//twofold productions
		Assert.assertEquals(createProduction("foosufsff", "P", "st:foo"), words.get(2));
		//lastfold productions
		Assert.assertEquals(createProduction("prefoo", "S", "st:foo"), words.get(3));
		Assert.assertEquals(createProduction("prefoosuf", "T", "st:foo"), words.get(4));
		Assert.assertEquals(createProduction("prefoosufsff", null, "st:foo"), words.get(5));


		String[] inputCompounds = new String[]{
			"foo/XPS",
			"bar/XPS"
		};
		words = backbone.getWordGenerator().applyCompoundFlag(inputCompounds, 4, 2);
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
		Assert.assertEquals(expected, words);
	}

	@Test
	public void permitFlag() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"COMPOUNDFLAG X",
			"COMPOUNDPERMITFLAG Y",
			"PFX P Y 1",
			"PFX P 0 pre/Y .",
			"SFX S Y 1",
			"SFX S 0 suf/Y .");
		loadData(affFile.getAbsolutePath());


		String line = "foo/XPS";
		List<Production> words = backbone.getWordGenerator().applyRules(line);

		Assert.assertEquals(4, words.size());
		//base production
		Assert.assertEquals(createProduction("foo", "XPS", "st:foo"), words.get(0));
		//onefold productions
		Assert.assertEquals(createProduction("foosuf", "PY", "st:foo"), words.get(1));
		//twofold productions
		Assert.assertEquals(createProduction("prefoo", "SY", "st:foo"), words.get(2));
		Assert.assertEquals(createProduction("prefoosuf", "Y", "st:foo"), words.get(3));
		//lastfold productions


		String[] inputCompounds = new String[]{
			"foo/XPS",
			"bar/XPS"
		};
		words = backbone.getWordGenerator().applyCompoundFlag(inputCompounds, 4, 2);
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
		Assert.assertEquals(expected, words);
	}

	@Test
	public void forbidFlag() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"COMPOUNDFLAG X",
			"COMPOUNDFORBIDFLAG Z",
			"PFX P Y 1",
			"PFX P 0 pre/Z .",
			"SFX S Y 1",
			"SFX S 0 suf/Z .");
		loadData(affFile.getAbsolutePath());


		String line = "foo/XPS";
		List<Production> words = backbone.getWordGenerator().applyRules(line);

		Assert.assertEquals(4, words.size());
		//base production
		Assert.assertEquals(createProduction("foo", "XPS", "st:foo"), words.get(0));
		//onefold productions
		Assert.assertEquals(createProduction("foosuf", "PZ", "st:foo"), words.get(1));
		//twofold productions
		Assert.assertEquals(createProduction("prefoo", "SZ", "st:foo"), words.get(2));
		Assert.assertEquals(createProduction("prefoosuf", "Z", "st:foo"), words.get(3));
		//lastfold productions


		String[] inputCompounds = new String[]{
			"foo/XPS",
			"bar/XPS"
		};
		words = backbone.getWordGenerator().applyCompoundFlag(inputCompounds, 4, 2);
		List<Production> expected = Arrays.asList(
			createProduction("foofoo", "PS", "pa:foo st:foo pa:foo st:foo"),
			createProduction("foobar", "PS", "pa:foo st:foo pa:bar st:bar"),
			createProduction("barfoo", "PS", "pa:bar st:bar pa:foo st:foo"),
			createProduction("barbar", "PS", "pa:bar st:bar pa:bar st:bar")
		);
		Assert.assertEquals(expected, words);
	}

	@Test
	public void checkCompoundCase() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"COMPOUNDMIN 1",
			"CHECKCOMPOUNDCASE",
			"COMPOUNDFLAG A");
		loadData(affFile.getAbsolutePath());


		String[] inputCompounds = new String[]{
			"foo/A",
			"Bar/A",
			"BAZ/A",
			"-/A"
		};

		List<Production> words = backbone.getWordGenerator().applyCompoundFlag(inputCompounds, 100, 3);
		List<Production> expected = Arrays.asList(
			createProduction("foofoo", null, "pa:foo st:foo pa:foo st:foo"),
			createProduction("foo-", null, "pa:foo st:foo pa:- st:-"),
			createProduction("Barfoo", null, "pa:Bar st:Bar pa:foo st:foo"),
			createProduction("Bar-", null, "pa:Bar st:Bar pa:- st:-"),
			createProduction("BAZBAZ", null, "pa:BAZ st:BAZ pa:BAZ st:BAZ"),
			createProduction("BAZ-", null, "pa:BAZ st:BAZ pa:- st:-"),
			createProduction("-foo", null, "pa:- st:- pa:foo st:foo"),
			createProduction("-Bar", null, "pa:- st:- pa:Bar st:Bar"),
			createProduction("-BAZ", null, "pa:- st:- pa:BAZ st:BAZ"),
			createProduction("--", null, "pa:- st:- pa:- st:-"),
			createProduction("foofoofoo", null, "pa:foo st:foo pa:foo st:foo pa:foo st:foo"),
			createProduction("foofoo-", null, "pa:foo st:foo pa:foo st:foo pa:- st:-"),
			createProduction("foo-foo", null, "pa:foo st:foo pa:- st:- pa:foo st:foo"),
			createProduction("foo-Bar", null, "pa:foo st:foo pa:- st:- pa:Bar st:Bar"),
			createProduction("foo-BAZ", null, "pa:foo st:foo pa:- st:- pa:BAZ st:BAZ"),
			createProduction("foo--", null, "pa:foo st:foo pa:- st:- pa:- st:-"),
			createProduction("Barfoofoo", null, "pa:Bar st:Bar pa:foo st:foo pa:foo st:foo"),
			createProduction("Barfoo-", null, "pa:Bar st:Bar pa:foo st:foo pa:- st:-"),
			createProduction("Bar-foo", null, "pa:Bar st:Bar pa:- st:- pa:foo st:foo"),
			createProduction("Bar-Bar", null, "pa:Bar st:Bar pa:- st:- pa:Bar st:Bar"),
			createProduction("Bar-BAZ", null, "pa:Bar st:Bar pa:- st:- pa:BAZ st:BAZ"),
			createProduction("Bar--", null, "pa:Bar st:Bar pa:- st:- pa:- st:-"),
			createProduction("BAZBAZBAZ", null, "pa:BAZ st:BAZ pa:BAZ st:BAZ pa:BAZ st:BAZ"),
			createProduction("BAZBAZ-", null, "pa:BAZ st:BAZ pa:BAZ st:BAZ pa:- st:-"),
			createProduction("BAZ-foo", null, "pa:BAZ st:BAZ pa:- st:- pa:foo st:foo"),
			createProduction("BAZ-Bar", null, "pa:BAZ st:BAZ pa:- st:- pa:Bar st:Bar"),
			createProduction("BAZ-BAZ", null, "pa:BAZ st:BAZ pa:- st:- pa:BAZ st:BAZ"),
			createProduction("BAZ--", null, "pa:BAZ st:BAZ pa:- st:- pa:- st:-"),
			createProduction("-foofoo", null, "pa:- st:- pa:foo st:foo pa:foo st:foo"),
			createProduction("-foo-", null, "pa:- st:- pa:foo st:foo pa:- st:-"),
			createProduction("-Barfoo", null, "pa:- st:- pa:Bar st:Bar pa:foo st:foo"),
			createProduction("-Bar-", null, "pa:- st:- pa:Bar st:Bar pa:- st:-"),
			createProduction("-BAZBAZ", null, "pa:- st:- pa:BAZ st:BAZ pa:BAZ st:BAZ"),
			createProduction("-BAZ-", null, "pa:- st:- pa:BAZ st:BAZ pa:- st:-"),
			createProduction("--foo", null, "pa:- st:- pa:- st:- pa:foo st:foo"),
			createProduction("--Bar", null, "pa:- st:- pa:- st:- pa:Bar st:Bar"),
			createProduction("--BAZ", null, "pa:- st:- pa:- st:- pa:BAZ st:BAZ"),
			createProduction("---", null, "pa:- st:- pa:- st:- pa:- st:-")
		);
		Assert.assertEquals(expected, words);
	}

	@Test
	public void compoundReplacement() throws IOException, TimeoutException{
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
		loadData(affFile.getAbsolutePath(), dicFile.getAbsolutePath());

		String[] inputCompounds = new String[]{
			"szer/A",
			"víz/A",
			"kocsi/A",
			"szerviz"
		};
		List<Production> words = backbone.getWordGenerator().applyCompoundFlag(inputCompounds, 40, 3);
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
		Assert.assertEquals(expected, words);
	}


	@Test
	public void forbiddenWord() throws IOException, TimeoutException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"FORBIDDENWORD X",
			"COMPOUNDFLAG Y",
			"SFX S Y 1",
			"SFX S 0 s .");
		loadData(affFile.getAbsolutePath());

		String[] inputCompounds = new String[]{
			"foo/S	po:1",
			"foo/YX	po:2",
			"foo/Y	po:3",
			"bar/YS	po:4",
			"bars/X",
			"foos/X"
		};
		List<Production> words = backbone.getWordGenerator().applyCompoundFlag(inputCompounds, 100, 2);
words.forEach(stem -> System.out.println(stem));
//bad: bars, foos, foobar, barfoo
		List<Production> expected = Arrays.asList(
			createProduction("foofoo", null, "pa:foo st:foo pa:foo st:foo"),
			createProduction("foofoos", null, "pa:foo st:foo pa:foo st:foo")
		);
		Assert.assertEquals(expected, words);
	}

}
