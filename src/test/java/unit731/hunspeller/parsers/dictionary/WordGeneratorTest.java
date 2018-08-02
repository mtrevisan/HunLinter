package unit731.hunspeller.parsers.dictionary;

import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.StringJoiner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import unit731.hunspeller.parsers.affix.AffixParser;
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
		strategy = affParser.getFlagParsingStrategy();
		wordGenerator = new WordGenerator(affParser);
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
		String line = "a/A";

		List<RuleProductionEntry> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(4, stems.size());
		//base production
		Assert.assertEquals(new RuleProductionEntry("a", "A", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new RuleProductionEntry("aa", "", strategy), stems.get(1));
		Assert.assertEquals(new RuleProductionEntry("ac", "", strategy), stems.get(2));
		Assert.assertEquals(new RuleProductionEntry("ae", "", strategy), stems.get(3));
	}

	@Test
	public void stems() throws IOException{
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
		String line = "a/ABCDE";

		List<RuleProductionEntry> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(12, stems.size());
		//base production
		Assert.assertEquals(new RuleProductionEntry("a", "ABCDE", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new RuleProductionEntry("aa", "E", strategy), stems.get(1));
		Assert.assertEquals(new RuleProductionEntry("ab", "AE", strategy), stems.get(2));
		Assert.assertEquals(new RuleProductionEntry("ac", "E", strategy), stems.get(3));
		Assert.assertEquals(new RuleProductionEntry("ad", "AE", strategy), stems.get(4));
		//twofold productions
		Assert.assertEquals(new RuleProductionEntry("aba", "", strategy), stems.get(5));
		Assert.assertEquals(new RuleProductionEntry("ada", "", strategy), stems.get(6));
		//lastfold productions
		Assert.assertEquals(new RuleProductionEntry("ea", "", strategy), stems.get(7));
		Assert.assertEquals(new RuleProductionEntry("eaa", "", strategy), stems.get(8));
		Assert.assertEquals(new RuleProductionEntry("eac", "", strategy), stems.get(10));
		Assert.assertEquals(new RuleProductionEntry("ead", "", strategy), stems.get(11));
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
		String line = "a/ABCDEFGH";

		List<RuleProductionEntry> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(27, stems.size());
		//base production
		Assert.assertEquals(new RuleProductionEntry("a", "ABCDEFGH", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new RuleProductionEntry("aa", "EFGH", strategy), stems.get(1));
		Assert.assertEquals(new RuleProductionEntry("ab", "AEFGH", strategy), stems.get(2));
		Assert.assertEquals(new RuleProductionEntry("ac", "EFGH", strategy), stems.get(3));
		Assert.assertEquals(new RuleProductionEntry("ad", "AEFGH", strategy), stems.get(4));
		//twofold productions
		Assert.assertEquals(new RuleProductionEntry("aba", "", strategy), stems.get(5));
		Assert.assertEquals(new RuleProductionEntry("ada", "", strategy), stems.get(6));
		//lastfold productions
		Assert.assertEquals(new RuleProductionEntry("ea", "", strategy), stems.get(7));
		Assert.assertEquals(new RuleProductionEntry("fa", "A", strategy), stems.get(8));
		Assert.assertEquals(new RuleProductionEntry("ga", "E", strategy), stems.get(9));
		Assert.assertEquals(new RuleProductionEntry("ha", "AE", strategy), stems.get(10));
		Assert.assertEquals(new RuleProductionEntry("eaa", "", strategy), stems.get(11));
		Assert.assertEquals(new RuleProductionEntry("faa", "A", strategy), stems.get(12));
		Assert.assertEquals(new RuleProductionEntry("gaa", "E", strategy), stems.get(13));
		Assert.assertEquals(new RuleProductionEntry("haa", "AE", strategy), stems.get(14));
		Assert.assertEquals(new RuleProductionEntry("eab", "", strategy), stems.get(15));
		Assert.assertEquals(new RuleProductionEntry("fab", "A", strategy), stems.get(16));
		Assert.assertEquals(new RuleProductionEntry("gab", "E", strategy), stems.get(17));
		Assert.assertEquals(new RuleProductionEntry("hab", "AE", strategy), stems.get(18));
		Assert.assertEquals(new RuleProductionEntry("eac", "", strategy), stems.get(19));
		Assert.assertEquals(new RuleProductionEntry("fac", "A", strategy), stems.get(20));
		Assert.assertEquals(new RuleProductionEntry("gac", "E", strategy), stems.get(21));
		Assert.assertEquals(new RuleProductionEntry("hac", "AE", strategy), stems.get(22));
		Assert.assertEquals(new RuleProductionEntry("ead", "", strategy), stems.get(23));
		Assert.assertEquals(new RuleProductionEntry("fad", "A", strategy), stems.get(24));
		Assert.assertEquals(new RuleProductionEntry("gad", "E", strategy), stems.get(25));
		Assert.assertEquals(new RuleProductionEntry("had", "AE", strategy), stems.get(26));
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
		String line = "a/ABCDE";

		List<RuleProductionEntry> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(12, stems.size());
		//base production
		Assert.assertEquals(new RuleProductionEntry("a", "ABCDE", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new RuleProductionEntry("aa", "E", strategy), stems.get(1));
		Assert.assertEquals(new RuleProductionEntry("ba", "AE", strategy), stems.get(2));
		Assert.assertEquals(new RuleProductionEntry("ca", "E", strategy), stems.get(3));
		Assert.assertEquals(new RuleProductionEntry("da", "AE", strategy), stems.get(4));
		//twofold productions
		Assert.assertEquals(new RuleProductionEntry("aba", "", strategy), stems.get(5));
		Assert.assertEquals(new RuleProductionEntry("ada", "", strategy), stems.get(6));
		//lastfold productions
		Assert.assertEquals(new RuleProductionEntry("ae", "", strategy), stems.get(7));
		Assert.assertEquals(new RuleProductionEntry("aae", "", strategy), stems.get(8));
		Assert.assertEquals(new RuleProductionEntry("cae", "", strategy), stems.get(10));
		Assert.assertEquals(new RuleProductionEntry("dae", "", strategy), stems.get(11));
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
		String line = "a/ABCDEFGH";

		List<RuleProductionEntry> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(27, stems.size());
		//base production
		Assert.assertEquals(new RuleProductionEntry("a", "ABCDEFGH", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new RuleProductionEntry("aa", "EFGH", strategy), stems.get(1));
		Assert.assertEquals(new RuleProductionEntry("ba", "AEFGH", strategy), stems.get(2));
		Assert.assertEquals(new RuleProductionEntry("ca", "EFGH", strategy), stems.get(3));
		Assert.assertEquals(new RuleProductionEntry("da", "AEFGH", strategy), stems.get(4));
		//twofold productions
		Assert.assertEquals(new RuleProductionEntry("aba", "", strategy), stems.get(5));
		Assert.assertEquals(new RuleProductionEntry("ada", "", strategy), stems.get(6));
		//lastfold productions
		Assert.assertEquals(new RuleProductionEntry("ae", "", strategy), stems.get(7));
		Assert.assertEquals(new RuleProductionEntry("af", "A", strategy), stems.get(8));
		Assert.assertEquals(new RuleProductionEntry("ag", "E", strategy), stems.get(9));
		Assert.assertEquals(new RuleProductionEntry("ah", "AE", strategy), stems.get(10));
		Assert.assertEquals(new RuleProductionEntry("aae", "", strategy), stems.get(11));
		Assert.assertEquals(new RuleProductionEntry("aaf", "A", strategy), stems.get(12));
		Assert.assertEquals(new RuleProductionEntry("aag", "E", strategy), stems.get(13));
		Assert.assertEquals(new RuleProductionEntry("aah", "AE", strategy), stems.get(14));
		Assert.assertEquals(new RuleProductionEntry("bae", "", strategy), stems.get(15));
		Assert.assertEquals(new RuleProductionEntry("baf", "A", strategy), stems.get(16));
		Assert.assertEquals(new RuleProductionEntry("bag", "E", strategy), stems.get(17));
		Assert.assertEquals(new RuleProductionEntry("bah", "AE", strategy), stems.get(18));
		Assert.assertEquals(new RuleProductionEntry("cae", "", strategy), stems.get(19));
		Assert.assertEquals(new RuleProductionEntry("caf", "A", strategy), stems.get(20));
		Assert.assertEquals(new RuleProductionEntry("cag", "E", strategy), stems.get(21));
		Assert.assertEquals(new RuleProductionEntry("cah", "AE", strategy), stems.get(22));
		Assert.assertEquals(new RuleProductionEntry("dae", "", strategy), stems.get(23));
		Assert.assertEquals(new RuleProductionEntry("daf", "A", strategy), stems.get(24));
		Assert.assertEquals(new RuleProductionEntry("dag", "E", strategy), stems.get(25));
		Assert.assertEquals(new RuleProductionEntry("dah", "AE", strategy), stems.get(26));
	}

}
