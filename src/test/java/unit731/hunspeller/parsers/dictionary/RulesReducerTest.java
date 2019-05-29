package unit731.hunspeller.parsers.dictionary;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
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
		List<String> words = Arrays.asList("ge", "la", "na", "nu", "vu", "ge", "sto", "adove", "indove", "kome", "kuando", "tuto", "de", "so",
			"sora", "tèrŧo", "tèrso", "kuarto", "koarto", "kuinto", "sèsto", "par", "kaxa", "sensa", "senŧa", "komòdo", "frate", "nudo");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.map(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX))
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		List<LineEntry> expectedCompactedRules = Arrays.asList(
			new LineEntry("ove", "óʼ", "ove", Arrays.asList("indove", "adove")),
			new LineEntry("r", "ʼ", "r", "par"),
			new LineEntry("u", "ʼ", "u", Arrays.asList("nu", "vu")),
			new LineEntry("ra", "ʼ", "ra", "sora"),
			new LineEntry("do", "ʼ", "do", Arrays.asList("nudo", "komòdo", "kuando")),
			new LineEntry("te", "ʼ", "te", "frate"),
			new LineEntry("xa", "ʼ", "xa", "kaxa"),
			new LineEntry("me", "ʼ", "me", "kome"),
			new LineEntry("o", "ʼ", "[^d]o", Arrays.asList("koarto", "kuinto", "kuarto", "sèsto", "tèrso", "tèrŧo", "tuto", "so", "sto")),
			new LineEntry("e", "ʼ", "[dg]e", Arrays.asList("de", "ge")),
			new LineEntry("a", "ʼ", "[^rx]a", Arrays.asList("sensa", "senŧa", "na", "la"))
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
		List<String> words = Arrays.asList("kanèƚo", "kapèƚa", "kapèƚo", "ƚibro", "vedèƚa", "vedèƚo", "moƚo", "rosiñoƚo", "roxiñoƚo", "kaƚandra",
			"kaƚandro", "xeƚo", "rusiñoƚo", "ruxiñoƚo");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.map(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX))
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		List<LineEntry> expectedCompactedRules = Arrays.asList(
			new LineEntry("èƚa", "eƚata", "èƚa", Arrays.asList("kapèƚa", "vedèƚa")),
			new LineEntry("èƚo", "eƚato", "èƚo", Arrays.asList("kapèƚo", "vedèƚo", "kanèƚo")),
			new LineEntry("o", "ato", "[^ƚ]o", Arrays.asList("ƚibro", "kaƚandro")),
			new LineEntry("0", "ta", "[^ƚ]a", "kaƚandra"),
			new LineEntry("o", "ato", "[^è]ƚo", Arrays.asList("moƚo", "roxiñoƚo", "rosiñoƚo", "xeƚo", "ruxiñoƚo", "rusiñoƚo"))
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
		List<String> words = Arrays.asList("aria", "bar", "baron", "bon", "borso", "bosko", "dixnar", "dòna", "fakin", "far", "fator", "fatora",
			"grada", "granfo", "gòba", "gòbo", "inkuixitor", "inkuixitora", "inspetor", "inspetora", "kalandra", "kalandro", "kanèl", "kapèl",
			"kapèla", "kara", "kojon", "konto", "kora", "koɉon", "kuadra", "kuadro", "kòsa", "libro", "maca", "mando", "manxo", "manđo", "marenda",
			"merenda", "mol", "muso", "padron", "paron", "patron", "pecenin", "pesenin", "peŧenin", "porko", "pòka", "pòko", "rexon", "rosiñol",
			"roxiñol", "rusiñol", "ruxiñol", "ròba", "savia", "savio", "sen", "sinsin", "soko", "solfro", "sorgo", "speso", "sporko", "tabar",
			"toxa", "vedèl", "vedèla", "verdo", "vesin", "vexin", "vexo", "veŧin", "visio", "viŧio", "xbir", "xel", "òka", "òko", "òmo", "òvo",
			"đeneral", "đilio", "ŧedro", "ŧinŧin", "ŧoko");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.map(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX))
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		List<LineEntry> expectedCompactedRules = Arrays.asList(
			new LineEntry("òbo", "obato", "òbo", "gòbo"),
			new LineEntry("òba", "obata", "òba", Arrays.asList("gòba", "ròba")),
			new LineEntry("òko", "okato", "òko", Arrays.asList("pòko", "òko")),
			new LineEntry("èla", "elata", "èla", Arrays.asList("kapèla", "vedèla")),
			new LineEntry("òna", "onata", "òna", "dòna"),
			new LineEntry("òmo", "omato", "òmo", "òmo"),
			new LineEntry("òsa", "osata", "òsa", "kòsa"),
			new LineEntry("èl", "elato", "èl", Arrays.asList("vedèl", "kanèl", "kapèl")),
			new LineEntry("òka", "okata", "òka", Arrays.asList("òka", "pòka")),
			new LineEntry("òvo", "ovato", "òvo", "òvo"),
			new LineEntry("0", "ato", "[nr]", Arrays.asList("bon", "dixnar", "veŧin", "bar", "far", "tabar", "paron", "koɉon", "ŧinŧin", "inkuixitor",
				"sen", "baron", "vexin", "patron", "peŧenin", "vesin", "pecenin", "xbir", "kojon", "rexon", "inspetor", "fator", "sinsin", "padron",
				"pesenin", "fakin")),
			new LineEntry("o", "ato", "[^bkmv]o", Arrays.asList("verdo", "libro", "đilio", "mando", "viŧio", "savio", "speso", "kalandro", "vexo",
				"ŧedro", "konto", "manđo", "granfo", "sorgo", "visio", "muso", "borso", "manxo", "kuadro", "solfro")),
			new LineEntry("0", "ato", "[^è]l", Arrays.asList("rusiñol", "ruxiñol", "rosiñol", "mol", "đeneral", "roxiñol", "xel")),
			new LineEntry("0", "ta", "[^bklns]a", Arrays.asList("kalandra", "kora", "maca", "savia", "aria", "inkuixitora", "marenda", "kuadra",
				"inspetora", "toxa", "grada", "merenda", "kara", "fatora")),
			new LineEntry("o", "ato", "[^ò]ko", Arrays.asList("bosko", "soko", "ŧoko", "porko", "sporko"))
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
			"SFX v1 Y 10",
			"SFX v1 e ista e",
			"SFX v1 0 ista [ln]",
			"SFX v1 ía ista ía",
			"SFX v1 o sta io",
			"SFX v1 èr erista èr",
			"SFX v1 a ista [^dií]a",
			"SFX v1 o ista [^i]o",
			"SFX v1 0 ista [^è]r",
			"SFX v1 òda odista òda",
			"SFX v1 ònia onista ònia"
		);
		Pair<RulesReducer, WordGenerator> pair = createReducer(affFile);
		RulesReducer reducer = pair.getLeft();
		WordGenerator wordGenerator = pair.getRight();
		String flag = "v1";
		List<String> words = Arrays.asList("folar", "foƚar", "spiƚorsar", "kaxo", "arte", "bonba", "dente", "dornal", "đornal", "fegura", "fogo",
			"kal", "kaƚo", "kapital", "kapitaƚe", "kolor", "koƚor", "lexe", "ƚexe", "mòda", "paexe", "palaso", "palaŧo", "paƚaseto", "paƚaso",
			"real", "reaƚe", "stua", "xornal", "xornaƚe", "bragièr", "figura", "boridon", "filoxomía", "fiƚoxomía", "alarme", "alkimía", "aƚarme",
			"arkimía", "bonton", "finoxomía", "kanbio", "kitara", "konto", "ŧerimònia", "ŧifra", "bregièr");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.map(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX))
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		List<LineEntry> expectedCompactedRules = Arrays.asList(
			new LineEntry("òda", "odista", "òda", "mòda"),
			new LineEntry("e", "ista", "e", Arrays.asList("kapitaƚe", "alarme", "ƚexe", "lexe", "paexe", "xornaƚe", "aƚarme", "reaƚe", "dente",
				"arte")),
			new LineEntry("ía", "ista", "ía", Arrays.asList("finoxomía", "fiƚoxomía", "alkimía", "arkimía", "filoxomía")),
			new LineEntry("ònia", "onista", "ònia", "ŧerimònia"),
			new LineEntry("èr", "erista", "èr", Arrays.asList("bregièr", "bragièr")),
			new LineEntry("0", "ista", "[ln]", Arrays.asList("dornal", "kal", "bonton", "đornal", "xornal", "real", "boridon", "kapital")),
			new LineEntry("o", "ista", "[^i]o", Arrays.asList("fogo", "konto", "paƚaso", "palaŧo", "kaƚo", "palaso", "paƚaseto", "kaxo")),
			new LineEntry("o", "sta", "io", "kanbio"),
			new LineEntry("a", "ista", "[^dií]a", Arrays.asList("fegura", "figura", "ŧifra", "bonba", "stua", "kitara")),
			new LineEntry("0", "ista", "[^è]r", Arrays.asList("folar", "koƚor", "foƚar", "spiƚorsar", "kolor"))
		);
		Assertions.assertEquals(expectedCompactedRules, compactedRules);

		List<String> rules = reducer.convertFormat(flag, false, compactedRules);
		List<String> expectedRules = Arrays.asList(
			"SFX v1 Y 10",
			"SFX v1 e ista e",
			"SFX v1 0 ista [ln]",
			"SFX v1 o sta io",
			"SFX v1 ía ista ía",
			"SFX v1 èr erista èr",
			"SFX v1 a ista [^dií]a",
			"SFX v1 o ista [^i]o",
			"SFX v1 0 ista [^è]r",
			"SFX v1 òda odista òda",
			"SFX v1 ònia onista ònia"
		);
		Assertions.assertEquals(expectedRules, rules);

		reducer.checkReductionCorrectness(flag, rules, originalRules, originalLines);
	}

	@Test
	public void simple5() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"LANG vec",
			"FLAG long",
			"SFX v0 Y 73",
			"SFX v0 0 ría e",
			"SFX v0 0 rieta e",
			"SFX v0 e aría e",
			"SFX v0 e arieta e",
			"SFX v0 0 aría [ln]",
			"SFX v0 0 ería [ln]",
			"SFX v0 0 arieta [ln]",
			"SFX v0 0 erieta [ln]",
			"SFX v0 0 ía ar",
			"SFX v0 0 ieta ar",
			"SFX v0 ar ería ar",
			"SFX v0 ar erieta ar",
			"SFX v0 èr aría èr",
			"SFX v0 èr ería èr",
			"SFX v0 èr arieta èr",
			"SFX v0 èr erieta èr",
			"SFX v0 0 ría [^ílƚ]a",
			"SFX v0 0 rieta [^ílƚ]a",
			"SFX v0 a ería [^ílƚ]a",
			"SFX v0 a erieta [^ílƚ]a",
			"SFX v0 o aría [^cdđkx]o",
			"SFX v0 o ería [^cdđkx]o",
			"SFX v0 o arieta [^cdđkx]o",
			"SFX v0 o erieta [^cdđkx]o",
			"SFX v0 0 ía [^aè]r",
			"SFX v0 0 ieta [^aè]r",
			"SFX v0 èla elaría èla",
			"SFX v0 èla elería èla",
			"SFX v0 èla elarieta èla",
			"SFX v0 èla elerieta èla",
			"SFX v0 èƚa eƚaría èƚa",
			"SFX v0 èƚa eƚería èƚa",
			"SFX v0 èƚa eƚarieta èƚa",
			"SFX v0 èƚa eƚerieta èƚa",
			"SFX v0 òco ocaría òco",
			"SFX v0 òco ocería òco",
			"SFX v0 òco ocarieta òco",
			"SFX v0 òco ocerieta òco",
			"SFX v0 èdo edaría èdo",
			"SFX v0 èdo edería èdo",
			"SFX v0 èdo edarieta èdo",
			"SFX v0 èdo ederieta èdo",
			"SFX v0 òdo odaría òdo",
			"SFX v0 òdo odería òdo",
			"SFX v0 òdo odarieta òdo",
			"SFX v0 òdo oderieta òdo",
			"SFX v0 èđo eđaría èđo",
			"SFX v0 èđo eđería èđo",
			"SFX v0 èđo eđarieta èđo",
			"SFX v0 èđo eđerieta èđo",
			"SFX v0 òko okaría òko",
			"SFX v0 òko okería òko",
			"SFX v0 òko okarieta òko",
			"SFX v0 òko okerieta òko",
			"SFX v0 èxo exaría èxo",
			"SFX v0 èxo exería èxo",
			"SFX v0 èxo exarieta èxo",
			"SFX v0 èxo exerieta èxo",
			"SFX v0 o aría [^èò]do",
			"SFX v0 o ería [^èò]do",
			"SFX v0 o arieta [^èò]do",
			"SFX v0 o erieta [^èò]do",
			"SFX v0 o aría [^ò]ko",
			"SFX v0 o ería [^ò]ko",
			"SFX v0 o arieta [^ò]ko",
			"SFX v0 o erieta [^ò]ko",
			"SFX v0 o aría [^è]xo",
			"SFX v0 o ería [^è]xo",
			"SFX v0 o arieta [^è]xo",
			"SFX v0 o erieta [^è]xo",
			"SFX v0 ía ieta ería",
			"SFX v0 ería aría ería",
			"SFX v0 ería arieta ería"
		);
		Pair<RulesReducer, WordGenerator> pair = createReducer(affFile);
		RulesReducer reducer = pair.getLeft();
		WordGenerator wordGenerator = pair.getRight();
		String flag = "v0";
		List<String> words = Arrays.asList("albergar", "ardentar", "arđentar", "arxentar", "axenar", "bañar", "barar", "berekinar", "berikinar",
			"bibiar", "birar", "birbantar", "bonbar", "bufonar", "cakolar", "cakoƚar", "drapar", "strafantar", "fatucar", "rafinar", "fondar",
			"fornar", "fraskar", "garbar", "gardar", "garđar", "garxar", "grixonar", "guanto", "vanto", "jetar", "ɉetar", "kanselar", "kanŧelar",
			"kanseƚar", "kañar", "kapocar", "kastronar", "kavalar", "kavaƚar", "kojonar", "koɉonar", "kojonbarar", "koɉonbarar", "kokolar",
			"kokoƚar", "komandar", "komensar", "komenŧar", "komensiar", "komenŧiar", "kontar", "kontrolar", "kontroƚar", "koro", "kordar",
			"rekordar", "krokar", "kuadro", "ladrar", "ƚadrar", "lexinar", "ƚexinar", "lotar", "ƚotar", "mañar", "minconar", "nodar", "panetar",
			"peocar", "peskar", "piedar", "pieđar", "piexar", "pièxo", "spilorŧar", "spilorsar", "piocar", "pitokar", "poltronar", "pomo", "porkar",
			"portar", "putelar", "puteƚar", "retelar", "reteƚar", "robar", "saonar", "senpiar", "skorsar", "skorŧar", "soldar", "sovercar",
			"spesiar", "speŧiar", "spisiar", "spiŧiar", "sporkar", "isporkar", "stanpar", "stranbar", "strasar", "straŧar", "striar", "strigar",
			"takonar", "tapesar", "tapeŧar", "tartufolar", "tartufoƚar", "sansar", "ŧimar", "simar", "vakar", "provedo", "vergexar", "xmorfiar",
			"mèdo", "mèđo", "mèxo", "kaxo", "bianka", "banko", "banpor", "batería", "bekèr", "còdo", "fresa", "freŧa", "kasèla", "kasèƚa", "kojon",
			"koɉon", "libro", "ƚibro", "muscèr", "pedòco", "peòco", "persegèr", "piòco", "polar", "poƚar", "prado", "salgèr", "sensèr", "siñor",
			"sior", "skoasa", "skoaŧa", "skovasa", "skovaŧa", "spesièr", "speŧièr", "strasa", "straŧa", "striga", "tintor", "trator", "vaka", "bira",
			"boletin", "boƚetin", "cetin", "dolfin", "fante", "fator", "forestería", "fraska", "furbo", "galante", "gaƚante", "goloxo", "goƚoxo",
			"kalegèr", "kaƚegèr", "kamarlengo", "konetrería", "ladro", "lata", "ludro", "ƚadro", "ƚata", "ƚudro", "marsèr", "marŧèr", "masèr",
			"nodara", "olivo", "oƚivo", "paregin", "parejin", "pareɉin", "pedante", "pistor", "raxente", "sekreto", "skorsèr", "skorŧèr", "skroa",
			"spada", "storno", "tentor", "teña", "birbante", "boèr", "boteja", "boteɉa", "ridikolería", "ridikoƚería", "artejería", "arteɉería",
			"artelería", "arteƚería", "kaxolería", "kaxoƚería", "kortexan", "ladron", "ƚadron", "merkandería", "ostèr", "pelatería", "peƚatería",
			"podestería", "poestería", "señor", "skrova", "skudo", "kaxèla", "kaxèƚa", "strion", "angería", "fiskal", "fiskaƚe", "mersería",
			"merŧería", "meseto", "momería", "palandería", "paƚandería", "panatería", "piskería", "desentería", "erbería", "fravo", "garda", "garđa",
			"garxa", "gril", "griƚo", "kaxolin", "kaxoƚin", "adorator", "asesor", "baldería", "balestrería", "baƚestrería", "bibioxo", "bixutería",
			"botilia", "botiƚia", "butilia", "butiƚia", "falkon", "galería", "gaƚería", "inbasería", "kafetería", "kakofonería", "kotería",
			"piavolería", "piavoƚería", "porko", "rajonato", "raɉonato", "salegèr", "saƚegèr", "santocería", "senpio", "siòko", "sixor", "skorería",
			"sovarcería", "spakon", "spiŧièr", "sporko", "stranbo", "supercería", "telería", "teƚería", "teñoxería", "tersería", "terŧería",
			"ŧaratan", "ŧibaldería", "ŧixor", "vetrería", "grixonería", "kordería", "ŧimexería", "citin", "sapientería", "segretería", "maŧèr",
			"gexo");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.map(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX))
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		List<LineEntry> expectedCompactedRules = Arrays.asList(
			new LineEntry("èla", new HashSet<>(Arrays.asList("elaría", "elería", "elarieta", "elerieta")), "èla", Arrays.asList("kasèla", "kaxèla")),
			new LineEntry("èr", new HashSet<>(Arrays.asList("arieta", "aría", "ería", "erieta")), "èr", Arrays.asList("kalegèr", "sensèr", "marŧèr",
				"muscèr", "masèr", "bekèr", "persegèr", "ostèr", "speŧièr", "saƚegèr", "maŧèr", "skorŧèr", "spiŧièr", "kaƚegèr", "marsèr", "salgèr",
				"skorsèr", "spesièr", "boèr", "salegèr")),
			new LineEntry("ería", new HashSet<>(Arrays.asList("arieta", "aría", "erieta")), "ería", Arrays.asList("supercería", "konetrería",
				"pelatería", "poestería", "baldería", "kafetería", "angería", "mersería", "tersería", "galería", "ŧibaldería", "kaxolería",
				"peƚatería", "erbería", "sovarcería", "segretería", "kotería", "teƚería", "artejería", "momería", "batería", "inbasería",
				"piavoƚería", "paƚandería", "kordería", "sapientería", "terŧería", "teñoxería", "ŧimexería", "bixutería", "forestería", "kakofonería",
				"ridikoƚería", "artelería", "podestería", "baƚestrería", "grixonería", "piskería", "balestrería", "telería", "arteƚería",
				"ridikolería", "panatería", "piavolería", "santocería", "vetrería", "gaƚería", "desentería", "arteɉería", "kaxoƚería", "palandería",
				"merkandería", "merŧería", "skorería")),
			new LineEntry("èdo", new HashSet<>(Arrays.asList("edería", "ederieta", "edarieta", "edaría")), "èdo", "mèdo"),
			new LineEntry("èƚa", new HashSet<>(Arrays.asList("eƚaría", "eƚerieta", "eƚería", "eƚarieta")), "èƚa", Arrays.asList("kasèƚa", "kaxèƚa")),
			new LineEntry("ar", new HashSet<>(Arrays.asList("arieta", "aría", "ería", "erieta")), "ar", Arrays.asList("axenar", "sporkar", "portar",
				"fatucar", "komensar", "kojonbarar", "koɉonbarar", "kavalar", "barar", "komensiar", "strafantar", "peskar", "komenŧar", "takonar",
				"komandar", "kanŧelar", "arđentar", "kastronar", "arxentar", "drapar", "bonbar", "koɉonar", "rekordar", "spisiar", "kontar",
				"panetar", "tapeŧar", "kokoƚar", "strasar", "tartufolar", "retelar", "vergexar", "senpiar", "bibiar", "putelar", "pitokar",
				"berikinar", "saonar", "strigar", "kavaƚar", "striar", "straŧar", "polar", "cakoƚar", "lotar", "piocar", "kokolar", "isporkar",
				"garbar", "rafinar", "kanseƚar", "nodar", "kojonar", "stanpar", "garđar", "poltronar", "ƚexinar", "speŧiar", "spilorŧar", "soldar",
				"ɉetar", "minconar", "piexar", "poƚar", "jetar", "reteƚar", "peocar", "piedar", "skorsar", "garxar", "kontroƚar", "kanselar",
				"puteƚar", "albergar", "ardentar", "spilorsar", "kordar", "tapesar", "grixonar", "ƚotar", "sovercar", "xmorfiar", "stranbar",
				"cakolar", "kapocar", "kontrolar", "sansar", "spiŧiar", "ŧimar", "robar", "simar", "fraskar", "komenŧiar", "bañar", "birbantar",
				"bufonar", "porkar", "kañar", "mañar", "krokar", "ladrar", "skorŧar", "fornar", "birar", "fondar", "pieđar", "ƚadrar", "gardar",
				"lexinar", "berekinar", "tartufoƚar", "vakar", "spesiar")),
			new LineEntry("òko", new HashSet<>(Arrays.asList("okarieta", "okería", "okaría", "okerieta")), "òko", "siòko"),
			new LineEntry("èđo", new HashSet<>(Arrays.asList("eđarieta", "eđaría", "eđería", "eđerieta")), "èđo", "mèđo"),
			new LineEntry("èxo", new HashSet<>(Arrays.asList("exerieta", "exería", "exaría", "exarieta")), "èxo", Arrays.asList("mèxo", "pièxo")),
			new LineEntry("òco", new HashSet<>(Arrays.asList("ocarieta", "ocería", "ocerieta", "ocaría")), "òco", Arrays.asList("pedòco", "peòco",
				"piòco")),
			new LineEntry("òdo", new HashSet<>(Arrays.asList("odaría", "oderieta", "odería", "odarieta")), "òdo", "còdo"),
			new LineEntry("e", new HashSet<>(Arrays.asList("arieta", "aría", "ería", "erieta")), "e", Arrays.asList("raxente", "galante", "gaƚante",
				"birbante", "fiskaƚe", "fante", "pedante")),
			new LineEntry("0", new HashSet<>(Arrays.asList("arieta", "aría", "ería", "erieta")), "[ln]", Arrays.asList("strion", "kaxoƚin",
				"kortexan", "kaxolin", "boƚetin", "fiskal", "falkon", "ŧaratan", "kojon", "ladron", "spakon", "cetin", "pareɉin", "citin", "dolfin",
				"koɉon", "boletin", "paregin", "ƚadron", "parejin", "gril")),
			new LineEntry("o", new HashSet<>(Arrays.asList("arieta", "aría", "ería", "erieta")), "[^cdđkx]o", Arrays.asList("meseto", "libro",
				"furbo", "ƚadro", "olivo", "ƚudro", "oƚivo", "ludro", "storno", "rajonato", "griƚo", "vanto", "ƚibro", "koro", "kamarlengo", "ladro",
				"pomo", "raɉonato", "guanto", "sekreto", "stranbo", "senpio", "fravo", "kuadro")),
			new LineEntry("a", new HashSet<>(Arrays.asList("arieta", "aría", "ería", "erieta")), "[^ílƚ]a", Arrays.asList("skovasa", "butiƚia",
				"skovaŧa", "strasa", "garda", "ƚata", "lata", "skoaŧa", "garđa", "botiƚia", "fraska", "skrova", "bira", "garxa", "freŧa", "butilia",
				"botilia", "striga", "spada", "skroa", "skoasa", "nodara", "fresa", "vaka", "teña", "boteja", "bianka", "boteɉa", "straŧa")),
			new LineEntry("0", new HashSet<>(Arrays.asList("ieta", "ía")), "or", Arrays.asList("banpor", "pistor", "adorator", "sixor", "señor",
				"asesor", "sior", "ŧixor", "tentor", "trator", "tintor", "fator", "siñor")),
			new LineEntry("o", new HashSet<>(Arrays.asList("arieta", "aría", "ería", "erieta")), "[^èò]do", Arrays.asList("provedo", "prado",
				"skudo")),
			new LineEntry("o", new HashSet<>(Arrays.asList("arieta", "aría", "ería", "erieta")), "[^è]xo", Arrays.asList("goloxo", "gexo", "bibioxo",
				"goƚoxo", "kaxo")),
			new LineEntry("o", new HashSet<>(Arrays.asList("arieta", "aría", "ería", "erieta")), "[^ò]ko", Arrays.asList("banko", "porko", "sporko"))
		);
		Assertions.assertEquals(expectedCompactedRules, compactedRules);

		List<String> rules = reducer.convertFormat(flag, false, compactedRules);
		List<String> expectedRules = Arrays.asList(
			"SFX v0 Y 73",
			"SFX v0 0 ría e",
			"SFX v0 0 rieta e",
			"SFX v0 e aría e",
			"SFX v0 e arieta e",
			"SFX v0 0 aría [ln]",
			"SFX v0 0 ería [ln]",
			"SFX v0 0 arieta [ln]",
			"SFX v0 0 erieta [ln]",
			"SFX v0 0 ía ar",
			"SFX v0 0 ieta ar",
			"SFX v0 0 ía or",
			"SFX v0 0 ieta or",
			"SFX v0 ar ería ar",
			"SFX v0 ar erieta ar",
			"SFX v0 èr aría èr",
			"SFX v0 èr ería èr",
			"SFX v0 èr arieta èr",
			"SFX v0 èr erieta èr",
			"SFX v0 0 ría [^ílƚ]a",
			"SFX v0 0 rieta [^ílƚ]a",
			"SFX v0 a ería [^ílƚ]a",
			"SFX v0 a erieta [^ílƚ]a",
			"SFX v0 o aría [^cdđkx]o",
			"SFX v0 o ería [^cdđkx]o",
			"SFX v0 o arieta [^cdđkx]o",
			"SFX v0 o erieta [^cdđkx]o",
			"SFX v0 èla elaría èla",
			"SFX v0 èla elería èla",
			"SFX v0 èla elarieta èla",
			"SFX v0 èla elerieta èla",
			"SFX v0 èƚa eƚaría èƚa",
			"SFX v0 èƚa eƚería èƚa",
			"SFX v0 èƚa eƚarieta èƚa",
			"SFX v0 èƚa eƚerieta èƚa",
			"SFX v0 òco ocaría òco",
			"SFX v0 òco ocería òco",
			"SFX v0 òco ocarieta òco",
			"SFX v0 òco ocerieta òco",
			"SFX v0 èdo edaría èdo",
			"SFX v0 èdo edería èdo",
			"SFX v0 èdo edarieta èdo",
			"SFX v0 èdo ederieta èdo",
			"SFX v0 òdo odaría òdo",
			"SFX v0 òdo odería òdo",
			"SFX v0 òdo odarieta òdo",
			"SFX v0 òdo oderieta òdo",
			"SFX v0 èđo eđaría èđo",
			"SFX v0 èđo eđería èđo",
			"SFX v0 èđo eđarieta èđo",
			"SFX v0 èđo eđerieta èđo",
			"SFX v0 òko okaría òko",
			"SFX v0 òko okería òko",
			"SFX v0 òko okarieta òko",
			"SFX v0 òko okerieta òko",
			"SFX v0 èxo exaría èxo",
			"SFX v0 èxo exería èxo",
			"SFX v0 èxo exarieta èxo",
			"SFX v0 èxo exerieta èxo",
			"SFX v0 o aría [^èò]do",
			"SFX v0 o ería [^èò]do",
			"SFX v0 o arieta [^èò]do",
			"SFX v0 o erieta [^èò]do",
			"SFX v0 o aría [^ò]ko",
			"SFX v0 o ería [^ò]ko",
			"SFX v0 o arieta [^ò]ko",
			"SFX v0 o erieta [^ò]ko",
			"SFX v0 o aría [^è]xo",
			"SFX v0 o ería [^è]xo",
			"SFX v0 o arieta [^è]xo",
			"SFX v0 o erieta [^è]xo",
			"SFX v0 ía ieta ería",
			"SFX v0 ería aría ería",
			"SFX v0 ería arieta ería"
		);
		Assertions.assertEquals(expectedRules, rules);

		reducer.checkReductionCorrectness(flag, rules, originalRules, originalLines);
	}

	@Test
	public void simple6() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"LANG vec",
			"FLAG long",
			"SFX s1 Y 3",
			"SFX s1 0 ixmo r",
			"SFX s1 ía ixmo ía",
			"SFX s1 òmo omixmo òmo"
		);
		Pair<RulesReducer, WordGenerator> pair = createReducer(affFile);
		RulesReducer reducer = pair.getLeft();
		WordGenerator wordGenerator = pair.getRight();
		String flag = "s1";
		List<String> words = Arrays.asList("ƚuminar", "gaƚantòmo", "maƚinkonía");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.map(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX))
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		List<LineEntry> expectedCompactedRules = Arrays.asList(
			new LineEntry("ía", "ixmo", "ía", "maƚinkonía"),
			new LineEntry("òmo", "omixmo", "òmo", "gaƚantòmo"),
			new LineEntry("0", "ixmo", "r", "ƚuminar")
		);
		Assertions.assertEquals(expectedCompactedRules, compactedRules);

		List<String> rules = reducer.convertFormat(flag, false, compactedRules);
		List<String> expectedRules = Arrays.asList(
			"SFX s1 Y 3",
			"SFX s1 0 ixmo r",
			"SFX s1 ía ixmo ía",
			"SFX s1 òmo omixmo òmo"
		);
		Assertions.assertEquals(expectedRules, rules);

		reducer.checkReductionCorrectness(flag, rules, originalRules, originalLines);
	}

	@Test
	public void simple7() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"LANG vec",
			"FLAG long",
			"SFX s0 Y 6",
			"SFX s0 0 ixmo [nr]",
			"SFX s0 ía ixmo ía",
			"SFX s0 a ixmo [^í]a",
			"SFX s0 òko okixmo òko",
			"SFX s0 òmo omixmo òmo",
			"SFX s0 òto otixmo òto"
		);
		Pair<RulesReducer, WordGenerator> pair = createReducer(affFile);
		RulesReducer reducer = pair.getLeft();
		WordGenerator wordGenerator = pair.getRight();
		String flag = "s0";
		List<String> words = Arrays.asList("malinkonía", "bigòto", "galantòmo", "pitòko", "bigòto", "baron", "kokon", "konpar", "luminar",
			"franŧexa", "fransexa");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.map(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX))
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		List<LineEntry> expectedCompactedRules = Arrays.asList(
			new LineEntry("òko", "okixmo", "òko", "pitòko"),
			new LineEntry("ía", "ixmo", "ía", "malinkonía"),
			new LineEntry("òmo", "omixmo", "òmo", "galantòmo"),
			new LineEntry("òto", "otixmo", "òto", "bigòto"),
			new LineEntry("0", "ixmo", "[nr]", Arrays.asList("baron", "kokon", "konpar", "luminar")),
			new LineEntry("a", "ixmo", "[^í]a", Arrays.asList("franŧexa", "fransexa"))
		);
		Assertions.assertEquals(expectedCompactedRules, compactedRules);

		List<String> rules = reducer.convertFormat(flag, false, compactedRules);
		List<String> expectedRules = Arrays.asList(
			"SFX s0 Y 6",
			"SFX s0 0 ixmo [nr]",
			"SFX s0 ía ixmo ía",
			"SFX s0 a ixmo [^í]a",
			"SFX s0 òko okixmo òko",
			"SFX s0 òmo omixmo òmo",
			"SFX s0 òto otixmo òto"
		);
		Assertions.assertEquals(expectedRules, rules);

		reducer.checkReductionCorrectness(flag, rules, originalRules, originalLines);
	}

	@Test
	public void simple8() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"LANG vec",
			"FLAG long",
			"SFX r8 Y 12",
			"SFX r8 r ora r",
			"SFX r8 r dora r",
			"SFX r8 r or r",
			"SFX r8 r tora r",
			"SFX r8 r dor r",
			"SFX r8 r oreta r",
			"SFX r8 r tor r",
			"SFX r8 r doreta r",
			"SFX r8 r toreta r",
			"SFX r8 r oreto r",
			"SFX r8 r doreto r",
			"SFX r8 r toreto r"
		);
		Pair<RulesReducer, WordGenerator> pair = createReducer(affFile);
		RulesReducer reducer = pair.getLeft();
		WordGenerator wordGenerator = pair.getRight();
		String flag = "r8";
		List<String> words = Arrays.asList("ƚargar", "boƚar", "noƚixar", "noƚexar", "spigoƚar", "ƚustrar", "sesoƚar", "kalkoƚar", "ƚavorar",
			"iƚuminar", "piƚar", "regoƚar", "kaƚibrar", "señaƚar", "oxeƚar", "kriveƚar", "saƚixar", "ventiƚar", "ƚuminar", "aƚienar", "ƚexixlar",
			"triveƚar", "spekuƚar", "garbeƚar", "ƚibar", "paƚar", "koƚorir", "ƚigar", "siaƚakuar", "mormoƚar", "ƚikar", "soƚesitar", "skarpeƚar",
			"ƚaorar", "foƚar", "stroƚegar", "spoƚar", "stroƚogar", "baƚar", "fiƚar", "koƚar", "saƚar", "ƚevar", "baƚotar", "ƚavar");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.map(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX))
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		List<LineEntry> expectedCompactedRules = Arrays.asList(
			new LineEntry("r", new HashSet<>(Arrays.asList("oreto", "toreto", "dora", "doreta", "ora", "doreto", "tor", "toreta", "oreta", "or",
				"tora", "dor")), "r", Arrays.asList("ƚargar", "boƚar", "noƚixar", "noƚexar", "spigoƚar", "ƚustrar", "sesoƚar", "kalkoƚar", "ƚavorar",
				"iƚuminar", "piƚar", "regoƚar", "kaƚibrar", "señaƚar", "oxeƚar", "kriveƚar", "saƚixar", "ventiƚar", "ƚuminar", "aƚienar", "ƚexixlar",
				"triveƚar", "spekuƚar", "garbeƚar", "ƚibar", "paƚar", "koƚorir", "ƚigar", "siaƚakuar", "mormoƚar", "ƚikar", "soƚesitar", "skarpeƚar",
				"ƚaorar", "foƚar", "stroƚegar", "spoƚar", "stroƚogar", "baƚar", "fiƚar", "koƚar", "saƚar", "ƚevar", "baƚotar", "ƚavar"))
		);
		Assertions.assertEquals(expectedCompactedRules, compactedRules);

		List<String> rules = reducer.convertFormat(flag, false, compactedRules);
		List<String> expectedRules = Arrays.asList(
			"SFX r8 Y 12",
			"SFX r8 r or r",
			"SFX r8 r dor r",
			"SFX r8 r ora r",
			"SFX r8 r tor r",
			"SFX r8 r dora r",
			"SFX r8 r tora r",
			"SFX r8 r oreta r",
			"SFX r8 r oreto r",
			"SFX r8 r doreta r",
			"SFX r8 r doreto r",
			"SFX r8 r toreta r",
			"SFX r8 r toreto r"
		);
		Assertions.assertEquals(expectedRules, rules);

		reducer.checkReductionCorrectness(flag, rules, originalRules, originalLines);
	}

	@Test
	public void simple9() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"LANG vec",
			"FLAG long",
			"SFX r7 Y 34",
			"SFX r7 rò ora rò",
			"SFX r7 rò dora rò",
			"SFX r7 rò or rò",
			"SFX r7 rò tora rò",
			"SFX r7 rò dor rò",
			"SFX r7 rò oreta rò",
			"SFX r7 rò tor rò",
			"SFX r7 rò doreta rò",
			"SFX r7 rò toreta rò",
			"SFX r7 rò oreto rò",
			"SFX r7 rò doreto rò",
			"SFX r7 rò toreto rò",
			"SFX r7 r ora [^o]r",
			"SFX r7 r dora [^o]r",
			"SFX r7 r or [^o]r",
			"SFX r7 r tora [^o]r",
			"SFX r7 r dor [^o]r",
			"SFX r7 r oreta [^o]r",
			"SFX r7 r tor [^o]r",
			"SFX r7 r doreta [^o]r",
			"SFX r7 r toreta [^o]r",
			"SFX r7 r oreto [^o]r",
			"SFX r7 r doreto [^o]r",
			"SFX r7 r toreto [^o]r",
			"SFX r7 0 eto dor",
			"SFX r7 dor or dor",
			"SFX r7 dor tor dor",
			"SFX r7 dor oreto dor",
			"SFX r7 dor toreto dor",
			"SFX r7 a eta dora",
			"SFX r7 dora ora dora",
			"SFX r7 dora tora dora",
			"SFX r7 dora oreta dora",
			"SFX r7 dora toreta dora"
		);
		Pair<RulesReducer, WordGenerator> pair = createReducer(affFile);
		RulesReducer reducer = pair.getLeft();
		WordGenerator wordGenerator = pair.getRight();
		String flag = "r7";
		List<String> words = Arrays.asList("reŧevidor", "reŧeidor", "stridor", "resevidor", "reseidor", "fondarò", "batarò", "duxarò", "vendarò",
			"kredarò", "provedarò", "bevarò", "resevidora", "reseidora", "bratadora", "feridora", "reŧevidora", "reŧeidora", "dopiar", "kavar",
			"armar", "soleŧitar", "sadar", "trionfar", "kojonbarar", "pasar", "strologar", "dorar", "salar", "arar", "barar", "rengar", "ligar",
			"partegar", "bevarar", "destramedar", "kolorir", "spekular", "tajar", "guidar", "paŧifegar", "ventilar", "komandar", "noledar",
			"inpaɉar", "tubiar", "barkar", "spolar", "sopresar", "sagurar", "suxerir", "filar", "karexar", "stokar", "taɉar", "xjonfar", "mendar",
			"remar", "sostentar", "mexar", "sperdurar", "faitar", "versar", "sprexurar", "alienar", "trebiar", "kronpar", "suɉerir", "xogar",
			"kuantifegar", "inbiankar", "garbelar", "audir", "palar", "studiar", "renkurar", "renfreskar", "vixitar", "fiokar", "međar", "mexurar",
			"befar", "kaređar", "kalŧar", "desturbar", "botiđar", "kurar", "falsifegar", "spigolar", "kalkar", "stronđar", "bitar", "brunir",
			"xugar", "kartar", "kalsar", "raxar", "trufar", "sujarir", "koniar", "vanpar", "garđar", "bufar", "salidar", "semenar", "danexar",
			"sekurar", "minconar", "piantar", "kuadrar", "nolixar", "krivelar", "tarixar", "stuŧegar", "infrandar", "krear", "inbotir", "strondar",
			"setar", "travaxar", "buratar", "akuxar", "sparxurar", "menar", "kopar", "kuniar", "mediar", "dupiar", "skansar", "botonar", "suɉarir",
			"oxelar", "goernar", "pasifegar", "solesitar", "ingraviar", "saltar", "mentir", "servar", "kastrar", "parar", "segurar", "konfortar",
			"mokar", "sekar", "kalibrar", "sorar", "medar", "pionar", "kaŧar", "bufonar", "xɉonfar", "karedar", "mormolar", "đontar", "kasar",
			"takar", "predegar", "mañar", "sesolar", "luminar", "konŧar", "koñar", "stimar", "xbrufar", "paisar", "kantar", "ŧarlar", "fondar",
			"sarlar", "defamar", "fornir", "gardar", "botidar", "ŧetar", "kondurar", "banpar", "partir", "redar", "menestrar", "xontar", "konsar",
			"inpajar", "portar", "rekamar", "inpinir", "infranđar", "governar", "koɉonbarar", "sitar", "sarvar", "aldir", "sperxurar", "dugar",
			"pagar", "pisar", "sapar", "nolexar", "tokar", "saxar", "tirar", "likar", "peskar", "kalkolar", "gabar", "tornir", "ŧernir", "kuñar",
			"rascar", "xgrafiñar", "istuar", "sfroxar", "spaŧar", "dimandar", "stivar", "apaltar", "borar", "lavorar", "levar", "konđurar",
			"destramexar", "saldar", "varar", "mirar", "stusegar", "koɉonar", "noliđar", "sagomar", "operar", "kontar", "kaveŧar", "mormorar",
			"konsumar", "vantar", "cacarar", "sperđurar", "papar", "montar", "tamixar", "murar", "tariđar", "urtar", "kavesar", "argomentar",
			"libar", "spasar", "fiabar", "sikurar", "strisar", "kargar", "misiar", "pilar", "piŧar", "prokurar", "skarpelar", "salixar", "ŧerkar",
			"lustrar", "forar", "balar", "purgar", "sađar", "radar", "spedir", "dontar", "rafinar", "ŧapar", "ŧitar", "kojonar", "stanpar",
			"spredurar", "estimar", "serkar", "bruskar", "noar", "pianar", "subarendar", "fiorir", "stronxar", "lavar", "sajar", "spređurar",
			"rear", "braŧar", "pontar", "kapar", "springar", "beverar", "fregar", "ordir", "brasar", "petenar", "siegar", "garxar", "pekar",
			"xbuxar", "kaminar", "laorar", "cetar", "đogar", "sigurar", "saɉar", "gomiar", "bolar", "maxenar", "formar", "trivelar", "suxarir",
			"balotar", "biastemar", "nolidar", "ŧanŧar", "kolar", "supiar", "sialakuar", "infiar", "noleđar", "sernir", "varsar", "granir",
			"pertegar", "trovar", "rinfreskar", "matar", "sugar", "andar", "segar", "sansar", "konxurar", "iluminar", "sujerir", "botixar",
			"mankar", "ŧimar", "petar", "sonar", "examinar", "largar", "señalar", "guarnir", "simar", "saliđar", "guastar", "kagar", "refreskar",
			"lexixlar", "señar", "dogar", "minusar", "konprar", "inpastar", "trasinar", "regolar", "rostir", "dexeñar", "taridar", "skardar",
			"folar", "kalmar", "raspar", "butar", "đugar", "minuŧar", "strupiar", "infranxar", "destrameđar", "bosar", "frapar", "stuar", "mixurar",
			"ŧoetar", "manestrar", "testar", "kortegar", "maŧar", "soetar", "strolegar", "boŧar", "parlar", "sublokar", "guantar", "fortifegar",
			"fumar", "inganar", "negar", "masar", "stukar");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.map(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX))
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		List<LineEntry> expectedCompactedRules = Arrays.asList(
			new LineEntry("rò", new HashSet<>(Arrays.asList("oreto", "dora", "doreta", "ora", "toreto", "doreto", "tor", "toreta", "oreta", "or",
				"tora", "dor")), "rò", Arrays.asList("fondarò", "batarò", "duxarò", "vendarò", "kredarò", "provedarò", "bevarò")),
			new LineEntry("dora", new HashSet<>(Arrays.asList("doreta", "ora", "toreta", "oreta", "tora")), "dora", Arrays.asList("resevidora",
				"reseidora", "bratadora", "feridora", "reŧevidora", "reŧeidora")),
			new LineEntry("dor", new HashSet<>(Arrays.asList("oreto", "toreto", "doreto", "tor", "or")), "dor", Arrays.asList("reŧevidor",
				"reŧeidor", "stridor", "resevidor", "reseidor")),
			new LineEntry("r", new HashSet<>(Arrays.asList("oreto", "dora", "doreta", "ora", "toreto", "doreto", "tor", "toreta", "oreta", "or",
				"tora", "dor")), "[^o]r", Arrays.asList("dopiar", "kavar", "armar", "soleŧitar", "sadar", "trionfar", "kojonbarar", "pasar",
				"strologar", "dorar", "salar", "arar", "barar", "rengar", "ligar", "partegar", "bevarar", "destramedar", "kolorir", "spekular",
				"tajar", "guidar", "paŧifegar", "ventilar", "komandar", "noledar", "inpaɉar", "tubiar", "barkar", "spolar", "sopresar", "sagurar",
				"suxerir", "filar", "karexar", "stokar", "taɉar", "xjonfar", "mendar", "remar", "sostentar", "mexar", "sperdurar", "faitar",
				"versar", "sprexurar", "alienar", "trebiar", "kronpar", "suɉerir", "xogar", "kuantifegar", "inbiankar", "garbelar", "audir", "palar",
				"studiar", "renkurar", "renfreskar", "vixitar", "fiokar", "međar", "mexurar", "befar", "kaređar", "kalŧar", "desturbar", "botiđar",
				"kurar", "falsifegar", "spigolar", "kalkar", "stronđar", "bitar", "brunir", "xugar", "kartar", "kalsar", "raxar", "trufar", "sujarir",
				"koniar", "vanpar", "garđar", "bufar", "salidar", "semenar", "danexar", "sekurar", "minconar", "piantar", "kuadrar", "nolixar",
				"krivelar", "tarixar", "stuŧegar", "infrandar", "krear", "inbotir", "strondar", "setar", "travaxar", "buratar", "akuxar", "sparxurar",
				"menar", "kopar", "kuniar", "mediar", "dupiar", "skansar", "botonar", "suɉarir", "oxelar", "goernar", "pasifegar", "solesitar",
				"ingraviar", "saltar", "mentir", "servar", "kastrar", "parar", "segurar", "konfortar", "mokar", "sekar", "kalibrar", "sorar", "medar",
				"pionar", "kaŧar", "bufonar", "xɉonfar", "karedar", "mormolar", "đontar", "kasar", "takar", "predegar", "mañar", "sesolar", "luminar",
				"konŧar", "koñar", "stimar", "xbrufar", "paisar", "kantar", "ŧarlar", "fondar", "sarlar", "defamar", "fornir", "gardar", "botidar",
				"ŧetar", "kondurar", "banpar", "partir", "redar", "menestrar", "xontar", "konsar", "inpajar", "portar", "rekamar", "inpinir",
				"infranđar", "governar", "koɉonbarar", "sitar", "sarvar", "aldir", "sperxurar", "dugar", "pagar", "pisar", "sapar", "nolexar",
				"tokar", "saxar", "tirar", "likar", "peskar", "kalkolar", "gabar", "tornir", "ŧernir", "kuñar", "rascar", "xgrafiñar", "istuar",
				"sfroxar", "spaŧar", "dimandar", "stivar", "apaltar", "borar", "lavorar", "levar", "konđurar", "destramexar", "saldar", "varar",
				"mirar", "stusegar", "koɉonar", "noliđar", "sagomar", "operar", "kontar", "kaveŧar", "mormorar", "konsumar", "vantar", "cacarar",
				"sperđurar", "papar", "montar", "tamixar", "murar", "tariđar", "urtar", "kavesar", "argomentar", "libar", "spasar", "fiabar",
				"sikurar", "strisar", "kargar", "misiar", "pilar", "piŧar", "prokurar", "skarpelar", "salixar", "ŧerkar", "lustrar", "forar",
				"balar", "purgar", "sađar", "radar", "spedir", "dontar", "rafinar", "ŧapar", "ŧitar", "kojonar", "stanpar", "spredurar", "estimar",
				"serkar", "bruskar", "noar", "pianar", "subarendar", "fiorir", "stronxar", "lavar", "sajar", "spređurar", "rear", "braŧar", "pontar",
				"kapar", "springar", "beverar", "fregar", "ordir", "brasar", "petenar", "siegar", "garxar", "pekar", "xbuxar", "kaminar", "laorar",
				"cetar", "đogar", "sigurar", "saɉar", "gomiar", "bolar", "maxenar", "formar", "trivelar", "suxarir", "balotar", "biastemar",
				"nolidar", "ŧanŧar", "kolar", "supiar", "sialakuar", "infiar", "noleđar", "sernir", "varsar", "granir", "pertegar", "trovar",
				"rinfreskar", "matar", "sugar", "andar", "segar", "sansar", "konxurar", "iluminar", "sujerir", "botixar", "mankar", "ŧimar", "petar",
				"sonar", "examinar", "largar", "señalar", "guarnir", "simar", "saliđar", "guastar", "kagar", "refreskar", "lexixlar", "señar",
				"dogar", "minusar", "konprar", "inpastar", "trasinar", "regolar", "rostir", "dexeñar", "taridar", "skardar", "folar", "kalmar",
				"raspar", "butar", "đugar", "minuŧar", "strupiar", "infranxar", "destrameđar", "bosar", "frapar", "stuar", "mixurar", "ŧoetar",
				"manestrar", "testar", "kortegar", "maŧar", "soetar", "strolegar", "boŧar", "parlar", "sublokar", "guantar", "fortifegar", "fumar",
				"inganar", "negar", "masar", "stukar"))
		);
		Assertions.assertEquals(expectedCompactedRules, compactedRules);

		List<String> rules = reducer.convertFormat(flag, false, compactedRules);
		List<String> expectedRules = Arrays.asList(
			"SFX r7 Y 34",
			"SFX r7 rò or rò",
			"SFX r7 rò dor rò",
			"SFX r7 rò ora rò",
			"SFX r7 rò tor rò",
			"SFX r7 rò dora rò",
			"SFX r7 rò tora rò",
			"SFX r7 rò oreta rò",
			"SFX r7 rò oreto rò",
			"SFX r7 rò doreta rò",
			"SFX r7 rò doreto rò",
			"SFX r7 rò toreta rò",
			"SFX r7 rò toreto rò",
			"SFX r7 r or [^o]r",
			"SFX r7 r dor [^o]r",
			"SFX r7 r ora [^o]r",
			"SFX r7 r tor [^o]r",
			"SFX r7 r dora [^o]r",
			"SFX r7 r tora [^o]r",
			"SFX r7 r oreta [^o]r",
			"SFX r7 r oreto [^o]r",
			"SFX r7 r doreta [^o]r",
			"SFX r7 r doreto [^o]r",
			"SFX r7 r toreta [^o]r",
			"SFX r7 r toreto [^o]r",
			"SFX r7 0 eto dor",
			"SFX r7 dor or dor",
			"SFX r7 dor tor dor",
			"SFX r7 dor oreto dor",
			"SFX r7 dor toreto dor",
			"SFX r7 a eta dora",
			"SFX r7 dora ora dora",
			"SFX r7 dora tora dora",
			"SFX r7 dora oreta dora",
			"SFX r7 dora toreta dora"
		);
		Assertions.assertEquals(expectedRules, rules);

		reducer.checkReductionCorrectness(flag, rules, originalRules, originalLines);
	}

	@Test
	public void simple10() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"LANG vec",
			"FLAG long",
			"SFX r6 Y 8",
			"SFX r6 r ura [^u]r",
			"SFX r6 r dura [^u]r",
			"SFX r6 r ureta [^u]r",
			"SFX r6 r dureta [^u]r",
			"SFX r6 dur ur dur",
			"SFX r6 dur ureto dur",
			"SFX r6 dura ura dura",
			"SFX r6 dura ureta dura"
		);
		Pair<RulesReducer, WordGenerator> pair = createReducer(affFile);
		RulesReducer reducer = pair.getLeft();
		WordGenerator wordGenerator = pair.getRight();
		String flag = "r6";
		List<String> words = Arrays.asList("baƚar", "boƚar", "doƚar", "fiƚar", "gaƚopar", "kaneƚar", "kavaƚar", "koƚar", "inkordeƚar", "kriveƚar",
			"ƚatar", "ƚavar", "ƚeskar", "ƚetarar", "ƚeterar", "ƚexenar", "ƚexixlar", "ƚigar", "ƚimar", "ƚuminar", "iƚuminar", "moƚar", "paƚar",
			"paƚetar", "peƚar", "saƚar", "skarseƚar", "stabiƚir", "soƚar", "vaƚir", "vixiƚar", "koƚadur", "stabeƚidura", "vaƚidur");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.map(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX))
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		List<LineEntry> expectedCompactedRules = Arrays.asList(
			new LineEntry("dura", new HashSet<>(Arrays.asList("ureta", "ura")), "dura", "stabeƚidura"),
			new LineEntry("dur", new HashSet<>(Arrays.asList("ureto", "ur")), "dur", Arrays.asList("koƚadur", "vaƚidur")),
			new LineEntry("r", new HashSet<>(Arrays.asList("ureta", "ura", "dureta", "dura")), "[^u]r", Arrays.asList("ƚatar", "ƚeterar", "boƚar",
				"vaƚir", "ƚetarar", "iƚuminar", "moƚar", "doƚar", "kriveƚar", "kavaƚar", "ƚuminar", "kaneƚar", "ƚexixlar", "soƚar", "paƚar", "stabiƚir",
				"paƚetar", "peƚar", "ƚigar", "ƚimar", "inkordeƚar", "ƚexenar", "skarseƚar", "baƚar", "fiƚar", "koƚar", "saƚar", "vixiƚar", "ƚeskar",
				"gaƚopar", "ƚavar"))
		);
		Assertions.assertEquals(expectedCompactedRules, compactedRules);

		List<String> rules = reducer.convertFormat(flag, false, compactedRules);
		List<String> expectedRules = Arrays.asList(
			"SFX r6 Y 8",
			"SFX r6 r ura [^u]r",
			"SFX r6 r dura [^u]r",
			"SFX r6 r ureta [^u]r",
			"SFX r6 r dureta [^u]r",
			"SFX r6 dur ur dur",
			"SFX r6 dur ureto dur",
			"SFX r6 dura ura dura",
			"SFX r6 dura ureta dura"
		);
		Assertions.assertEquals(expectedRules, rules);

		reducer.checkReductionCorrectness(flag, rules, originalRules, originalLines);
	}

	@Test
	public void simple11() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"LANG vec",
			"FLAG long",
			"SFX r5 Y 12",
			"SFX r5 r ura [^u]r",
			"SFX r5 r dura [^u]r",
			"SFX r5 r ureta [^u]r",
			"SFX r5 r dureta [^u]r",
			"SFX r5 erò iura erò",
			"SFX r5 erò idura erò",
			"SFX r5 erò iureta erò",
			"SFX r5 erò idureta erò",
			"SFX r5 dur ur dur",
			"SFX r5 dur ureto dur",
			"SFX r5 dura ura dura",
			"SFX r5 dura ureta dura"
		);
		Pair<RulesReducer, WordGenerator> pair = createReducer(affFile);
		RulesReducer reducer = pair.getLeft();
		WordGenerator wordGenerator = pair.getRight();
		String flag = "r5";
		List<String> words = Arrays.asList("baƚar", "boƚar", "doƚar", "fiƚar", "gaƚopar", "kaneƚar", "kavaƚar", "koƚar", "inkordeƚar", "kriveƚar",
			"ƚatar", "ƚavar", "ƚeskar", "ƚetarar", "ƚeterar", "ƚexenar", "ƚexixlar", "ƚigar", "ƚimar", "ƚuminar", "iƚuminar", "moƚar", "paƚar",
			"paƚetar", "peƚar", "saƚar", "skarseƚar", "stabiƚir", "soƚar", "vaƚir", "vixiƚar", "koƚadur", "stabeƚidura", "vaƚidur", "fenderò",
			"teserò", "inprimerò", "sebaterò", "baterò", "kuxerò", "torderò", "koxerò", "torđerò", "sobaterò", "torxerò");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.map(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX))
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		List<LineEntry> expectedCompactedRules = Arrays.asList(
			new LineEntry("dura", new HashSet<>(Arrays.asList("ureta", "ura")), "dura", Arrays.asList("bokadura", "inđinadura", "nadura", "sfendadura",
				"batadura", "resapadura", "strenxadura", "madura", "sobatidura", "bastidura", "skrivadura", "alberadura", "sprokadura", "sobatadura",
				"fredura", "introfregadura", "stabelidura", "tesadura", "sfendidura", "spakadura", "anadura", "reŧapadura", "strenđadura", "sabatadura",
				"albaradura", "proŧedura", "vertadura", "sperdadura", "korporadura", "fogonadura", "ponxadura", "sferdadura", "inxinadura", "prosedura",
				"indinadura", "strendadura")),
			new LineEntry("erò", new HashSet<>(Arrays.asList("idura", "iura", "iureta", "idureta")), "erò", Arrays.asList("fenderò", "teserò",
				"inprimerò", "sebaterò", "baterò", "kuxerò", "torderò", "koxerò", "torđerò", "sobaterò", "torxerò")),
			new LineEntry("dur", new HashSet<>(Arrays.asList("ur", "ureto")), "dur", Arrays.asList("madur", "traxeɉadur", "traxejadur", "tradadur",
				"trađadur", "kagadur", "traxadur", "tragadur", "ordadur", "koladur", "validur", "ordidur", "skortegadur")),
			new LineEntry("r", new HashSet<>(Arrays.asList("dura", "ureta", "dureta", "ura")), "[^u]r", Arrays.asList("dopiar", "kavar", "armar",
				"broɉar", "pasar", "pontedar", "forŧar", "salar", "sejar", "arar", "latar", "roxegar", "serar", "ardenar", "ligar", "kadenar", "forsar",
				"ingarđir", "inɉermar", "tajar", "dentar", "vixilar", "injermar", "mastegar", "seɉar", "ingropar", "inpaɉar", "brojar", "bonar", "vokar",
				"tragar", "filar", "leterar", "piŧegar", "kornar", "skurtar", "risar", "riŧar", "stokar", "taɉar", "bordar", "inkrespar", "mendar",
				"bojir", "farar", "alborar", "sakar", "inɉarmar", "indopionar", "palar", "paletar", "ponsar", "kalŧar", "inkordelar", "kurar", "infarinar",
				"bardar", "boɉir", "ŧeɉar", "kalkar", "stronđar", "brunir", "kartar", "incodar", "kalsar", "josar", "arborar", "garđar", "inkroxar",
				"remurcar", "joŧar", "injarmar", "fasar", "skuarsar", "dolar", "pisegar", "bragar", "ɉetar", "jetar", "molar", "ŧejar", "stortar",
				"skuarŧar", "krivelar", "inbotir", "strondar", "gomitar", "ponteđar", "ŧerpir", "mostar", "valir", "xbokar", "dupiar", "netar", "spasiar",
				"botonar", "tresar", "ingarxir", "letarar", "ɉosar", "ɉoŧar", "rusar", "ingraviar", "baronar", "travar", "ferar", "vomitar", "sivansar",
				"bastir", "kavalkar", "mokar", "sekar", "iriŧar", "sivanŧar", "inbriagar", "toxar", "piegar", "xgrafar", "puñar", "đontar", "kasar",
				"takar", "mañar", "limar", "luminar", "konŧar", "frixar", "invastir", "kaenar", "fornir", "gardar", "partir", "veriar", "xontar", "konsar",
				"rondar", "sfexar", "tenperar", "inpajar", "portar", "spalmar", "rekamar", "skarselar", "vansar", "foɉar", "musar", "arxenar", "kanelar",
				"skoar", "krepar", "kavalar", "sapar", "copar", "guxar", "bavar", "tirar", "rasar", "strukar", "spaŧar", "bekar", "spakar", "borar",
				"saldar", "pontexar", "rekordar", "serpir", "vanŧar", "tastar", "kaveŧar", "inkarnar", "papar", "montar", "solar", "kavesar", "xetar",
				"spasar", "stabilir", "vestir", "kargar", "inmaltar", "ŧercar", "arđenar", "stranŧir", "fojar", "manegar", "spinar", "balar", "inpajetar",
				"purgar", "bastonar", "stransir", "garbar", "rexentar", "sercar", "vergar", "inboxemar", "dontar", "rafinar", "ŧapar", "skotar", "lexenar",
				"kalŧinar", "bruskar", "ingardir", "pianar", "rebaltar", "stronxar", "lavar", "rear", "leskar", "vedriar", "braŧar", "sarar", "pontar",
				"kapar", "fregar", "ordir", "brasar", "petenar", "kinkar", "siegar", "garxar", "botar", "ingrespar", "postar", "inpaɉetar", "bolar",
				"maxenar", "speŧar", "spesar", "kolar", "kordar", "spaŧiar", "infiar", "granir", "ingaxiar", "skaldar", "andar", "segar", "iluminar",
				"breviar", "ŧimar", "inkorsar", "kuxinar", "guarnir", "preŧar", "simar", "raɉar", "fodrar", "pelar", "kagar", "inbokar", "lexixlar",
				"señar", "skorlar", "bañar", "bendar", "kalsinar", "bruxar", "rostir", "fiankar", "makar", "vardar", "galopar", "strupiar", "kuxir",
				"gonfiar", "testar", "kortegar", "presar", "skontrar", "morsegar", "inkamixar", "stekar", "rajar", "fumar", "skermar"))
		);
		Assertions.assertEquals(expectedCompactedRules, compactedRules);

		List<String> rules = reducer.convertFormat(flag, false, compactedRules);
		List<String> expectedRules = Arrays.asList(
			"SFX r5 Y 12",
			"SFX r5 r ura [^u]r",
			"SFX r5 r dura [^u]r",
			"SFX r5 r ureta [^u]r",
			"SFX r5 r dureta [^u]r",
			"SFX r5 erò iura erò",
			"SFX r5 erò idura erò",
			"SFX r5 erò iureta erò",
			"SFX r5 erò idureta erò",
			"SFX r5 dur ur dur",
			"SFX r5 dur ureto dur",
			"SFX r5 dura ura dura",
			"SFX r5 dura ureta dura"
		);
		Assertions.assertEquals(expectedRules, rules);

		reducer.checkReductionCorrectness(flag, rules, originalRules, originalLines);
	}

	@Test
	public void simple12() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"LANG vec",
			"FLAG long",
			"SFX r4 Y 4",
			"SFX r4 r sion r",
			"SFX r4 xerò sion xerò",
			"SFX r4 lderò ƚusion lderò",
			"SFX r4 lverò ƚusion lverò"
		);
		Pair<RulesReducer, WordGenerator> pair = createReducer(affFile);
		RulesReducer reducer = pair.getLeft();
		WordGenerator wordGenerator = pair.getRight();
		String flag = "r4";
		List<String> words = Arrays.asList("solverò", "evolverò", "revolverò", "rexolverò", "solderò", "ƚegrar", "reaƚixar", "saƚuar", "veƚar", 
			"manipoƚar", "artikoƚar", "desimiƚar", "spekuƚar", "naturaƚixar", "saƚutar", "ƚegar", "ƚiberar", "mormoƚar", "ƚibarar", "vaƚutar",
			"saƚudar", "stroƚegar", "asimiƚar", "xeneraƚixar", "xenaraƚixar", "maƚedir", "ƚimitar", "emuƚar", "koƚaudar", "triboƚar", "gaƚixar",
			"paƚatixar", "turbuƚar", "deƚetar", "iƚuminar", "ƚokuir", "simuƚar", "ƚuminar", "troboƚar", "torboƚar", "peƚar", "skaƚinar", "ƚenir",
			"eƚevar", "ƚegaƚixar", "kapitoƚar", "steƚar", "kanseƚar", "stiƚar", "ƚamentar", "soƚevar", "strakoƚar", "staƚar", "stroƚogar",
			"vokaƚixar", "koƚar", "ƚevar", "baƚotar", "eƚexerò", "ƚexerò");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.map(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX))
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		List<LineEntry> expectedCompactedRules = Arrays.asList(
			new LineEntry("lderò", "ƚusion", "lderò", "solderò"),
			new LineEntry("lverò", "ƚusion", "lverò", Arrays.asList("solverò", "evolverò", "revolverò", "rexolverò")),
			new LineEntry("r", "sion", "r", Arrays.asList("ƚegrar", "reaƚixar", "saƚuar", "veƚar", "manipoƚar", "artikoƚar", "desimiƚar",
				"spekuƚar", "naturaƚixar", "saƚutar", "ƚegar", "ƚiberar", "mormoƚar", "ƚibarar", "vaƚutar", "saƚudar", "stroƚegar", "asimiƚar",
				"xeneraƚixar", "xenaraƚixar", "maƚedir", "ƚimitar", "emuƚar", "koƚaudar", "triboƚar", "gaƚixar", "paƚatixar", "turbuƚar", "deƚetar",
				"iƚuminar", "ƚokuir", "simuƚar", "ƚuminar", "troboƚar", "torboƚar", "peƚar", "skaƚinar", "ƚenir", "eƚevar", "ƚegaƚixar",
				"kapitoƚar", "steƚar", "kanseƚar", "stiƚar", "ƚamentar", "soƚevar", "strakoƚar", "staƚar", "stroƚogar", "vokaƚixar", "koƚar",
				"ƚevar", "baƚotar")),
			new LineEntry("xerò", "sion", "xerò", Arrays.asList("eƚexerò", "ƚexerò"))
		);
		Assertions.assertEquals(expectedCompactedRules, compactedRules);

		List<String> rules = reducer.convertFormat(flag, false, compactedRules);
		List<String> expectedRules = Arrays.asList(
			"SFX r4 Y 4",
			"SFX r4 r sion r",
			"SFX r4 xerò sion xerò",
			"SFX r4 lderò ƚusion lderò",
			"SFX r4 lverò ƚusion lverò"
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
