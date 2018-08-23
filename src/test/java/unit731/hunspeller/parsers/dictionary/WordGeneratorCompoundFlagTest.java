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
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryEntry;
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
			new Production("foofoo", Arrays.asList(new DictionaryEntry("foo", strategy), new DictionaryEntry("foo", strategy))),
			new Production("foobar", Arrays.asList(new DictionaryEntry("foo", strategy), new DictionaryEntry("bar", strategy))),
			new Production("fooxy", Arrays.asList(new DictionaryEntry("foo", strategy), new DictionaryEntry("xy", strategy))),
			new Production("fooyz", Arrays.asList(new DictionaryEntry("foo", strategy), new DictionaryEntry("yz", strategy))),
			new Production("barfoo", Arrays.asList(new DictionaryEntry("bar", strategy), new DictionaryEntry("foo", strategy))),
			new Production("barbar", Arrays.asList(new DictionaryEntry("bar", strategy), new DictionaryEntry("bar", strategy))),
			new Production("barxy", Arrays.asList(new DictionaryEntry("bar", strategy), new DictionaryEntry("xy", strategy))),
			new Production("baryz", Arrays.asList(new DictionaryEntry("bar", strategy), new DictionaryEntry("yz", strategy))),
			new Production("xyfoo", Arrays.asList(new DictionaryEntry("xy", strategy), new DictionaryEntry("foo", strategy))),
			new Production("xybar", Arrays.asList(new DictionaryEntry("xy", strategy), new DictionaryEntry("bar", strategy)))
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
			new Production("foofoo", Arrays.asList(new DictionaryEntry("foo", strategy), new DictionaryEntry("foo", strategy))),
			new Production("foobar", Arrays.asList(new DictionaryEntry("foo", strategy), new DictionaryEntry("bar", strategy))),
			new Production("barfoo", Arrays.asList(new DictionaryEntry("bar", strategy), new DictionaryEntry("foo", strategy))),
			new Production("barbar", Arrays.asList(new DictionaryEntry("bar", strategy), new DictionaryEntry("bar", strategy)))
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
			new Production("foofoo", Arrays.asList(new DictionaryEntry("foo", strategy), new DictionaryEntry("foo", strategy))),
			new Production("fooeel", Arrays.asList(new DictionaryEntry("foo", strategy), new DictionaryEntry("eel", strategy))),
			new Production("foobare", Arrays.asList(new DictionaryEntry("foo", strategy), new DictionaryEntry("bare", strategy))),
			new Production("operafoo", Arrays.asList(new DictionaryEntry("opera", strategy), new DictionaryEntry("foo", strategy))),
			new Production("operaopera", Arrays.asList(new DictionaryEntry("opera", strategy), new DictionaryEntry("opera", strategy))),
			new Production("operaeel", Arrays.asList(new DictionaryEntry("opera", strategy), new DictionaryEntry("eel", strategy))),
			new Production("operabare", Arrays.asList(new DictionaryEntry("opera", strategy), new DictionaryEntry("bare", strategy))),
			new Production("eelfoo", Arrays.asList(new DictionaryEntry("eel", strategy), new DictionaryEntry("foo", strategy))),
			new Production("eelopera", Arrays.asList(new DictionaryEntry("eel", strategy), new DictionaryEntry("opera", strategy))),
			new Production("eeleel", Arrays.asList(new DictionaryEntry("eel", strategy), new DictionaryEntry("eel", strategy))),
			new Production("eelbare", Arrays.asList(new DictionaryEntry("eel", strategy), new DictionaryEntry("bare", strategy)))
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
			new Production("glassglass", Arrays.asList(new DictionaryEntry("glass", strategy), new DictionaryEntry("glass", strategy))),
			new Production("glassko", Arrays.asList(new DictionaryEntry("glass", strategy), new DictionaryEntry("sko", strategy))),
			new Production("skoglass", Arrays.asList(new DictionaryEntry("sko", strategy), new DictionaryEntry("glass", strategy)))
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
			new Production("foobar", Arrays.asList(new DictionaryEntry("foo", strategy), new DictionaryEntry("bar", strategy))),
			new Production("fooyz", Arrays.asList(new DictionaryEntry("foo", strategy), new DictionaryEntry("yz", strategy))),
			new Production("barfoo", Arrays.asList(new DictionaryEntry("bar", strategy), new DictionaryEntry("foo", strategy))),
			new Production("baryz", Arrays.asList(new DictionaryEntry("bar", strategy), new DictionaryEntry("yz", strategy))),
			new Production("yzfoo", Arrays.asList(new DictionaryEntry("yz", strategy), new DictionaryEntry("foo", strategy))),
			new Production("yzbar", Arrays.asList(new DictionaryEntry("yz", strategy), new DictionaryEntry("bar", strategy)))
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
			new Production("foofoo", null, "pa:foo st:foo pa:foo st:foo", strategy),
			new Production("foofoosuf", null, "pa:foo st:foo pa:foosuf st:foosuf", strategy),
			new Production("fooprefoo", null, "pa:foo st:foo pa:prefoo st:prefoo", strategy),
			new Production("fooprefoosuf", null, "pa:foo st:foo pa:prefoosuf st:prefoosuf", strategy),
			new Production("foosuffoo", null, "pa:foosuf st:foosuf pa:foo st:foo", strategy),
			new Production("foosuffoosuf", null, "pa:foosuf st:foosuf pa:foosuf st:foosuf", strategy),
			new Production("foosufprefoo", null, "pa:foosuf st:foosuf pa:prefoo st:prefoo", strategy),
			new Production("foosufprefoosuf", null, "pa:foosuf st:foosuf pa:prefoosuf st:prefoosuf", strategy),
			new Production("prefoofoo", null, "pa:prefoo st:prefoo pa:foo st:foo", strategy),
			new Production("prefoofoosuf", null, "pa:prefoo st:prefoo pa:foosuf st:foosuf", strategy),
			new Production("prefooprefoo", null, "pa:prefoo st:prefoo pa:prefoo st:prefoo", strategy),
			new Production("prefooprefoosuf", null, "pa:prefoo st:prefoo pa:prefoosuf st:prefoosuf", strategy),
			new Production("prefoosuffoo", null, "pa:prefoosuf st:prefoosuf pa:foo st:foo", strategy),
			new Production("prefoosuffoosuf", null, "pa:prefoosuf st:prefoosuf pa:foosuf st:foosuf", strategy),
			new Production("prefoosufprefoo", null, "pa:prefoosuf st:prefoosuf pa:prefoo st:prefoo", strategy),
			new Production("prefoosufprefoosuf", null, "pa:prefoosuf st:prefoosuf pa:prefoosuf st:prefoosuf", strategy),
			new Production("foobar", null, "pa:foo st:foo pa:bar st:bar", strategy),
			new Production("foobarsuf", null, "pa:foo st:foo pa:barsuf st:barsuf", strategy),
			new Production("fooprebar", null, "pa:foo st:foo pa:prebar st:prebar", strategy),
			new Production("fooprebarsuf", null, "pa:foo st:foo pa:prebarsuf st:prebarsuf", strategy),
			new Production("foosufbar", null, "pa:foosuf st:foosuf pa:bar st:bar", strategy),
			new Production("foosufbarsuf", null, "pa:foosuf st:foosuf pa:barsuf st:barsuf", strategy),
			new Production("foosufprebar", null, "pa:foosuf st:foosuf pa:prebar st:prebar", strategy),
			new Production("foosufprebarsuf", null, "pa:foosuf st:foosuf pa:prebarsuf st:prebarsuf", strategy),
			new Production("prefoobar", null, "pa:prefoo st:prefoo pa:bar st:bar", strategy),
			new Production("prefoobarsuf", null, "pa:prefoo st:prefoo pa:barsuf st:barsuf", strategy),
			new Production("prefooprebar", null, "pa:prefoo st:prefoo pa:prebar st:prebar", strategy),
			new Production("prefooprebarsuf", null, "pa:prefoo st:prefoo pa:prebarsuf st:prebarsuf", strategy),
			new Production("prefoosufbar", null, "pa:prefoosuf st:prefoosuf pa:bar st:bar", strategy),
			new Production("prefoosufbarsuf", null, "pa:prefoosuf st:prefoosuf pa:barsuf st:barsuf", strategy),
			new Production("prefoosufprebar", null, "pa:prefoosuf st:prefoosuf pa:prebar st:prebar", strategy),
			new Production("prefoosufprebarsuf", null, "pa:prefoosuf st:prefoosuf pa:prebarsuf st:prebarsuf", strategy),
			new Production("barfoo", null, "pa:bar st:bar pa:foo st:foo", strategy),
			new Production("barfoosuf", null, "pa:bar st:bar pa:foosuf st:foosuf", strategy),
			new Production("barprefoo", null, "pa:bar st:bar pa:prefoo st:prefoo", strategy),
			new Production("barprefoosuf", null, "pa:bar st:bar pa:prefoosuf st:prefoosuf", strategy),
			new Production("barsuffoo", null, "pa:barsuf st:barsuf pa:foo st:foo", strategy),
			new Production("barsuffoosuf", null, "pa:barsuf st:barsuf pa:foosuf st:foosuf", strategy),
			new Production("barsufprefoo", null, "pa:barsuf st:barsuf pa:prefoo st:prefoo", strategy),
			new Production("barsufprefoosuf", null, "pa:barsuf st:barsuf pa:prefoosuf st:prefoosuf", strategy),
			new Production("prebarfoo", null, "pa:prebar st:prebar pa:foo st:foo", strategy),
			new Production("prebarfoosuf", null, "pa:prebar st:prebar pa:foosuf st:foosuf", strategy),
			new Production("prebarprefoo", null, "pa:prebar st:prebar pa:prefoo st:prefoo", strategy),
			new Production("prebarprefoosuf", null, "pa:prebar st:prebar pa:prefoosuf st:prefoosuf", strategy),
			new Production("prebarsuffoo", null, "pa:prebarsuf st:prebarsuf pa:foo st:foo", strategy),
			new Production("prebarsuffoosuf", null, "pa:prebarsuf st:prebarsuf pa:foosuf st:foosuf", strategy),
			new Production("prebarsufprefoo", null, "pa:prebarsuf st:prebarsuf pa:prefoo st:prefoo", strategy),
			new Production("prebarsufprefoosuf", null, "pa:prebarsuf st:prebarsuf pa:prefoosuf st:prefoosuf", strategy),
			new Production("barbar", null, "pa:bar st:bar pa:bar st:bar", strategy),
			new Production("barbarsuf", null, "pa:bar st:bar pa:barsuf st:barsuf", strategy),
			new Production("barprebar", null, "pa:bar st:bar pa:prebar st:prebar", strategy),
			new Production("barprebarsuf", null, "pa:bar st:bar pa:prebarsuf st:prebarsuf", strategy),
			new Production("barsufbar", null, "pa:barsuf st:barsuf pa:bar st:bar", strategy),
			new Production("barsufbarsuf", null, "pa:barsuf st:barsuf pa:barsuf st:barsuf", strategy),
			new Production("barsufprebar", null, "pa:barsuf st:barsuf pa:prebar st:prebar", strategy),
			new Production("barsufprebarsuf", null, "pa:barsuf st:barsuf pa:prebarsuf st:prebarsuf", strategy),
			new Production("prebarbar", null, "pa:prebar st:prebar pa:bar st:bar", strategy),
			new Production("prebarbarsuf", null, "pa:prebar st:prebar pa:barsuf st:barsuf", strategy),
			new Production("prebarprebar", null, "pa:prebar st:prebar pa:prebar st:prebar", strategy),
			new Production("prebarprebarsuf", null, "pa:prebar st:prebar pa:prebarsuf st:prebarsuf", strategy),
			new Production("prebarsufbar", null, "pa:prebarsuf st:prebarsuf pa:bar st:bar", strategy),
			new Production("prebarsufbarsuf", null, "pa:prebarsuf st:prebarsuf pa:barsuf st:barsuf", strategy),
			new Production("prebarsufprebar", null, "pa:prebarsuf st:prebarsuf pa:prebar st:prebar", strategy),
			new Production("prebarsufprebarsuf", null, "pa:prebarsuf st:prebarsuf pa:prebarsuf st:prebarsuf", strategy)
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
			new Production("foofoo", null, "pa:foo st:foo pa:foo st:foo", strategy),
			new Production("foobar", null, "pa:foo st:foo pa:bar st:bar", strategy),
			new Production("barfoo", null, "pa:bar st:bar pa:foo st:foo", strategy),
			new Production("barbar", null, "pa:bar st:bar pa:bar st:bar", strategy)
		);
		Assert.assertEquals(expected, words);
	}

}
