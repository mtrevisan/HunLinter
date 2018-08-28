package unit731.hunspeller.parsers.dictionary;

import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryEntry;
import unit731.hunspeller.services.FileHelper;


/** @see <a href="https://github.com/hunspell/hunspell/tree/master/tests/v1cmdline">Hunspell tests</a> */
public class WordGeneratorCompoundRuleTest{

	private final Backbone backbone = new Backbone(null, null);


	private void loadData(String affixFilePath) throws IOException{
		backbone.loadFile(affixFilePath);
	}

	private Production createProduction(String word, String continuationFlags, String morphologicalFields){
		FlagParsingStrategy strategy = backbone.getAffParser().getFlagParsingStrategy();
		return new Production(word, continuationFlags, morphologicalFields, strategy);
	}

	@Test
	public void compoundRule_BjörnJacke() throws IOException, TimeoutException{
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
		Assert.assertEquals(expected.stream().map(exp -> new Production(exp, null, (List<DictionaryEntry>)null, null)).collect(Collectors.toList()), words);
	}

	@Test
	public void compoundRuleSimple() throws IOException, TimeoutException{
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
		Assert.assertEquals(expected.stream().map(exp -> new Production(exp, null, (List<DictionaryEntry>)null, null)).collect(Collectors.toList()), words);
	}

	@Test
	public void compoundRuleInfinite() throws IOException, TimeoutException{
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
		Assert.assertEquals(expected.stream().map(exp -> new Production(exp, null, (List<DictionaryEntry>)null, null)).collect(Collectors.toList()), words);
	}

	@Test
	public void compoundRuleZeroOrOne() throws IOException, TimeoutException{
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
		Assert.assertEquals(expected.stream().map(exp -> new Production(exp, null, (List<DictionaryEntry>)null, null)).collect(Collectors.toList()), words);
	}

	@Test
	public void compoundRuleLongFlag() throws IOException, TimeoutException{
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
		Assert.assertEquals(expected.stream().map(exp -> new Production(exp, null, (List<DictionaryEntry>)null, null)).collect(Collectors.toList()), words);
	}

	@Test
	public void compoundRuleNumericalFlag() throws IOException, TimeoutException{
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
		Assert.assertEquals(expected.stream().map(exp -> new Production(exp, null, (List<DictionaryEntry>)null, null)).collect(Collectors.toList()), words);
	}

}
