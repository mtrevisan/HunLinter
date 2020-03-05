package unit731.hunlinter.parsers.dictionary.generators;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.parsers.enums.AffixOption;
import unit731.hunlinter.parsers.affix.ConversionTable;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.FileHelper;


/** @see <a href="https://github.com/hunspell/hunspell/tree/master/tests/v1cmdline">Hunspell tests</a> */
class WordGeneratorAffixTest extends TestBase{

	@Test
	void affFormat() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"# Testing also whitespace and comments.",
			"OCONV 1",
			"OCONV é É 	",
			"",
			" # space",
			"  # 2xspace",
			"	# tab",
			"		# 2xtab",
			" 	# space+tab",
			"	 # tab+space");
		loadData(affFile, language);

		ConversionTable table = affixData.getData(AffixOption.OUTPUT_CONVERSION_TABLE);
		Assertions.assertEquals("[affixOption=OUTPUT_CONVERSION_TABLE,table={  =[(é,É)]}]", table.toString());
	}


	@Test
	void flagUTF8() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
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
		loadData(affFile, language);

		String line = "foo/AÜ";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(8, words.size());
		//base production
		Assertions.assertEquals(createProduction("foo", "AÜ", "st:foo"), words.get(0));
		//suffix productions
		Assertions.assertEquals(createProduction("foos", "ÖÜü", "st:foo"), words.get(1));
		//prefix productions
		Assertions.assertEquals(createProduction("foosbar", "Ü", "st:foo"), words.get(2));
		Assertions.assertEquals(createProduction("foosbaz", "Ü", "st:foo"), words.get(3));
		//twofold productions
		Assertions.assertEquals(createProduction("unfoo", "A", "st:foo"), words.get(4));
		Assertions.assertEquals(createProduction("unfoos", "Öü", "st:foo"), words.get(5));
		Assertions.assertEquals(createProduction("unfoosbar", null, "st:foo"), words.get(6));
		Assertions.assertEquals(createProduction("unfoosbaz", null, "st:foo"), words.get(7));
	}

	@Test
	void flagNumerical() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
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
		loadData(affFile, language);

		String line = "foo/999,54321";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(8, words.size());
		//base production
		Assertions.assertEquals(createProduction("foo", "999,54321", "st:foo"), words.get(0));
		//suffix productions
		Assertions.assertEquals(createProduction("foos", "54321,214,216", "st:foo"), words.get(1));
		//prefix productions
		Assertions.assertEquals(createProduction("foosbar", "54321", "st:foo"), words.get(2));
		Assertions.assertEquals(createProduction("foosbaz", "54321", "st:foo"), words.get(3));
		//twofold productions
		Assertions.assertEquals(createProduction("unfoo", "999", "st:foo"), words.get(4));
		Assertions.assertEquals(createProduction("unfoos", "214,216", "st:foo"), words.get(5));
		Assertions.assertEquals(createProduction("unfoosbar", null, "st:foo"), words.get(6));
		Assertions.assertEquals(createProduction("unfoosbaz", null, "st:foo"), words.get(7));
	}

	@Test
	void flagASCII() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"SFX A Y 1",
			"SFX A 0 s/123 .",
			"SFX 1 Y 1",
			"SFX 1 0 bar .",
			"SFX 2 Y 1",
			"SFX 2 0 baz .",
			"PFX 3 Y 1",
			"PFX 3 0 un .");
		loadData(affFile, language);

		String line = "foo/A3";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(8, words.size());
		//base production
		Assertions.assertEquals(createProduction("foo", "A3", "st:foo"), words.get(0));
		//suffix productions
		Assertions.assertEquals(createProduction("foos", "123", "st:foo"), words.get(1));
		//prefix productions
		Assertions.assertEquals(createProduction("foosbar", "3", "st:foo"), words.get(2));
		Assertions.assertEquals(createProduction("foosbaz", "3", "st:foo"), words.get(3));
		//twofold productions
		Assertions.assertEquals(createProduction("unfoo", "A", "st:foo"), words.get(4));
		Assertions.assertEquals(createProduction("unfoos", "12", "st:foo"), words.get(5));
		Assertions.assertEquals(createProduction("unfoosbar", null, "st:foo"), words.get(6));
		Assertions.assertEquals(createProduction("unfoosbaz", null, "st:foo"), words.get(7));
	}

	@Test
	void flagDoubleASCII() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
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
		loadData(affFile, language);

		String line = "foo/zx09";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(8, words.size());
		//base production
		Assertions.assertEquals(createProduction("foo", "zx09", "st:foo"), words.get(0));
		//suffix productions
		Assertions.assertEquals(createProduction("foos", "1Gg?09", "st:foo"), words.get(1));
		//prefix productions
		Assertions.assertEquals(createProduction("foosbaz", "09", "st:foo"), words.get(2));
		Assertions.assertEquals(createProduction("foosbar", "09", "st:foo"), words.get(3));
		//twofold productions
		Assertions.assertEquals(createProduction("unfoo", "zx", "st:foo"), words.get(4));
		Assertions.assertEquals(createProduction("unfoos", "1Gg?", "st:foo"), words.get(5));
		Assertions.assertEquals(createProduction("unfoosbaz", null, "st:foo"), words.get(6));
		Assertions.assertEquals(createProduction("unfoosbar", null, "st:foo"), words.get(7));
	}


	@Test
	void conditions() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"SFX A Y 6",
			"SFX A 0 a .",
			"SFX A 0 b b",
			"SFX A 0 c [ab]",
			"SFX A 0 d [^ab]",
			"SFX A 0 e [^c]",
			"SFX A 0 f a[^ab]b");
		loadData(affFile, language);

		String line = "a/A";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(4, words.size());
		//base production
		Assertions.assertEquals(createProduction("a", "A", "st:a"), words.get(0));
		//suffix productions
		Assertions.assertEquals(createProduction("aa", null, "st:a"), words.get(1));
		Assertions.assertEquals(createProduction("ac", null, "st:a"), words.get(2));
		Assertions.assertEquals(createProduction("ae", null, "st:a"), words.get(3));
	}


	@Test
	void stems1() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"FLAG long",
			"SFX S1 Y 1",
			"SFX S1 0 s1/S2P1",
			"SFX S2 Y 1",
			"SFX S2 0 s2",
			"PFX P1 Y 1",
			"PFX P1 0 p1");
		loadData(affFile, language);

		String line = "aa/S1";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(5, words.size());
		//base production
		Assertions.assertEquals(createProduction("aa", "S1", "st:aa"), words.get(0));
		//suffix productions
		Assertions.assertEquals(createProduction("aas1", "P1S2", "st:aa"), words.get(1));
		//prefix productions
		Assertions.assertEquals(createProduction("aas1s2", "P1", "st:aa"), words.get(2));
		//twofold productions
		Assertions.assertEquals(createProduction("p1aas1", "S2", "st:aa"), words.get(3));
		Assertions.assertEquals(createProduction("p1aas1s2", null, "st:aa"), words.get(4));
	}

	@Test
	void stems2() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"FLAG long",
			"SFX S1 Y 1",
			"SFX S1 0 s1/S2",
			"SFX S2 Y 1",
			"SFX S2 0 s2/P1",
			"PFX P1 Y 1",
			"PFX P1 0 p1");
		loadData(affFile, language);

		String line = "aa/S1";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(4, words.size());
		//base production
		Assertions.assertEquals(createProduction("aa", "S1", "st:aa"), words.get(0));
		//suffix productions
		Assertions.assertEquals(createProduction("aas1", "S2", "st:aa"), words.get(1));
		//prefix productions
		//twofold productions
		Assertions.assertEquals(createProduction("aas1s2", "P1", "st:aa"), words.get(2));
		Assertions.assertEquals(createProduction("p1aas1s2", null, "st:aa"), words.get(3));
	}

	@Test
	void stems3() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"FLAG long",
			"SFX S1 Y 1",
			"SFX S1 0 s1/S2",
			"SFX S2 Y 1",
			"SFX S2 0 s2",
			"PFX P1 Y 1",
			"PFX P1 0 p1");
		loadData(affFile, language);

		String line = "aa/S1P1";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(6, words.size());
		//base production
		Assertions.assertEquals(createProduction("aa", "S1P1", "st:aa"), words.get(0));
		//suffix productions
		Assertions.assertEquals(createProduction("aas1", "P1S2", "st:aa"), words.get(1));
		//prefix productions
		Assertions.assertEquals(createProduction("aas1s2", "P1", "st:aa"), words.get(2));
		//twofold productions
		Assertions.assertEquals(createProduction("p1aa", "S1", "st:aa"), words.get(3));
		Assertions.assertEquals(createProduction("p1aas1", "S2", "st:aa"), words.get(4));
		Assertions.assertEquals(createProduction("p1aas1s2", null, "st:aa"), words.get(5));
	}

	@Test
	void stems4() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"FLAG long",
			"SFX S1 Y 1",
			"SFX S1 0 s1/S2",
			"SFX S2 Y 1",
			"SFX S2 0 s2",
			"PFX P1 Y 1",
			"PFX P1 0 p1");
		loadData(affFile, language);

		String line = "aa/P1S1";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(6, words.size());
		//base production
		Assertions.assertEquals(createProduction("aa", "P1S1", "st:aa"), words.get(0));
		//suffix productions
		Assertions.assertEquals(createProduction("aas1", "P1S2", "st:aa"), words.get(1));
		//prefix productions
		Assertions.assertEquals(createProduction("aas1s2", "P1", "st:aa"), words.get(2));
		//twofold productions
		Assertions.assertEquals(createProduction("p1aa", "S1", "st:aa"), words.get(3));
		Assertions.assertEquals(createProduction("p1aas1", "S2", "st:aa"), words.get(4));
		Assertions.assertEquals(createProduction("p1aas1s2", null, "st:aa"), words.get(5));
	}

	@Test
	void stems5() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
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
		loadData(affFile, language);

		String line = "a/ABCDE";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(14, words.size());
		//base production
		Assertions.assertEquals(createProduction("a", "ABCDE", "st:a"), words.get(0));
		//suffix productions
		Assertions.assertEquals(createProduction("aa", "E", "st:a"), words.get(1));
		Assertions.assertEquals(createProduction("ab", "AE", "st:a"), words.get(2));
		Assertions.assertEquals(createProduction("ac", "E", "st:a"), words.get(3));
		Assertions.assertEquals(createProduction("ad", "AE", "st:a"), words.get(4));
		//prefix productions
		Assertions.assertEquals(createProduction("aba", "E", "st:a"), words.get(5));
		Assertions.assertEquals(createProduction("ada", "E", "st:a"), words.get(6));
		//twofold productions
		Assertions.assertEquals(createProduction("ea", "ABCD", "st:a"), words.get(7));
		Assertions.assertEquals(createProduction("eaa", null, "st:a"), words.get(8));
		Assertions.assertEquals(createProduction("eab", "A", "st:a"), words.get(9));
		Assertions.assertEquals(createProduction("eac", null, "st:a"), words.get(10));
		Assertions.assertEquals(createProduction("ead", "A", "st:a"), words.get(11));
		Assertions.assertEquals(createProduction("eaba", null, "st:a"), words.get(12));
		Assertions.assertEquals(createProduction("eada", null, "st:a"), words.get(13));
	}


	@Test
	void stemsInvalidFullstrip(){
		Throwable exception = Assertions.assertThrows(LinterException.class, () -> {
			String language = "xxx";
			File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
				"SET UTF-8",
				"SFX A Y 1",
				"SFX A a b a");
			loadData(affFile, language);

			String line = "a/A";
			DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
			wordGenerator.applyAffixRules(dicEntry);
		});
		Assertions.assertEquals("Cannot strip full word 'a' without the FULLSTRIP option", exception.getMessage());
	}

	@Test
	void stemsValidFullstrip() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"FULLSTRIP",
			"SFX A Y 1",
			"SFX A a b a");
		loadData(affFile, language);

		String line = "a/A";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(2, words.size());
		//base production
		Assertions.assertEquals(createProduction("a", "A", "st:a"), words.get(0));
		//suffix productions
		Assertions.assertEquals(createProduction("b", null, "st:a"), words.get(1));
	}


	@Test
	void stemsInvalidTwofold1(){
		Throwable exception = Assertions.assertThrows(LinterException.class, () -> {
			String language = "xxx";
			File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
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
			loadData(affFile, language);

			String line = "aa/S1";
			DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
			wordGenerator.applyAffixRules(dicEntry);
		});
		Assertions.assertEquals("Twofold rule violated for 'p1aas1/P2,S2	st:aa	from	S1 > P1 from S1 > P1' (S1 > P1 still has rules P2)", exception.getMessage());
	}

	@Test
	void stemsInvalidTwofold2(){
		Throwable exception = Assertions.assertThrows(LinterException.class, () -> {
			String language = "xxx";
			File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
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
			loadData(affFile, language);

			String line = "a/ABCDEFGH";
			DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
			wordGenerator.applyAffixRules(dicEntry);
		});
		Assertions.assertEquals("Twofold rule violated for 'ga/A,B,C,D,E	st:a	from	G from G' (G still has rules E)", exception.getMessage());
	}


	@Test
	void complexPrefixes1() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
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
		loadData(affFile, language);

		String line = "a/ABCDE";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(14, words.size());
		//base production
		Assertions.assertEquals(createProduction("a", "ABCDE", "st:a"), words.get(0));
		//suffix productions
		Assertions.assertEquals(createProduction("aa", "E", "st:a"), words.get(1));
		Assertions.assertEquals(createProduction("ba", "AE", "st:a"), words.get(2));
		Assertions.assertEquals(createProduction("ca", "E", "st:a"), words.get(3));
		Assertions.assertEquals(createProduction("da", "AE", "st:a"), words.get(4));
		//prefix productions
		Assertions.assertEquals(createProduction("aba", "E", "st:a"), words.get(5));
		Assertions.assertEquals(createProduction("ada", "E", "st:a"), words.get(6));
		//twofold productions
		Assertions.assertEquals(createProduction("ae", "ABCD", "st:a"), words.get(7));
		Assertions.assertEquals(createProduction("aae", null, "st:a"), words.get(8));
		Assertions.assertEquals(createProduction("bae", "A", "st:a"), words.get(9));
		Assertions.assertEquals(createProduction("cae", null, "st:a"), words.get(10));
		Assertions.assertEquals(createProduction("dae", "A", "st:a"), words.get(11));
		Assertions.assertEquals(createProduction("abae", null, "st:a"), words.get(12));
		Assertions.assertEquals(createProduction("adae", null, "st:a"), words.get(13));
	}

	@Test
	void complexPrefixes2() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPLEXPREFIXES",
			"PFX A Y 1",
			"PFX A 0 tek .",
			"PFX B Y 1",
			"PFX B 0 met/A .");
		loadData(affFile, language);

		String line = "ouro/B";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(3, words.size());
		//base production
		Assertions.assertEquals(createProduction("ouro", "B", "st:ouro"), words.get(0));
		//suffix productions
		Assertions.assertEquals(createProduction("metouro", "A", "st:ouro"), words.get(1));
		//prefix productions
		Assertions.assertEquals(createProduction("tekmetouro", null, "st:ouro"), words.get(2));
		//twofold productions
	}

	@Test
	void complexPrefixesUTF8() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPLEXPREFIXES",
			"PFX A Y 1",
			"PFX A 0 ⲧⲉⲕ .",
			"PFX B Y 1",
			"PFX B 0 ⲙⲉⲧ/A .");
		loadData(affFile, language);

		String line = "ⲟⲩⲣⲟ/B";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(3, words.size());
		//base production
		Assertions.assertEquals(createProduction("ⲟⲩⲣⲟ", "B", "st:ⲟⲩⲣⲟ"), words.get(0));
		//suffix productions
		Assertions.assertEquals(createProduction("ⲙⲉⲧⲟⲩⲣⲟ", "A", "st:ⲟⲩⲣⲟ"), words.get(1));
		//prefix productions
		Assertions.assertEquals(createProduction("ⲧⲉⲕⲙⲉⲧⲟⲩⲣⲟ", null, "st:ⲟⲩⲣⲟ"), words.get(2));
		//twofold productions
	}

	@Test
	void complexPrefixesInvalidTwofold(){
		Throwable exception = Assertions.assertThrows(LinterException.class, () -> {
			String language = "xxx";
			File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
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
			loadData(affFile, language);

			String line = "a/ABCDEFGH";
			DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
			List<Production> words = wordGenerator.applyAffixRules(dicEntry);
		});
		Assertions.assertEquals("Twofold rule violated for 'ag/A,B,C,D,E	st:a	from	G from G' (G still has rules E)", exception.getMessage());
	}


	@Test
	void needAffix3() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"NEEDAFFIX X",
			"SFX A Y 1",
			"SFX A 0 s/XB .",
			"SFX B Y 1",
			"SFX B 0 baz .");
		loadData(affFile, language);

		String line = "foo/A";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(2, words.size());
		//base production
		Assertions.assertEquals(createProduction("foo", "A", "st:foo"), words.get(0));
		//suffix productions
		//prefix productions
		Assertions.assertEquals(createProduction("foosbaz", null, "st:foo"), words.get(1));
		//twofold productions
	}

	@Test
	void needAffix5() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
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
		loadData(affFile, language);

		String line = "foo/AC";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(9, words.size());
		//base production
		Assertions.assertEquals(createProduction("foo", "AC", "st:foo"), words.get(0));
		//suffix productions
		Assertions.assertEquals(createProduction("foo-suf", "BC", "st:foo"), words.get(1));
		//prefix productions
		Assertions.assertEquals(createProduction("foo-suf-bar", "C", "st:foo"), words.get(2));
		Assertions.assertEquals(createProduction("foo-pseudosuf-bar", "C", "st:foo"), words.get(3));
		//twofold productions
		Assertions.assertEquals(createProduction("pre-foo", "A", "st:foo"), words.get(4));
		Assertions.assertEquals(createProduction("pre-foo-suf", "B", "st:foo"), words.get(5));
		Assertions.assertEquals(createProduction("pre-foo-pseudosuf", "B", "st:foo"), words.get(6));
		Assertions.assertEquals(createProduction("pre-foo-suf-bar", null, "st:foo"), words.get(7));
		Assertions.assertEquals(createProduction("pre-foo-pseudosuf-bar", null, "st:foo"), words.get(8));
	}


	@Test
	void circumfix1() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
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
		loadData(affFile, language);

		String line = "nagy/C";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(4, words.size());
		//base production
		Assertions.assertEquals(createProduction("nagy", "C", "st:nagy"), words.get(0));
		//suffix productions
		Assertions.assertEquals(createProduction("nagyobb", null, "st:nagy"), words.get(1));
		//prefix productions
		//twofold productions
		Assertions.assertEquals(createProduction("legnagyobb", null, "st:nagy"), words.get(2));
		Assertions.assertEquals(createProduction("legeslegnagyobb", null, "st:nagy"), words.get(3));
	}

	@Test
	void circumfix2() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
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
		loadData(affFile, language);

		String line = "nagy/CX";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(2, words.size());
		//base production
		//suffix productions
		//prefix productions
		//twofold productions
		Assertions.assertEquals(createProduction("legnagyobb", null, "st:nagy"), words.get(0));
		Assertions.assertEquals(createProduction("legeslegnagyobb", null, "st:nagy"), words.get(1));
	}

	@Test
	void circumfix3() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"CIRCUMFIX X",
			"PFX a Y 4",
			"PFX a 0 a .",
			"PFX a 0 n .",
			"PFX a 0 t .",
			"PFX a 0 y .",
			"PFX c Y 5",
			"PFX c a g/X a[^y]",
			"PFX c a f/X a[^y]",
			"PFX c 0 t/X [^a]",
			"PFX c 0 lt/X [^a]",
			"PFX c 0 wlt/X [^a]",
			"PFX d Y 1",
			"PFX d 0 y/X .",
			"SFX b Y 1",
			"SFX b 0 i/cX .",
			"SFX e Y 2",
			"SFX e 0 un/cdX .",
			"SFX e 0 n/cdX .");
		loadData(affFile, language);

		String line = "bark/abe";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(16, words.size());
		//base production
		Assertions.assertEquals(createProduction("bark", "abe", "st:bark"), words.get(0));
		//suffix productions
		//twofold productions
		Assertions.assertEquals(createProduction("abark", "be", "st:bark"), words.get(1));
		Assertions.assertEquals(createProduction("nbark", "be", "st:bark"), words.get(2));
		Assertions.assertEquals(createProduction("tbark", "be", "st:bark"), words.get(3));
		Assertions.assertEquals(createProduction("ybark", "be", "st:bark"), words.get(4));
		Assertions.assertEquals(createProduction("tbarki", null, "st:bark"), words.get(5));
		Assertions.assertEquals(createProduction("ltbarki", null, "st:bark"), words.get(6));
		Assertions.assertEquals(createProduction("wltbarki", null, "st:bark"), words.get(7));
		Assertions.assertEquals(createProduction("tbarkun", null, "st:bark"), words.get(8));
		Assertions.assertEquals(createProduction("ltbarkun", null, "st:bark"), words.get(9));
		Assertions.assertEquals(createProduction("wltbarkun", null, "st:bark"), words.get(10));
		Assertions.assertEquals(createProduction("tbarkn", null, "st:bark"), words.get(11));
		Assertions.assertEquals(createProduction("ltbarkn", null, "st:bark"), words.get(12));
		Assertions.assertEquals(createProduction("wltbarkn", null, "st:bark"), words.get(13));
		Assertions.assertEquals(createProduction("ybarkun", null, "st:bark"), words.get(14));
		Assertions.assertEquals(createProduction("ybarkn", null, "st:bark"), words.get(15));
	}


	@Test
	void morphologicalAnalisys() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"PFX P Y 1",
			"PFX P 0 un . dp:pfx_un sp:un",
			"SFX S Y 1",
			"SFX S 0 s . is:plur",
			"SFX Q Y 1",
			"SFX Q 0 s . is:sg_3",
			"SFX R Y 1",
			"SFX R 0 able/PS . ds:der_able");
		loadData(affFile, language);

		String line = "drink/S	po:noun";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(2, words.size());
		//base production
		Assertions.assertEquals(createProduction("drink", "S", "st:drink po:noun"), words.get(0));
		//suffix productions
		Assertions.assertEquals(createProduction("drinks", null, "st:drink po:noun is:plur"), words.get(1));
		//prefix productions
		//twofold productions


		line = "drink/RQ	po:verb	al:drank	al:drunk	ts:present";
		dicEntry = wordGenerator.createFromDictionaryLine(line);
		words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(6, words.size());
		//base production
		Assertions.assertEquals(createProduction("drink", "RQ", "st:drink po:verb al:drank al:drunk ts:present"), words.get(0));
		//suffix productions
		Assertions.assertEquals(createProduction("drinks", null, "st:drink po:verb al:drank al:drunk is:sg_3"), words.get(1));
		Assertions.assertEquals(createProduction("drinkable", "PS", "st:drink po:verb al:drank al:drunk ts:present ds:der_able"), words.get(2));
		//prefix productions
		Assertions.assertEquals(createProduction("drinkables", "P", "st:drink po:verb al:drank al:drunk ds:der_able is:plur"),
			words.get(3));
		//twofold productions
		Assertions.assertEquals(createProduction("undrinkable", "S", "dp:pfx_un sp:un st:drink po:verb al:drank al:drunk ts:present ds:der_able"),
			words.get(4));
		Assertions.assertEquals(createProduction("undrinkables", null, "dp:pfx_un sp:un st:drink po:verb al:drank al:drunk ds:der_able is:plur"),
			words.get(5));
	}


	@Test
	void alias1() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"AF 2",
			"AF AB",
			"AF A",
			"SFX A Y 1",
			"SFX A 0 x .",
			"SFX B Y 1",
			"SFX B 0 y/2 .");
		loadData(affFile, language);


		String line = "foo/1";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(4, words.size());
		//base production
		Assertions.assertEquals(createProduction("foo", "AB", "st:foo"), words.get(0));
		//suffix productions
		Assertions.assertEquals(createProduction("foox", null, "st:foo"), words.get(1));
		Assertions.assertEquals(createProduction("fooy", "A", "st:foo"), words.get(2));
		//prefix productions
		Assertions.assertEquals(createProduction("fooyx", null, "st:foo"), words.get(3));
		//twofold productions
	}


	@Test
	void escapeSlash() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"SFX A Y 1",
			"SFX A 0 x .",
			"SFX B Y 1",
			"SFX B 0 y\\/z .");
		loadData(affFile, language);


		String line = "foo\\/bar/AB";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(3, words.size());
		//base production
		Assertions.assertEquals(createProduction("foo/bar", "AB", "st:foo/bar"), words.get(0));
		//suffix productions
		Assertions.assertEquals(createProduction("foo/barx", null, "st:foo/bar"), words.get(1));
		Assertions.assertEquals(createProduction("foo/bary/z", null, "st:foo/bar"), words.get(2));
		//prefix productions
		//twofold productions
	}


	@Test
	void forbiddenWord() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"FORBIDDENWORD !",
			"SFX s N 1",
			"SFX s 0 os .");
		loadData(affFile, language);

		String line = "forbidden/!s";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertTrue(words.isEmpty());
	}

}
