package unit731.hunspeller.parsers.dictionary;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.generators.WordGenerator;
import unit731.hunspeller.parsers.dictionary.vos.AffixEntry;
import unit731.hunspeller.services.FileHelper;


public class RulesReducerTest{

	@Test
	public void simple1() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"LANG vec",
			"FLAG long",
			"SFX ʼ0 Y 11",
			"SFX ʼ0 r ʼ r",
			"SFX ʼ0 u ʼ u",
			"SFX ʼ0 ra ʼ ra",
			"SFX ʼ0 xa ʼ xa",
			"SFX ʼ0 me ʼ me",
			"SFX ʼ0 te ʼ te",
			"SFX ʼ0 do ʼ do",
			"SFX ʼ0 a ʼ [^rx]a",
			"SFX ʼ0 e ʼ [^mtv]e",
			"SFX ʼ0 o ʼ [^d]o",
			"SFX ʼ0 ove óʼ ove");
		Pair<RulesReducer, WordGenerator> pair = createReducer(affFile);
		RulesReducer reducer = pair.getLeft();
		WordGenerator wordGenerator = pair.getRight();
		String flag = "ʼ0";
		List<String> words = Arrays.asList("ge", "la", "na", "nu", "vu", "ge", "sto", "adove", "indove", "kome", "kuando", "tuto", "de", "so", "sora", "tèrŧo",
			"tèrso", "kuarto", "koarto", "kuinto", "sèsto", "par", "kaxa", "sensa", "senŧa", "komòdo", "frate", "nudo");
		List<RulesReducer.LineEntry> plainRules = words.stream()
			.map(word -> wordGenerator.applyAffixRules(word + "/" + flag))
			.map(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX))
			.collect(Collectors.toList());
		List<RulesReducer.LineEntry> compactedRules = reducer.reduceProductions(plainRules);

		List<RulesReducer.LineEntry> expectedCompactedRules = Arrays.asList(
			new RulesReducer.LineEntry("ove", "óʼ", "ove", Arrays.asList("indove", "adove")),
			new RulesReducer.LineEntry("r", "ʼ", "r", "par"),
			new RulesReducer.LineEntry("u", "ʼ", "u", Arrays.asList("nu", "vu")),
			new RulesReducer.LineEntry("ra", "ʼ", "ra", "sora"),
			new RulesReducer.LineEntry("do", "ʼ", "do", Arrays.asList("nudo", "komòdo", "kuando")),
			new RulesReducer.LineEntry("te", "ʼ", "te", "frate"),
			new RulesReducer.LineEntry("xa", "ʼ", "xa", "kaxa"),
			new RulesReducer.LineEntry("me", "ʼ", "me", "kome"),
			new RulesReducer.LineEntry("o", "ʼ", "[^d]o", Arrays.asList("koarto", "kuinto", "kuarto", "sèsto", "tèrso", "tèrŧo", "tuto", "so", "sto")),
			new RulesReducer.LineEntry("e", "ʼ", "[dg]e", Arrays.asList("de", "ge")),
			new RulesReducer.LineEntry("a", "ʼ", "[^rx]a", Arrays.asList("sensa", "senŧa", "na", "la"))
		);
		Assertions.assertEquals(expectedCompactedRules, compactedRules);

		List<String> rules = reducer.convertFormat(flag, false, compactedRules);
		List<String> expectedRules = Arrays.asList(
			"SFX ʼ0 Y 11",
			"SFX ʼ0 r ʼ r",
			"SFX ʼ0 u ʼ u",
			"SFX ʼ0 ra ʼ ra",
			"SFX ʼ0 xa ʼ xa",
			"SFX ʼ0 me ʼ me",
			"SFX ʼ0 te ʼ te",
			"SFX ʼ0 do ʼ do",
			"SFX ʼ0 a ʼ [^rx]a",
			"SFX ʼ0 e ʼ [dg]e",
			"SFX ʼ0 o ʼ [^d]o",
			"SFX ʼ0 ove óʼ ove"
		);
		Assertions.assertEquals(expectedRules, rules);
	}

	@Test
	public void simple2() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"LANG vec",
			"FLAG long",
			"SFX §1 Y 5",
			"SFX §1 0 ta [^ƚ]a",
			"SFX §1 o ato [^ƚ]o",
			"SFX §1 èƚa eƚata èƚa",
			"SFX §1 èƚo eƚato èƚo",
			"SFX §1 o ato [^è]ƚo");
		Pair<RulesReducer, WordGenerator> pair = createReducer(affFile);
		RulesReducer reducer = pair.getLeft();
		WordGenerator wordGenerator = pair.getRight();
		String flag = "§1";
		List<String> words = Arrays.asList("kanèƚo", "kapèƚa", "kapèƚo", "ƚibro", "vedèƚa", "vedèƚo", "moƚo", "rosiñoƚo", "roxiñoƚo", "kaƚandra", "kaƚandro",
			"xeƚo", "rusiñoƚo", "ruxiñoƚo");
		List<RulesReducer.LineEntry> plainRules = words.stream()
			.map(word -> wordGenerator.applyAffixRules(word + "/" + flag))
			.map(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX))
			.collect(Collectors.toList());
		List<RulesReducer.LineEntry> compactedRules = reducer.reduceProductions(plainRules);

		List<RulesReducer.LineEntry> expectedCompactedRules = Arrays.asList(
			new RulesReducer.LineEntry("èƚa", "eƚata", "èƚa", Arrays.asList("kapèƚa", "vedèƚa")),
			new RulesReducer.LineEntry("èƚo", "eƚato", "èƚo", Arrays.asList("kapèƚo", "vedèƚo", "kanèƚo")),
			new RulesReducer.LineEntry("o", "ato", "[^ƚ]o", Arrays.asList("ƚibro", "kaƚandro")),
			new RulesReducer.LineEntry("0", "ta", "ra", "kaƚandra"),
			new RulesReducer.LineEntry("o", "ato", "[^è]ƚo", Arrays.asList("moƚo", "roxiñoƚo", "rosiñoƚo", "xeƚo", "ruxiñoƚo", "rusiñoƚo"))
		);
		Assertions.assertEquals(expectedCompactedRules, compactedRules);

		List<String> rules = reducer.convertFormat(flag, false, compactedRules);
		List<String> expectedRules = Arrays.asList(
			"SFX §1 Y 5",
			"SFX §1 0 ta ra",
			"SFX §1 o ato [^ƚ]o",
			"SFX §1 èƚa eƚata èƚa",
			"SFX §1 èƚo eƚato èƚo",
			"SFX §1 o ato [^è]ƚo"
		);
		Assertions.assertEquals(expectedRules, rules);
	}

	private Pair<RulesReducer, WordGenerator> createReducer(File affFile) throws IOException{
		AffixParser affParser = new AffixParser();
		affParser.parse(affFile);
		AffixData affixData = affParser.getAffixData();
		File dicFile = FileHelper.getTemporaryUTF8File("xxx", ".dic",
			"0");
		DictionaryParser dicParser = new DictionaryParser(dicFile, affixData.getLanguage(), affixData.getCharset());
		WordGenerator wordGenerator = new WordGenerator(affixData, dicParser);
		RulesReducer reducer = new RulesReducer(affixData, wordGenerator);
		return Pair.of(reducer, wordGenerator);
	}

}
