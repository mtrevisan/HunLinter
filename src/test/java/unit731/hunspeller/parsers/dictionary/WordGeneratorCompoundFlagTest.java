package unit731.hunspeller.parsers.dictionary;

import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.junit.Test;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.FileService;
import unit731.hunspeller.services.PermutationsWithRepetitions;


/** @see <a href="https://github.com/hunspell/hunspell/tree/master/tests/v1cmdline">Hunspell tests</a> */
public class WordGeneratorCompoundFlagTest{

	private final AffixParser affParser = new AffixParser();


	@Test
	public void simple() throws IOException, TimeoutException{
		String language = "xxx";
		File affFile = FileService.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPOUNDMIN 1",
			"COMPOUNDFLAG A");
		affParser.parse(affFile);
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser);

		String line = "A";
		String[] inputCompounds = new String[]{
			"foo/A",
			"bar/A",
			"xy/A",
			"yz/A"
		};
		List<Production> words = wordGenerator.applyCompoundFlag(inputCompounds, line, 10, PermutationsWithRepetitions.MAX_COMPOUNDS_INFINITY);
		List<Production> expected = Arrays.asList(
			new Production("foofoo", null, "pa:foo st:foo pa:foo st:foo", strategy),
			new Production("foobar", null, "pa:foo st:foo pa:bar st:bar", strategy),
			new Production("fooxy", null, "pa:foo st:foo pa:xy st:xy", strategy),
			new Production("fooyz", null, "pa:foo st:foo pa:yz st:yz", strategy),
			new Production("barfoo", null, "pa:bar st:bar pa:foo st:foo", strategy),
			new Production("barbar", null, "pa:bar st:bar pa:bar st:bar", strategy),
			new Production("barxy", null, "pa:bar st:bar pa:xy st:xy", strategy),
			new Production("baryz", null, "pa:bar st:bar pa:yz st:yz", strategy),
			new Production("xyfoo", null, "pa:xy st:xy pa:foo st:foo", strategy),
			new Production("xybar", null, "pa:xy st:xy pa:bar st:bar", strategy)
		);
		Assert.assertEquals(expected, words);
	}

	@Test
	public void compoundMinLength() throws IOException, TimeoutException{
		String language = "xxx";
		File affFile = FileService.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPOUNDMIN 3",
			"COMPOUNDFLAG A");
		affParser.parse(affFile);
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser);

		String line = "A";
		String[] inputCompounds = new String[]{
			"foo/A",
			"bar/A",
			"yz/A"
		};
		List<Production> words = wordGenerator.applyCompoundFlag(inputCompounds, line, 100, 2);
		List<Production> expected = Arrays.asList(
			new Production("foofoo", null, "pa:foo st:foo pa:foo st:foo", strategy),
			new Production("foobar", null, "pa:foo st:foo pa:bar st:bar", strategy),
			new Production("barfoo", null, "pa:bar st:bar pa:foo st:foo", strategy),
			new Production("barbar", null, "pa:bar st:bar pa:bar st:bar", strategy)
		);
		Assert.assertEquals(expected, words);
	}

	@Test
	public void checkCompoundTriple() throws IOException, TimeoutException{
		String language = "xxx";
		File affFile = FileService.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"CHECKCOMPOUNDTRIPLE",
			"COMPOUNDFLAG A");
		affParser.parse(affFile);
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser);

		String line = "A";
		String[] inputCompounds = new String[]{
			"foo/A",
			"opera/A",
			"eel/A",
			"bare/A"
		};
		List<Production> words = wordGenerator.applyCompoundFlag(inputCompounds, line, 12, PermutationsWithRepetitions.MAX_COMPOUNDS_INFINITY);
		List<Production> expected = Arrays.asList(
			new Production("foofoo", null, "pa:foo st:foo pa:foo st:foo", strategy),
			new Production("fooeel", null, "pa:foo st:foo pa:eel st:eel", strategy),
			new Production("foobare", null, "pa:foo st:foo pa:bare st:bare", strategy),
			new Production("operafoo", null, "pa:opera st:opera pa:foo st:foo", strategy),
			new Production("operaopera", null, "pa:opera st:opera pa:opera st:opera", strategy),
			new Production("operaeel", null, "pa:opera st:opera pa:eel st:eel", strategy),
			new Production("operabare", null, "pa:opera st:opera pa:bare st:bare", strategy),
			new Production("eelfoo", null, "pa:eel st:eel pa:foo st:foo", strategy),
			new Production("eelopera", null, "pa:eel st:eel pa:opera st:opera", strategy),
			new Production("eeleel", null, "pa:eel st:eel pa:eel st:eel", strategy),
			new Production("eelbare", null, "pa:eel st:eel pa:bare st:bare", strategy)
		);
		Assert.assertEquals(expected, words);
	}

	@Test
	public void simplifiedTriple() throws IOException, TimeoutException{
		String language = "xxx";
		File affFile = FileService.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"CHECKCOMPOUNDTRIPLE",
			"SIMPLIFIEDTRIPLE",
			"COMPOUNDMIN 2",
			"COMPOUNDFLAG A");
		affParser.parse(affFile);
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser);

		String line = "A";
		String[] inputCompounds = new String[]{
			"glass/A",
			"sko/A"
		};
		List<Production> words = wordGenerator.applyCompoundFlag(inputCompounds, line, 3, PermutationsWithRepetitions.MAX_COMPOUNDS_INFINITY);
		List<Production> expected = Arrays.asList(
			new Production("glassglass", null, "pa:glass st:glass pa:glass st:glass", strategy),
			new Production("glassko", null, "pa:glass st:glass pa:sko st:sko", strategy),
			new Production("skoglass", null, "pa:sko st:sko pa:glass st:glass", strategy)
		);
		Assert.assertEquals(expected, words);
	}

	@Test
	public void forbidWordDuplication() throws IOException, TimeoutException{
		String language = "xxx";
		File affFile = FileService.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"CHECKCOMPOUNDDUP",
			"COMPOUNDMIN 2",
			"COMPOUNDFLAG A");
		affParser.parse(affFile);
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser);

		String line = "A";
		String[] inputCompounds = new String[]{
			"foo/A",
			"bar/A",
			"yz/A"
		};
		List<Production> words = wordGenerator.applyCompoundFlag(inputCompounds, line, 100, 2);
		List<Production> expected = Arrays.asList(
			new Production("foobar", null, "pa:foo st:foo pa:bar st:bar", strategy),
			new Production("fooyz", null, "pa:foo st:foo pa:yz st:yz", strategy),
			new Production("barfoo", null, "pa:bar st:bar pa:foo st:foo", strategy),
			new Production("baryz", null, "pa:bar st:bar pa:yz st:yz", strategy),
			new Production("yzfoo", null, "pa:yz st:yz pa:foo st:foo", strategy),
			new Production("yzbar", null, "pa:yz st:yz pa:bar st:bar", strategy)
		);
		Assert.assertEquals(expected, words);
	}

	@Test
	public void withAffixes() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"COMPOUNDFLAG X",
			"PFX P Y 1",
			"PFX P 0 pre .",
			"SFX S Y 1",
			"SFX S 0 suf .");
		affParser.parse(affFile);
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser);


		String line = "foo/XPS";
		List<Production> words = wordGenerator.applyRules(line);

		Assert.assertEquals(4, words.size());
		//base production
		Assert.assertEquals(new Production("foo", "XPS", "st:foo", strategy), words.get(0));
		//onefold productions
		Assert.assertEquals(new Production("foosuf", "P", "st:foo", strategy), words.get(1));
		//twofold productions
		Assert.assertEquals(new Production("prefoo", null, "st:foo", strategy), words.get(2));
		Assert.assertEquals(new Production("prefoosuf", null, "st:foo", strategy), words.get(3));
		//lastfold productions


		line = "X";
		String[] inputCompounds = new String[]{
			"foo/XPS",
			"bar/XPS"
		};
		words = wordGenerator.applyCompoundFlag(inputCompounds, line, 4, 2);
words.forEach(stem -> System.out.println(stem));
		List<Production> expected = Arrays.asList(
			new Production("foofoo", "PS", "pa:foo st:foo pa:foo st:foo", strategy),
			new Production("foofoosuf", "P", "pa:foo st:foo pa:foo st:foo", strategy),
			new Production("prefoofoo", null, "pa:foo st:foo pa:foo st:foo", strategy),
			new Production("prefoofoosuf", null, "pa:foo st:foo pa:foo st:foo", strategy),
			new Production("foobar", "PS", "pa:foo st:foo pa:bar st:bar", strategy),
			new Production("foobarsuf", "P", "pa:foo st:foo pa:bar st:bar", strategy),
			new Production("prefoobar", null, "pa:foo st:foo pa:bar st:bar", strategy),
			new Production("prefoobarsuf", null, "pa:foo st:foo pa:bar st:bar", strategy),
			new Production("barfoo", "PS", "pa:bar st:bar pa:foo st:foo", strategy),
			new Production("barfoosuf", "P", "pa:bar st:bar pa:foo st:foo", strategy),
			new Production("prebarfoo", null, "pa:bar st:bar pa:foo st:foo", strategy),
			new Production("prebarfoosuf", null, "pa:bar st:bar pa:foo st:foo", strategy),
			new Production("barbar", "PS", "pa:bar st:bar pa:bar st:bar", strategy),
			new Production("barbarsuf", "P", "pa:bar st:bar pa:bar st:bar", strategy),
			new Production("prebarbar", null, "pa:bar st:bar pa:bar st:bar", strategy),
			new Production("prebarbarsuf", null, "pa:bar st:bar pa:bar st:bar", strategy)
		);
		Assert.assertEquals(expected, words);
	}

	@Test
	public void permitFlag() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"COMPOUNDFLAG X",
			"COMPOUNDPERMITFLAG Y",
			"PFX P Y 1",
			"PFX P 0 pre/Y .",
			"SFX S Y 1",
			"SFX S 0 suf/Y .");
		affParser.parse(affFile);
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser);


		String line = "foo/XPS";
		List<Production> words = wordGenerator.applyRules(line);

		Assert.assertEquals(4, words.size());
		//base production
		Assert.assertEquals(new Production("foo", "XPS", "st:foo", strategy), words.get(0));
		//onefold productions
		Assert.assertEquals(new Production("foosuf", "PY", "st:foo", strategy), words.get(1));
		//twofold productions
		Assert.assertEquals(new Production("prefoo", "Y", "st:foo", strategy), words.get(2));
		Assert.assertEquals(new Production("prefoosuf", "Y", "st:foo", strategy), words.get(3));
		//lastfold productions


		line = "X";
		String[] inputCompounds = new String[]{
			"foo/XPS",
			"bar/XPS"
		};
		words = wordGenerator.applyCompoundFlag(inputCompounds, line, 4, 2);
		List<Production> expected = Arrays.asList(
			new Production("foofoo", "PS", "pa:foo st:foo pa:foo st:foo", strategy),
			new Production("foofoosuf", "PY", "pa:foo st:foo pa:foosuf st:foosuf", strategy),
			new Production("fooprefoo", "PY", "pa:foo st:foo pa:prefoo st:prefoo", strategy),
			new Production("fooprefoosuf", "PY", "pa:foo st:foo pa:prefoosuf st:prefoosuf", strategy),
			new Production("foosuffoo", "PSY", "pa:foosuf st:foosuf pa:foo st:foo", strategy),
			new Production("foosuffoosuf", "PY", "pa:foosuf st:foosuf pa:foosuf st:foosuf", strategy),
			new Production("foosufprefoo", "PY", "pa:foosuf st:foosuf pa:prefoo st:prefoo", strategy),
			new Production("foosufprefoosuf", "PY", "pa:foosuf st:foosuf pa:prefoosuf st:prefoosuf", strategy),
			new Production("prefoofoo", "SY", "pa:prefoo st:prefoo pa:foo st:foo", strategy),
			new Production("prefoofoosuf", "Y", "pa:prefoo st:prefoo pa:foosuf st:foosuf", strategy),
			new Production("prefooprefoo", "Y", "pa:prefoo st:prefoo pa:prefoo st:prefoo", strategy),
			new Production("prefooprefoosuf", "Y", "pa:prefoo st:prefoo pa:prefoosuf st:prefoosuf", strategy),
			new Production("prefoosuffoo", "SY", "pa:prefoosuf st:prefoosuf pa:foo st:foo", strategy),
			new Production("prefoosuffoosuf", "Y", "pa:prefoosuf st:prefoosuf pa:foosuf st:foosuf", strategy),
			new Production("prefoosufprefoo", "Y", "pa:prefoosuf st:prefoosuf pa:prefoo st:prefoo", strategy),
			new Production("prefoosufprefoosuf", "Y", "pa:prefoosuf st:prefoosuf pa:prefoosuf st:prefoosuf", strategy),
			new Production("foobar", "PS", "pa:foo st:foo pa:bar st:bar", strategy),
			new Production("foobarsuf", "PY", "pa:foo st:foo pa:barsuf st:barsuf", strategy),
			new Production("fooprebar", "PY", "pa:foo st:foo pa:prebar st:prebar", strategy),
			new Production("fooprebarsuf", "PY", "pa:foo st:foo pa:prebarsuf st:prebarsuf", strategy),
			new Production("foosufbar", "PSY", "pa:foosuf st:foosuf pa:bar st:bar", strategy),
			new Production("foosufbarsuf", "PY", "pa:foosuf st:foosuf pa:barsuf st:barsuf", strategy),
			new Production("foosufprebar", "PY", "pa:foosuf st:foosuf pa:prebar st:prebar", strategy),
			new Production("foosufprebarsuf", "PY", "pa:foosuf st:foosuf pa:prebarsuf st:prebarsuf", strategy),
			new Production("prefoobar", "SY", "pa:prefoo st:prefoo pa:bar st:bar", strategy),
			new Production("prefoobarsuf", "Y", "pa:prefoo st:prefoo pa:barsuf st:barsuf", strategy),
			new Production("prefooprebar", "Y", "pa:prefoo st:prefoo pa:prebar st:prebar", strategy),
			new Production("prefooprebarsuf", "Y", "pa:prefoo st:prefoo pa:prebarsuf st:prebarsuf", strategy),
			new Production("prefoosufbar", "SY", "pa:prefoosuf st:prefoosuf pa:bar st:bar", strategy),
			new Production("prefoosufbarsuf", "Y", "pa:prefoosuf st:prefoosuf pa:barsuf st:barsuf", strategy),
			new Production("prefoosufprebar", "Y", "pa:prefoosuf st:prefoosuf pa:prebar st:prebar", strategy),
			new Production("prefoosufprebarsuf", "Y", "pa:prefoosuf st:prefoosuf pa:prebarsuf st:prebarsuf", strategy),
			new Production("barfoo", "PS", "pa:bar st:bar pa:foo st:foo", strategy),
			new Production("barfoosuf", "PY", "pa:bar st:bar pa:foosuf st:foosuf", strategy),
			new Production("barprefoo", "PY", "pa:bar st:bar pa:prefoo st:prefoo", strategy),
			new Production("barprefoosuf", "PY", "pa:bar st:bar pa:prefoosuf st:prefoosuf", strategy),
			new Production("barsuffoo", "PSY", "pa:barsuf st:barsuf pa:foo st:foo", strategy),
			new Production("barsuffoosuf", "PY", "pa:barsuf st:barsuf pa:foosuf st:foosuf", strategy),
			new Production("barsufprefoo", "PY", "pa:barsuf st:barsuf pa:prefoo st:prefoo", strategy),
			new Production("barsufprefoosuf", "PY", "pa:barsuf st:barsuf pa:prefoosuf st:prefoosuf", strategy),
			new Production("prebarfoo", "SY", "pa:prebar st:prebar pa:foo st:foo", strategy),
			new Production("prebarfoosuf", "Y", "pa:prebar st:prebar pa:foosuf st:foosuf", strategy),
			new Production("prebarprefoo", "Y", "pa:prebar st:prebar pa:prefoo st:prefoo", strategy),
			new Production("prebarprefoosuf", "Y", "pa:prebar st:prebar pa:prefoosuf st:prefoosuf", strategy),
			new Production("prebarsuffoo", "SY", "pa:prebarsuf st:prebarsuf pa:foo st:foo", strategy),
			new Production("prebarsuffoosuf", "Y", "pa:prebarsuf st:prebarsuf pa:foosuf st:foosuf", strategy),
			new Production("prebarsufprefoo", "Y", "pa:prebarsuf st:prebarsuf pa:prefoo st:prefoo", strategy),
			new Production("prebarsufprefoosuf", "Y", "pa:prebarsuf st:prebarsuf pa:prefoosuf st:prefoosuf", strategy),
			new Production("barbar", "PS", "pa:bar st:bar pa:bar st:bar", strategy),
			new Production("barbarsuf", "PY", "pa:bar st:bar pa:barsuf st:barsuf", strategy),
			new Production("barprebar", "PY", "pa:bar st:bar pa:prebar st:prebar", strategy),
			new Production("barprebarsuf", "PY", "pa:bar st:bar pa:prebarsuf st:prebarsuf", strategy),
			new Production("barsufbar", "PSY", "pa:barsuf st:barsuf pa:bar st:bar", strategy),
			new Production("barsufbarsuf", "PY", "pa:barsuf st:barsuf pa:barsuf st:barsuf", strategy),
			new Production("barsufprebar", "PY", "pa:barsuf st:barsuf pa:prebar st:prebar", strategy),
			new Production("barsufprebarsuf", "PY", "pa:barsuf st:barsuf pa:prebarsuf st:prebarsuf", strategy),
			new Production("prebarbar", "SY", "pa:prebar st:prebar pa:bar st:bar", strategy),
			new Production("prebarbarsuf", "Y", "pa:prebar st:prebar pa:barsuf st:barsuf", strategy),
			new Production("prebarprebar", "Y", "pa:prebar st:prebar pa:prebar st:prebar", strategy),
			new Production("prebarprebarsuf", "Y", "pa:prebar st:prebar pa:prebarsuf st:prebarsuf", strategy),
			new Production("prebarsufbar", "SY", "pa:prebarsuf st:prebarsuf pa:bar st:bar", strategy),
			new Production("prebarsufbarsuf", "Y", "pa:prebarsuf st:prebarsuf pa:barsuf st:barsuf", strategy),
			new Production("prebarsufprebar", "Y", "pa:prebarsuf st:prebarsuf pa:prebar st:prebar", strategy),
			new Production("prebarsufprebarsuf", "Y", "pa:prebarsuf st:prebarsuf pa:prebarsuf st:prebarsuf", strategy)
		);
		Assert.assertEquals(expected, words);
}

	@Test
	public void forbidFlag() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"COMPOUNDFLAG X",
			"COMPOUNDFORBIDFLAG Z",
			"PFX P Y 1",
			"PFX P 0 pre/Z .",
			"SFX S Y 1",
			"SFX S 0 suf/Z .");
		affParser.parse(affFile);
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser);


		String line = "foo/XPS";
		List<Production> words = wordGenerator.applyRules(line);

		Assert.assertEquals(4, words.size());
		//base production
		Assert.assertEquals(new Production("foo", "XPS", "st:foo", strategy), words.get(0));
		//onefold productions
		Assert.assertEquals(new Production("foosuf", "PZ", "st:foo", strategy), words.get(1));
		//twofold productions
		Assert.assertEquals(new Production("prefoo", "Z", "st:foo", strategy), words.get(2));
		Assert.assertEquals(new Production("prefoosuf", "Z", "st:foo", strategy), words.get(3));
		//lastfold productions


		line = "X";
		String[] inputCompounds = new String[]{
			"foo/XPS",
			"bar/XPS"
		};
		words = wordGenerator.applyCompoundFlag(inputCompounds, line, 4, 2);
		List<Production> expected = Arrays.asList(
			new Production("foofoo", "PS", "pa:foo st:foo pa:foo st:foo", strategy),
			new Production("foobar", "PS", "pa:foo st:foo pa:bar st:bar", strategy),
			new Production("barfoo", "PS", "pa:bar st:bar pa:foo st:foo", strategy),
			new Production("barbar", "PS", "pa:bar st:bar pa:bar st:bar", strategy)
		);
		Assert.assertEquals(expected, words);
	}

}
