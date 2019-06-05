package unit731.hunspeller.parsers.dictionary;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.generators.WordGenerator;
import unit731.hunspeller.parsers.dictionary.vos.AffixEntry;
import unit731.hunspeller.services.FileHelper;
import unit731.hunspeller.services.SetHelper;


public class RulesReducerTest{

	/**
	[rem= o,add=[ʼ],cond= o,from=[koarto, kuinto, kuarto, sèsto, tèrso, tuto, tèrŧo, so, sto]]	=> [s, t, ŧ]
	[rem=do,add=[ʼ],cond=do,from=[nudo, komòdo, kuando]]														=> [d]

	[rem=  e,add=[ ʼ],cond=  e,from=[de, ge]]				=> [d, g]
	[rem= te,add=[ ʼ],cond= te,from=[frate]]				=> [t]
	[rem= me,add=[ ʼ],cond= me,from=[kome]]				=> [m]
	[rem=ove,add=[óʼ],cond=ove,from=[indove, adove]]	=> [v]

	[rem= a,add=[ʼ],cond= a,from=[senŧa, na, la, sensa]]	=> [s, ŧ, l, n]
	[rem=ra,add=[ʼ],cond=ra,from=[sora]]						=> [r]
	[rem=xa,add=[ʼ],cond=xa,from=[kaxa]]						=> [x]
	*/
	@Test
	public void case1() throws IOException{
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
			"SFX ʼ0 e ʼ [dg]e",
			"SFX ʼ0 o ʼ [^d]o",
			"SFX ʼ0 ove óʼ ove");
		Pair<RulesReducer, WordGenerator> pair = createReducer(affFile);
		RulesReducer reducer = pair.getLeft();
		WordGenerator wordGenerator = pair.getRight();
		String flag = "ʼ0";
		List<String> words = Arrays.asList("ge", "la", "na", "nu", "vu", "ge", "sto", "adove", "indove", "kome", "kuando", "tuto", "de", "so", "sora", "tèrŧo", "tèrso", "kuarto", "koarto", "kuinto", "sèsto", "par", "kaxa", "sensa", "senŧa", "komòdo", "frate", "nudo");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.flatMap(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX).stream())
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		Set<LineEntry> expectedCompactedRules = SetHelper.setOf(
			new LineEntry("r", "ʼ", "r", Arrays.asList("par")),
			new LineEntry("u", "ʼ", "u", Arrays.asList("nu", "vu")),
			new LineEntry("ra", "ʼ", "ra", Arrays.asList("sora")),
			new LineEntry("xa", "ʼ", "xa", Arrays.asList("kaxa")),
			new LineEntry("me", "ʼ", "me", Arrays.asList("kome")),
			new LineEntry("te", "ʼ", "te", Arrays.asList("frate")),
			new LineEntry("do", "ʼ", "do", Arrays.asList("nudo", "komòdo", "kuando")),
			new LineEntry("a", "ʼ", "[^rx]a", Arrays.asList("senŧa", "na", "la", "sensa")),
			new LineEntry("e", "ʼ", "[^mtv]e", Arrays.asList("de", "ge")),
			new LineEntry("o", "ʼ", "[^d]o", Arrays.asList("koarto", "kuinto", "kuarto", "sèsto", "tèrso", "tuto", "tèrŧo", "so", "sto")),
			new LineEntry("ove", "óʼ", "ove", Arrays.asList("indove", "adove"))
		);
		Assertions.assertEquals(expectedCompactedRules, new HashSet<>(compactedRules));

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
			"SFX ʼ0 e ʼ [^mtv]e",
			"SFX ʼ0 o ʼ [^d]o",
			"SFX ʼ0 ove óʼ ove"
		);
		Assertions.assertEquals(expectedRules, rules);

		reducer.checkReductionCorrectness(flag, rules, originalRules, originalLines);
	}

	/**
	[rem=  o,add=[  ato],cond=  o,from=[ƚibro, moƚo, roxiñoƚo, kaƚandro, rosiñoƚo, xeƚo, ruxiñoƚo, rusiñoƚo]]	=> [r, ƚ]
	[rem=èƚo,add=[eƚato],cond=èƚo,from=[kapèƚo, vedèƚo, kanèƚo]]																=> [ƚ]

	[rem=  0,add=[   ta],cond=  a,from=[kaƚandra]]			=> [r]
	[rem=èƚa,add=[eƚata],cond=èƚa,from=[kapèƚa, vedèƚa]]	=> [ƚ]
	*/
	@Test
	public void case2() throws IOException{
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
		List<String> words = Arrays.asList("kanèƚo", "kapèƚa", "kapèƚo", "ƚibro", "vedèƚa", "vedèƚo", "moƚo", "rosiñoƚo", "roxiñoƚo", "kaƚandra", "kaƚandro", "xeƚo", "rusiñoƚo", "ruxiñoƚo");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.flatMap(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX).stream())
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		Set<LineEntry> expectedCompactedRules = SetHelper.setOf(
			new LineEntry("0", "ta", "[^ƚ]a", "kaƚandra"),
			new LineEntry("o", "ato", "[^ƚ]o", Arrays.asList("ƚibro", "kaƚandro")),
			new LineEntry("èƚa", "eƚata", "èƚa", Arrays.asList("kapèƚa", "vedèƚa")),
			new LineEntry("èƚo", "eƚato", "èƚo", Arrays.asList("kapèƚo", "vedèƚo", "kanèƚo")),
			new LineEntry("o", "ato", "[^è]ƚo", Arrays.asList("moƚo", "roxiñoƚo", "rosiñoƚo", "xeƚo", "ruxiñoƚo", "rusiñoƚo"))
		);
		Assertions.assertEquals(expectedCompactedRules, new HashSet<>(compactedRules));

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

	/**
	[rem=  o,add=[  ato],cond=  o,from=[verdo, mando, viŧio, savio, speso, kalandro, vexo, konto, granfo, solfro, libro, đilio, ŧoko, porko,
		ŧedro, bosko, manđo, soko, sorgo, visio, muso, borso, manxo, kuadro, sporko]]	=> [d, f, g, i, k, r, s, t, x, đ]
	[rem=òbo,add=[obato],cond=òbo,from=[gòbo]]													=> [b]
	[rem=òvo,add=[ovato],cond=òvo,from=[òvo]]														=> [v]
	[rem=òko,add=[okato],cond=òko,from=[pòko, òko]]												=> [k]
	[rem=òmo,add=[omato],cond=òmo,from=[òmo]]														=> [m]

	[rem=  0,add=[   ta],cond=  a,from=[kalandra, kora, maca, savia, aria, inkuixitora, marenda, kuadra, inspetora, toxa, grada, merenda,
		kara, fatora]]													=> [c, d, i, r, x]
	[rem=òsa,add=[osata],cond=òsa,from=[kòsa]]				=> [s]
	[rem=òka,add=[okata],cond=òka,from=[òka, pòka]]			=> [k]
	[rem=òba,add=[obata],cond=òba,from=[gòba, ròba]]		=> [b]
	[rem=èla,add=[elata],cond=èla,from=[kapèla, vedèla]]	=> [l]
	[rem=òna,add=[onata],cond=òna,from=[dòna]]				=> [n]

	[rem= 0,add=[  ato],cond= l,from=[rusiñol, ruxiñol, rosiñol, mol, đeneral, roxiñol, xel]]	=> [a, e, o]
	[rem=èl,add=[elato],cond=èl,from=[vedèl, kanèl, kapèl]]												=> [è]
	*/
	@Test
	public void case3() throws IOException{
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
		List<String> words = Arrays.asList("aria", "bar", "baron", "bon", "borso", "bosko", "dixnar", "dòna", "fakin", "far", "fator", "fatora", "grada", "granfo", "gòba", "gòbo", "inkuixitor", "inkuixitora", "inspetor", "inspetora", "kalandra", "kalandro", "kanèl", "kapèl", "kapèla", "kara", "kojon", "konto", "kora", "koɉon", "kuadra", "kuadro", "kòsa", "libro", "maca", "mando", "manxo", "manđo", "marenda", "merenda", "mol", "muso", "padron", "paron", "patron", "pecenin", "pesenin", "peŧenin", "porko", "pòka", "pòko", "rexon", "rosiñol", "roxiñol", "rusiñol", "ruxiñol", "ròba", "savia", "savio", "sen", "sinsin", "soko", "solfro", "sorgo", "speso", "sporko", "tabar", "toxa", "vedèl", "vedèla", "verdo", "vesin", "vexin", "vexo", "veŧin", "visio", "viŧio", "xbir", "xel", "òka", "òko", "òmo", "òvo", "đeneral", "đilio", "ŧedro", "ŧinŧin", "ŧoko");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.flatMap(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX).stream())
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		Set<LineEntry> expectedCompactedRules = SetHelper.setOf(
			new LineEntry("0", "ato", "[nr]", Arrays.asList("verdo", "mando", "viŧio", "savio", "speso", "kalandro", "vexo", "konto", "granfo", "solfro", "libro", "đilio", "ŧoko", "porko", "ŧedro", "bosko", "manđo", "soko", "sorgo", "visio", "muso", "borso", "manxo", "kuadro", "sporko")),
			new LineEntry("èl", "elato", "èl", Arrays.asList("vedèl", "kanèl", "kapèl")),
			new LineEntry("0", "ta", "[^bklns]a", Arrays.asList("kalandra", "kora", "maca", "savia", "aria", "inkuixitora", "marenda", "kuadra", "inspetora", "toxa", "grada", "merenda", "kara", "fatora")),
			new LineEntry("0", "ato", "[^è]l", Arrays.asList("rusiñol", "ruxiñol", "rosiñol", "mol", "đeneral", "roxiñol", "xel")),
			new LineEntry("o", "ato", "[^bkmv]o", Arrays.asList("verdo", "mando", "viŧio", "savio", "speso", "kalandro", "vexo", "konto", "granfo", "solfro", "libro", "đilio", "ŧoko", "porko", "ŧedro", "bosko", "manđo", "soko", "sorgo", "visio", "muso", "borso", "manxo", "kuadro", "sporko")),
			new LineEntry("òba", "obata", "òba", Arrays.asList("gòba", "ròba")),
			new LineEntry("òka", "okata", "òka", Arrays.asList("òka", "pòka")),
			new LineEntry("èla", "elata", "èla", Arrays.asList("kapèla", "vedèla")),
			new LineEntry("òna", "onata", "òna", "dòna"),
			new LineEntry("òsa", "osata", "òsa", "kòsa"),
			new LineEntry("òbo", "obato", "òbo", "gòbo"),
			new LineEntry("òko", "okato", "òko", Arrays.asList("pòko", "òko")),
			new LineEntry("òmo", "omato", "òmo", "òmo"),
			new LineEntry("òvo", "ovato", "òvo", "òvo"),
			new LineEntry("o", "ato", "[^ò]ko", Arrays.asList("bosko", "soko", "ŧoko", "porko", "sporko"))
		);
		Assertions.assertEquals(expectedCompactedRules, new HashSet<>(compactedRules));

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

	/**
	[rem= 0,add=[  ista],cond= r,from=[folar, koƚor, foƚar, spiƚorsar, kolor]]	=> [a, o]
	[rem=èr,add=[erista],cond=èr,from=[bregièr, bragièr]]								=> [è]

	[rem=   a,add=[  ista],cond=   a,from=[fegura, figura, ŧifra, bonba, stua, kitara]]				=> [r, b, u]
	[rem=  ía,add=[  ista],cond=  ía,from=[finoxomía, fiƚoxomía, alkimía, arkimía, filoxomía]]	=> [í]
	[rem= òda,add=[odista],cond= òda,from=[mòda]]																=> [d]
	[rem=ònia,add=[onista],cond=ònia,from=[ŧerimònia]]															=> [i]

	[rem=o,add=[ista],cond=o,from=[fogo, konto, paƚaso, palaŧo, kaƚo, palaso, paƚaseto, kaxo]]	=> [s, t, g, ŧ, x, ƚ]
	[rem=o,add=[ sta],cond=o,from=[kanbio]]																		=> [i]

	[rem=o,add=[sta],cond=o,from=[kanbio]]	=> [i]
	*/
	@Test
	public void case4() throws IOException{
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
		List<String> words = Arrays.asList("folar", "foƚar", "spiƚorsar", "kaxo", "arte", "bonba", "dente", "dornal", "đornal", "fegura", "fogo", "kal", "kaƚo", "kapital", "kapitaƚe", "kolor", "koƚor", "lexe", "ƚexe", "mòda", "paexe", "palaso", "palaŧo", "paƚaseto", "paƚaso", "real", "reaƚe", "stua", "xornal", "xornaƚe", "bragièr", "figura", "boridon", "filoxomía", "fiƚoxomía", "alarme", "alkimía", "aƚarme", "arkimía", "bonton", "finoxomía", "kanbio", "kitara", "konto", "ŧerimònia", "ŧifra", "bregièr");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.flatMap(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX).stream())
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		Set<LineEntry> expectedCompactedRules = SetHelper.setOf(
			new LineEntry("e", "ista", "e", Arrays.asList("kapitaƚe", "alarme", "ƚexe", "lexe", "paexe", "xornaƚe", "aƚarme", "reaƚe", "dente", "arte")),
			new LineEntry("òda", "odista", "òda", "mòda"),
			new LineEntry("ía", "ista", "ía", Arrays.asList("finoxomía", "fiƚoxomía", "alkimía", "arkimía", "filoxomía")),
			new LineEntry("èr", "erista", "èr", Arrays.asList("bregièr", "bragièr")),
			new LineEntry("ònia", "onista", "ònia", "ŧerimònia"),
			new LineEntry("0", "ista", "[ln]", Arrays.asList("dornal", "kal", "bonton", "đornal", "xornal", "real", "boridon", "kapital")),
			new LineEntry("o", "ista", "[^i]o", Arrays.asList("fogo", "konto", "paƚaso", "palaŧo", "kaƚo", "palaso", "paƚaseto", "kaxo")),
			new LineEntry("o", "sta", "io", "kanbio"),
			new LineEntry("a", "ista", "[^dií]a", Arrays.asList("fegura", "figura", "ŧifra", "bonba", "stua", "kitara")),
			new LineEntry("0", "ista", "[^è]r", Arrays.asList("folar", "koƚor", "foƚar", "spiƚorsar", "kolor"))
		);
		Assertions.assertEquals(expectedCompactedRules, new HashSet<>(compactedRules));

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

	/**
	[rem= 0,add=[ía, ieta],                  cond= r,from=[banpor, pistor, adorator, sixor, señor, asesor, sior, ŧixor, tentor, trator, tintor, fator, siñor]]	=> [o]
	[rem=ar,add=[arieta, erieta, aría, ería],cond=ar,from=[axenar, sporkar, portar, fatucar, komensar, kojonbarar, koɉonbarar, kavalar, barar, komensiar, ...]]	=> [a]
	[rem=èr,add=[arieta, erieta, aría, ería],cond=èr,from=[kalegèr, sensèr, marŧèr, muscèr, masèr, bekèr, persegèr, ostèr, speŧièr, saƚegèr, maŧèr, ...]]	=> [è]

	[rem=   a,add=[erieta, arieta, aría, ería],        cond=   a,from=[skovasa, butiƚia, skovaŧa, strasa, ...]]	=> [d, ŧ, g, i, ɉ, j, k, o, đ, ñ, r, s, t, v, x]
	[rem= èla,add=[elería, elarieta, elerieta, elaría],cond= èla,from=[kasèla, kaxèla]]									=> [l]
	[rem= èƚa,add=[eƚarieta, eƚaría, eƚerieta, eƚería],cond= èƚa,from=[kasèƚa, kaxèƚa]]									=> [ƚ]
	[rem=ería,add=[arieta, erieta, aría],              cond=ería,from=[supercería, konetrería, pelatería, ...]]	=> [í]

	[rem=  o,add=[erieta, arieta, aría, ería],        cond=  o,from=[furbo, ƚadro, ludro, storno, rajonato, griƚo, koro, ...]]	=> [b, r, t, d, v, g, x, i, ƚ, k, m, n]
	[rem=òko,add=[okería, okaría, okerieta, okarieta],cond=òko,from=[siòko]]																	=> [k]
	[rem=èđo,add=[eđería, eđaría, eđerieta, eđarieta],cond=èđo,from=[mèđo]]																		=> [đ]
	[rem=èxo,add=[exería, exaría, exerieta, exarieta],cond=èxo,from=[mèxo, pièxo]]															=> [x]
	[rem=òco,add=[ocaría, ocerieta, ocarieta, ocería],cond=òco,from=[pedòco, peòco, piòco]]												=> [c]
	[rem=òdo,add=[odarieta, odaría, oderieta, odería],cond=òdo,from=[còdo]]																		=> [d]
	[rem=èdo,add=[ederieta, edarieta, edería, edaría],cond=èdo,from=[mèdo]]																		=> [d]
	*/
	@Test
	public void case5() throws IOException{
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
		List<String> words = Arrays.asList("albergar", "ardentar", "arđentar", "arxentar", "axenar", "bañar", "barar", "berekinar", "berikinar", "bibiar", "birar", "birbantar", "bonbar", "bufonar", "cakolar", "cakoƚar", "drapar", "strafantar", "fatucar", "rafinar", "fondar", "fornar", "fraskar", "garbar", "gardar", "garđar", "garxar", "grixonar", "guanto", "vanto", "jetar", "ɉetar", "kanselar", "kanŧelar", "kanseƚar", "kañar", "kapocar", "kastronar", "kavalar", "kavaƚar", "kojonar", "koɉonar", "kojonbarar", "koɉonbarar", "kokolar", "kokoƚar", "komandar", "komensar", "komenŧar", "komensiar", "komenŧiar", "kontar", "kontrolar", "kontroƚar", "koro", "kordar", "rekordar", "krokar", "kuadro", "ladrar", "ƚadrar", "lexinar", "ƚexinar", "lotar", "ƚotar", "mañar", "minconar", "nodar", "panetar", "peocar", "peskar", "piedar", "pieđar", "piexar", "pièxo", "spilorŧar", "spilorsar", "piocar", "pitokar", "poltronar", "pomo", "porkar", "portar", "putelar", "puteƚar", "retelar", "reteƚar", "robar", "saonar", "senpiar", "skorsar", "skorŧar", "soldar", "sovercar", "spesiar", "speŧiar", "spisiar", "spiŧiar", "sporkar", "isporkar", "stanpar", "stranbar", "strasar", "straŧar", "striar", "strigar", "takonar", "tapesar", "tapeŧar", "tartufolar", "tartufoƚar", "sansar", "ŧimar", "simar", "vakar", "provedo", "vergexar", "xmorfiar", "mèdo", "mèđo", "mèxo", "kaxo", "bianka", "banko", "banpor", "batería", "bekèr", "còdo", "fresa", "freŧa", "kasèla", "kasèƚa", "kojon", "koɉon", "libro", "ƚibro", "muscèr", "pedòco", "peòco", "persegèr", "piòco", "polar", "poƚar", "prado", "salgèr", "sensèr", "siñor", "sior", "skoasa", "skoaŧa", "skovasa", "skovaŧa", "spesièr", "speŧièr", "strasa", "straŧa", "striga", "tintor", "trator", "vaka", "bira", "boletin", "boƚetin", "cetin", "dolfin", "fante", "fator", "forestería", "fraska", "furbo", "galante", "gaƚante", "goloxo", "goƚoxo", "kalegèr", "kaƚegèr", "kamarlengo", "konetrería", "ladro", "lata", "ludro", "ƚadro", "ƚata", "ƚudro", "marsèr", "marŧèr", "masèr", "nodara", "olivo", "oƚivo", "paregin", "parejin", "pareɉin", "pedante", "pistor", "raxente", "sekreto", "skorsèr", "skorŧèr", "skroa", "spada", "storno", "tentor", "teña", "birbante", "boèr", "boteja", "boteɉa", "ridikolería", "ridikoƚería", "artejería", "arteɉería", "artelería", "arteƚería", "kaxolería", "kaxoƚería", "kortexan", "ladron", "ƚadron", "merkandería", "ostèr", "pelatería", "peƚatería", "podestería", "poestería", "señor", "skrova", "skudo", "kaxèla", "kaxèƚa", "strion", "angería", "fiskal", "fiskaƚe", "mersería", "merŧería", "meseto", "momería", "palandería", "paƚandería", "panatería", "piskería", "desentería", "erbería", "fravo", "garda", "garđa", "garxa", "gril", "griƚo", "kaxolin", "kaxoƚin", "adorator", "asesor", "baldería", "balestrería", "baƚestrería", "bibioxo", "bixutería", "botilia", "botiƚia", "butilia", "butiƚia", "falkon", "galería", "gaƚería", "inbasería", "kafetería", "kakofonería", "kotería", "piavolería", "piavoƚería", "porko", "rajonato", "raɉonato", "salegèr", "saƚegèr", "santocería", "senpio", "siòko", "sixor", "skorería", "sovarcería", "spakon", "spiŧièr", "sporko", "stranbo", "supercería", "telería", "teƚería", "teñoxería", "tersería", "terŧería", "ŧaratan", "ŧibaldería", "ŧixor", "vetrería", "grixonería", "kordería", "ŧimexería", "citin", "sapientería", "segretería", "maŧèr", "gexo");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.flatMap(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX).stream())
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		Set<LineEntry> expectedCompactedRules = SetHelper.setOf(
			new LineEntry("ar", SetHelper.setOf("arieta", "aría", "ería", "erieta"), "ar", Arrays.asList("axenar", "sporkar", "portar", "fatucar", "komensar", "kojonbarar", "koɉonbarar", "kavalar", "barar", "komensiar", "strafantar", "peskar", "komenŧar", "takonar", "komandar", "kanŧelar", "arđentar", "kastronar", "arxentar", "drapar", "bonbar", "koɉonar", "rekordar", "spisiar", "kontar", "panetar", "tapeŧar", "kokoƚar", "strasar", "tartufolar", "retelar", "vergexar", "senpiar", "bibiar", "putelar", "pitokar", "berikinar", "saonar", "strigar", "kavaƚar", "striar", "straŧar", "polar", "cakoƚar", "lotar", "piocar", "kokolar", "isporkar", "garbar", "rafinar", "kanseƚar", "nodar", "kojonar", "stanpar", "garđar", "poltronar", "ƚexinar", "speŧiar", "spilorŧar", "soldar", "ɉetar", "minconar", "piexar", "poƚar", "jetar", "reteƚar", "peocar", "piedar", "skorsar", "garxar", "kontroƚar", "kanselar", "puteƚar", "albergar", "ardentar", "spilorsar", "kordar", "tapesar", "grixonar", "ƚotar", "sovercar", "xmorfiar", "stranbar", "cakolar", "kapocar", "kontrolar", "sansar", "spiŧiar", "ŧimar", "robar", "simar", "fraskar", "komenŧiar", "bañar", "birbantar", "bufonar", "porkar", "kañar", "mañar", "krokar", "ladrar", "skorŧar", "fornar", "birar", "fondar", "pieđar", "ƚadrar", "gardar", "lexinar", "berekinar", "tartufoƚar", "vakar", "spesiar")),
			new LineEntry("èla", SetHelper.setOf("elaría", "elería", "elarieta", "elerieta"), "èla", Arrays.asList("kasèla", "kaxèla")),
			new LineEntry("òco", SetHelper.setOf("ocarieta", "ocería", "ocerieta", "ocaría"), "òco", Arrays.asList("pedòco", "peòco", "piòco")),
			new LineEntry("èđo", SetHelper.setOf("eđarieta", "eđaría", "eđería", "eđerieta"), "èđo", "mèđo"),
			new LineEntry("èxo", SetHelper.setOf("exerieta", "exería", "exaría", "exarieta"), "èxo", Arrays.asList("mèxo", "pièxo")),
			new LineEntry("òko", SetHelper.setOf("okarieta", "okería", "okaría", "okerieta"), "òko", "siòko"),
			new LineEntry("ería", SetHelper.setOf("arieta", "aría", "erieta"), "ería", Arrays.asList("supercería", "konetrería", "pelatería", "poestería", "baldería", "kafetería", "angería", "mersería", "tersería", "galería", "ŧibaldería", "kaxolería", "peƚatería", "erbería", "sovarcería", "segretería", "kotería", "teƚería", "artejería", "momería", "batería", "inbasería", "piavoƚería", "paƚandería", "kordería", "sapientería", "terŧería", "teñoxería", "ŧimexería", "bixutería", "forestería", "kakofonería", "ridikoƚería", "artelería", "podestería", "baƚestrería", "grixonería", "piskería", "balestrería", "telería", "arteƚería", "ridikolería", "panatería", "piavolería", "santocería", "vetrería", "gaƚería", "desentería", "arteɉería", "kaxoƚería", "palandería", "merkandería", "merŧería", "skorería")),
			new LineEntry("èƚa", SetHelper.setOf("eƚaría", "eƚerieta", "eƚería", "eƚarieta"), "èƚa", Arrays.asList("kasèƚa", "kaxèƚa")),
			new LineEntry("òdo", SetHelper.setOf("odaría", "oderieta", "odería", "odarieta"), "òdo", "còdo"),
			new LineEntry("èr", SetHelper.setOf("arieta", "aría", "ería", "erieta"), "èr", Arrays.asList("kalegèr", "sensèr", "marŧèr", "muscèr", "masèr", "bekèr", "persegèr", "ostèr", "speŧièr", "saƚegèr", "maŧèr", "skorŧèr", "spiŧièr", "kaƚegèr", "marsèr", "salgèr", "skorsèr", "spesièr", "boèr", "salegèr")),
			new LineEntry("èdo", SetHelper.setOf("edería", "ederieta", "edarieta", "edaría"), "èdo", "mèdo"),
			new LineEntry("e", SetHelper.setOf("arieta", "aría", "ería", "erieta"), "e", Arrays.asList("raxente", "galante", "gaƚante", "birbante", "fiskaƚe", "fante", "pedante")),
			new LineEntry("0", SetHelper.setOf("arieta", "aría", "ería", "erieta"), "[ln]", Arrays.asList("strion", "kaxoƚin", "kortexan", "kaxolin", "boƚetin", "fiskal", "falkon", "ŧaratan", "kojon", "ladron", "spakon", "cetin", "pareɉin", "citin", "dolfin", "koɉon", "boletin", "paregin", "ƚadron", "parejin", "gril")),
			new LineEntry("o", SetHelper.setOf("arieta", "aría", "ería", "erieta"), "[^cdđkx]o", Arrays.asList("meseto", "libro", "furbo", "ƚadro", "olivo", "ƚudro", "oƚivo", "ludro", "storno", "rajonato", "griƚo", "vanto", "ƚibro", "koro", "kamarlengo", "ladro", "pomo", "raɉonato", "guanto", "sekreto", "stranbo", "senpio", "fravo", "kuadro")),
			new LineEntry("a", SetHelper.setOf("arieta", "aría", "ería", "erieta"), "[^ílƚ]a", Arrays.asList("skovasa", "butiƚia", "skovaŧa", "strasa", "garda", "ƚata", "lata", "skoaŧa", "garđa", "botiƚia", "fraska", "skrova", "bira", "garxa", "freŧa", "butilia", "botilia", "striga", "spada", "skroa", "skoasa", "nodara", "fresa", "vaka", "teña", "boteja", "bianka", "boteɉa", "straŧa")),
			new LineEntry("0", SetHelper.setOf("ieta", "ía"), "[^aè]r", Arrays.asList("banpor", "pistor", "adorator", "sixor", "señor", "asesor", "sior", "ŧixor", "tentor", "trator", "tintor", "fator", "siñor")),
			new LineEntry("o", SetHelper.setOf("arieta", "aría", "ería", "erieta"), "[^èò]do", Arrays.asList("provedo", "prado", "skudo")),
			new LineEntry("o", SetHelper.setOf("arieta", "aría", "ería", "erieta"), "[^è]xo", Arrays.asList("goloxo", "gexo", "bibioxo", "goƚoxo", "kaxo")),
			new LineEntry("o", SetHelper.setOf("arieta", "aría", "ería", "erieta"), "[^ò]ko", Arrays.asList("banko", "porko", "sporko"))
		);
		Assertions.assertEquals(expectedCompactedRules, new HashSet<>(compactedRules));

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
		Assertions.assertEquals(expectedRules, rules);

		reducer.checkReductionCorrectness(flag, rules, originalRules, originalLines);
	}

	@Test
	public void case6() throws IOException{
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
			.flatMap(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX).stream())
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		Set<LineEntry> expectedCompactedRules = SetHelper.setOf(
			new LineEntry("òmo", "omixmo", "òmo", "gaƚantòmo"),
			new LineEntry("ía", "ixmo", "ía", "maƚinkonía"),
			new LineEntry("0", "ixmo", "r", "ƚuminar")
		);
		Assertions.assertEquals(expectedCompactedRules, new HashSet<>(compactedRules));

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
	public void case7() throws IOException{
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
		List<String> words = Arrays.asList("malinkonía", "bigòto", "galantòmo", "pitòko", "bigòto", "baron", "kokon", "konpar", "luminar", "franŧexa", "fransexa");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.flatMap(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX).stream())
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		Set<LineEntry> expectedCompactedRules = SetHelper.setOf(
			new LineEntry("òmo", "omixmo", "òmo", "galantòmo"),
			new LineEntry("òko", "okixmo", "òko", "pitòko"),
			new LineEntry("òto", "otixmo", "òto", "bigòto"),
			new LineEntry("ía", "ixmo", "ía", "malinkonía"),
			new LineEntry("0", "ixmo", "[nr]", Arrays.asList("baron", "kokon", "konpar", "luminar")),
			new LineEntry("a", "ixmo", "[^í]a", Arrays.asList("franŧexa", "fransexa"))
		);
		Assertions.assertEquals(expectedCompactedRules, new HashSet<>(compactedRules));

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
	public void case8() throws IOException{
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
		List<String> words = Arrays.asList("ƚargar", "boƚar", "noƚixar", "noƚexar", "spigoƚar", "ƚustrar", "sesoƚar", "kalkoƚar", "ƚavorar", "iƚuminar", "piƚar", "regoƚar", "kaƚibrar", "señaƚar", "oxeƚar", "kriveƚar", "saƚixar", "ventiƚar", "ƚuminar", "aƚienar", "ƚexixlar", "triveƚar", "spekuƚar", "garbeƚar", "ƚibar", "paƚar", "koƚorir", "ƚigar", "siaƚakuar", "mormoƚar", "ƚikar", "soƚesitar", "skarpeƚar", "ƚaorar", "foƚar", "stroƚegar", "spoƚar", "stroƚogar", "baƚar", "fiƚar", "koƚar", "saƚar", "ƚevar", "baƚotar", "ƚavar");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.flatMap(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX).stream())
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		List<LineEntry> expectedCompactedRules = Arrays.asList(
			new LineEntry("r", SetHelper.setOf("oreto", "toreto", "dora", "doreta", "ora", "doreto", "tor", "toreta", "oreta", "or", "tora", "dor"), "r", Arrays.asList("ƚargar", "boƚar", "noƚixar", "noƚexar", "spigoƚar", "ƚustrar", "sesoƚar", "kalkoƚar", "ƚavorar", "iƚuminar", "piƚar", "regoƚar", "kaƚibrar", "señaƚar", "oxeƚar", "kriveƚar", "saƚixar", "ventiƚar", "ƚuminar", "aƚienar", "ƚexixlar", "triveƚar", "spekuƚar", "garbeƚar", "ƚibar", "paƚar", "koƚorir", "ƚigar", "siaƚakuar", "mormoƚar", "ƚikar", "soƚesitar", "skarpeƚar", "ƚaorar", "foƚar", "stroƚegar", "spoƚar", "stroƚogar", "baƚar", "fiƚar", "koƚar", "saƚar", "ƚevar", "baƚotar", "ƚavar"))
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
	public void case9() throws IOException{
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
		List<String> words = Arrays.asList("reŧevidor", "reŧeidor", "stridor", "resevidor", "reseidor", "fondarò", "batarò", "duxarò", "vendarò", "kredarò", "provedarò", "bevarò", "resevidora", "reseidora", "bratadora", "feridora", "reŧevidora", "reŧeidora", "dopiar", "kavar", "armar", "soleŧitar", "sadar", "trionfar", "kojonbarar", "pasar", "strologar", "dorar", "salar", "arar", "barar", "rengar", "ligar", "partegar", "bevarar", "destramedar", "kolorir", "spekular", "tajar", "guidar", "paŧifegar", "ventilar", "komandar", "noledar", "inpaɉar", "tubiar", "barkar", "spolar", "sopresar", "sagurar", "suxerir", "filar", "karexar", "stokar", "taɉar", "xjonfar", "mendar", "remar", "sostentar", "mexar", "sperdurar", "faitar", "versar", "sprexurar", "alienar", "trebiar", "kronpar", "suɉerir", "xogar", "kuantifegar", "inbiankar", "garbelar", "audir", "palar", "studiar", "renkurar", "renfreskar", "vixitar", "fiokar", "međar", "mexurar", "befar", "kaređar", "kalŧar", "desturbar", "botiđar", "kurar", "falsifegar", "spigolar", "kalkar", "stronđar", "bitar", "brunir", "xugar", "kartar", "kalsar", "raxar", "trufar", "sujarir", "koniar", "vanpar", "garđar", "bufar", "salidar", "semenar", "danexar", "sekurar", "minconar", "piantar", "kuadrar", "nolixar", "krivelar", "tarixar", "stuŧegar", "infrandar", "krear", "inbotir", "strondar", "setar", "travaxar", "buratar", "akuxar", "sparxurar", "menar", "kopar", "kuniar", "mediar", "dupiar", "skansar", "botonar", "suɉarir", "oxelar", "goernar", "pasifegar", "solesitar", "ingraviar", "saltar", "mentir", "servar", "kastrar", "parar", "segurar", "konfortar", "mokar", "sekar", "kalibrar", "sorar", "medar", "pionar", "kaŧar", "bufonar", "xɉonfar", "karedar", "mormolar", "đontar", "kasar", "takar", "predegar", "mañar", "sesolar", "luminar", "konŧar", "koñar", "stimar", "xbrufar", "paisar", "kantar", "ŧarlar", "fondar", "sarlar", "defamar", "fornir", "gardar", "botidar", "ŧetar", "kondurar", "banpar", "partir", "redar", "menestrar", "xontar", "konsar", "inpajar", "portar", "rekamar", "inpinir", "infranđar", "governar", "koɉonbarar", "sitar", "sarvar", "aldir", "sperxurar", "dugar", "pagar", "pisar", "sapar", "nolexar", "tokar", "saxar", "tirar", "likar", "peskar", "kalkolar", "gabar", "tornir", "ŧernir", "kuñar", "rascar", "xgrafiñar", "istuar", "sfroxar", "spaŧar", "dimandar", "stivar", "apaltar", "borar", "lavorar", "levar", "konđurar", "destramexar", "saldar", "varar", "mirar", "stusegar", "koɉonar", "noliđar", "sagomar", "operar", "kontar", "kaveŧar", "mormorar", "konsumar", "vantar", "cacarar", "sperđurar", "papar", "montar", "tamixar", "murar", "tariđar", "urtar", "kavesar", "argomentar", "libar", "spasar", "fiabar", "sikurar", "strisar", "kargar", "misiar", "pilar", "piŧar", "prokurar", "skarpelar", "salixar", "ŧerkar", "lustrar", "forar", "balar", "purgar", "sađar", "radar", "spedir", "dontar", "rafinar", "ŧapar", "ŧitar", "kojonar", "stanpar", "spredurar", "estimar", "serkar", "bruskar", "noar", "pianar", "subarendar", "fiorir", "stronxar", "lavar", "sajar", "spređurar", "rear", "braŧar", "pontar", "kapar", "springar", "beverar", "fregar", "ordir", "brasar", "petenar", "siegar", "garxar", "pekar", "xbuxar", "kaminar", "laorar", "cetar", "đogar", "sigurar", "saɉar", "gomiar", "bolar", "maxenar", "formar", "trivelar", "suxarir", "balotar", "biastemar", "nolidar", "ŧanŧar", "kolar", "supiar", "sialakuar", "infiar", "noleđar", "sernir", "varsar", "granir", "pertegar", "trovar", "rinfreskar", "matar", "sugar", "andar", "segar", "sansar", "konxurar", "iluminar", "sujerir", "botixar", "mankar", "ŧimar", "petar", "sonar", "examinar", "largar", "señalar", "guarnir", "simar", "saliđar", "guastar", "kagar", "refreskar", "lexixlar", "señar", "dogar", "minusar", "konprar", "inpastar", "trasinar", "regolar", "rostir", "dexeñar", "taridar", "skardar", "folar", "kalmar", "raspar", "butar", "đugar", "minuŧar", "strupiar", "infranxar", "destrameđar", "bosar", "frapar", "stuar", "mixurar", "ŧoetar", "manestrar", "testar", "kortegar", "maŧar", "soetar", "strolegar", "boŧar", "parlar", "sublokar", "guantar", "fortifegar", "fumar", "inganar", "negar", "masar", "stukar");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.flatMap(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX).stream())
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		Set<LineEntry> expectedCompactedRules = SetHelper.setOf(
			new LineEntry("dor", SetHelper.setOf("oreto", "toreto", "doreto", "tor", "or"), "dor", Arrays.asList("reŧevidor", "reŧeidor", "stridor", "resevidor", "reseidor")),
			new LineEntry("rò", SetHelper.setOf("oreto", "dora", "doreta", "ora", "toreto", "doreto", "tor", "toreta", "oreta", "or", "tora", "dor"), "rò", Arrays.asList("fondarò", "batarò", "duxarò", "vendarò", "kredarò", "provedarò", "bevarò")),
			new LineEntry("dora", SetHelper.setOf("doreta", "ora", "toreta", "oreta", "tora"), "dora", Arrays.asList("resevidora", "reseidora", "bratadora", "feridora", "reŧevidora", "reŧeidora")),
			new LineEntry("r", SetHelper.setOf("oreto", "dora", "doreta", "ora", "toreto", "doreto", "tor", "toreta", "oreta", "or", "tora", "dor"), "[^o]r", Arrays.asList("dopiar", "kavar", "armar", "soleŧitar", "sadar", "trionfar", "kojonbarar", "pasar", "strologar", "dorar", "salar", "arar", "barar", "rengar", "ligar", "partegar", "bevarar", "destramedar", "kolorir", "spekular", "tajar", "guidar", "paŧifegar", "ventilar", "komandar", "noledar", "inpaɉar", "tubiar", "barkar", "spolar", "sopresar", "sagurar", "suxerir", "filar", "karexar", "stokar", "taɉar", "xjonfar", "mendar", "remar", "sostentar", "mexar", "sperdurar", "faitar", "versar", "sprexurar", "alienar", "trebiar", "kronpar", "suɉerir", "xogar", "kuantifegar", "inbiankar", "garbelar", "audir", "palar", "studiar", "renkurar", "renfreskar", "vixitar", "fiokar", "međar", "mexurar", "befar", "kaređar", "kalŧar", "desturbar", "botiđar", "kurar", "falsifegar", "spigolar", "kalkar", "stronđar", "bitar", "brunir", "xugar", "kartar", "kalsar", "raxar", "trufar", "sujarir", "koniar", "vanpar", "garđar", "bufar", "salidar", "semenar", "danexar", "sekurar", "minconar", "piantar", "kuadrar", "nolixar", "krivelar", "tarixar", "stuŧegar", "infrandar", "krear", "inbotir", "strondar", "setar", "travaxar", "buratar", "akuxar", "sparxurar", "menar", "kopar", "kuniar", "mediar", "dupiar", "skansar", "botonar", "suɉarir", "oxelar", "goernar", "pasifegar", "solesitar", "ingraviar", "saltar", "mentir", "servar", "kastrar", "parar", "segurar", "konfortar", "mokar", "sekar", "kalibrar", "sorar", "medar", "pionar", "kaŧar", "bufonar", "xɉonfar", "karedar", "mormolar", "đontar", "kasar", "takar", "predegar", "mañar", "sesolar", "luminar", "konŧar", "koñar", "stimar", "xbrufar", "paisar", "kantar", "ŧarlar", "fondar", "sarlar", "defamar", "fornir", "gardar", "botidar", "ŧetar", "kondurar", "banpar", "partir", "redar", "menestrar", "xontar", "konsar", "inpajar", "portar", "rekamar", "inpinir", "infranđar", "governar", "koɉonbarar", "sitar", "sarvar", "aldir", "sperxurar", "dugar", "pagar", "pisar", "sapar", "nolexar", "tokar", "saxar", "tirar", "likar", "peskar", "kalkolar", "gabar", "tornir", "ŧernir", "kuñar", "rascar", "xgrafiñar", "istuar", "sfroxar", "spaŧar", "dimandar", "stivar", "apaltar", "borar", "lavorar", "levar", "konđurar", "destramexar", "saldar", "varar", "mirar", "stusegar", "koɉonar", "noliđar", "sagomar", "operar", "kontar", "kaveŧar", "mormorar", "konsumar", "vantar", "cacarar", "sperđurar", "papar", "montar", "tamixar", "murar", "tariđar", "urtar", "kavesar", "argomentar", "libar", "spasar", "fiabar", "sikurar", "strisar", "kargar", "misiar", "pilar", "piŧar", "prokurar", "skarpelar", "salixar", "ŧerkar", "lustrar", "forar", "balar", "purgar", "sađar", "radar", "spedir", "dontar", "rafinar", "ŧapar", "ŧitar", "kojonar", "stanpar", "spredurar", "estimar", "serkar", "bruskar", "noar", "pianar", "subarendar", "fiorir", "stronxar", "lavar", "sajar", "spređurar", "rear", "braŧar", "pontar", "kapar", "springar", "beverar", "fregar", "ordir", "brasar", "petenar", "siegar", "garxar", "pekar", "xbuxar", "kaminar", "laorar", "cetar", "đogar", "sigurar", "saɉar", "gomiar", "bolar", "maxenar", "formar", "trivelar", "suxarir", "balotar", "biastemar", "nolidar", "ŧanŧar", "kolar", "supiar", "sialakuar", "infiar", "noleđar", "sernir", "varsar", "granir", "pertegar", "trovar", "rinfreskar", "matar", "sugar", "andar", "segar", "sansar", "konxurar", "iluminar", "sujerir", "botixar", "mankar", "ŧimar", "petar", "sonar", "examinar", "largar", "señalar", "guarnir", "simar", "saliđar", "guastar", "kagar", "refreskar", "lexixlar", "señar", "dogar", "minusar", "konprar", "inpastar", "trasinar", "regolar", "rostir", "dexeñar", "taridar", "skardar", "folar", "kalmar", "raspar", "butar", "đugar", "minuŧar", "strupiar", "infranxar", "destrameđar", "bosar", "frapar", "stuar", "mixurar", "ŧoetar", "manestrar", "testar", "kortegar", "maŧar", "soetar", "strolegar", "boŧar", "parlar", "sublokar", "guantar", "fortifegar", "fumar", "inganar", "negar", "masar", "stukar"))
		);
		Assertions.assertEquals(expectedCompactedRules, new HashSet<>(compactedRules));

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
	public void case10() throws IOException{
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
		List<String> words = Arrays.asList("baƚar", "boƚar", "doƚar", "fiƚar", "gaƚopar", "kaneƚar", "kavaƚar", "koƚar", "inkordeƚar", "kriveƚar", "ƚatar", "ƚavar", "ƚeskar", "ƚetarar", "ƚeterar", "ƚexenar", "ƚexixlar", "ƚigar", "ƚimar", "ƚuminar", "iƚuminar", "moƚar", "paƚar", "paƚetar", "peƚar", "saƚar", "skarseƚar", "stabiƚir", "soƚar", "vaƚir", "vixiƚar", "koƚadur", "stabeƚidura", "vaƚidur");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.flatMap(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX).stream())
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		Set<LineEntry> expectedCompactedRules = SetHelper.setOf(
			new LineEntry("dur", SetHelper.setOf("ureto", "ur"), "dur", Arrays.asList("koƚadur", "vaƚidur")),
			new LineEntry("dura", SetHelper.setOf("ureta", "ura"), "dura", "stabeƚidura"),
			new LineEntry("r", SetHelper.setOf("ureta", "ura", "dureta", "dura"), "[^u]r", Arrays.asList("ƚatar", "ƚeterar", "boƚar", "vaƚir", "ƚetarar", "iƚuminar", "moƚar", "doƚar", "kriveƚar", "kavaƚar", "ƚuminar", "kaneƚar", "ƚexixlar", "soƚar", "paƚar", "stabiƚir", "paƚetar", "peƚar", "ƚigar", "ƚimar", "inkordeƚar", "ƚexenar", "skarseƚar", "baƚar", "fiƚar", "koƚar", "saƚar", "vixiƚar", "ƚeskar", "gaƚopar", "ƚavar"))
		);
		Assertions.assertEquals(expectedCompactedRules, new HashSet<>(compactedRules));

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
	public void case11() throws IOException{
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
		List<String> words = Arrays.asList("baƚar", "boƚar", "doƚar", "fiƚar", "gaƚopar", "kaneƚar", "kavaƚar", "koƚar", "inkordeƚar", "kriveƚar", "ƚatar", "ƚavar", "ƚeskar", "ƚetarar", "ƚeterar", "ƚexenar", "ƚexixlar", "ƚigar", "ƚimar", "ƚuminar", "iƚuminar", "moƚar", "paƚar", "paƚetar", "peƚar", "saƚar", "skarseƚar", "stabiƚir", "soƚar", "vaƚir", "vixiƚar", "koƚadur", "stabeƚidura", "vaƚidur", "fenderò", "teserò", "inprimerò", "sebaterò", "baterò", "kuxerò", "torderò", "koxerò", "torđerò", "sobaterò", "torxerò");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.flatMap(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX).stream())
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		Set<LineEntry> expectedCompactedRules = SetHelper.setOf(
			new LineEntry("dur", SetHelper.setOf("ur", "ureto"), "dur", Arrays.asList("madur", "traxeɉadur", "traxejadur", "tradadur", "trađadur", "kagadur", "traxadur", "tragadur", "ordadur", "koladur", "validur", "ordidur", "skortegadur")),
			new LineEntry("erò", SetHelper.setOf("idura", "iura", "iureta", "idureta"), "erò", Arrays.asList("fenderò", "teserò", "inprimerò", "sebaterò", "baterò", "kuxerò", "torderò", "koxerò", "torđerò", "sobaterò", "torxerò")),
			new LineEntry("dura", SetHelper.setOf("ureta", "ura"), "dura", Arrays.asList("bokadura", "inđinadura", "nadura", "sfendadura", "batadura", "resapadura", "strenxadura", "madura", "sobatidura", "bastidura", "skrivadura", "alberadura", "sprokadura", "sobatadura", "fredura", "introfregadura", "stabelidura", "tesadura", "sfendidura", "spakadura", "anadura", "reŧapadura", "strenđadura", "sabatadura", "albaradura", "proŧedura", "vertadura", "sperdadura", "korporadura", "fogonadura", "ponxadura", "sferdadura", "inxinadura", "prosedura", "indinadura", "strendadura")),
			new LineEntry("r", SetHelper.setOf("dura", "ureta", "dureta", "ura"), "[^u]r", Arrays.asList("dopiar", "kavar", "armar", "broɉar", "pasar", "pontedar", "forŧar", "salar", "sejar", "arar", "latar", "roxegar", "serar", "ardenar", "ligar", "kadenar", "forsar", "ingarđir", "inɉermar", "tajar", "dentar", "vixilar", "injermar", "mastegar", "seɉar", "ingropar", "inpaɉar", "brojar", "bonar", "vokar", "tragar", "filar", "leterar", "piŧegar", "kornar", "skurtar", "risar", "riŧar", "stokar", "taɉar", "bordar", "inkrespar", "mendar", "bojir", "farar", "alborar", "sakar", "inɉarmar", "indopionar", "palar", "paletar", "ponsar", "kalŧar", "inkordelar", "kurar", "infarinar", "bardar", "boɉir", "ŧeɉar", "kalkar", "stronđar", "brunir", "kartar", "incodar", "kalsar", "josar", "arborar", "garđar", "inkroxar", "remurcar", "joŧar", "injarmar", "fasar", "skuarsar", "dolar", "pisegar", "bragar", "ɉetar", "jetar", "molar", "ŧejar", "stortar", "skuarŧar", "krivelar", "inbotir", "strondar", "gomitar", "ponteđar", "ŧerpir", "mostar", "valir", "xbokar", "dupiar", "netar", "spasiar", "botonar", "tresar", "ingarxir", "letarar", "ɉosar", "ɉoŧar", "rusar", "ingraviar", "baronar", "travar", "ferar", "vomitar", "sivansar", "bastir", "kavalkar", "mokar", "sekar", "iriŧar", "sivanŧar", "inbriagar", "toxar", "piegar", "xgrafar", "puñar", "đontar", "kasar", "takar", "mañar", "limar", "luminar", "konŧar", "frixar", "invastir", "kaenar", "fornir", "gardar", "partir", "veriar", "xontar", "konsar", "rondar", "sfexar", "tenperar", "inpajar", "portar", "spalmar", "rekamar", "skarselar", "vansar", "foɉar", "musar", "arxenar", "kanelar", "skoar", "krepar", "kavalar", "sapar", "copar", "guxar", "bavar", "tirar", "rasar", "strukar", "spaŧar", "bekar", "spakar", "borar", "saldar", "pontexar", "rekordar", "serpir", "vanŧar", "tastar", "kaveŧar", "inkarnar", "papar", "montar", "solar", "kavesar", "xetar", "spasar", "stabilir", "vestir", "kargar", "inmaltar", "ŧercar", "arđenar", "stranŧir", "fojar", "manegar", "spinar", "balar", "inpajetar", "purgar", "bastonar", "stransir", "garbar", "rexentar", "sercar", "vergar", "inboxemar", "dontar", "rafinar", "ŧapar", "skotar", "lexenar", "kalŧinar", "bruskar", "ingardir", "pianar", "rebaltar", "stronxar", "lavar", "rear", "leskar", "vedriar", "braŧar", "sarar", "pontar", "kapar", "fregar", "ordir", "brasar", "petenar", "kinkar", "siegar", "garxar", "botar", "ingrespar", "postar", "inpaɉetar", "bolar", "maxenar", "speŧar", "spesar", "kolar", "kordar", "spaŧiar", "infiar", "granir", "ingaxiar", "skaldar", "andar", "segar", "iluminar", "breviar", "ŧimar", "inkorsar", "kuxinar", "guarnir", "preŧar", "simar", "raɉar", "fodrar", "pelar", "kagar", "inbokar", "lexixlar", "señar", "skorlar", "bañar", "bendar", "kalsinar", "bruxar", "rostir", "fiankar", "makar", "vardar", "galopar", "strupiar", "kuxir", "gonfiar", "testar", "kortegar", "presar", "skontrar", "morsegar", "inkamixar", "stekar", "rajar", "fumar", "skermar"))
		);
		Assertions.assertEquals(expectedCompactedRules, new HashSet<>(compactedRules));

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
	public void case12() throws IOException{
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
			"manipoƚar", "artikoƚar", "desimiƚar", "spekuƚar", "naturaƚixar", "saƚutar", "ƚegar", "ƚiberar", "mormoƚar", "ƚibarar", "vaƚutar", "saƚudar", "stroƚegar", "asimiƚar", "xeneraƚixar", "xenaraƚixar", "maƚedir", "ƚimitar", "emuƚar", "koƚaudar", "triboƚar", "gaƚixar", "paƚatixar", "turbuƚar", "deƚetar", "iƚuminar", "ƚokuir", "simuƚar", "ƚuminar", "troboƚar", "torboƚar", "peƚar", "skaƚinar", "ƚenir", "eƚevar", "ƚegaƚixar", "kapitoƚar", "steƚar", "kanseƚar", "stiƚar", "ƚamentar", "soƚevar", "strakoƚar", "staƚar", "stroƚogar", "vokaƚixar", "koƚar", "ƚevar", "baƚotar", "eƚexerò", "ƚexerò");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.flatMap(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX).stream())
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		Set<LineEntry> expectedCompactedRules = SetHelper.setOf(
			new LineEntry("lderò", "ƚusion", "lderò", "solderò"),
			new LineEntry("r", "sion", "r", Arrays.asList("ƚegrar", "reaƚixar", "saƚuar", "veƚar", "manipoƚar", "artikoƚar", "desimiƚar", "spekuƚar", "naturaƚixar", "saƚutar", "ƚegar", "ƚiberar", "mormoƚar", "ƚibarar", "vaƚutar", "saƚudar", "stroƚegar", "asimiƚar", "xeneraƚixar", "xenaraƚixar", "maƚedir", "ƚimitar", "emuƚar", "koƚaudar", "triboƚar", "gaƚixar", "paƚatixar", "turbuƚar", "deƚetar", "iƚuminar", "ƚokuir", "simuƚar", "ƚuminar", "troboƚar", "torboƚar", "peƚar", "skaƚinar", "ƚenir", "eƚevar", "ƚegaƚixar", "kapitoƚar", "steƚar", "kanseƚar", "stiƚar", "ƚamentar", "soƚevar", "strakoƚar", "staƚar", "stroƚogar", "vokaƚixar", "koƚar", "ƚevar", "baƚotar")),
			new LineEntry("xerò", "sion", "xerò", Arrays.asList("eƚexerò", "ƚexerò")),
			new LineEntry("lverò", "ƚusion", "lverò", Arrays.asList("solverò", "evolverò", "revolverò", "rexolverò"))
		);
		Assertions.assertEquals(expectedCompactedRules, new HashSet<>(compactedRules));

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

	@Test
	public void case13() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"LANG vec",
			"FLAG long",
			"SFX r3 Y 20",
			"SFX r3 àer asion àer",
			"SFX r3 ir sion uir",
			"SFX r3 r sion [^t]ar",
			"SFX r3 r sion [^u]ir",
			"SFX r3 merò nsion merò",
			"SFX r3 nerò xision nerò",
			"SFX r3 ñerò nsion ñerò",
			"SFX r3 erò ision terò",
			"SFX r3 r sion [^en]tar",
			"SFX r3 derò usion lderò",
			"SFX r3 derò sion nderò",
			"SFX r3 guerò sion guerò",
			"SFX r3 verò sion iverò",
			"SFX r3 verò usion lverò",
			"SFX r3 tar sion petar",
			"SFX r3 xerò sion [^r]xerò",
			"SFX r3 r sion [^p]etar",
			"SFX r3 orxerò uresion orxerò",
			"SFX r3 tar sion [vx]entar",
			"SFX r3 r sion [^vx]entar"
		);
		Pair<RulesReducer, WordGenerator> pair = createReducer(affFile);
		RulesReducer reducer = pair.getLeft();
		WordGenerator wordGenerator = pair.getRight();
		String flag = "r3";
		List<String> words = Arrays.asList("prexuponerò", "posponerò", "esponerò", "oponerò", "ponerò", "konponerò", "proponerò", "xustaponerò", "duxerò", "elexerò", "estraxerò", "lexerò", "faxerò", "korexerò", "aflixerò", "struxerò", "produxerò", "introduxerò", "destinguerò", "solderò", "kavar", "fermentar", "notar", "sastufar", "strologar", "inpedir", "aplikar", "exibir", "traxlokar", "komodar", "permutar", "suparar", "komunegar", "spekular", "kostipar", "velar", "destinar", "kondir", "eskorporar", "nudrir", "danar", "konmixerar", "tribolar", "vokar", "markar", "sagurar", "piñorar", "stelar", "presixar", "lamentar", "sonorixar", "fidar", "sostentar", "subordenar", "liberar", "oparar", "versar", "xenarar", "prosimar", "koniugar", "klasifegar", "kuantifegar", "tonar", "sklamar", "vixitar", "torefar", "mexurar", "koordenar", "konvokar", "bitar", "numarar", "reputar", "tradir", "satusfar", "putrefar", "insinuar", "intimar", "edukar", "sinserar", "peñorar", "naturalixar", "vendegar", "esterminar", "valutar", "sekurar", "ostentar", "ultimar", "frankar", "trobolar", "simular", "kolaudar", "termenar", "krear", "setar", "akuxar", "legar", "oblar", "rekuixir", "sindikar", "stilar", "soportar", "konmixarar", "verifegar", "opinar", "privar", "xenerar", "provar", "torbolar", "saludar", "servar", "perlustrar", "solevar", "parar", "ativar", "mutar", "segurar", "maledir", "autorixar", "provokar", "petir", "satisfar", "notifegar", "akuixir", "artikolar", "legalixar", "piegar", "mormolar", "alterar", "numerar", "ubigar", "luminar", "vibrar", "sorafar", "remunerar", "binar", "spetorar", "salutar", "ordenar", "partir", "redar", "sinsierar", "estermenar", "prevarikar", "trasformar", "realixar", "skriturar", "skorporar", "votar", "monir", "raprexar", "eskavar", "ministrar", "sitar", "sarvar", "saluar", "malversar", "skalinar", "terminar", "vasinar", "far", "desimilar", "vidimar", "ondar", "interogar", "augumentar", "emular", "strukar", "mansipar", "rasionar", "levar", "fenir", "malfar", "mirar", "palpitar", "deputar", "variar", "operar", "exborsar", "mormorar", "tesar", "konsumar", "tratar", "fetar", "salvar", "spesifegar", "xetar", "limitar", "depoxitar", "sikurar", "vestir", "munir", "legrar", "orar", "traversar", "pernotar", "identifegar", "radar", "dexertar", "rafinar", "asimilar", "obligar", "straxordenar", "rapatumar", "partesipar", "superar", "ostinar", "strakolar", "subarendar", "vokalixar", "fisar", "suplegar", "punir", "esklamar", "inkonbinar", "fregar", "turbular", "separar", "proibir", "kanselar", "cetar", "manipolar", "revokar", "sigurar", "filtrar", "supurar", "benedir", "formar", "balotar", "interpretar", "kolar", "xrenar", "stalar", "elevar", "varsar", "sodisfar", "finir", "kapitolar", "skaldar", "deletar", "proar", "panixar", "substentar", "legalidar", "iluminar", "kontaminar", "libarar", "inibir", "malvarsar", "examinar", "guarnir", "suporar", "pelar", "espurgar", "parir", "palatixar", "mortifegar", "kalsinar", "soporar", "tentar", "lenir", "nomenar", "exaltar", "exortar", "inkuixir", "rivar", "butar", "sperar", "mixurar", "senplifegar", "situar", "sistemar", "testar", "xmenbrar", "strolegar", "tranxar", "negar", "sorxerò", "prexentar", "exentar", "ventar", "sospetar", "repeterò", "estenderò", "fenderò", "sospenderò", "espanderò", "suspenderò", "tenderò", "asumerò", "prexumerò", "konsumerò", "koskriverò", "sotoskriverò", "skriverò", "iskriverò", "tràer", "estràer", "konstituir", "atribuir", "kostituir", "kostruir", "deminuir", "sostituir", "instruir", "destribuir", "diminuir", "lokuir", "konveñerò", "solverò", "evolverò", "revolverò", "rexolverò");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.flatMap(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX).stream())
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		Set<LineEntry> expectedCompactedRules = SetHelper.setOf(
			new LineEntry("nerò", "xision", "nerò", Arrays.asList("prexuponerò", "posponerò", "esponerò", "oponerò", "ponerò", "konponerò", "proponerò", "xustaponerò")),
			new LineEntry("merò", "nsion", "merò", Arrays.asList("asumerò", "prexumerò", "konsumerò")),
			new LineEntry("orxerò", "uresion", "orxerò", "sorxerò"),
			new LineEntry("ñerò", "nsion", "ñerò", "konveñerò"),
			new LineEntry("guerò", "sion", "guerò", "destinguerò"),
			new LineEntry("àer", "asion", "àer", Arrays.asList("tràer", "estràer")),
			new LineEntry("ir", "sion", "uir", Arrays.asList("konstituir", "atribuir", "kostituir", "kostruir", "deminuir", "sostituir", "instruir", "destribuir", "diminuir", "lokuir")),
			new LineEntry("r", "sion", "[^u]ir", Arrays.asList("proibir", "rekuixir", "monir", "kondir", "inkuixir", "benedir", "nudrir", "inibir", "tradir", "guarnir", "inpedir", "maledir", "exibir", "petir", "parir", "punir", "fenir", "akuixir", "partir", "vestir", "munir", "finir", "lenir")),
			new LineEntry("r", "sion", "[^t]ar", Arrays.asList("kavar", "sastufar", "strologar", "aplikar", "traxlokar", "komodar", "suparar", "komunegar", "spekular", "kostipar", "velar", "destinar", "eskorporar", "danar", "konmixerar", "tribolar", "vokar", "markar", "sagurar", "piñorar", "stelar", "presixar", "sonorixar", "fidar", "subordenar", "liberar", "oparar", "versar", "xenarar", "prosimar", "koniugar", "klasifegar", "kuantifegar", "tonar", "sklamar", "torefar", "mexurar", "koordenar", "konvokar", "numarar", "satusfar", "putrefar", "insinuar", "intimar", "edukar", "sinserar", "peñorar", "naturalixar", "vendegar", "esterminar", "sekurar", "ultimar", "frankar", "trobolar", "simular", "kolaudar", "termenar", "krear", "akuxar", "legar", "oblar", "sindikar", "stilar", "konmixarar", "verifegar", "opinar", "privar", "xenerar", "provar", "torbolar", "saludar", "servar", "perlustrar", "solevar", "parar", "ativar", "segurar", "autorixar", "provokar", "satisfar", "notifegar", "artikolar", "legalixar", "piegar", "mormolar", "alterar", "numerar", "ubigar", "luminar", "vibrar", "sorafar", "remunerar", "binar", "spetorar", "ordenar", "redar", "sinsierar", "estermenar", "prevarikar", "trasformar", "realixar", "skriturar", "skorporar", "raprexar", "eskavar", "ministrar", "sarvar", "saluar", "malversar", "skalinar", "terminar", "vasinar", "far", "desimilar", "vidimar", "ondar", "interogar", "emular", "strukar", "mansipar", "rasionar", "levar", "malfar", "mirar", "variar", "operar", "exborsar", "mormorar", "tesar", "konsumar", "salvar", "spesifegar", "sikurar", "legrar", "orar", "traversar", "identifegar", "radar", "rafinar", "asimilar", "obligar", "straxordenar", "rapatumar", "partesipar", "superar", "ostinar", "strakolar", "subarendar", "vokalixar", "fisar", "suplegar", "esklamar", "inkonbinar", "fregar", "turbular", "separar", "kanselar", "manipolar", "revokar", "sigurar", "filtrar", "supurar", "formar", "kolar", "xrenar", "stalar", "elevar", "varsar", "sodisfar", "kapitolar", "skaldar", "proar", "panixar", "legalidar", "iluminar", "kontaminar", "libarar", "malvarsar", "examinar", "suporar", "pelar", "espurgar", "palatixar", "mortifegar", "kalsinar", "soporar", "nomenar", "rivar", "sperar", "mixurar", "senplifegar", "situar", "sistemar", "xmenbrar", "strolegar", "tranxar", "negar")),
			new LineEntry("erò", "ision", "terò", "repeterò"),
			new LineEntry("r", "sion", "[^en]tar", Arrays.asList("exaltar", "exortar", "tratar", "pernotar", "soportar", "notar", "bitar", "sitar", "butar", "mutar", "reputar", "balotar", "limitar", "dexertar", "testar", "salutar", "depoxitar", "palpitar", "deputar", "permutar", "vixitar", "valutar", "votar")),
			new LineEntry("derò", "usion", "[^n]derò", "solderò"),
			new LineEntry("derò", "sion", "nderò", Arrays.asList("estenderò", "fenderò", "sospenderò", "espanderò", "suspenderò", "tenderò")),
			new LineEntry("xerò", "sion", "[^r]xerò", Arrays.asList("duxerò", "elexerò", "estraxerò", "lexerò", "faxerò", "korexerò", "aflixerò", "struxerò", "produxerò", "introduxerò")),
			new LineEntry("verò", "sion", "[^l]verò", Arrays.asList("koskriverò", "sotoskriverò", "skriverò", "iskriverò")),
			new LineEntry("verò", "usion", "lverò", Arrays.asList("solverò", "evolverò", "revolverò", "rexolverò")),
			new LineEntry("tar", "sion", "petar", "sospetar"),
			new LineEntry("r", "sion", "[^p]etar", Arrays.asList("deletar", "cetar", "xetar", "setar", "fetar", "interpretar")),
			new LineEntry("tar", "sion", "[^mt]entar", Arrays.asList("prexentar", "exentar", "ventar")),
			new LineEntry("r", "sion", "[mt]entar", Arrays.asList("substentar", "fermentar", "ostentar", "lamentar", "tentar", "sostentar", "augumentar"))
		);
		Assertions.assertEquals(expectedCompactedRules, new HashSet<>(compactedRules));

		List<String> rules = reducer.convertFormat(flag, false, compactedRules);
		List<String> expectedRules = Arrays.asList(
			"SFX r3 Y 20",
			"SFX r3 ir sion uir",
			"SFX r3 àer asion àer",
			"SFX r3 r sion [^t]ar",
			"SFX r3 r sion [^u]ir",
			"SFX r3 erò ision terò",
			"SFX r3 merò nsion merò",
			"SFX r3 nerò xision nerò",
			"SFX r3 ñerò nsion ñerò",
			"SFX r3 r sion [^en]tar",
			"SFX r3 tar sion petar",
			"SFX r3 derò sion nderò",
			"SFX r3 verò usion lverò",
			"SFX r3 guerò sion guerò",
			"SFX r3 r sion [^p]etar",
			"SFX r3 derò usion [^n]derò",
			"SFX r3 verò sion [^l]verò",
			"SFX r3 xerò sion [^r]xerò",
			"SFX r3 orxerò uresion orxerò",
			"SFX r3 r sion [mt]entar",
			"SFX r3 tar sion [^mt]entar"
		);
		Assertions.assertEquals(expectedRules, rules);

		reducer.checkReductionCorrectness(flag, rules, originalRules, originalLines);
	}

	@Test
	public void case14() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"LANG vec",
			"FLAG long",
			"SFX r2 Y 26",
			"SFX r2 r ŧion [^aei]r",
			"SFX r2 r ŧion [^t]ar",
			"SFX r2 r ŧion [^en]tar",
			"SFX r2 r ŧion [^p]etar",
			"SFX r2 r ŧion [^s]petar",
			"SFX r2 r ŧion [^e]ntar",
			"SFX r2 r ŧion [^vx]entar",
			"SFX r2 r ŧion [^à]er",
			"SFX r2 r ŧion [^u]ir",
			"SFX r2 tar ŧion spetar",
			"SFX r2 tar ŧion [vx]entar",
			"SFX r2 àer aŧion àer",
			"SFX r2 ir ŧion uir",
			"SFX r2 erò ion [^dmnñtuvsŧx]erò",
			"SFX r2 erò ion [sŧ]erò",
			"SFX r2 xerò ŧion [aeiu]xerò",
			"SFX r2 orxerò ureŧion sorxerò",
			"SFX r2 merò nŧion umerò",
			"SFX r2 nerò xiŧion onerò",
			"SFX r2 verò ŧion iverò",
			"SFX r2 erò iŧion eterò",
			"SFX r2 derò ŧion [ae]nderò",
			"SFX r2 ñerò nŧion eñerò",
			"SFX r2 derò uŧion olderò",
			"SFX r2 verò uŧion olverò",
			"SFX r2 guerò ŧion stinguerò"
		);
		Pair<RulesReducer, WordGenerator> pair = createReducer(affFile);
		RulesReducer reducer = pair.getLeft();
		WordGenerator wordGenerator = pair.getRight();
		String flag = "r2";
		List<String> words = Arrays.asList("koskriverò", "sotoskriverò", "skriverò", "iskriverò", "sorxerò", "konveñerò", "solverò", "evolverò", "revolverò", "rexolverò", "tràer", "estràer", "prexentar", "exentar", "ventar", "sospetar", "destinguerò", "konstituir", "atribuir", "kostituir", "kostruir", "deminuir", "sostituir", "instruir", "destribuir", "diminuir", "lokuir", "raserò", "faserò", "struđerò", "raŧerò", "repeterò", "kavar", "đenerar", "fermentar", "notar", "sastufar", "strologar", "inpedir", "aplikar", "exibir", "traxlokar", "galidar", "komodar", "permutar", "suparar", "komunegar", "spekular", "kostipar", "velar", "destinar", "kondir", "kanŧelar", "eskorporar", "nudrir", "danar", "konmixerar", "tribolar", "vokar", "markar", "sagurar", "piñorar", "stelar", "raŧionar", "lamentar", "sonorixar", "fidar", "sostentar", "subordenar", "liberar", "oparar", "versar", "prosimar", "koniugar", "klasifegar", "kuantifegar", "realiđar", "tonar", "sklamar", "vixitar", "torefar", "mexurar", "koordenar", "sonoriđar", "konvokar", "bitar", "numarar", "reputar", "tradir", "satusfar", "putrefar", "insinuar", "intimar", "edukar", "peñorar", "vendegar", "esterminar", "valutar", "palatiđar", "sekurar", "ostentar", "denbrar", "ultimar", "deneralidar", "frankar", "trobolar", "simular", "kolaudar", "termenar", "naturaliđar", "krear", "paniđar", "vokalidar", "akuxar", "legar", "oblar", "rekuixir", "sindikar", "stilar", "soportar", "konmixarar", "verifegar", "opinar", "privar", "sinŧierar", "xeneralixar", "provar", "torbolar", "saludar", "servar", "perlustrar", "solevar", "parar", "ativar", "mutar", "segurar", "maledir", "provokar", "petir", "satisfar", "notifegar", "akuixir", "panidar", "artikolar", "piegar", "mormolar", "alterar", "numerar", "ubigar", "luminar", "vibrar", "sorafar", "palatidar", "remunerar", "binar", "spetorar", "speŧifegar", "salutar", "ŧetar", "ordenar", "partir", "redar", "estermenar", "prevarikar", "trasformar", "realixar", "skriturar", "skorporar", "votar", "monir", "naturalidar", "raprexar", "eskavar", "ministrar", "sarvar", "saluar", "malversar", "autoriđar", "skalinar", "terminar", "far", "desimilar", "rekuiđir", "vidimar", "ondar", "parteŧipar", "interogar", "augumentar", "emular", "strukar", "sonoridar", "levar", "legaliđar", "fenir", "malfar", "mirar", "palpitar", "deputar", "galixar", "variar", "operar", "exborsar", "mormorar", "tesar", "konsumar", "tratar", "đeneraliđar", "đenaraliđar", "fetar", "salvar", "xetar", "limitar", "depoxitar", "sikurar", "vestir", "munir", "legrar", "orar", "traversar", "pernotar", "identifegar", "radar", "dexertar", "rafinar", "asimilar", "ŧitar", "obligar", "straxordenar", "rapatumar", "kalŧinar", "superar", "ostinar", "strakolar", "subarendar", "galiđar", "vokalixar", "denaralidar", "fisar", "sinŧerar", "suplegar", "preŧixar", "punir", "esklamar", "inkonbinar", "fregar", "turbular", "separar", "proibir", "cetar", "manipolar", "revokar", "autoridar", "manŧipar", "sigurar", "filtrar", "supurar", "benedir", "formar", "vaŧinar", "balotar", "denerar", "interpretar", "kolar", "xrenar", "stalar", "elevar", "rekuidir", "varsar", "sodisfar", "finir", "kapitolar", "skaldar", "deletar", "proar", "substentar", "legalidar", "iluminar", "kontaminar", "libarar", "xenaralixar", "inibir", "malvarsar", "examinar", "guarnir", "suporar", "pelar", "espurgar", "parir", "palatixar", "mortifegar", "soporar", "tentar", "lenir", "nomenar", "vokaliđar", "exaltar", "exortar", "inkuixir", "rivar", "butar", "sperar", "realidar", "mixurar", "senplifegar", "situar", "sistemar", "testar", "xmenbrar", "strolegar", "tranxar", "negar", "fenderò", "sospenderò", "espanderò", "suspenderò", "tenderò", "prexuponerò", "posponerò", "esponerò", "oponerò", "ponerò", "konponerò", "proponerò", "xustaponerò", "asumerò", "prexumerò", "konsumerò", "duxerò", "elexerò", "estraxerò", "lexerò", "faxerò", "korexerò", "aflixerò", "struxerò", "produxerò", "introduxerò", "solderò");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.flatMap(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX).stream())
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		List<LineEntry> expectedCompactedRules = Arrays.asList(
			new LineEntry("àer", "aŧion", "àer", Arrays.asList("tràer", "estràer")),
			new LineEntry("nerò", "xiŧion", "nerò", Arrays.asList("prexuponerò", "posponerò", "esponerò", "oponerò", "ponerò", "konponerò", "proponerò", "xustaponerò")),
			new LineEntry("guerò", "ŧion", "guerò", "destinguerò"),
			new LineEntry("orxerò", "ureŧion", "orxerò", "sorxerò"),
			new LineEntry("merò", "nŧion", "merò", Arrays.asList("asumerò", "prexumerò", "konsumerò")),
			new LineEntry("ñerò", "nŧion", "ñerò", "konveñerò"),
			new LineEntry("ir", "ŧion", "uir", Arrays.asList("konstituir", "atribuir", "kostituir", "kostruir", "deminuir", "sostituir", "instruir", "destribuir", "diminuir", "lokuir")),
			new LineEntry("r", "ŧion", "[^u]ir", Arrays.asList("monir", "inibir", "guarnir", "inpedir", "maledir", "exibir", "petir", "parir", "punir", "akuixir", "rekuiđir", "vestir", "munir", "lenir", "proibir", "rekuixir", "kondir", "inkuixir", "benedir", "nudrir", "tradir", "rekuidir", "fenir", "partir", "finir")),
			new LineEntry("r", "ŧion", "[^t]ar", Arrays.asList("kavar", "đenerar", "sastufar", "strologar", "aplikar", "traxlokar", "galidar", "komodar", "suparar", "komunegar", "spekular", "kostipar", "velar", "destinar", "kanŧelar", "eskorporar", "danar", "konmixerar", "tribolar", "vokar", "markar", "sagurar", "piñorar", "stelar", "raŧionar", "sonorixar", "fidar", "subordenar", "liberar", "oparar", "versar", "prosimar", "koniugar", "klasifegar", "kuantifegar", "realiđar", "tonar", "sklamar", "torefar", "mexurar", "koordenar", "sonoriđar", "konvokar", "numarar", "satusfar", "putrefar", "insinuar", "intimar", "edukar", "peñorar", "vendegar", "esterminar", "palatiđar", "sekurar", "denbrar", "ultimar", "deneralidar", "frankar", "trobolar", "simular", "kolaudar", "termenar", "naturaliđar", "krear", "paniđar", "vokalidar", "akuxar", "legar", "oblar", "sindikar", "stilar", "konmixarar", "verifegar", "opinar", "privar", "sinŧierar", "xeneralixar", "provar", "torbolar", "saludar", "servar", "perlustrar", "solevar", "parar", "ativar", "segurar", "provokar", "satisfar", "notifegar", "panidar", "artikolar", "piegar", "mormolar", "alterar", "numerar", "ubigar", "luminar", "vibrar", "sorafar", "palatidar", "remunerar", "binar", "spetorar", "speŧifegar", "ordenar", "redar", "estermenar", "prevarikar", "trasformar", "realixar", "skriturar", "skorporar", "naturalidar", "raprexar", "eskavar", "ministrar", "sarvar", "saluar", "malversar", "autoriđar", "skalinar", "terminar", "far", "desimilar", "vidimar", "ondar", "parteŧipar", "interogar", "emular", "strukar", "sonoridar", "levar", "legaliđar", "malfar", "mirar", "galixar", "variar", "operar", "exborsar", "mormorar", "tesar", "konsumar", "đeneraliđar", "đenaraliđar", "salvar", "sikurar", "legrar", "orar", "traversar", "identifegar", "radar", "rafinar", "asimilar", "obligar", "straxordenar", "rapatumar", "kalŧinar", "superar", "ostinar", "strakolar", "subarendar", "galiđar", "vokalixar", "denaralidar", "fisar", "sinŧerar", "suplegar", "preŧixar", "esklamar", "inkonbinar", "fregar", "turbular", "separar", "manipolar", "revokar", "autoridar", "manŧipar", "sigurar", "filtrar", "supurar", "formar", "vaŧinar", "denerar", "kolar", "xrenar", "stalar", "elevar", "varsar", "sodisfar", "kapitolar", "skaldar", "proar", "legalidar", "iluminar", "kontaminar", "libarar", "xenaralixar", "malvarsar", "examinar", "suporar", "pelar", "espurgar", "palatixar", "mortifegar", "soporar", "nomenar", "vokaliđar", "rivar", "sperar", "realidar", "mixurar", "senplifegar", "situar", "sistemar", "xmenbrar", "strolegar", "tranxar", "negar")),
			new LineEntry("erò", "iŧion", "terò", "repeterò"),
			new LineEntry("erò", "ion", "[đsŧ]erò", Arrays.asList("raserò", "faserò", "struđerò", "raŧerò")),
			new LineEntry("r", "ŧion", "[^en]tar", Arrays.asList("exaltar", "exortar", "tratar", "pernotar", "soportar", "notar", "bitar", "butar", "mutar", "reputar", "balotar", "limitar", "dexertar", "testar", "salutar", "depoxitar", "palpitar", "deputar", "ŧitar", "permutar", "vixitar", "valutar", "votar")),
			new LineEntry("verò", "ŧion", "[^l]verò", Arrays.asList("koskriverò", "sotoskriverò", "skriverò", "iskriverò")),
			new LineEntry("verò", "uŧion", "lverò", Arrays.asList("solverò", "evolverò", "revolverò", "rexolverò")),
			new LineEntry("xerò", "ŧion", "[^r]xerò", Arrays.asList("duxerò", "elexerò", "estraxerò", "lexerò", "faxerò", "korexerò", "aflixerò", "struxerò", "produxerò", "introduxerò")),
			new LineEntry("derò", "uŧion", "[^n]derò", "solderò"),
			new LineEntry("derò", "ŧion", "nderò", Arrays.asList("fenderò", "sospenderò", "espanderò", "suspenderò", "tenderò")),
			new LineEntry("tar", "ŧion", "petar", "sospetar"),
			new LineEntry("r", "ŧion", "[^p]etar", Arrays.asList("deletar", "cetar", "xetar", "ŧetar", "fetar", "interpretar")),
			new LineEntry("tar", "ŧion", "[^mt]entar", Arrays.asList("prexentar", "exentar", "ventar")),
			new LineEntry("r", "ŧion", "[mt]entar", Arrays.asList("substentar", "fermentar", "ostentar", "lamentar", "tentar", "sostentar", "augumentar"))
		);
		Assertions.assertEquals(expectedCompactedRules, compactedRules);

		List<String> rules = reducer.convertFormat(flag, false, compactedRules);
		List<String> expectedRules = Arrays.asList(
			"SFX r2 Y 21",
			"SFX r2 ir ŧion uir",
			"SFX r2 àer aŧion àer",
			"SFX r2 r ŧion [^t]ar",
			"SFX r2 r ŧion [^u]ir",
			"SFX r2 erò iŧion terò",
			"SFX r2 merò nŧion merò",
			"SFX r2 nerò xiŧion nerò",
			"SFX r2 ñerò nŧion ñerò",
			"SFX r2 r ŧion [^en]tar",
			"SFX r2 erò ion [đsŧ]erò",
			"SFX r2 tar ŧion petar",
			"SFX r2 derò ŧion nderò",
			"SFX r2 verò uŧion lverò",
			"SFX r2 guerò ŧion guerò",
			"SFX r2 r ŧion [^p]etar",
			"SFX r2 derò uŧion [^n]derò",
			"SFX r2 verò ŧion [^l]verò",
			"SFX r2 xerò ŧion [^r]xerò",
			"SFX r2 orxerò ureŧion orxerò",
			"SFX r2 r ŧion [mt]entar",
			"SFX r2 tar ŧion [^mt]entar"
		);
		Assertions.assertEquals(expectedRules, rules);

		reducer.checkReductionCorrectness(flag, rules, originalRules, originalLines);
	}

	@Test
	public void case15() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"LANG vec",
			"FLAG long",
			"SFX r1 Y 2",
			"SFX r1 r mento r",
			"SFX r1 rò mento arò"
		);
		Pair<RulesReducer, WordGenerator> pair = createReducer(affFile);
		RulesReducer reducer = pair.getLeft();
		WordGenerator wordGenerator = pair.getRight();
		String flag = "r1";
		List<String> words = Arrays.asList("pispoƚar", "josoƚar", "ƚatar", "xbesoƚar", "diƚatar", "ƚontanar", "bagoƚar", "indeboƚir", "vaƚir", "strukoƚar", "boƚegar", "indoƚentrar", "deƚinear", "formigoƚar", "desarveƚar", "biskoƚar", "kaƚar", "sifoƚar", "rueƚar", "dindoƚar", "krikoƚar", "ƚigar", "siaƚakuar", "ƚoxar", "ƚisar", "deserveƚar", "ƚanpexar", "nuvoƚar", "kabaƚar", "ñaoƚar", "bueƚar", "ƚanpixar", "spigoƚar", "triboƚar", "turbuƚar", "proƚongar", "trabakoƚar", "krokoƚar", "skonbusoƚar", "cicoƚar", "skorkoƚar", "kavaƚar", "skrisoƚar", "troboƚar", "basiƚar", "torboƚar", "ƚogar", "paƚar", "faƚir", "ƚanbikar", "peƚar", "dexserveƚar", "sigaƚar", "dexsarveƚar", "buƚegar", "strakoƚar", "voltoƚar", "koƚar", "ɉosoƚar", "guaƚivar", "ƚuxarò");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.flatMap(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX).stream())
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		List<LineEntry> expectedCompactedRules = Arrays.asList(
			new LineEntry("r", "mento", "r", Arrays.asList("pispoƚar", "josoƚar", "ƚatar", "xbesoƚar", "diƚatar", "ƚontanar", "bagoƚar", "indeboƚir", "vaƚir", "strukoƚar", "boƚegar", "indoƚentrar", "deƚinear", "formigoƚar", "desarveƚar", "biskoƚar", "kaƚar", "sifoƚar", "rueƚar", "dindoƚar", "krikoƚar", "ƚigar", "siaƚakuar", "ƚoxar", "ƚisar", "deserveƚar", "ƚanpexar", "nuvoƚar", "kabaƚar", "ñaoƚar", "bueƚar", "ƚanpixar", "spigoƚar", "triboƚar", "turbuƚar", "proƚongar", "trabakoƚar", "krokoƚar", "skonbusoƚar", "cicoƚar", "skorkoƚar", "kavaƚar", "skrisoƚar", "troboƚar", "basiƚar", "torboƚar", "ƚogar", "paƚar", "faƚir", "ƚanbikar", "peƚar", "dexserveƚar", "sigaƚar", "dexsarveƚar", "buƚegar", "strakoƚar", "voltoƚar", "koƚar", "ɉosoƚar", "guaƚivar")),
			new LineEntry("rò", "mento", "rò", "ƚuxarò")
		);
		Assertions.assertEquals(expectedCompactedRules, compactedRules);

		List<String> rules = reducer.convertFormat(flag, false, compactedRules);
		List<String> expectedRules = Arrays.asList(
			"SFX r1 Y 2",
			"SFX r1 r mento r",
			"SFX r1 rò mento rò"
		);
		Assertions.assertEquals(expectedRules, rules);

		reducer.checkReductionCorrectness(flag, rules, originalRules, originalLines);
	}

	@Test
	public void case16() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"LANG vec",
			"FLAG long",
			"SFX r0 Y 7",
			"SFX r0 0 amento n",
			"SFX r0 o amento o",
			"SFX r0 r mento [^a]r",
			"SFX r0 r mento [^u]ar",
			"SFX r0 uar omento uar",
			"SFX r0 rò mento arò",
			"SFX r0 erò imento erò"
		);
		Pair<RulesReducer, WordGenerator> pair = createReducer(affFile);
		RulesReducer reducer = pair.getLeft();
		WordGenerator wordGenerator = pair.getRight();
		String flag = "r0";
		List<String> words = Arrays.asList("baŧilar", "ŧigalar", "skriŧolar", "pentir", "tarar", "xiovar", "furigar", "inpedir", "latar", "rudar", "favorir", "savatar", "traxlokar", "komodar", "baxotar", "komandar", "stornir", "bonar", "kanonar", "pedorar", "buelar", "ɉosolar", "indebolir", "piŧegar", "ronkexar", "iskurir", "farar", "trasferir", "durar", "suɉerir", "logar", "palar", "skuinternar", "ronkiđar", "strigar", "lanpixar", "muxegar", "striar", "insokir", "loxar", "kurar", "boɉir", "bađotar", "tradir", "trapar", "inkroxar", "ŧonkar", "tenparar", "semenar", "skorabiar", "ordar", "skurir", "raforsar", "taɉusar", "trobolar", "traxlatar", "skarpasar", "đirar", "ñaolar", "skorajar", "tejar", "inkoraxar", "trair", "skarpaŧar", "matir", "sobojir", "gravar", "sojar", "raforŧar", "lanpeđar", "deŧervelar", "dexsarvelar", "fogar", "ondixar", "torbiar", "stravinar", "susurar", "sentir", "ŧavariar", "pensar", "dirar", "sekar", "voltolar", "egreɉar", "ŧensir", "spolvarar", "formigar", "cicolar", "ŧukar", "inbriagar", "xbeŧolar", "biskolar", "josolar", "kasar", "mañar", "desarvelar", "fufiñar", "konŧar", "straviar", "tarmar", "bajar", "fondar", "fornir", "botidar", "degladiar", "jurar", "scantidar", "sfredir", "partir", "taɉuŧar", "tikiñar", "xontar", "konsar", "jadar", "sensir", "orbar", "spegaŧar", "portar", "badotar", "konpañar", "malsontar", "karesar", "rakoŧar", "pagar", "kareŧar", "rakosar", "lodar", "ansar", "vanir", "xenocar", "rasar", "pestar", "strukar", "rascar", "borar", "ondexar", "spegasar", "fenir", "roxar", "galdir", "stusegar", "skaltrir", "vertir", "kaɉar", "ruspar", "bolegar", "vesinar", "jaŧar", "cacarar", "korteɉar", "lanpidar", "jasar", "vestir", "bagolar", "xirar", "peđorar", "đenocar", "perir", "fojar", "moɉar", "xlanbanar", "pastiŧar", "griñar", "spigaŧar", "spigasar", "pastisar", "skoređar", "riondar", "skonkasar", "strakolar", "propiar", "intronar", "dindolar", "sonkar", "rebaltar", "fardir", "furegar", "braŧar", "sarar", "ordir", "brasar", "ruđar", "ronkidar", "kordar", "abokar", "stordir", "lanbikar", "inviar", "pastrocar", "andar", "substentar", "ɉaŧar", "sujerir", "sortir", "ondedar", "guarnir", "ɉasar", "interesar", "ronkeđar", "dexservelar", "skorlar", "insurir", "regolar", "dormenŧar", "raspar", "savariar", "kokonar", "koŧar", "dormensar", "gonfiar", "morsegar", "xñikar", "inŧokir", "krikolar", "armar", "baɉar", "granfir", "sasinar", "trabakolar", "điovar", "kustionar", "skorkolar", "devorar", "roxegar", "serar", "tosir", "denocar", "kanar", "kosar", "ligar", "seneɉar", "xovar", "ingropar", "joŧolar", "tramortir", "danar", "indolentrar", "ferir", "falir", "tribolar", "inkarir", "malŧontar", "maistrar", "xjonfar", "kabalar", "stupidir", "sostentar", "lanpiđar", "bojir", "prolongar", "lontanar", "tremar", "alŧar", "rodar", "skantinar", "strasar", "ŧifolar", "scantiđar", "mejorar", "deservelar", "meɉorar", "bagordar", "ɉoŧolar", "moxegar", "spolverar", "voltar", "straŧar", "botiđar", "spigolar", "desipar", "pispolar", "sposar", "viŧinar", "indupionar", "sujarir", "sofegar", "terar", "skrisolar", "basilar", "xbregar", "pisegar", "sarir", "ŧavatar", "inpietrir", "kapir", "stuŧegar", "kalar", "miɉorar", "skorexar", "intaresar", "soboɉir", "skuintarnar", "batocar", "valir", "egrejar", "menar", "kopar", "teɉar", "sukar", "skoraɉar", "suɉarir", "lođar", "exaurir", "ronkixar", "naspar", "stomegar", "travar", "veŧinar", "torbolar", "ferar", "parar", "ruspiar", "rengrasiar", "kavalkar", "sorar", "duplikar", "kanpanar", "lanpexar", "soɉar", "cacerar", "basar", "xurar", "xɉonfar", "strukolar", "đontar", "takar", "predegar", "ondiđar", "brontolar", "dexŧervelar", "inkoraɉar", "ruelar", "formigolar", "diovar", "tenperar", "bixegar", "moroxar", "xvegrar", "inpinir", "inkorajar", "dovar", "foɉar", "sitar", "mojar", "kavalar", "kajar", "pexorar", "skonbusolar", "tirar", "đurar", "inserir", "futiñar", "ronkedar", "kortejar", "saldar", "bonir", "baŧixar", "drapar", "rekordar", "asokir", "muñegar", "indurir", "agravar", "fumegar", "tratar", "inkrikar", "ondidar", "farir", "sfegatar", "gualivar", "salvar", "patir", "aŧokir", "ondeđar", "divertir", "teñir", "misiar", "lisar", "kanbiar", "dontar", "xbesolar", "ŧitar", "biankir", "kaxar", "insukir", "tajusar", "tajuŧar", "provixionar", "skoredar", "dilatar", "xlanbar", "pesar", "pontar", "basixar", "nuvolar", "fregar", "xlavacar", "turbular", "peŧar", "arsar", "mijorar", "deŧipar", "bulegar", "speŧar", "spesar", "kolar", "infiar", "finir", "trovar", "ronkir", "botixar", "ferdir", "petar", "sonar", "lanpedar", "đovar", "rexegar", "kagar", "panar", "rođar", "bruxar", "trasinar", "trasfarir", "akorar", "scantixar", "visinar", "divinar", "arpentir", "senejar", "krokolar", "rengraŧiar", "maŧar", "delinear", "ruxar", "masar", "kanbio", "baso", "ordo", "bojerò", "fenderò", "moverò", "naserò", "sebaterò", "renkreserò", "torđerò", "meterò", "manteñerò", "sobaterò", "akorxerò", "skoderò", "ponxerò", "sorxerò", "skonbaterò", "veñerò", "galderò", "sparxerò", "akorderò", "goderò", "vederò", "boɉerò", "ponđerò", "roderò", "rexerò", "ponderò", "rinkreserò", "baterò", "kreserò", "provederò", "akorđerò", "korerò", "torxerò", "sialakuar", "fruar", "arguar", "vexin", "visin", "vesin", "viŧin", "xgrendarò", "xgrenđarò", "xgrenxarò", "luxarò");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.flatMap(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX).stream())
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		List<LineEntry> expectedCompactedRules = Arrays.asList(
			new LineEntry("erò", "imento", "erò", Arrays.asList("bojerò", "fenderò", "moverò", "naserò", "sebaterò", "renkreserò", "torđerò", "meterò", "manteñerò", "sobaterò", "akorxerò", "skoderò", "ponxerò", "sorxerò", "skonbaterò", "veñerò", "galderò", "sparxerò", "akorderò", "goderò", "vederò", "boɉerò", "ponđerò", "roderò", "rexerò", "ponderò", "rinkreserò", "baterò", "kreserò", "provederò", "akorđerò", "korerò", "torxerò")),
			new LineEntry("o", "amento", "o", Arrays.asList("kanbio", "baso", "ordo")),
			new LineEntry("uar", "omento", "uar", Arrays.asList("sialakuar", "fruar", "arguar")),
			new LineEntry("0", "amento", "n", Arrays.asList("vexin", "visin", "vesin", "viŧin")),
			new LineEntry("r", "mento", "[^a]r", Arrays.asList("sensir", "granfir", "insukir", "pentir", "skurir", "inpinir", "fardir", "inpedir", "sarir", "inpietrir", "kapir", "favorir", "tosir", "trair", "ordir", "soboɉir", "vanir", "stornir", "inserir", "matir", "sobojir", "tramortir", "valir", "ferir", "falir", "bonir", "fenir", "suɉarir", "inkarir", "indebolir", "galdir", "stordir", "asokir", "skaltrir", "vertir", "finir", "ronkir", "stupidir", "exaurir", "indurir", "bojir", "iskurir", "sentir", "trasferir", "sujerir", "sortir", "ferdir", "suɉerir", "farir", "guarnir", "ŧensir", "patir", "aŧokir", "vestir", "insurir", "divertir", "teñir", "trasfarir", "insokir", "perir", "boɉir", "tradir", "arpentir", "fornir", "sujarir", "sfredir", "partir", "biankir", "inŧokir")),
			new LineEntry("rò", "mento", "[^e]rò", Arrays.asList("xgrendarò", "xgrenđarò", "xgrenxarò", "luxarò")),
			new LineEntry("r", "mento", "[^u]ar", Arrays.asList("baŧilar", "armar", "ŧigalar", "baɉar", "skriŧolar", "sasinar", "trabakolar", "tarar", "điovar", "xiovar", "furigar", "latar", "rudar", "kustionar", "skorkolar", "devorar", "savatar", "traxlokar", "roxegar", "serar", "komodar", "baxotar", "denocar", "kanar", "kosar", "ligar", "seneɉar", "xovar", "komandar", "ingropar", "joŧolar", "bonar", "kanonar", "pedorar", "danar", "indolentrar", "buelar", "tribolar", "ɉosolar", "malŧontar", "piŧegar", "maistrar", "xjonfar", "kabalar", "ronkexar", "sostentar", "lanpiđar", "prolongar", "lontanar", "tremar", "alŧar", "rodar", "farar", "skantinar", "strasar", "ŧifolar", "durar", "scantiđar", "mejorar", "deservelar", "meɉorar", "bagordar", "logar", "ɉoŧolar", "moxegar", "palar", "skuinternar", "ronkiđar", "spolverar", "strigar", "voltar", "lanpixar", "muxegar", "striar", "straŧar", "loxar", "kurar", "botiđar", "spigolar", "desipar", "pispolar", "sposar", "viŧinar", "bađotar", "indupionar", "trapar", "inkroxar", "ŧonkar", "sofegar", "tenparar", "semenar", "terar", "skorabiar", "ordar", "skrisolar", "basilar", "xbregar", "pisegar", "raforsar", "taɉusar", "trobolar", "ŧavatar", "traxlatar", "skarpasar", "đirar", "ñaolar", "skorajar", "stuŧegar", "kalar", "tejar", "inkoraxar", "miɉorar", "skorexar", "intaresar", "skuintarnar", "skarpaŧar", "batocar", "gravar", "egrejar", "menar", "kopar", "teɉar", "sukar", "skoraɉar", "sojar", "lođar", "raforŧar", "lanpeđar", "deŧervelar", "dexsarvelar", "fogar", "ondixar", "torbiar", "stravinar", "ronkixar", "susurar", "naspar", "stomegar", "travar", "veŧinar", "torbolar", "ŧavariar", "ferar", "pensar", "parar", "dirar", "ruspiar", "sekar", "rengrasiar", "kavalkar", "voltolar", "sorar", "egreɉar", "duplikar", "kanpanar", "lanpexar", "soɉar", "cacerar", "spolvarar", "formigar", "cicolar", "ŧukar", "basar", "xurar", "inbriagar", "xɉonfar", "xbeŧolar", "biskolar", "josolar", "strukolar", "kasar", "đontar", "takar", "predegar", "mañar", "desarvelar", "fufiñar", "ondiđar", "konŧar", "straviar", "brontolar", "dexŧervelar", "tarmar", "bajar", "fondar", "inkoraɉar", "botidar", "ruelar", "degladiar", "jurar", "scantidar", "taɉuŧar", "formigolar", "tikiñar", "xontar", "konsar", "jadar", "diovar", "tenperar", "bixegar", "orbar", "spegaŧar", "moroxar", "portar", "xvegrar", "badotar", "konpañar", "malsontar", "inkorajar", "dovar", "foɉar", "sitar", "karesar", "rakoŧar", "mojar", "pagar", "kavalar", "kajar", "kareŧar", "rakosar", "pexorar", "lodar", "skonbusolar", "tirar", "ansar", "xenocar", "rasar", "đurar", "pestar", "strukar", "rascar", "futiñar", "ronkedar", "borar", "ondexar", "kortejar", "spegasar", "saldar", "baŧixar", "roxar", "drapar", "stusegar", "rekordar", "kaɉar", "muñegar", "ruspar", "bolegar", "agravar", "fumegar", "vesinar", "tratar", "jaŧar", "cacarar", "korteɉar", "lanpidar", "inkrikar", "ondidar", "sfegatar", "gualivar", "jasar", "salvar", "ondeđar", "bagolar", "xirar", "peđorar", "misiar", "đenocar", "fojar", "moɉar", "lisar", "xlanbanar", "kanbiar", "pastiŧar", "dontar", "xbesolar", "ŧitar", "griñar", "spigaŧar", "spigasar", "pastisar", "skoređar", "riondar", "skonkasar", "kaxar", "strakolar", "propiar", "intronar", "dindolar", "sonkar", "tajusar", "tajuŧar", "provixionar", "skoredar", "rebaltar", "furegar", "dilatar", "xlanbar", "braŧar", "pesar", "sarar", "pontar", "basixar", "nuvolar", "fregar", "brasar", "xlavacar", "turbular", "peŧar", "arsar", "mijorar", "ruđar", "deŧipar", "ronkidar", "bulegar", "speŧar", "spesar", "kolar", "kordar", "abokar", "infiar", "lanbikar", "trovar", "inviar", "pastrocar", "andar", "substentar", "ɉaŧar", "botixar", "petar", "sonar", "ondedar", "ɉasar", "lanpedar", "interesar", "đovar", "rexegar", "ronkeđar", "dexservelar", "kagar", "skorlar", "panar", "rođar", "bruxar", "trasinar", "regolar", "akorar", "scantixar", "dormenŧar", "visinar", "raspar", "savariar", "kokonar", "divinar", "koŧar", "senejar", "dormensar", "gonfiar", "krokolar", "rengraŧiar", "maŧar", "morsegar", "xñikar", "delinear", "ruxar", "masar", "krikolar"))
		);
		Assertions.assertEquals(expectedCompactedRules, compactedRules);

		List<String> rules = reducer.convertFormat(flag, false, compactedRules);
		List<String> expectedRules = Arrays.asList(
			"SFX r0 Y 7",
			"SFX r0 0 amento n",
			"SFX r0 o amento o",
			"SFX r0 r mento [^a]r",
			"SFX r0 erò imento erò",
			"SFX r0 uar omento uar",
			"SFX r0 r mento [^u]ar",
			"SFX r0 rò mento [^e]rò"
		);
		Assertions.assertEquals(expectedRules, rules);

		reducer.checkReductionCorrectness(flag, rules, originalRules, originalLines);
	}

	@Test
	public void case17() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"LANG vec",
			"FLAG long",
			"SFX q1 Y 14",
			"SFX q1 0 sa [^d]e",
			"SFX q1 0 sa [^ò]de",
			"SFX q1 òde odesa òde",
			"SFX q1 o esa [^dƚs]o",
			"SFX q1 o esa [^ò][ds]o",
			"SFX q1 òdo odesa òdo",
			"SFX q1 òso osesa òso",
			"SFX q1 0 esa n",
			"SFX q1 0 esa [^è]l",
			"SFX q1 èl elesa èl",
			"SFX q1 0 esa [^è]r",
			"SFX q1 èr eresa èr",
			"SFX q1 o esa [^è]ƚo",
			"SFX q1 èƚo eƚesa èƚo"
		);
		Pair<RulesReducer, WordGenerator> pair = createReducer(affFile);
		RulesReducer reducer = pair.getLeft();
		WordGenerator wordGenerator = pair.getRight();
		String flag = "q1";
		List<String> words = Arrays.asList("onorato", "seko", "pronto", "tondo", "reƚasato", "ardito", "xɉonfo", "xjonfo", "stranio", "kieto", "sfrenato", "raro", "streto", "tato", "burto", "gaɉardo", "fiako", "goƚoxo", "sfasato", "goloxo", "kontento", "aspro", "ƚegro", "straño", "presto", "fondo", "relasato", "skuexito", "sutiƚo", "baldo", "alto", "ƚargo", "exato", "grando", "longo", "fato", "largo", "guaƚivo", "bastardo", "vago", "keto", "gajardo", "legro", "suto", "kueto", "riko", "fiso", "adeguato", "ceto", "grevo", "bruto", "gualivo", "garbo", "bojo", "magro", "boɉo", "molexin", "moƚexin", "xal", "mulexin", "muƚexin", "car", "segur", "repien", "fin", "sorafin", "man", "sutil", "grave", "mite", "bèl", "pròde", "ledièr", "leđièr", "lexièr", "ƚexièr", "fièr", "bèƚo", "sòdo", "gròso");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.flatMap(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX).stream())
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		List<LineEntry> expectedCompactedRules = Arrays.asList(
			new LineEntry("èl", "elesa", "èl", Arrays.asList("bèl")),
			new LineEntry("òde", "odesa", "òde", Arrays.asList("pròde")),
			new LineEntry("èƚo", "eƚesa", "èƚo", "bèƚo"),
			new LineEntry("òdo", "odesa", "òdo", "sòdo"),
			new LineEntry("òso", "osesa", "òso", Arrays.asList("gròso")),
			new LineEntry("èr", "eresa", "èr", Arrays.asList("ledièr", "leđièr", "lexièr", "ƚexièr", "fièr")),
			new LineEntry("0", "esa", "n", Arrays.asList("molexin", "moƚexin", "mulexin", "muƚexin", "repien", "fin", "sorafin", "man")),
			new LineEntry("o", "esa", "[^dƚs]o", Arrays.asList("onorato", "relasato", "seko", "skuexito", "pronto", "reƚasato", "ardito", "alto", "ƚargo", "exato", "xɉonfo", "xjonfo", "longo", "stranio", "kieto", "sfrenato", "raro", "fato", "streto", "largo", "tato", "guaƚivo", "burto", "vago", "keto", "legro", "suto", "fiako", "kueto", "riko", "goƚoxo", "sfasato", "goloxo", "adeguato", "ceto", "grevo", "kontento", "aspro", "ƚegro", "bruto", "straño", "gualivo", "garbo", "bojo", "magro", "boɉo", "presto")),
			new LineEntry("0", "sa", "[^d]e", Arrays.asList("grave", "mite")),
			new LineEntry("o", "esa", "[^è]ƚo", Arrays.asList("sutiƚo")),
			new LineEntry("o", "esa", "[^ò][ds]o", Arrays.asList("fiso")),
			new LineEntry("0", "esa", "[^è][lr]", Arrays.asList("car", "segur"))
		);
		Assertions.assertEquals(expectedCompactedRules, compactedRules);

		List<String> rules = reducer.convertFormat(flag, false, compactedRules);
		List<String> expectedRules = Arrays.asList(
			"SFX q1 Y 12",
			"SFX q1 0 esa n",
			"SFX q1 èl elesa èl",
			"SFX q1 èr eresa èr",
			"SFX q1 0 sa [^d]e",
			"SFX q1 o esa [^dƚs]o",
			"SFX q1 0 esa [^è][lr]",
			"SFX q1 òde odesa òde",
			"SFX q1 òdo odesa òdo",
			"SFX q1 èƚo eƚesa èƚo",
			"SFX q1 òso osesa òso",
			"SFX q1 o esa [^è]ƚo",
			"SFX q1 o esa [^ò][ds]o"
		);
		Assertions.assertEquals(expectedRules, rules);

		reducer.checkReductionCorrectness(flag, rules, originalRules, originalLines);
	}

	@Test
	public void case18() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"LANG vec",
			"FLAG long",
			"FULLSTRIP",
			"SFX mf Y 55",
			"SFX mf el ili el",
			"SFX mf ol uli ol",
			"SFX mf on uni on",
			"SFX mf en ini en",
			"SFX mf er iri er",
			"SFX mf or uri or",
			"SFX mf exe ixi exe",
			"SFX mf edo idi edo",
			"SFX mf odo udi odo",
			"SFX mf eđo iđi eđo",
			"SFX mf ođo uđi ođo",
			"SFX mf eko iki eko",
			"SFX mf oko uki oko",
			"SFX mf eƚo iƚi eƚo",
			"SFX mf oƚo uƚi oƚo",
			"SFX mf eño iñi eño",
			"SFX mf oño iñi oño",
			"SFX mf eso isi eso",
			"SFX mf oso usi oso",
			"SFX mf eto iti eto",
			"SFX mf oto uti oto",
			"SFX mf eŧo iŧi eŧo",
			"SFX mf oŧo uŧi oŧo",
			"SFX mf evo ivi evo",
			"SFX mf ovo uvi ovo",
			"SFX mf exo ixi exo",
			"SFX mf oxo uxi oxo",
			"SFX mf orse ursi orse",
			"SFX mf ente inti ente",
			"SFX mf onte unti onte",
			"SFX mf oldo uldi oldo",
			"SFX mf ordo urdi ordo",
			"SFX mf orđo urđi orđo",
			"SFX mf olko ulki olko",
			"SFX mf orko urki orko",
			"SFX mf ento inti ento",
			"SFX mf onto unti onto",
			"SFX mf orxo urxi orxo",
			"SFX mf ojoxo ujuxi ojoxo",
			"SFX mf oɉoxo uɉuxi oɉoxo",
			"SFX mf oloxo uluxi oloxo",
			"SFX mf oƚoxo uƚuxi oƚoxo",
			"SFX mf omoxo umuxi omoxo",
			"SFX mf oñoxo uñuxi oñoxo",
			"SFX mf oroxo uruxi oroxo",
			"SFX mf órden úrdini órden",
			"SFX mf obioxo ubiuxi obioxo",
			"SFX mf ofioxo ufiuxi ofioxo",
			"SFX mf olioxo uliuxi olioxo",
			"SFX mf oƚioxo uƚiuxi oƚioxo",
			"SFX mf onioxo uniuxi onioxo",
			"SFX mf orioxo uriuxi orioxo",
			"SFX mf ostoxo ustuxi ostoxo",
			"SFX mf obrioxo ubriuxi obrioxo",
			"SFX mf ordioxo urdiuxi ordioxo"
		);
		Pair<RulesReducer, WordGenerator> pair = createReducer(affFile);
		RulesReducer reducer = pair.getLeft();
		WordGenerator wordGenerator = pair.getRight();
		String flag = "mf";
		List<String> words = Arrays.asList("bifolko", "olko", "folko", "beolko", "solko", "biolko", "grevo", "trevo", "kànevo", "arlevo", "véskevo", "dexeño", "maƚedeño", "seño", "sosteño", "kondeño", "pareño", "maledeño", "konđeño", "reteño", "cipileño", "cipiƚeño", "kontraseño", "inđeño", "maxeño", "deño", "despeño", "lexeño", "soveño", "ƚexeño", "ordeño", "konveño", "konxeño", "poseño", "pisarol", "skodarol", "sotarol", "brònbol", "rovejol", "kódol", "porésol", "èrbol", "korósol", "fínfol", "buxigàtol", "narónkol", "jèvol", "fífol", "poréŧol", "onbrígol", "ígol", "ucarol", "kuriàtol", "réfol", "postríbol", "piàvol", "deñévol", "fisol", "ovarol", "xjoŧarol", "íxol", "ranarol", "kortívol", "karígol", "pípol", "sièol", "brúfol", "skrópol", "pisàndol", "fiŧol", "libarol", "ròdol", "đógol", "revendígol", "núgol", "desútol", "orbexígol", "koriàndol", "skapuŧiol", "màskol", "spónxol", "jègol", "róndol", "ràxol", "saldarol", "beđarol", "tastarol", "intíngol", "piàgol", "ponterol", "faxiol", "kortígol", "persénbol", "bròkol", "kòtol", "kanarol", "trémol", "tòpol", "supiarol", "biavarol", "prexudiŧiévol", "bògol", "búsol", "rapónsol", "parol", "liŧarol", "bronbeɉol", "níol", "perdonévol", "sóŧol", "garofol", "faŧiol", "rapónŧol", "faxol", "arđigògol", "braŧarol", "pikàñol", "sósol", "ŧendrarol", "piàol", "morévol", "forse", "straleko", "baƚeko", "seko", "ŧanbeko", "mexoseko", "beko", "straƚeko", "međoseko", "arketeko", "medoseko", "baleko", "xñeko", "pasteko", "tomier", "soaxier", "pólver", "xberoer", "viperier", "kalsinier", "mulinier", "peƚatier", "krivelier", "balkoner", "sanbugier", "nespolier", "latonier", "đaletier", "sorboƚier", "baretier", "magađener", "bísker", "samitier", "angurer", "bronbexier", "ŧarexier", "ganŧier", "pitarier", "đojelier", "stopier", "pelatier", "poster", "trameser", "miliardier", "spuƚexier", "fogolier", "lagrimier", "poƚastrier", "doaner", "ƚusernier", "karoboƚier", "figier", "pionbier", "manteñer", "ŧínger", "riŧolier", "baƚestrier", "kukúmer", "lavranier", "peƚaƚer", "konŧer", "meƚionier", "paraŧéner", "arminier", "anŧipresier", "muƚinier", "fornier", "fragier", "lanŧier", "bolsier", "kriveƚier", "perier", "dènder", "bronđier", "korbelier", "fontegier", "spulexier", "vivier", "kredenser", "pelaler", "maskarier", "piader", "armeƚinier", "ofeƚer", "diamantier", "nespoƚier", "portinier", "fabriŧer", "sorbolier", "peƚisier", "gadeter", "librier", "goŧo", "bragoŧo", "biroŧo", "robegoŧo", "scoŧo", "toŧo", "ɉoŧo", "loŧo", "moŧo", "skabioŧo", "boŧo", "joŧo", "kavieƚo", "sieƚo", "jeƚo", "pomeƚo", "kontrapeƚo", "kueƚo", "kaosteƚo", "pexeƚo", "veƚo", "strapeƚo", "kavakaveƚo", "brògeƚo", "ceƚo", "kaveeƚo", "kontropeƚo", "xeƚo", "bruxapeƚo", "eƚo", "peteƚo", "foɉoxo", "noɉoxo", "voɉoxo", "xoɉoxo", "oɉoxo", "orxo", "pòrta–ovo", "véskovo", "vovo", "xlansaƚovo", "xlanŧalovo", "novo", "ovo", "védovo", "portavovo", "pòrta–vovo", "lovo", "ƚovo", "portaovo", "xlansalovo", "véovo", "obrobioxo", "xmaƚankoñoxo", "roñoxo", "malenkoñoxo", "maƚenkoñoxo", "xmalankoñoxo", "analidente", "franxoxente", "eseƚente", "romansinente", "intitoƚente", "ƚekiñente", "lidierente", "kaveŧente", "botixente", "ƚogorente", "voƚente", "agredente", "ingrotoƚente", "xenbente", "superbente", "xboxemente", "spreferente", "xbisaɉente", "sparnuɉente", "perkorente", "sparpañente", "inbalinente", "koramidente", "guañente", "dejarente", "solesitente", "sestente", "ƚavente", "scopidente", "spređudigente", "xmarente", "tilente", "maluxente", "fanfarente", "tindonente", "frabikente", "destegolente", "inbarente", "dexgrapiente", "tormentente", "oblente", "spaxemente", "inskeletrente", "sifrente", "guantente", "opinente", "sparpanente", "favoriđente", "xboente", "inpermalente", "ardondente", "fradeliđente", "pinsente", "stente", "ŧoetente", "xbanpolente", "josolente", "intestardente", "bekotente", "despendiente", "bigolente", "interogente", "potensiente", "sfiƚasente", "lapasente", "ronfente", "sfioridente", "remoŧion", "autorixasion", "radaŧion", "filtraŧion", "tinason", "xlovon", "paragon", "faraon", "manaŧon", "inrotulaŧion", "truson", "sonorixaŧion", "frustadon", "kalandron", "saƚison", "ŧavaton", "saƚudasion", "malision", "groon", "emision", "peaŧon", "arđaron", "sifon", "xɉonfon", "bandieron", "boladon", "goldon", "lion", "ubigaŧion", "folon", "gucon", "kotolon", "moreton", "tormenton", "garlon", "rosteƚon", "piavolon", "previxion", "ventron", "greson", "salvaŧion", "milion", "legaŧion", "brustolon", "guidon", "seƚadon", "bagajon", "pekatoron", "dopion", "velion", "sudiŧion", "ventolon", "sinserasion", "galidaŧion", "erbion", "ƚimon", "cokoƚaton", "xustapoxiŧion", "darion", "rođeton", "braŧon", "xganbeton", "stafon", "union", "skuakueron", "moreƚon", "litigon", "arđiñon", "baston", "tavaron", "alberon", "boton", "kospeton", "torŧon", "torcon", "ƚerijon", "proporŧion", "bregieron", "xganberlon", "xguaɉaton", "sporton", "dexaponto", "konfronto", "dekonto", "defonto", "pronto", "straponto", "rendikonto", "đonto", "rakonto", "konto", "soxonto", "ponto", "soraponto", "arxonto", "seraponto", "ardonto", "raxonto", "sara–ponto", "arđonto", "rađonto", "soraxonto", "tokaponto", "sođonto", "rexikonto", "sorađonto", "skonto", "sotoponto", "sodonto", "radonto", "onto", "soradonto", "monto", "bixonto", "sèra–ponto", "tornakonto", "bruxapel", "konprensíbel", "kuel", "fiel", "riferíbel", "el", "kontropel", "kaveel", "débel", "cel", "pomel", "ŧiel", "kontrapel", "pexel", "jel", "kel", "strapel", "kaviel", "fiével", "vel", "brògel", "inviolàbel", "sedoto", "voto", "kurakondoto", "dedoto", "feroto", "salvakondoto", "doto", "kura–kondoto", "introdoto", "roto", "intaroto", "rekondoto", "interoto", "kondoto", "feroroto", "koroto", "skoto", "bioto", "dèspoto", "mèdoto", "fèro–roto", "reprodoto", "prodoto", "groto", "gređo", "laveđo", "xboteđo", "mexerekordioxo", "obrobrioxo", "jàronte", "solféjonte", "xbravéđonte", "kanónonte", "búligonte", "bragétonte", "inɉúrionte", "spàkonte", "denòconte", "tamúsonte", "xbiƚànsionte", "đenaralíđonte", "paríxonte", "múltonte", "lóronte", "ŧúpegonte", "fórmolonte", "strađúronte", "marmoríxonte", "popétonte", "deskàvedonte", "xlúxegonte", "negréxonte", "dópionte", "ústonte", "tenporéjonte", "èrdonte", "kuèstuonte", "đògonte", "fròlonte", "inkuèrconte", "ingaƚúsonte", "xgílsonte", "visígonte", "kòƚonte", "espàtrionte", "venesiànonte", "dexradíxonte", "súlonte", "skúrionte", "piàsonte", "incòstronte", "ŧavàrionte", "mucegàronte", "fiàbonte", "đirlàndonte", "prométonte", "pégolonte", "scàtonte", "pànsonte", "artíkolonte", "miètonte", "aƚàrmonte", "tavanéxonte", "brilàntonte", "xàfonte", "doméstegonte", "pólxonte", "bàlonte", "dexjémonte", "paŧídonte", "sémenonte", "stípuƚonte", "konđéñonte", "ràxonte", "kojónbaronte", "ƚímonte", "palangónonte", "pèrmutonte", "órden", "malmostoxo", "kostoxo", "mostoxo", "oƚioxo", "orko", "porko", "forko", "sporko", "senpioldo", "senplisioldo", "sinpioldo", "ŧeoldo", "protexo", "sotintexo", "lavexo", "bilexo", "desparexo", "prexo", "soraintexo", "intraprexo", "propexo", "krexo", "entexo", "preintexo", "sotointexo", "perintexo", "parintexo", "xbotexo", "spexo", "resiexo", "splexo", "spàrexo", "fexo", "sospexo", "vilipexo", "ƚavexo", "texo", "insexo", "rexo", "pontexo", "suspexo", "malintexo", "inŧexo", "sorprexo", "raprexo", "ofexo", "sfexo", "parexo", "estexo", "maƚintexo", "sorintexo", "viƚipexo", "biƚexo", "xbertexo", "xgrexo", "xvegramento", "dormensamento", "akorđimento", "malsontamento", "ƚigamento", "rekordamento", "renkresimento", "basixamento", "fotivento", "stento", "stravinamento", "caceramento", "trovamento", "uƚimento", "cacaramento", "aŧokimento", "torbolamento", "pastisamento", "mostravento", "joŧolamento", "roxamento", "sasinamento", "ŧimento", "bonimento", "bagoƚamento", "brasamento", "nuvoƚamento", "asento", "predegamento", "spavento", "indurimento", "torbiamento", "ƚuxamento", "rueƚamento", "malkontento", "kavaƚamento", "sonkamento", "valimento", "skonkasamento", "spegaŧamento", "seneɉamento", "ingropamento", "konsamento", "indupionamento", "komento", "josolamento", "flatulento", "buƚegamento", "inpedimento", "desarvelamento", "ƚisamento", "suɉarimento", "konvento", "agravamento", "skuinternamento", "taɉusamento", "falimento", "danamento", "trabakoƚamento", "baŧixamento", "latamento", "basilamento", "dexŧervelamento", "stođo", "trođo", "pođo", "nevodo", "gederodo", "stodo", "vodo", "ànodo", "trodo", "mètodo", "anètodo", "período", "neodo", "podo", "kòmodo", "orđo", "kortelaŧeto", "biƚietereto", "skapineto", "malaneto", "saoreto", "kocereto", "boraoreto", "formaɉereto", "coeto", "miseto", "datolereto", "saŧieto", "kaparoŧoleto", "farinaroleto", "naransereto", "fisoleto", "granŧideto", "bronbeƚereto", "vensatoreto", "kalegereto", "versaoreto", "ƚuamereto", "olanereto", "paɉaroleto", "ƚustraoreto", "roxiñoleto", "kalŧareto", "veriolereto", "ŧeriexereto", "kaloreto", "kalamereto", "kaloroxeto", "grumeto", "manganereto", "podoleto", "formajereto", "triveƚaoreto", "destrameđaoreto", "molonereto", "sufieto", "ŧerfoɉereto", "ƚumineto", "soneto", "baƚonereto", "ruxiñoleto", "panpalugeto", "toƚineto", "kaƚiseto", "subioƚereto", "ƚeutereto", "vovaroƚeto", "skabeƚeto", "skuarseto", "kordaroleto", "grataroƚeto", "straneto", "kolmeƚeto", "sforŧanereto", "nevodeto", "peƚeto", "guseto", "peƚaƚereto", "seradurereto", "suketo", "margaritereto", "đoeto", "konŧereto", "paraoreto", "ŧerfojereto", "scavàđen", "ƚankúxen", "marúxen", "freskúđen", "rúđen", "arko–vèrxen", "taja–fen", "teren", "sènen", "infiúxen", "fenòmen", "magađen", "àxen", "lentíden", "bokàxen", "inbeŧilàxen", "grèmen", "kaƚúxen", "pèten", "taɉafen", "terapien", "notaben", "bokàden", "vertíxen", "stra-pien", "kalúđen", "strapien", "réden", "ultrateren", "skargalàxen", "ŧéxen", "strabúxen", "pien", "repien", "ƚentíxen", "meden", "inkúđen", "skargaƚàxen", "sen", "kalíđen", "màrden", "scopàđen", "incetúden", "freskúxen", "jóven", "freskúden", "marúden", "tajafen", "grèben", "lankúđen", "piantàden", "bokàđen", "lankúden", "fien", "vèrgen", "orbàxen", "piantàxen", "màrxen", "ben", "paragrànden", "kalúden", "solitúden", "seren", "làvren", "magaxen", "kalúxen", "kàrpen", "similituden", "lankúxen", "sèlen", "vèrđen", "arkovèrxen", "mexen", "velen", "ŧérŧen", "scavàden", "guardamagaxen", "lentíđen", "inbesilàxen", "inkúxen", "magaden", "tientinben", "vírxen", "xojoxo", "ojoxo", "fojoxo", "vojoxo", "nojoxo", "xbrodoloxo", "katisoloxo", "riŧoloxo", "fregoloxo", "anpoloxo", "katorigoloxo", "sasoloxo", "gropoloxo", "risoloxo", "xgronsoloxo", "fredoloxo", "barboloxo", "xgronŧoloxo", "skrupoloxo", "skrovoloxo", "kagoloxo", "kokoloxo", "ridikoloxo", "xbrisoloxo", "mirakoloxo", "mokoloxo", "soŧoloxo", "mitikoloxo", "bruskoloxo", "ŧakoloxo", "nuvoloxo", "straŧoloxo", "bagoloxo", "semoloxo", "brufoloxo", "strasoloxo", "perigoloxo", "tuberkoloxo", "katiŧoloxo", "goloxo", "petoloxo", "perikoloxo", "xbrindoloxo", "piatoloxo", "spetakoloxo", "sosoloxo", "prigoloxo", "vergoloxo", "kaoroso", "toso", "skabioso", "koaroso", "boso", "bragoso", "ɉoso", "infraroso", "loso", "ƚoso", "moso", "joso", "robegoso", "paƚoso", "petoroso", "pèto–roso", "kodaroso", "biroso", "paloso", "scoso", "promoxo", "koño", "moño", "barkoño", "kodoño", "sfroño", "ingordo", "tordo", "balordo", "baƚordo", "parabordo", "sordo", "bordo", "xlordo", "ordo", "ƚeñoxo", "speƚumoxo", "skisiñoxo", "bordeloxo", "kreoxo", "malaugurioxo", "inŧendoxo", "montoxo", "robegoxo", "koraɉoxo", "piaɉoxo", "spelusoxo", "krestoxo", "astioxo", "maƚenoxo", "flatoxo", "kontrastoxo", "likoxo", "paltanoxo", "xbosegoxo", "spiritoxo", "xmanioxo", "moxo", "peƚoxo", "fanoxo", "oŧioxo", "strasoxo", "malenoxo", "kaloxo", "skorusoxo", "skoruŧoxo", "rekalŧitroxo", "ƚuminoxo", "veseoxo", "lerijoxo", "piovoxo", "deveoxo", "xbrisoxo", "riondoxo", "leñoxo", "xmerdoxo", "koraxoxo", "spreŧilioxo", "scamoxo", "ƚeproxo", "prunoxo", "skejoxo", "velenoxo", "grasioxo", "spongoxo", "meravejoxo", "paludoxo", "begoxo", "lekoxo", "superstisioxo", "despetoxo", "inŧendioxo", "bakanoxo", "virtulioxo", "rivoltoxo", "sekajinoxo", "voƚontaroxo", "sasoxo", "skuarŧoxo", "ventoxo", "xmolsinoxo", "karnoxo", "nebuloxo", "verenoxo", "revoltoxo", "xmorfioxo", "nebioxo", "sekaɉinoxo", "skuarsoxo", "ƚikoxo", "veƚenoxo", "rixegoxo", "vènedo", "sínedo", "notevedo", "dedo", "refredo", "gredo", "kontraspedo", "tapedo", "boƚedo", "kredo", "kòspedo", "fredo", "lavedo", "axedo", "tréspedo", "kanedo", "boledo", "xbotedo", "sòsedo", "spedo", "sòŧedo", "kavaor", "strondaor", "proditor", "oldor", "governaor", "voltor", "fondidor", "ƚéor", "apaltaor", "debitor", "trusor", "vivador", "robor", "traitor", "miraor", "skritor", "laoraor", "stuŧegaor", "saxaor", "menor", "suxarior", "provedaor", "spontitor", "luxor", "ŧimor", "ingranditor", "pexor", "penđador", "balotaor", "konplementador", "kalkolaor", "belumor", "nolexaor", "piuxor", "milefior", "batikor", "serkaor", "kalor", "posterior", "gladiator", "bonsior", "kargaor", "retor", "onor", "sferdor", "tarixaor", "lexixlaor", "minusaor", "radiotraxmetitor", "operaor", "bañafior", "sprendor", "ŧiterior", "ŧapaor", "pekator", "peskaor", "spasaor", "konfortaor", "anterior", "interior", "ŧexor", "iƚuminaor", "camor", "burataor", "ventilaor", "malfator", "sagomaor", "vivaor", "kantor", "setaor", "petaor", "stronđaor", "pistor", "tremor", "vendidor", "kalkoƚador", "koniaor", "trivelaor", "braŧaor", "inbasador", "kreaor", "maxor", "skarmofioxo", "isteso", "senpieso", "tateso", "xmorbeso", "boɉeso", "kareso", "steso", "ñoñoleso", "kageso", "kokoƚeso", "boreso", "goƚeso", "munegeso", "bibieso", "ribreso", "morbieso", "dotoreso", "emeso", "permeso", "buƚeso", "strioneso", "lekeso", "gongoƚeso", "puteƚeso", "paƚeso", "dexmestegeso", "viƚaneso", "bojeso", "piageso", "xentilomeneso", "piajeso", "xgibieso", "skarmofieso", "gengeso", "petegoleso", "sigaƚeso", "ƚadroneso", "kagoleso", "konsaƚaveso", "frameso", "strafareso", "baroneso", "traxmeso", "skivoƚeso", "cakoƚeso", "xmorfieso", "pitokeso", "stomegeso", "bordeleso", "sproteso", "ƚanpeso", "kavaƚeso", "neso", "poltroneso", "muƚeso", "bufoneso", "piavoleso", "xbrikeso", "piaɉeso", "skamufieso", "pakañeso", "mañoleso", "goleso", "menareso", "rabioxeso", "gexeso", "stufeso", "trameso", "fioƚeso", "meƚeso", "sotomeso", "piatoƚeso", "moroxeso", "skamofieso", "fiseso", "rufianeso", "bagoƚeso", "kaveso", "ñoñoƚeso", "xboko", "reŧíproko", "poko", "scapaŧoko", "ŧoko", "scapasoko", "scapa–soko", "pàroko", "toko", "scapa–ŧoko", "ñoko", "resíproko", "olioxo", "trémoƚo", "xíroƚo", "pesoƚo", "rasaroƚo", "biavaroƚo", "garétoƚo", "grúmoƚo", "vèrgoƚo", "verloƚo", "bòdoƚo", "gataroƚo", "pípoƚo", "saldaroƚo", "narúnkoƚo", "revendígoƚo", "barkaroƚo", "nèspoƚo", "kapítoƚo", "bixékoƚo", "nisoƚo", "xgriñapòpoƚo", "ranabòtoƚo", "bexaroƚo", "skrimiàtoƚo", "kòtoƚo", "riàoƚo", "tubèrkoƚo", "móskoƚo", "franségoƚo", "scoparoƚo", "bròkoƚo", "ridòtoƚo", "ƚónboƚo", "garúxoƚo", "primaroƚo", "xbríndoƚo", "bígoƚo", "miñòñoƚo", "róndoƚo", "resetàkoƚo", "bugaroƚo", "strupiaskóvoƚo", "orxoƚo", "bàtoƚo", "fusténgoƚo", "brónboƚo", "spondoƚo", "pékoƚo", "búfoƚo", "réfoƚo", "bixígoƚo", "brànkoƚo", "poƚo", "nónsoƚo", "trataroƚo", "basaroƚo", "sàpoƚo", "físoƚo", "sotokòtoƚo", "dóngoƚo", "símoƚo", "skapusioƚo", "bronbeɉoƚo", "ƚaxaroƚo", "kortígoƚo", "múskoƚo", "stisaroƚo", "jéskoƚo", "kivoƚo", "kodarósoƚo", "orbégoƚo", "ƚibèrkoƚo", "pómoƚo", "rosiñoƚo", "apòstoƚo", "bronbijoƚo", "àmoƚo", "ucaroƚo", "sporkeŧo", "konŧalaveŧo", "stolideŧo", "kaveŧo", "ladroneŧo", "laveŧo", "fioleŧo", "goleŧo", "kavaleŧo", "fureŧo", "pakañeŧo", "mateŧo", "gongoleŧo", "bulegeŧo", "puteleŧo", "inbriageŧo", "diavoleŧo", "putaneŧo", "buleŧo", "đentilomeneŧo", "demonieŧo", "ñeñeŧo", "fifeŧo", "balbeŧo", "morbineŧo", "rabieŧo", "aveŧo", "tatareŧo", "strolegeŧo", "vermineŧo", "kokoleŧo", "sonoleŧo", "bandiereŧo", "xñanfeŧo", "beŧo", "vilaneŧo", "spiriteŧo", "cakoleŧo", "xbugeŧo", "cacareŧo", "skivoleŧo", "lanpeŧo", "cetineŧo", "karoñeŧo", "burleŧo", "strigeŧo", "muleŧo", "peteŧo", "piatoleŧo", "kokoneŧo", "vermeneŧo", "xmorbeŧo", "fufiñeŧo", "tateŧo", "ñoñoleŧo", "kageŧo", "morbieŧo", "dentilomeneŧo", "piajeŧo", "stranbeŧo", "bagoleŧo", "goloxeŧo", "boɉeŧo", "kareŧo", "senpieŧo", "bibieŧo", "piageŧo", "boreŧo", "munegeŧo", "ribreŧo", "dotoreŧo", "xmorfioxeŧo", "gengeŧo", "lekeŧo", "bojeŧo", "skarmofieŧo", "xgibieŧo", "petegoleŧo", "maƚenkonioxo", "akrimonioxo", "malankonioxo", "maƚankonioxo", "malenkonioxo", "bruskoƚoxo", "xbrodoƚoxo", "anpoƚoxo", "sasoƚoxo", "perigoƚoxo", "xbrindoƚoxo", "sosoƚoxo", "spetakoƚoxo", "vergoƚoxo", "semoƚoxo", "risoƚoxo", "katorigoƚoxo", "perikoƚoxo", "prigoƚoxo", "piatoƚoxo", "katisoƚoxo", "petoƚoxo", "xbrisoƚoxo", "strasoƚoxo", "brufoƚoxo", "nuvoƚoxo", "skrupoƚoxo", "sakoƚoxo", "fredoƚoxo", "tuberkoƚoxo", "goƚoxo", "mokoƚoxo", "barboƚoxo", "kokoƚoxo", "ridikoƚoxo", "xgronsoƚoxo", "gropoƚoxo", "fregoƚoxo", "bagoƚoxo", "mirakoƚoxo", "mitikoƚoxo", "skrovoƚoxo", "kagoƚoxo", "koroxo", "kaƚoroxo", "saoroxo", "vigoroxo", "valoroxo", "moroxo", "amoroxo", "kaloroxo", "rigoroxo", "vaƚoroxo", "vitorioxo", "glorioxo", "monŧelexe", "làrexe", "kòdexe", "ƚexe", "seguxinexe", "éndexe", "inglexe", "élexe", "pistorexe", "kararexe", "pòlexe", "àrpexe", "kontramantexe", "màstexe", "borgexe", "lexe", "kòpexe", "rèvexe", "pòƚexe", "konejanexe", "muranexe", "veneŧiexe", "paƚexe", "orèvexe", "kornudexe", "provexe", "đenoexe", "orexe", "cinexe", "èƚexe", "púlexe", "léexe", "turkexe", "kàƚexe", "monseƚexe", "júdexe", "krémexe", "éƚexe", "ŧímexe", "koneɉanexe", "préndexe", "monselexe", "sàlexe", "penexe", "venesiexe", "montebelunexe", "sélexe", "palexe", "poƚexe", "séƚexe", "pistolexe", "fórvexe", "orèdexe", "sàƚexe", "polexe", "ƚàrexe", "danexe", "pistoƚexe", "màntexe", "montebeƚunexe", "paexe", "krèmexe", "ƚéexe", "púƚexe", "pavexe", "príndexe", "índexe", "féƚexe", "kàlexe", "veronexe", "fórfexe", "fórbexe", "đenovexe", "àspexe", "kalŧexe", "àstexe", "èlexe", "đúdexe", "félexe", "kalsexe", "sandonatexe");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.flatMap(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX).stream())
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		List<LineEntry> expectedCompactedRules = Arrays.asList(
			new LineEntry("olko", SetHelper.setOf("ulki"), "olko", Arrays.asList("bifolko", "olko", "folko", "beolko", "solko", "biolko")),
			new LineEntry("evo", SetHelper.setOf("ivi"), "evo", Arrays.asList("grevo", "trevo", "kànevo", "arlevo", "véskevo")),
			new LineEntry("eño", SetHelper.setOf("iñi"), "eño", Arrays.asList("dexeño", "maƚedeño", "seño", "sosteño", "kondeño", "pareño", "maledeño", "konđeño", "reteño", "cipileño", "cipiƚeño", "kontraseño", "inđeño", "maxeño", "deño", "despeño", "lexeño", "soveño", "ƚexeño", "ordeño", "konveño", "konxeño", "poseño")),
			new LineEntry("ol", SetHelper.setOf("uli"), "ol", Arrays.asList("pisarol", "skodarol", "sotarol", "brònbol", "rovejol", "kódol", "porésol", "èrbol", "korósol", "fínfol", "buxigàtol", "narónkol", "jèvol", "fífol", "poréŧol", "onbrígol", "ígol", "ucarol", "kuriàtol", "réfol", "postríbol", "piàvol", "deñévol", "fisol", "ovarol", "xjoŧarol", "íxol", "ranarol", "kortívol", "karígol", "pípol", "sièol", "brúfol", "skrópol", "pisàndol", "fiŧol", "libarol", "ròdol", "đógol", "revendígol", "núgol", "desútol", "orbexígol", "koriàndol", "skapuŧiol", "màskol", "spónxol", "jègol", "róndol", "ràxol", "saldarol", "beđarol", "tastarol", "intíngol", "piàgol", "ponterol", "faxiol", "kortígol", "persénbol", "bròkol", "kòtol", "kanarol", "trémol", "tòpol", "supiarol", "biavarol", "prexudiŧiévol", "bògol", "búsol", "rapónsol", "parol", "liŧarol", "bronbeɉol", "níol", "perdonévol", "sóŧol", "garofol", "faŧiol", "rapónŧol", "faxol", "arđigògol", "braŧarol", "pikàñol", "sósol", "ŧendrarol", "piàol", "morévol")),
			new LineEntry("orse", SetHelper.setOf("ursi"), "orse", Arrays.asList("forse")),
			new LineEntry("eko", SetHelper.setOf("iki"), "eko", Arrays.asList("straleko", "baƚeko", "seko", "ŧanbeko", "mexoseko", "beko", "straƚeko", "međoseko", "arketeko", "medoseko", "baleko", "xñeko", "pasteko")),
			new LineEntry("er", SetHelper.setOf("iri"), "er", Arrays.asList("tomier", "soaxier", "pólver", "xberoer", "viperier", "kalsinier", "mulinier", "peƚatier", "krivelier", "balkoner", "sanbugier", "nespolier", "latonier", "đaletier", "sorboƚier", "baretier", "magađener", "bísker", "samitier", "angurer", "bronbexier", "ŧarexier", "ganŧier", "pitarier", "đojelier", "stopier", "pelatier", "poster", "trameser", "miliardier", "spuƚexier", "fogolier", "lagrimier", "poƚastrier", "doaner", "ƚusernier", "karoboƚier", "figier", "pionbier", "manteñer", "ŧínger", "riŧolier", "baƚestrier", "kukúmer", "lavranier", "peƚaƚer", "konŧer", "meƚionier", "paraŧéner", "arminier", "anŧipresier", "muƚinier", "fornier", "fragier", "lanŧier", "bolsier", "kriveƚier", "perier", "dènder", "bronđier", "korbelier", "fontegier", "spulexier", "vivier", "kredenser", "pelaler", "maskarier", "piader", "armeƚinier", "ofeƚer", "diamantier", "nespoƚier", "portinier", "fabriŧer", "sorbolier", "peƚisier", "gadeter", "librier")),
			new LineEntry("oŧo", SetHelper.setOf("uŧi"), "oŧo", Arrays.asList("goŧo", "bragoŧo", "biroŧo", "robegoŧo", "scoŧo", "toŧo", "ɉoŧo", "loŧo", "moŧo", "skabioŧo", "boŧo", "joŧo")),
			new LineEntry("eƚo", SetHelper.setOf("iƚi"), "eƚo", Arrays.asList("kavieƚo", "sieƚo", "jeƚo", "pomeƚo", "kontrapeƚo", "kueƚo", "kaosteƚo", "pexeƚo", "veƚo", "strapeƚo", "kavakaveƚo", "brògeƚo", "ceƚo", "kaveeƚo", "kontropeƚo", "xeƚo", "bruxapeƚo", "eƚo", "peteƚo")),
			new LineEntry("oɉoxo", SetHelper.setOf("oɉuxi", "uɉuxi"), "oɉoxo", Arrays.asList("foɉoxo", "noɉoxo", "voɉoxo", "xoɉoxo", "oɉoxo")),
			new LineEntry("orxo", SetHelper.setOf("urxi"), "orxo", Arrays.asList("orxo")),
			new LineEntry("ovo", SetHelper.setOf("uvi"), "ovo", Arrays.asList("pòrta–ovo", "véskovo", "vovo", "xlansaƚovo", "xlanŧalovo", "novo", "ovo", "védovo", "portavovo", "pòrta–vovo", "lovo", "ƚovo", "portaovo", "xlansalovo", "véovo")),
			new LineEntry("obioxo", SetHelper.setOf("obiuxi", "ubiuxi"), "obioxo", Arrays.asList("obrobioxo")),
			new LineEntry("oñoxo", SetHelper.setOf("uñuxi", "oñuxi"), "oñoxo", Arrays.asList("xmaƚankoñoxo", "roñoxo", "malenkoñoxo", "maƚenkoñoxo", "xmalankoñoxo")),
			new LineEntry("ente", SetHelper.setOf("inti"), "ente", Arrays.asList("analidente", "franxoxente", "eseƚente", "romansinente", "intitoƚente", "ƚekiñente", "lidierente", "kaveŧente", "botixente", "ƚogorente", "voƚente", "agredente", "ingrotoƚente", "xenbente", "superbente", "xboxemente", "spreferente", "xbisaɉente", "sparnuɉente", "perkorente", "sparpañente", "inbalinente", "koramidente", "guañente", "dejarente", "solesitente", "sestente", "ƚavente", "scopidente", "spređudigente", "xmarente", "tilente", "maluxente", "fanfarente", "tindonente", "frabikente", "destegolente", "inbarente", "dexgrapiente", "tormentente", "oblente", "spaxemente", "inskeletrente", "sifrente", "guantente", "opinente", "sparpanente", "favoriđente", "xboente", "inpermalente", "ardondente", "fradeliđente", "pinsente", "stente", "ŧoetente", "xbanpolente", "josolente", "intestardente", "bekotente", "despendiente", "bigolente", "interogente", "potensiente", "sfiƚasente", "lapasente", "ronfente", "sfioridente")),
			new LineEntry("on", SetHelper.setOf("uni"), "on", Arrays.asList("remoŧion", "autorixasion", "radaŧion", "filtraŧion", "tinason", "xlovon", "paragon", "faraon", "manaŧon", "inrotulaŧion", "truson", "sonorixaŧion", "frustadon", "kalandron", "saƚison", "ŧavaton", "saƚudasion", "malision", "groon", "emision", "peaŧon", "arđaron", "sifon", "xɉonfon", "bandieron", "boladon", "goldon", "lion", "ubigaŧion", "folon", "gucon", "kotolon", "moreton", "tormenton", "garlon", "rosteƚon", "piavolon", "previxion", "ventron", "greson", "salvaŧion", "milion", "legaŧion", "brustolon", "guidon", "seƚadon", "bagajon", "pekatoron", "dopion", "velion", "sudiŧion", "ventolon", "sinserasion", "galidaŧion", "erbion", "ƚimon", "cokoƚaton", "xustapoxiŧion", "darion", "rođeton", "braŧon", "xganbeton", "stafon", "union", "skuakueron", "moreƚon", "litigon", "arđiñon", "baston", "tavaron", "alberon", "boton", "kospeton", "torŧon", "torcon", "ƚerijon", "proporŧion", "bregieron", "xganberlon", "xguaɉaton", "sporton")),
			new LineEntry("onto", SetHelper.setOf("unti"), "onto", Arrays.asList("dexaponto", "konfronto", "dekonto", "defonto", "pronto", "straponto", "rendikonto", "đonto", "rakonto", "konto", "soxonto", "ponto", "soraponto", "arxonto", "seraponto", "ardonto", "raxonto", "sara–ponto", "arđonto", "rađonto", "soraxonto", "tokaponto", "sođonto", "rexikonto", "sorađonto", "skonto", "sotoponto", "sodonto", "radonto", "onto", "soradonto", "monto", "bixonto", "sèra–ponto", "tornakonto")),
			new LineEntry("el", SetHelper.setOf("ili"), "el", Arrays.asList("bruxapel", "konprensíbel", "kuel", "fiel", "riferíbel", "el", "kontropel", "kaveel", "débel", "cel", "pomel", "ŧiel", "kontrapel", "pexel", "jel", "kel", "strapel", "kaviel", "fiével", "vel", "brògel", "inviolàbel")),
			new LineEntry("oto", SetHelper.setOf("uti"), "oto", Arrays.asList("sedoto", "voto", "kurakondoto", "dedoto", "feroto", "salvakondoto", "doto", "kura–kondoto", "introdoto", "roto", "intaroto", "rekondoto", "interoto", "kondoto", "feroroto", "koroto", "skoto", "bioto", "dèspoto", "mèdoto", "fèro–roto", "reprodoto", "prodoto", "groto")),
			new LineEntry("eđo", SetHelper.setOf("iđi"), "eđo", Arrays.asList("gređo", "laveđo", "xboteđo")),
			new LineEntry("ordioxo", SetHelper.setOf("urdiuxi", "ordiuxi"), "ordioxo", Arrays.asList("mexerekordioxo")),
			new LineEntry("obrioxo", SetHelper.setOf("ubriuxi", "obriuxi"), "obrioxo", Arrays.asList("obrobrioxo")),
			new LineEntry("onte", SetHelper.setOf("unti"), "onte", Arrays.asList("jàronte", "solféjonte", "xbravéđonte", "kanónonte", "búligonte", "bragétonte", "inɉúrionte", "spàkonte", "denòconte", "tamúsonte", "xbiƚànsionte", "đenaralíđonte", "paríxonte", "múltonte", "lóronte", "ŧúpegonte", "fórmolonte", "strađúronte", "marmoríxonte", "popétonte", "deskàvedonte", "xlúxegonte", "negréxonte", "dópionte", "ústonte", "tenporéjonte", "èrdonte", "kuèstuonte", "đògonte", "fròlonte", "inkuèrconte", "ingaƚúsonte", "xgílsonte", "visígonte", "kòƚonte", "espàtrionte", "venesiànonte", "dexradíxonte", "súlonte", "skúrionte", "piàsonte", "incòstronte", "ŧavàrionte", "mucegàronte", "fiàbonte", "đirlàndonte", "prométonte", "pégolonte", "scàtonte", "pànsonte", "artíkolonte", "miètonte", "aƚàrmonte", "tavanéxonte", "brilàntonte", "xàfonte", "doméstegonte", "pólxonte", "bàlonte", "dexjémonte", "paŧídonte", "sémenonte", "stípuƚonte", "konđéñonte", "ràxonte", "kojónbaronte", "ƚímonte", "palangónonte", "pèrmutonte")),
			new LineEntry("órden", SetHelper.setOf("órdini", "úrdini"), "órden", Arrays.asList("órden")),
			new LineEntry("ostoxo", SetHelper.setOf("ustuxi", "ostuxi"), "ostoxo", Arrays.asList("malmostoxo", "kostoxo", "mostoxo")),
			new LineEntry("oƚioxo", SetHelper.setOf("oƚiuxi", "uƚiuxi"), "oƚioxo", Arrays.asList("oƚioxo")),
			new LineEntry("orko", SetHelper.setOf("urki"), "orko", Arrays.asList("orko", "porko", "forko", "sporko")),
			new LineEntry("oldo", SetHelper.setOf("uldi"), "oldo", Arrays.asList("senpioldo", "senplisioldo", "sinpioldo", "ŧeoldo")),
			new LineEntry("exo", SetHelper.setOf("ixi"), "exo", Arrays.asList("protexo", "sotintexo", "lavexo", "bilexo", "desparexo", "prexo", "soraintexo", "intraprexo", "propexo", "krexo", "entexo", "preintexo", "sotointexo", "perintexo", "parintexo", "xbotexo", "spexo", "resiexo", "splexo", "spàrexo", "fexo", "sospexo", "vilipexo", "ƚavexo", "texo", "insexo", "rexo", "pontexo", "suspexo", "malintexo", "inŧexo", "sorprexo", "raprexo", "ofexo", "sfexo", "parexo", "estexo", "maƚintexo", "sorintexo", "viƚipexo", "biƚexo", "xbertexo", "xgrexo")),
			new LineEntry("ento", SetHelper.setOf("inti"), "ento", Arrays.asList("xvegramento", "dormensamento", "akorđimento", "malsontamento", "ƚigamento", "rekordamento", "renkresimento", "basixamento", "fotivento", "stento", "stravinamento", "caceramento", "trovamento", "uƚimento", "cacaramento", "aŧokimento", "torbolamento", "pastisamento", "mostravento", "joŧolamento", "roxamento", "sasinamento", "ŧimento", "bonimento", "bagoƚamento", "brasamento", "nuvoƚamento", "asento", "predegamento", "spavento", "indurimento", "torbiamento", "ƚuxamento", "rueƚamento", "malkontento", "kavaƚamento", "sonkamento", "valimento", "skonkasamento", "spegaŧamento", "seneɉamento", "ingropamento", "konsamento", "indupionamento", "komento", "josolamento", "flatulento", "buƚegamento", "inpedimento", "desarvelamento", "ƚisamento", "suɉarimento", "konvento", "agravamento", "skuinternamento", "taɉusamento", "falimento", "danamento", "trabakoƚamento", "baŧixamento", "latamento", "basilamento", "dexŧervelamento")),
			new LineEntry("ođo", SetHelper.setOf("uđi"), "ođo", Arrays.asList("stođo", "trođo", "pođo")),
			new LineEntry("odo", SetHelper.setOf("udi"), "odo", Arrays.asList("nevodo", "gederodo", "stodo", "vodo", "ànodo", "trodo", "mètodo", "anètodo", "período", "neodo", "podo", "kòmodo")),
			new LineEntry("orđo", SetHelper.setOf("urđi"), "orđo", Arrays.asList("orđo")),
			new LineEntry("eto", SetHelper.setOf("iti"), "eto", Arrays.asList("kortelaŧeto", "biƚietereto", "skapineto", "malaneto", "saoreto", "kocereto", "boraoreto", "formaɉereto", "coeto", "miseto", "datolereto", "saŧieto", "kaparoŧoleto", "farinaroleto", "naransereto", "fisoleto", "granŧideto", "bronbeƚereto", "vensatoreto", "kalegereto", "versaoreto", "ƚuamereto", "olanereto", "paɉaroleto", "ƚustraoreto", "roxiñoleto", "kalŧareto", "veriolereto", "ŧeriexereto", "kaloreto", "kalamereto", "kaloroxeto", "grumeto", "manganereto", "podoleto", "formajereto", "triveƚaoreto", "destrameđaoreto", "molonereto", "sufieto", "ŧerfoɉereto", "ƚumineto", "soneto", "baƚonereto", "ruxiñoleto", "panpalugeto", "toƚineto", "kaƚiseto", "subioƚereto", "ƚeutereto", "vovaroƚeto", "skabeƚeto", "skuarseto", "kordaroleto", "grataroƚeto", "straneto", "kolmeƚeto", "sforŧanereto", "nevodeto", "peƚeto", "guseto", "peƚaƚereto", "seradurereto", "suketo", "margaritereto", "đoeto", "konŧereto", "paraoreto", "ŧerfojereto")),
			new LineEntry("en", SetHelper.setOf("ini"), "en", Arrays.asList("scavàđen", "ƚankúxen", "marúxen", "freskúđen", "rúđen", "arko–vèrxen", "taja–fen", "teren", "sènen", "infiúxen", "fenòmen", "magađen", "àxen", "lentíden", "bokàxen", "inbeŧilàxen", "grèmen", "kaƚúxen", "pèten", "taɉafen", "terapien", "notaben", "bokàden", "vertíxen", "stra-pien", "kalúđen", "strapien", "réden", "ultrateren", "skargalàxen", "ŧéxen", "strabúxen", "pien", "repien", "ƚentíxen", "meden", "inkúđen", "skargaƚàxen", "sen", "kalíđen", "màrden", "scopàđen", "incetúden", "freskúxen", "jóven", "freskúden", "marúden", "tajafen", "grèben", "lankúđen", "piantàden", "bokàđen", "lankúden", "fien", "vèrgen", "orbàxen", "piantàxen", "màrxen", "ben", "paragrànden", "kalúden", "solitúden", "seren", "làvren", "magaxen", "kalúxen", "kàrpen", "similituden", "lankúxen", "sèlen", "vèrđen", "arkovèrxen", "mexen", "velen", "ŧérŧen", "scavàden", "guardamagaxen", "lentíđen", "inbesilàxen", "inkúxen", "magaden", "tientinben", "vírxen")),
			new LineEntry("ojoxo", SetHelper.setOf("ujuxi", "ojuxi"), "ojoxo", Arrays.asList("xojoxo", "ojoxo", "fojoxo", "vojoxo", "nojoxo")),
			new LineEntry("oloxo", SetHelper.setOf("oluxi", "uluxi"), "oloxo", Arrays.asList("xbrodoloxo", "katisoloxo", "riŧoloxo", "fregoloxo", "anpoloxo", "katorigoloxo", "sasoloxo", "gropoloxo", "risoloxo", "xgronsoloxo", "fredoloxo", "barboloxo", "xgronŧoloxo", "skrupoloxo", "skrovoloxo", "kagoloxo", "kokoloxo", "ridikoloxo", "xbrisoloxo", "mirakoloxo", "mokoloxo", "soŧoloxo", "mitikoloxo", "bruskoloxo", "ŧakoloxo", "nuvoloxo", "straŧoloxo", "bagoloxo", "semoloxo", "brufoloxo", "strasoloxo", "perigoloxo", "tuberkoloxo", "katiŧoloxo", "goloxo", "petoloxo", "perikoloxo", "xbrindoloxo", "piatoloxo", "spetakoloxo", "sosoloxo", "prigoloxo", "vergoloxo")),
			new LineEntry("oso", SetHelper.setOf("usi"), "oso", Arrays.asList("kaoroso", "toso", "skabioso", "koaroso", "boso", "bragoso", "ɉoso", "infraroso", "loso", "ƚoso", "moso", "joso", "robegoso", "paƚoso", "petoroso", "pèto–roso", "kodaroso", "biroso", "paloso", "scoso")),
			new LineEntry("omoxo", SetHelper.setOf("omuxi", "umuxi"), "omoxo", Arrays.asList("promoxo")),
			new LineEntry("oño", SetHelper.setOf("iñi"), "oño", Arrays.asList("koño", "moño", "barkoño", "kodoño", "sfroño")),
			new LineEntry("ordo", SetHelper.setOf("urdi"), "ordo", Arrays.asList("ingordo", "tordo", "balordo", "baƚordo", "parabordo", "sordo", "bordo", "xlordo", "ordo")),
			new LineEntry("oxo", SetHelper.setOf("uxi"), "oxo", Arrays.asList("ƚeñoxo", "speƚumoxo", "skisiñoxo", "bordeloxo", "kreoxo", "malaugurioxo", "inŧendoxo", "montoxo", "robegoxo", "koraɉoxo", "piaɉoxo", "spelusoxo", "krestoxo", "astioxo", "maƚenoxo", "flatoxo", "kontrastoxo", "likoxo", "paltanoxo", "xbosegoxo", "spiritoxo", "xmanioxo", "moxo", "peƚoxo", "fanoxo", "oŧioxo", "strasoxo", "malenoxo", "kaloxo", "skorusoxo", "skoruŧoxo", "rekalŧitroxo", "ƚuminoxo", "veseoxo", "lerijoxo", "piovoxo", "deveoxo", "xbrisoxo", "riondoxo", "leñoxo", "xmerdoxo", "koraxoxo", "spreŧilioxo", "scamoxo", "ƚeproxo", "prunoxo", "skejoxo", "velenoxo", "grasioxo", "spongoxo", "meravejoxo", "paludoxo", "begoxo", "lekoxo", "superstisioxo", "despetoxo", "inŧendioxo", "bakanoxo", "virtulioxo", "rivoltoxo", "sekajinoxo", "voƚontaroxo", "sasoxo", "skuarŧoxo", "ventoxo", "xmolsinoxo", "karnoxo", "nebuloxo", "verenoxo", "revoltoxo", "xmorfioxo", "nebioxo", "sekaɉinoxo", "skuarsoxo", "ƚikoxo", "veƚenoxo", "rixegoxo")),
			new LineEntry("edo", SetHelper.setOf("idi"), "edo", Arrays.asList("vènedo", "sínedo", "notevedo", "dedo", "refredo", "gredo", "kontraspedo", "tapedo", "boƚedo", "kredo", "kòspedo", "fredo", "lavedo", "axedo", "tréspedo", "kanedo", "boledo", "xbotedo", "sòsedo", "spedo", "sòŧedo")),
			new LineEntry("or", SetHelper.setOf("uri"), "or", Arrays.asList("kavaor", "strondaor", "proditor", "oldor", "governaor", "voltor", "fondidor", "ƚéor", "apaltaor", "debitor", "trusor", "vivador", "robor", "traitor", "miraor", "skritor", "laoraor", "stuŧegaor", "saxaor", "menor", "suxarior", "provedaor", "spontitor", "luxor", "ŧimor", "ingranditor", "pexor", "penđador", "balotaor", "konplementador", "kalkolaor", "belumor", "nolexaor", "piuxor", "milefior", "batikor", "serkaor", "kalor", "posterior", "gladiator", "bonsior", "kargaor", "retor", "onor", "sferdor", "tarixaor", "lexixlaor", "minusaor", "radiotraxmetitor", "operaor", "bañafior", "sprendor", "ŧiterior", "ŧapaor", "pekator", "peskaor", "spasaor", "konfortaor", "anterior", "interior", "ŧexor", "iƚuminaor", "camor", "burataor", "ventilaor", "malfator", "sagomaor", "vivaor", "kantor", "setaor", "petaor", "stronđaor", "pistor", "tremor", "vendidor", "kalkoƚador", "koniaor", "trivelaor", "braŧaor", "inbasador", "kreaor", "maxor")),
			new LineEntry("ofioxo", SetHelper.setOf("ufiuxi", "ofiuxi"), "ofioxo", Arrays.asList("skarmofioxo")),
			new LineEntry("eso", SetHelper.setOf("isi"), "eso", Arrays.asList("isteso", "senpieso", "tateso", "xmorbeso", "boɉeso", "kareso", "steso", "ñoñoleso", "kageso", "kokoƚeso", "boreso", "goƚeso", "munegeso", "bibieso", "ribreso", "morbieso", "dotoreso", "emeso", "permeso", "buƚeso", "strioneso", "lekeso", "gongoƚeso", "puteƚeso", "paƚeso", "dexmestegeso", "viƚaneso", "bojeso", "piageso", "xentilomeneso", "piajeso", "xgibieso", "skarmofieso", "gengeso", "petegoleso", "sigaƚeso", "ƚadroneso", "kagoleso", "konsaƚaveso", "frameso", "strafareso", "baroneso", "traxmeso", "skivoƚeso", "cakoƚeso", "xmorfieso", "pitokeso", "stomegeso", "bordeleso", "sproteso", "ƚanpeso", "kavaƚeso", "neso", "poltroneso", "muƚeso", "bufoneso", "piavoleso", "xbrikeso", "piaɉeso", "skamufieso", "pakañeso", "mañoleso", "goleso", "menareso", "rabioxeso", "gexeso", "stufeso", "trameso", "fioƚeso", "meƚeso", "sotomeso", "piatoƚeso", "moroxeso", "skamofieso", "fiseso", "rufianeso", "bagoƚeso", "kaveso", "ñoñoƚeso")),
			new LineEntry("oko", SetHelper.setOf("uki"), "oko", Arrays.asList("xboko", "reŧíproko", "poko", "scapaŧoko", "ŧoko", "scapasoko", "scapa–soko", "pàroko", "toko", "scapa–ŧoko", "ñoko", "resíproko")),
			new LineEntry("olioxo", SetHelper.setOf("oliuxi", "uliuxi"), "olioxo", Arrays.asList("olioxo")),
			new LineEntry("oƚo", SetHelper.setOf("uƚi"), "oƚo", Arrays.asList("trémoƚo", "xíroƚo", "pesoƚo", "rasaroƚo", "biavaroƚo", "garétoƚo", "grúmoƚo", "vèrgoƚo", "verloƚo", "bòdoƚo", "gataroƚo", "pípoƚo", "saldaroƚo", "narúnkoƚo", "revendígoƚo", "barkaroƚo", "nèspoƚo", "kapítoƚo", "bixékoƚo", "nisoƚo", "xgriñapòpoƚo", "ranabòtoƚo", "bexaroƚo", "skrimiàtoƚo", "kòtoƚo", "riàoƚo", "tubèrkoƚo", "móskoƚo", "franségoƚo", "scoparoƚo", "bròkoƚo", "ridòtoƚo", "ƚónboƚo", "garúxoƚo", "primaroƚo", "xbríndoƚo", "bígoƚo", "miñòñoƚo", "róndoƚo", "resetàkoƚo", "bugaroƚo", "strupiaskóvoƚo", "orxoƚo", "bàtoƚo", "fusténgoƚo", "brónboƚo", "spondoƚo", "pékoƚo", "búfoƚo", "réfoƚo", "bixígoƚo", "brànkoƚo", "poƚo", "nónsoƚo", "trataroƚo", "basaroƚo", "sàpoƚo", "físoƚo", "sotokòtoƚo", "dóngoƚo", "símoƚo", "skapusioƚo", "bronbeɉoƚo", "ƚaxaroƚo", "kortígoƚo", "múskoƚo", "stisaroƚo", "jéskoƚo", "kivoƚo", "kodarósoƚo", "orbégoƚo", "ƚibèrkoƚo", "pómoƚo", "rosiñoƚo", "apòstoƚo", "bronbijoƚo", "àmoƚo", "ucaroƚo")),
			new LineEntry("eŧo", SetHelper.setOf("iŧi"), "eŧo", Arrays.asList("sporkeŧo", "konŧalaveŧo", "stolideŧo", "kaveŧo", "ladroneŧo", "laveŧo", "fioleŧo", "goleŧo", "kavaleŧo", "fureŧo", "pakañeŧo", "mateŧo", "gongoleŧo", "bulegeŧo", "puteleŧo", "inbriageŧo", "diavoleŧo", "putaneŧo", "buleŧo", "đentilomeneŧo", "demonieŧo", "ñeñeŧo", "fifeŧo", "balbeŧo", "morbineŧo", "rabieŧo", "aveŧo", "tatareŧo", "strolegeŧo", "vermineŧo", "kokoleŧo", "sonoleŧo", "bandiereŧo", "xñanfeŧo", "beŧo", "vilaneŧo", "spiriteŧo", "cakoleŧo", "xbugeŧo", "cacareŧo", "skivoleŧo", "lanpeŧo", "cetineŧo", "karoñeŧo", "burleŧo", "strigeŧo", "muleŧo", "peteŧo", "piatoleŧo", "kokoneŧo", "vermeneŧo", "xmorbeŧo", "fufiñeŧo", "tateŧo", "ñoñoleŧo", "kageŧo", "morbieŧo", "dentilomeneŧo", "piajeŧo", "stranbeŧo", "bagoleŧo", "goloxeŧo", "boɉeŧo", "kareŧo", "senpieŧo", "bibieŧo", "piageŧo", "boreŧo", "munegeŧo", "ribreŧo", "dotoreŧo", "xmorfioxeŧo", "gengeŧo", "lekeŧo", "bojeŧo", "skarmofieŧo", "xgibieŧo", "petegoleŧo")),
			new LineEntry("onioxo", SetHelper.setOf("uniuxi", "oniuxi"), "onioxo", Arrays.asList("maƚenkonioxo", "akrimonioxo", "malankonioxo", "maƚankonioxo", "malenkonioxo")),
			new LineEntry("oƚoxo", SetHelper.setOf("oƚuxi", "uƚuxi"), "oƚoxo", Arrays.asList("bruskoƚoxo", "xbrodoƚoxo", "anpoƚoxo", "sasoƚoxo", "perigoƚoxo", "xbrindoƚoxo", "sosoƚoxo", "spetakoƚoxo", "vergoƚoxo", "semoƚoxo", "risoƚoxo", "katorigoƚoxo", "perikoƚoxo", "prigoƚoxo", "piatoƚoxo", "katisoƚoxo", "petoƚoxo", "xbrisoƚoxo", "strasoƚoxo", "brufoƚoxo", "nuvoƚoxo", "skrupoƚoxo", "sakoƚoxo", "fredoƚoxo", "tuberkoƚoxo", "goƚoxo", "mokoƚoxo", "barboƚoxo", "kokoƚoxo", "ridikoƚoxo", "xgronsoƚoxo", "gropoƚoxo", "fregoƚoxo", "bagoƚoxo", "mirakoƚoxo", "mitikoƚoxo", "skrovoƚoxo", "kagoƚoxo")),
			new LineEntry("oroxo", SetHelper.setOf("oruxi", "uruxi"), "oroxo", Arrays.asList("koroxo", "kaƚoroxo", "saoroxo", "vigoroxo", "valoroxo", "moroxo", "amoroxo", "kaloroxo", "rigoroxo", "vaƚoroxo")),
			new LineEntry("orioxo", SetHelper.setOf("oriuxi", "uriuxi"), "orioxo", Arrays.asList("vitorioxo", "glorioxo")),
			new LineEntry("exe", SetHelper.setOf("ixi"), "exe", Arrays.asList("monŧelexe", "làrexe", "kòdexe", "ƚexe", "seguxinexe", "éndexe", "inglexe", "élexe", "pistorexe", "kararexe", "pòlexe", "àrpexe", "kontramantexe", "màstexe", "borgexe", "lexe", "kòpexe", "rèvexe", "pòƚexe", "konejanexe", "muranexe", "veneŧiexe", "paƚexe", "orèvexe", "kornudexe", "provexe", "đenoexe", "orexe", "cinexe", "èƚexe", "púlexe", "léexe", "turkexe", "kàƚexe", "monseƚexe", "júdexe", "krémexe", "éƚexe", "ŧímexe", "koneɉanexe", "préndexe", "monselexe", "sàlexe", "penexe", "venesiexe", "montebelunexe", "sélexe", "palexe", "poƚexe", "séƚexe", "pistolexe", "fórvexe", "orèdexe", "sàƚexe", "polexe", "ƚàrexe", "danexe", "pistoƚexe", "màntexe", "montebeƚunexe", "paexe", "krèmexe", "ƚéexe", "púƚexe", "pavexe", "príndexe", "índexe", "féƚexe", "kàlexe", "veronexe", "fórfexe", "fórbexe", "đenovexe", "àspexe", "kalŧexe", "àstexe", "èlexe", "đúdexe", "félexe", "kalsexe", "sandonatexe"))
		);
		Assertions.assertEquals(expectedCompactedRules, compactedRules);

		List<String> rules = reducer.convertFormat(flag, false, compactedRules);
		List<String> expectedRules = Arrays.asList(
			"SFX mf Y 55",
			"SFX mf el ili el",
			"SFX mf ol uli ol",
			"SFX mf on uni on",
			"SFX mf en ini en",
			"SFX mf er iri er",
			"SFX mf or uri or",
			"SFX mf exe ixi exe",
			"SFX mf edo idi edo",
			"SFX mf odo udi odo",
			"SFX mf eđo iđi eđo",
			"SFX mf ođo uđi ođo",
			"SFX mf eko iki eko",
			"SFX mf oko uki oko",
			"SFX mf eƚo iƚi eƚo",
			"SFX mf oƚo uƚi oƚo",
			"SFX mf eño iñi eño",
			"SFX mf oño iñi oño",
			"SFX mf eso isi eso",
			"SFX mf oso usi oso",
			"SFX mf eto iti eto",
			"SFX mf oto uti oto",
			"SFX mf eŧo iŧi eŧo",
			"SFX mf oŧo uŧi oŧo",
			"SFX mf evo ivi evo",
			"SFX mf ovo uvi ovo",
			"SFX mf exo ixi exo",
			"SFX mf oxo uxi oxo",
			"SFX mf orse ursi orse",
			"SFX mf ente inti ente",
			"SFX mf onte unti onte",
			"SFX mf oldo uldi oldo",
			"SFX mf ordo urdi ordo",
			"SFX mf orđo urđi orđo",
			"SFX mf olko ulki olko",
			"SFX mf orko urki orko",
			"SFX mf ento inti ento",
			"SFX mf onto unti onto",
			"SFX mf orxo urxi orxo",
			"SFX mf ojoxo ujuxi ojoxo",
			"SFX mf oɉoxo uɉuxi oɉoxo",
			"SFX mf oloxo uluxi oloxo",
			"SFX mf oƚoxo uƚuxi oƚoxo",
			"SFX mf omoxo umuxi omoxo",
			"SFX mf oñoxo uñuxi oñoxo",
			"SFX mf oroxo uruxi oroxo",
			"SFX mf órden úrdini órden",
			"SFX mf obioxo ubiuxi obioxo",
			"SFX mf ofioxo ufiuxi ofioxo",
			"SFX mf olioxo uliuxi olioxo",
			"SFX mf oƚioxo uƚiuxi oƚioxo",
			"SFX mf onioxo uniuxi onioxo",
			"SFX mf orioxo uriuxi orioxo",
			"SFX mf ostoxo ustuxi ostoxo",
			"SFX mf obrioxo ubriuxi obrioxo",
			"SFX mf ordioxo urdiuxi ordioxo"
		);
		Assertions.assertEquals(expectedRules, rules);

		reducer.checkReductionCorrectness(flag, rules, originalRules, originalLines);
	}

	@Test
	public void case19() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"LANG vec",
			"FLAG long",
			"SFX V1 Y 42",
			"SFX V1 a eta oƚa",
			"SFX V1 o eto oƚo",
			"SFX V1 0 eto dor",
			"SFX V1 èr er ièr",
			"SFX V1 oƚa òƚa oƚa",
			"SFX V1 oƚa ioƚa oƚa",
			"SFX V1 oƚa iòƚa oƚa",
			"SFX V1 oƚa ioƚeta oƚa",
			"SFX V1 oƚo òƚo oƚo",
			"SFX V1 oƚo iòƚo oƚo",
			"SFX V1 oƚo ioƚo oƚo",
			"SFX V1 oƚo ioƚeto oƚo",
			"SFX V1 ièr ar ièr",
			"SFX V1 ièr er ièr",
			"SFX V1 ièr areto ièr",
			"SFX V1 ièr ereto ièr",
			"SFX V1 dor or dor",
			"SFX V1 dor tor dor",
			"SFX V1 dor oreto dor",
			"SFX V1 dor toreto dor",
			"SFX V1 èr ar [^i]èr",
			"SFX V1 èr ièr [^cijɉñ]èr",
			"SFX V1 èr ier [^cijɉñ]èr",
			"SFX V1 èr areto [^i]èr",
			"SFX V1 èr ereto [^i]èr",
			"SFX V1 èr iereto [^cijɉñ]èr",
			"SFX V1 a eta dora",
			"SFX V1 ièra ara ièra",
			"SFX V1 ièra era ièra",
			"SFX V1 ièra areta ièra",
			"SFX V1 ièra ereta ièra",
			"SFX V1 dora ora dora",
			"SFX V1 dora tora dora",
			"SFX V1 dora oreta dora",
			"SFX V1 dora toreta dora",
			"SFX V1 èra ara [^i]èra",
			"SFX V1 èra era [^i]èra",
			"SFX V1 èra iera [^cijɉñ]èra",
			"SFX V1 èra ièra [^cijɉñ]èra",
			"SFX V1 èra areta [^i]èra",
			"SFX V1 èra ereta [^i]èra",
			"SFX V1 èra iereta [^cijɉñ]èra"
		);
		Pair<RulesReducer, WordGenerator> pair = createReducer(affFile);
		RulesReducer reducer = pair.getLeft();
		WordGenerator wordGenerator = pair.getRight();
		String flag = "V1";
		List<String> words = Arrays.asList("puƚidor", "duƚador", "ƚatinador", "duƚadora", "ƚatinadora", "puƚidora", "kanseƚèra", "seƚèra", "moƚonèra", "baƚestrèra", "ƚusernèra", "datoƚèra", "noxeƚèra", "ƚansèra", "kaƚegèra", "biƚietèra", "teƚèra", "bigoƚèra", "ocaƚèra", "karoboƚèra", "armiƚèra", "marineƚèra", "meƚiardèra", "fragoƚèra", "baƚonèra", "piñoƚèra", "xojeƚèra", "torkoƚèra", "brasoƚèra", "orioƚèra", "verioƚèra", "kartoƚèra", "miƚiardèra", "busoƚèra", "triveƚinèra", "moƚendinèra", "kriveƚèra", "bruƚèra", "korteƚèra", "ƚavandèra", "bronbeƚèra", "korbeƚèra", "saƚinèra", "puƚexèra", "cokoƚatèra", "kapeƚèra", "staeƚèra", "ƚeutèra", "fritoƚèra", "franboƚèra", "peƚatèra", "risoƚèra", "miƚèra", "visoƚèra", "ƚanèra", "saƚegèra", "skatoƚèra", "baƚansèra", "meƚegèra", "masteƚèra", "mesteƚèra", "xinxoƚèra", "peƚisèra", "subioƚèra", "skaƚetèra", "onbreƚèra", "staƚierèra", "xixoƚèra", "vioƚèra", "kaseƚèra", "maseƚèra", "ƚatonèra", "ventoƚèra", "sesteƚèra", "poƚastrèra", "bokaƚèra", "ƚagrimèra", "xuxoƚèra", "amoƚèra", "sokoƚèra", "ƚavexèra", "ƚautèra", "ƚatèra", "muƚinèra", "ƚibrèra", "oƚanèra", "skudeƚèra", "meƚionèra", "sardeƚèra", "fiƚatogèra", "gaƚinèra", "stadeƚèra", "sendaƚèra", "dudoƚèra", "miƚionèra", "ƚavranèra", "faxoƚèra", "meƚonèra", "staƚiarèra", "xoɉeƚèra", "gaƚèra", "kristaƚèra", "bronboƚèra", "mandoƚèra", "veƚèra", "ƚoamèra", "biƚiardèra", "fiƚandèra", "moƚinèra", "kalsoƚèra", "seƚegèra", "ƚunèra", "ƚuganegèra", "grixoƚèra", "korteƚinèra", "mastroveƚèra", "ƚanpadèra", "xansaƚèra", "ninxoƚèra", "saƚèra", "veƚudèra", "sorboƚèra", "ƚuamèra", "kornoƚèra", "ƚeamèra", "busoƚadèra", "armeƚinèra", "xaƚetèra", "oƚivèra", "kaxeƚèra", "skarpoƚèra", "ƚisèra", "kavaƚèra", "uƚivèra", "nespoƚèra", "paƚudèra", "ƚimoxinièr", "spaƚièr", "fuxiƚièr", "kaƚamièr", "furatoƚièr", "saƚumièr", "staƚièr", "soƚasièr", "taoƚièr", "ƚavorièr", "ofeƚièr", "skoƚièr", "ƚemoxinièr", "ƚabardièr", "kinkaƚièr", "ƚangurièr", "ƚaorièr", "peƚaƚièr", "gondoƚièr", "sesteƚèr", "busoƚèr", "skaƚetèr", "teƚèr", "dudoƚèr", "baƚansèr", "baƚonèr", "saƚegèr", "ƚisèr", "armiƚèr", "ƚuamèr", "muƚinèr", "peƚatèr", "xixoƚèr", "saƚèr", "mesteƚèr", "baƚestrèr", "xansaƚèr", "sokoƚèr", "spuƚexèr", "xuxoƚèr", "nespoƚèr", "ƚeamèr", "veƚèr", "ƚatèr", "bronbeƚèr", "oƚanèr", "visoƚèr", "ƚusernèr", "oƚivèr", "peƚisèr", "grixoƚèr", "kaƚegèr", "biƚietèr", "kaseƚèr", "skudeƚèr", "miƚionèr", "staƚierèr", "sorboƚèr", "moƚendinèr", "biƚiardèr", "torkoƚèr", "busoƚadèr", "fritoƚèr", "onbreƚèr", "sendaƚèr", "noxeƚèr", "vioƚèr", "xinxoƚèr", "gaƚèr", "skarpoƚèr", "skatoƚèr", "masteƚèr", "faxoƚèr", "fragoƚèr", "kriveƚèr", "ƚavandèr", "armeƚinèr", "ocaƚèr", "fiƚatogèr", "karoboƚèr", "mastroveƚèr", "kalsoƚèr", "meƚionèr", "gaƚinèr", "meƚonèr", "piñoƚèr", "kaxeƚèr", "meƚegèr", "kartoƚèr", "moƚinèr", "mandoƚèr", "fiƚandèr", "fogoƚèr", "seƚèr", "brasoƚèr", "ƚanpadèr", "ƚeutèr", "miƚiardèr", "ƚatonèr", "korbeƚèr", "ninxoƚèr", "ƚavexèr", "ƚoamèr", "poƚastrèr", "paƚudèr", "xojeƚèr", "kanseƚèr", "ƚanèr", "risoƚèr", "staƚiarèr", "seƚegèr", "kornoƚèr", "ƚunèr", "meƚiardèr", "cokoƚatèr", "ƚansèr", "miƚèr", "sardeƚèr", "amoƚèr", "ƚautèr", "bokaƚèr", "kristaƚèr", "franboƚèr", "marineƚèr", "saƚinèr", "verioƚèr", "korteƚèr", "staeƚèr", "stadeƚèr", "uƚivèr", "moƚonèr", "bruƚèr", "subioƚèr", "veƚudèr", "kapeƚèr", "bigoƚèr", "orioƚèr", "maseƚèr", "xoɉeƚèr", "ƚuganegèr", "ƚibrèr", "kavaƚèr", "bronboƚèr", "triveƚinèr", "korteƚinèr", "ƚagrimèr", "xaƚetèr", "ventoƚèr", "datoƚèr", "reƚoɉèra", "ƚeñèra", "ƚaroɉèra", "ƚarojèra", "ƚerojèra", "reƚojèra", "ƚaxañèra", "ƚeroɉèra", "barkaroƚa", "mexaroƚa", "viñaroƚa", "fondaroƚa", "ƚaxaroƚa", "coaroƚa", "pasaroƚa", "kasaroƚa", "josaroƚa", "rasaroƚa", "miedaroƚa", "agostaroƚa", "skinkaroƚa", "ƚumaroƚa", "ganbaroƚa", "frutaroƚa", "biavaroƚa", "kagaroƚa", "gataroƚa", "xbacaroƚa", "tastaroƚa", "bexaroƚa", "xontaroƚa", "xornaroƚa", "xluxaroƚa", "pisaroƚa", "koaroƚa", "teƚaroƚa", "sekaroƚa", "pegoraroƚa", "cavaroƚa", "ɉosaroƚa", "spuaroƚa", "pestaroƚa", "saldaroƚa", "kiɉaroƚa", "navaroƚa", "ƚibaroƚa", "bugaroƚa", "cijaroƚa", "spondaroƚa", "xataroƚa", "kontaroƚa", "tremaroƚa", "ƚinaroƚa", "ganxaroƚa", "skaɉaroƚa", "kijaroƚa", "sportaroƚa", "ƚuxaroƚa", "basaroƚa", "panaroƚa", "armaroƚa", "skonbraroƚa", "ponaroƚa", "borsaroƚa", "bandaroƚa", "codaroƚa", "spigaroƚa", "rasparoƚa", "muxaroƚa", "reditaroƚa", "rubaroƚa", "skajaroƚa", "stisaroƚa", "besaroƚa", "piasaroƚa", "ranpegaroƚa", "feraroƚa", "spiansaroƚa", "erbaroƚa", "seraroƚa", "kanaroƚa", "orbaroƚa", "kasoƚa", "tersaroƚa", "kuajaroƚa", "stuaroƚa", "gucaroƚa", "boaroƚa", "skondaroƚa", "ucaroƚa", "pesaroƚa", "bañaroƚa", "beveraroƚa", "sponxaroƚa", "sorxaroƚa", "kanpañaroƚa", "boskaroƚa", "pradaroƚa", "varoƚa", "trexoƚa", "saƚaroƚa", "poƚaroƚa", "primaroƚa", "bokaroƚa", "grataroƚa", "farinaroƚa", "koƚaroƚa", "rigaroƚa", "revendaroƚa", "brasaroƚa", "ranaroƚa", "fiabaroƚa", "stradaroƚa", "kanevaroƚa", "ƚataroƚa", "kamixoƚa", "peñaroƚa", "sestaroƚa", "strasaroƚa", "kaxaroƚa", "semensaroƚa", "inpisaroƚa", "penaroƚa", "skueraroƚa", "sendraroƚa", "akuaroƚa", "kordaroƚa", "ƚisaroƚa", "moskaroƚa", "tetaroƚa", "guxaroƚa", "gondoƚièra", "skoƚièra", "peƚaƚièra", "ƚemoxinièra", "fuxiƚièra", "ofeƚièra", "kinkaƚièra", "staƚièra", "ƚavorièra", "spaƚièra", "furatoƚièra", "taoƚièra", "saƚumièra", "soƚasièra", "ƚaorièra", "ƚangurièra", "kaƚamièra", "ƚabardièra", "ƚimoxinièra", "rasaroƚo", "biavaroƚo", "skodaroƚo", "stentaroƚo", "gataroƚo", "skuderoƚo", "saldaroƚo", "viñaroƚo", "fondaroƚo", "masaroƚo", "kanevaroƚo", "barkaroƚo", "tastaroƚo", "pexaroƚo", "miedaroƚo", "porixoƚo", "xornaroƚo", "cavaroƚo", "pisaroƚo", "bexaroƚo", "ponteroƚo", "artaroƚo", "paroƚo", "frutaroƚo", "pestaroƚo", "scoparoƚo", "xontaroƚo", "tersoƚo", "primaroƚo", "pegoraroƚo", "draparoƚo", "borsaroƚo", "bugaroƚo", "ƚinaroƚo", "teƚaroƚo", "fumaroƚo", "muxaroƚo", "karoƚo", "spondoƚo", "ganxaroƚo", "navaroƚo", "kijaroƚo", "simaroƚo", "skondaroƚo", "cijaroƚo", "viscaroƚo", "xataroƚo", "reditaroƚo", "trataroƚo", "codaroƚo", "bavaroƚo", "basaroƚo", "spigaroƚo", "sportaroƚo", "armaroƚo", "ƚaxaroƚo", "kiɉaroƚo", "rixaroƚo", "stisaroƚo", "panaroƚo", "ƚibaroƚo", "ƚuxaroƚo", "kanaroƚo", "spiansaroƚo", "seraroƚo", "akuaroƚo", "kuartaroƚo", "sendraroƚo", "gucaroƚo", "ucaroƚo", "rubaroƚo", "erbaroƚo", "ƚataroƚo", "ponaroƚo", "bañaroƚo", "sernieroƚo", "skonbraroƚo", "kontaroƚo", "beveraroƚo", "gansaroƚo", "sorxaroƚo", "vovaroƚo", "bevaroƚo", "besaroƚo", "xjosaroƚo", "koresoƚo", "pesaroƚo", "poxoƚo", "piasaroƚo", "solsaroƚo", "semensaroƚo", "skueraroƚo", "boskaroƚo", "revendaroƚo", "pradaroƚo", "guxaroƚo", "brasaroƚo", "farinaroƚo", "ovaroƚo", "pajaroƚo", "merdaroƚo", "grataroƚo", "bokaroƚo", "koƚaroƚo", "poƚaroƚo", "boaroƚo", "sestaroƚo", "xɉosaroƚo", "penaroƚo", "feraroƚo", "supiaroƚo", "agostaroƚo", "kamixoƚo", "ƚumaroƚo", "montixoƚo", "tersaroƚo", "kordaroƚo", "fiabaroƚo", "stradaroƚo", "kuajaroƚo", "ranaroƚo", "pontaroƚo", "peñaroƚo", "paɉaroƚo", "saƚaroƚo", "kanpañaroƚo", "ƚisaroƚo", "coaroƚo", "ranpegaroƚo", "strasaroƚo", "kaxaroƚo", "korparoƚo", "ƚaxañèr", "ƚerojèr", "ƚeñèr", "reƚoɉèr", "reƚojèr", "ƚarojèr", "ƚaroɉèr", "ƚeroɉèr");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.flatMap(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX).stream())
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		List<LineEntry> expectedCompactedRules = Arrays.asList(
			new LineEntry("dor", SetHelper.setOf("oreto", "toreto", "doreto", "tor", "or"), "dor", Arrays.asList("puƚidor", "duƚador", "ƚatinador")),
			new LineEntry("oƚo", SetHelper.setOf("ioƚo", "òƚo", "ioƚeto", "iòƚo", "oƚeto"), "oƚo", Arrays.asList("rasaroƚo", "biavaroƚo", "skodaroƚo", "stentaroƚo", "gataroƚo", "skuderoƚo", "saldaroƚo", "viñaroƚo", "fondaroƚo", "masaroƚo", "kanevaroƚo", "barkaroƚo", "tastaroƚo", "pexaroƚo", "miedaroƚo", "porixoƚo", "xornaroƚo", "cavaroƚo", "pisaroƚo", "bexaroƚo", "ponteroƚo", "artaroƚo", "paroƚo", "frutaroƚo", "pestaroƚo", "scoparoƚo", "xontaroƚo", "tersoƚo", "primaroƚo", "pegoraroƚo", "draparoƚo", "borsaroƚo", "bugaroƚo", "ƚinaroƚo", "teƚaroƚo", "fumaroƚo", "muxaroƚo", "karoƚo", "spondoƚo", "ganxaroƚo", "navaroƚo", "kijaroƚo", "simaroƚo", "skondaroƚo", "cijaroƚo", "viscaroƚo", "xataroƚo", "reditaroƚo", "trataroƚo", "codaroƚo", "bavaroƚo", "basaroƚo", "spigaroƚo", "sportaroƚo", "armaroƚo", "ƚaxaroƚo", "kiɉaroƚo", "rixaroƚo", "stisaroƚo", "panaroƚo", "ƚibaroƚo", "ƚuxaroƚo", "kanaroƚo", "spiansaroƚo", "seraroƚo", "akuaroƚo", "kuartaroƚo", "sendraroƚo", "gucaroƚo", "ucaroƚo", "rubaroƚo", "erbaroƚo", "ƚataroƚo", "ponaroƚo", "bañaroƚo", "sernieroƚo", "skonbraroƚo", "kontaroƚo", "beveraroƚo", "gansaroƚo", "sorxaroƚo", "vovaroƚo", "bevaroƚo", "besaroƚo", "xjosaroƚo", "koresoƚo", "pesaroƚo", "poxoƚo", "piasaroƚo", "solsaroƚo", "semensaroƚo", "skueraroƚo", "boskaroƚo", "revendaroƚo", "pradaroƚo", "guxaroƚo", "brasaroƚo", "farinaroƚo", "ovaroƚo", "pajaroƚo", "merdaroƚo", "grataroƚo", "bokaroƚo", "koƚaroƚo", "poƚaroƚo", "boaroƚo", "sestaroƚo", "xɉosaroƚo", "penaroƚo", "feraroƚo", "supiaroƚo", "agostaroƚo", "kamixoƚo", "ƚumaroƚo", "montixoƚo", "tersaroƚo", "kordaroƚo", "fiabaroƚo", "stradaroƚo", "kuajaroƚo", "ranaroƚo", "pontaroƚo", "peñaroƚo", "paɉaroƚo", "saƚaroƚo", "kanpañaroƚo", "ƚisaroƚo", "coaroƚo", "ranpegaroƚo", "strasaroƚo", "kaxaroƚo", "korparoƚo")),
			new LineEntry("ièr", SetHelper.setOf("areto", "ier", "ereto", "er", "ar"), "ièr", Arrays.asList("ƚimoxinièr", "spaƚièr", "fuxiƚièr", "kaƚamièr", "furatoƚièr", "saƚumièr", "staƚièr", "soƚasièr", "taoƚièr", "ƚavorièr", "ofeƚièr", "skoƚièr", "ƚemoxinièr", "ƚabardièr", "kinkaƚièr", "ƚangurièr", "ƚaorièr", "peƚaƚièr", "gondoƚièr")),
			new LineEntry("dora", SetHelper.setOf("doreta", "ora", "toreta", "oreta", "tora"), "dora", Arrays.asList("duƚadora", "ƚatinadora", "puƚidora")),
			new LineEntry("oƚa", SetHelper.setOf("òƚa", "oƚeta", "iòƚa", "ioƚa", "ioƚeta"), "oƚa", Arrays.asList("barkaroƚa", "mexaroƚa", "viñaroƚa", "fondaroƚa", "ƚaxaroƚa", "coaroƚa", "pasaroƚa", "kasaroƚa", "josaroƚa", "rasaroƚa", "miedaroƚa", "agostaroƚa", "skinkaroƚa", "ƚumaroƚa", "ganbaroƚa", "frutaroƚa", "biavaroƚa", "kagaroƚa", "gataroƚa", "xbacaroƚa", "tastaroƚa", "bexaroƚa", "xontaroƚa", "xornaroƚa", "xluxaroƚa", "pisaroƚa", "koaroƚa", "teƚaroƚa", "sekaroƚa", "pegoraroƚa", "cavaroƚa", "ɉosaroƚa", "spuaroƚa", "pestaroƚa", "saldaroƚa", "kiɉaroƚa", "navaroƚa", "ƚibaroƚa", "bugaroƚa", "cijaroƚa", "spondaroƚa", "xataroƚa", "kontaroƚa", "tremaroƚa", "ƚinaroƚa", "ganxaroƚa", "skaɉaroƚa", "kijaroƚa", "sportaroƚa", "ƚuxaroƚa", "basaroƚa", "panaroƚa", "armaroƚa", "skonbraroƚa", "ponaroƚa", "borsaroƚa", "bandaroƚa", "codaroƚa", "spigaroƚa", "rasparoƚa", "muxaroƚa", "reditaroƚa", "rubaroƚa", "skajaroƚa", "stisaroƚa", "besaroƚa", "piasaroƚa", "ranpegaroƚa", "feraroƚa", "spiansaroƚa", "erbaroƚa", "seraroƚa", "kanaroƚa", "orbaroƚa", "kasoƚa", "tersaroƚa", "kuajaroƚa", "stuaroƚa", "gucaroƚa", "boaroƚa", "skondaroƚa", "ucaroƚa", "pesaroƚa", "bañaroƚa", "beveraroƚa", "sponxaroƚa", "sorxaroƚa", "kanpañaroƚa", "boskaroƚa", "pradaroƚa", "varoƚa", "trexoƚa", "saƚaroƚa", "poƚaroƚa", "primaroƚa", "bokaroƚa", "grataroƚa", "farinaroƚa", "koƚaroƚa", "rigaroƚa", "revendaroƚa", "brasaroƚa", "ranaroƚa", "fiabaroƚa", "stradaroƚa", "kanevaroƚa", "ƚataroƚa", "kamixoƚa", "peñaroƚa", "sestaroƚa", "strasaroƚa", "kaxaroƚa", "semensaroƚa", "inpisaroƚa", "penaroƚa", "skueraroƚa", "sendraroƚa", "akuaroƚa", "kordaroƚa", "ƚisaroƚa", "moskaroƚa", "tetaroƚa", "guxaroƚa")),
			new LineEntry("ièra", SetHelper.setOf("ara", "ereta", "era", "areta"), "ièra", Arrays.asList("gondoƚièra", "skoƚièra", "peƚaƚièra", "ƚemoxinièra", "fuxiƚièra", "ofeƚièra", "kinkaƚièra", "staƚièra", "ƚavorièra", "spaƚièra", "furatoƚièra", "taoƚièra", "saƚumièra", "soƚasièra", "ƚaorièra", "ƚangurièra", "kaƚamièra", "ƚabardièra", "ƚimoxinièra")),
			new LineEntry("èr", SetHelper.setOf("areto", "ereto", "ar"), "[jɉñ]èr", Arrays.asList("ƚaxañèr", "ƚerojèr", "ƚeñèr", "reƚoɉèr", "reƚojèr", "ƚarojèr", "ƚaroɉèr", "ƚeroɉèr")),
			new LineEntry("èr", SetHelper.setOf("ier", "areto", "ereto", "ièr", "ar", "iereto"), "[dgƚmnrstvx]èr", Arrays.asList("sesteƚèr", "busoƚèr", "skaƚetèr", "teƚèr", "dudoƚèr", "baƚansèr", "baƚonèr", "saƚegèr", "ƚisèr", "armiƚèr", "ƚuamèr", "muƚinèr", "peƚatèr", "xixoƚèr", "saƚèr", "mesteƚèr", "baƚestrèr", "xansaƚèr", "sokoƚèr", "spuƚexèr", "xuxoƚèr", "nespoƚèr", "ƚeamèr", "veƚèr", "ƚatèr", "bronbeƚèr", "oƚanèr", "visoƚèr", "ƚusernèr", "oƚivèr", "peƚisèr", "grixoƚèr", "kaƚegèr", "biƚietèr", "kaseƚèr", "skudeƚèr", "miƚionèr", "staƚierèr", "sorboƚèr", "moƚendinèr", "biƚiardèr", "torkoƚèr", "busoƚadèr", "fritoƚèr", "onbreƚèr", "sendaƚèr", "noxeƚèr", "vioƚèr", "xinxoƚèr", "gaƚèr", "skarpoƚèr", "skatoƚèr", "masteƚèr", "faxoƚèr", "fragoƚèr", "kriveƚèr", "ƚavandèr", "armeƚinèr", "ocaƚèr", "fiƚatogèr", "karoboƚèr", "mastroveƚèr", "kalsoƚèr", "meƚionèr", "gaƚinèr", "meƚonèr", "piñoƚèr", "kaxeƚèr", "meƚegèr", "kartoƚèr", "moƚinèr", "mandoƚèr", "fiƚandèr", "fogoƚèr", "seƚèr", "brasoƚèr", "ƚanpadèr", "ƚeutèr", "miƚiardèr", "ƚatonèr", "korbeƚèr", "ninxoƚèr", "ƚavexèr", "ƚoamèr", "poƚastrèr", "paƚudèr", "xojeƚèr", "kanseƚèr", "ƚanèr", "risoƚèr", "staƚiarèr", "seƚegèr", "kornoƚèr", "ƚunèr", "meƚiardèr", "cokoƚatèr", "ƚansèr", "miƚèr", "sardeƚèr", "amoƚèr", "ƚautèr", "bokaƚèr", "kristaƚèr", "franboƚèr", "marineƚèr", "saƚinèr", "verioƚèr", "korteƚèr", "staeƚèr", "stadeƚèr", "uƚivèr", "moƚonèr", "bruƚèr", "subioƚèr", "veƚudèr", "kapeƚèr", "bigoƚèr", "orioƚèr", "maseƚèr", "xoɉeƚèr", "ƚuganegèr", "ƚibrèr", "kavaƚèr", "bronboƚèr", "triveƚinèr", "korteƚinèr", "ƚagrimèr", "xaƚetèr", "ventoƚèr", "datoƚèr")),
			new LineEntry("èra", SetHelper.setOf("ara", "ereta", "iera", "areta", "era", "iereta", "ièra"), "[^ijɉñ]èra", Arrays.asList("kanseƚèra", "seƚèra", "moƚonèra", "baƚestrèra", "ƚusernèra", "datoƚèra", "noxeƚèra", "ƚansèra", "kaƚegèra", "biƚietèra", "teƚèra", "bigoƚèra", "ocaƚèra", "karoboƚèra", "armiƚèra", "marineƚèra", "meƚiardèra", "fragoƚèra", "baƚonèra", "piñoƚèra", "xojeƚèra", "torkoƚèra", "brasoƚèra", "orioƚèra", "verioƚèra", "kartoƚèra", "miƚiardèra", "busoƚèra", "triveƚinèra", "moƚendinèra", "kriveƚèra", "bruƚèra", "korteƚèra", "ƚavandèra", "bronbeƚèra", "korbeƚèra", "saƚinèra", "puƚexèra", "cokoƚatèra", "kapeƚèra", "staeƚèra", "ƚeutèra", "fritoƚèra", "franboƚèra", "peƚatèra", "risoƚèra", "miƚèra", "visoƚèra", "ƚanèra", "saƚegèra", "skatoƚèra", "baƚansèra", "meƚegèra", "masteƚèra", "mesteƚèra", "xinxoƚèra", "peƚisèra", "subioƚèra", "skaƚetèra", "onbreƚèra", "staƚierèra", "xixoƚèra", "vioƚèra", "kaseƚèra", "maseƚèra", "ƚatonèra", "ventoƚèra", "sesteƚèra", "poƚastrèra", "bokaƚèra", "ƚagrimèra", "xuxoƚèra", "amoƚèra", "sokoƚèra", "ƚavexèra", "ƚautèra", "ƚatèra", "muƚinèra", "ƚibrèra", "oƚanèra", "skudeƚèra", "meƚionèra", "sardeƚèra", "fiƚatogèra", "gaƚinèra", "stadeƚèra", "sendaƚèra", "dudoƚèra", "miƚionèra", "ƚavranèra", "faxoƚèra", "meƚonèra", "staƚiarèra", "xoɉeƚèra", "gaƚèra", "kristaƚèra", "bronboƚèra", "mandoƚèra", "veƚèra", "ƚoamèra", "biƚiardèra", "fiƚandèra", "moƚinèra", "kalsoƚèra", "seƚegèra", "ƚunèra", "ƚuganegèra", "grixoƚèra", "korteƚinèra", "mastroveƚèra", "ƚanpadèra", "xansaƚèra", "ninxoƚèra", "saƚèra", "veƚudèra", "sorboƚèra", "ƚuamèra", "kornoƚèra", "ƚeamèra", "busoƚadèra", "armeƚinèra", "xaƚetèra", "oƚivèra", "kaxeƚèra", "skarpoƚèra", "ƚisèra", "kavaƚèra", "uƚivèra", "nespoƚèra", "paƚudèra")),
			new LineEntry("èra", SetHelper.setOf("ara", "ereta", "areta", "era"), "[jɉñ]èra", Arrays.asList("reƚoɉèra", "ƚeñèra", "ƚaroɉèra", "ƚarojèra", "ƚerojèra", "reƚojèra", "ƚaxañèra", "ƚeroɉèra"))
		);
		Assertions.assertEquals(expectedCompactedRules, compactedRules);

		List<String> rules = reducer.convertFormat(flag, false, compactedRules);
		List<String> expectedRules = Arrays.asList(
			"SFX V1 Y 42",
			"SFX V1 a eta oƚa",
			"SFX V1 o eto oƚo",
			"SFX V1 0 eto dor",
			"SFX V1 èr er ièr",
			"SFX V1 oƚa òƚa oƚa",
			"SFX V1 oƚa ioƚa oƚa",
			"SFX V1 oƚa iòƚa oƚa",
			"SFX V1 oƚa ioƚeta oƚa",
			"SFX V1 oƚo òƚo oƚo",
			"SFX V1 oƚo ioƚo oƚo",
			"SFX V1 oƚo iòƚo oƚo",
			"SFX V1 oƚo ioƚeto oƚo",
			"SFX V1 ièr ar ièr",
			"SFX V1 ièr er ièr",
			"SFX V1 ièr areto ièr",
			"SFX V1 ièr ereto ièr",
			"SFX V1 dor or dor",
			"SFX V1 dor tor dor",
			"SFX V1 dor oreto dor",
			"SFX V1 dor toreto dor",
			"SFX V1 èr ar [dgjɉƚmnñrstvx]èr",
			"SFX V1 èr ier [dgƚmnrstvx]èr",
			"SFX V1 èr ièr [dgƚmnrstvx]èr",
			"SFX V1 èr areto [dgjɉƚmnñrstvx]èr",
			"SFX V1 èr ereto [dgjɉƚmnñrstvx]èr",
			"SFX V1 èr iereto [dgƚmnrstvx]èr",
			"SFX V1 a eta dora",
			"SFX V1 ièra ara ièra",
			"SFX V1 ièra era ièra",
			"SFX V1 ièra areta ièra",
			"SFX V1 ièra ereta ièra",
			"SFX V1 dora ora dora",
			"SFX V1 dora tora dora",
			"SFX V1 dora oreta dora",
			"SFX V1 dora toreta dora",
			"SFX V1 èra ara [^i]èra",
			"SFX V1 èra era [^i]èra",
			"SFX V1 èra iera [^ijɉñ]èra",
			"SFX V1 èra ièra [^ijɉñ]èra",
			"SFX V1 èra areta [^i]èra",
			"SFX V1 èra ereta [^i]èra",
			"SFX V1 èra iereta [^ijɉñ]èra"
		);
		Assertions.assertEquals(expectedRules, rules);

		reducer.checkReductionCorrectness(flag, rules, originalRules, originalLines);
	}

	@Test
	public void case20() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"LANG vec",
			"FLAG long",
			"SFX V0 Y 42",
			"SFX V0 èr ar [^i]èr",
			"SFX V0 èr ereto [^i]èr",
			"SFX V0 èr areto [^i]èr",
			"SFX V0 èra era [^i]èra",
			"SFX V0 èra ara [^i]èra",
			"SFX V0 èra ereta [^i]èra",
			"SFX V0 èra areta [^i]èra",
			"SFX V0 ièr er ièr",
			"SFX V0 ièr ar ièr",
			"SFX V0 ièr ereto ièr",
			"SFX V0 ièr areto ièr",
			"SFX V0 ièra era ièra",
			"SFX V0 ièra ara ièra",
			"SFX V0 ièra ereta ièra",
			"SFX V0 ièra areta ièra",
			"SFX V0 èr er ièr",
			"SFX V0 èr ier [^aeoucijɉñ]èr",
			"SFX V0 èr iereto [^aeoucijɉñ]èr",
			"SFX V0 èra iera [^aeoucijɉñ]èra",
			"SFX V0 èra iereta [^aeoucijɉñ]èra",
			"SFX V0 èr ièr [^aeoucijɉñ]èr",
			"SFX V0 èra ièra [^aeoucijɉñ]èra",
			"SFX V0 dor tor [aei]dor",
			"SFX V0 dor toreto [aei]dor",
			"SFX V0 0 eto [aei]dor",
			"SFX V0 dor or [aei]dor",
			"SFX V0 dor oreto [aei]dor",
			"SFX V0 dora tora [aei]dora",
			"SFX V0 a eta [aei]dora",
			"SFX V0 dora toreta [aei]dora",
			"SFX V0 dora ora [aei]dora",
			"SFX V0 dora oreta [aei]dora",
			"SFX V0 ol òl [dđrsŧx]ol",
			"SFX V0 0 eto [dđrsŧx]ol",
			"SFX V0 ol iol [dđrsŧx]ol",
			"SFX V0 ol ioleto [dđrsŧx]ol",
			"SFX V0 ol iòl [dđrsŧx]ol",
			"SFX V0 ola òla [dđrsŧx]ola",
			"SFX V0 a eta [dđrsŧx]ola",
			"SFX V0 ola iola [dđrsŧx]ola",
			"SFX V0 ola ioleta [dđrsŧx]ola",
			"SFX V0 ola iòla [dđrsŧx]ola"
		);
		Pair<RulesReducer, WordGenerator> pair = createReducer(affFile);
		RulesReducer reducer = pair.getLeft();
		WordGenerator wordGenerator = pair.getRight();
		String flag = "V0";
		List<String> words = Arrays.asList("fresèr", "melegèr", "kanpèr", "masèr", "stropèr", "brulèr", "verdasèr", "verdaŧèr", "bonbaxèr", "baretèr", "kalderèr", "maraskèr", "fogèr", "jandèr", "papuŧèr", "bronbolèr", "spolverinèr", "piñokèr", "tabakèr", "ŧerèr", "ŧedrèr", "maŧèr", "bronđèr", "viperèr", "xanŧalèr", "festèr", "danŧalèr", "perèr", "saonèr", "fasinèr", "sperlusèr", "ulivèr", "balansèr", "subiolèr", "bugansèr", "skatolèr", "buganŧèr", "skorŧèr", "kutèr", "kapelèr", "kapuŧèr", "luamèr", "kalmonèr", "xinxolèr", "remesèr", "skarpèr", "kalegèr", "piñolèr", "sensèr", "brasolèr", "piatèr", "papusèr", "fasèr", "ortigèr", "veriolèr", "skagaŧèr", "fornèr", "mestelèr", "samitèr", "skorsèr", "latèr", "skagasèr", "ŧukèr", "kartèr", "tornèr", "ŧeriexèr", "tarmèr", "pontonèr", "scaxèr", "stiorèr", "solèr", "visolèr", "spulexèr", "riŧolèr", "sagèr", "ŧelegèr", "xuxolèr", "anŧopresèr", "montanèr", "fraskèr", "leamèr", "fontegèr", "sanbugèr", "mastrovelèr", "sorbolèr", "sperluŧèr", "pastèr", "konfuxionèr", "karegèr", "ŧankanèr", "marinèr", "stramaŧèr", "lavexèr", "bixèr", "dudolèr", "remèr", "botonèr", "đuđolèr", "veludèr", "ansipresèr", "kaxelèr", "stramasèr", "kalŧinèr", "ŧestelèr", "ortrigèr", "pasarèr", "selèr", "munèr", "đojelèr", "milèr", "freŧèr", "olmèr", "personèr", "teraŧèr", "fiorèr", "luganegèr", "meliardèr", "terasèr", "karoŧèr", "pomèr", "vakèr", "bigolèr", "botèr", "balestrèr", "maskarèr", "fornaxèr", "onbrelèr", "naransèr", "đensaminèr", "molonèr", "bruskinèr", "kanevèr", "đoɉelèr", "naranŧèr", "datolèr", "krivelèr", "buŧoladèr", "gatèr", "pitèr", "karobèr", "cokolatèr", "ponèr", "kordèr", "guantèr", "kalŧerèr", "skarpolèr", "stalierèr", "kalierèr", "đaletèr", "storèr", "fagèr", "vivèr", "spadèr", "roerèr", "skudelèr", "karetèr", "torkolèr", "lunèr", "salmistrèr", "fenestrèr", "sforŧanèr", "dindolèr", "finestrèr", "karosèr", "ŧokolèr", "karèr", "ostregèr", "fragèr", "onarèr", "kalsetèr", "marŧèr", "xavaskèr", "portenèr", "damaskèr", "ganŧèr", "marinelèr", "ŧarèr", "marsèr", "tamixèr", "sparaxèr", "ganbarèr", "sansèr", "fiaskèr", "librèr", "skoaŧèr", "orèr", "grixolèr", "skoasèr", "ŧuketèr", "sabionèr", "gansèr", "codèr", "ŧatarèr", "malgaritèr", "braŧolèr", "molendinèr", "morèr", "lagrimèr", "fragolèr", "bandèr", "balanŧèr", "verèr", "filandèr", "ostèr", "ŧirexèr", "roverèr", "lansèr", "kalŧetèr", "moskèr", "oxarèr", "milionèr", "soaxèr", "lusernèr", "bixutèr", "sogèr", "roxèr", "sforsanèr", "ŧenturèr", "voltèr", "spulxèr", "mulinèr", "pionbèr", "lavranèr", "ŧestèr", "noxelèr", "sardelèr", "oltrigèr", "tinasèr", "latonèr", "tinaŧèr", "vespèr", "lanèr", "kalserèr", "stadelèr", "fuxèr", "galinèr", "otonèr", "ŧatèr", "sparexèr", "kapotèr", "stuvèr", "paludèr", "kodèr", "lanŧèr", "karobolèr", "peltrèr", "kristalèr", "franbolèr", "suxinèr", "lanpadèr", "puinèr", "straŧèr", "trivelinèr", "vrespèr", "strasèr", "figèr", "felsèr", "kartolèr", "velèr", "armelinèr", "anŧipresèr", "felŧèr", "olivèr", "salinèr", "koramèr", "antanèr", "xansalèr", "kalŧolèr", "kalsinèr", "grasinèr", "armèr", "salèr", "bastionèr", "xixibèr", "porkèr", "kanèr", "leutèr", "skaletèr", "risolèr", "biskotèr", "musatèr", "kornolèr", "lavandèr", "polastrèr", "pinèr", "bekèr", "bronbexèr", "bronxèr", "kaselèr", "ŧarexèr", "maskerèr", "ferèr", "onèr", "ranèr", "kaldarèr", "salegèr", "fritolèr", "laveđèr", "monèr", "fiokèr", "pelatèr", "saltèr", "xixolèr", "điđolèr", "đenŧaminèr", "pastiŧèr", "seradurèr", "margaritèr", "kavrèr", "gomarèr", "mortèr", "xatèr", "frutèr", "tesèr", "armilèr", "melionèr", "ocalèr", "maxèr", "pestèr", "persegèr", "kortelinèr", "granèr", "spinèr", "ŧavatèr", "pastisèr", "stopèr", "busoladèr", "ninxolèr", "pitarèr", "amolèr", "botegèr", "manganèr", "ŧentenèr", "varotèr", "ŧendalèr", "didolèr", "melonèr", "naspersegèr", "braxèr", "orarèr", "maronèr", "portinèr", "tripèr", "galèr", "kantèr", "mastelèr", "korbelèr", "ŧariexèr", "violèr", "kakèr", "ŧierexèr", "denŧaminèr", "kuxinèr", "pestrinèr", "fiubèr", "kortelèr", "anemèr", "kanselèr", "bronbèr", "sanguetèr", "koronèr", "kalsolèr", "loamèr", "bokalèr", "scexèr", "petenèr", "arminèr", "diamantèr", "ŧimoxèr", "maselèr", "birèr", "patinèr", "nespolèr", "brespèr", "pegorèr", "piñatèr", "salgèr", "perlèr", "oriolèr", "buŧolèr", "olanèr", "kanpanèr", "uxurèr", "staelèr", "telèr", "viŧolèr", "ŧerexèr", "paltumèr", "morsèr", "lisèr", "filatogèr", "peliŧèr", "lautèr", "ansopresèr", "bierèr", "akuavitèr", "bronbelèr", "nogèr", "bolsèr", "palmèr", "fontanèr", "bilietèr", "kaponèr", "đanŧalèr", "bolŧèr", "ruxèr", "musèr", "piegorèr", "brondèr", "pasamanèr", "kuramèr", "peatèr", "bastèr", "perukèr", "semenŧèr", "liŧèr", "sperlongèr", "ventolèr", "kavalèr", "frasenèr", "limonèr", "semensèr", "tavernèr", "gotèr", "tomèr", "faxolèr", "karbonèr", "senavèr", "staliarèr", "scopetèr", "merŧèr", "lavedèr", "fogolèr", "murèr", "mersèr", "karatèr", "molinèr", "ŧinbanèr", "biliardèr", "busolèr", "mandolèr", "balonèr", "kaldierèr", "miliardèr", "kaxèr", "pelisèr", "đinđolèr", "spañèr", "leñèr", "marcèr", "nocèr", "sojèr", "specèr", "kaveɉèr", "formaɉèr", "inđeñèr", "kavejèr", "leroɉèr", "muscèr", "ŧerfoɉèr", "laxañèr", "burcèr", "argañèr", "ŧarfoɉèr", "preèr", "stuèr", "beroèr", "meajèr", "kodoñèr", "soɉèr", "ŧercèr", "boèr", "pajèr", "relojèr", "ŧerfojèr", "indeñèr", "inxeñèr", "fabricèr", "laroɉèr", "lerojèr", "bruñèr", "majèr", "reloɉèr", "stañèr", "kastañèr", "coèr", "montañèr", "kinkajèr", "paɉèr", "kocèr", "mascèr", "larojèr", "malgarañèr", "magrañèr", "meaɉèr", "ŧarfojèr", "kinkaɉèr", "đoèr", "formajèr", "bancèr", "karñèr", "piñèr", "koverxidor", "pendador", "fondidor", "torxidor", "odidor", "penxador", "udidor", "anbasiador", "skodidor", "karador", "stendidor", "penđador", "konplementador", "strusador", "latinador", "avogador", "koɉidor", "venŧador", "vensador", "destamedador", "oponidor", "tordidor", "revedador", "sasadador", "konponidor", "pulidor", "konpoxidor", "torđidor", "inperador", "mapador", "koverđidor", "frotador", "koridor", "torsidor", "koñidor", "anbasador", "đenador", "destamexador", "kontrador", "servidor", "vendedor", "spendidor", "vendidor", "destameđador", "inbasador", "spontidor", "perkurador", "batidor", "kojidor", "bevador", "dulador", "torŧidor", "minador", "modifegador", "koverdidor", "bevidor", "teñidor", "spendidora", "koverdidora", "pendadora", "đenadora", "stendidora", "karadora", "destamedadora", "anbasadora", "skodidora", "frotadora", "modifegadora", "konponidora", "vendidora", "vensadora", "koɉidora", "batidora", "torsidora", "penxadora", "mapadora", "duladora", "minadora", "odidora", "venŧadora", "pulidora", "bevadora", "anbasiadora", "konplementadora", "latinadora", "oponidora", "destameđadora", "destamexadora", "torŧidora", "tordidora", "kontradora", "kojidora", "inbasadora", "penđadora", "perkuradora", "teñidora", "koverđidora", "konpoxidora", "koñidora", "servidora", "udidora", "koridora", "torxidora", "revedadora", "vendedora", "sasadadora", "bevidora", "fondidora", "inperadora", "strusadora", "torđidora", "koverxidora", "avogadora", "xuxolèra", "lunèra", "otonèra", "dindolèra", "ŧelegèra", "filatogèra", "ŧarèra", "ventolèra", "leutèra", "kanèra", "paltumèra", "frutèra", "piñokèra", "melegèra", "marsèra", "sforŧanèra", "sensèra", "ŧatarèra", "lansèra", "uxurèra", "tabakèra", "bruskinèra", "kanevèra", "sparexèra", "ŧestèra", "mersèra", "ŧedrèra", "perlèra", "mastrovelèra", "fiorèra", "galèra", "skaletèra", "trivelinèra", "sabionèra", "oriolèra", "grixolèra", "morèra", "salmistrèra", "bronbolèra", "fresèra", "ŧerexèra", "busolèra", "ganbarèra", "bugansèra", "karbonèra", "pasarèra", "botegèra", "bierèra", "kanpèra", "seradurèra", "selèra", "botèra", "pitèra", "onèra", "lisèra", "ranèra", "lautèra", "skarpolèra", "fasinèra", "leamèra", "meliardèra", "pipinèra", "margaritèra", "brasolèra", "roerèra", "sansèra", "kroxèra", "guantèra", "telèra", "orarèra", "bolŧèra", "mestelèra", "tamixèra", "xixibèra", "kornolèra", "puinèra", "fiaskèra", "kapotèra", "tavernèra", "kartolèra", "paludèra", "palmèra", "pasamanèra", "braŧolèra", "balansèra", "terasèra", "skarpèra", "masèra", "stiorèra", "ŧatèra", "kalierèra", "xatèra", "oxarèra", "violèra", "rixèra", "scaxèra", "piatèra", "maskerèra", "đanŧalèra", "busoladèra", "balonèra", "bastionèra", "papuŧèra", "gansèra", "kaldarèra", "malgaritèra", "ortrigèra", "porkèra", "lanèra", "maselèra", "pulexèra", "fiubèra", "scexèra", "birèra", "pestrinèra", "ponèra", "fagèra", "ansipresèra", "stuvèra", "spinèra", "storèra", "vrespèra", "bandèra", "karobèra", "veludèra", "sogèra", "sperlusèra", "kortelinèra", "piñatèra", "kavrèra", "staelèra", "skatolèra", "brespèra", "armèra", "botonèra", "risolèra", "portenèra", "semenŧèra", "verdasèra", "milionèra", "ŧestelèra", "perukèra", "ŧeriexèra", "karoŧèra", "vespèra", "vivèra", "kortelèra", "ŧariexèra", "kuxinèra", "fasèra", "pomèra", "ŧinbanèra", "kalegèra", "melionèra", "sorbolèra", "brulèra", "piñolèra", "roxèra", "fraskèra", "peatèra", "fornèra", "ŧendalèra", "marinèra", "koramèra", "karèra", "straŧèra", "murèra", "naranŧèra", "bixutèra", "balanŧèra", "riŧolèra", "lusernèra", "stalierèra", "mastelèra", "remesèra", "bigolèra", "salèra", "fragolèra", "kutèra", "persegèra", "skorŧèra", "buŧoladèra", "skoasèra", "torkolèra", "bolsèra", "verèra", "fritolèra", "kalmonèra", "spolverinèra", "xixolèra", "musèra", "spadèra", "onarèra", "montanèra", "melonèra", "sanbugèra", "karatèra", "loamèra", "ansopresèra", "pestèra", "akuavitèra", "armelinèra", "luganegèra", "spulxèra", "pegorèra", "felŧèra", "molinèra", "ocalèra", "kalŧerèra", "kantèra", "bastèra", "lagrimèra", "orèra", "tripèra", "tesèra", "saonèra", "maŧèra", "kalserèra", "saltèra", "lavranèra", "konfuxionèra", "ŧerèra", "arminèra", "dudolèra", "bronxèra", "bokalèra", "kalŧetèra", "fragèra", "roverèra", "manganèra", "lanŧèra", "skorsèra", "pontonèra", "kapuŧèra", "korbelèra", "perèra", "lavedèra", "finestrèra", "xansalèra", "bronbèra", "kalsolèra", "sanguetèra", "kuramèra", "ŧankanèra", "kapelèra", "viperèra", "fuxèra", "ŧentenèra", "frasenèra", "kristalèra", "nespolèra", "stopèra", "sparaxèra", "diamantèra", "merŧèra", "fornaxèra", "baretèra", "kavalèra", "remèra", "đinđolèra", "biskotèra", "pinèra", "bronbexèra", "karegèra", "pelatèra", "đojelèra", "xavaskèra", "librèra", "brondèra", "strasèra", "kalŧinèra", "fontanèra", "marinelèra", "kaxelèra", "karobolèra", "freŧèra", "kalŧolèra", "nogèra", "naspersegèra", "figèra", "điđolèra", "patinèra", "maxèra", "viŧolèra", "moskèra", "đuđolèra", "skagaŧèra", "filandèra", "damaskèra", "gatèra", "ŧierexèra", "skoaŧèra", "ninxolèra", "stadèra", "franbolèra", "stramasèra", "lanpadèra", "fontegèra", "latèra", "soaxèra", "kalderèra", "voltèra", "tomèra", "maronèra", "grasinèra", "kalsinèra", "krivelèra", "marŧèra", "maraskèra", "jaŧèra", "kalsetèra", "monèra", "maskarèra", "onbrelèra", "kordèra", "ŧenturèra", "amolèra", "latonèra", "armilèra", "laveđèra", "piegorèra", "samitèra", "mulinèra", "felsèra", "kaponèra", "danŧalèra", "ŧarexèra", "pitarèra", "portinèra", "ŧukèra", "tarmèra", "đaletèra", "musatèra", "antanèra", "sperlongèra", "petenèra", "kakèra", "karetèra", "kaselèra", "granèra", "pastèra", "jasèra", "xinxolèra", "braxèra", "sperluŧèra", "tinaŧèra", "vakèra", "olivèra", "festèra", "tinasèra", "ulivèra", "faxolèra", "salinèra", "sforsanèra", "gomarèra", "đenŧaminèra", "polastrèra", "gotèra", "peltrèra", "suxinèra", "stadelèra", "bilietèra", "ŧimoxèra", "ŧirexèra", "semensèra", "cokolatèra", "munèra", "salgèra", "verdaŧèra", "milèra", "morsèra", "visolèra", "velèra", "ŧavatèra", "mandolèra", "stropèra", "ruxèra", "pelisèra", "sagèra", "bixèra", "denŧaminèra", "naransèra", "galinèra", "molonèra", "sorđèra", "karosèra", "sorxèra", "ostregèra", "salegèra", "pastiŧèra", "đensaminèra", "ferèra", "bekèra", "biliardèra", "olmèra", "pastisèra", "buŧolèra", "oltrigèra", "personèra", "sordèra", "noxelèra", "balestrèra", "buganŧèra", "skagasèra", "kanselèra", "peliŧèra", "xanŧalèra", "pionbèra", "datolèra", "molendinèra", "đoɉelèra", "didolèra", "codèra", "bronđèra", "olanèra", "senavèra", "varotèra", "anŧipresèra", "anŧopresèra", "stramaŧèra", "kanpanèra", "ŧuketèra", "kartèra", "koronèra", "fenestrèra", "kaldierèra", "bonbaxèra", "luamèra", "veriolèra", "kaxèra", "teraŧèra", "jandèra", "lavandèra", "subiolèra", "anemèra", "fiokèra", "lavexèra", "staliarèra", "sardelèra", "ostèra", "liŧèra", "skudelèra", "ŧokolèra", "limonèra", "tornèra", "ganŧèra", "bronbelèra", "scopetèra", "miliardèra", "ortigèra", "papusèra", "ternièr", "konsièr", "labardièr", "magadenièr", "seremonièr", "lavorièr", "skolièr", "pupièr", "faŧendièr", "stalièr", "tramesièr", "arsièr", "limoxinièr", "timonièr", "kaxermièr", "konŧièr", "gadetièr", "kredenŧièr", "speŧièr", "prièr", "portièr", "spontièr", "vedrièr", "ardentièr", "provièr", "spediŧionièr", "magađenièr", "daŧièr", "kanŧelièr", "matonièr", "kristièr", "xberovièr", "taolièr", "xbaroièr", "langurièr", "xbravièr", "drapièr", "spesièr", "doanièr", "karnièr", "karserièr", "gropièr", "pirièr", "solasièr", "laorièr", "ŧervièr", "karŧerièr", "dasièr", "virièr", "spiŧièr", "magaxenièr", "ponpièr", "ovrièr", "fuxilièr", "spasidièr", "guardarobièr", "solaŧièr", "klistièr", "đardinièr", "spalièr", "burlièr", "venturièr", "despensièr", "skufièr", "fasendièr", "spasixièr", "ofelièr", "kasièr", "vivandièr", "tapeŧièr", "ŧimièr", "arkibuxièr", "maxagenièr", "gabièr", "falkonièr", "tapesièr", "popièr", "salumièr", "arxentièr", "barbièr", "manifaturièr", "dopièr", "balkonièr", "postièr", "arđentièr", "kredensièr", "fitanŧièr", "biskaŧièr", "brasièr", "fabriŧièr", "skorlièr", "drogièr", "spoletièr", "furatolièr", "gondolièr", "braŧièr", "texorièr", "tramisièr", "kalamièr", "biskasièr", "pelalièr", "fabrisièr", "bankonièr", "lemoxinièr", "fitansièr", "gaxetièr", "pivièr", "dispensièr", "arŧièr", "texaurièr", "xbarovièr", "kinkalièr", "usièr", "angurièr", "kondotièr", "kavièr", "konfeturièr", "xberoièr", "kafetièr", "gađetièr", "spasiđièr", "ŧerimonièr", "pisarol", "skodarol", "sponxol", "coarol", "kanpañarol", "skuderol", "kanevarol", "guxarol", "borsarol", "rubarol", "primarol", "gatarol", "solŧarol", "bokarol", "rixarol", "semensarol", "gratarol", "stiŧarol", "ganŧarol", "ucarol", "spondol", "vovarol", "boarol", "ovarol", "strasarol", "xjoŧarol", "ranarol", "lumarol", "penarol", "ŧimarol", "pođol", "bavarol", "kiɉarol", "sorđarol", "libarol", "spiansarol", "kaxarol", "sorxarol", "beŧarol", "piaŧarol", "skondarol", "saldarol", "kordarol", "beđarol", "tastarol", "spianŧarol", "kamixol", "navarol", "ponterol", "erbarol", "xɉosarol", "linarol", "stentarol", "kanarol", "supiarol", "biavarol", "gansarol", "terŧarol", "fondarol", "stradarol", "ŧestarol", "parol", "liŧarol", "codarol", "ferarol", "gucarol", "sportarol", "spigarol", "sordarol", "braŧarol", "merdarol", "ŧendrarol", "pesarol", "tersol", "terŧol", "bañarol", "rasarol", "kuajarol", "koresol", "porixol", "pegorarol", "viscarol", "ŧerarol", "ladarol", "scoparol", "barkarol", "panarol", "poridol", "brasarol", "kolarol", "cavarol", "laxarol", "revendarol", "besarol", "ganxarol", "masarol", "korparol", "pajarol", "telarol", "straŧarol", "bexarol", "karol", "akuarol", "poxol", "tersarol", "đontarol", "semenŧarol", "cijarol", "pexarol", "stisarol", "đornarol", "ranpegarol", "xɉoŧarol", "ganđarol", "lisarol", "reditarol", "beverarol", "skuerarol", "podol", "salarol", "ponarol", "pestarol", "skonbrarol", "piasarol", "montixol", "basarol", "bevarol", "frutarol", "pradarol", "luxarol", "fiabarol", "kontarol", "pontarol", "farinarol", "agostarol", "draparol", "lađarol", "viñarol", "armarol", "polarol", "ŧernierol", "latarol", "tratarol", "fumarol", "kuartarol", "maŧarol", "miedarol", "bugarol", "peŧarol", "bedarol", "artarol", "boskarol", "poriđol", "gandarol", "paɉarol", "muxarol", "xjosarol", "peñarol", "sponđol", "kijarol", "solsarol", "xatarol", "stañèra", "indeñèra", "preèra", "coèra", "kastañèra", "bancèra", "ŧercèra", "kavejèra", "kinkaɉèra", "majèra", "montañèra", "burcèra", "nocèra", "kinkajèra", "formaɉèra", "paɉèra", "ŧarfojèra", "mascèra", "leroɉèra", "kuaɉèra", "bruñèra", "piñèra", "muscèra", "beroèra", "magrañèra", "boèra", "argañèra", "meaɉèra", "leñèra", "reloɉèra", "marcèra", "specèra", "ŧerfojèra", "stuèra", "sojèra", "ŧerfoɉèra", "kuajèra", "ŧarfoɉèra", "larojèra", "laroɉèra", "lerojèra", "relojèra", "kodoñèra", "inxeñèra", "kocèra", "meajèra", "malgarañèra", "kaveɉèra", "đoèra", "fabricèra", "soɉèra", "laxañèra", "pajèra", "spañèra", "inđeñèra", "formajèra", "peŧarola", "beverarola", "ferarola", "kolarola", "kasola", "spigarola", "đontarola", "sponxarola", "tersarola", "polarola", "inpiŧarola", "lumarola", "fiabarola", "stradarola", "kuajarola", "salarola", "besarola", "armarola", "peñarola", "skaɉarola", "stisarola", "piasarola", "ranpegarola", "laxarola", "kaxarola", "kaŧola", "spiansarola", "orbarola", "stiŧarola", "terŧarola", "luxarola", "ucarola", "stuarola", "spianŧarola", "straŧarola", "bañarola", "erbarola", "sponđarola", "gratarola", "đornarola", "boarola", "skondarola", "braŧarola", "rigarola", "penarola", "cavarola", "pisarola", "kamixola", "bedarola", "lisarola", "moskarola", "tetarola", "ŧestarola", "pestarola", "semenŧarola", "kanpañarola", "ranarola", "primarola", "inpisarola", "strasarola", "pegorarola", "koarola", "skuerarola", "semensarola", "piaŧarola", "međarola", "akuarola", "boskarola", "pradarola", "guxarola", "brasarola", "gucarola", "viñarola", "fondarola", "trexola", "farinarola", "rubarola", "tređola", "mexarola", "varola", "kanevarola", "pasarola", "bokarola", "miedarola", "beŧarola", "skinkarola", "agostarola", "bexarola", "basarola", "frutarola", "kordarola", "ganbarola", "beđarola", "kagarola", "spuarola", "sordarola", "coarola", "ŧekarola", "ɉoŧarola", "ganđarola", "josarola", "bandarola", "kaŧarola", "biavarola", "ɉosarola", "revendarola", "saldarola", "latarola", "telarola", "kasarola", "ganxarola", "barkarola", "sorđarola", "xbacarola", "tastarola", "gandarola", "xatarola", "reditarola", "kontarola", "codarola", "spondarola", "tremarola", "sorxarola", "sportarola", "linarola", "ŧerarola", "kiɉarola", "panarola", "pesarola", "libarola", "kanarola", "borsarola", "medarola", "rasarola", "rasparola", "bugarola", "muxarola", "joŧarola", "gatarola", "skajarola", "frakarola", "navarola", "ŧendrarola", "ponarola", "kijarola", "tredola", "skonbrarola", "cijarola", "karnièra", "kondotièra", "pelalièra", "arkibuxièra", "salumièra", "ofelièra", "fuxilièra", "magaxenièra", "fitansièra", "timonièra", "gabièra", "spoletièra", "xbaroièra", "balkonièra", "bankonièra", "labardièra", "kasièra", "kredenŧièra", "pupièra", "tramisièra", "gaxetièra", "venturièra", "konfeturièra", "drogièra", "skolièra", "karserièra", "spasidièra", "đardinièra", "konŧièra", "laorièra", "despensièra", "kanŧelièra", "solaŧièra", "fabriŧièra", "gađetièra", "arsièra", "gadetièra", "biskasièra", "burlièra", "skorlièra", "xbarovièra", "pirièra", "tapeŧièra", "ovrièra", "xberovièra", "kalamièra", "brasièra", "dasièra", "ternièra", "popièra", "solasièra", "lavorièra", "spasixièra", "lemoxinièra", "dopièra", "ŧervièra", "spontièra", "virièra", "kristièra", "falkonièra", "magađenièra", "portièra", "speŧièra", "stalièra", "kinkalièra", "usièra", "postièra", "prièra", "xbravièra", "karŧerièra", "manifaturièra", "spediŧionièra", "drapièra", "doanièra", "maxagenièra", "konsièra", "kafetièra", "spesièra", "kavièra", "tramesièra", "kredensièra", "ardentièra", "guardarobièra", "skufièra", "braŧièra", "xberoièra", "pivièra", "vedrièra", "furatolièra", "vivandièra", "matonièra", "fasendièra", "arxentièra", "taolièra", "seremonièra", "klistièra", "spiŧièra", "provièra", "limoxinièra", "spalièra", "fabrisièra", "dispensièra", "biskaŧièra", "arŧièra", "ponpièra", "faŧendièra", "tapesièra", "texorièra", "spasiđièra", "gondolièra", "kaxermièra", "daŧièra", "fitanŧièra", "angurièra", "texaurièra", "langurièra", "magadenièra", "arđentièra", "gropièra");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.flatMap(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX).stream())
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		List<LineEntry> expectedCompactedRules = Arrays.asList(
			new LineEntry("dor", SetHelper.setOf("or", "tor", "toreto", "oreto", "doreto"), "dor", Arrays.asList("koverxidor", "pendador", "fondidor", "torxidor", "odidor", "penxador", "udidor", "anbasiador", "skodidor", "karador", "stendidor", "penđador", "konplementador", "strusador", "latinador", "avogador", "koɉidor", "venŧador", "vensador", "destamedador", "oponidor", "tordidor", "revedador", "sasadador", "konponidor", "pulidor", "konpoxidor", "torđidor", "inperador", "mapador", "koverđidor", "frotador", "koridor", "torsidor", "koñidor", "anbasador", "đenador", "destamexador", "kontrador", "servidor", "vendedor", "spendidor", "vendidor", "destameđador", "inbasador", "spontidor", "perkurador", "batidor", "kojidor", "bevador", "dulador", "torŧidor", "minador", "modifegador", "koverdidor", "bevidor", "teñidor")),
			new LineEntry("èr", SetHelper.setOf("ar", "areto", "ereto"), "[^i]èr", Arrays.asList("fresèr", "melegèr", "kanpèr", "masèr", "stropèr", "brulèr", "verdasèr", "verdaŧèr", "bonbaxèr", "sojèr", "baretèr", "kalderèr", "maraskèr", "fogèr", "jandèr", "inđeñèr", "papuŧèr", "bronbolèr", "spolverinèr", "piñokèr", "tabakèr", "muscèr", "laxañèr", "ŧerèr", "ŧedrèr", "maŧèr", "bronđèr", "preèr", "viperèr", "xanŧalèr", "festèr", "danŧalèr", "perèr", "saonèr", "fasinèr", "sperlusèr", "ulivèr", "balansèr", "subiolèr", "bugansèr", "skatolèr", "buganŧèr", "skorŧèr", "kutèr", "kapelèr", "kapuŧèr", "luamèr", "kalmonèr", "xinxolèr", "remesèr", "skarpèr", "kalegèr", "piñolèr", "sensèr", "brasolèr", "piatèr", "papusèr", "fasèr", "ortigèr", "veriolèr", "skagaŧèr", "ŧarfojèr", "fornèr", "mestelèr", "samitèr", "skorsèr", "latèr", "skagasèr", "ŧukèr", "kartèr", "tornèr", "ŧeriexèr", "tarmèr", "pontonèr", "nocèr", "scaxèr", "stiorèr", "solèr", "visolèr", "spulexèr", "riŧolèr", "leroɉèr", "sagèr", "ŧelegèr", "xuxolèr", "anŧopresèr", "montanèr", "fraskèr", "leamèr", "fontegèr", "ŧerfoɉèr", "sanbugèr", "mastrovelèr", "burcèr", "sorbolèr", "sperluŧèr", "pastèr", "pajèr", "konfuxionèr", "karegèr", "indeñèr", "ŧankanèr", "marinèr", "stramaŧèr", "lavexèr", "bixèr", "reloɉèr", "dudolèr", "remèr", "botonèr", "đuđolèr", "veludèr", "ansipresèr", "kaxelèr", "montañèr", "stramasèr", "kalŧinèr", "ŧestelèr", "ortrigèr", "pasarèr", "selèr", "munèr", "đojelèr", "milèr", "freŧèr", "olmèr", "personèr", "teraŧèr", "fiorèr", "luganegèr", "meliardèr", "terasèr", "karoŧèr", "pomèr", "vakèr", "bigolèr", "botèr", "balestrèr", "maskarèr", "fornaxèr", "onbrelèr", "naransèr", "đensaminèr", "molonèr", "bruskinèr", "kanevèr", "đoɉelèr", "naranŧèr", "datolèr", "ŧercèr", "krivelèr", "buŧoladèr", "gatèr", "pitèr", "karobèr", "cokolatèr", "ponèr", "kordèr", "bruñèr", "guantèr", "kalŧerèr", "skarpolèr", "stalierèr", "kalierèr", "đaletèr", "storèr", "fagèr", "vivèr", "spadèr", "roerèr", "skudelèr", "karetèr", "torkolèr", "lunèr", "salmistrèr", "fenestrèr", "sforŧanèr", "dindolèr", "finestrèr", "karosèr", "ŧokolèr", "formajèr", "karèr", "ostregèr", "bancèr", "fragèr", "onarèr", "kalsetèr", "marcèr", "marŧèr", "xavaskèr", "portenèr", "damaskèr", "ganŧèr", "marinelèr", "ŧarèr", "marsèr", "tamixèr", "sparaxèr", "ganbarèr", "sansèr", "fiaskèr", "librèr", "skoaŧèr", "orèr", "grixolèr", "skoasèr", "kavejèr", "ŧuketèr", "sabionèr", "gansèr", "codèr", "ŧatarèr", "malgaritèr", "braŧolèr", "molendinèr", "morèr", "lagrimèr", "boèr", "fragolèr", "bandèr", "balanŧèr", "verèr", "filandèr", "inxeñèr", "ostèr", "laroɉèr", "ŧirexèr", "roverèr", "lansèr", "kalŧetèr", "stañèr", "moskèr", "oxarèr", "milionèr", "coèr", "soaxèr", "lusernèr", "bixutèr", "sogèr", "roxèr", "sforsanèr", "ŧenturèr", "voltèr", "spulxèr", "mascèr", "mulinèr", "pionbèr", "lavranèr", "ŧestèr", "noxelèr", "sardelèr", "meaɉèr", "oltrigèr", "tinasèr", "kinkaɉèr", "latonèr", "tinaŧèr", "vespèr", "karñèr", "lanèr", "kalserèr", "stadelèr", "fuxèr", "galinèr", "otonèr", "ŧatèr", "sparexèr", "kapotèr", "stuvèr", "paludèr", "kodèr", "lanŧèr", "karobolèr", "peltrèr", "kristalèr", "franbolèr", "suxinèr", "lanpadèr", "ŧarfoɉèr", "puinèr", "straŧèr", "soɉèr", "trivelinèr", "vrespèr", "strasèr", "figèr", "felsèr", "kartolèr", "velèr", "armelinèr", "majèr", "anŧipresèr", "felŧèr", "olivèr", "salinèr", "koramèr", "kocèr", "antanèr", "xansalèr", "kalŧolèr", "kalsinèr", "grasinèr", "armèr", "salèr", "bastionèr", "xixibèr", "porkèr", "kanèr", "leutèr", "skaletèr", "risolèr", "biskotèr", "musatèr", "kornolèr", "lavandèr", "polastrèr", "pinèr", "bekèr", "bronbexèr", "bronxèr", "kaselèr", "ŧarexèr", "maskerèr", "ferèr", "onèr", "ranèr", "kaldarèr", "salegèr", "fritolèr", "laveđèr", "monèr", "fiokèr", "pelatèr", "saltèr", "xixolèr", "điđolèr", "đenŧaminèr", "pastiŧèr", "seradurèr", "beroèr", "margaritèr", "kodoñèr", "kavrèr", "gomarèr", "relojèr", "mortèr", "xatèr", "frutèr", "tesèr", "armilèr", "ŧerfojèr", "melionèr", "ocalèr", "maxèr", "pestèr", "persegèr", "kortelinèr", "lerojèr", "granèr", "spinèr", "ŧavatèr", "pastisèr", "kastañèr", "stopèr", "busoladèr", "ninxolèr", "pitarèr", "amolèr", "botegèr", "manganèr", "đoèr", "ŧentenèr", "varotèr", "ŧendalèr", "didolèr", "melonèr", "naspersegèr", "braxèr", "piñèr", "orarèr", "maronèr", "leñèr", "portinèr", "tripèr", "galèr", "kantèr", "mastelèr", "korbelèr", "ŧariexèr", "violèr", "kakèr", "ŧierexèr", "denŧaminèr", "specèr", "kuxinèr", "pestrinèr", "fiubèr", "kortelèr", "anemèr", "formaɉèr", "kanselèr", "bronbèr", "sanguetèr", "koronèr", "kalsolèr", "loamèr", "bokalèr", "scexèr", "argañèr", "stuèr", "petenèr", "arminèr", "diamantèr", "ŧimoxèr", "maselèr", "birèr", "patinèr", "fabricèr", "nespolèr", "brespèr", "pegorèr", "piñatèr", "salgèr", "perlèr", "oriolèr", "buŧolèr", "paɉèr", "olanèr", "kanpanèr", "malgarañèr", "magrañèr", "uxurèr", "staelèr", "telèr", "viŧolèr", "ŧerexèr", "paltumèr", "morsèr", "spañèr", "lisèr", "filatogèr", "peliŧèr", "lautèr", "ansopresèr", "bierèr", "akuavitèr", "bronbelèr", "nogèr", "bolsèr", "palmèr", "fontanèr", "kaveɉèr", "bilietèr", "kaponèr", "đanŧalèr", "bolŧèr", "ruxèr", "musèr", "piegorèr", "brondèr", "pasamanèr", "kuramèr", "peatèr", "bastèr", "perukèr", "semenŧèr", "liŧèr", "meajèr", "sperlongèr", "ventolèr", "kavalèr", "frasenèr", "limonèr", "semensèr", "tavernèr", "gotèr", "tomèr", "faxolèr", "karbonèr", "senavèr", "staliarèr", "kinkajèr", "scopetèr", "merŧèr", "lavedèr", "fogolèr", "murèr", "larojèr", "mersèr", "karatèr", "molinèr", "ŧinbanèr", "biliardèr", "busolèr", "mandolèr", "balonèr", "kaldierèr", "miliardèr", "kaxèr", "pelisèr", "đinđolèr")),
			new LineEntry("ol", SetHelper.setOf("ioleto", "iòl", "iol", "òl", "oleto"), "ol", Arrays.asList("pisarol", "skodarol", "sponxol", "coarol", "kanpañarol", "skuderol", "kanevarol", "guxarol", "borsarol", "rubarol", "primarol", "gatarol", "solŧarol", "bokarol", "rixarol", "semensarol", "gratarol", "stiŧarol", "ganŧarol", "ucarol", "spondol", "vovarol", "boarol", "ovarol", "strasarol", "xjoŧarol", "ranarol", "lumarol", "penarol", "ŧimarol", "pođol", "bavarol", "kiɉarol", "sorđarol", "libarol", "spiansarol", "kaxarol", "sorxarol", "beŧarol", "piaŧarol", "skondarol", "saldarol", "kordarol", "beđarol", "tastarol", "spianŧarol", "kamixol", "navarol", "ponterol", "erbarol", "xɉosarol", "linarol", "stentarol", "kanarol", "supiarol", "biavarol", "gansarol", "terŧarol", "fondarol", "stradarol", "ŧestarol", "parol", "liŧarol", "codarol", "ferarol", "gucarol", "sportarol", "spigarol", "sordarol", "braŧarol", "merdarol", "ŧendrarol", "pesarol", "tersol", "terŧol", "bañarol", "rasarol", "kuajarol", "koresol", "porixol", "pegorarol", "viscarol", "ŧerarol", "ladarol", "scoparol", "barkarol", "panarol", "poridol", "brasarol", "kolarol", "cavarol", "laxarol", "revendarol", "besarol", "ganxarol", "masarol", "korparol", "pajarol", "telarol", "straŧarol", "bexarol", "karol", "akuarol", "poxol", "tersarol", "đontarol", "semenŧarol", "cijarol", "pexarol", "stisarol", "đornarol", "ranpegarol", "xɉoŧarol", "ganđarol", "lisarol", "reditarol", "beverarol", "skuerarol", "podol", "salarol", "ponarol", "pestarol", "skonbrarol", "piasarol", "montixol", "basarol", "bevarol", "frutarol", "pradarol", "luxarol", "fiabarol", "kontarol", "pontarol", "farinarol", "agostarol", "draparol", "lađarol", "viñarol", "armarol", "polarol", "ŧernierol", "latarol", "tratarol", "fumarol", "kuartarol", "maŧarol", "miedarol", "bugarol", "peŧarol", "bedarol", "artarol", "boskarol", "poriđol", "gandarol", "paɉarol", "muxarol", "xjosarol", "peñarol", "sponđol", "kijarol", "solsarol", "xatarol")),
			new LineEntry("èra", SetHelper.setOf("ereta", "ara", "era", "iera", "ièra", "areta", "iereta"), "[^ceijɉñou]èra", Arrays.asList("xuxolèra", "lunèra", "otonèra", "dindolèra", "ŧelegèra", "filatogèra", "ŧarèra", "ventolèra", "leutèra", "kanèra", "paltumèra", "frutèra", "piñokèra", "melegèra", "marsèra", "sforŧanèra", "sensèra", "ŧatarèra", "lansèra", "uxurèra", "tabakèra", "bruskinèra", "kanevèra", "sparexèra", "ŧestèra", "mersèra", "ŧedrèra", "perlèra", "mastrovelèra", "fiorèra", "galèra", "skaletèra", "trivelinèra", "sabionèra", "oriolèra", "grixolèra", "morèra", "salmistrèra", "bronbolèra", "fresèra", "ŧerexèra", "busolèra", "ganbarèra", "bugansèra", "karbonèra", "pasarèra", "botegèra", "bierèra", "kanpèra", "seradurèra", "selèra", "botèra", "pitèra", "onèra", "lisèra", "ranèra", "lautèra", "skarpolèra", "fasinèra", "leamèra", "meliardèra", "pipinèra", "margaritèra", "brasolèra", "roerèra", "sansèra", "kroxèra", "guantèra", "telèra", "orarèra", "bolŧèra", "mestelèra", "tamixèra", "xixibèra", "kornolèra", "puinèra", "fiaskèra", "kapotèra", "tavernèra", "kartolèra", "paludèra", "palmèra", "pasamanèra", "braŧolèra", "balansèra", "terasèra", "skarpèra", "masèra", "stiorèra", "ŧatèra", "kalierèra", "xatèra", "oxarèra", "violèra", "rixèra", "scaxèra", "piatèra", "maskerèra", "đanŧalèra", "busoladèra", "balonèra", "bastionèra", "papuŧèra", "gansèra", "kaldarèra", "malgaritèra", "ortrigèra", "porkèra", "lanèra", "maselèra", "pulexèra", "fiubèra", "scexèra", "birèra", "pestrinèra", "ponèra", "fagèra", "ansipresèra", "stuvèra", "spinèra", "storèra", "vrespèra", "bandèra", "karobèra", "veludèra", "sogèra", "sperlusèra", "kortelinèra", "piñatèra", "kavrèra", "staelèra", "skatolèra", "brespèra", "armèra", "botonèra", "risolèra", "portenèra", "semenŧèra", "verdasèra", "milionèra", "ŧestelèra", "perukèra", "ŧeriexèra", "karoŧèra", "vespèra", "vivèra", "kortelèra", "ŧariexèra", "kuxinèra", "fasèra", "pomèra", "ŧinbanèra", "kalegèra", "melionèra", "sorbolèra", "brulèra", "piñolèra", "roxèra", "fraskèra", "peatèra", "fornèra", "ŧendalèra", "marinèra", "koramèra", "karèra", "straŧèra", "murèra", "naranŧèra", "bixutèra", "balanŧèra", "riŧolèra", "lusernèra", "stalierèra", "mastelèra", "remesèra", "bigolèra", "salèra", "fragolèra", "kutèra", "persegèra", "skorŧèra", "buŧoladèra", "skoasèra", "torkolèra", "bolsèra", "verèra", "fritolèra", "kalmonèra", "spolverinèra", "xixolèra", "musèra", "spadèra", "onarèra", "montanèra", "melonèra", "sanbugèra", "karatèra", "loamèra", "ansopresèra", "pestèra", "akuavitèra", "armelinèra", "luganegèra", "spulxèra", "pegorèra", "felŧèra", "molinèra", "ocalèra", "kalŧerèra", "kantèra", "bastèra", "lagrimèra", "orèra", "tripèra", "tesèra", "saonèra", "maŧèra", "kalserèra", "saltèra", "lavranèra", "konfuxionèra", "ŧerèra", "arminèra", "dudolèra", "bronxèra", "bokalèra", "kalŧetèra", "fragèra", "roverèra", "manganèra", "lanŧèra", "skorsèra", "pontonèra", "kapuŧèra", "korbelèra", "perèra", "lavedèra", "finestrèra", "xansalèra", "bronbèra", "kalsolèra", "sanguetèra", "kuramèra", "ŧankanèra", "kapelèra", "viperèra", "fuxèra", "ŧentenèra", "frasenèra", "kristalèra", "nespolèra", "stopèra", "sparaxèra", "diamantèra", "merŧèra", "fornaxèra", "baretèra", "kavalèra", "remèra", "đinđolèra", "biskotèra", "pinèra", "bronbexèra", "karegèra", "pelatèra", "đojelèra", "xavaskèra", "librèra", "brondèra", "strasèra", "kalŧinèra", "fontanèra", "marinelèra", "kaxelèra", "karobolèra", "freŧèra", "kalŧolèra", "nogèra", "naspersegèra", "figèra", "điđolèra", "patinèra", "maxèra", "viŧolèra", "moskèra", "đuđolèra", "skagaŧèra", "filandèra", "damaskèra", "gatèra", "ŧierexèra", "skoaŧèra", "ninxolèra", "stadèra", "franbolèra", "stramasèra", "lanpadèra", "fontegèra", "latèra", "soaxèra", "kalderèra", "voltèra", "tomèra", "maronèra", "grasinèra", "kalsinèra", "krivelèra", "marŧèra", "maraskèra", "jaŧèra", "kalsetèra", "monèra", "maskarèra", "onbrelèra", "kordèra", "ŧenturèra", "amolèra", "latonèra", "armilèra", "laveđèra", "piegorèra", "samitèra", "mulinèra", "felsèra", "kaponèra", "danŧalèra", "ŧarexèra", "pitarèra", "portinèra", "ŧukèra", "tarmèra", "đaletèra", "musatèra", "antanèra", "sperlongèra", "petenèra", "kakèra", "karetèra", "kaselèra", "granèra", "pastèra", "jasèra", "xinxolèra", "braxèra", "sperluŧèra", "tinaŧèra", "vakèra", "olivèra", "festèra", "tinasèra", "ulivèra", "faxolèra", "salinèra", "sforsanèra", "gomarèra", "đenŧaminèra", "polastrèra", "gotèra", "peltrèra", "suxinèra", "stadelèra", "bilietèra", "ŧimoxèra", "ŧirexèra", "semensèra", "cokolatèra", "munèra", "salgèra", "verdaŧèra", "milèra", "morsèra", "visolèra", "velèra", "ŧavatèra", "mandolèra", "stropèra", "ruxèra", "pelisèra", "sagèra", "bixèra", "denŧaminèra", "naransèra", "galinèra", "molonèra", "sorđèra", "karosèra", "sorxèra", "ostregèra", "salegèra", "pastiŧèra", "đensaminèra", "ferèra", "bekèra", "biliardèra", "olmèra", "pastisèra", "buŧolèra", "oltrigèra", "personèra", "sordèra", "noxelèra", "balestrèra", "buganŧèra", "skagasèra", "kanselèra", "peliŧèra", "xanŧalèra", "pionbèra", "datolèra", "molendinèra", "đoɉelèra", "didolèra", "codèra", "bronđèra", "olanèra", "senavèra", "varotèra", "anŧipresèra", "anŧopresèra", "stramaŧèra", "kanpanèra", "ŧuketèra", "kartèra", "koronèra", "fenestrèra", "kaldierèra", "bonbaxèra", "luamèra", "veriolèra", "kaxèra", "teraŧèra", "jandèra", "lavandèra", "subiolèra", "anemèra", "fiokèra", "lavexèra", "staliarèra", "sardelèra", "ostèra", "liŧèra", "skudelèra", "ŧokolèra", "limonèra", "tornèra", "ganŧèra", "bronbelèra", "scopetèra", "miliardèra", "ortigèra", "papusèra")),
			new LineEntry("ièr", SetHelper.setOf("ar", "areto", "ereto", "ier", "er"), "ièr", Arrays.asList("ternièr", "konsièr", "labardièr", "magadenièr", "seremonièr", "lavorièr", "skolièr", "pupièr", "faŧendièr", "stalièr", "tramesièr", "arsièr", "limoxinièr", "timonièr", "kaxermièr", "konŧièr", "gadetièr", "kredenŧièr", "speŧièr", "prièr", "portièr", "spontièr", "vedrièr", "ardentièr", "provièr", "spediŧionièr", "magađenièr", "daŧièr", "kanŧelièr", "matonièr", "kristièr", "xberovièr", "taolièr", "xbaroièr", "langurièr", "xbravièr", "drapièr", "spesièr", "doanièr", "karnièr", "karserièr", "gropièr", "pirièr", "solasièr", "laorièr", "ŧervièr", "karŧerièr", "dasièr", "virièr", "spiŧièr", "magaxenièr", "ponpièr", "ovrièr", "fuxilièr", "spasidièr", "guardarobièr", "solaŧièr", "klistièr", "đardinièr", "spalièr", "burlièr", "venturièr", "despensièr", "skufièr", "fasendièr", "spasixièr", "ofelièr", "kasièr", "vivandièr", "tapeŧièr", "ŧimièr", "arkibuxièr", "maxagenièr", "gabièr", "falkonièr", "tapesièr", "popièr", "salumièr", "arxentièr", "barbièr", "manifaturièr", "dopièr", "balkonièr", "postièr", "arđentièr", "kredensièr", "fitanŧièr", "biskaŧièr", "brasièr", "fabriŧièr", "skorlièr", "drogièr", "spoletièr", "furatolièr", "gondolièr", "braŧièr", "texorièr", "tramisièr", "kalamièr", "biskasièr", "pelalièr", "fabrisièr", "bankonièr", "lemoxinièr", "fitansièr", "gaxetièr", "pivièr", "dispensièr", "arŧièr", "texaurièr", "xbarovièr", "kinkalièr", "usièr", "angurièr", "kondotièr", "kavièr", "konfeturièr", "xberoièr", "kafetièr", "gađetièr", "spasiđièr", "ŧerimonièr")),
			new LineEntry("èr", SetHelper.setOf("ièr", "ier", "iereto"), "[^ceijɉñou]èr", Arrays.asList("fresèr", "melegèr", "kanpèr", "masèr", "stropèr", "brulèr", "verdasèr", "verdaŧèr", "bonbaxèr", "baretèr", "kalderèr", "maraskèr", "fogèr", "jandèr", "papuŧèr", "bronbolèr", "spolverinèr", "piñokèr", "tabakèr", "ŧerèr", "ŧedrèr", "maŧèr", "bronđèr", "viperèr", "xanŧalèr", "festèr", "danŧalèr", "perèr", "saonèr", "fasinèr", "sperlusèr", "ulivèr", "balansèr", "subiolèr", "bugansèr", "skatolèr", "buganŧèr", "skorŧèr", "kutèr", "kapelèr", "kapuŧèr", "luamèr", "kalmonèr", "xinxolèr", "remesèr", "skarpèr", "kalegèr", "piñolèr", "sensèr", "brasolèr", "piatèr", "papusèr", "fasèr", "ortigèr", "veriolèr", "skagaŧèr", "fornèr", "mestelèr", "samitèr", "skorsèr", "latèr", "skagasèr", "ŧukèr", "kartèr", "tornèr", "ŧeriexèr", "tarmèr", "pontonèr", "scaxèr", "stiorèr", "solèr", "visolèr", "spulexèr", "riŧolèr", "sagèr", "ŧelegèr", "xuxolèr", "anŧopresèr", "montanèr", "fraskèr", "leamèr", "fontegèr", "sanbugèr", "mastrovelèr", "sorbolèr", "sperluŧèr", "pastèr", "konfuxionèr", "karegèr", "ŧankanèr", "marinèr", "stramaŧèr", "lavexèr", "bixèr", "dudolèr", "remèr", "botonèr", "đuđolèr", "veludèr", "ansipresèr", "kaxelèr", "stramasèr", "kalŧinèr", "ŧestelèr", "ortrigèr", "pasarèr", "selèr", "munèr", "đojelèr", "milèr", "freŧèr", "olmèr", "personèr", "teraŧèr", "fiorèr", "luganegèr", "meliardèr", "terasèr", "karoŧèr", "pomèr", "vakèr", "bigolèr", "botèr", "balestrèr", "maskarèr", "fornaxèr", "onbrelèr", "naransèr", "đensaminèr", "molonèr", "bruskinèr", "kanevèr", "đoɉelèr", "naranŧèr", "datolèr", "krivelèr", "buŧoladèr", "gatèr", "pitèr", "karobèr", "cokolatèr", "ponèr", "kordèr", "guantèr", "kalŧerèr", "skarpolèr", "stalierèr", "kalierèr", "đaletèr", "storèr", "fagèr", "vivèr", "spadèr", "roerèr", "skudelèr", "karetèr", "torkolèr", "lunèr", "salmistrèr", "fenestrèr", "sforŧanèr", "dindolèr", "finestrèr", "karosèr", "ŧokolèr", "karèr", "ostregèr", "fragèr", "onarèr", "kalsetèr", "marŧèr", "xavaskèr", "portenèr", "damaskèr", "ganŧèr", "marinelèr", "ŧarèr", "marsèr", "tamixèr", "sparaxèr", "ganbarèr", "sansèr", "fiaskèr", "librèr", "skoaŧèr", "orèr", "grixolèr", "skoasèr", "ŧuketèr", "sabionèr", "gansèr", "codèr", "ŧatarèr", "malgaritèr", "braŧolèr", "molendinèr", "morèr", "lagrimèr", "fragolèr", "bandèr", "balanŧèr", "verèr", "filandèr", "ostèr", "ŧirexèr", "roverèr", "lansèr", "kalŧetèr", "moskèr", "oxarèr", "milionèr", "soaxèr", "lusernèr", "bixutèr", "sogèr", "roxèr", "sforsanèr", "ŧenturèr", "voltèr", "spulxèr", "mulinèr", "pionbèr", "lavranèr", "ŧestèr", "noxelèr", "sardelèr", "oltrigèr", "tinasèr", "latonèr", "tinaŧèr", "vespèr", "lanèr", "kalserèr", "stadelèr", "fuxèr", "galinèr", "otonèr", "ŧatèr", "sparexèr", "kapotèr", "stuvèr", "paludèr", "kodèr", "lanŧèr", "karobolèr", "peltrèr", "kristalèr", "franbolèr", "suxinèr", "lanpadèr", "puinèr", "straŧèr", "trivelinèr", "vrespèr", "strasèr", "figèr", "felsèr", "kartolèr", "velèr", "armelinèr", "anŧipresèr", "felŧèr", "olivèr", "salinèr", "koramèr", "antanèr", "xansalèr", "kalŧolèr", "kalsinèr", "grasinèr", "armèr", "salèr", "bastionèr", "xixibèr", "porkèr", "kanèr", "leutèr", "skaletèr", "risolèr", "biskotèr", "musatèr", "kornolèr", "lavandèr", "polastrèr", "pinèr", "bekèr", "bronbexèr", "bronxèr", "kaselèr", "ŧarexèr", "maskerèr", "ferèr", "onèr", "ranèr", "kaldarèr", "salegèr", "fritolèr", "laveđèr", "monèr", "fiokèr", "pelatèr", "saltèr", "xixolèr", "điđolèr", "đenŧaminèr", "pastiŧèr", "seradurèr", "margaritèr", "kavrèr", "gomarèr", "mortèr", "xatèr", "frutèr", "tesèr", "armilèr", "melionèr", "ocalèr", "maxèr", "pestèr", "persegèr", "kortelinèr", "granèr", "spinèr", "ŧavatèr", "pastisèr", "stopèr", "busoladèr", "ninxolèr", "pitarèr", "amolèr", "botegèr", "manganèr", "ŧentenèr", "varotèr", "ŧendalèr", "didolèr", "melonèr", "naspersegèr", "braxèr", "orarèr", "maronèr", "portinèr", "tripèr", "galèr", "kantèr", "mastelèr", "korbelèr", "ŧariexèr", "violèr", "kakèr", "ŧierexèr", "denŧaminèr", "kuxinèr", "pestrinèr", "fiubèr", "kortelèr", "anemèr", "kanselèr", "bronbèr", "sanguetèr", "koronèr", "kalsolèr", "loamèr", "bokalèr", "scexèr", "petenèr", "arminèr", "diamantèr", "ŧimoxèr", "maselèr", "birèr", "patinèr", "nespolèr", "brespèr", "pegorèr", "piñatèr", "salgèr", "perlèr", "oriolèr", "buŧolèr", "olanèr", "kanpanèr", "uxurèr", "staelèr", "telèr", "viŧolèr", "ŧerexèr", "paltumèr", "morsèr", "lisèr", "filatogèr", "peliŧèr", "lautèr", "ansopresèr", "bierèr", "akuavitèr", "bronbelèr", "nogèr", "bolsèr", "palmèr", "fontanèr", "bilietèr", "kaponèr", "đanŧalèr", "bolŧèr", "ruxèr", "musèr", "piegorèr", "brondèr", "pasamanèr", "kuramèr", "peatèr", "bastèr", "perukèr", "semenŧèr", "liŧèr", "sperlongèr", "ventolèr", "kavalèr", "frasenèr", "limonèr", "semensèr", "tavernèr", "gotèr", "tomèr", "faxolèr", "karbonèr", "senavèr", "staliarèr", "scopetèr", "merŧèr", "lavedèr", "fogolèr", "murèr", "mersèr", "karatèr", "molinèr", "ŧinbanèr", "biliardèr", "busolèr", "mandolèr", "balonèr", "kaldierèr", "miliardèr", "kaxèr", "pelisèr", "đinđolèr")),
			new LineEntry("dora", SetHelper.setOf("toreta", "tora", "doreta", "oreta", "ora"), "dora", Arrays.asList("spendidora", "koverdidora", "pendadora", "đenadora", "stendidora", "karadora", "destamedadora", "anbasadora", "skodidora", "frotadora", "modifegadora", "konponidora", "vendidora", "vensadora", "koɉidora", "batidora", "torsidora", "penxadora", "mapadora", "duladora", "minadora", "odidora", "venŧadora", "pulidora", "bevadora", "anbasiadora", "konplementadora", "latinadora", "oponidora", "destameđadora", "destamexadora", "torŧidora", "tordidora", "kontradora", "kojidora", "inbasadora", "penđadora", "perkuradora", "teñidora", "koverđidora", "konpoxidora", "koñidora", "servidora", "udidora", "koridora", "torxidora", "revedadora", "vendedora", "sasadadora", "bevidora", "fondidora", "inperadora", "strusadora", "torđidora", "koverxidora", "avogadora")),
			new LineEntry("èra", SetHelper.setOf("ereta", "ara", "era", "areta"), "[cejɉñou]èra", Arrays.asList("stañèra", "indeñèra", "preèra", "coèra", "kastañèra", "bancèra", "ŧercèra", "kavejèra", "kinkaɉèra", "majèra", "montañèra", "burcèra", "nocèra", "kinkajèra", "formaɉèra", "paɉèra", "ŧarfojèra", "mascèra", "leroɉèra", "kuaɉèra", "bruñèra", "piñèra", "muscèra", "beroèra", "magrañèra", "boèra", "argañèra", "meaɉèra", "leñèra", "reloɉèra", "marcèra", "specèra", "ŧerfojèra", "stuèra", "sojèra", "ŧerfoɉèra", "kuajèra", "ŧarfoɉèra", "larojèra", "laroɉèra", "lerojèra", "relojèra", "kodoñèra", "inxeñèra", "kocèra", "meajèra", "malgarañèra", "kaveɉèra", "đoèra", "fabricèra", "soɉèra", "laxañèra", "pajèra", "spañèra", "inđeñèra", "formajèra")),
			new LineEntry("ola", SetHelper.setOf("òla", "ioleta", "iola", "oleta", "iòla"), "ola", Arrays.asList("peŧarola", "beverarola", "ferarola", "kolarola", "kasola", "spigarola", "đontarola", "sponxarola", "tersarola", "polarola", "inpiŧarola", "lumarola", "fiabarola", "stradarola", "kuajarola", "salarola", "besarola", "armarola", "peñarola", "skaɉarola", "stisarola", "piasarola", "ranpegarola", "laxarola", "kaxarola", "kaŧola", "spiansarola", "orbarola", "stiŧarola", "terŧarola", "luxarola", "ucarola", "stuarola", "spianŧarola", "straŧarola", "bañarola", "erbarola", "sponđarola", "gratarola", "đornarola", "boarola", "skondarola", "braŧarola", "rigarola", "penarola", "cavarola", "pisarola", "kamixola", "bedarola", "lisarola", "moskarola", "tetarola", "ŧestarola", "pestarola", "semenŧarola", "kanpañarola", "ranarola", "primarola", "inpisarola", "strasarola", "pegorarola", "koarola", "skuerarola", "semensarola", "piaŧarola", "međarola", "akuarola", "boskarola", "pradarola", "guxarola", "brasarola", "gucarola", "viñarola", "fondarola", "trexola", "farinarola", "rubarola", "tređola", "mexarola", "varola", "kanevarola", "pasarola", "bokarola", "miedarola", "beŧarola", "skinkarola", "agostarola", "bexarola", "basarola", "frutarola", "kordarola", "ganbarola", "beđarola", "kagarola", "spuarola", "sordarola", "coarola", "ŧekarola", "ɉoŧarola", "ganđarola", "josarola", "bandarola", "kaŧarola", "biavarola", "ɉosarola", "revendarola", "saldarola", "latarola", "telarola", "kasarola", "ganxarola", "barkarola", "sorđarola", "xbacarola", "tastarola", "gandarola", "xatarola", "reditarola", "kontarola", "codarola", "spondarola", "tremarola", "sorxarola", "sportarola", "linarola", "ŧerarola", "kiɉarola", "panarola", "pesarola", "libarola", "kanarola", "borsarola", "medarola", "rasarola", "rasparola", "bugarola", "muxarola", "joŧarola", "gatarola", "skajarola", "frakarola", "navarola", "ŧendrarola", "ponarola", "kijarola", "tredola", "skonbrarola", "cijarola")),
			new LineEntry("ièra", SetHelper.setOf("ereta", "ara", "era", "areta"), "ièra", Arrays.asList("karnièra", "kondotièra", "pelalièra", "arkibuxièra", "salumièra", "ofelièra", "fuxilièra", "magaxenièra", "fitansièra", "timonièra", "gabièra", "spoletièra", "xbaroièra", "balkonièra", "bankonièra", "labardièra", "kasièra", "kredenŧièra", "pupièra", "tramisièra", "gaxetièra", "venturièra", "konfeturièra", "drogièra", "skolièra", "karserièra", "spasidièra", "đardinièra", "konŧièra", "laorièra", "despensièra", "kanŧelièra", "solaŧièra", "fabriŧièra", "gađetièra", "arsièra", "gadetièra", "biskasièra", "burlièra", "skorlièra", "xbarovièra", "pirièra", "tapeŧièra", "ovrièra", "xberovièra", "kalamièra", "brasièra", "dasièra", "ternièra", "popièra", "solasièra", "lavorièra", "spasixièra", "lemoxinièra", "dopièra", "ŧervièra", "spontièra", "virièra", "kristièra", "falkonièra", "magađenièra", "portièra", "speŧièra", "stalièra", "kinkalièra", "usièra", "postièra", "prièra", "xbravièra", "karŧerièra", "manifaturièra", "spediŧionièra", "drapièra", "doanièra", "maxagenièra", "konsièra", "kafetièra", "spesièra", "kavièra", "tramesièra", "kredensièra", "ardentièra", "guardarobièra", "skufièra", "braŧièra", "xberoièra", "pivièra", "vedrièra", "furatolièra", "vivandièra", "matonièra", "fasendièra", "arxentièra", "taolièra", "seremonièra", "klistièra", "spiŧièra", "provièra", "limoxinièra", "spalièra", "fabrisièra", "dispensièra", "biskaŧièra", "arŧièra", "ponpièra", "faŧendièra", "tapesièra", "texorièra", "spasiđièra", "gondolièra", "kaxermièra", "daŧièra", "fitanŧièra", "angurièra", "texaurièra", "langurièra", "magadenièra", "arđentièra", "gropièra"))
		);
		Assertions.assertEquals(expectedCompactedRules, compactedRules);

		List<String> rules = reducer.convertFormat(flag, false, compactedRules);
		List<String> expectedRules = Arrays.asList(
			"SFX V0 Y 46",
			"SFX V0 0 eto ol",
			"SFX V0 ol òl ol",
			"SFX V0 ol iol ol",
			"SFX V0 ol iòl ol",
			"SFX V0 ol ioleto ol",
			"SFX V0 a eta ola",
			"SFX V0 0 eto dor",
			"SFX V0 èr er ièr",
			"SFX V0 ola òla ola",
			"SFX V0 ola iola ola",
			"SFX V0 ola iòla ola",
			"SFX V0 ola ioleta ola",
			"SFX V0 ièr ar ièr",
			"SFX V0 ièr er ièr",
			"SFX V0 ièr areto ièr",
			"SFX V0 ièr ereto ièr",
			"SFX V0 dor or dor",
			"SFX V0 dor tor dor",
			"SFX V0 dor oreto dor",
			"SFX V0 dor toreto dor",
			"SFX V0 èr ar [^i]èr",
			"SFX V0 èr areto [^i]èr",
			"SFX V0 èr ereto [^i]èr",
			"SFX V0 èr ier [^ceijɉñou]èr",
			"SFX V0 èr ièr [^ceijɉñou]èr",
			"SFX V0 èr iereto [^ceijɉñou]èr",
			"SFX V0 a eta dora",
			"SFX V0 ièra ara ièra",
			"SFX V0 ièra era ièra",
			"SFX V0 ièra areta ièra",
			"SFX V0 ièra ereta ièra",
			"SFX V0 dora ora dora",
			"SFX V0 dora tora dora",
			"SFX V0 dora oreta dora",
			"SFX V0 dora toreta dora",
			"SFX V0 èra ara [cejɉñou]èra",
			"SFX V0 èra era [cejɉñou]èra",
			"SFX V0 èra areta [cejɉñou]èra",
			"SFX V0 èra ereta [cejɉñou]èra",
			"SFX V0 èra ara [^ceijɉñou]èra",
			"SFX V0 èra era [^ceijɉñou]èra",
			"SFX V0 èra iera [^ceijɉñou]èra",
			"SFX V0 èra ièra [^ceijɉñou]èra",
			"SFX V0 èra areta [^ceijɉñou]èra",
			"SFX V0 èra ereta [^ceijɉñou]èra",
			"SFX V0 èra iereta [^ceijɉñou]èra"
		);
		Assertions.assertEquals(expectedRules, rules);

		reducer.checkReductionCorrectness(flag, rules, originalRules, originalLines);
	}

	@Test
	public void case21() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"LANG vec",
			"FLAG long",
			"SFX U0 Y 10",
			"SFX U0 l i [àéèóòú]l",
			"SFX U0 n i [àéèóòú]n",
			"SFX U0 al ài al",
			"SFX U0 an ài an",
			"SFX U0 el éi el",
			"SFX U0 en éi en",
			"SFX U0 ol ói ol",
			"SFX U0 on ói on",
			"SFX U0 ul úi ul",
			"SFX U0 un úi un"
		);
		Pair<RulesReducer, WordGenerator> pair = createReducer(affFile);
		RulesReducer reducer = pair.getLeft();
		WordGenerator wordGenerator = pair.getRight();
		String flag = "U0";
		List<String> words = Arrays.asList("bixnòn", "bontòn", "patròn", "mexèn", "pièn", "međèn", "alièn", "armèn", "sòn", "tòn", "trèn", "bonbòn", "spròn", "òn", "medèn", "saòn", "kakasèn", "ultrateren", "guardamagađen", "seren", "baxen", "međen", "taja–fen", "magaxen", "teren", "saraŧen", "repien", "meden", "similituden", "sen", "stra-ben", "magađen", "mexen", "maxagen", "baŧen", "velen", "straben", "basen", "guardamagaxen", "taɉafen", "terapien", "taɉa–fen", "tajafen", "magaden", "notaben", "tientinben", "guardamagaden", "stra-pien", "strapien", "remoŧion", "autorixasion", "radaŧion", "filtraŧion", "tinason", "xlovon", "paragon", "faraon", "manaŧon", "inrotulaŧion", "truson", "sonorixaŧion", "frustadon", "kalandron", "ŧavaton", "malision", "groon", "emision", "peaŧon", "arđaron", "sifon", "xɉonfon", "bandieron", "boladon", "goldon", "lion", "ubigaŧion", "folon", "gucon", "kotolon", "moreton", "tormenton", "garlon", "piavolon", "previxion", "ventron", "greson", "salvaŧion", "milion", "legaŧion", "brustolon", "guidon", "bagajon", "pekatoron", "dopion", "velion", "sudiŧion", "ventolon", "sinserasion", "galidaŧion", "erbion", "xustapoxiŧion", "darion", "rođeton", "braŧon", "xganbeton", "stafon", "union", "skuakueron", "litigon", "arđiñon", "baston", "tavaron", "alberon", "boton", "kospeton", "torŧon", "torcon", "proporŧion", "bregieron", "xganberlon", "xguaɉaton", "sporton", "konmixeraŧion", "lokuŧion", "kaveŧon", "lanŧon", "bardon", "retrocesion", "saon", "balkon", "strapason", "ruđaron", "ŧinbanon", "koređon", "xlapaŧon", "ruxenon", "traxlokaŧion", "tripon", "axenon", "skison", "marangon", "santon", "konpañon", "prexupoxiŧion", "pòrta–speron", "prexion", "krepon", "omenon", "pòrta–ŧenjon", "ronkon", "rataparaŧion", "tortijon", "vendiŧion", "tarlixon", "rebelion", "praxon", "iluminaŧion", "xdenton", "skorexon", "konserton", "sipiton", "danaŧion", "reŧixion", "dromon", "gropon", "rejeŧion", "dormeɉon", "tonon", "poltron", "kokalon", "pilon", "strukaŧion", "parangon", "teñon", "fifon", "beaton", "pisegon", "polmon", "palason", "xeneralixasion", "straxordenaŧion", "vermilion", "stupinon", "tugon", "malvarsaŧion", "destinŧion", "ŧetaŧion", "xbesolon", "kuajon", "destraton", "niolon", "gabion", "superstiŧion", "revendon", "destinaŧion", "sovenŧion", "gravion", "elixion", "konstituŧion", "bianketon", "skopaŧon", "redacon", "ŧaltron", "trabukon", "insinuaŧion", "fiankon", "leon", "rekuixiŧion", "talpon", "kostituŧion", "poron", "kostriŧion", "suŧeŧion", "xbadajon", "xlubion", "presepiton", "frankon", "tonbolon", "dreton", "kavalon", "oraŧion", "arpion", "ruxon", "moniŧion", "sekamincon", "rekluxion", "inbrojon", "mason", "xmergon", "puliton", "konŧon", "lapidon", "torbolaŧion", "citinon", "oparaŧion", "finton", "kornidon", "batajon", "napolion", "sotoskriŧion", "nomenaŧion", "dormiɉon", "boxon", "dexvion", "malfaŧion", "bragon", "vanedon", "bokon", "susesion", "eskorporaŧion", "ponson", "xbaɉon", "portaboŧon", "stangon", "ŧedron", "spilungon", "dukadon", "otaŧion", "faeton", "xbaɉafon", "đeneraliđaŧion", "palkon", "bibaron", "kartelon", "mision", "kokon", "traŧion", "spesifegasion", "artikolaŧion", "sastufaŧion", "speron", "ultimaŧion", "ŧiton", "raprexaŧion", "ŧurlon", "spaŧeton", "xjonfon", "mormoraŧion", "saxon", "naturaliđaŧion", "trangujon", "vokaliđaŧion", "kalabron", "provixion", "inkatiɉon", "mastelon", "traverson", "đafranon", "paston", "pelaton", "ridon", "marxokon", "potacon", "traversaŧion", "skaton", "pegoron", "xbolsegon", "bonbon", "vokalixaŧion", "xvejadon", "skuakuaron", "vasinasion", "koniugaŧion", "autoridaŧion", "kogolon", "dormicon", "plafon", "mañamaron", "pruŧision", "konfesion", "gobon", "sason", "ronpon", "aseton", "kavion", "sistemaŧion", "spuacon", "supuraŧion", "ruson", "duron", "santilion", "bonpaston", "spondon", "điridon", "salarion", "xveɉadon", "gardon", "naturalixasion", "stramaŧon", "baxon", "espurgaŧion", "privaŧion", "diskreŧion", "morelon", "sutilon", "desturbon", "identifegaŧion", "sinsierasion", "fenestron", "frugon", "tortejon", "makaron", "konkruxion", "papaŧon", "xguseton", "estermenaŧion", "postijon", "kostipaŧion", "arson", "đonŧion", "dromeɉon", "flesion", "lanpion", "bagaron", "sostituŧion", "afliŧion", "meskolon", "modejon", "xbefon", "panidaŧion", "bagon", "karnaxon", "peñoraŧion", "bonton", "sonoriđaŧion", "cucon", "korexon", "bison", "buxion", "xenaralixaŧion", "manŧipaŧion", "fanfruñon", "mostron", "skitoŧon", "pòrta–boŧon", "maneŧon", "serolon", "kareton", "balordon", "skoređon", "tension", "strakolaŧion", "putelon", "domandon", "fosadon", "ranpiñon", "struxon", "piŧon", "infiaxon", "ŧeɉon", "formaŧion", "budaron", "legalidaŧion", "patalon", "radon", "partisipasion", "dejestion", "ŧixon", "kuadron", "dexluvion", "vixitaŧion", "cetaŧion", "pinson", "kavelon", "sureŧion", "scafon", "grason", "partiŧipaŧion", "skalinon", "grixolon", "moskaton", "likon", "xganbirlon", "đirandolon", "maskaron", "spuledon", "pianton", "porŧesion", "piton", "rubon", "berikinon", "spasidon", "kareŧon", "dondelon", "varsaŧion", "proŧesion", "rekuiđiŧion", "đipon", "skarpaŧon", "pikandolon", "vestiŧion", "oson", "staŧadon", "indikaŧion", "barnaboton", "realiđaŧion", "vidon", "kaveon", "xbravaŧon", "speraŧion", "savarion", "asimilaŧion", "balanŧon", "konfuxion", "maskalŧon", "pertegon", "kufolon", "kostion", "ruvijon", "setasion", "insolenton", "taolon", "polakon", "kaŧaxon", "konfalon", "palatiđaŧion", "porŧion", "xbrasaron", "spontiñon", "terminaŧion", "gruñon", "kuistion", "revokaŧion", "ŧenjon", "đabajon", "botiron", "vibraŧion", "divixion", "kordolon", "kokolon", "rexiron", "burcon", "đogatolon", "markanton", "presipiton", "mexuraŧion", "falŧon", "kavadon", "tranxaŧion", "aprension", "skoŧon", "visiadon", "brikon", "sialakuon", "stajon", "malagraŧion", "piatolon", "rasionasion", "batialon", "tirinton", "ridacon", "friŧion", "pelon", "straŧion", "xbroeton", "faton", "perlustraŧion", "furon", "konsekuŧion", "strukon", "permutaŧion", "palpon", "tirabuson", "spenton", "adpoxiŧion", "manegon", "skaveŧon", "xguataron", "fenŧion", "majon", "lixon", "turbion", "anemalon", "sabalon", "inpersonifegaŧion", "fregolon", "paon", "xlonfron", "patanfion", "xbrindolon", "torđion", "struŧion", "notifegaŧion", "frusion", "koson", "pacafon", "xguason", "kulon", "cakaron", "leniŧion", "maraveɉon", "taparon", "postion", "xlufon", "variaŧion", "rostelon", "lenguason", "sekakojon", "groton", "komunegaŧion", "kanevon", "spenditon", "lađaron", "luminaŧion", "dimension", "toron", "pamojon", "reɉeŧion", "radiron", "raviŧon", "totanon", "xlodron", "okaxion", "pipon", "skaveɉon", "dixnaron", "đafaranon", "malversaŧion", "ruđon", "uŧixion", "fiokon", "gravalon", "bistion", "produŧion", "anbon", "xlofon", "autoriđaŧion", "inibiŧion", "turbulaŧion", "kucaron", "guanton", "tajakanton", "xmerdon", "groson", "xonsion", "luxerton", "maskarpon", "supion", "vimenon", "recon", "tinaŧon", "maraveon", "bevon", "stekon", "ŧiŧalardon", "joŧon", "kamaŧon", "fiɉolon", "ladron", "arxaron", "fraɉon", "servision", "numaraŧion", "fruston", "sinŧeraŧion", "kabalon", "bion", "portasenjon", "kormelon", "skapuson", "xmafaron", "trinkon", "maleŧion", "strolegaŧion", "kavilon", "instruŧion", "mixeron", "piston", "sanpikon", "diavolon", "kondiŧion", "bordion", "ŧapegon", "speŧegon", "deminuŧion", "stranbon", "kastron", "satusfaŧion", "vokalidaŧion", "faŧion", "xguaŧaron", "makeron", "skiŧon", "fiadon", "granson", "okon", "lanson", "capon", "roxaron", "guardaporton", "desipon", "paneton", "ŧiñon", "torson", "regason", "fraton", "laxaron", "bokolon", "baron", "elevaŧion", "bestion", "sandon", "loskon", "skrokon", "soversion", "xbordelon", "pikon", "votaŧion", "sermon", "fagoton", "farfujon", "rixon", "stison", "brokon", "tinpanon", "palaŧon", "naŧion", "xguŧeton", "faŧoleton", "depoxitaŧion", "vagabondon", "infetaŧion", "subarendaŧion", "mojon", "opoxiŧion", "fetaŧion", "tontonon", "skulaŧon", "aŧion", "korosolon", "kuŧolon", "papon", "duŧion", "porŧelon", "kavalaron", "mucon", "codon", "skiton", "kavedon", "papalon", "fadigon", "noŧenton", "staxon", "salamon", "arxiñon", "alcon", "ranson", "revoltolon", "maniŧon", "rondon", "ŧankon", "represion", "senpliŧon", "ruxaron", "cexon", "realixaŧion", "amortidaŧion", "betegon", "ređiron", "stradon", "deluvion", "ŧimexon", "trasformaŧion", "ŧopegon", "peŧon", "mataron", "reolton", "bereton", "spulexon", "saladon", "tiraboŧon", "skoredon", "vaneŧon", "xgriñon", "koordenaŧion", "roñon", "xgrinfon", "peskon", "remiŧion", "tintinbon", "piantadon", "miraŧion", "peton", "fuxion", "polŧon", "sfrakason", "fregaŧion", "pendon", "exenpon", "restelon", "deŧipon", "rasion", "biñon", "ŧenpedon", "futiñon", "prexunŧion", "palatidaŧion", "exborsaŧion", "balotaŧion", "storton", "kolaudaŧion", "legalixasion", "kuadreton", "garđon", "maron", "inparsonifegaŧion", "rufianon", "ruvidon", "riŧon", "kuestion", "direŧion", "augumentaŧion", "vokaŧion", "jandon", "provokaŧion", "gravelaron", "klasifegaŧion", "fievaron", "jurixdiŧion", "iñoranton", "rustegon", "petegolon", "petolon", "bolpon", "kapelon", "ason", "skuartadon", "xgalmaron", "komeson", "kuŧon", "kucon", "kontaminaŧion", "bojon", "xlavaŧon", "eskusion", "insixion", "stornelon", "lagremon", "morñon", "prusision", "salpikon", "strologaŧion", "rufon", "modijon", "parladoron", "palon", "koresponsion", "galixaŧion", "galon", "dukaton", "fijolon", "suplegaŧion", "kojon", "spesegon", "xberloton", "barbixon", "spakon", "galiđaŧion", "matafion", "foson", "tenkon", "kreson", "pluton", "spion", "ŧeñon", "fogeron", "xgarafon", "invaxion", "soviŧion", "linon", "ostregon", "butaŧion", "panixasion", "puniŧion", "bragañon", "petaŧion", "supon", "skaldaŧion", "radegon", "stramason", "kusolon", "fugason", "cokolaton", "jiron", "naturalidaŧion", "makinon", "foton", "spiron", "kandelieron", "benon", "xberlon", "maon", "molton", "spalaŧon", "fioron", "opinaŧion", "mañon", "đavaɉon", "xmenbraŧion", "kastelon", "feniŧion", "furlon", "berganŧon", "pingolon", "arditon", "fasion", "valon", "ŧigalon", "gordon", "pacofon", "buxiaron", "simon", "kaston", "fadegon", "jintiron", "piaɉon", "scaton", "konplension", "ocon", "biondon", "seka–mincon", "mansipasion", "megolon", "introduŧion", "skagaron", "susuron", "interpretaŧion", "frixon", "obligaŧion", "spendaŧon", "diron", "pison", "sarangon", "spendacon", "ŧirkospeŧion", "morion", "prevarikaŧion", "boldon", "orbon", "oton", "baronon", "polon", "đeneraŧion", "bajon", "inpiantajon", "bitaŧion", "fosaton", "sporkaŧon", "negaŧion", "tordion", "fogon", "taɉon", "semiton", "bondanton", "desperadon", "kodegon", "texon", "refudaxon", "pelandron", "mardokon", "konvenŧion", "mansion", "sisalardon", "patron", "stañadon", "saguraŧion", "xbajafon", "kurioxon", "sekuraŧion", "đogadoron", "cicon", "suparstiŧion", "xgorlon", "proaŧion", "lobion", "koncesion", "pianeton", "turlon", "deskreŧion", "palatixaŧion", "inkursion", "xbrajon", "vespon", "destribuŧion", "speon", "benefiŧion", "buđaron", "bukolon", "fasendon", "sardon", "maskalson", "balanson", "ŧukon", "sklamaŧion", "talon", "garxon", "urton", "rexon", "spinon", "cacaron", "barbaŧon", "konesion", "fason", "soldon", "skalinaŧion", "donđelon", "numeraŧion", "rexoluŧion", "rafegon", "realidaŧion", "pinion", "fionon", "kontrokapon", "ŧotegon", "koredon", "gralaon", "xgarbon", "tartixon", "nevolon", "siegon", "rodolon", "rivaŧion", "kañon", "patanflon", "xmardelon", "opinion", "diminuŧion", "boridon", "berolon", "fakinon", "rikon", "sojeton", "senplision", "kapostasion", "examinaŧion", "malediŧion", "tenajon", "brikonon", "xguajaton", "sodisfaŧion", "peston", "franbolon", "afermaŧion", "muxon", "paton", "preŧepiton", "rodon", "gastaldon", "kodarosolon", "pacon", "loɉon", "guadañon", "lerijon", "điganton", "streton", "regolon", "fioreton", "spekulaŧion", "spiantadon", "avaron", "skarpelon", "finiŧion", "ŧerkanton", "xguaŧon", "rusolon", "arion", "konbustion", "lenguaŧon", "sirkospesion", "trufon", "mormolaŧion", "stivalon", "bobon", "manipolaŧion", "budelon", "seolon", "kalŧon", "mokajon", "feltron", "frustegon", "kavaron", "garbuɉon", "piaton", "teŧon", "tecon", "atribuŧion", "perdon", "rapatumaŧion", "joton", "abaton", "skalon", "kasaxon", "dixestion", "repetiŧion", "koron", "tabakon", "omason", "naxon", "buxieron", "xmañason", "kreaŧion", "scafaŧon", "reversion", "kordialon", "konŧerton", "kosperon", "kamason", "barbon", "jaketon", "sindikaŧion", "rudaron", "levaŧion", "boxiaron", "skorpion", "preŧixaŧion", "skosion", "operaŧion", "vidixon", "tonaŧion", "afaron", "morsegon", "joson", "ruŧolon", "dromejon", "blataron", "binaŧion", "vaneđon", "strapon", "provaŧion", "dindolon", "staŧion", "bicaron", "goloxon", "trafegon", "fruñon", "visigon", "skopeloton", "politon", "piajon", "botegon", "koaton", "rađon", "suspenŧion", "pason", "kanton", "espoxiŧion", "kortelon", "rospon", "mutrion", "tolon", "satisfaŧion", "pacugon", "piantaxon", "imajinaŧion", "kastañon", "spaseton", "fanfaron", "đavajon", "skorporaŧion", "skalseron", "resixion", "famincon", "xgrafon", "xlordon", "kanŧon", "lamentaŧion", "stiŧon", "biankon", "lamenton", "fortunadon", "pasion", "maneskon", "granŧon", "talenton", "maturlon", "melon", "xgerlon", "ruspion", "caceron", "exaltaŧion", "regaŧon", "ŧukolon", "agon", "kartabon", "inkuixiŧion", "xguardon", "luxertolon", "ŧitaŧion", "konkluxion", "rodeton", "brontolon", "spanpanon", "aɉeron", "sproton", "benediŧion", "inpresion", "alokon", "skuaron", "fragolon", "iskriŧion", "trenton", "bordon", "doron", "spegaŧon", "karnevalon", "sfondron", "alteraŧion", "ŧejon", "muson", "straŧon", "retenŧion", "pospoxiŧion", "muxelon", "ŧopelon", "pòrta–senjon", "sospenŧion", "marŧon", "mortaron", "straon", "xbraŧaron", "padron", "inkonbinaŧion", "makakon", "kataron", "eskluxion", "fanferon", "flusion", "bavelon", "konmixaraŧion", "esklamaŧion", "padilion", "bragieron", "struka–limon", "xbrinon", "exenŧion", "isolon", "bubon", "spiriton", "bonason", "bastion", "emoŧion", "airon", "ardiñon", "marafon", "peson", "stilaŧion", "senplison", "fanelon", "vendegaŧion", "persesion", "proibiŧion", "suton", "subordenaŧion", "strasinon", "scapon", "xgusaron", "savon", "xgranfion", "polson", "sujiŧion", "ŧiarlon", "karpion", "karateron", "kalavron", "fronton", "kanelon", "bontenpon", "scauson", "jaon", "regalon", "stramanon", "simiton", "sucon", "suŧon", "tenaɉon", "suɉiŧion", "taŧon", "tanton", "cakolon", "gaiton", "salton", "merkanton", "xbrodolon", "disipon", "buridon", "sostentaŧion", "ajeron", "kostruŧion", "tenŧion", "stra-kulon", "kofolon", "vaskon", "modion", "sorxon", "saludaŧion", "bokalon", "punion", "pearon", "duplon", "xbevacon", "kotimon", "kopelon", "skrituraŧion", "raŧionaŧion", "tartaɉon", "rebuton", "procesion", "strakulon", "belon", "rovinon", "interkonesion", "xjonfabalon", "scexon", "ranpon", "kavajon", "xgrafiñon", "skavejon", "intromision", "pelaŧion", "partesipasion", "pamoɉon", "ardaron", "loxon", "pieron", "resureŧion", "xlavason", "devoŧion", "ŧenon", "bankon", "verdon", "xbaron", "tindon", "tientinbon", "xgarbion", "spasixon", "sorafaŧion", "kuson", "akuxaŧion", "substentaŧion", "formenton", "reputaŧion", "mustason", "bastardon", "xbandon", "kontenton", "koskriŧion", "pirlon", "trinkanon", "xvernixon", "limitaŧion", "guarniŧion", "deletaŧion", "oxelon", "konvokaŧion", "amigon", "barboton", "emulaŧion", "ranpegon", "laron", "dotoron", "konsunŧion", "inbriagon", "piantađon", "lanternon", "rejilion", "viscon", "felukon", "jotiron", "berekinon", "limon", "fufiñon", "modon", "vakon", "beniŧion", "termenaŧion", "evoluŧion", "finŧion", "komision", "rudon", "viŧiadon", "xbeacon", "batelon", "điron", "bagaɉon", "sfregolon", "legaliđaŧion", "aston", "naranŧon", "kaseton", "troton", "salison", "ŧarlatanon", "kroxon", "roxon", "soɉeton", "subvenŧion", "godon", "redaŧion", "gramolon", "spendason", "tason", "vanexon", "skurton", "berton", "đovenon", "ingraton", "tenpon", "skarpon", "raxiron", "semolon", "libraŧon", "kaparon", "materon", "saŧion", "biceron", "servaŧion", "buxon", "ŧeron", "feŧion", "rejon", "kaxion", "veladon", "amision", "indukaŧion", "dormejon", "skagason", "ŧercon", "xbrison", "intrigon", "spilorŧon", "sporkason", "sonoridaŧion", "đakolon", "bagaton", "viaxon", "boaŧon", "korusolon", "petiŧion", "faxiolon", "palpuñon", "galanton", "cetinon", "xgranfiñon", "naon", "strion", "xbrision", "barufon", "delaŧion", "stropon", "bolŧon", "palangon", "verifegaŧion", "kavaŧion", "markaŧion", "saluaŧion", "prosesion", "violon", "dexestion", "soluŧion", "enfiaxion", "kativon", "xlandron", "taɉakanton", "xguasaron", "fidaŧion", "tanburon", "pastison", "komaron", "đenocon", "donŧion", "sospeŧion", "ruñon", "teson", "xlondron", "dexmentegon", "sabajon", "kavason", "fraskon", "vixdekaŧon", "redeŧimaŧion", "muniŧion", "ganbon", "opresion", "kapon", "amortixasion", "vaŧinaŧion", "gaɉon", "separaŧion", "exekuŧion", "volpon", "palidon", "promision", "deɉestion", "amortiđaŧion", "exibiŧion", "tradiŧion", "xrenaŧion", "aplikaŧion", "pantalon", "dormijon", "buxaron", "ramaxon", "sfronton", "esterminaŧion", "fakojon", "salutaŧion", "vagon", "prosimaŧion", "ternion", "grandon", "vaxon", "piron", "spropoxiton", "paraŧion", "panŧon", "baketon", "reon", "takon", "sukolon", "konsumaŧion", "kanpion", "spuleđon", "komodaŧion", "sfrontadon", "kapitolaŧion", "remuneraŧion", "dimandon", "kalieron", "kapistasion", "lubion", "pavejon", "partion", "roton", "donxelon", "jestion", "moñon", "spasiđon", "sopresion", "medajon", "evaxion", "senplifegaŧion", "blateron", "putinon", "strijon", "spigaŧon", "banboŧon", "semenaxon", "bolxon", "murelon", "skapuŧon", "Auton", "onŧion", "longon", "fugaŧon", "laxañon", "galioton", "kalaton", "kamaron", "sorđon", "tranguɉon", "diakilon", "inversion", "bon–paston", "donon", "sikuraŧion", "tribolaŧion", "relijon", "fermentaŧion", "gaton", "seguraŧion", "xmañaŧon", "propoxiŧion", "varniŧion", "xberegon", "peceron", "suaxion", "usixion", "forfexon", "xeneralixaŧion", "koreŧion", "xbrodegon", "omaŧon", "perdiŧion", "soportaŧion", "omon", "gaon", "galton", "speŧifegaŧion", "stalfon", "asunŧion", "vejon", "sportelon", "parabolon", "paron", "pension", "koɉon", "stalaŧion", "vereton", "rekoñiŧion", "sfaŧadon", "pancanon", "situaŧion", "skanpon", "desimilaŧion", "peveron", "mencon", "partiŧion", "suporaŧion", "inŧixion", "pindolon", "alboron", "grespon", "presixasion", "relegaŧion", "sasaron", "xgrendenadon", "dekoŧion", "leŧion", "piegaŧion", "garbujon", "ferion", "pernotaŧion", "struđion", "venŧion", "rediron", "valutaŧion", "mosketon", "furbacon", "voxon", "sinŧieraŧion", "ŧaŧaron", "maŧukon", "sensaŧion", "inpediŧion", "fanfuñon", "mortifegaŧion", "bigolon", "solevaŧion", "intimaŧion", "armeron", "molon", "fonŧion", "diaolon", "magon", "spaŧion", "bardason", "kaselon", "mixuraŧion", "kapi–stasion", "sitasion", "tremon", "pionon", "skajon", "maxon", "felpon", "sanson", "seleŧion", "batocon", "navon", "esesion", "fiton", "dexutilon", "grixon", "kaveson", "delubion", "spilon", "mixerion", "rogolon", "ŧenturon", "carlon", "strapaŧon", "soporaŧion", "ravaŧon", "strason", "fafuñon", "stomegon", "skortejon", "stasadon", "eskavaŧion", "exortaŧion", "bexon", "kanacon", "superaŧion", "nitron", "tajon", "notaŧion", "grongolon", "kustion", "angon", "kamuŧon", "kalison", "anexon", "konplesion", "ponaron", "preŧipiton", "bordelon", "suprestiŧion", "inbroɉon", "broketon", "trepudion", "spinadon", "turion", "xenerasion", "piŧigon", "baɉon", "mutaŧion", "siguraŧion", "tortion", "xbardelon", "kanaton", "krapolon", "spigolon", "brespon", "minoraŧion", "trataŧion", "ardijon", "krivelon", "liberaŧion", "espanŧion", "xbiron", "korpon", "sponđon", "nosenton", "pikolon", "skulieron", "marson", "treskon", "fiabon", "sponxon", "porsesion", "unŧion", "vidimaŧion", "faxolon", "bonaŧon", "estraŧion", "rugolon", "vilanon", "xbraɉon", "ñoranton", "ruđenon", "portasperon", "maton", "caron", "xventadon", "denaralidaŧion", "sparnacon", "vergon", "spuncon", "xmanfaron", "porkon", "menestron", "dexlubion", "peliŧon", "rođon", "skorŧon", "kritikon", "redesimasion", "denbraŧion", "selon", "ativaŧion", "velaŧion", "macon", "lojon", "maŧon", "espresion", "galavron", "akoɉimenton", "skovolon", "politegon", "marđokon", "falkon", "marmiton", "kaxon", "skudon", "skafon", "krespon", "farfuɉon", "xbregon", "tesaŧion", "kalmon", "tontolon", "taskon", "frankaŧion", "rafinaŧion", "kagon", "ardejon", "sordon", "panon", "tangaron", "toxon", "pecaron", "sfasadon", "beveron", "gajon", "đupon", "senton", "malagrasion", "mestolon", "skuliaron", "buskaron", "korlikon", "vinon", "aŧeton", "burlon", "cunbon", "roxeton", "strangojon", "rudenon", "frevon", "gorxon", "persuaxion", "dexertaŧion", "kondon", "feveron", "konpoxiŧion", "xlapon", "kuantifegaŧion", "tiron", "soɉeŧion", "deputaŧion", "pevaron", "tarokon", "stangirlon", "putrefaŧion", "polenton", "tokon", "xenarasion", "kucolon", "spernacon", "bon", "revixion", "kogomon", "revoluŧion", "bronbon", "napolon", "ponton", "soxeton", "nuvolon", "fasoleton", "trakanon", "versaŧion", "kanpanelon", "xguŧaron", "đergon", "kanŧelaŧion", "armaron", "kabulon", "birbon", "testaŧion", "mustaŧon", "ministraŧion", "maravejon", "marŧion", "ŧinjon", "peladon", "mustacon", "pavion", "bekon", "viandon", "konvulsion", "feton", "bovolon", "vaon", "sospension", "barakon", "perukon", "sesion", "mostacon", "ganason", "paniđaŧion", "bufon", "xetaŧion", "mincon", "pipion", "edukaŧion", "mostaŧon", "deon", "komedon", "frajon", "reɉilion", "skofon", "kordon", "deneraŧion", "porselon", "arŧon", "finestron", "ŧiexon", "demonion", "sfondradon", "batolon", "bindolon", "akojimenton", "serviŧion", "faŧendon", "kortexanon", "remengon", "tentaŧion", "sexon", "teston", "gornixon", "karbon", "agresion", "trapudion", "lovon", "somision", "piŧegon", "kornixon", "torxion", "perŧeŧion", "kaporion", "filon", "strabukon", "libaraŧion", "kason", "segon", "redenŧion", "melion", "pisigon", "patakon", "goŧon", "sion", "xgalton", "kalŧinaŧion", "ucixion", "gatomamon", "akuixiŧion", "graton", "enbrion", "sturion", "ŧopon", "skarafon", "femenon", "stanŧon", "naranson", "xgrendenon", "monton", "vixion", "brageson", "skueloton", "añon", "legraŧion", "pitokon", "inpianton", "kapurion", "ostinaŧion", "fustegon", "kalsinasion", "goton", "sedolon", "prexenŧion", "stelaŧion", "boñon", "rovijon", "kaxaton", "ŧeston", "prevenŧion", "oblaŧion", "skagaŧon", "saltalion", "masukon", "pinŧon", "fiakon", "baldon", "xbarbatolon", "ordenaŧion", "boason", "turkinon", "timon", "tartanon", "strepiton", "feston", "tronbon", "skarselon", "piegoron", "scavon", "bolson", "saŧon", "geridon", "vixdekason", "skriŧion", "xbaxucon", "suparaŧion", "dragon", "puñaton", "torion", "kavaŧon", "miserion", "pastiŧon", "sapienton", "đenaraliđaŧion", "xbadaɉon", "tolpon", "manestron", "koverton", "potifon", "tavolon", "trobolaŧion", "balon", "tanfurlon", "stolidon", "kapoton", "sojeŧion", "pastrocon", "pandolon", "relaŧion", "fisaŧion", "skoson", "kaldiron", "puton", "porton", "skorabion", "lardon", "revolton", "simulaŧion", "xbrufon", "tripudion", "eŧeŧion", "skorlon", "invenŧion", "đanbon", "komunion", "veleton", "secon", "raŧion", "inbandixon", "koton", "restrinŧion", "portaŧenjon", "panson", "baukon", "morteron", "nudriŧion", "piñoraŧion", "stangerlon", "galantomenon", "infiaxion", "seon", "ladaron", "eleŧion", "kaldiaron", "anton", "koston", "skaveson", "kapocon", "karnavalon", "ŧoton", "pariŧion", "orñon", "roketon", "kolaŧion", "palpitaŧion", "pendolon", "rekuidiŧion", "sodiŧion", "interogaŧion", "deneralidaŧion", "tendon", "janpikon", "fiolon", "solfon", "sarvaŧion", "strukalimon", "bailon", "kapo–stasion", "polveron", "manutenŧion", "materialon", "vrespon", "sasion", "bevaron", "rađiron", "kapion", "ŧotiñon", "tanon", "scapinon", "ŧefon", "furegon", "đibaldon", "imaɉinaŧion", "đofranon", "destrigon", "murlon", "parteŧipaŧion", "fraŧion", "xbajon", "tartajon", "ŧiatilion", "ondaŧion", "spetoraŧion", "gorđon", "torefaŧion", "kanselasion", "estension", "subversion", "poxiŧion", "beŧon", "ostentaŧion", "korniđon", "inkatijon", "soxeŧion", "dexun", "ñisun", "ñaun", "brun", "nesun", "ñesun", "oñun", "kalkun", "nisun", "brunbrun", "negun", "tribun", "nigun", "koalkedun", "ŧaskun", "kualkun", "dedun", "Utun", "alkun", "kualkedun", "Autun", "kalkedun", "komun", "deđun", "koalkun", "Otun", "algun", "ŧaskadun", "strakul", "miul", "trul", "strul", "spigarul", "albul", "stra-kul", "ingrasamul", "tirakul", "batikul", "ronpikul", "furbikul", "baul", "bul", "forbikul", "ponxikul", "meul", "stropakul", "kojonŧèl", "xbarbatèl", "ranaròl", "soriòl", "ovaròl", "seconŧèl", "tenpexèl", "kastañòl", "karagòl", "liŧaròl", "masèl", "braŧaròl", "ŧigaròl", "redexèl", "albòl", "bertevèl", "kokajòl", "barbisòl", "flajèl", "ruxiòl", "skodaròl", "krudèl", "tastaròl", "monexèl", "pisaròl", "lionŧèl", "garxonsèl", "koɉonŧèl", "miedaròl", "armakòl", "fainèl", "buèl", "kaxonŧèl", "bativèl", "faxiòl", "lađaròl", "artedanèl", "tinasòl", "lusatèl", "ventexèl", "gostaròl", "libaròl", "kuintèl", "bañòl", "formentèl", "novèl", "faxòl", "ŧendraròl", "menoèl", "ganbèl", "kavriòl", "fosatèl", "dakòl", "bocòl", "fornèl", "brikonŧèl", "kanpañòl", "barbuŧòl", "poridòl", "porsèl", "manevèl", "bexaròl", "đemèl", "bardevèl", "kroñòl", "borèl", "galiotèl", "korparòl", "santarèl", "boriñòl", "toajòl", "oridèl", "osakòl", "pursinèl", "barixèl", "timonsèl", "menedèl", "kontarèl", "burcèl", "fogerèl", "raixèl", "roviɉòl", "gomisièl", "filŧòl", "arditèl", "bindèl", "bronbijòl", "kartèl", "korexiòl", "buratèl", "rièl", "kaveŧòl", "ŧarlatanèl", "bolsanèl", "grajotèl", "kortexanèl", "pasarèl", "valonsèl", "stanpatèl", "staterèl", "palasòl", "fatarèl", "beveraròl", "apèl", "kuariŧèl", "rovejòl", "borsaròl", "kavièl", "rudiòl", "bardòl", "mexadèl", "ranpegaròl", "antianèl", "arteđanèl", "ladronŧèl", "korñòl", "tornèl", "trataròl", "intianèl", "solexèl", "maŧaròl", "gransiòl", "gatèl", "penaròl", "đoɉèl", "alberèl", "arxonèl", "boŧèl", "ruviɉòl", "figadèl", "kasèl", "sportaròl", "bajardèl", "infogadèl", "kovièl", "kaecòl", "linaròl", "bastonŧèl", "paŧarèl", "kortèl", "kaŧonèl", "violonŧèl", "fraxèl", "periòl", "piòl", "xgabèl", "luŧatèl", "sonaɉòl", "biviòl", "soldadèl", "furtarèl", "barđòl", "dindiotèl", "baɉardèl", "rovijòl", "krivèl", "purŧinèl", "anèl", "fondèl", "korniòl", "tariòl", "poŧòl", "pontexèl", "faganèl", "franguèl", "oropèl", "andiòl", "masòl", "spianŧaròl", "putatèl", "karetèl", "picarèl", "kapuŧòl", "fidèl", "vexòl", "sigaròl", "bordèl", "xɉoŧaròl", "moèl", "xbardavèl", "salaròl", "gavinèl", "marŧèl", "morèl", "kolomèl", "risòl", "banbinèl", "brasadèl", "luxaròl", "ròl", "putèl", "mantexèl", "fumaròl", "orđiòl", "granđiòl", "skartabèl", "đanbèl", "solaròl", "fijòl", "kantonŧèl", "kokoñèl", "sorđaròl", "borsèl", "matuxèl", "menuèl", "ŧestèl", "xolariòl", "piegadèl", "tovaɉòl", "sexèl", "đenocèl", "baronŧèl", "purisinèl", "tondèl", "saltarèl", "ferasòl", "viñaròl", "bruɉèl", "agostaròl", "đovèl", "gardòl", "sordaròl", "kanpièl", "terŧòl", "takakapèl", "bokonŧèl", "meɉaròl", "skansèl", "konterèl", "brokadèl", "pontèl", "xguaɉatèl", "skopèl", "sponsòl", "pòl", "gonfiadèl", "detreganiòl", "rixaròl", "roveɉòl", "solŧaròl", "balkonsèl", "xɉosaròl", "ardarèl", "viŧinèl", "vovaròl", "sparsèl", "rosiñòl", "kavicòl", "vetriòl", "mariòl", "pajòl", "pođòl", "kanèl", "bemòl", "lekapestèl", "kusinèl", "martèl", "statarèl", "codèl", "kaŧòl", "orpèl", "kordaròl", "braŧadèl", "fiumexèl", "kamixòl", "linbèl", "kokaɉòl", "stentaròl", "lovastrèl", "pediŧèl", "spondòl", "viscaròl", "lixèl", "biskotèl", "brandèl", "paɉaròl", "somarèl", "skapusiòl", "ŧestaròl", "fondaròl", "karòl", "trabukèl", "ŧarvèl", "torèl", "remendèl", "ganxaròl", "bisinèl", "kainèl", "rejotèl", "forsèl", "ordiòl", "guardòl", "batèl", "kagonŧèl", "poriđòl", "jaŧòl", "sedèl", "xataròl", "burèl", "spiansaròl", "kanpañaròl", "guxaròl", "turkèl", "bixinèl", "kairòl", "panexèl", "polesòl", "burlèl", "fiabaròl", "kolaròl", "carèl", "rigabèl", "faɉòl", "boaròl", "livèl", "korixiòl", "sfasèl", "stra-bèl", "faŧòl", "penonŧèl", "mièl", "molexèl", "restèl", "tovajòl", "ŧaratanèl", "fanèl", "sonèl", "bagèl", "kiɉaròl", "orxiòl", "rebèl", "skondaròl", "kapriòl", "kavedèl", "kuadrèl", "semensaròl", "intrigadèl", "storđikòl", "stuèl", "primaròl", "xmardèl", "grataròl", "veèl", "ganŧaròl", "seđèl", "terŧaròl", "sukòl", "soranèl", "kanevaròl", "menevèl", "piñatèl", "vecexòl", "đornaròl", "gardèl", "porixòl", "vedriòl", "betinèl", "dragonŧèl", "kanpanèl", "artexanèl", "moskatèl", "ruvijòl", "niŧiòl", "rapanèl", "bekiñòl", "linsòl", "ninsiòl", "martarèl", "subiòl", "spigaròl", "kanòl", "niŧòl", "korbatèl", "osokòl", "bigorèl", "poxòl", "brujèl", "lisaròl", "fapèl", "barbusòl", "merdaròl", "bonèl", "montexèl", "bisterèl", "rufiòl", "sediòl", "pontaròl", "ŧivèl", "gropèl", "scoparòl", "oripèl", "panaròl", "indovinèl", "konsapèl", "bugaròl", "medadèl", "majòl", "stentarèl", "garbèl", "peñaròl", "torsèl", "grinxòl", "xjosaròl", "arxarèl", "mantèl", "kapitèl", "gròl", "bertovèl", "menarèl", "noxèl", "ninŧòl", "minuèl", "kontaròl", "listèl", "panetèl", "brasaròl", "bokaròl", "barxòl", "vanarèl", "durèl", "stiŧaròl", "lionsèl", "brikonsèl", "ardonèl", "kojonsèl", "barbiŧòl", "brasiòl", "trapèl", "palaŧòl", "oxèl", "portakapèl", "remondèl", "ponaròl", "maŧèl", "ŧixèl", "brentèl", "karoxèl", "ponteròl", "peterèl", "garxonŧèl", "koɉonsèl", "kaxonsèl", "tasèl", "pòrta–kapèl", "paròl", "skarpèl", "fasinèl", "tinèl", "bulièl", "kanpanièl", "tornexèl", "bronbeɉòl", "marèl", "maŧaporŧèl", "kosinèl", "bustarèl", "pesaròl", "bigarèl", "faŧiòl", "albuòl", "rafiòl", "piatèl", "bèl", "beđaròl", "barkaròl", "raganèl", "paɉòl", "supiaròl", "međadèl", "vedèl", "filixèl", "bestiòl", "seconsèl", "skarkaɉòl", "ucaròl", "kanaròl", "peŧarèl", "ruđiòl", "konastrèl", "kojonèl", "tajòl", "finestrèl", "lenŧiòl", "kuarisèl", "timonŧèl", "laxaròl", "ravanèl", "arđonèl", "perpetuèl", "ŧexendèl", "kavesòl", "kankarèl", "rokèl", "dentèl", "referatèl", "bronbiòl", "buricinèl", "biavaròl", "fièl", "manipòl", "ŧernieròl", "cavaròl", "storxikòl", "ŧimaròl", "fajòl", "baketèl", "boskaròl", "besaròl", "kasiòl", "ŧervèl", "đirèl", "tinaŧòl", "karaguòl", "mapèl", "batibèl", "spañòl", "ŧenocèl", "sponđòl", "marexèl", "kastèl", "sanbuèl", "reditaròl", "kuajaròl", "podòl", "frutaròl", "barbastèl", "remandèl", "xbrindèl", "deèl", "piasaròl", "ladronsèl", "pradaròl", "filivèl", "akuarèl", "skabèl", "salbanèl", "tersaròl", "priòl", "bikiñòl", "sorxaròl", "penèl", "pesarèl", "filexèl", "muxaròl", "riganèl", "peŧòl", "fagotèl", "kokonèl", "añèl", "strasaròl", "kavrinòl", "bulèl", "bastonsèl", "buxnèl", "modèl", "bartavèl", "buɉòl", "ŧercèl", "ñèl", "feraròl", "skuderòl", "skavesakòl", "polaròl", "montèl", "orxòl", "recotèl", "rasaròl", "beŧaròl", "tristarèl", "xbrindakòl", "posòl", "trabokèl", "bosèl", "puriŧinèl", "forkamèl", "lumaròl", "nixèl", "stornèl", "ninŧiòl", "piaŧaròl", "đinbèl", "rusèl", "ocèl", "palixèl", "sponxòl", "konostrèl", "cavexèl", "menestròl", "rimandèl", "maŧòl", "violonsèl", "xenocèl", "pesatèl", "straŧaròl", "gandaròl", "masaròl", "trivèl", "kalkañòl", "pajaròl", "gucaròl", "fraèl", "porŧèl", "ruxiñòl", "fiòl", "baronsèl", "kapusòl", "rantegèl", "marsèl", "lensiòl", "maɉòl", "ŧokatèl", "farinaròl", "ŧexèl", "saldaròl", "kanarèl", "riŧòl", "mastèl", "portèl", "mokaròl", "karamèl", "bekonèl", "kantonsèl", "xnèl", "guèl", "strabèl", "solsaròl", "ŧerlatanèl", "akuaròl", "kolmèl", "filandèl", "kapèl", "kunèl", "skañèl", "xugèl", "garđòl", "kolonèl", "latexiòl", "rededèl", "gratarèl", "kanapiòl", "pestaròl", "kuarèl", "bolsonèl", "vedovèl", "armaròl", "priastèl", "feraŧòl", "krièl", "đimèl", "kaxaròl", "ortexèl", "tritèl", "ordòl", "braŧiòl", "filièl", "bufonèl", "secèl", "kalastrèl", "bakariòl", "boxèl", "vixinèl", "bavaròl", "bokonsèl", "skanŧèl", "skarkajòl", "mejaròl", "fuxèl", "tersòl", "balkonŧèl", "bròl", "skonbraròl", "skòl", "bañaròl", "kananiòl", "bronbejòl", "matonèl", "fradèl", "xbrefèl", "draparòl", "batarèl", "mañaputèl", "radixèl", "sponŧòl", "trufèl", "patèl", "veriòl", "martorèl", "kasòl", "pijakaragòl", "kasonèl", "buranèl", "fasiòl", "toxatèl", "đontaròl", "ŧeraròl", "cijaròl", "ganđaròl", "bastardèl", "frèl", "sixèl", "sturiòl", "masaporsèl", "bolŧonèl", "vedoèl", "flaɉèl", "kogòl", "tinpanèl", "navaròl", "denocèl", "erbaròl", "redeđèl", "pedisèl", "karuxèl", "skaveŧakòl", "polastrèl", "stañòl", "spasèl", "telaròl", "kamèl", "bixatèl", "pexaròl", "manganèl", "semenŧaròl", "saltèl", "forŧèl", "taɉòl", "siòl", "guarnèl", "medòl", "kagonsèl", "sorakòl", "grandiòl", "rostèl", "dovèl", "revendaròl", "infiadèl", "kadenèl", "bronbiɉòl", "garnèl", "jasòl", "skueraròl", "daòl", "sfrasèl", "artaròl", "spinèl", "kavestrèl", "poleŧòl", "tabarèl", "mòl", "travexèl", "xguajatèl", "granèl", "kruèl", "fardèl", "montixòl", "garangèl", "storiòl", "sfaŧèl", "sapientèl", "vesinèl", "buxinèl", "lataròl", "peloxèl", "vasèl", "codaròl", "roxiñòl", "konŧapèl", "pekòl", "đojèl", "pòrta–mantèl", "gavitèl", "dragonsèl", "garxòl", "barexèl", "fanfarièl", "fasòl", "kijaròl", "paganèl", "penonsèl", "sonajòl", "granxiòl", "kaniòl", "bigòl", "budèl", "coaròl", "bokardèl", "kuartaròl", "gataròl", "stradaròl", "kañòl", "gansaròl", "rusiñòl", "skankòl", "ŧukòl", "tirafòl", "dukatèl", "visinèl", "stisaròl", "karavèl", "salbrunèl", "kòl", "rubaròl", "pividèl", "koresòl", "portamantèl", "karièl", "tirèl", "karakòl", "nisiòl", "linŧòl", "tornakòl", "konkòl", "gaitèl", "filèl", "gabanèl", "koɉonèl", "altarèl", "bedaròl", "betarèl", "kormèl", "arđarèl", "trinèl", "piñòl", "basaròl", "manuèl", "albarèl", "bevaròl", "skirèl", "drapèl", "orđòl", "bruñòl", "asexèl", "spinièl", "koruxiòl", "ronpikòl", "kanestrèl", "fròl", "bertoèl", "panèl", "peŧaròl", "petèl", "pipistrèl", "tramandèl", "kamòl", "pandòl", "matarèl", "xjoŧaròl", "ninsòl", "graɉotèl", "reɉotèl", "kaŧiòl", "karatèl", "molinèl", "pegoraròl", "stordikòl", "libèl", "bujòl", "ladaròl", "xovèl", "nisòl", "axenèl", "fiɉòl", "kaxèl", "murèl", "mestèl", "lavèl", "ŧopèl", "barbarinèl", "ŧinbanèl", "spinarèl", "bruxapel", "fiel", "kavakavel", "kontropel", "kaveel", "pomel", "ŧiel", "kontrapel", "pexel", "kaostel", "strapel", "kaviel", "kavel", "petel", "skol", "pisarol", "skodarol", "sotarol", "rovejol", "garxiñol", "solŧarol", "rixarol", "ucarol", "spondol", "vovarol", "boarol", "roveɉol", "rosiñol", "fisol", "ovarol", "xjoŧarol", "ranarol", "pođol", "bavarol", "fiŧol", "libarol", "storol", "spiansarol", "kaxarol", "bronbejol", "skapuŧiol", "korexol", "saldarol", "kordarol", "beđarol", "tastarol", "kamixol", "navarol", "ponterol", "erbarol", "faxiol", "xɉosarol", "stentarol", "kanarol", "skapusiol", "supiarol", "biavarol", "fondarol", "parol", "liŧarol", "letexol", "bruxarol", "bronbeɉol", "sordarol", "garofol", "faŧiol", "faxol", "braŧarol", "fasiol", "ŧendrarol", "pesarol", "tersol", "terŧol", "bañarol", "kroxol", "bisol", "rafiol", "kuajarol", "laudevol", "porixol", "viscarol", "sturiol", "barkarol", "kolarol", "grandiol", "vedol", "cavarol", "laxarol", "revendarol", "pipirol", "kivol", "besarol", "rusiñol", "ganxarol", "korparol", "telarol", "garđiñol", "bexarol", "karol", "tersarol", "đontarol", "studiol", "semenŧarol", "tajol", "koređol", "cijarol", "pexarol", "đornarol", "ranpegarol", "reditarol", "beverarol", "skuerarol", "podol", "picol", "portarol", "garŧiñol", "ponarol", "piasarol", "bronbijol", "montixol", "frutarol", "pradarol", "fiabarol", "draparol", "ŧernierol", "latarol", "tratarol", "kuartarol", "miedarol", "gaxol", "artarol", "boskarol", "poriđol", "balanŧiol", "paɉarol", "pontirol", "bronbiɉol", "sponđol", "introl", "xatarol", "kairol", "sponxol", "coarol", "gardiñol", "parasol", "kanapiol", "kanpañarol", "skuderol", "kanevarol", "guxarol", "borsarol", "rubarol", "skodirol", "primarol", "gatarol", "bokarol", "venol", "seseɉol", "semensarol", "ninsiol", "gratarol", "stiŧarol", "feraŧol", "ganŧarol", "ninŧiol", "pesol", "strasarol", "ninŧol", "lumarol", "penarol", "pekol", "ferasol", "tirafol", "ninsol", "ŧimarol", "veđol", "kiɉarol", "sorđarol", "peŧol", "lodevol", "jarol", "sorxarol", "beŧarol", "figaŧol", "piaŧarol", "garsiñol", "dentarol", "skondarol", "rofiol", "koredol", "spianŧarol", "figasol", "linarol", "niŧiol", "nisiol", "roxiñol", "gansarol", "ruxiñol", "terŧarol", "stradarol", "ŧestarol", "codarol", "ferarol", "sesejol", "gucarol", "sportarol", "spigarol", "merdarol", "rasarol", "koresol", "pegorarol", "ŧerarol", "rufiol", "boŧol", "ladarol", "scoparol", "panarol", "poridol", "brasarol", "granđiol", "masarol", "kanol", "pajarol", "straŧarol", "akuarol", "poxol", "niŧol", "speriol", "đirasol", "stisarol", "xɉoŧarol", "ganđarol", "lisarol", "broñol", "bosol", "salarol", "latexiol", "pestarol", "balansiol", "skonbrarol", "orxol", "basarol", "bevarol", "nisol", "luxarol", "kontarol", "fiol", "pontarol", "farinarol", "agostarol", "lađarol", "viñarol", "mañarol", "mokarol", "armarol", "polarol", "fumarol", "maŧarol", "bugarol", "peŧarol", "bedarol", "verlol", "pixol", "gandarol", "vexol", "muxarol", "xjosarol", "peñarol", "kijarol", "solsarol", "kapusal", "parameđal", "anoal", "naŧional", "bal", "perpetual", "trionfal", "sokorsal", "masakaval", "infernal", "dental", "megal", "kaval", "lonđitudenal", "spisial", "substanŧial", "personal", "lidial", "aspetual", "làvar–dental", "kurial", "kapuŧal", "prepoxiŧional", "skinal", "sotogrondal", "malugual", "kauxal", "siñal", "deal", "piasal", "logativa–direŧional", "dixial", "strajudisial", "manual", "tribunal", "stamenal", "pontekanal", "komunal", "logativo–direŧional", "matrikal", "kriminal", "piaŧal", "vesal", "muxegal", "deŧimal", "averbial", "sostanŧial", "autonal", "veŧal", "oriđenal", "labial", "gronbial", "ospedal", "frontal", "saondal", "piantal", "sagramental", "kontestual", "dexegual", "estrajudisial", "komersal", "modal", "bakanal", "sonđal", "komerŧal", "verdoxal", "maregal", "inpal", "sojal", "internasional", "final", "ibernal", "vental", "amoral", "orixenal", "internaŧional", "ofesial", "solkal", "ocal", "subafitual", "pioval", "bilabial", "nabukal", "kaotonal", "prinsipal", "artifisial", "kuintal", "luminal", "badial", "inlegal", "material", "ordenal", "agual", "sforsanal", "karamal", "moral", "ŧerforal", "soŧial", "prepoxisional", "batipal", "bati–pal", "nasional", "fraimal", "metal", "oriental", "korporal", "stival", "boral", "ugual", "tenporal", "skosal", "prexidenŧial", "inbal", "anal", "konfesional", "teritorial", "subtropegal", "pedestal", "kantonal", "diđial", "vertegal", "meridional", "petoral", "vidal", "faval", "pontual", "kaveal", "fonŧional", "speal", "sonxal", "penal", "làvaro–dental", "argomental", "presidenŧial", "femenal", "mesal", "londitudenal", "brunal", "reversal", "logativo–diresional", "ŧinɉal", "soŧal", "prinŧipal", "đudiŧial", "dimisorial", "brasal", "estraɉudiŧial", "speŧial", "grinbial", "rumegal", "sforŧanal", "gardinal", "gramadegal", "braŧal", "predial", "saonxal", "estrajudiŧial", "ŧenjal", "retikal", "noal", "vixal", "fitual", "fonsional", "gual", "real", "maŧakaval", "pertegal", "sponsal", "seraval", "postal", "verdodal", "onbrinal", "piantanemal", "ufisial", "vetural", "kavaŧal", "dorsal", "kultural", "kavastival", "serviŧial", "đornal", "xornal", "mental", "regal", "setemanal", "kavasal", "total", "inŧidental", "universal", "utunal", "sureal", "nomenal", "fraxal", "barbusal", "ofeŧial", "didial", "kristal", "tirastival", "ganbal", "barbuŧal", "iniŧial", "ŧentral", "đeneral", "kardenal", "aɉetival", "kanal", "dukal", "bordenal", "xmerdagal", "presidial", "afitual", "somensal", "koral", "kornal", "ajetival", "vial", "naxal", "verdođal", "gardenal", "sondal", "nadural", "ferial", "ufiŧial", "kaporal", "autunal", "animal", "destretual", "stromental", "arsanal", "grondal", "orixinal", "sostansial", "xendal", "substansial", "bordonal", "inperial", "nominal", "paramexal", "parangal", "somenŧal", "lial", "saonđal", "stal", "strajudiŧial", "nemal", "dornal", "kavedal", "sosal", "pivial", "lateral", "karneval", "skarkaval", "invernal", "lengual", "skartakaval", "lesegal", "kondisional", "vernegal", "kopal", "abitual", "referensial", "kondiŧional", "verbal", "estraɉudisial", "ofisial", "menal", "dexial", "oridontal", "farnimal", "skenal", "sirkostansial", "pòrta–penal", "ilegal", "egual", "plural", "straɉudiŧial", "inisial", "koronal", "nadal", "manoal", "baoral", "vokal", "minimal", "fardimal", "puñal", "kanocal", "insial", "soɉal", "lonxitudenal", "feminal", "numeral", "memorial", "dedal", "mortal", "servisial", "normal", "skritural", "palatal", "insidental", "baxoal", "stradal", "solidal", "aramal", "pontifegal", "gramal", "bestial", "traval", "spesial", "babal", "sperimental", "senal", "pòrta–mesal", "interinal", "presidensial", "traversal", "superfiŧial", "baroal", "xguinsal", "natural", "xudisial", "karnaval", "existensial", "feral", "xguinŧal", "grenbial", "konvensional", "ospeal", "tinbal", "orixontal", "portegal", "doxenal", "portapenal", "mural", "anemal", "referenŧial", "unilateral", "vakasal", "vakaŧal", "existenŧial", "plateal", "kavesal", "lokal", "semi–lateral", "madregal", "engual", "portamesal", "dudiŧial", "setimanal", "ŧinjal", "kaveŧal", "dial", "ŧendal", "fortunal", "papagal", "paramedal", "kapital", "modegal", "logativa–diresional", "patual", "portogal", "uxual", "kanbial", "ŧenɉal", "noval", "skarta–kaval", "xguansal", "ŧirkostanŧial", "orinal", "pòst–àlveo–palatal", "katredal", "frimal", "konvenŧional", "pontal", "bankal", "krestal", "individual", "sakramental", "konetral", "desimal", "ofiŧial", "dexeal", "fanal", "xguanŧal", "arsenal", "prexidensial", "orial", "grenal", "kuartal", "mantexenal", "minal", "semi–naxal", "señal", "distretual", "fiskal", "spiŧial", "sosial", "oridenal", "pronomenal", "straɉudisial", "superfisial", "xlibral", "oriđontal", "fondamental", "aŧal", "kordial", "kokal", "ministerial", "otunal", "inŧial", "parsonal", "bokal", "asal", "meal", "dotoral", "viñal", "rival", "malsan", "tulipan", "bonvivan", "pastran", "borgexan", "đoldan", "baɉan", "segrestan", "deretan", "pisakan", "bresan", "tajan", "vineŧian", "taja–pan", "vinesian", "ŧarabaldan", "nostran", "mexan", "taɉapan", "itaɉan", "maran", "katalan", "lotregan", "turbian", "bekaran", "trivixan", "ragan", "bigaran", "pantegan", "arlan", "patan", "romitan", "skalŧakan", "skrivan", "kavian", "trevixan", "bran", "marabolan", "padan", "međan", "vinisian", "kavarđaran", "marostegan", "brixan", "bragolan", "palandran", "tajapan", "viniŧian", "pacan", "ŧanbelan", "kuran", "balŧan", "ingan", "barakan", "balsan", "anbrakan", "ŧerlatan", "itajan", "lateran", "medolan", "eraklean", "barbasan", "sopran", "sakrestan", "barbakan", "barbaŧan", "padovan", "roskan", "pojapian", "malsakan", "pulitan", "caran", "violipan", "bakan", "mundan", "skan", "venesian", "rodolan", "ŧapa–pian", "vilan", "detregan", "drian", "rabikan", "kordovan", "antian", "famalan", "ostregan", "maturlan", "piovexan", "soran", "barbajan", "kavardaran", "takapan", "vulkan", "sovran", "medan", "pikapan", "piovan", "polikan", "borean", "kastelan", "baban", "surian", "piovegan", "pesekan", "kristian", "tralian", "bajan", "uragan", "lontan", "urban", "anpesan", "barbaɉan", "taɉa–pan", "anpeŧan", "anglekan", "degan", "anŧian", "arteđan", "marobolan", "tavan", "mañapan", "valexan", "lavran", "bastran", "pagan", "morian", "languisan", "ŧapapian", "tiran", "roan", "detragan", "pedan", "agortan", "carabaldan", "katapan", "talian", "gaban", "tandan", "kaopian", "paexan", "solan", "carlan", "afan", "montan", "graexan", "domenikan", "furian", "grosolan", "pantan", "pelikan", "liban", "artexan", "ŧinban", "međolan", "ledan", "burlan", "anglo–meregan", "guardian", "xletran", "kafetan", "mataran", "kapitan", "marŧapan", "intian", "altopian", "ansian", "indian", "altivolan", "furlan", "ixolan", "mexolan", "agostan", "arkan", "malxapan", "paltan", "konplean", "peskapian", "bevan", "karantan", "parocan", "ŧarlatan", "bean", "molnan", "parabolan", "galan", "bagan", "malan", "polexan", "bon–vivan", "ŧaratan", "spiantan", "kapotan", "dulipan", "forean", "artedan", "pepian", "rafakan", "tanburlan", "masakan", "veneŧian", "portolan", "krestian", "faxan", "arŧaran", "skalsakan", "toskan", "torexan", "gabian", "arsaran", "ortolan", "pavan", "kortexan", "divan", "padoan", "kastrakan", "axolan", "marsapan", "maŧakan", "kan", "rufian", "barban", "kavarxaran", "antan", "prusian", "matapan", "italian", "korean", "vigan", "sotodegan", "mean", "pedemontan", "poɉapian", "ostegan");
		List<String> originalLines = words.stream()
			.map(word -> word + "/" + flag)
			.collect(Collectors.toList());
		List<LineEntry> originalRules = originalLines.stream()
			.map(line -> wordGenerator.applyAffixRules(line))
			.flatMap(productions -> reducer.collectProductionsByFlag(productions, flag, AffixEntry.Type.SUFFIX).stream())
			.collect(Collectors.toList());
		List<LineEntry> compactedRules = reducer.reduceRules(originalRules);

		List<LineEntry> expectedCompactedRules = Arrays.asList(
			new LineEntry("en", "éi", "en", Arrays.asList("ultrateren", "guardamagađen", "seren", "baxen", "međen", "taja–fen", "magaxen", "teren", "saraŧen", "repien", "meden", "similituden", "sen", "stra-ben", "magađen", "mexen", "maxagen", "baŧen", "velen", "straben", "basen", "guardamagaxen", "taɉafen", "terapien", "taɉa–fen", "tajafen", "magaden", "notaben", "tientinben", "guardamagaden", "stra-pien", "strapien")),
			new LineEntry("on", "ói", "on", Arrays.asList("remoŧion", "autorixasion", "radaŧion", "filtraŧion", "tinason", "xlovon", "paragon", "faraon", "manaŧon", "inrotulaŧion", "truson", "sonorixaŧion", "frustadon", "kalandron", "ŧavaton", "malision", "groon", "emision", "peaŧon", "arđaron", "sifon", "xɉonfon", "bandieron", "boladon", "goldon", "lion", "ubigaŧion", "folon", "gucon", "kotolon", "moreton", "tormenton", "garlon", "piavolon", "previxion", "ventron", "greson", "salvaŧion", "milion", "legaŧion", "brustolon", "guidon", "bagajon", "pekatoron", "dopion", "velion", "sudiŧion", "ventolon", "sinserasion", "galidaŧion", "erbion", "xustapoxiŧion", "darion", "rođeton", "braŧon", "xganbeton", "stafon", "union", "skuakueron", "litigon", "arđiñon", "baston", "tavaron", "alberon", "boton", "kospeton", "torŧon", "torcon", "proporŧion", "bregieron", "xganberlon", "xguaɉaton", "sporton", "konmixeraŧion", "lokuŧion", "kaveŧon", "lanŧon", "bardon", "retrocesion", "saon", "balkon", "strapason", "ruđaron", "ŧinbanon", "koređon", "xlapaŧon", "ruxenon", "traxlokaŧion", "tripon", "axenon", "skison", "marangon", "santon", "konpañon", "prexupoxiŧion", "pòrta–speron", "prexion", "krepon", "omenon", "pòrta–ŧenjon", "ronkon", "rataparaŧion", "tortijon", "vendiŧion", "tarlixon", "rebelion", "praxon", "iluminaŧion", "xdenton", "skorexon", "konserton", "sipiton", "danaŧion", "reŧixion", "dromon", "gropon", "rejeŧion", "dormeɉon", "tonon", "poltron", "kokalon", "pilon", "strukaŧion", "parangon", "teñon", "fifon", "beaton", "pisegon", "polmon", "palason", "xeneralixasion", "straxordenaŧion", "vermilion", "stupinon", "tugon", "malvarsaŧion", "destinŧion", "ŧetaŧion", "xbesolon", "kuajon", "destraton", "niolon", "gabion", "superstiŧion", "revendon", "destinaŧion", "sovenŧion", "gravion", "elixion", "konstituŧion", "bianketon", "skopaŧon", "redacon", "ŧaltron", "trabukon", "insinuaŧion", "fiankon", "leon", "rekuixiŧion", "talpon", "kostituŧion", "poron", "kostriŧion", "suŧeŧion", "xbadajon", "xlubion", "presepiton", "frankon", "tonbolon", "dreton", "kavalon", "oraŧion", "arpion", "ruxon", "moniŧion", "sekamincon", "rekluxion", "inbrojon", "mason", "xmergon", "puliton", "konŧon", "lapidon", "torbolaŧion", "citinon", "oparaŧion", "finton", "kornidon", "batajon", "napolion", "sotoskriŧion", "nomenaŧion", "dormiɉon", "boxon", "dexvion", "malfaŧion", "bragon", "vanedon", "bokon", "susesion", "eskorporaŧion", "ponson", "xbaɉon", "portaboŧon", "stangon", "ŧedron", "spilungon", "dukadon", "otaŧion", "faeton", "xbaɉafon", "đeneraliđaŧion", "palkon", "bibaron", "kartelon", "mision", "kokon", "traŧion", "spesifegasion", "artikolaŧion", "sastufaŧion", "speron", "ultimaŧion", "ŧiton", "raprexaŧion", "ŧurlon", "spaŧeton", "xjonfon", "mormoraŧion", "saxon", "naturaliđaŧion", "trangujon", "vokaliđaŧion", "kalabron", "provixion", "inkatiɉon", "mastelon", "traverson", "đafranon", "paston", "pelaton", "ridon", "marxokon", "potacon", "traversaŧion", "skaton", "pegoron", "xbolsegon", "bonbon", "vokalixaŧion", "xvejadon", "skuakuaron", "vasinasion", "koniugaŧion", "autoridaŧion", "kogolon", "dormicon", "plafon", "mañamaron", "pruŧision", "konfesion", "gobon", "sason", "ronpon", "aseton", "kavion", "sistemaŧion", "spuacon", "supuraŧion", "ruson", "duron", "santilion", "bonpaston", "spondon", "điridon", "salarion", "xveɉadon", "gardon", "naturalixasion", "stramaŧon", "baxon", "espurgaŧion", "privaŧion", "diskreŧion", "morelon", "sutilon", "desturbon", "identifegaŧion", "sinsierasion", "fenestron", "frugon", "tortejon", "makaron", "konkruxion", "papaŧon", "xguseton", "estermenaŧion", "postijon", "kostipaŧion", "arson", "đonŧion", "dromeɉon", "flesion", "lanpion", "bagaron", "sostituŧion", "afliŧion", "meskolon", "modejon", "xbefon", "panidaŧion", "bagon", "karnaxon", "peñoraŧion", "bonton", "sonoriđaŧion", "cucon", "korexon", "bison", "buxion", "xenaralixaŧion", "manŧipaŧion", "fanfruñon", "mostron", "skitoŧon", "pòrta–boŧon", "maneŧon", "serolon", "kareton", "balordon", "skoređon", "tension", "strakolaŧion", "putelon", "domandon", "fosadon", "ranpiñon", "struxon", "piŧon", "infiaxon", "ŧeɉon", "formaŧion", "budaron", "legalidaŧion", "patalon", "radon", "partisipasion", "dejestion", "ŧixon", "kuadron", "dexluvion", "vixitaŧion", "cetaŧion", "pinson", "kavelon", "sureŧion", "scafon", "grason", "partiŧipaŧion", "skalinon", "grixolon", "moskaton", "likon", "xganbirlon", "đirandolon", "maskaron", "spuledon", "pianton", "porŧesion", "piton", "rubon", "berikinon", "spasidon", "kareŧon", "dondelon", "varsaŧion", "proŧesion", "rekuiđiŧion", "đipon", "skarpaŧon", "pikandolon", "vestiŧion", "oson", "staŧadon", "indikaŧion", "barnaboton", "realiđaŧion", "vidon", "kaveon", "xbravaŧon", "speraŧion", "savarion", "asimilaŧion", "balanŧon", "konfuxion", "maskalŧon", "pertegon", "kufolon", "kostion", "ruvijon", "setasion", "insolenton", "taolon", "polakon", "kaŧaxon", "konfalon", "palatiđaŧion", "porŧion", "xbrasaron", "spontiñon", "terminaŧion", "gruñon", "kuistion", "revokaŧion", "ŧenjon", "đabajon", "botiron", "vibraŧion", "divixion", "kordolon", "kokolon", "rexiron", "burcon", "đogatolon", "markanton", "presipiton", "mexuraŧion", "falŧon", "kavadon", "tranxaŧion", "aprension", "skoŧon", "visiadon", "brikon", "sialakuon", "stajon", "malagraŧion", "piatolon", "rasionasion", "batialon", "tirinton", "ridacon", "friŧion", "pelon", "straŧion", "xbroeton", "faton", "perlustraŧion", "furon", "konsekuŧion", "strukon", "permutaŧion", "palpon", "tirabuson", "spenton", "adpoxiŧion", "manegon", "skaveŧon", "xguataron", "fenŧion", "majon", "lixon", "turbion", "anemalon", "sabalon", "inpersonifegaŧion", "fregolon", "paon", "xlonfron", "patanfion", "xbrindolon", "torđion", "struŧion", "notifegaŧion", "frusion", "koson", "pacafon", "xguason", "kulon", "cakaron", "leniŧion", "maraveɉon", "taparon", "postion", "xlufon", "variaŧion", "rostelon", "lenguason", "sekakojon", "groton", "komunegaŧion", "kanevon", "spenditon", "lađaron", "luminaŧion", "dimension", "toron", "pamojon", "reɉeŧion", "radiron", "raviŧon", "totanon", "xlodron", "okaxion", "pipon", "skaveɉon", "dixnaron", "đafaranon", "malversaŧion", "ruđon", "uŧixion", "fiokon", "gravalon", "bistion", "produŧion", "anbon", "xlofon", "autoriđaŧion", "inibiŧion", "turbulaŧion", "kucaron", "guanton", "tajakanton", "xmerdon", "groson", "xonsion", "luxerton", "maskarpon", "supion", "vimenon", "recon", "tinaŧon", "maraveon", "bevon", "stekon", "ŧiŧalardon", "joŧon", "kamaŧon", "fiɉolon", "ladron", "arxaron", "fraɉon", "servision", "numaraŧion", "fruston", "sinŧeraŧion", "kabalon", "bion", "portasenjon", "kormelon", "skapuson", "xmafaron", "trinkon", "maleŧion", "strolegaŧion", "kavilon", "instruŧion", "mixeron", "piston", "sanpikon", "diavolon", "kondiŧion", "bordion", "ŧapegon", "speŧegon", "deminuŧion", "stranbon", "kastron", "satusfaŧion", "vokalidaŧion", "faŧion", "xguaŧaron", "makeron", "skiŧon", "fiadon", "granson", "okon", "lanson", "capon", "roxaron", "guardaporton", "desipon", "paneton", "ŧiñon", "torson", "regason", "fraton", "laxaron", "bokolon", "baron", "elevaŧion", "bestion", "sandon", "loskon", "skrokon", "soversion", "xbordelon", "pikon", "votaŧion", "sermon", "fagoton", "farfujon", "rixon", "stison", "brokon", "tinpanon", "palaŧon", "naŧion", "xguŧeton", "faŧoleton", "depoxitaŧion", "vagabondon", "infetaŧion", "subarendaŧion", "mojon", "opoxiŧion", "fetaŧion", "tontonon", "skulaŧon", "aŧion", "korosolon", "kuŧolon", "papon", "duŧion", "porŧelon", "kavalaron", "mucon", "codon", "skiton", "kavedon", "papalon", "fadigon", "noŧenton", "staxon", "salamon", "arxiñon", "alcon", "ranson", "revoltolon", "maniŧon", "rondon", "ŧankon", "represion", "senpliŧon", "ruxaron", "cexon", "realixaŧion", "amortidaŧion", "betegon", "ređiron", "stradon", "deluvion", "ŧimexon", "trasformaŧion", "ŧopegon", "peŧon", "mataron", "reolton", "bereton", "spulexon", "saladon", "tiraboŧon", "skoredon", "vaneŧon", "xgriñon", "koordenaŧion", "roñon", "xgrinfon", "peskon", "remiŧion", "tintinbon", "piantadon", "miraŧion", "peton", "fuxion", "polŧon", "sfrakason", "fregaŧion", "pendon", "exenpon", "restelon", "deŧipon", "rasion", "biñon", "ŧenpedon", "futiñon", "prexunŧion", "palatidaŧion", "exborsaŧion", "balotaŧion", "storton", "kolaudaŧion", "legalixasion", "kuadreton", "garđon", "maron", "inparsonifegaŧion", "rufianon", "ruvidon", "riŧon", "kuestion", "direŧion", "augumentaŧion", "vokaŧion", "jandon", "provokaŧion", "gravelaron", "klasifegaŧion", "fievaron", "jurixdiŧion", "iñoranton", "rustegon", "petegolon", "petolon", "bolpon", "kapelon", "ason", "skuartadon", "xgalmaron", "komeson", "kuŧon", "kucon", "kontaminaŧion", "bojon", "xlavaŧon", "eskusion", "insixion", "stornelon", "lagremon", "morñon", "prusision", "salpikon", "strologaŧion", "rufon", "modijon", "parladoron", "palon", "koresponsion", "galixaŧion", "galon", "dukaton", "fijolon", "suplegaŧion", "kojon", "spesegon", "xberloton", "barbixon", "spakon", "galiđaŧion", "matafion", "foson", "tenkon", "kreson", "pluton", "spion", "ŧeñon", "fogeron", "xgarafon", "invaxion", "soviŧion", "linon", "ostregon", "butaŧion", "panixasion", "puniŧion", "bragañon", "petaŧion", "supon", "skaldaŧion", "radegon", "stramason", "kusolon", "fugason", "cokolaton", "jiron", "naturalidaŧion", "makinon", "foton", "spiron", "kandelieron", "benon", "xberlon", "maon", "molton", "spalaŧon", "fioron", "opinaŧion", "mañon", "đavaɉon", "xmenbraŧion", "kastelon", "feniŧion", "furlon", "berganŧon", "pingolon", "arditon", "fasion", "valon", "ŧigalon", "gordon", "pacofon", "buxiaron", "simon", "kaston", "fadegon", "jintiron", "piaɉon", "scaton", "konplension", "ocon", "biondon", "seka–mincon", "mansipasion", "megolon", "introduŧion", "skagaron", "susuron", "interpretaŧion", "frixon", "obligaŧion", "spendaŧon", "diron", "pison", "sarangon", "spendacon", "ŧirkospeŧion", "morion", "prevarikaŧion", "boldon", "orbon", "oton", "baronon", "polon", "đeneraŧion", "bajon", "inpiantajon", "bitaŧion", "fosaton", "sporkaŧon", "negaŧion", "tordion", "fogon", "taɉon", "semiton", "bondanton", "desperadon", "kodegon", "texon", "refudaxon", "pelandron", "mardokon", "konvenŧion", "mansion", "sisalardon", "patron", "stañadon", "saguraŧion", "xbajafon", "kurioxon", "sekuraŧion", "đogadoron", "cicon", "suparstiŧion", "xgorlon", "proaŧion", "lobion", "koncesion", "pianeton", "turlon", "deskreŧion", "palatixaŧion", "inkursion", "xbrajon", "vespon", "destribuŧion", "speon", "benefiŧion", "buđaron", "bukolon", "fasendon", "sardon", "maskalson", "balanson", "ŧukon", "sklamaŧion", "talon", "garxon", "urton", "rexon", "spinon", "cacaron", "barbaŧon", "konesion", "fason", "soldon", "skalinaŧion", "donđelon", "numeraŧion", "rexoluŧion", "rafegon", "realidaŧion", "pinion", "fionon", "kontrokapon", "ŧotegon", "koredon", "gralaon", "xgarbon", "tartixon", "nevolon", "siegon", "rodolon", "rivaŧion", "kañon", "patanflon", "xmardelon", "opinion", "diminuŧion", "boridon", "berolon", "fakinon", "rikon", "sojeton", "senplision", "kapostasion", "examinaŧion", "malediŧion", "tenajon", "brikonon", "xguajaton", "sodisfaŧion", "peston", "franbolon", "afermaŧion", "muxon", "paton", "preŧepiton", "rodon", "gastaldon", "kodarosolon", "pacon", "loɉon", "guadañon", "lerijon", "điganton", "streton", "regolon", "fioreton", "spekulaŧion", "spiantadon", "avaron", "skarpelon", "finiŧion", "ŧerkanton", "xguaŧon", "rusolon", "arion", "konbustion", "lenguaŧon", "sirkospesion", "trufon", "mormolaŧion", "stivalon", "bobon", "manipolaŧion", "budelon", "seolon", "kalŧon", "mokajon", "feltron", "frustegon", "kavaron", "garbuɉon", "piaton", "teŧon", "tecon", "atribuŧion", "perdon", "rapatumaŧion", "joton", "abaton", "skalon", "kasaxon", "dixestion", "repetiŧion", "koron", "tabakon", "omason", "naxon", "buxieron", "xmañason", "kreaŧion", "scafaŧon", "reversion", "kordialon", "konŧerton", "kosperon", "kamason", "barbon", "jaketon", "sindikaŧion", "rudaron", "levaŧion", "boxiaron", "skorpion", "preŧixaŧion", "skosion", "operaŧion", "vidixon", "tonaŧion", "afaron", "morsegon", "joson", "ruŧolon", "dromejon", "blataron", "binaŧion", "vaneđon", "strapon", "provaŧion", "dindolon", "staŧion", "bicaron", "goloxon", "trafegon", "fruñon", "visigon", "skopeloton", "politon", "piajon", "botegon", "koaton", "rađon", "suspenŧion", "pason", "kanton", "espoxiŧion", "kortelon", "rospon", "mutrion", "tolon", "satisfaŧion", "pacugon", "piantaxon", "imajinaŧion", "kastañon", "spaseton", "fanfaron", "đavajon", "skorporaŧion", "skalseron", "resixion", "famincon", "xgrafon", "xlordon", "kanŧon", "lamentaŧion", "stiŧon", "biankon", "lamenton", "fortunadon", "pasion", "maneskon", "granŧon", "talenton", "maturlon", "melon", "xgerlon", "ruspion", "caceron", "exaltaŧion", "regaŧon", "ŧukolon", "agon", "kartabon", "inkuixiŧion", "xguardon", "luxertolon", "ŧitaŧion", "konkluxion", "rodeton", "brontolon", "spanpanon", "aɉeron", "sproton", "benediŧion", "inpresion", "alokon", "skuaron", "fragolon", "iskriŧion", "trenton", "bordon", "doron", "spegaŧon", "karnevalon", "sfondron", "alteraŧion", "ŧejon", "muson", "straŧon", "retenŧion", "pospoxiŧion", "muxelon", "ŧopelon", "pòrta–senjon", "sospenŧion", "marŧon", "mortaron", "straon", "xbraŧaron", "padron", "inkonbinaŧion", "makakon", "kataron", "eskluxion", "fanferon", "flusion", "bavelon", "konmixaraŧion", "esklamaŧion", "padilion", "bragieron", "struka–limon", "xbrinon", "exenŧion", "isolon", "bubon", "spiriton", "bonason", "bastion", "emoŧion", "airon", "ardiñon", "marafon", "peson", "stilaŧion", "senplison", "fanelon", "vendegaŧion", "persesion", "proibiŧion", "suton", "subordenaŧion", "strasinon", "scapon", "xgusaron", "savon", "xgranfion", "polson", "sujiŧion", "ŧiarlon", "karpion", "karateron", "kalavron", "fronton", "kanelon", "bontenpon", "scauson", "jaon", "regalon", "stramanon", "simiton", "sucon", "suŧon", "tenaɉon", "suɉiŧion", "taŧon", "tanton", "cakolon", "gaiton", "salton", "merkanton", "xbrodolon", "disipon", "buridon", "sostentaŧion", "ajeron", "kostruŧion", "tenŧion", "stra-kulon", "kofolon", "vaskon", "modion", "sorxon", "saludaŧion", "bokalon", "punion", "pearon", "duplon", "xbevacon", "kotimon", "kopelon", "skrituraŧion", "raŧionaŧion", "tartaɉon", "rebuton", "procesion", "strakulon", "belon", "rovinon", "interkonesion", "xjonfabalon", "scexon", "ranpon", "kavajon", "xgrafiñon", "skavejon", "intromision", "pelaŧion", "partesipasion", "pamoɉon", "ardaron", "loxon", "pieron", "resureŧion", "xlavason", "devoŧion", "ŧenon", "bankon", "verdon", "xbaron", "tindon", "tientinbon", "xgarbion", "spasixon", "sorafaŧion", "kuson", "akuxaŧion", "substentaŧion", "formenton", "reputaŧion", "mustason", "bastardon", "xbandon", "kontenton", "koskriŧion", "pirlon", "trinkanon", "xvernixon", "limitaŧion", "guarniŧion", "deletaŧion", "oxelon", "konvokaŧion", "amigon", "barboton", "emulaŧion", "ranpegon", "laron", "dotoron", "konsunŧion", "inbriagon", "piantađon", "lanternon", "rejilion", "viscon", "felukon", "jotiron", "berekinon", "limon", "fufiñon", "modon", "vakon", "beniŧion", "termenaŧion", "evoluŧion", "finŧion", "komision", "rudon", "viŧiadon", "xbeacon", "batelon", "điron", "bagaɉon", "sfregolon", "legaliđaŧion", "aston", "naranŧon", "kaseton", "troton", "salison", "ŧarlatanon", "kroxon", "roxon", "soɉeton", "subvenŧion", "godon", "redaŧion", "gramolon", "spendason", "tason", "vanexon", "skurton", "berton", "đovenon", "ingraton", "tenpon", "skarpon", "raxiron", "semolon", "libraŧon", "kaparon", "materon", "saŧion", "biceron", "servaŧion", "buxon", "ŧeron", "feŧion", "rejon", "kaxion", "veladon", "amision", "indukaŧion", "dormejon", "skagason", "ŧercon", "xbrison", "intrigon", "spilorŧon", "sporkason", "sonoridaŧion", "đakolon", "bagaton", "viaxon", "boaŧon", "korusolon", "petiŧion", "faxiolon", "palpuñon", "galanton", "cetinon", "xgranfiñon", "naon", "strion", "xbrision", "barufon", "delaŧion", "stropon", "bolŧon", "palangon", "verifegaŧion", "kavaŧion", "markaŧion", "saluaŧion", "prosesion", "violon", "dexestion", "soluŧion", "enfiaxion", "kativon", "xlandron", "taɉakanton", "xguasaron", "fidaŧion", "tanburon", "pastison", "komaron", "đenocon", "donŧion", "sospeŧion", "ruñon", "teson", "xlondron", "dexmentegon", "sabajon", "kavason", "fraskon", "vixdekaŧon", "redeŧimaŧion", "muniŧion", "ganbon", "opresion", "kapon", "amortixasion", "vaŧinaŧion", "gaɉon", "separaŧion", "exekuŧion", "volpon", "palidon", "promision", "deɉestion", "amortiđaŧion", "exibiŧion", "tradiŧion", "xrenaŧion", "aplikaŧion", "pantalon", "dormijon", "buxaron", "ramaxon", "sfronton", "esterminaŧion", "fakojon", "salutaŧion", "vagon", "prosimaŧion", "ternion", "grandon", "vaxon", "piron", "spropoxiton", "paraŧion", "panŧon", "baketon", "reon", "takon", "sukolon", "konsumaŧion", "kanpion", "spuleđon", "komodaŧion", "sfrontadon", "kapitolaŧion", "remuneraŧion", "dimandon", "kalieron", "kapistasion", "lubion", "pavejon", "partion", "roton", "donxelon", "jestion", "moñon", "spasiđon", "sopresion", "medajon", "evaxion", "senplifegaŧion", "blateron", "putinon", "strijon", "spigaŧon", "banboŧon", "semenaxon", "bolxon", "murelon", "skapuŧon", "Auton", "onŧion", "longon", "fugaŧon", "laxañon", "galioton", "kalaton", "kamaron", "sorđon", "tranguɉon", "diakilon", "inversion", "bon–paston", "donon", "sikuraŧion", "tribolaŧion", "relijon", "fermentaŧion", "gaton", "seguraŧion", "xmañaŧon", "propoxiŧion", "varniŧion", "xberegon", "peceron", "suaxion", "usixion", "forfexon", "xeneralixaŧion", "koreŧion", "xbrodegon", "omaŧon", "perdiŧion", "soportaŧion", "omon", "gaon", "galton", "speŧifegaŧion", "stalfon", "asunŧion", "vejon", "sportelon", "parabolon", "paron", "pension", "koɉon", "stalaŧion", "vereton", "rekoñiŧion", "sfaŧadon", "pancanon", "situaŧion", "skanpon", "desimilaŧion", "peveron", "mencon", "partiŧion", "suporaŧion", "inŧixion", "pindolon", "alboron", "grespon", "presixasion", "relegaŧion", "sasaron", "xgrendenadon", "dekoŧion", "leŧion", "piegaŧion", "garbujon", "ferion", "pernotaŧion", "struđion", "venŧion", "rediron", "valutaŧion", "mosketon", "furbacon", "voxon", "sinŧieraŧion", "ŧaŧaron", "maŧukon", "sensaŧion", "inpediŧion", "fanfuñon", "mortifegaŧion", "bigolon", "solevaŧion", "intimaŧion", "armeron", "molon", "fonŧion", "diaolon", "magon", "spaŧion", "bardason", "kaselon", "mixuraŧion", "kapi–stasion", "sitasion", "tremon", "pionon", "skajon", "maxon", "felpon", "sanson", "seleŧion", "batocon", "navon", "esesion", "fiton", "dexutilon", "grixon", "kaveson", "delubion", "spilon", "mixerion", "rogolon", "ŧenturon", "carlon", "strapaŧon", "soporaŧion", "ravaŧon", "strason", "fafuñon", "stomegon", "skortejon", "stasadon", "eskavaŧion", "exortaŧion", "bexon", "kanacon", "superaŧion", "nitron", "tajon", "notaŧion", "grongolon", "kustion", "angon", "kamuŧon", "kalison", "anexon", "konplesion", "ponaron", "preŧipiton", "bordelon", "suprestiŧion", "inbroɉon", "broketon", "trepudion", "spinadon", "turion", "xenerasion", "piŧigon", "baɉon", "mutaŧion", "siguraŧion", "tortion", "xbardelon", "kanaton", "krapolon", "spigolon", "brespon", "minoraŧion", "trataŧion", "ardijon", "krivelon", "liberaŧion", "espanŧion", "xbiron", "korpon", "sponđon", "nosenton", "pikolon", "skulieron", "marson", "treskon", "fiabon", "sponxon", "porsesion", "unŧion", "vidimaŧion", "faxolon", "bonaŧon", "estraŧion", "rugolon", "vilanon", "xbraɉon", "ñoranton", "ruđenon", "portasperon", "maton", "caron", "xventadon", "denaralidaŧion", "sparnacon", "vergon", "spuncon", "xmanfaron", "porkon", "menestron", "dexlubion", "peliŧon", "rođon", "skorŧon", "kritikon", "redesimasion", "denbraŧion", "selon", "ativaŧion", "velaŧion", "macon", "lojon", "maŧon", "espresion", "galavron", "akoɉimenton", "skovolon", "politegon", "marđokon", "falkon", "marmiton", "kaxon", "skudon", "skafon", "krespon", "farfuɉon", "xbregon", "tesaŧion", "kalmon", "tontolon", "taskon", "frankaŧion", "rafinaŧion", "kagon", "ardejon", "sordon", "panon", "tangaron", "toxon", "pecaron", "sfasadon", "beveron", "gajon", "đupon", "senton", "malagrasion", "mestolon", "skuliaron", "buskaron", "korlikon", "vinon", "aŧeton", "burlon", "cunbon", "roxeton", "strangojon", "rudenon", "frevon", "gorxon", "persuaxion", "dexertaŧion", "kondon", "feveron", "konpoxiŧion", "xlapon", "kuantifegaŧion", "tiron", "soɉeŧion", "deputaŧion", "pevaron", "tarokon", "stangirlon", "putrefaŧion", "polenton", "tokon", "xenarasion", "kucolon", "spernacon", "bon", "revixion", "kogomon", "revoluŧion", "bronbon", "napolon", "ponton", "soxeton", "nuvolon", "fasoleton", "trakanon", "versaŧion", "kanpanelon", "xguŧaron", "đergon", "kanŧelaŧion", "armaron", "kabulon", "birbon", "testaŧion", "mustaŧon", "ministraŧion", "maravejon", "marŧion", "ŧinjon", "peladon", "mustacon", "pavion", "bekon", "viandon", "konvulsion", "feton", "bovolon", "vaon", "sospension", "barakon", "perukon", "sesion", "mostacon", "ganason", "paniđaŧion", "bufon", "xetaŧion", "mincon", "pipion", "edukaŧion", "mostaŧon", "deon", "komedon", "frajon", "reɉilion", "skofon", "kordon", "deneraŧion", "porselon", "arŧon", "finestron", "ŧiexon", "demonion", "sfondradon", "batolon", "bindolon", "akojimenton", "serviŧion", "faŧendon", "kortexanon", "remengon", "tentaŧion", "sexon", "teston", "gornixon", "karbon", "agresion", "trapudion", "lovon", "somision", "piŧegon", "kornixon", "torxion", "perŧeŧion", "kaporion", "filon", "strabukon", "libaraŧion", "kason", "segon", "redenŧion", "melion", "pisigon", "patakon", "goŧon", "sion", "xgalton", "kalŧinaŧion", "ucixion", "gatomamon", "akuixiŧion", "graton", "enbrion", "sturion", "ŧopon", "skarafon", "femenon", "stanŧon", "naranson", "xgrendenon", "monton", "vixion", "brageson", "skueloton", "añon", "legraŧion", "pitokon", "inpianton", "kapurion", "ostinaŧion", "fustegon", "kalsinasion", "goton", "sedolon", "prexenŧion", "stelaŧion", "boñon", "rovijon", "kaxaton", "ŧeston", "prevenŧion", "oblaŧion", "skagaŧon", "saltalion", "masukon", "pinŧon", "fiakon", "baldon", "xbarbatolon", "ordenaŧion", "boason", "turkinon", "timon", "tartanon", "strepiton", "feston", "tronbon", "skarselon", "piegoron", "scavon", "bolson", "saŧon", "geridon", "vixdekason", "skriŧion", "xbaxucon", "suparaŧion", "dragon", "puñaton", "torion", "kavaŧon", "miserion", "pastiŧon", "sapienton", "đenaraliđaŧion", "xbadaɉon", "tolpon", "manestron", "koverton", "potifon", "tavolon", "trobolaŧion", "balon", "tanfurlon", "stolidon", "kapoton", "sojeŧion", "pastrocon", "pandolon", "relaŧion", "fisaŧion", "skoson", "kaldiron", "puton", "porton", "skorabion", "lardon", "revolton", "simulaŧion", "xbrufon", "tripudion", "eŧeŧion", "skorlon", "invenŧion", "đanbon", "komunion", "veleton", "secon", "raŧion", "inbandixon", "koton", "restrinŧion", "portaŧenjon", "panson", "baukon", "morteron", "nudriŧion", "piñoraŧion", "stangerlon", "galantomenon", "infiaxion", "seon", "ladaron", "eleŧion", "kaldiaron", "anton", "koston", "skaveson", "kapocon", "karnavalon", "ŧoton", "pariŧion", "orñon", "roketon", "kolaŧion", "palpitaŧion", "pendolon", "rekuidiŧion", "sodiŧion", "interogaŧion", "deneralidaŧion", "tendon", "janpikon", "fiolon", "solfon", "sarvaŧion", "strukalimon", "bailon", "kapo–stasion", "polveron", "manutenŧion", "materialon", "vrespon", "sasion", "bevaron", "rađiron", "kapion", "ŧotiñon", "tanon", "scapinon", "ŧefon", "furegon", "đibaldon", "imaɉinaŧion", "đofranon", "destrigon", "murlon", "parteŧipaŧion", "fraŧion", "xbajon", "tartajon", "ŧiatilion", "ondaŧion", "spetoraŧion", "gorđon", "torefaŧion", "kanselasion", "estension", "subversion", "poxiŧion", "beŧon", "ostentaŧion", "korniđon", "inkatijon", "soxeŧion")),
			new LineEntry("un", "úi", "un", Arrays.asList("dexun", "ñisun", "ñaun", "brun", "nesun", "ñesun", "oñun", "kalkun", "nisun", "brunbrun", "negun", "tribun", "nigun", "koalkedun", "ŧaskun", "kualkun", "dedun", "Utun", "alkun", "kualkedun", "Autun", "kalkedun", "komun", "deđun", "koalkun", "Otun", "algun", "ŧaskadun")),
			new LineEntry("ul", "úi", "ul", Arrays.asList("strakul", "miul", "trul", "strul", "spigarul", "albul", "stra-kul", "ingrasamul", "tirakul", "batikul", "ronpikul", "furbikul", "baul", "bul", "forbikul", "ponxikul", "meul", "stropakul")),
			new LineEntry("el", "éi", "el", Arrays.asList("bruxapel", "fiel", "kavakavel", "kontropel", "kaveel", "pomel", "ŧiel", "kontrapel", "pexel", "kaostel", "strapel", "kaviel", "kavel", "petel")),
			new LineEntry("ol", "ói", "ol", Arrays.asList("skol", "pisarol", "skodarol", "sotarol", "rovejol", "garxiñol", "solŧarol", "rixarol", "ucarol", "spondol", "vovarol", "boarol", "roveɉol", "rosiñol", "fisol", "ovarol", "xjoŧarol", "ranarol", "pođol", "bavarol", "fiŧol", "libarol", "storol", "spiansarol", "kaxarol", "bronbejol", "skapuŧiol", "korexol", "saldarol", "kordarol", "beđarol", "tastarol", "kamixol", "navarol", "ponterol", "erbarol", "faxiol", "xɉosarol", "stentarol", "kanarol", "skapusiol", "supiarol", "biavarol", "fondarol", "parol", "liŧarol", "letexol", "bruxarol", "bronbeɉol", "sordarol", "garofol", "faŧiol", "faxol", "braŧarol", "fasiol", "ŧendrarol", "pesarol", "tersol", "terŧol", "bañarol", "kroxol", "bisol", "rafiol", "kuajarol", "laudevol", "porixol", "viscarol", "sturiol", "barkarol", "kolarol", "grandiol", "vedol", "cavarol", "laxarol", "revendarol", "pipirol", "kivol", "besarol", "rusiñol", "ganxarol", "korparol", "telarol", "garđiñol", "bexarol", "karol", "tersarol", "đontarol", "studiol", "semenŧarol", "tajol", "koređol", "cijarol", "pexarol", "đornarol", "ranpegarol", "reditarol", "beverarol", "skuerarol", "podol", "picol", "portarol", "garŧiñol", "ponarol", "piasarol", "bronbijol", "montixol", "frutarol", "pradarol", "fiabarol", "draparol", "ŧernierol", "latarol", "tratarol", "kuartarol", "miedarol", "gaxol", "artarol", "boskarol", "poriđol", "balanŧiol", "paɉarol", "pontirol", "bronbiɉol", "sponđol", "introl", "xatarol", "kairol", "sponxol", "coarol", "gardiñol", "parasol", "kanapiol", "kanpañarol", "skuderol", "kanevarol", "guxarol", "borsarol", "rubarol", "skodirol", "primarol", "gatarol", "bokarol", "venol", "seseɉol", "semensarol", "ninsiol", "gratarol", "stiŧarol", "feraŧol", "ganŧarol", "ninŧiol", "pesol", "strasarol", "ninŧol", "lumarol", "penarol", "pekol", "ferasol", "tirafol", "ninsol", "ŧimarol", "veđol", "kiɉarol", "sorđarol", "peŧol", "lodevol", "jarol", "sorxarol", "beŧarol", "figaŧol", "piaŧarol", "garsiñol", "dentarol", "skondarol", "rofiol", "koredol", "spianŧarol", "figasol", "linarol", "niŧiol", "nisiol", "roxiñol", "gansarol", "ruxiñol", "terŧarol", "stradarol", "ŧestarol", "codarol", "ferarol", "sesejol", "gucarol", "sportarol", "spigarol", "merdarol", "rasarol", "koresol", "pegorarol", "ŧerarol", "rufiol", "boŧol", "ladarol", "scoparol", "panarol", "poridol", "brasarol", "granđiol", "masarol", "kanol", "pajarol", "straŧarol", "akuarol", "poxol", "niŧol", "speriol", "đirasol", "stisarol", "xɉoŧarol", "ganđarol", "lisarol", "broñol", "bosol", "salarol", "latexiol", "pestarol", "balansiol", "skonbrarol", "orxol", "basarol", "bevarol", "nisol", "luxarol", "kontarol", "fiol", "pontarol", "farinarol", "agostarol", "lađarol", "viñarol", "mañarol", "mokarol", "armarol", "polarol", "fumarol", "maŧarol", "bugarol", "peŧarol", "bedarol", "verlol", "pixol", "gandarol", "vexol", "muxarol", "xjosarol", "peñarol", "kijarol", "solsarol")),
			new LineEntry("al", "ài", "al", Arrays.asList("kapusal", "parameđal", "anoal", "naŧional", "bal", "perpetual", "trionfal", "sokorsal", "masakaval", "infernal", "dental", "megal", "kaval", "lonđitudenal", "spisial", "substanŧial", "personal", "lidial", "aspetual", "làvar–dental", "kurial", "kapuŧal", "prepoxiŧional", "skinal", "sotogrondal", "malugual", "kauxal", "siñal", "deal", "piasal", "logativa–direŧional", "dixial", "strajudisial", "manual", "tribunal", "stamenal", "pontekanal", "komunal", "logativo–direŧional", "matrikal", "kriminal", "piaŧal", "vesal", "muxegal", "deŧimal", "averbial", "sostanŧial", "autonal", "veŧal", "oriđenal", "labial", "gronbial", "ospedal", "frontal", "saondal", "piantal", "sagramental", "kontestual", "dexegual", "estrajudisial", "komersal", "modal", "bakanal", "sonđal", "komerŧal", "verdoxal", "maregal", "inpal", "sojal", "internasional", "final", "ibernal", "vental", "amoral", "orixenal", "internaŧional", "ofesial", "solkal", "ocal", "subafitual", "pioval", "bilabial", "nabukal", "kaotonal", "prinsipal", "artifisial", "kuintal", "luminal", "badial", "inlegal", "material", "ordenal", "agual", "sforsanal", "karamal", "moral", "ŧerforal", "soŧial", "prepoxisional", "batipal", "bati–pal", "nasional", "fraimal", "metal", "oriental", "korporal", "stival", "boral", "ugual", "tenporal", "skosal", "prexidenŧial", "inbal", "anal", "konfesional", "teritorial", "subtropegal", "pedestal", "kantonal", "diđial", "vertegal", "meridional", "petoral", "vidal", "faval", "pontual", "kaveal", "fonŧional", "speal", "sonxal", "penal", "làvaro–dental", "argomental", "presidenŧial", "femenal", "mesal", "londitudenal", "brunal", "reversal", "logativo–diresional", "ŧinɉal", "soŧal", "prinŧipal", "đudiŧial", "dimisorial", "brasal", "estraɉudiŧial", "speŧial", "grinbial", "rumegal", "sforŧanal", "gardinal", "gramadegal", "braŧal", "predial", "saonxal", "estrajudiŧial", "ŧenjal", "retikal", "noal", "vixal", "fitual", "fonsional", "gual", "real", "maŧakaval", "pertegal", "sponsal", "seraval", "postal", "verdodal", "onbrinal", "piantanemal", "ufisial", "vetural", "kavaŧal", "dorsal", "kultural", "kavastival", "serviŧial", "đornal", "xornal", "mental", "regal", "setemanal", "kavasal", "total", "inŧidental", "universal", "utunal", "sureal", "nomenal", "fraxal", "barbusal", "ofeŧial", "didial", "kristal", "tirastival", "ganbal", "barbuŧal", "iniŧial", "ŧentral", "đeneral", "kardenal", "aɉetival", "kanal", "dukal", "bordenal", "xmerdagal", "presidial", "afitual", "somensal", "koral", "kornal", "ajetival", "vial", "naxal", "verdođal", "gardenal", "sondal", "nadural", "ferial", "ufiŧial", "kaporal", "autunal", "animal", "destretual", "stromental", "arsanal", "grondal", "orixinal", "sostansial", "xendal", "substansial", "bordonal", "inperial", "nominal", "paramexal", "parangal", "somenŧal", "lial", "saonđal", "stal", "strajudiŧial", "nemal", "dornal", "kavedal", "sosal", "pivial", "lateral", "karneval", "skarkaval", "invernal", "lengual", "skartakaval", "lesegal", "kondisional", "vernegal", "kopal", "abitual", "referensial", "kondiŧional", "verbal", "estraɉudisial", "ofisial", "menal", "dexial", "oridontal", "farnimal", "skenal", "sirkostansial", "pòrta–penal", "ilegal", "egual", "plural", "straɉudiŧial", "inisial", "koronal", "nadal", "manoal", "baoral", "vokal", "minimal", "fardimal", "puñal", "kanocal", "insial", "soɉal", "lonxitudenal", "feminal", "numeral", "memorial", "dedal", "mortal", "servisial", "normal", "skritural", "palatal", "insidental", "baxoal", "stradal", "solidal", "aramal", "pontifegal", "gramal", "bestial", "traval", "spesial", "babal", "sperimental", "senal", "pòrta–mesal", "interinal", "presidensial", "traversal", "superfiŧial", "baroal", "xguinsal", "natural", "xudisial", "karnaval", "existensial", "feral", "xguinŧal", "grenbial", "konvensional", "ospeal", "tinbal", "orixontal", "portegal", "doxenal", "portapenal", "mural", "anemal", "referenŧial", "unilateral", "vakasal", "vakaŧal", "existenŧial", "plateal", "kavesal", "lokal", "semi–lateral", "madregal", "engual", "portamesal", "dudiŧial", "setimanal", "ŧinjal", "kaveŧal", "dial", "ŧendal", "fortunal", "papagal", "paramedal", "kapital", "modegal", "logativa–diresional", "patual", "portogal", "uxual", "kanbial", "ŧenɉal", "noval", "skarta–kaval", "xguansal", "ŧirkostanŧial", "orinal", "pòst–àlveo–palatal", "katredal", "frimal", "konvenŧional", "pontal", "bankal", "krestal", "individual", "sakramental", "konetral", "desimal", "ofiŧial", "dexeal", "fanal", "xguanŧal", "arsenal", "prexidensial", "orial", "grenal", "kuartal", "mantexenal", "minal", "semi–naxal", "señal", "distretual", "fiskal", "spiŧial", "sosial", "oridenal", "pronomenal", "straɉudisial", "superfisial", "xlibral", "oriđontal", "fondamental", "aŧal", "kordial", "kokal", "ministerial", "otunal", "inŧial", "parsonal", "bokal", "asal", "meal", "dotoral", "viñal", "rival")),
			new LineEntry("an", "ài", "an", Arrays.asList("malsan", "tulipan", "bonvivan", "pastran", "borgexan", "đoldan", "baɉan", "segrestan", "deretan", "pisakan", "bresan", "tajan", "vineŧian", "taja–pan", "vinesian", "ŧarabaldan", "nostran", "mexan", "taɉapan", "itaɉan", "maran", "katalan", "lotregan", "turbian", "bekaran", "trivixan", "ragan", "bigaran", "pantegan", "arlan", "patan", "romitan", "skalŧakan", "skrivan", "kavian", "trevixan", "bran", "marabolan", "padan", "međan", "vinisian", "kavarđaran", "marostegan", "brixan", "bragolan", "palandran", "tajapan", "viniŧian", "pacan", "ŧanbelan", "kuran", "balŧan", "ingan", "barakan", "balsan", "anbrakan", "ŧerlatan", "itajan", "lateran", "medolan", "eraklean", "barbasan", "sopran", "sakrestan", "barbakan", "barbaŧan", "padovan", "roskan", "pojapian", "malsakan", "pulitan", "caran", "violipan", "bakan", "mundan", "skan", "venesian", "rodolan", "ŧapa–pian", "vilan", "detregan", "drian", "rabikan", "kordovan", "antian", "famalan", "ostregan", "maturlan", "piovexan", "soran", "barbajan", "kavardaran", "takapan", "vulkan", "sovran", "medan", "pikapan", "piovan", "polikan", "borean", "kastelan", "baban", "surian", "piovegan", "pesekan", "kristian", "tralian", "bajan", "uragan", "lontan", "urban", "anpesan", "barbaɉan", "taɉa–pan", "anpeŧan", "anglekan", "degan", "anŧian", "arteđan", "marobolan", "tavan", "mañapan", "valexan", "lavran", "bastran", "pagan", "morian", "languisan", "ŧapapian", "tiran", "roan", "detragan", "pedan", "agortan", "carabaldan", "katapan", "talian", "gaban", "tandan", "kaopian", "paexan", "solan", "carlan", "afan", "montan", "graexan", "domenikan", "furian", "grosolan", "pantan", "pelikan", "liban", "artexan", "ŧinban", "međolan", "ledan", "burlan", "anglo–meregan", "guardian", "xletran", "kafetan", "mataran", "kapitan", "marŧapan", "intian", "altopian", "ansian", "indian", "altivolan", "furlan", "ixolan", "mexolan", "agostan", "arkan", "malxapan", "paltan", "konplean", "peskapian", "bevan", "karantan", "parocan", "ŧarlatan", "bean", "molnan", "parabolan", "galan", "bagan", "malan", "polexan", "bon–vivan", "ŧaratan", "spiantan", "kapotan", "dulipan", "forean", "artedan", "pepian", "rafakan", "tanburlan", "masakan", "veneŧian", "portolan", "krestian", "faxan", "arŧaran", "skalsakan", "toskan", "torexan", "gabian", "arsaran", "ortolan", "pavan", "kortexan", "divan", "padoan", "kastrakan", "axolan", "marsapan", "maŧakan", "kan", "rufian", "barban", "kavarxaran", "antan", "prusian", "matapan", "italian", "korean", "vigan", "sotodegan", "mean", "pedemontan", "poɉapian", "ostegan")),
			new LineEntry("n", "i", "[èò]n", Arrays.asList("bixnòn", "bontòn", "patròn", "mexèn", "pièn", "međèn", "alièn", "armèn", "sòn", "tòn", "trèn", "bonbòn", "spròn", "òn", "medèn", "saòn", "kakasèn")),
			new LineEntry("l", "i", "[èò]l", Arrays.asList("kojonŧèl", "xbarbatèl", "ranaròl", "soriòl", "ovaròl", "seconŧèl", "tenpexèl", "kastañòl", "karagòl", "liŧaròl", "masèl", "braŧaròl", "ŧigaròl", "redexèl", "albòl", "bertevèl", "kokajòl", "barbisòl", "flajèl", "ruxiòl", "skodaròl", "krudèl", "tastaròl", "monexèl", "pisaròl", "lionŧèl", "garxonsèl", "koɉonŧèl", "miedaròl", "armakòl", "fainèl", "buèl", "kaxonŧèl", "bativèl", "faxiòl", "lađaròl", "artedanèl", "tinasòl", "lusatèl", "ventexèl", "gostaròl", "libaròl", "kuintèl", "bañòl", "formentèl", "novèl", "faxòl", "ŧendraròl", "menoèl", "ganbèl", "kavriòl", "fosatèl", "dakòl", "bocòl", "fornèl", "brikonŧèl", "kanpañòl", "barbuŧòl", "poridòl", "porsèl", "manevèl", "bexaròl", "đemèl", "bardevèl", "kroñòl", "borèl", "galiotèl", "korparòl", "santarèl", "boriñòl", "toajòl", "oridèl", "osakòl", "pursinèl", "barixèl", "timonsèl", "menedèl", "kontarèl", "burcèl", "fogerèl", "raixèl", "roviɉòl", "gomisièl", "filŧòl", "arditèl", "bindèl", "bronbijòl", "kartèl", "korexiòl", "buratèl", "rièl", "kaveŧòl", "ŧarlatanèl", "bolsanèl", "grajotèl", "kortexanèl", "pasarèl", "valonsèl", "stanpatèl", "staterèl", "palasòl", "fatarèl", "beveraròl", "apèl", "kuariŧèl", "rovejòl", "borsaròl", "kavièl", "rudiòl", "bardòl", "mexadèl", "ranpegaròl", "antianèl", "arteđanèl", "ladronŧèl", "korñòl", "tornèl", "trataròl", "intianèl", "solexèl", "maŧaròl", "gransiòl", "gatèl", "penaròl", "đoɉèl", "alberèl", "arxonèl", "boŧèl", "ruviɉòl", "figadèl", "kasèl", "sportaròl", "bajardèl", "infogadèl", "kovièl", "kaecòl", "linaròl", "bastonŧèl", "paŧarèl", "kortèl", "kaŧonèl", "violonŧèl", "fraxèl", "periòl", "piòl", "xgabèl", "luŧatèl", "sonaɉòl", "biviòl", "soldadèl", "furtarèl", "barđòl", "dindiotèl", "baɉardèl", "rovijòl", "krivèl", "purŧinèl", "anèl", "fondèl", "korniòl", "tariòl", "poŧòl", "pontexèl", "faganèl", "franguèl", "oropèl", "andiòl", "masòl", "spianŧaròl", "putatèl", "karetèl", "picarèl", "kapuŧòl", "fidèl", "vexòl", "sigaròl", "bordèl", "xɉoŧaròl", "moèl", "xbardavèl", "salaròl", "gavinèl", "marŧèl", "morèl", "kolomèl", "risòl", "banbinèl", "brasadèl", "luxaròl", "ròl", "putèl", "mantexèl", "fumaròl", "orđiòl", "granđiòl", "skartabèl", "đanbèl", "solaròl", "fijòl", "kantonŧèl", "kokoñèl", "sorđaròl", "borsèl", "matuxèl", "menuèl", "ŧestèl", "xolariòl", "piegadèl", "tovaɉòl", "sexèl", "đenocèl", "baronŧèl", "purisinèl", "tondèl", "saltarèl", "ferasòl", "viñaròl", "bruɉèl", "agostaròl", "đovèl", "gardòl", "sordaròl", "kanpièl", "terŧòl", "takakapèl", "bokonŧèl", "meɉaròl", "skansèl", "konterèl", "brokadèl", "pontèl", "xguaɉatèl", "skopèl", "sponsòl", "pòl", "gonfiadèl", "detreganiòl", "rixaròl", "roveɉòl", "solŧaròl", "balkonsèl", "xɉosaròl", "ardarèl", "viŧinèl", "vovaròl", "sparsèl", "rosiñòl", "kavicòl", "vetriòl", "mariòl", "pajòl", "pođòl", "kanèl", "bemòl", "lekapestèl", "kusinèl", "martèl", "statarèl", "codèl", "kaŧòl", "orpèl", "kordaròl", "braŧadèl", "fiumexèl", "kamixòl", "linbèl", "kokaɉòl", "stentaròl", "lovastrèl", "pediŧèl", "spondòl", "viscaròl", "lixèl", "biskotèl", "brandèl", "paɉaròl", "somarèl", "skapusiòl", "ŧestaròl", "fondaròl", "karòl", "trabukèl", "ŧarvèl", "torèl", "remendèl", "ganxaròl", "bisinèl", "kainèl", "rejotèl", "forsèl", "ordiòl", "guardòl", "batèl", "kagonŧèl", "poriđòl", "jaŧòl", "sedèl", "xataròl", "burèl", "spiansaròl", "kanpañaròl", "guxaròl", "turkèl", "bixinèl", "kairòl", "panexèl", "polesòl", "burlèl", "fiabaròl", "kolaròl", "carèl", "rigabèl", "faɉòl", "boaròl", "livèl", "korixiòl", "sfasèl", "stra-bèl", "faŧòl", "penonŧèl", "mièl", "molexèl", "restèl", "tovajòl", "ŧaratanèl", "fanèl", "sonèl", "bagèl", "kiɉaròl", "orxiòl", "rebèl", "skondaròl", "kapriòl", "kavedèl", "kuadrèl", "semensaròl", "intrigadèl", "storđikòl", "stuèl", "primaròl", "xmardèl", "grataròl", "veèl", "ganŧaròl", "seđèl", "terŧaròl", "sukòl", "soranèl", "kanevaròl", "menevèl", "piñatèl", "vecexòl", "đornaròl", "gardèl", "porixòl", "vedriòl", "betinèl", "dragonŧèl", "kanpanèl", "artexanèl", "moskatèl", "ruvijòl", "niŧiòl", "rapanèl", "bekiñòl", "linsòl", "ninsiòl", "martarèl", "subiòl", "spigaròl", "kanòl", "niŧòl", "korbatèl", "osokòl", "bigorèl", "poxòl", "brujèl", "lisaròl", "fapèl", "barbusòl", "merdaròl", "bonèl", "montexèl", "bisterèl", "rufiòl", "sediòl", "pontaròl", "ŧivèl", "gropèl", "scoparòl", "oripèl", "panaròl", "indovinèl", "konsapèl", "bugaròl", "medadèl", "majòl", "stentarèl", "garbèl", "peñaròl", "torsèl", "grinxòl", "xjosaròl", "arxarèl", "mantèl", "kapitèl", "gròl", "bertovèl", "menarèl", "noxèl", "ninŧòl", "minuèl", "kontaròl", "listèl", "panetèl", "brasaròl", "bokaròl", "barxòl", "vanarèl", "durèl", "stiŧaròl", "lionsèl", "brikonsèl", "ardonèl", "kojonsèl", "barbiŧòl", "brasiòl", "trapèl", "palaŧòl", "oxèl", "portakapèl", "remondèl", "ponaròl", "maŧèl", "ŧixèl", "brentèl", "karoxèl", "ponteròl", "peterèl", "garxonŧèl", "koɉonsèl", "kaxonsèl", "tasèl", "pòrta–kapèl", "paròl", "skarpèl", "fasinèl", "tinèl", "bulièl", "kanpanièl", "tornexèl", "bronbeɉòl", "marèl", "maŧaporŧèl", "kosinèl", "bustarèl", "pesaròl", "bigarèl", "faŧiòl", "albuòl", "rafiòl", "piatèl", "bèl", "beđaròl", "barkaròl", "raganèl", "paɉòl", "supiaròl", "međadèl", "vedèl", "filixèl", "bestiòl", "seconsèl", "skarkaɉòl", "ucaròl", "kanaròl", "peŧarèl", "ruđiòl", "konastrèl", "kojonèl", "tajòl", "finestrèl", "lenŧiòl", "kuarisèl", "timonŧèl", "laxaròl", "ravanèl", "arđonèl", "perpetuèl", "ŧexendèl", "kavesòl", "kankarèl", "rokèl", "dentèl", "referatèl", "bronbiòl", "buricinèl", "biavaròl", "fièl", "manipòl", "ŧernieròl", "cavaròl", "storxikòl", "ŧimaròl", "fajòl", "baketèl", "boskaròl", "besaròl", "kasiòl", "ŧervèl", "đirèl", "tinaŧòl", "karaguòl", "mapèl", "batibèl", "spañòl", "ŧenocèl", "sponđòl", "marexèl", "kastèl", "sanbuèl", "reditaròl", "kuajaròl", "podòl", "frutaròl", "barbastèl", "remandèl", "xbrindèl", "deèl", "piasaròl", "ladronsèl", "pradaròl", "filivèl", "akuarèl", "skabèl", "salbanèl", "tersaròl", "priòl", "bikiñòl", "sorxaròl", "penèl", "pesarèl", "filexèl", "muxaròl", "riganèl", "peŧòl", "fagotèl", "kokonèl", "añèl", "strasaròl", "kavrinòl", "bulèl", "bastonsèl", "buxnèl", "modèl", "bartavèl", "buɉòl", "ŧercèl", "ñèl", "feraròl", "skuderòl", "skavesakòl", "polaròl", "montèl", "orxòl", "recotèl", "rasaròl", "beŧaròl", "tristarèl", "xbrindakòl", "posòl", "trabokèl", "bosèl", "puriŧinèl", "forkamèl", "lumaròl", "nixèl", "stornèl", "ninŧiòl", "piaŧaròl", "đinbèl", "rusèl", "ocèl", "palixèl", "sponxòl", "konostrèl", "cavexèl", "menestròl", "rimandèl", "maŧòl", "violonsèl", "xenocèl", "pesatèl", "straŧaròl", "gandaròl", "masaròl", "trivèl", "kalkañòl", "pajaròl", "gucaròl", "fraèl", "porŧèl", "ruxiñòl", "fiòl", "baronsèl", "kapusòl", "rantegèl", "marsèl", "lensiòl", "maɉòl", "ŧokatèl", "farinaròl", "ŧexèl", "saldaròl", "kanarèl", "riŧòl", "mastèl", "portèl", "mokaròl", "karamèl", "bekonèl", "kantonsèl", "xnèl", "guèl", "strabèl", "solsaròl", "ŧerlatanèl", "akuaròl", "kolmèl", "filandèl", "kapèl", "kunèl", "skañèl", "xugèl", "garđòl", "kolonèl", "latexiòl", "rededèl", "gratarèl", "kanapiòl", "pestaròl", "kuarèl", "bolsonèl", "vedovèl", "armaròl", "priastèl", "feraŧòl", "krièl", "đimèl", "kaxaròl", "ortexèl", "tritèl", "ordòl", "braŧiòl", "filièl", "bufonèl", "secèl", "kalastrèl", "bakariòl", "boxèl", "vixinèl", "bavaròl", "bokonsèl", "skanŧèl", "skarkajòl", "mejaròl", "fuxèl", "tersòl", "balkonŧèl", "bròl", "skonbraròl", "skòl", "bañaròl", "kananiòl", "bronbejòl", "matonèl", "fradèl", "xbrefèl", "draparòl", "batarèl", "mañaputèl", "radixèl", "sponŧòl", "trufèl", "patèl", "veriòl", "martorèl", "kasòl", "pijakaragòl", "kasonèl", "buranèl", "fasiòl", "toxatèl", "đontaròl", "ŧeraròl", "cijaròl", "ganđaròl", "bastardèl", "frèl", "sixèl", "sturiòl", "masaporsèl", "bolŧonèl", "vedoèl", "flaɉèl", "kogòl", "tinpanèl", "navaròl", "denocèl", "erbaròl", "redeđèl", "pedisèl", "karuxèl", "skaveŧakòl", "polastrèl", "stañòl", "spasèl", "telaròl", "kamèl", "bixatèl", "pexaròl", "manganèl", "semenŧaròl", "saltèl", "forŧèl", "taɉòl", "siòl", "guarnèl", "medòl", "kagonsèl", "sorakòl", "grandiòl", "rostèl", "dovèl", "revendaròl", "infiadèl", "kadenèl", "bronbiɉòl", "garnèl", "jasòl", "skueraròl", "daòl", "sfrasèl", "artaròl", "spinèl", "kavestrèl", "poleŧòl", "tabarèl", "mòl", "travexèl", "xguajatèl", "granèl", "kruèl", "fardèl", "montixòl", "garangèl", "storiòl", "sfaŧèl", "sapientèl", "vesinèl", "buxinèl", "lataròl", "peloxèl", "vasèl", "codaròl", "roxiñòl", "konŧapèl", "pekòl", "đojèl", "pòrta–mantèl", "gavitèl", "dragonsèl", "garxòl", "barexèl", "fanfarièl", "fasòl", "kijaròl", "paganèl", "penonsèl", "sonajòl", "granxiòl", "kaniòl", "bigòl", "budèl", "coaròl", "bokardèl", "kuartaròl", "gataròl", "stradaròl", "kañòl", "gansaròl", "rusiñòl", "skankòl", "ŧukòl", "tirafòl", "dukatèl", "visinèl", "stisaròl", "karavèl", "salbrunèl", "kòl", "rubaròl", "pividèl", "koresòl", "portamantèl", "karièl", "tirèl", "karakòl", "nisiòl", "linŧòl", "tornakòl", "konkòl", "gaitèl", "filèl", "gabanèl", "koɉonèl", "altarèl", "bedaròl", "betarèl", "kormèl", "arđarèl", "trinèl", "piñòl", "basaròl", "manuèl", "albarèl", "bevaròl", "skirèl", "drapèl", "orđòl", "bruñòl", "asexèl", "spinièl", "koruxiòl", "ronpikòl", "kanestrèl", "fròl", "bertoèl", "panèl", "peŧaròl", "petèl", "pipistrèl", "tramandèl", "kamòl", "pandòl", "matarèl", "xjoŧaròl", "ninsòl", "graɉotèl", "reɉotèl", "kaŧiòl", "karatèl", "molinèl", "pegoraròl", "stordikòl", "libèl", "bujòl", "ladaròl", "xovèl", "nisòl", "axenèl", "fiɉòl", "kaxèl", "murèl", "mestèl", "lavèl", "ŧopèl", "barbarinèl", "ŧinbanèl", "spinarèl"))
		);
		Assertions.assertEquals(expectedCompactedRules, compactedRules);

		List<String> rules = reducer.convertFormat(flag, false, compactedRules);
		List<String> expectedRules = Arrays.asList(
			"SFX U0 Y 10",
			"SFX U0 al ài al",
			"SFX U0 el éi el",
			"SFX U0 ol ói ol",
			"SFX U0 ul úi ul",
			"SFX U0 an ài an",
			"SFX U0 en éi en",
			"SFX U0 on ói on",
			"SFX U0 un úi un",
			"SFX U0 l i [èò]l",
			"SFX U0 n i [èò]n"
		);
		Assertions.assertEquals(expectedRules, rules);

		reducer.checkReductionCorrectness(flag, rules, originalRules, originalLines);
	}

	//P8, P7, P6, ... cases

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
