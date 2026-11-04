package io.github.mtrevisan.hunlinter.parsers.dictionary;

import io.github.mtrevisan.hunlinter.parsers.dictionary.generators.WordGenerator;
import io.github.mtrevisan.hunlinter.parsers.enums.AffixType;
import io.github.mtrevisan.hunlinter.services.system.FileHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Several conditions of the same transformation that select all and only the words selected by a single condition are
 * replaced by it, without accepting characters (or a number of characters) that the original conditions do not accept.
 */
@SuppressWarnings("ALL")
class RulesReducerMergeTest{

	private static final String LANGUAGE = "vec-IT";
	private static final String FLAG = "A0";


	/**
	 * <pre>
	 * SFX A0 e 0 [aeèinòru]ce
	 * SFX A0 e 0 [aeèoòru]ñe
	 * SFX A0 e 0 [aeèioòu]re
	 * </pre>
	 * select the same words of <code>[aeèinoòru][cñr]e</code> (the words `bce`, `bñe` and `bre` carry the flag but undergo
	 * nothing, and are not selected by it).
	 */
	@Test
	void threeConditionsDifferingInTwoTokens() throws IOException{
		File affFile = FileHelper.createDeleteOnExitFile(LANGUAGE, ".aff",
			"SET UTF-8",
			"LANG " + LANGUAGE,
			"FLAG long",
			"SFX " + FLAG + " Y 3",
			"SFX " + FLAG + " e 0 [aeèinòru]ce",
			"SFX " + FLAG + " e 0 [aeèoòru]ñe",
			"SFX " + FLAG + " e 0 [aeèioòu]re"
		);
		List<String> words = new ArrayList<>();
		for(String chr : "a e è i n ò r u".split(" "))
			words.add(chr + "ce");
		for(String chr : "a e è o ò r u".split(" "))
			words.add(chr + "ñe");
		for(String chr : "a e è i o ò u".split(" "))
			words.add(chr + "re");
		List<String> inertWords = Arrays.asList("bce", "bñe", "bre");

		List<String> rules = reduce(affFile, words, inertWords);

		Assertions.assertEquals(Arrays.asList(
			"SFX " + FLAG + " Y 1",
			"SFX " + FLAG + " e 0 [aeèinoòru][cñr]e"
		), rules);
	}

	/**
	 * <pre>
	 * SFX A0 o 0 [^bfknpx][dlmnñ]o
	 * SFX A0 o 0 .ndo
	 * </pre>
	 * the second one is the first one with the `n` (excluded by the first one) before the `d`, and requires a character more
	 * than the first one: they are replaced by <code>[^bfkpx][dlmnñ]o</code>.
	 */
	@Test
	void conditionsOfDifferentLength() throws IOException{
		File affFile = FileHelper.createDeleteOnExitFile(LANGUAGE, ".aff",
			"SET UTF-8",
			"LANG " + LANGUAGE,
			"FLAG long",
			"SFX " + FLAG + " Y 2",
			"SFX " + FLAG + " o 0 [^bfknpx][dlmnñ]o",
			"SFX " + FLAG + " o 0 .ndo"
		);
		List<String> words = Arrays.asList("ado", "edo", "ido", "odo", "udo", "rdo", "alo", "amo", "ano", "año", "ando", "endo",
			"ondo");
		List<String> inertWords = Arrays.asList("bdo", "fdo", "kdo", "pdo", "xdo");

		List<String> rules = reduce(affFile, words, inertWords);

		Assertions.assertEquals(Arrays.asList(
			"SFX " + FLAG + " Y 1",
			"SFX " + FLAG + " o 0 [^bfkpx][dlmnñ]o"
		), rules);
	}


	/**
	 * <pre>
	 * SFX A0 e 0 ace
	 * SFX A0 e 0 bde
	 * </pre>
	 * select the same words of <code>[ab][cd]e</code> (the words `ade` and `bce` do not exist, and the words `kce` and `kde`
	 * undergo nothing, and are not selected by it).
	 * <p>
	 * Neither condition can be widened by itself (a token is widened only to the characters of the words that exist, there are
	 * no `ade` and `bce`), and they differ in both tokens, so only the merge of the two selected conditions can find it.
	 */
	@Test
	void conditionsNotReachableByWidening() throws IOException{
		File affFile = FileHelper.createDeleteOnExitFile(LANGUAGE, ".aff",
			"SET UTF-8",
			"LANG " + LANGUAGE,
			"FLAG long",
			"SFX " + FLAG + " Y 2",
			"SFX " + FLAG + " e 0 ace",
			"SFX " + FLAG + " e 0 bde"
		);
		List<String> words = Arrays.asList("ace", "bde");
		List<String> inertWords = Arrays.asList("kce", "kde");

		List<String> rules = reduce(affFile, words, inertWords);

		Assertions.assertEquals(Arrays.asList(
			"SFX " + FLAG + " Y 1",
			"SFX " + FLAG + " e 0 [ab][cd]e"
		), rules);
	}


	/**
	 * Reduces the rules of the flag (as the worker does, so the conditions are kept within the original ones), and verifies
	 * that the reduced rules give back the same inflections.
	 *
	 * @param inertWords	Words that carry the flag but undergo no transformation (they are not dictionary lines, as the generator refuses a flag that cannot be applied).
	 */
	private static List<String> reduce(File affFile, List<String> words, List<String> inertWords) throws IOException{
		Pair<RulesReducer, WordGenerator> pair = RulesReducerUtils.createReducer(affFile, LANGUAGE);
		RulesReducer reducer = pair.getLeft();
		WordGenerator wordGenerator = pair.getRight();

		List<String> originalLines = new ArrayList<>();
		for(String word : words)
			originalLines.add(word + "/" + FLAG);
		List<LineEntry> originalRules = originalLines.stream()
			.map(wordGenerator::createFromDictionaryLine)
			.map(wordGenerator::applyAffixRules)
			.map(inflections -> reducer.collectInflectionsByFlag(inflections, FLAG, AffixType.SUFFIX))
			.flatMap(List::stream)
			.toList();

		List<LineEntry> reducedRules = reducer.reduceRules(originalRules, new HashSet<>(inertWords), FLAG, null);
		List<String> rules = reducer.convertFormat(FLAG, false, new ArrayList<>(reducedRules));

		RulesReducerUtils.checkReductionCorrectness(reducer, FLAG, rules, new HashSet<>(originalLines));
		return rules;
	}

}
