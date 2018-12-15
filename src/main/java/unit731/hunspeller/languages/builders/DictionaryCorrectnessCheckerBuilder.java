package unit731.hunspeller.languages.builders;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import unit731.hunspeller.languages.DictionaryCorrectnessChecker;
import unit731.hunspeller.languages.vec.DictionaryCorrectnessCheckerVEC;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.hyphenation.hyphenators.HyphenatorInterface;


public class DictionaryCorrectnessCheckerBuilder{

	private static final BiFunction<AffixParser, HyphenatorInterface, DictionaryCorrectnessChecker> DEFAULT_CHECKER = (affParser, hyphenator)
		-> new DictionaryCorrectnessChecker(affParser, hyphenator);

	private static final Map<String, BiFunction<AffixParser, HyphenatorInterface, DictionaryCorrectnessChecker>> CHECKERS = new HashMap<>();
	static{
		CHECKERS.put(DictionaryCorrectnessCheckerVEC.LANGUAGE, (affParser, hyphenator) -> new DictionaryCorrectnessCheckerVEC(affParser, hyphenator));
	}


	private DictionaryCorrectnessCheckerBuilder(){}

	public static DictionaryCorrectnessChecker getCorrectnessChecker(AffixParser affParser, HyphenatorInterface hyphenator) throws IOException{
		DictionaryCorrectnessChecker checker = CHECKERS.getOrDefault(affParser.getLanguage(), DEFAULT_CHECKER)
			.apply(affParser, hyphenator);
		checker.loadRules();
		return checker;
	}

}
