package unit731.hunspeller.parsers.dictionary;

import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.FileService;


//https://github.com/hunspell/hunspell/tree/master/tests/v1cmdline > morph.aff upward
public class WordGeneratorTest{

	private AffixParser affParser;
	private FlagParsingStrategy strategy;
	private WordGenerator wordGenerator;


	@Before
	public void init(){
		affParser = new AffixParser();
		wordGenerator = new WordGenerator(affParser, null, null);
	}


	@Test
	public void affFormat() throws IOException{
		StringJoiner sj = new StringJoiner("\n");
		String content = sj.add("SET UTF-8")
			.add("# Testing also whitespace and comments.")
			.add("OCONV 7 # space, space")
			.add("OCONV	a A # tab, space, space")
			.add("OCONV	á	Á # tab, tab, space")
			.add("OCONV	b	B	# tab, tab, tab")
			.add("OCONV  c  C		# 2xspace, 2xspace, 2xtab")
			.add("OCONV	 d 	D # tab+space, space+tab, space")
			.add("OCONV e E #")
			.add("OCONV é É 	")
			.add("")
			.add(" # space")
			.add("  # 2xspace")
			.add("	# tab")
			.add("		# 2xtab")
			.add(" 	# space+tab")
			.add("	 # tab+space")
			.toString();
		File affFile = FileService.getTemporaryUTF8File(content);
		affParser.parse(affFile);

		Map<String, String> outputConversionTable = affParser.getData(AffixTag.OUTPUT_CONVERSION_TABLE);

		Map<String, String> expected = new HashMap<>();
		expected.put("a", "A");
		expected.put("á", "Á");
		expected.put("b", "B");
		expected.put("c", "C");
		expected.put("d", "D");
		expected.put("e", "E");
		expected.put("é", "É");
		Assert.assertEquals(expected, outputConversionTable);
	}


	@Test
	public void conditions() throws IOException{
		StringJoiner sj = new StringJoiner("\n");
		String content = sj.add("SET UTF-8")
			.add("SFX A Y 6")
			.add("SFX A 0 a .")
			.add("SFX A 0 b b")
			.add("SFX A 0 c [ab]")
			.add("SFX A 0 d [^ab]")
			.add("SFX A 0 e [^c]")
			.add("SFX A 0 f a[^ab]b")
			.toString();
		File affFile = FileService.getTemporaryUTF8File(content);
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();

		String line = "a/A";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(4, stems.size());
		//base production
		Assert.assertEquals(new Production("a", "A", "st:a", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("aa", null, "st:a", strategy), stems.get(1));
		Assert.assertEquals(new Production("ac", null, "st:a", strategy), stems.get(2));
		Assert.assertEquals(new Production("ae", null, "st:a", strategy), stems.get(3));
	}


	@Test
	public void stems1() throws IOException{
		StringJoiner sj = new StringJoiner("\n");
		String content = sj.add("SET UTF-8")
			.add("FLAG long")
			.add("SFX S1 Y 1")
			.add("SFX S1 0 s1/S2P1")
			.add("SFX S2 Y 1")
			.add("SFX S2 0 s2")
			.add("PFX P1 Y 1")
			.add("PFX P1 0 p1")
			.toString();
		File affFile = FileService.getTemporaryUTF8File(content);
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();

		String line = "aa/S1";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(5, stems.size());
		//base production
		Assert.assertEquals(new Production("aa", "S1", "st:aa", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("aas1", "P1S2", "st:aa", strategy), stems.get(1));
		//twofold productions
		Assert.assertEquals(new Production("aas1s2", "P1", "st:aa", strategy), stems.get(2));
		//lastfold productions
		Assert.assertEquals(new Production("p1aas1", null, "st:aa", strategy), stems.get(3));
		Assert.assertEquals(new Production("p1aas1s2", null, "st:aa", strategy), stems.get(4));
	}

	@Test
	public void stems2() throws IOException{
		StringJoiner sj = new StringJoiner("\n");
		String content = sj.add("SET UTF-8")
			.add("FLAG long")
			.add("SFX S1 Y 1")
			.add("SFX S1 0 s1/S2")
			.add("SFX S2 Y 1")
			.add("SFX S2 0 s2/P1")
			.add("PFX P1 Y 1")
			.add("PFX P1 0 p1")
			.toString();
		File affFile = FileService.getTemporaryUTF8File(content);
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();

		String line = "aa/S1";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(4, stems.size());
		//base production
		Assert.assertEquals(new Production("aa", "S1", "st:aa", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("aas1", "S2", "st:aa", strategy), stems.get(1));
		//twofold productions
		//lastfold productions
		Assert.assertEquals(new Production("aas1s2", "P1", "st:aa", strategy), stems.get(2));
		Assert.assertEquals(new Production("p1aas1s2", null, "st:aa", strategy), stems.get(3));
	}

	@Test
	public void stems3() throws IOException{
		StringJoiner sj = new StringJoiner("\n");
		String content = sj.add("SET UTF-8")
			.add("FLAG long")
			.add("SFX S1 Y 1")
			.add("SFX S1 0 s1/S2")
			.add("SFX S2 Y 1")
			.add("SFX S2 0 s2")
			.add("PFX P1 Y 1")
			.add("PFX P1 0 p1")
			.toString();
		File affFile = FileService.getTemporaryUTF8File(content);
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();

		String line = "aa/S1P1";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(6, stems.size());
		//base production
		Assert.assertEquals(new Production("aa", "S1P1", "st:aa", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("aas1", "P1S2", "st:aa", strategy), stems.get(1));
		//twofold productions
		Assert.assertEquals(new Production("aas1s2", "P1", "st:aa", strategy), stems.get(2));
		//lastfold productions
		Assert.assertEquals(new Production("p1aa", null, "st:aa", strategy), stems.get(3));
		Assert.assertEquals(new Production("p1aas1", null, "st:aa", strategy), stems.get(4));
		Assert.assertEquals(new Production("p1aas1s2", null, "st:aa", strategy), stems.get(5));
	}

	@Test
	public void stems4() throws IOException{
		StringJoiner sj = new StringJoiner("\n");
		String content = sj.add("SET UTF-8")
			.add("FLAG long")
			.add("SFX S1 Y 1")
			.add("SFX S1 0 s1/S2")
			.add("SFX S2 Y 1")
			.add("SFX S2 0 s2")
			.add("PFX P1 Y 1")
			.add("PFX P1 0 p1")
			.toString();
		File affFile = FileService.getTemporaryUTF8File(content);
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();

		String line = "aa/P1S1";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(6, stems.size());
		//base production
		Assert.assertEquals(new Production("aa", "P1S1", "st:aa", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("aas1", "P1S2", "st:aa", strategy), stems.get(1));
		//twofold productions
		Assert.assertEquals(new Production("aas1s2", "P1", "st:aa", strategy), stems.get(2));
		//lastfold productions
		Assert.assertEquals(new Production("p1aa", null, "st:aa", strategy), stems.get(3));
		Assert.assertEquals(new Production("p1aas1", null, "st:aa", strategy), stems.get(4));
		Assert.assertEquals(new Production("p1aas1s2", null, "st:aa", strategy), stems.get(5));
	}

	@Test
	public void stems5() throws IOException{
		StringJoiner sj = new StringJoiner("\n");
		String content = sj.add("SET UTF-8")
			.add("SFX A Y 1")
			.add("SFX A 0 a")
			.add("SFX B Y 1")
			.add("SFX B 0 b/A")
			.add("SFX C Y 1")
			.add("SFX C 0 c/E")
			.add("SFX D Y 1")
			.add("SFX D 0 d/AE")
			.add("PFX E Y 1")
			.add("PFX E 0 e")
			.toString();
		File affFile = FileService.getTemporaryUTF8File(content);
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();

		String line = "a/ABCDE";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(14, stems.size());
		//base production
		Assert.assertEquals(new Production("a", "ABCDE", "st:a", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("aa", "E", "st:a", strategy), stems.get(1));
		Assert.assertEquals(new Production("ab", "AE", "st:a", strategy), stems.get(2));
		Assert.assertEquals(new Production("ac", "E", "st:a", strategy), stems.get(3));
		Assert.assertEquals(new Production("ad", "AE", "st:a", strategy), stems.get(4));
		//twofold productions
		Assert.assertEquals(new Production("aba", "E", "st:a", strategy), stems.get(5));
		Assert.assertEquals(new Production("ada", "E", "st:a", strategy), stems.get(6));
		//lastfold productions
		Assert.assertEquals(new Production("ea", null, "st:a", strategy), stems.get(7));
		Assert.assertEquals(new Production("eaa", null, "st:a", strategy), stems.get(8));
		Assert.assertEquals(new Production("eab", null, "st:a", strategy), stems.get(9));
		Assert.assertEquals(new Production("eac", null, "st:a", strategy), stems.get(10));
		Assert.assertEquals(new Production("ead", null, "st:a", strategy), stems.get(11));
		Assert.assertEquals(new Production("eaba", null, "st:a", strategy), stems.get(12));
		Assert.assertEquals(new Production("eada", null, "st:a", strategy), stems.get(13));
	}


	@Test(expected = IllegalArgumentException.class)
	public void stemsInvalidFullstrip() throws IOException{
		StringJoiner sj = new StringJoiner("\n");
		String content = sj.add("SET UTF-8")
			.add("SFX A Y 1")
			.add("SFX A a b a")
			.toString();
		File affFile = FileService.getTemporaryUTF8File(content);
		affParser.parse(affFile);

		String line = "a/A";
		wordGenerator.applyRules(line);
	}

	@Test
	public void stemsValidFullstrip() throws IOException{
		StringJoiner sj = new StringJoiner("\n");
		String content = sj.add("SET UTF-8")
			.add("FULLSTRIP")
			.add("SFX A Y 1")
			.add("SFX A a b a")
			.toString();
		File affFile = FileService.getTemporaryUTF8File(content);
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();

		String line = "a/A";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(2, stems.size());
		//base production
		Assert.assertEquals(new Production("a", "A", "st:a", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("b", null, "st:a", strategy), stems.get(1));
	}


	@Test(expected = IllegalArgumentException.class)
	public void stemsInvalidTwofold1() throws IOException{
		StringJoiner sj = new StringJoiner("\n");
		String content = sj.add("SET UTF-8")
			.add("FLAG long")
			.add("SFX S1 Y 1")
			.add("SFX S1 0 s1/S2P1")
			.add("SFX S2 Y 1")
			.add("SFX S2 0 s2/S3")
			.add("SFX S3 Y 1")
			.add("SFX S3 0 s3")
			.add("PFX P1 Y 1")
			.add("PFX P1 0 p1/P2")
			.add("PFX P2 Y 1")
			.add("PFX P2 0 p2")
			.toString();
		File affFile = FileService.getTemporaryUTF8File(content);
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();

		String line = "aa/S1";
		wordGenerator.applyRules(line);
	}

	@Test(expected = IllegalArgumentException.class)
	public void stemsInvalidTwofold() throws IOException{
		StringJoiner sj = new StringJoiner("\n");
		String content = sj.add("SET UTF-8")
			.add("SFX A Y 1")
			.add("SFX A 0 a")
			.add("SFX B Y 1")
			.add("SFX B 0 b/A")
			.add("SFX C Y 1")
			.add("SFX C 0 c/E")
			.add("SFX D Y 1")
			.add("SFX D 0 d/AE")
			.add("PFX E Y 1")
			.add("PFX E 0 e")
			.add("PFX F Y 1")
			.add("PFX F 0 f/A")
			.add("PFX G Y 1")
			.add("PFX G 0 g/E")
			.add("PFX H Y 1")
			.add("PFX H 0 h/AE")
			.toString();
		File affFile = FileService.getTemporaryUTF8File(content);
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();

		String line = "a/ABCDEFGH";
		wordGenerator.applyRules(line);
	}

	@Test
	public void stemsComplexPrefixes() throws IOException{
		StringJoiner sj = new StringJoiner("\n");
		String content = sj.add("SET UTF-8")
			.add("COMPLEXPREFIXES")
			.add("PFX A Y 1")
			.add("PFX A 0 a")
			.add("PFX B Y 1")
			.add("PFX B 0 b/A")
			.add("PFX C Y 1")
			.add("PFX C 0 c/E")
			.add("PFX D Y 1")
			.add("PFX D 0 d/AE")
			.add("SFX E Y 1")
			.add("SFX E 0 e")
			.toString();
		File affFile = FileService.getTemporaryUTF8File(content);
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();

		String line = "a/ABCDE";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(14, stems.size());
		//base production
		Assert.assertEquals(new Production("a", "ABCDE", "st:a", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("aa", "E", "st:a", strategy), stems.get(1));
		Assert.assertEquals(new Production("ba", "AE", "st:a", strategy), stems.get(2));
		Assert.assertEquals(new Production("ca", "E", "st:a", strategy), stems.get(3));
		Assert.assertEquals(new Production("da", "AE", "st:a", strategy), stems.get(4));
		//twofold productions
		Assert.assertEquals(new Production("aba", "E", "st:a", strategy), stems.get(5));
		Assert.assertEquals(new Production("ada", "E", "st:a", strategy), stems.get(6));
		//lastfold productions
		Assert.assertEquals(new Production("ae", null, "st:a", strategy), stems.get(7));
		Assert.assertEquals(new Production("aae", null, "st:a", strategy), stems.get(8));
		Assert.assertEquals(new Production("bae", null, "st:a", strategy), stems.get(9));
		Assert.assertEquals(new Production("cae", null, "st:a", strategy), stems.get(10));
		Assert.assertEquals(new Production("dae", null, "st:a", strategy), stems.get(11));
		Assert.assertEquals(new Production("abae", null, "st:a", strategy), stems.get(12));
		Assert.assertEquals(new Production("adae", null, "st:a", strategy), stems.get(13));
	}

	@Test(expected = IllegalArgumentException.class)
	public void stemsInvalidTwofoldComplexPrefixes() throws IOException{
		StringJoiner sj = new StringJoiner("\n");
		String content = sj.add("SET UTF-8")
			.add("COMPLEXPREFIXES")
			.add("PFX A Y 1")
			.add("PFX A 0 a")
			.add("PFX B Y 1")
			.add("PFX B 0 b/A")
			.add("PFX C Y 1")
			.add("PFX C 0 c/E")
			.add("PFX D Y 1")
			.add("PFX D 0 d/AE")
			.add("SFX E Y 1")
			.add("SFX E 0 e")
			.add("SFX F Y 1")
			.add("SFX F 0 f/A")
			.add("SFX G Y 1")
			.add("SFX G 0 g/E")
			.add("SFX H Y 1")
			.add("SFX H 0 h/AE")
			.toString();
		File affFile = FileService.getTemporaryUTF8File(content);
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();

		String line = "a/ABCDEFGH";
		wordGenerator.applyRules(line);
	}


	@Test
	public void needAffix3() throws IOException{
		StringJoiner sj = new StringJoiner("\n");
		String content = sj.add("SET UTF-8")
			.add("NEEDAFFIX X")
			.add("SFX A Y 1")
			.add("SFX A 0 s/XB .")
			.add("SFX B Y 1")
			.add("SFX B 0 baz .")
			.toString();
		File affFile = FileService.getTemporaryUTF8File(content);
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();

		String line = "foo/A";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(2, stems.size());
		//base production
		Assert.assertEquals(new Production("foo", "A", "st:foo", strategy), stems.get(0));
		//onefold productions
		//twofold productions
		Assert.assertEquals(new Production("foosbaz", "X", "st:foo", strategy), stems.get(1));
		//lastfold productions
	}

	@Test
	public void needAffix5() throws IOException{
		StringJoiner sj = new StringJoiner("\n");
		String content = sj.add("SET UTF-8")
			.add("NEEDAFFIX X")
			.add("SFX A Y 2")
			.add("SFX A 0 -suf/B .")
			.add("SFX A 0 -pseudosuf/XB .")
			.add("SFX B Y 1")
			.add("SFX B 0 -bar .")
			.add("PFX C Y 2")
			.add("PFX C 0 pre- .")
			.add("PFX C 0 pseudopre-/X .")
			.toString();
		File affFile = FileService.getTemporaryUTF8File(content);
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();

		String line = "foo/AC";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(12, stems.size());
		//base production
		Assert.assertEquals(new Production("foo", "AC", "st:foo", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("foo-suf", "BC", "st:foo", strategy), stems.get(1));
		//twofold productions
		Assert.assertEquals(new Production("foo-suf-bar", "C", "st:foo", strategy), stems.get(2));
		Assert.assertEquals(new Production("foo-pseudosuf-bar", "CX", "st:foo", strategy), stems.get(3));
		//lastfold productions
		Assert.assertEquals(new Production("pre-foo", null, "st:foo", strategy), stems.get(4));
		Assert.assertEquals(new Production("pre-foo-suf", null, "st:foo", strategy), stems.get(5));
		Assert.assertEquals(new Production("pseudopre-foo-suf", "X", "st:foo", strategy), stems.get(6));
		Assert.assertEquals(new Production("pre-foo-pseudosuf", "X", "st:foo", strategy), stems.get(7));
		Assert.assertEquals(new Production("pre-foo-suf-bar", null, "st:foo", strategy), stems.get(8));
		Assert.assertEquals(new Production("pseudopre-foo-suf-bar", "X", "st:foo", strategy), stems.get(9));
		Assert.assertEquals(new Production("pre-foo-pseudosuf-bar", "X", "st:foo", strategy), stems.get(10));
		Assert.assertEquals(new Production("pseudopre-foo-pseudosuf-bar", "X", "st:foo", strategy), stems.get(11));
	}

	
	@Test
	public void circumfix() throws IOException{
		StringJoiner sj = new StringJoiner("\n");
		String content = sj.add("SET UTF-8")
			.add("CIRCUMFIX X")
			.add("PFX A Y 1")
			.add("PFX A 0 leg/X .")
			.add("PFX B Y 1")
			.add("PFX B 0 legesleg/X .")
			.add("SFX C Y 3")
			.add("SFX C 0 obb .")
			.add("SFX C 0 obb/AX .")
			.add("SFX C 0 obb/BX .")
			.toString();
		File affFile = FileService.getTemporaryUTF8File(content);
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();

		String line = "nagy/C";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(6, stems.size());
		//base production
		Assert.assertEquals(new Production("nagy", "C", "st:nagy", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("nagyobb", null, "st:nagy", strategy), stems.get(1));
		Assert.assertEquals(new Production("nagyobb", "AX", "st:nagy", strategy), stems.get(2));
		Assert.assertEquals(new Production("nagyobb", "BX", "st:nagy", strategy), stems.get(3));
		//twofold productions
		//lastfold productions
		Assert.assertEquals(new Production("legnagyobb", "X", "st:nagy", strategy), stems.get(4));
		Assert.assertEquals(new Production("legeslegnagyobb", "X", "st:nagy", strategy), stems.get(5));
	}

	
	@Test
	public void morphologicalAnalisys() throws IOException{
		StringJoiner sj = new StringJoiner("\n");
		String content = sj.add("SET UTF-8")
			.add("PFX P Y 1")
			.add("PFX P   0 un . dp:pfx_un sp:un")
			.add("SFX S Y 1")
			.add("SFX S   0 s . is:plur")
			.add("SFX Q Y 1")
			.add("SFX Q   0 s . is:sg_3")
			.add("SFX R Y 1")
			.add("SFX R   0 able/PS . ds:der_able")
			.toString();
		File affFile = FileService.getTemporaryUTF8File(content);
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();

		String line = "drink/S	po:noun";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(2, stems.size());
		//base production
		Assert.assertEquals(new Production("drink", "S", "st:drink po:noun", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("drinks", "", "st:drink po:noun is:plur", strategy), stems.get(1));
		//twofold productions
		//lastfold productions


		line = "drink/RQ	po:verb	al:drank	al:drunk	ts:present";
		stems = wordGenerator.applyRules(line);

		Assert.assertEquals(6, stems.size());
		//base production
		Assert.assertEquals(new Production("drink", "RQ", "st:drink po:verb al:drank al:drunk ts:present", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("drinkable", "PS", "st:drink po:verb	al:drank	al:drunk	ts:present ds:der_able", strategy), stems.get(1));
		Assert.assertEquals(new Production("drinks", "", "st:drink po:verb al:drank al:drunk ts:present is:sg_3", strategy), stems.get(2));
		//twofold productions
		//lastfold productions


		line = "eat/RQ	po:verb	al:ate	al:eaten	ts:present";
		stems = wordGenerator.applyRules(line);

		Assert.assertEquals(2, stems.size());
		//base production
		Assert.assertEquals(new Production("eat", "S", "st:eat po:verb", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("eats", "", "st:eat po:verb al:ate al:eaten ts:present is:singular_3rd", strategy), stems.get(1));
		//twofold productions
		//lastfold productions
	}

}
