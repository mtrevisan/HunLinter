package unit731.hunlinter.parsers.dictionary.generators;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.FileHelper;


/** @see <a href="https://github.com/hunspell/hunspell/tree/master/tests/v1cmdline">Hunspell tests</a> */
class WordGeneratorCompoundRuleTest extends TestBase{

	@Test
	void testBjörnJacke() throws IOException{
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
		loadData(affFile, language);

		String line = "vw";
		String[] inputCompounds = new String[]{
			"arbeits/v",
			"scheu/Aw",
			"farbig/A"
		};
		List<Production> words = wordGenerator.applyCompoundRules(inputCompounds, line, 10);

		List<Production> expected = Arrays.asList(
			createProduction("arbeitsscheu", "A", "pa:arbeits st:arbeits pa:scheu st:scheu"),
			createProduction("arbeitsscheue", null, "pa:arbeits st:arbeits pa:scheu st:scheu"),
			createProduction("arbeitsscheuer", null, "pa:arbeits st:arbeits pa:scheu st:scheu"),
			createProduction("arbeitsscheuen", null, "pa:arbeits st:arbeits pa:scheu st:scheu"),
			createProduction("arbeitsscheuem", null, "pa:arbeits st:arbeits pa:scheu st:scheu"),
			createProduction("arbeitsscheues", null, "pa:arbeits st:arbeits pa:scheu st:scheu")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	void simple() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPOUNDMIN 1",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE ABC");
		loadData(affFile, language);

		String line = "ABC";
		String[] inputCompounds = new String[]{
			"a/A",
			"b/B",
			"c/BC"
		};
		List<Production> words = wordGenerator.applyCompoundRules(inputCompounds, line, 37);

		List<Production> expected = Arrays.asList(
			createProduction("abc", null, "pa:a st:a pa:b st:b pa:c st:c"),
			createProduction("acc", null, "pa:a st:a pa:c st:c pa:c st:c")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	void infinite() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPOUNDMIN 1",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE A*B*C*");
		loadData(affFile, language);

		String line = "A*B*C*";
		String[] inputCompounds = new String[]{
			"a/A",
			"b/B",
			"c/BC"
		};
		List<Production> words = wordGenerator.applyCompoundRules(inputCompounds, line, 37);

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
		Assertions.assertEquals(expected, words);
	}

	@Test
	void onlyInCompound3() throws IOException{
		String language = "en-GB";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPOUNDMIN 1",
			"ONLYINCOMPOUND _",
			"COMPOUNDRULE 2",
			"COMPOUNDRULE #*0{",
			"COMPOUNDRULE #*@}");
		loadData(affFile, language);

		String line = "#*0{";
		String[] inputCompounds = new String[]{
			"0/#@",
			"0th/}{",
			"1/#0",
			"1st/}",
			"1th/{_",
			"2/#@",
			"2nd/}",
			"2th/{_",
			"3/#@",
			"3rd/}",
			"3th/{_",
			"4/#@",
			"4th/}{",
			"5/#@",
			"5th/}{",
			"6/#@",
			"6th/}{",
			"7/#@",
			"7th/}{",
			"8/#@",
			"8th/}{",
			"9/#@",
			"9th/}{"
		};
		List<Production> words = wordGenerator.applyCompoundRules(inputCompounds, line, 37);

		List<Production> expected = Arrays.asList(
			createProduction("13th", "_", "pa:1 st:1 pa:3th st:3th"),
			createProduction("15th", null, "pa:1 st:1 pa:5th st:5th"),
			createProduction("11th", "_", "pa:1 st:1 pa:1th st:1th"),
			createProduction("18th", null, "pa:1 st:1 pa:8th st:8th"),
			createProduction("10th", null, "pa:1 st:1 pa:0th st:0th"),
			createProduction("12th", "_", "pa:1 st:1 pa:2th st:2th"),
			createProduction("14th", null, "pa:1 st:1 pa:4th st:4th"),
			createProduction("19th", null, "pa:1 st:1 pa:9th st:9th"),
			createProduction("16th", null, "pa:1 st:1 pa:6th st:6th"),
			createProduction("17th", null, "pa:1 st:1 pa:7th st:7th"),
			createProduction("013th", "_", "pa:0 st:0 pa:1 st:1 pa:3th st:3th"),
			createProduction("015th", null, "pa:0 st:0 pa:1 st:1 pa:5th st:5th"),
			createProduction("011th", "_", "pa:0 st:0 pa:1 st:1 pa:1th st:1th"),
			createProduction("018th", null, "pa:0 st:0 pa:1 st:1 pa:8th st:8th"),
			createProduction("010th", null, "pa:0 st:0 pa:1 st:1 pa:0th st:0th"),
			createProduction("012th", "_", "pa:0 st:0 pa:1 st:1 pa:2th st:2th"),
			createProduction("014th", null, "pa:0 st:0 pa:1 st:1 pa:4th st:4th"),
			createProduction("019th", null, "pa:0 st:0 pa:1 st:1 pa:9th st:9th"),
			createProduction("016th", null, "pa:0 st:0 pa:1 st:1 pa:6th st:6th"),
			createProduction("017th", null, "pa:0 st:0 pa:1 st:1 pa:7th st:7th"),
			createProduction("613th", "_", "pa:6 st:6 pa:1 st:1 pa:3th st:3th"),
			createProduction("615th", null, "pa:6 st:6 pa:1 st:1 pa:5th st:5th"),
			createProduction("611th", "_", "pa:6 st:6 pa:1 st:1 pa:1th st:1th"),
			createProduction("618th", null, "pa:6 st:6 pa:1 st:1 pa:8th st:8th"),
			createProduction("610th", null, "pa:6 st:6 pa:1 st:1 pa:0th st:0th"),
			createProduction("612th", "_", "pa:6 st:6 pa:1 st:1 pa:2th st:2th"),
			createProduction("614th", null, "pa:6 st:6 pa:1 st:1 pa:4th st:4th"),
			createProduction("619th", null, "pa:6 st:6 pa:1 st:1 pa:9th st:9th"),
			createProduction("616th", null, "pa:6 st:6 pa:1 st:1 pa:6th st:6th"),
			createProduction("617th", null, "pa:6 st:6 pa:1 st:1 pa:7th st:7th"),
			createProduction("713th", "_", "pa:7 st:7 pa:1 st:1 pa:3th st:3th"),
			createProduction("715th", null, "pa:7 st:7 pa:1 st:1 pa:5th st:5th"),
			createProduction("711th", "_", "pa:7 st:7 pa:1 st:1 pa:1th st:1th"),
			createProduction("718th", null, "pa:7 st:7 pa:1 st:1 pa:8th st:8th"),
			createProduction("710th", null, "pa:7 st:7 pa:1 st:1 pa:0th st:0th"),
			createProduction("712th", "_", "pa:7 st:7 pa:1 st:1 pa:2th st:2th"),
			createProduction("714th", null, "pa:7 st:7 pa:1 st:1 pa:4th st:4th")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	void zeroOrOne() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPOUNDMIN 1",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE A?B?C?");
		loadData(affFile, language);

		String line = "A?B?C?";
		String[] inputCompounds = new String[]{
			"a/A",
			"b/B",
			"c/BC"
		};
		List<Production> words = wordGenerator.applyCompoundRules(inputCompounds, line, 37);

		List<Production> expected = Arrays.asList(
			createProduction("bc", null, "pa:b st:b pa:c st:c"),
			createProduction("cc", null, "pa:c st:c pa:c st:c"),
			createProduction("ac", null, "pa:a st:a pa:c st:c"),
			createProduction("ab", null, "pa:a st:a pa:b st:b"),
			createProduction("abc", null, "pa:a st:a pa:b st:b pa:c st:c"),
			createProduction("acc", null, "pa:a st:a pa:c st:c pa:c st:c")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	void longFlag() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"FLAG long",
			"COMPOUNDMIN 1",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE (aa)?(bb)?(cc)?");
		loadData(affFile, language);

		String line = "(aa)?(bb)?(cc)?";
		String[] inputCompounds = new String[]{
			"a/aa",
			"b/bb",
			"c/bbcc"
		};
		List<Production> words = wordGenerator.applyCompoundRules(inputCompounds, line, 37);

		List<Production> expected = Arrays.asList(
			createProduction("cc", null, "pa:c st:c pa:c st:c"),
			createProduction("bc", null, "pa:b st:b pa:c st:c"),
			createProduction("ac", null, "pa:a st:a pa:c st:c"),
			createProduction("ab", null, "pa:a st:a pa:b st:b"),
			createProduction("acc", null, "pa:a st:a pa:c st:c pa:c st:c"),
			createProduction("abc", null, "pa:a st:a pa:b st:b pa:c st:c")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	void numericalFlag() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"FLAG num",
			"COMPOUNDMIN 1",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE (1)?(2)?(3)?");
		loadData(affFile, language);

		String line = "(1)?(2)?(3)?";
		String[] inputCompounds = new String[]{
			"a/1",
			"b/2",
			"c/2,3"
		};
		List<Production> words = wordGenerator.applyCompoundRules(inputCompounds, line, 37);

		List<Production> expected = Arrays.asList(
			createProduction("bc", null, "pa:b st:b pa:c st:c"),
			createProduction("cc", null, "pa:c st:c pa:c st:c"),
			createProduction("ac", null, "pa:a st:a pa:c st:c"),
			createProduction("ab", null, "pa:a st:a pa:b st:b"),
			createProduction("abc", null, "pa:a st:a pa:b st:b pa:c st:c"),
			createProduction("acc", null, "pa:a st:a pa:c st:c pa:c st:c")
		);
		Assertions.assertEquals(expected, words);
	}


	@Test
	void forbiddenWordMissingRule(){
		Throwable exception = Assertions.assertThrows(LinterException.class, () -> {
			String language = "xxx";
			File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
				"SET UTF-8",
				"COMPOUNDRULE 1",
				"COMPOUNDRULE vw");
			loadData(affFile, language);

			String line = "vw";
			String[] inputCompounds = new String[]{
				"arbeits/v",
				"scheu/v"
			};
			wordGenerator.applyCompoundRules(inputCompounds, line, 5);
		});
		Assertions.assertEquals("Missing word(s) for rule w in compound rule vw", exception.getMessage());
	}

	@Test
	void forbiddenWord() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"FORBIDDENWORD X",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE vw");
		loadData(affFile, language);

		String line = "vw";
		String[] inputCompounds = new String[]{
			"arbeits/v",
			"scheu/wX"
		};
		List<Production> words = wordGenerator.applyCompoundRules(inputCompounds, line, 5);

		Assertions.assertTrue(words.isEmpty());
	}


	@Test
	void forceUppercase() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"FORCEUCASE U",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE vw");
		loadData(affFile, language);

		String line = "vw";
		String[] inputCompounds = new String[]{
			"arbeits/v",
			"scheu/wU"
		};
		List<Production> words = wordGenerator.applyCompoundRules(inputCompounds, line, 5);

		List<Production> expected = Collections.singletonList(
			createProduction("Arbeitsscheu", null, "pa:arbeits st:arbeits pa:scheu st:scheu")
		);
		Assertions.assertEquals(expected, words);
	}

}
