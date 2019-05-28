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
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<RulesReducer.LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.map(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX))
			.collect(Collectors.toList());
		List<RulesReducer.LineEntry> compactedRules = reducer.reduceProductions(originalRules);

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

		reducer.checkReductionCorrectness(flag, rules, originalRules, originalLines);
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
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<RulesReducer.LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.map(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX))
			.collect(Collectors.toList());
		List<RulesReducer.LineEntry> compactedRules = reducer.reduceProductions(originalRules);

		List<RulesReducer.LineEntry> expectedCompactedRules = Arrays.asList(
			new RulesReducer.LineEntry("èƚa", "eƚata", "èƚa", Arrays.asList("kapèƚa", "vedèƚa")),
			new RulesReducer.LineEntry("èƚo", "eƚato", "èƚo", Arrays.asList("kapèƚo", "vedèƚo", "kanèƚo")),
			new RulesReducer.LineEntry("o", "ato", "[^ƚ]o", Arrays.asList("ƚibro", "kaƚandro")),
			new RulesReducer.LineEntry("0", "ta", "[^ƚ]a", "kaƚandra"),
			new RulesReducer.LineEntry("o", "ato", "[^è]ƚo", Arrays.asList("moƚo", "roxiñoƚo", "rosiñoƚo", "xeƚo", "ruxiñoƚo", "rusiñoƚo"))
		);
		Assertions.assertEquals(expectedCompactedRules, compactedRules);

		List<String> rules = reducer.convertFormat(flag, false, compactedRules);
		List<String> expectedRules = Arrays.asList(
			"SFX §1 Y 5",
			"SFX §1 0 ta [^ƚ]a",
			"SFX §1 o ato [^ƚ]o",
			"SFX §1 èƚa eƚata èƚa",
			"SFX §1 èƚo eƚato èƚo",
			"SFX §1 o ato [^è]ƚo"
		);
		Assertions.assertEquals(expectedRules, rules);

		reducer.checkReductionCorrectness(flag, rules, originalRules, originalLines);
	}

	@Test
	public void simple3() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"LANG vec",
			"FLAG long",
			"FULLSTRIP",
			"SFX §0 Y 17",
			"SFX §0 0 ato [nr]",
			"SFX §0 èl elato èl",
			"SFX §0 0 ta [^bklns]a",
			"SFX §0 0 ato [^è]l",
			"SFX §0 o ato [^bkmv]o",
			"SFX §0 òba obata òba",
			"SFX §0 òka okata òka",
			"SFX §0 èla elata èla",
			"SFX §0 òna onata òna",
			"SFX §0 òsa osata òsa",
			"SFX §0 òbo obato òbo",
			"SFX §0 òko okato òko",
			"SFX §0 òmo omato òmo",
			"SFX §0 òvo ovato òvo",
			"SFX §0 0 ta [^è]la",
			"SFX §0 o ato [^ò]ko",
			"SFX §0 0 ta [^ò][kns]a"
		);
		Pair<RulesReducer, WordGenerator> pair = createReducer(affFile);
		RulesReducer reducer = pair.getLeft();
		WordGenerator wordGenerator = pair.getRight();
		String flag = "§0";
		List<String> words = Arrays.asList("aria", "bar", "baron", "bon", "borso", "bosko", "dixnar", "dòna", "fakin", "far", "fator", "fatora", "grada", "granfo",
			"gòba", "gòbo", "inkuixitor", "inkuixitora", "inspetor", "inspetora", "kalandra", "kalandro", "kanèl", "kapèl", "kapèla", "kara", "kojon", "konto",
			"kora", "koɉon", "kuadra", "kuadro", "kòsa", "libro", "maca", "mando", "manxo", "manđo", "marenda", "merenda", "mol", "muso", "padron", "paron",
			"patron", "pecenin", "pesenin", "peŧenin", "porko", "pòka", "pòko", "rexon", "rosiñol", "roxiñol", "rusiñol", "ruxiñol", "ròba", "savia", "savio", "sen",
			"sinsin", "soko", "solfro", "sorgo", "speso", "sporko", "tabar", "toxa", "vedèl", "vedèla", "verdo", "vesin", "vexin", "vexo", "veŧin", "visio", "viŧio",
			"xbir", "xel", "òka", "òko", "òmo", "òvo", "đeneral", "đilio", "ŧedro", "ŧinŧin", "ŧoko");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<RulesReducer.LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.map(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX))
			.collect(Collectors.toList());
		List<RulesReducer.LineEntry> compactedRules = reducer.reduceProductions(originalRules);

		List<RulesReducer.LineEntry> expectedCompactedRules = Arrays.asList(
			new RulesReducer.LineEntry("òbo", "obato", "òbo", "gòbo"),
			new RulesReducer.LineEntry("òba", "obata", "òba", Arrays.asList("gòba", "ròba")),
			new RulesReducer.LineEntry("òko", "okato", "òko", Arrays.asList("pòko", "òko")),
			new RulesReducer.LineEntry("èla", "elata", "èla", Arrays.asList("kapèla", "vedèla")),
			new RulesReducer.LineEntry("òna", "onata", "òna", "dòna"),
			new RulesReducer.LineEntry("òmo", "omato", "òmo", "òmo"),
			new RulesReducer.LineEntry("òsa", "osata", "òsa", "kòsa"),
			new RulesReducer.LineEntry("èl", "elato", "èl", Arrays.asList("vedèl", "kanèl", "kapèl")),
			new RulesReducer.LineEntry("òka", "okata", "òka", Arrays.asList("òka", "pòka")),
			new RulesReducer.LineEntry("òvo", "ovato", "òvo", "òvo"),
			new RulesReducer.LineEntry("0", "ato", "[nr]", Arrays.asList("bon", "dixnar", "veŧin", "bar", "far", "tabar", "paron", "koɉon", "ŧinŧin", "inkuixitor",
				"sen", "baron", "vexin", "patron", "peŧenin", "vesin", "pecenin", "xbir", "kojon", "rexon", "inspetor", "fator", "sinsin", "padron", "pesenin", "fakin")),
			new RulesReducer.LineEntry("o", "ato", "[^bkmv]o", Arrays.asList("verdo", "libro", "đilio", "mando", "viŧio", "savio", "speso", "kalandro", "vexo",
				"ŧedro", "konto", "manđo", "granfo", "sorgo", "visio", "muso", "borso", "manxo", "kuadro", "solfro")),
			new RulesReducer.LineEntry("0", "ato", "[^è]l", Arrays.asList("rusiñol", "ruxiñol", "rosiñol", "mol", "đeneral", "roxiñol", "xel")),
			new RulesReducer.LineEntry("0", "ta", "[^bklns]a", Arrays.asList("kalandra", "kora", "maca", "savia", "aria", "inkuixitora", "marenda", "kuadra",
				"inspetora", "toxa", "grada", "merenda", "kara", "fatora")),
			new RulesReducer.LineEntry("o", "ato", "[^ò]ko", Arrays.asList("bosko", "soko", "ŧoko", "porko", "sporko"))
		);
		Assertions.assertEquals(expectedCompactedRules, compactedRules);

		List<String> rules = reducer.convertFormat(flag, false, compactedRules);
		List<String> expectedRules = Arrays.asList(
			"SFX §0 Y 15",
			"SFX §0 0 ato [nr]",
			"SFX §0 èl elato èl",
			"SFX §0 0 ta [^bklns]a",
			"SFX §0 0 ato [^è]l",
			"SFX §0 o ato [^bkmv]o",
			"SFX §0 òba obata òba",
			"SFX §0 òka okata òka",
			"SFX §0 èla elata èla",
			"SFX §0 òna onata òna",
			"SFX §0 òsa osata òsa",
			"SFX §0 òbo obato òbo",
			"SFX §0 òko okato òko",
			"SFX §0 òmo omato òmo",
			"SFX §0 òvo ovato òvo",
			"SFX §0 o ato [^ò]ko"
		);
		Assertions.assertEquals(expectedRules, rules);

		reducer.checkReductionCorrectness(flag, rules, originalRules, originalLines);
	}

	@Test
	public void simple4() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"LANG vec",
			"FLAG long",
			"FULLSTRIP",
			"SFX §0 Y 17",
			"SFX §0 0 ato [nr]",
			"SFX §0 èl elato èl",
			"SFX §0 0 ta [^bklns]a",
			"SFX §0 0 ato [^è]l",
			"SFX §0 o ato [^bkmv]o",
			"SFX §0 òba obata òba",
			"SFX §0 òka okata òka",
			"SFX §0 èla elata èla",
			"SFX §0 òna onata òna",
			"SFX §0 òsa osata òsa",
			"SFX §0 òbo obato òbo",
			"SFX §0 òko okato òko",
			"SFX §0 òmo omato òmo",
			"SFX §0 òvo ovato òvo",
			"SFX §0 0 ta [^è]la",
			"SFX §0 o ato [^ò]ko",
			"SFX §0 0 ta [^ò][kns]a"
		);
		Pair<RulesReducer, WordGenerator> pair = createReducer(affFile);
		RulesReducer reducer = pair.getLeft();
		WordGenerator wordGenerator = pair.getRight();
		String flag = "§0";
		List<String> words = Arrays.asList("aria", "bar", "baron", "bon", "borso", "bosko", "dixnar", "dòna", "fakin", "far", "fator", "fatora", "grada", "granfo",
			"gòba", "gòbo", "inkuixitor", "inkuixitora", "inspetor", "inspetora", "kalandra", "kalandro", "kanèl", "kapèl", "kapèla", "kara", "kojon", "konto",
			"kora", "koɉon", "kuadra", "kuadro", "kòsa", "libro", "maca", "mando", "manxo", "manđo", "marenda", "merenda", "mol", "muso", "padron", "paron",
			"patron", "pecenin", "pesenin", "peŧenin", "porko", "pòka", "pòko", "rexon", "rosiñol", "roxiñol", "rusiñol", "ruxiñol", "ròba", "savia", "savio", "sen",
			"sinsin", "soko", "solfro", "sorgo", "speso", "sporko", "tabar", "toxa", "vedèl", "vedèla", "verdo", "vesin", "vexin", "vexo", "veŧin", "visio", "viŧio",
			"xbir", "xel", "òka", "òko", "òmo", "òvo", "đeneral", "đilio", "ŧedro", "ŧinŧin", "ŧoko");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<RulesReducer.LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.map(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX))
			.collect(Collectors.toList());
		List<RulesReducer.LineEntry> compactedRules = reducer.reduceProductions(originalRules);

		List<RulesReducer.LineEntry> expectedCompactedRules = Arrays.asList(
			new RulesReducer.LineEntry("òbo", "obato", "òbo", "gòbo"),
			new RulesReducer.LineEntry("òba", "obata", "òba", Arrays.asList("gòba", "ròba")),
			new RulesReducer.LineEntry("òko", "okato", "òko", Arrays.asList("pòko", "òko")),
			new RulesReducer.LineEntry("èla", "elata", "èla", Arrays.asList("kapèla", "vedèla")),
			new RulesReducer.LineEntry("òna", "onata", "òna", "dòna"),
			new RulesReducer.LineEntry("òmo", "omato", "òmo", "òmo"),
			new RulesReducer.LineEntry("òsa", "osata", "òsa", "kòsa"),
			new RulesReducer.LineEntry("èl", "elato", "èl", Arrays.asList("vedèl", "kanèl", "kapèl")),
			new RulesReducer.LineEntry("òka", "okata", "òka", Arrays.asList("òka", "pòka")),
			new RulesReducer.LineEntry("òvo", "ovato", "òvo", "òvo"),
			new RulesReducer.LineEntry("0", "ato", "[nr]", Arrays.asList("bon", "dixnar", "veŧin", "bar", "far", "tabar", "paron", "koɉon", "ŧinŧin", "inkuixitor",
				"sen", "baron", "vexin", "patron", "peŧenin", "vesin", "pecenin", "xbir", "kojon", "rexon", "inspetor", "fator", "sinsin", "padron", "pesenin", "fakin")),
			new RulesReducer.LineEntry("o", "ato", "[^bkmv]o", Arrays.asList("verdo", "libro", "đilio", "mando", "viŧio", "savio", "speso", "kalandro", "vexo",
				"ŧedro", "konto", "manđo", "granfo", "sorgo", "visio", "muso", "borso", "manxo", "kuadro", "solfro")),
			new RulesReducer.LineEntry("0", "ato", "[^è]l", Arrays.asList("rusiñol", "ruxiñol", "rosiñol", "mol", "đeneral", "roxiñol", "xel")),
			new RulesReducer.LineEntry("0", "ta", "[^bklns]a", Arrays.asList("kalandra", "kora", "maca", "savia", "aria", "inkuixitora", "marenda", "kuadra",
				"inspetora", "toxa", "grada", "merenda", "kara", "fatora")),
			new RulesReducer.LineEntry("o", "ato", "[^ò]ko", Arrays.asList("bosko", "soko", "ŧoko", "porko", "sporko"))
		);
		Assertions.assertEquals(expectedCompactedRules, compactedRules);

		List<String> rules = reducer.convertFormat(flag, false, compactedRules);
		List<String> expectedRules = Arrays.asList(
			"SFX §0 Y 15",
			"SFX §0 0 ato [nr]",
			"SFX §0 èl elato èl",
			"SFX §0 0 ta [^bklns]a",
			"SFX §0 0 ato [^è]l",
			"SFX §0 o ato [^bkmv]o",
			"SFX §0 òba obata òba",
			"SFX §0 òka okata òka",
			"SFX §0 èla elata èla",
			"SFX §0 òna onata òna",
			"SFX §0 òsa osata òsa",
			"SFX §0 òbo obato òbo",
			"SFX §0 òko okato òko",
			"SFX §0 òmo omato òmo",
			"SFX §0 òvo ovato òvo",
			"SFX §0 o ato [^ò]ko"
		);
		Assertions.assertEquals(expectedRules, rules);

		reducer.checkReductionCorrectness(flag, rules, originalRules, originalLines);
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
