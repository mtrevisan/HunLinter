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
words.forEach(stem -> System.out.println(stem));
		//missing: foosufbar, fooprebarsuf, ...
		List<Production> expected = Arrays.asList(
			new Production("foofoo", "PS", "pa:foo st:foo pa:foo st:foo", strategy),
			new Production("foosuffoo", "PS", "pa:foo st:foo pa:foo st:foo", strategy),
			new Production("foosuffoosuf", "PS", "pa:foo st:foo pa:foo st:foo", strategy),
			new Production("foosufprefoo", "PS", "pa:foo st:foo pa:foo st:foo", strategy),
			new Production("foosufprefoosuf", "PS", "pa:foo st:foo pa:foo st:foo", strategy),
			new Production("prefoofoo", "PS", "pa:foo st:foo pa:foo st:foo", strategy),
			new Production("prefoofoosuf", "PS", "pa:foo st:foo pa:foo st:foo", strategy),
			new Production("prefooprefoo", "PS", "pa:foo st:foo pa:foo st:foo", strategy),
			new Production("prefooprefoosuf", "PS", "pa:foo st:foo pa:foo st:foo", strategy),
			new Production("prefoosuffoo", "PS", "pa:foo st:foo pa:foo st:foo", strategy),
			new Production("prefoosuffoosuf", "PS", "pa:foo st:foo pa:foo st:foo", strategy),
			new Production("prefoosufprefoo", "PS", "pa:foo st:foo pa:foo st:foo", strategy),
			new Production("prefoosufprefoosuf", "PS", "pa:foo st:foo pa:foo st:foo", strategy),
			new Production("foofoosuf", "PY", "pa:foo st:foo pa:foo st:foo", strategy),
			new Production("foobar", "PS", "pa:foo st:foo pa:bar st:bar", strategy),
			new Production("foobarsuf", "PY", "pa:foo st:foo pa:bar st:bar", strategy),
			new Production("prefoobar", "Y", "pa:foo st:foo pa:bar st:bar", strategy),
			new Production("prefoobarsuf", "Y", "pa:foo st:foo pa:bar st:bar", strategy),
			new Production("prefooprebar", "Y", "pa:foo st:foo pa:bar st:bar", strategy),
			new Production("prefooprebarsuf", "Y", "pa:foo st:foo pa:bar st:bar", strategy),
			new Production("barfoo", "PS", "pa:bar st:bar pa:foo st:foo", strategy),
			new Production("barfoosuf", "PY", "pa:bar st:bar pa:foo st:foo", strategy),
			new Production("prebarfoo", "Y", "pa:bar st:bar pa:foo st:foo", strategy),
			new Production("prebarfoosuf", "Y", "pa:bar st:bar pa:foo st:foo", strategy),
			new Production("prebarprefoo", "Y", "pa:bar st:bar pa:foo st:foo", strategy),
			new Production("prebarprefoosuf", "Y", "pa:bar st:bar pa:foo st:foo", strategy),
			new Production("barbar", "PS", "	pa:bar st:bar pa:bar st:bar", strategy),
			new Production("barbarsuf", "PY", "	pa:bar st:bar pa:bar st:bar", strategy),
			new Production("prebarbar", "Y", "	pa:bar st:bar pa:bar st:bar", strategy),
			new Production("prebarbarsuf", "Y", "	pa:bar st:bar pa:bar st:bar", strategy),
			new Production("prebarprebar", "Y", "	pa:bar st:bar pa:bar st:bar", strategy),
			new Production("prebarprebarsuf", "Y", "	pa:bar st:bar pa:bar st:bar", strategy)
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
