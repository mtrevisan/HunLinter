package io.github.mtrevisan.hunlinter.parsers.dictionary;

import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixParser;
import io.github.mtrevisan.hunlinter.parsers.dictionary.generators.WordGenerator;
import io.github.mtrevisan.hunlinter.services.system.FileHelper;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.util.List;


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
			final List<String> originalLines){
		reducer.checkReductionCorrectness(flag, reducedRules, originalLines, null);
	}

}
