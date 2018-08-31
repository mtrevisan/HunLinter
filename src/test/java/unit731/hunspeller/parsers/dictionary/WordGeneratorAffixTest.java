package unit731.hunspeller.parsers.dictionary;

import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.FileHelper;


/** @see <a href="https://github.com/hunspell/hunspell/tree/master/tests/v1cmdline">Hunspell tests</a> */
public class WordGeneratorAffixTest{

	private final Backbone backbone = new Backbone(null, null);


	private void loadData(String affixFilePath) throws IOException{
		backbone.loadFile(affixFilePath);
	}

	private Production createProduction(String word, String continuationFlags, String morphologicalFields){
		FlagParsingStrategy strategy = backbone.getAffParser().getFlagParsingStrategy();
		return new Production(word, continuationFlags, morphologicalFields, strategy);
	}

	@Test
	public void affFormat() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"# Testing also whitespace and comments.",
			"OCONV 7 # space, space",
			"OCONV	a A # tab, space, space",
			"OCONV	á	Á # tab, tab, space",
			"OCONV	b	B	# tab, tab, tab",
			"OCONV  c  C		# 2xspace, 2xspace, 2xtab",
			"OCONV	 d 	D # tab+space, space+tab, space",
			"OCONV e E #",
			"OCONV é É 	",
			"",
			" # space",
			"  # 2xspace",
			"	# tab",
			"		# 2xtab",
			" 	# space+tab",
			"	 # tab+space");
		loadData(affFile.getAbsolutePath());

		List<Pair<String, String>> outputConversionTable = backbone.getAffParser().getData(AffixTag.OUTPUT_CONVERSION_TABLE);

		List<Pair<String, String>> expected = new ArrayList<>();
		expected.add(Pair.of("a", "A"));
		expected.add(Pair.of("á", "Á"));
		expected.add(Pair.of("b", "B"));
		expected.add(Pair.of("c", "C"));
		expected.add(Pair.of("d", "D"));
		expected.add(Pair.of("e", "E"));
		expected.add(Pair.of("é", "É"));
		Assert.assertEquals(expected, outputConversionTable);
	}


	@Test
	public void flagUTF8() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"FLAG UTF-8",
			"SFX A Y 1",
			"SFX A 0 s/ÖüÜ .",
			"SFX Ö Y 1",
			"SFX Ö 0 bar .",
			"SFX ü Y 1",
			"SFX ü 0 baz .",
			"PFX Ü Y 1",
			"PFX Ü 0 un .");
		loadData(affFile.getAbsolutePath());

		String line = "foo/AÜ";
		List<Production> words = backbone.getWordGenerator().applyAffixRules(line);

		Assert.assertEquals(8, words.size());
		//base production
		Assert.assertEquals(createProduction("foo", "AÜ", "st:foo"), words.get(0));
		//onefold productions
		Assert.assertEquals(createProduction("foos", "ÖÜü", "st:foo"), words.get(1));
		//twofold productions
		Assert.assertEquals(createProduction("foosbar", "Ü", "st:foo"), words.get(2));
		Assert.assertEquals(createProduction("foosbaz", "Ü", "st:foo"), words.get(3));
		//lastfold productions
		Assert.assertEquals(createProduction("unfoo", "A", "st:foo"), words.get(4));
		Assert.assertEquals(createProduction("unfoos", "Öü", "st:foo"), words.get(5));
		Assert.assertEquals(createProduction("unfoosbar", null, "st:foo"), words.get(6));
		Assert.assertEquals(createProduction("unfoosbaz", null, "st:foo"), words.get(7));
	}

	@Test
	public void flagNumerical() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"FLAG num",
			"SFX 999 Y 1",
			"SFX 999 0 s/214,216,54321 .",
			"SFX 214 Y 1",
			"SFX 214 0 bar .",
			"SFX 216 Y 1",
			"SFX 216 0 baz .",
			"PFX 54321 Y 1",
			"PFX 54321 0 un .");
		loadData(affFile.getAbsolutePath());

		String line = "foo/999,54321";
		List<Production> words = backbone.getWordGenerator().applyAffixRules(line);

		Assert.assertEquals(8, words.size());
		//base production
		Assert.assertEquals(createProduction("foo", "999,54321", "st:foo"), words.get(0));
		//onefold productions
		Assert.assertEquals(createProduction("foos", "54321,214,216", "st:foo"), words.get(1));
		//twofold productions
		Assert.assertEquals(createProduction("foosbar", "54321", "st:foo"), words.get(2));
		Assert.assertEquals(createProduction("foosbaz", "54321", "st:foo"), words.get(3));
		//lastfold productions
		Assert.assertEquals(createProduction("unfoo", "999", "st:foo"), words.get(4));
		Assert.assertEquals(createProduction("unfoos", "214,216", "st:foo"), words.get(5));
		Assert.assertEquals(createProduction("unfoosbar", null, "st:foo"), words.get(6));
		Assert.assertEquals(createProduction("unfoosbaz", null, "st:foo"), words.get(7));
	}

	@Test
	public void flagASCII() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"SFX A Y 1",
			"SFX A 0 s/123 .",
			"SFX 1 Y 1",
			"SFX 1 0 bar .",
			"SFX 2 Y 1",
			"SFX 2 0 baz .",
			"PFX 3 Y 1",
			"PFX 3 0 un .");
		loadData(affFile.getAbsolutePath());

		String line = "foo/A3";
		List<Production> words = backbone.getWordGenerator().applyAffixRules(line);

		Assert.assertEquals(8, words.size());
		//base production
		Assert.assertEquals(createProduction("foo", "A3", "st:foo"), words.get(0));
		//onefold productions
		Assert.assertEquals(createProduction("foos", "123", "st:foo"), words.get(1));
		//twofold productions
		Assert.assertEquals(createProduction("foosbar", "3", "st:foo"), words.get(2));
		Assert.assertEquals(createProduction("foosbaz", "3", "st:foo"), words.get(3));
		//lastfold productions
		Assert.assertEquals(createProduction("unfoo", "A", "st:foo"), words.get(4));
		Assert.assertEquals(createProduction("unfoos", "12", "st:foo"), words.get(5));
		Assert.assertEquals(createProduction("unfoosbar", null, "st:foo"), words.get(6));
		Assert.assertEquals(createProduction("unfoosbaz", null, "st:foo"), words.get(7));
	}

	@Test
	public void flagDoubleASCII() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"FLAG long",
			"SFX zx Y 1",
			"SFX zx 0 s/g?1G09 .",
			"SFX g? Y 1",
			"SFX g? 0 bar .",
			"SFX 1G Y 1",
			"SFX 1G 0 baz .",
			"PFX 09 Y 1",
			"PFX 09 0 un .");
		loadData(affFile.getAbsolutePath());

		String line = "foo/zx09";
		List<Production> words = backbone.getWordGenerator().applyAffixRules(line);

		Assert.assertEquals(8, words.size());
		//base production
		Assert.assertEquals(createProduction("foo", "zx09", "st:foo"), words.get(0));
		//onefold productions
		Assert.assertEquals(createProduction("foos", "1Gg?09", "st:foo"), words.get(1));
		//twofold productions
		Assert.assertEquals(createProduction("foosbaz", "09", "st:foo"), words.get(2));
		Assert.assertEquals(createProduction("foosbar", "09", "st:foo"), words.get(3));
		//lastfold productions
		Assert.assertEquals(createProduction("unfoo", "zx", "st:foo"), words.get(4));
		Assert.assertEquals(createProduction("unfoos", "1Gg?", "st:foo"), words.get(5));
		Assert.assertEquals(createProduction("unfoosbaz", null, "st:foo"), words.get(6));
		Assert.assertEquals(createProduction("unfoosbar", null, "st:foo"), words.get(7));
	}


	@Test
	public void conditions() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"SFX A Y 6",
			"SFX A 0 a .",
			"SFX A 0 b b",
			"SFX A 0 c [ab]",
			"SFX A 0 d [^ab]",
			"SFX A 0 e [^c]",
			"SFX A 0 f a[^ab]b");
		loadData(affFile.getAbsolutePath());

		String line = "a/A";
		List<Production> words = backbone.getWordGenerator().applyAffixRules(line);

		Assert.assertEquals(4, words.size());
		//base production
		Assert.assertEquals(createProduction("a", "A", "st:a"), words.get(0));
		//onefold productions
		Assert.assertEquals(createProduction("aa", null, "st:a"), words.get(1));
		Assert.assertEquals(createProduction("ac", null, "st:a"), words.get(2));
		Assert.assertEquals(createProduction("ae", null, "st:a"), words.get(3));
	}


	@Test
	public void stems1() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"FLAG long",
			"SFX S1 Y 1",
			"SFX S1 0 s1/S2P1",
			"SFX S2 Y 1",
			"SFX S2 0 s2",
			"PFX P1 Y 1",
			"PFX P1 0 p1");
		loadData(affFile.getAbsolutePath());

		String line = "aa/S1";
		List<Production> words = backbone.getWordGenerator().applyAffixRules(line);

		Assert.assertEquals(5, words.size());
		//base production
		Assert.assertEquals(createProduction("aa", "S1", "st:aa"), words.get(0));
		//onefold productions
		Assert.assertEquals(createProduction("aas1", "P1S2", "st:aa"), words.get(1));
		//twofold productions
		Assert.assertEquals(createProduction("aas1s2", "P1", "st:aa"), words.get(2));
		//lastfold productions
		Assert.assertEquals(createProduction("p1aas1", "S2", "st:aa"), words.get(3));
		Assert.assertEquals(createProduction("p1aas1s2", null, "st:aa"), words.get(4));
	}

	@Test
	public void stems2() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"FLAG long",
			"SFX S1 Y 1",
			"SFX S1 0 s1/S2",
			"SFX S2 Y 1",
			"SFX S2 0 s2/P1",
			"PFX P1 Y 1",
			"PFX P1 0 p1");
		loadData(affFile.getAbsolutePath());

		String line = "aa/S1";
		List<Production> words = backbone.getWordGenerator().applyAffixRules(line);

		Assert.assertEquals(4, words.size());
		//base production
		Assert.assertEquals(createProduction("aa", "S1", "st:aa"), words.get(0));
		//onefold productions
		Assert.assertEquals(createProduction("aas1", "S2", "st:aa"), words.get(1));
		//twofold productions
		//lastfold productions
		Assert.assertEquals(createProduction("aas1s2", "P1", "st:aa"), words.get(2));
		Assert.assertEquals(createProduction("p1aas1s2", null, "st:aa"), words.get(3));
	}

	@Test
	public void stems3() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"FLAG long",
			"SFX S1 Y 1",
			"SFX S1 0 s1/S2",
			"SFX S2 Y 1",
			"SFX S2 0 s2",
			"PFX P1 Y 1",
			"PFX P1 0 p1");
		loadData(affFile.getAbsolutePath());

		String line = "aa/S1P1";
		List<Production> words = backbone.getWordGenerator().applyAffixRules(line);

		Assert.assertEquals(6, words.size());
		//base production
		Assert.assertEquals(createProduction("aa", "S1P1", "st:aa"), words.get(0));
		//onefold productions
		Assert.assertEquals(createProduction("aas1", "P1S2", "st:aa"), words.get(1));
		//twofold productions
		Assert.assertEquals(createProduction("aas1s2", "P1", "st:aa"), words.get(2));
		//lastfold productions
		Assert.assertEquals(createProduction("p1aa", "S1", "st:aa"), words.get(3));
		Assert.assertEquals(createProduction("p1aas1", "S2", "st:aa"), words.get(4));
		Assert.assertEquals(createProduction("p1aas1s2", null, "st:aa"), words.get(5));
	}

	@Test
	public void stems4() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"FLAG long",
			"SFX S1 Y 1",
			"SFX S1 0 s1/S2",
			"SFX S2 Y 1",
			"SFX S2 0 s2",
			"PFX P1 Y 1",
			"PFX P1 0 p1");
		loadData(affFile.getAbsolutePath());

		String line = "aa/P1S1";
		List<Production> words = backbone.getWordGenerator().applyAffixRules(line);

		Assert.assertEquals(6, words.size());
		//base production
		Assert.assertEquals(createProduction("aa", "P1S1", "st:aa"), words.get(0));
		//onefold productions
		Assert.assertEquals(createProduction("aas1", "P1S2", "st:aa"), words.get(1));
		//twofold productions
		Assert.assertEquals(createProduction("aas1s2", "P1", "st:aa"), words.get(2));
		//lastfold productions
		Assert.assertEquals(createProduction("p1aa", "S1", "st:aa"), words.get(3));
		Assert.assertEquals(createProduction("p1aas1", "S2", "st:aa"), words.get(4));
		Assert.assertEquals(createProduction("p1aas1s2", null, "st:aa"), words.get(5));
	}

	@Test
	public void stems5() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"SFX A Y 1",
			"SFX A 0 a",
			"SFX B Y 1",
			"SFX B 0 b/A",
			"SFX C Y 1",
			"SFX C 0 c/E",
			"SFX D Y 1",
			"SFX D 0 d/AE",
			"PFX E Y 1",
			"PFX E 0 e");
		loadData(affFile.getAbsolutePath());

		String line = "a/ABCDE";
		List<Production> words = backbone.getWordGenerator().applyAffixRules(line);

		Assert.assertEquals(14, words.size());
		//base production
		Assert.assertEquals(createProduction("a", "ABCDE", "st:a"), words.get(0));
		//onefold productions
		Assert.assertEquals(createProduction("aa", "E", "st:a"), words.get(1));
		Assert.assertEquals(createProduction("ab", "AE", "st:a"), words.get(2));
		Assert.assertEquals(createProduction("ac", "E", "st:a"), words.get(3));
		Assert.assertEquals(createProduction("ad", "AE", "st:a"), words.get(4));
		//twofold productions
		Assert.assertEquals(createProduction("aba", "E", "st:a"), words.get(5));
		Assert.assertEquals(createProduction("ada", "E", "st:a"), words.get(6));
		//lastfold productions
		Assert.assertEquals(createProduction("ea", "ABCD", "st:a"), words.get(7));
		Assert.assertEquals(createProduction("eaa", null, "st:a"), words.get(8));
		Assert.assertEquals(createProduction("eab", "A", "st:a"), words.get(9));
		Assert.assertEquals(createProduction("eac", null, "st:a"), words.get(10));
		Assert.assertEquals(createProduction("ead", "A", "st:a"), words.get(11));
		Assert.assertEquals(createProduction("eaba", null, "st:a"), words.get(12));
		Assert.assertEquals(createProduction("eada", null, "st:a"), words.get(13));
	}


	@Test(expected = IllegalArgumentException.class)
	public void stemsInvalidFullstrip() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"SFX A Y 1",
			"SFX A a b a");
		loadData(affFile.getAbsolutePath());

		String line = "a/A";
		backbone.getWordGenerator().applyAffixRules(line);
	}

	@Test
	public void stemsValidFullstrip() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"FULLSTRIP",
			"SFX A Y 1",
			"SFX A a b a");
		loadData(affFile.getAbsolutePath());

		String line = "a/A";
		List<Production> words = backbone.getWordGenerator().applyAffixRules(line);

		Assert.assertEquals(2, words.size());
		//base production
		Assert.assertEquals(createProduction("a", "A", "st:a"), words.get(0));
		//onefold productions
		Assert.assertEquals(createProduction("b", null, "st:a"), words.get(1));
	}


	@Test(expected = IllegalArgumentException.class)
	public void stemsInvalidTwofold1() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"FLAG long",
			"SFX S1 Y 1",
			"SFX S1 0 s1/S2P1",
			"SFX S2 Y 1",
			"SFX S2 0 s2/S3",
			"SFX S3 Y 1",
			"SFX S3 0 s3",
			"PFX P1 Y 1",
			"PFX P1 0 p1/P2",
			"PFX P2 Y 1",
			"PFX P2 0 p2");
		loadData(affFile.getAbsolutePath());

		String line = "aa/S1";
		backbone.getWordGenerator().applyAffixRules(line);
	}

	@Test(expected = IllegalArgumentException.class)
	public void stemsInvalidTwofold2() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"SFX A Y 1",
			"SFX A 0 a",
			"SFX B Y 1",
			"SFX B 0 b/A",
			"SFX C Y 1",
			"SFX C 0 c/E",
			"SFX D Y 1",
			"SFX D 0 d/AE",
			"PFX E Y 1",
			"PFX E 0 e",
			"PFX F Y 1",
			"PFX F 0 f/A",
			"PFX G Y 1",
			"PFX G 0 g/E",
			"PFX H Y 1",
			"PFX H 0 h/AE");
		loadData(affFile.getAbsolutePath());

		String line = "a/ABCDEFGH";
		backbone.getWordGenerator().applyAffixRules(line);
	}


	@Test
	public void complexPrefixes1() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"COMPLEXPREFIXES",
			"PFX A Y 1",
			"PFX A 0 a",
			"PFX B Y 1",
			"PFX B 0 b/A",
			"PFX C Y 1",
			"PFX C 0 c/E",
			"PFX D Y 1",
			"PFX D 0 d/AE",
			"SFX E Y 1",
			"SFX E 0 e");
		loadData(affFile.getAbsolutePath());

		String line = "a/ABCDE";
		List<Production> words = backbone.getWordGenerator().applyAffixRules(line);

		Assert.assertEquals(14, words.size());
		//base production
		Assert.assertEquals(createProduction("a", "ABCDE", "st:a"), words.get(0));
		//onefold productions
		Assert.assertEquals(createProduction("aa", "E", "st:a"), words.get(1));
		Assert.assertEquals(createProduction("ba", "AE", "st:a"), words.get(2));
		Assert.assertEquals(createProduction("ca", "E", "st:a"), words.get(3));
		Assert.assertEquals(createProduction("da", "AE", "st:a"), words.get(4));
		//twofold productions
		Assert.assertEquals(createProduction("aba", "E", "st:a"), words.get(5));
		Assert.assertEquals(createProduction("ada", "E", "st:a"), words.get(6));
		//lastfold productions
		Assert.assertEquals(createProduction("ae", "ABCD", "st:a"), words.get(7));
		Assert.assertEquals(createProduction("aae", null, "st:a"), words.get(8));
		Assert.assertEquals(createProduction("bae", "A", "st:a"), words.get(9));
		Assert.assertEquals(createProduction("cae", null, "st:a"), words.get(10));
		Assert.assertEquals(createProduction("dae", "A", "st:a"), words.get(11));
		Assert.assertEquals(createProduction("abae", null, "st:a"), words.get(12));
		Assert.assertEquals(createProduction("adae", null, "st:a"), words.get(13));
	}

	@Test
	public void complexPrefixes2() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"COMPLEXPREFIXES",
			"PFX A Y 1",
			"PFX A 0 tek .",
			"PFX B Y 1",
			"PFX B 0 met/A .");
		loadData(affFile.getAbsolutePath());

		String line = "ouro/B";
		List<Production> words = backbone.getWordGenerator().applyAffixRules(line);

		Assert.assertEquals(3, words.size());
		//base production
		Assert.assertEquals(createProduction("ouro", "B", "st:ouro"), words.get(0));
		//onefold productions
		Assert.assertEquals(createProduction("metouro", "A", "st:ouro"), words.get(1));
		//twofold productions
		Assert.assertEquals(createProduction("tekmetouro", null, "st:ouro"), words.get(2));
		//lastfold productions
	}

	@Test
	public void complexPrefixesUTF8() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"COMPLEXPREFIXES",
			"PFX A Y 1",
			"PFX A 0 ⲧⲉⲕ .",
			"PFX B Y 1",
			"PFX B 0 ⲙⲉⲧ/A .");
		loadData(affFile.getAbsolutePath());

		String line = "ⲟⲩⲣⲟ/B";
		List<Production> words = backbone.getWordGenerator().applyAffixRules(line);

		Assert.assertEquals(3, words.size());
		//base production
		Assert.assertEquals(createProduction("ⲟⲩⲣⲟ", "B", "st:ⲟⲩⲣⲟ"), words.get(0));
		//onefold productions
		Assert.assertEquals(createProduction("ⲙⲉⲧⲟⲩⲣⲟ", "A", "st:ⲟⲩⲣⲟ"), words.get(1));
		//twofold productions
		Assert.assertEquals(createProduction("ⲧⲉⲕⲙⲉⲧⲟⲩⲣⲟ", null, "st:ⲟⲩⲣⲟ"), words.get(2));
		//lastfold productions
	}

	@Test(expected = IllegalArgumentException.class)
	public void complexPrefixesInvalidTwofold() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"COMPLEXPREFIXES",
			"PFX A Y 1",
			"PFX A 0 a",
			"PFX B Y 1",
			"PFX B 0 b/A",
			"PFX C Y 1",
			"PFX C 0 c/E",
			"PFX D Y 1",
			"PFX D 0 d/AE",
			"SFX E Y 1",
			"SFX E 0 e",
			"SFX F Y 1",
			"SFX F 0 f/A",
			"SFX G Y 1",
			"SFX G 0 g/E",
			"SFX H Y 1",
			"SFX H 0 h/AE");
		loadData(affFile.getAbsolutePath());

		String line = "a/ABCDEFGH";
		backbone.getWordGenerator().applyAffixRules(line);
	}


	@Test
	public void needAffix3() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"NEEDAFFIX X",
			"SFX A Y 1",
			"SFX A 0 s/XB .",
			"SFX B Y 1",
			"SFX B 0 baz .");
		loadData(affFile.getAbsolutePath());

		String line = "foo/A";
		List<Production> words = backbone.getWordGenerator().applyAffixRules(line);

		Assert.assertEquals(2, words.size());
		//base production
		Assert.assertEquals(createProduction("foo", "A", "st:foo"), words.get(0));
		//onefold productions
		//twofold productions
		Assert.assertEquals(createProduction("foosbaz", null, "st:foo"), words.get(1));
		//lastfold productions
	}

	@Test
	public void needAffix5() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"NEEDAFFIX X",
			"SFX A Y 2",
			"SFX A 0 -suf/B .",
			"SFX A 0 -pseudosuf/XB .",
			"SFX B Y 1",
			"SFX B 0 -bar .",
			"PFX C Y 2",
			"PFX C 0 pre- .",
			"PFX C 0 pseudopre-/X .");
		loadData(affFile.getAbsolutePath());

		String line = "foo/AC";
		List<Production> words = backbone.getWordGenerator().applyAffixRules(line);

		Assert.assertEquals(12, words.size());
		//base production
		Assert.assertEquals(createProduction("foo", "AC", "st:foo"), words.get(0));
		//onefold productions
		Assert.assertEquals(createProduction("foo-suf", "BC", "st:foo"), words.get(1));
		//twofold productions
		Assert.assertEquals(createProduction("foo-suf-bar", "C", "st:foo"), words.get(2));
		Assert.assertEquals(createProduction("foo-pseudosuf-bar", "C", "st:foo"), words.get(3));
		//lastfold productions
		Assert.assertEquals(createProduction("pre-foo", "A", "st:foo"), words.get(4));
		Assert.assertEquals(createProduction("pre-foo-suf", "B", "st:foo"), words.get(5));
		Assert.assertEquals(createProduction("pseudopre-foo-suf", "BX", "st:foo"), words.get(6));
		Assert.assertEquals(createProduction("pre-foo-pseudosuf", "B", "st:foo"), words.get(7));
		Assert.assertEquals(createProduction("pre-foo-suf-bar", null, "st:foo"), words.get(8));
		Assert.assertEquals(createProduction("pseudopre-foo-suf-bar", "X", "st:foo"), words.get(9));
		Assert.assertEquals(createProduction("pre-foo-pseudosuf-bar", null, "st:foo"), words.get(10));
		Assert.assertEquals(createProduction("pseudopre-foo-pseudosuf-bar", "X", "st:foo"), words.get(11));
	}


	@Test
	public void circumfix() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"CIRCUMFIX X",
			"PFX A Y 1",
			"PFX A 0 leg/X .",
			"PFX B Y 1",
			"PFX B 0 legesleg/X .",
			"SFX C Y 3",
			"SFX C 0 obb .",
			"SFX C 0 obb/AX .",
			"SFX C 0 obb/BX .");
		loadData(affFile.getAbsolutePath());

		String line = "nagy/C";
		List<Production> words = backbone.getWordGenerator().applyAffixRules(line);

		Assert.assertEquals(6, words.size());
		//base production
		Assert.assertEquals(createProduction("nagy", "C", "st:nagy"), words.get(0));
		//onefold productions
		Assert.assertEquals(createProduction("nagyobb", null, "st:nagy"), words.get(1));
		Assert.assertEquals(createProduction("nagyobb", "AX", "st:nagy"), words.get(2));
		Assert.assertEquals(createProduction("nagyobb", "BX", "st:nagy"), words.get(3));
		//twofold productions
		//lastfold productions
		Assert.assertEquals(createProduction("legnagyobb", "X", "st:nagy"), words.get(4));
		Assert.assertEquals(createProduction("legeslegnagyobb", "X", "st:nagy"), words.get(5));
	}


	public void morphologicalAnalisys() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"PFX P Y 1",
			"PFX P 0 un . dp:pfx_un sp:un",
			"SFX S Y 1",
			"SFX S 0 s . is:plur",
			"SFX Q Y 1",
			"SFX Q 0 s . is:sg_3",
			"SFX R Y 1",
			"SFX R 0 able/PS . ds:der_able");
		loadData(affFile.getAbsolutePath());

		String line = "drink/S	po:noun";
		List<Production> words = backbone.getWordGenerator().applyAffixRules(line);

		Assert.assertEquals(2, words.size());
		//base production
		Assert.assertEquals(createProduction("drink", "S", "st:drink po:noun"), words.get(0));
		//onefold productions
		Assert.assertEquals(createProduction("drinks", null, "st:drink po:noun is:plur"), words.get(1));
		//twofold productions
		//lastfold productions


		line = "drink/RQ	po:verb	al:drank	al:drunk	ts:present";
		words = backbone.getWordGenerator().applyAffixRules(line);

		Assert.assertEquals(6, words.size());
		//base production
		Assert.assertEquals(createProduction("drink", "RQ", "st:drink po:verb al:drank al:drunk ts:present"), words.get(0));
		//onefold productions
		Assert.assertEquals(createProduction("drinkable", "PS", "st:drink po:verb al:drank al:drunk ts:present ds:der_able"), words.get(1));
		Assert.assertEquals(createProduction("drinks", null, "st:drink po:verb al:drank al:drunk ts:present is:sg_3"), words.get(2));
		//twofold productions
		Assert.assertEquals(createProduction("drinkables", "P", "st:drink po:verb al:drank al:drunk ts:present ds:der_able is:plur"),
			words.get(3));
		//lastfold productions
		Assert.assertEquals(createProduction("undrinkable", null, "dp:pfx_un sp:un st:drink po:verb al:drank al:drunk ts:present ds:der_able"),
			words.get(4));
		Assert.assertEquals(createProduction("undrinkables", null, "dp:pfx_un sp:un st:drink po:verb al:drank al:drunk ts:present ds:der_able is:plur"), words.get(5));
	}


	@Test
	public void alias1() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"AF 2",
			"AF AB",
			"AF A",
			"SFX A Y 1",
			"SFX A 0 x .",
			"SFX B Y 1",
			"SFX B 0 y/2 .");
		loadData(affFile.getAbsolutePath());


		String line = "foo/1";
		List<Production> words = backbone.getWordGenerator().applyAffixRules(line);

		Assert.assertEquals(4, words.size());
		//base production
		Assert.assertEquals(createProduction("foo", "AB", "st:foo"), words.get(0));
		//onefold productions
		Assert.assertEquals(createProduction("foox", null, "st:foo"), words.get(1));
		Assert.assertEquals(createProduction("fooy", "A", "st:foo"), words.get(2));
		//twofold productions
		Assert.assertEquals(createProduction("fooyx", null, "st:foo"), words.get(3));
		//lastfold productions
	}


	@Test
	public void escapeSlash() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"SFX A Y 1",
			"SFX A 0 x .",
			"SFX B Y 1",
			"SFX B 0 y\\/z .");
		loadData(affFile.getAbsolutePath());


		String line = "foo\\/bar/AB";
		List<Production> words = backbone.getWordGenerator().applyAffixRules(line);
		Assert.assertEquals(3, words.size());
		//base production
		Assert.assertEquals(createProduction("foo/bar", "AB", "st:foo/bar"), words.get(0));
		//onefold productions
		Assert.assertEquals(createProduction("foo/barx", null, "st:foo/bar"), words.get(1));
		Assert.assertEquals(createProduction("foo/bary/z", null, "st:foo/bar"), words.get(2));
		//twofold productions
		//lastfold productions
	}


	@Test
	public void forbiddenWord() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"FORBIDDENWORD !",
			"SFX s N 1",
			"SFX s 0 os .");
		loadData(affFile.getAbsolutePath());

		String line = "forbidden/!s";
		List<Production> words = backbone.getWordGenerator().applyAffixRules(line);
		Assert.assertTrue(words.isEmpty());
	}

}
