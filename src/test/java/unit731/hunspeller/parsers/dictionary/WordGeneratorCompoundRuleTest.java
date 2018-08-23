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
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryEntry;
import unit731.hunspeller.services.FileService;


/** @see <a href="https://github.com/hunspell/hunspell/tree/master/tests/v1cmdline">Hunspell tests</a> */
public class WordGeneratorCompoundRuleTest{

	private final AffixParser affParser = new AffixParser();


	@Test
	public void compoundRule_BjörnJacke() throws IOException, TimeoutException{
		String language = "xxx";
		File affFile = FileService.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE vw",
			"SFX A Y 5",
			"SFX A 0 e .",
			"SFX A 0 er .",
			"SFX A 0 en .",
			"SFX A 0 em .",
			"SFX A 0 es .");
		affParser.parse(affFile);
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser);

		String line = "vw";
		String[] inputCompounds = new String[]{
			"arbeits/v",
			"scheu/Aw",
			"farbig/A"
		};
		List<Production> words = wordGenerator.applyCompoundRules(inputCompounds, line, 5);
		Assert.assertEquals(1, words.size());
		List<String> expected = Arrays.asList("arbeitsscheu");
		Assert.assertEquals(expected.stream().map(exp -> new Production(exp, null, (List<DictionaryEntry>)null, strategy)).collect(Collectors.toList()), words);
	}

	@Test
	public void compoundRuleSimple() throws IOException, TimeoutException{
		String language = "xxx";
		File affFile = FileService.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPOUNDMIN 1",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE ABC");
		affParser.parse(affFile);
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser);

		String line = "ABC";
		String[] inputCompounds = new String[]{
			"a/A",
			"b/B",
			"c/BC"
		};
		List<Production> words = wordGenerator.applyCompoundRules(inputCompounds, line, 37);
		Assert.assertEquals(2, words.size());
		List<String> expected = Arrays.asList("abc", "acc");
		Assert.assertEquals(expected.stream().map(exp -> new Production(exp, null, (List<DictionaryEntry>)null, strategy)).collect(Collectors.toList()), words);
	}

	@Test
	public void compoundRuleInfinite() throws IOException, TimeoutException{
		String language = "xxx";
		File affFile = FileService.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPOUNDMIN 1",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE A*B*C*");
		affParser.parse(affFile);
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser);

		String line = "A*B*C*";
		String[] inputCompounds = new String[]{
			"a/A",
			"b/B",
			"c/BC"
		};
		List<Production> words = wordGenerator.applyCompoundRules(inputCompounds, line, 37);
		Assert.assertEquals(37, words.size());
		List<String> expected = Arrays.asList("a", "b", "c", "aa", "ab", "ac", "bb", "bc", "cb", "cc", "aaa", "aab", "aac", "abb",
				"abc", "acb", "acc", "bbb", "bbc", "bcb", "bcc", "cbb", "cbc", "ccb", "ccc", "aaaa", "aaab", "aaac", "aabb", "aabc", "aacb", "aacc",
				"abbb", "abbc", "abcb", "abcc", "acbb");
		Assert.assertEquals(expected.stream().map(exp -> new Production(exp, null, (List<DictionaryEntry>)null, strategy)).collect(Collectors.toList()), words);
	}

	@Test
	public void compoundRuleZeroOrOne() throws IOException, TimeoutException{
		String language = "xxx";
		File affFile = FileService.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPOUNDMIN 1",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE A?B?C?");
		affParser.parse(affFile);
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser);

		String line = "A?B?C?";
		String[] inputCompounds = new String[]{
			"a/A",
			"b/B",
			"c/BC"
		};
		List<Production> words = wordGenerator.applyCompoundRules(inputCompounds, line, 37);
		Assert.assertEquals(9, words.size());
		List<String> expected = Arrays.asList("a", "b", "c", "ab", "ac", "bc", "cc", "abc", "acc");
		Assert.assertEquals(expected.stream().map(exp -> new Production(exp, null, (List<DictionaryEntry>)null, strategy)).collect(Collectors.toList()), words);
	}

	@Test
	public void compoundRuleLongFlag() throws IOException, TimeoutException{
		String language = "xxx";
		File affFile = FileService.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"FLAG long",
			"COMPOUNDMIN 1",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE (aa)?(bb)?(cc)?");
		affParser.parse(affFile);
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser);

		String line = "(aa)?(bb)?(cc)?";
		String[] inputCompounds = new String[]{
			"a/aa",
			"b/bb",
			"c/bbcc"
		};
		List<Production> words = wordGenerator.applyCompoundRules(inputCompounds, line, 37);
		Assert.assertEquals(9, words.size());
		List<String> expected = Arrays.asList("a", "b", "c", "ab", "ac", "bc", "cc", "abc", "acc");
		Assert.assertEquals(expected.stream().map(exp -> new Production(exp, null, (List<DictionaryEntry>)null, strategy)).collect(Collectors.toList()), words);
	}

	@Test
	public void compoundRuleNumericalFlag() throws IOException, TimeoutException{
		String language = "xxx";
		File affFile = FileService.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"FLAG num",
			"COMPOUNDMIN 1",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE (1)?(2)?(3)?");
		affParser.parse(affFile);
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser);

		String line = "(1)?(2)?(3)?";
		String[] inputCompounds = new String[]{
			"a/1",
			"b/2",
			"c/2,3"
		};
		List<Production> words = wordGenerator.applyCompoundRules(inputCompounds, line, 37);
		Assert.assertEquals(9, words.size());
		List<String> expected = Arrays.asList("a", "b", "c", "ab", "ac", "bc", "cc", "abc", "acc");
		Assert.assertEquals(expected.stream().map(exp -> new Production(exp, null, (List<DictionaryEntry>)null, strategy)).collect(Collectors.toList()), words);
	}

}
