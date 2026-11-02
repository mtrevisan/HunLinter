package io.github.mtrevisan.hunlinter.parsers.dictionary;

import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixParser;
import io.github.mtrevisan.hunlinter.parsers.dictionary.generators.WordGenerator;
import io.github.mtrevisan.hunlinter.parsers.vos.RuleEntry;
import io.github.mtrevisan.hunlinter.languages.BaseBuilder;
import io.github.mtrevisan.hunlinter.services.system.FileHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


class RulesReducerUtils{

	static Pair<RulesReducer, WordGenerator> createReducer(File affFile, String language) throws IOException{
		AffixParser affParser = new AffixParser();
		affParser.parse(affFile, language);
		AffixData affixData = affParser.getAffixData();
		File dicFile = FileHelper.createDeleteOnExitFile(language, ".dic",
			"0");
		DictionaryParser dicParser = new DictionaryParser(dicFile, affixData.getLanguage(), affixData.getCharset());
		WordGenerator wordGenerator = new WordGenerator(affixData, dicParser, null);
		RulesReducer reducer = new RulesReducer(affixData, wordGenerator);
		return Pair.of(reducer, wordGenerator);
	}

	static void checkReductionCorrectness(final RulesReducer reducer, final String flag, final List<String> reducedRules,
			final Set<String> originalLines){
		reducer.checkReductionCorrectness(flag, reducedRules, originalLines, null);
	}

	/**
	 * Reduces the plain rules and asserts that
	 * <ul>
	 * 	<li>the result is not longer than the rules in the `.aff` file (for the given flag);</li>
	 * 	<li>each reduced rule is selected by lemmas that are all covered by the original rule (each rule of the result
	 * 		applies only to words that undergo it), and every word that undergoes the original rule is still covered;</li>
	 * 	<li>the reduced rules produce exactly the same inflections of the original ones, for each dictionary line.</li>
	 * </ul>
	 */
	static void assertReduction(final RulesReducer reducer, final File affFile, final String language, final String flag,
			final List<LineEntry> originalRules, final List<String> originalLines) throws IOException{
		AffixParser affParser = new AffixParser();
		affParser.parse(affFile, language);
		RuleEntry originalRule = affParser.getAffixData().getData(flag);
		int originalLength = originalRule.getEntries().size();

		List<LineEntry> reducedRules = reducer.reduceRules(originalRules);

		Set<String> originalWords = new HashSet<>();
		for(LineEntry entry : originalRules)
			originalWords.addAll(entry.from);
		Set<String> coveredWords = new HashSet<>();
		for(LineEntry entry : reducedRules)
			coveredWords.addAll(entry.from);
		Assertions.assertEquals(originalWords, coveredWords);

		List<String> rules = reducer.convertFormat(flag, false, new ArrayList<>(reducedRules));
		//the first line is the header
		Assertions.assertTrue(rules.size() - 1 <= originalLength,
			"Reduced rules (" + (rules.size() - 1) + ") are longer than the original ones (" + originalLength + "): " + rules);

		//the rules go from the shortest added part to the longest one, then they are sorted by the collator of the language
		Comparator<String> comparator = BaseBuilder.getComparator(language);
		String previousAddition = null;
		for(int i = 1; i < rules.size(); i ++){
			String addition = rules.get(i).split("\\s+")[3];
			int slash = addition.indexOf('/');
			addition = (slash >= 0? addition.substring(0, slash): addition);
			addition = (addition.equals("0")? "": addition);
			if(previousAddition != null){
				Assertions.assertTrue(addition.length() >= previousAddition.length(),
					"Rules are not sorted by length of the added part: " + rules);
				if(addition.length() == previousAddition.length())
					Assertions.assertTrue(comparator.compare(previousAddition, addition) <= 0,
						"Rules are not sorted by collator: " + rules);
			}
			previousAddition = addition;
		}

		checkReductionCorrectness(reducer, flag, rules, new HashSet<>(originalLines));
	}

}
