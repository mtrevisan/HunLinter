package unit731.hunlinter.parsers.dictionary.generators;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.affix.AffixParser;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.parsers.workers.exceptions.LinterException;
import unit731.hunlinter.services.FileHelper;


/** @see <a href="https://github.com/hunspell/hunspell/tree/master/tests/v1cmdline">Hunspell tests</a> */
class WordGeneratorCompoundRuleTest{

	private AffixData affixData;
	private WordGenerator wordGenerator;


	@Test
	void testBjörnJacke() throws IOException, SAXException{
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
	void simple() throws IOException, SAXException{
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
	void infinite() throws IOException, SAXException{
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

//	@Test
//	void onlyInCompound3() throws IOException, SAXException{
//		String language = "en-GB";
//		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
//			"SET UTF-8",
//			"COMPOUNDMIN 1",
//			"ONLYINCOMPOUND _",
//			"COMPOUNDRULE 2",
//			"COMPOUNDRULE #*0{",
//			"COMPOUNDRULE #*@}");
//		loadData(affFile, language);
//
//		String line = "#*0{";
//		String[] inputCompounds = new String[]{
//			"0/#@",
//			"0th/}{",
//			"1/#0",
//			"1st/}",
//			"1th/{_",
//			"2/#@",
//			"2nd/}",
//			"2th/{_",
//			"3/#@",
//			"3rd/}",
//			"3th/{_",
//			"4/#@",
//			"4th/}{",
//			"5/#@",
//			"5th/}{",
//			"6/#@",
//			"6th/}{",
//			"7/#@",
//			"7th/}{",
//			"8/#@",
//			"8th/}{",
//			"9/#@",
//			"9th/}{"
//		};
//		List<Production> words = wordGenerator.applyCompoundRules(inputCompounds, line, 37);
//
//		List<Production> expected = Arrays.asList(
//			createProduction("10th", null, "pa: 0 pa:{"),
//			createProduction("11th", null, "pa: 0 pa:{"),
//			createProduction("12th", null, "pa: 0 pa:{"),
//			createProduction("13th", null, "pa: 0 pa:{"),
//			createProduction("14th", null, "pa: 0 pa:{"),
//			createProduction("15th", null, "pa: 0 pa:{"),
//			createProduction("16th", null, "pa: 0 pa:{"),
//			createProduction("17th", null, "pa: 0 pa:{"),
//			createProduction("18th", null, "pa: 0 pa:{"),
//			createProduction("19th", null, "pa: 0 pa:{")
//		);
//		Assertions.assertEquals(expected, words);
//	}

	@Test
	void zeroOrOne() throws IOException, SAXException{
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
	void longFlag() throws IOException, SAXException{
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
	void numericalFlag() throws IOException, SAXException{
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
	void forbiddenWord() throws IOException, SAXException{
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
	void forceUppercase() throws IOException, SAXException{
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
//words.forEach(System.out::println);

		List<Production> expected = Collections.singletonList(
			createProduction("Arbeitsscheu", null, "pa:arbeits st:arbeits pa:scheu st:scheu")
		);
		Assertions.assertEquals(expected, words);
	}

	private void loadData(File affFile, String language) throws IOException, SAXException{
		AffixParser affParser = new AffixParser();
		affParser.parse(affFile, language);
		affixData = affParser.getAffixData();
		wordGenerator = new WordGenerator(affixData, null);
	}

	private Production createProduction(String word, String continuationFlags, String morphologicalFields){
		FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();
		return new Production(word, continuationFlags, morphologicalFields, null, strategy);
	}

}
