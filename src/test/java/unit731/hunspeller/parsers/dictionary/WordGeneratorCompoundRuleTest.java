package unit731.hunspeller.parsers.dictionary;

import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryEntry;
import unit731.hunspeller.services.FileHelper;


/** @see <a href="https://github.com/hunspell/hunspell/tree/master/tests/v1cmdline">Hunspell tests</a> */
public class WordGeneratorCompoundRuleTest{

	private final Backbone backbone = new Backbone(null, null);


	private void loadData(String affixFilePath) throws IOException{
		backbone.loadFile(affixFilePath);
	}

	private Production createProduction(String word){
		return new Production(word, null, (List<DictionaryEntry>)null, null);
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
		List<Production> words = backbone.getWordGenerator().applyCompoundRules(inputCompounds, line, 5);
		List<String> expected = Arrays.asList("arbeitsscheu");
		Assert.assertEquals(expected.stream().map(exp -> createProduction(exp)).collect(Collectors.toList()), words);
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
		List<String> expected = Arrays.asList("abc", "acc");
		Assert.assertEquals(expected.stream().map(exp -> createProduction(exp)).collect(Collectors.toList()), words);
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
		List<String> expected = Arrays.asList("a", "b", "c", "aa", "ab", "ac", "bb", "bc", "cb", "cc", "aaa", "aab", "aac", "abb",
				"abc", "acb", "acc", "bbb", "bbc", "bcb", "bcc", "cbb", "cbc", "ccb", "ccc", "aaaa", "aaab", "aaac", "aabb", "aabc", "aacb", "aacc",
				"abbb", "abbc", "abcb", "abcc", "acbb");
		Assert.assertEquals(expected.stream().map(exp -> createProduction(exp)).collect(Collectors.toList()), words);
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
		List<String> expected = Arrays.asList("a", "b", "c", "ab", "ac", "bc", "cc", "abc", "acc");
		Assert.assertEquals(expected.stream().map(exp -> createProduction(exp)).collect(Collectors.toList()), words);
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
		List<String> expected = Arrays.asList("a", "b", "c", "ab", "ac", "bc", "cc", "abc", "acc");
		Assert.assertEquals(expected.stream().map(exp -> createProduction(exp)).collect(Collectors.toList()), words);
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
		List<String> expected = Arrays.asList("a", "b", "c", "ab", "ac", "bc", "cc", "abc", "acc");
		Assert.assertEquals(expected.stream().map(exp -> createProduction(exp)).collect(Collectors.toList()), words);
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
		List<String> expected = Arrays.asList("Arbeitsscheu");
		Assert.assertEquals(expected.stream().map(exp -> createProduction(exp)).collect(Collectors.toList()), words);
	}

}
