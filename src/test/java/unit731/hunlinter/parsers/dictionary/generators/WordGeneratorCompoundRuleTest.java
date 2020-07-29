/**
 * Copyright (c) 2019-2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package unit731.hunlinter.parsers.dictionary.generators;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.services.system.FileHelper;
import unit731.hunlinter.workers.exceptions.LinterException;

import java.io.File;
import java.io.IOException;


/** @see <a href="https://github.com/hunspell/hunspell/tree/master/tests/v1cmdline">Hunspell tests</a> */
class WordGeneratorCompoundRuleTest extends TestBase{

	@Test
	void björnJacke() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
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
		Inflection[] words = wordGenerator.applyCompoundRules(inputCompounds, line, 10);

		Inflection[] expected = new Inflection[]{
			createInflection("arbeitsscheu", "A", "pa:arbeits st:arbeits pa:scheu st:scheu"),
			createInflection("arbeitsscheue", null, "pa:arbeits st:arbeits pa:scheu st:scheu"),
			createInflection("arbeitsscheuer", null, "pa:arbeits st:arbeits pa:scheu st:scheu"),
			createInflection("arbeitsscheuen", null, "pa:arbeits st:arbeits pa:scheu st:scheu"),
			createInflection("arbeitsscheuem", null, "pa:arbeits st:arbeits pa:scheu st:scheu"),
			createInflection("arbeitsscheues", null, "pa:arbeits st:arbeits pa:scheu st:scheu")
		};
		Assertions.assertArrayEquals(expected, words);
	}

	@Test
	void simple() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
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
		Inflection[] words = wordGenerator.applyCompoundRules(inputCompounds, line, 37);

		Inflection[] expected = new Inflection[]{
			createInflection("abc", null, "pa:a st:a pa:b st:b pa:c st:c"),
			createInflection("acc", null, "pa:a st:a pa:c st:c pa:c st:c")
		};
		Assertions.assertArrayEquals(expected, words);
	}

	@Test
	void infinite() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
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
		Inflection[] words = wordGenerator.applyCompoundRules(inputCompounds, line, 37);

		Inflection[] expected = new Inflection[]{
			createInflection("aa", null, "pa:a st:a pa:a st:a"),
			createInflection("ab", null, "pa:a st:a pa:b st:b"),
			createInflection("ac", null, "pa:a st:a pa:c st:c"),
			createInflection("bb", null, "pa:b st:b pa:b st:b"),
			createInflection("bc", null, "pa:b st:b pa:c st:c"),
			createInflection("cb", null, "pa:c st:c pa:b st:b"),
			createInflection("cc", null, "pa:c st:c pa:c st:c"),
			createInflection("aaa", null, "pa:a st:a pa:a st:a pa:a st:a"),
			createInflection("aab", null, "pa:a st:a pa:a st:a pa:b st:b"),
			createInflection("aac", null, "pa:a st:a pa:a st:a pa:c st:c"),
			createInflection("abb", null, "pa:a st:a pa:b st:b pa:b st:b"),
			createInflection("abc", null, "pa:a st:a pa:b st:b pa:c st:c"),
			createInflection("acb", null, "pa:a st:a pa:c st:c pa:b st:b"),
			createInflection("acc", null, "pa:a st:a pa:c st:c pa:c st:c"),
			createInflection("bbb", null, "pa:b st:b pa:b st:b pa:b st:b"),
			createInflection("bbc", null, "pa:b st:b pa:b st:b pa:c st:c"),
			createInflection("bcb", null, "pa:b st:b pa:c st:c pa:b st:b"),
			createInflection("bcc", null, "pa:b st:b pa:c st:c pa:c st:c"),
			createInflection("cbb", null, "pa:c st:c pa:b st:b pa:b st:b"),
			createInflection("cbc", null, "pa:c st:c pa:b st:b pa:c st:c"),
			createInflection("ccb", null, "pa:c st:c pa:c st:c pa:b st:b"),
			createInflection("ccc", null, "pa:c st:c pa:c st:c pa:c st:c"),
			createInflection("aaaa", null, "pa:a st:a pa:a st:a pa:a st:a pa:a st:a"),
			createInflection("aaab", null, "pa:a st:a pa:a st:a pa:a st:a pa:b st:b"),
			createInflection("aaac", null, "pa:a st:a pa:a st:a pa:a st:a pa:c st:c"),
			createInflection("aabb", null, "pa:a st:a pa:a st:a pa:b st:b pa:b st:b"),
			createInflection("aabc", null, "pa:a st:a pa:a st:a pa:b st:b pa:c st:c"),
			createInflection("aacb", null, "pa:a st:a pa:a st:a pa:c st:c pa:b st:b"),
			createInflection("aacc", null, "pa:a st:a pa:a st:a pa:c st:c pa:c st:c"),
			createInflection("abbb", null, "pa:a st:a pa:b st:b pa:b st:b pa:b st:b"),
			createInflection("abbc", null, "pa:a st:a pa:b st:b pa:b st:b pa:c st:c"),
			createInflection("abcb", null, "pa:a st:a pa:b st:b pa:c st:c pa:b st:b"),
			createInflection("abcc", null, "pa:a st:a pa:b st:b pa:c st:c pa:c st:c"),
			createInflection("acbb", null, "pa:a st:a pa:c st:c pa:b st:b pa:b st:b"),
			createInflection("acbc", null, "pa:a st:a pa:c st:c pa:b st:b pa:c st:c"),
			createInflection("accb", null, "pa:a st:a pa:c st:c pa:c st:c pa:b st:b"),
			createInflection("accc", null, "pa:a st:a pa:c st:c pa:c st:c pa:c st:c")
		};
		Assertions.assertArrayEquals(expected, words);
	}

	@Test
	void onlyInCompound3() throws IOException{
		String language = "en-GB";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
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
		Inflection[] words = wordGenerator.applyCompoundRules(inputCompounds, line, 37);

		Inflection[] expected = new Inflection[]{
			createInflection("10th", null, "pa:1 st:1 pa:0th st:0th"),
			createInflection("11th", "_", "pa:1 st:1 pa:1th st:1th"),
			createInflection("12th", "_", "pa:1 st:1 pa:2th st:2th"),
			createInflection("13th", "_", "pa:1 st:1 pa:3th st:3th"),
			createInflection("14th", null, "pa:1 st:1 pa:4th st:4th"),
			createInflection("15th", null, "pa:1 st:1 pa:5th st:5th"),
			createInflection("16th", null, "pa:1 st:1 pa:6th st:6th"),
			createInflection("17th", null, "pa:1 st:1 pa:7th st:7th"),
			createInflection("18th", null, "pa:1 st:1 pa:8th st:8th"),
			createInflection("19th", null, "pa:1 st:1 pa:9th st:9th"),
			createInflection("010th", null, "pa:0 st:0 pa:1 st:1 pa:0th st:0th"),
			createInflection("011th", "_", "pa:0 st:0 pa:1 st:1 pa:1th st:1th"),
			createInflection("012th", "_", "pa:0 st:0 pa:1 st:1 pa:2th st:2th"),
			createInflection("013th", "_", "pa:0 st:0 pa:1 st:1 pa:3th st:3th"),
			createInflection("014th", null, "pa:0 st:0 pa:1 st:1 pa:4th st:4th"),
			createInflection("015th", null, "pa:0 st:0 pa:1 st:1 pa:5th st:5th"),
			createInflection("016th", null, "pa:0 st:0 pa:1 st:1 pa:6th st:6th"),
			createInflection("017th", null, "pa:0 st:0 pa:1 st:1 pa:7th st:7th"),
			createInflection("018th", null, "pa:0 st:0 pa:1 st:1 pa:8th st:8th"),
			createInflection("019th", null, "pa:0 st:0 pa:1 st:1 pa:9th st:9th"),
			createInflection("110th", null, "pa:1 st:1 pa:1 st:1 pa:0th st:0th"),
			createInflection("111th", "_", "pa:1 st:1 pa:1 st:1 pa:1th st:1th"),
			createInflection("112th", "_", "pa:1 st:1 pa:1 st:1 pa:2th st:2th"),
			createInflection("113th", "_", "pa:1 st:1 pa:1 st:1 pa:3th st:3th"),
			createInflection("114th", null, "pa:1 st:1 pa:1 st:1 pa:4th st:4th"),
			createInflection("115th", null, "pa:1 st:1 pa:1 st:1 pa:5th st:5th"),
			createInflection("116th", null, "pa:1 st:1 pa:1 st:1 pa:6th st:6th"),
			createInflection("117th", null, "pa:1 st:1 pa:1 st:1 pa:7th st:7th"),
			createInflection("118th", null, "pa:1 st:1 pa:1 st:1 pa:8th st:8th"),
			createInflection("119th", null, "pa:1 st:1 pa:1 st:1 pa:9th st:9th"),
			createInflection("210th", null, "pa:2 st:2 pa:1 st:1 pa:0th st:0th"),
			createInflection("211th", "_", "pa:2 st:2 pa:1 st:1 pa:1th st:1th"),
			createInflection("212th", "_", "pa:2 st:2 pa:1 st:1 pa:2th st:2th"),
			createInflection("213th", "_", "pa:2 st:2 pa:1 st:1 pa:3th st:3th"),
			createInflection("214th", null, "pa:2 st:2 pa:1 st:1 pa:4th st:4th"),
			createInflection("215th", null, "pa:2 st:2 pa:1 st:1 pa:5th st:5th"),
			createInflection("216th", null, "pa:2 st:2 pa:1 st:1 pa:6th st:6th")
		};
		Assertions.assertArrayEquals(expected, words);
	}

	@Test
	void zeroOrOne() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
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
		Inflection[] words = wordGenerator.applyCompoundRules(inputCompounds, line, 37);

		Inflection[] expected = new Inflection[]{
			createInflection("bc", null, "pa:b st:b pa:c st:c"),
			createInflection("cc", null, "pa:c st:c pa:c st:c"),
			createInflection("ac", null, "pa:a st:a pa:c st:c"),
			createInflection("ab", null, "pa:a st:a pa:b st:b"),
			createInflection("abc", null, "pa:a st:a pa:b st:b pa:c st:c"),
			createInflection("acc", null, "pa:a st:a pa:c st:c pa:c st:c")
		};
		Assertions.assertArrayEquals(expected, words);
	}

	@Test
	void longFlag() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
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
		Inflection[] words = wordGenerator.applyCompoundRules(inputCompounds, line, 37);

		Inflection[] expected = new Inflection[]{
			createInflection("bc", null, "pa:b st:b pa:c st:c"),
			createInflection("cc", null, "pa:c st:c pa:c st:c"),
			createInflection("ac", null, "pa:a st:a pa:c st:c"),
			createInflection("ab", null, "pa:a st:a pa:b st:b"),
			createInflection("abc", null, "pa:a st:a pa:b st:b pa:c st:c"),
			createInflection("acc", null, "pa:a st:a pa:c st:c pa:c st:c")
		};
		Assertions.assertArrayEquals(expected, words);
	}

	@Test
	void numericalFlag() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
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
		Inflection[] words = wordGenerator.applyCompoundRules(inputCompounds, line, 37);

		Inflection[] expected = new Inflection[]{
			createInflection("bc", null, "pa:b st:b pa:c st:c"),
			createInflection("cc", null, "pa:c st:c pa:c st:c"),
			createInflection("ac", null, "pa:a st:a pa:c st:c"),
			createInflection("ab", null, "pa:a st:a pa:b st:b"),
			createInflection("abc", null, "pa:a st:a pa:b st:b pa:c st:c"),
			createInflection("acc", null, "pa:a st:a pa:c st:c pa:c st:c")
		};
		Assertions.assertArrayEquals(expected, words);
	}


	@Test
	void forbiddenWordMissingRule(){
		Throwable exception = Assertions.assertThrows(LinterException.class, () -> {
			String language = "xxx";
			File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
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
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
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
		Inflection[] words = wordGenerator.applyCompoundRules(inputCompounds, line, 5);

		Assertions.assertTrue(words.length == 0);
	}


	@Test
	void forceUppercase() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.createDeleteOnExitFile(language, ".aff",
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
		Inflection[] words = wordGenerator.applyCompoundRules(inputCompounds, line, 5);

		Inflection[] expected = new Inflection[]{
			createInflection("Arbeitsscheu", null, "pa:arbeits st:arbeits pa:scheu st:scheu")
		};
		Assertions.assertArrayEquals(expected, words);
	}

}
