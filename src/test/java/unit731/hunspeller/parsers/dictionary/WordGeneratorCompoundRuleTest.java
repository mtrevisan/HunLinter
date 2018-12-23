package unit731.hunspeller.parsers.dictionary;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.vos.Production;
import unit731.hunspeller.services.FileHelper;


/** @see <a href="https://github.com/hunspell/hunspell/tree/master/tests/v1cmdline">Hunspell tests</a> */
public class WordGeneratorCompoundRuleTest{

	private final Backbone backbone = new Backbone(null, null);


	private void loadData(String affixFilePath) throws IOException{
		backbone.loadFile(affixFilePath);
	}

	private Production createProduction(String word, String continuationFlags, String morphologicalFields){
		FlagParsingStrategy strategy = backbone.getAffParser().getFlagParsingStrategy();
		return new Production(word, continuationFlags, morphologicalFields, null, strategy);
	}

	@Test
	public void testBjörnJacke() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE vw",
			"SFX A Y 5",
			"SFX A 0 e .",
			"SFX A 0 er .",
			"SFX A 0 en .",
			"SFX A 0 em .",
			"SFX A 0 es .");
		loadData(affFile.getAbsolutePath());

		String line = "vw";
		String[] inputCompounds = new String[]{
			"arbeits/v",
			"scheu/Aw",
			"farbig/A"
		};
		List<Production> words = backbone.getWordGenerator().applyCompoundRules(inputCompounds, line, 10);

		List<Production> expected = Arrays.asList(
			createProduction("arbeitsscheu", "A", "pa:arbeits st:arbeits pa:scheu st:scheu"),
			createProduction("arbeitsscheue", null, "pa:arbeits st:arbeits pa:scheu st:scheu"),
			createProduction("arbeitsscheuer", null, "pa:arbeits st:arbeits pa:scheu st:scheu"),
			createProduction("arbeitsscheuen", null, "pa:arbeits st:arbeits pa:scheu st:scheu"),
			createProduction("arbeitsscheuem", null, "pa:arbeits st:arbeits pa:scheu st:scheu"),
			createProduction("arbeitsscheues", null, "pa:arbeits st:arbeits pa:scheu st:scheu")
		);
		Assert.assertEquals(expected, words);
	}

	@Test
	public void simple() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPOUNDMIN 1",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE ABC");
		loadData(affFile.getAbsolutePath());

		String line = "ABC";
		String[] inputCompounds = new String[]{
			"a/A",
			"b/B",
			"c/BC"
		};
		List<Production> words = backbone.getWordGenerator().applyCompoundRules(inputCompounds, line, 37);

		List<Production> expected = Arrays.asList(
			createProduction("abc", null, "pa:a st:a pa:b st:b pa:c st:c"),
			createProduction("acc", null, "pa:a st:a pa:c st:c pa:c st:c")
		);
		Assert.assertEquals(expected, words);
	}

	@Test
	public void infinite() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPOUNDMIN 1",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE A*B*C*");
		loadData(affFile.getAbsolutePath());

		String line = "A*B*C*";
		String[] inputCompounds = new String[]{
			"a/A",
			"b/B",
			"c/BC"
		};
		List<Production> words = backbone.getWordGenerator().applyCompoundRules(inputCompounds, line, 37);

		List<Production> expected = Arrays.asList(
			createProduction("aa", null, "pa:a st:a pa:a st:a"),
			createProduction("ab", null, "pa:a st:a pa:b st:b"),
			createProduction("ac", null, "pa:a st:a pa:c st:c"),
			createProduction("bb", null, "pa:b st:b pa:b st:b"),
			createProduction("bc", null, "pa:b st:b pa:c st:c"),
			createProduction("cb", null, "pa:c st:c pa:b st:b"),
			createProduction("cc", null, "pa:c st:c pa:c st:c"),
			createProduction("aaa", null, "pa:a st:a pa:a st:a pa:a st:a"),
			createProduction("aab", null, "pa:a st:a pa:a st:a pa:b st:b"),
			createProduction("aac", null, "pa:a st:a pa:a st:a pa:c st:c"),
			createProduction("abb", null, "pa:a st:a pa:b st:b pa:b st:b"),
			createProduction("abc", null, "pa:a st:a pa:b st:b pa:c st:c"),
			createProduction("acb", null, "pa:a st:a pa:c st:c pa:b st:b"),
			createProduction("acc", null, "pa:a st:a pa:c st:c pa:c st:c"),
			createProduction("bbb", null, "pa:b st:b pa:b st:b pa:b st:b"),
			createProduction("bbc", null, "pa:b st:b pa:b st:b pa:c st:c"),
			createProduction("bcb", null, "pa:b st:b pa:c st:c pa:b st:b"),
			createProduction("bcc", null, "pa:b st:b pa:c st:c pa:c st:c"),
			createProduction("cbb", null, "pa:c st:c pa:b st:b pa:b st:b"),
			createProduction("cbc", null, "pa:c st:c pa:b st:b pa:c st:c"),
			createProduction("ccb", null, "pa:c st:c pa:c st:c pa:b st:b"),
			createProduction("ccc", null, "pa:c st:c pa:c st:c pa:c st:c"),
			createProduction("aaaa", null, "pa:a st:a pa:a st:a pa:a st:a pa:a st:a"),
			createProduction("aaab", null, "pa:a st:a pa:a st:a pa:a st:a pa:b st:b"),
			createProduction("aaac", null, "pa:a st:a pa:a st:a pa:a st:a pa:c st:c"),
			createProduction("aabb", null, "pa:a st:a pa:a st:a pa:b st:b pa:b st:b"),
			createProduction("aabc", null, "pa:a st:a pa:a st:a pa:b st:b pa:c st:c"),
			createProduction("aacb", null, "pa:a st:a pa:a st:a pa:c st:c pa:b st:b"),
			createProduction("aacc", null, "pa:a st:a pa:a st:a pa:c st:c pa:c st:c"),
			createProduction("abbb", null, "pa:a st:a pa:b st:b pa:b st:b pa:b st:b"),
			createProduction("abbc", null, "pa:a st:a pa:b st:b pa:b st:b pa:c st:c"),
			createProduction("abcb", null, "pa:a st:a pa:b st:b pa:c st:c pa:b st:b"),
			createProduction("abcc", null, "pa:a st:a pa:b st:b pa:c st:c pa:c st:c"),
			createProduction("acbb", null, "pa:a st:a pa:c st:c pa:b st:b pa:b st:b"),
			createProduction("acbc", null, "pa:a st:a pa:c st:c pa:b st:b pa:c st:c"),
			createProduction("accb", null, "pa:a st:a pa:c st:c pa:c st:c pa:b st:b"),
			createProduction("accc", null, "pa:a st:a pa:c st:c pa:c st:c pa:c st:c")
		);
		Assert.assertEquals(expected, words);
	}

	@Test
	public void zeroOrOne() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPOUNDMIN 1",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE A?B?C?");
		loadData(affFile.getAbsolutePath());

		String line = "A?B?C?";
		String[] inputCompounds = new String[]{
			"a/A",
			"b/B",
			"c/BC"
		};
		List<Production> words = backbone.getWordGenerator().applyCompoundRules(inputCompounds, line, 37);

		List<Production> expected = Arrays.asList(
			createProduction("bc", null, "pa:b st:b pa:c st:c"),
			createProduction("cc", null, "pa:c st:c pa:c st:c"),
			createProduction("ac", null, "pa:a st:a pa:c st:c"),
			createProduction("ab", null, "pa:a st:a pa:b st:b"),
			createProduction("abc", null, "pa:a st:a pa:b st:b pa:c st:c"),
			createProduction("acc", null, "pa:a st:a pa:c st:c pa:c st:c")
		);
		Assert.assertEquals(expected, words);
	}

	@Test
	public void longFlag() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"FLAG long",
			"COMPOUNDMIN 1",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE (aa)?(bb)?(cc)?");
		loadData(affFile.getAbsolutePath());

		String line = "(aa)?(bb)?(cc)?";
		String[] inputCompounds = new String[]{
			"a/aa",
			"b/bb",
			"c/bbcc"
		};
		List<Production> words = backbone.getWordGenerator().applyCompoundRules(inputCompounds, line, 37);

		List<Production> expected = Arrays.asList(
			createProduction("cc", null, "pa:c st:c pa:c st:c"),
			createProduction("bc", null, "pa:b st:b pa:c st:c"),
			createProduction("ac", null, "pa:a st:a pa:c st:c"),
			createProduction("ab", null, "pa:a st:a pa:b st:b"),
			createProduction("acc", null, "pa:a st:a pa:c st:c pa:c st:c"),
			createProduction("abc", null, "pa:a st:a pa:b st:b pa:c st:c")
		);
		Assert.assertEquals(expected, words);
	}

	@Test
	public void numericalFlag() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"FLAG num",
			"COMPOUNDMIN 1",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE (1)?(2)?(3)?");
		loadData(affFile.getAbsolutePath());

		String line = "(1)?(2)?(3)?";
		String[] inputCompounds = new String[]{
			"a/1",
			"b/2",
			"c/2,3"
		};
		List<Production> words = backbone.getWordGenerator().applyCompoundRules(inputCompounds, line, 37);

		List<Production> expected = Arrays.asList(
			createProduction("bc", null, "pa:b st:b pa:c st:c"),
			createProduction("cc", null, "pa:c st:c pa:c st:c"),
			createProduction("ac", null, "pa:a st:a pa:c st:c"),
			createProduction("ab", null, "pa:a st:a pa:b st:b"),
			createProduction("abc", null, "pa:a st:a pa:b st:b pa:c st:c"),
			createProduction("acc", null, "pa:a st:a pa:c st:c pa:c st:c")
		);
		Assert.assertEquals(expected, words);
	}


	@Test(expected = IllegalArgumentException.class)
	public void forbiddenWordMissingRule() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE vw");
		loadData(affFile.getAbsolutePath());

		String line = "vw";
		String[] inputCompounds = new String[]{
			"arbeits/v",
			"scheu/v"
		};
		backbone.getWordGenerator().applyCompoundRules(inputCompounds, line, 5);
	}

	@Test
	public void forbiddenWord() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"FORBIDDENWORD X",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE vw");
		loadData(affFile.getAbsolutePath());

		String line = "vw";
		String[] inputCompounds = new String[]{
			"arbeits/v",
			"scheu/wX"
		};
		List<Production> words = backbone.getWordGenerator().applyCompoundRules(inputCompounds, line, 5);

		Assert.assertTrue(words.isEmpty());
	}


	@Test
	public void forceUppercase() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"FORCEUCASE U",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE vw");
		loadData(affFile.getAbsolutePath());

		String line = "vw";
		String[] inputCompounds = new String[]{
			"arbeits/v",
			"scheu/wU"
		};
		List<Production> words = backbone.getWordGenerator().applyCompoundRules(inputCompounds, line, 5);
//words.forEach(System.out::println);

		List<Production> expected = Arrays.asList(
			createProduction("Arbeitsscheu", null, "pa:arbeits st:arbeits pa:scheu st:scheu")
		);
		Assert.assertEquals(expected, words);
	}

}
