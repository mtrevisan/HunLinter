package unit731.hunspeller.parsers.dictionary;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import unit731.hunspeller.parsers.affix.AffixParser;
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
		String line = "a/AABBCCDDEE";
		DictionaryEntry dicEntry = new DictionaryEntry(line, parser.getFlagParsingStrategy());

		List<RuleProductionEntry> stems = generator.applyRules(dicEntry);

		Assert.assertEquals(14, stems.size());

		//FIXME
	}

}
