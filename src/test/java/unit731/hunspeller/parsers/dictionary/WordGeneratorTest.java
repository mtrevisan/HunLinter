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
			+ "SFX A Y 1\n"
			+ "SFX A 0 a\n"
			+ "SFX B Y 1\n"
			+ "SFX B 0 b/A\n"
			+ "SFX C Y 1\n"
			+ "SFX C 0 c/E\n"
			+ "SFX D Y 1\n"
			+ "SFX D 0 d/AE\n"
			+ "PFX E Y 1\n"
			+ "PFX E 0 e\n";
		File affFile = FileService.getTemporaryUTF8File(content);
		AffixParser parser = new AffixParser();
		parser.parse(affFile);
		WordGenerator generator = new WordGenerator(parser);
		String line = "a/ABCDE";
		FlagParsingStrategy strategy = parser.getFlagParsingStrategy();
		DictionaryEntry dicEntry = new DictionaryEntry(line, strategy);

		List<RuleProductionEntry> stems = generator.applyRules(dicEntry);

		Assert.assertEquals(12, stems.size());
		Assert.assertEquals(new RuleProductionEntry("a", "ABCDE", strategy), stems.get(0));
		Assert.assertEquals(new RuleProductionEntry("aa", "E", strategy), stems.get(1));
		Assert.assertEquals(new RuleProductionEntry("ab", "AE", strategy), stems.get(2));
		Assert.assertEquals(new RuleProductionEntry("ac", "E", strategy), stems.get(3));
		Assert.assertEquals(new RuleProductionEntry("ad", "AE", strategy), stems.get(4));
		Assert.assertEquals(new RuleProductionEntry("aba", "", strategy), stems.get(5));
		Assert.assertEquals(new RuleProductionEntry("ada", "", strategy), stems.get(6));
		Assert.assertEquals(new RuleProductionEntry("ea", "", strategy), stems.get(7));
		Assert.assertEquals(new RuleProductionEntry("eaa", "", strategy), stems.get(8));
		Assert.assertEquals(new RuleProductionEntry("eac", "", strategy), stems.get(10));
		Assert.assertEquals(new RuleProductionEntry("ead", "", strategy), stems.get(11));
	}

}
