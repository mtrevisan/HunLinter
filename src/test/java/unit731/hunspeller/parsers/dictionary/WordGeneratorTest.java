package unit731.hunspeller.parsers.dictionary;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.FileService;


public class WordGeneratorTest{

	@Test
	public void stems() throws IOException{
		String content = "SET UTF-8\n"
			+ "FULLSTRIP\n"
			+ "FLAG long\n"
			+ "SFX AA Y 1\n"
			+ "SFX AA 0 a\n"
			+ "SFX BB Y 1\n"
			+ "SFX BB 0 b/AA\n"
			+ "SFX CC Y 1\n"
			+ "SFX CC 0 c/EE\n"
			+ "SFX DD Y 1\n"
			+ "SFX DD 0 d/AAEE\n"
			+ "PFX EE Y 1\n"
			+ "PFX EE 0 e\n"
			+ "PFX FF Y 1\n"
			+ "PFX FF 0 f/AA\n"
			+ "PFX GG Y 1\n"
			+ "PFX GG 0 f/EE\n"
			+ "PFX HH Y 1\n"
			+ "PFX HH 0 h/AAEE";
		File affFile = FileService.getTemporaryUTF8File(content);
		AffixParser parser = new AffixParser();
		parser.parse(affFile);
		WordGenerator generator = new WordGenerator(parser);
		String line = "a/AABBCCDDEEFFGGHH";
		FlagParsingStrategy strategy = parser.getFlagParsingStrategy();
		DictionaryEntry dicEntry = new DictionaryEntry(line, strategy);

		List<RuleProductionEntry> stems = generator.applyRules(dicEntry);

		Assert.assertEquals(35, stems.size());
		Assert.assertEquals(new RuleProductionEntry("a", "AABBCCDDEEFFGGHH", strategy), stems.get(0));
		Assert.assertEquals(new RuleProductionEntry("aa", "EEFFGGHH", strategy), stems.get(1));
		Assert.assertEquals(new RuleProductionEntry("ab", "AAEEFFGGHH", strategy), stems.get(2));
		Assert.assertEquals(new RuleProductionEntry("ac", "EEFFGGHH", strategy), stems.get(3));
		Assert.assertEquals(new RuleProductionEntry("ad", "AAEEFFGGHH", strategy), stems.get(4));
		Assert.assertEquals(new RuleProductionEntry("aba", "EEFFGGHH", strategy), stems.get(5));
		Assert.assertEquals(new RuleProductionEntry("ada", "EEFFGGHH", strategy), stems.get(6));
		Assert.assertEquals(new RuleProductionEntry("ea", "AABBCCDD", strategy), stems.get(7));
		Assert.assertEquals(new RuleProductionEntry("fa", "AABBCCDD", strategy), stems.get(8));
		Assert.assertEquals(new RuleProductionEntry("fa", "EEAABBCCDD", strategy), stems.get(9));
		Assert.assertEquals(new RuleProductionEntry("ha", "AAEEBBCCDD", strategy), stems.get(10));
		Assert.assertEquals(new RuleProductionEntry("eaa", "", strategy), stems.get(11));
		Assert.assertEquals(new RuleProductionEntry("faa", "AA", strategy), stems.get(12));
		Assert.assertEquals(new RuleProductionEntry("faa", "EE", strategy), stems.get(13));
		Assert.assertEquals(new RuleProductionEntry("haa", "AAEE", strategy), stems.get(14));
		Assert.assertEquals(new RuleProductionEntry("eab", "AA", strategy), stems.get(15));
		Assert.assertEquals(new RuleProductionEntry("fab", "AA", strategy), stems.get(16));
		Assert.assertEquals(new RuleProductionEntry("fab", "EEAA", strategy), stems.get(17));
		Assert.assertEquals(new RuleProductionEntry("hab", "AAEE", strategy), stems.get(18));
		Assert.assertEquals(new RuleProductionEntry("eac", "", strategy), stems.get(19));
		Assert.assertEquals(new RuleProductionEntry("fac", "AA", strategy), stems.get(20));
		Assert.assertEquals(new RuleProductionEntry("fac", "EE", strategy), stems.get(21));
		Assert.assertEquals(new RuleProductionEntry("hac", "AAEE", strategy), stems.get(22));
		Assert.assertEquals(new RuleProductionEntry("ead", "AA", strategy), stems.get(23));
		Assert.assertEquals(new RuleProductionEntry("fad", "AA", strategy), stems.get(24));
		Assert.assertEquals(new RuleProductionEntry("fad", "EEAA", strategy), stems.get(25));
		Assert.assertEquals(new RuleProductionEntry("had", "AAEE", strategy), stems.get(26));
		Assert.assertEquals(new RuleProductionEntry("eaba", "", strategy), stems.get(27));
		Assert.assertEquals(new RuleProductionEntry("faba", "AA", strategy), stems.get(28));
		Assert.assertEquals(new RuleProductionEntry("faba", "EE", strategy), stems.get(29));
		Assert.assertEquals(new RuleProductionEntry("haba", "AAEE", strategy), stems.get(30));
		Assert.assertEquals(new RuleProductionEntry("eada", "", strategy), stems.get(31));
		Assert.assertEquals(new RuleProductionEntry("fada", "AA", strategy), stems.get(32));
		Assert.assertEquals(new RuleProductionEntry("fada", "EE", strategy), stems.get(33));
		Assert.assertEquals(new RuleProductionEntry("hada", "AAEE", strategy), stems.get(34));
	}

}
