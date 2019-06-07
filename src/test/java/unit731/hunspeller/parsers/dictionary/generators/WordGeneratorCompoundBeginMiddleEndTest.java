package unit731.hunspeller.parsers.dictionary.generators;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.vos.Production;
import unit731.hunspeller.services.FileHelper;


/** @see <a href="https://github.com/hunspell/hunspell/tree/master/tests/v1cmdline">Hunspell tests</a> */
class WordGeneratorCompoundBeginMiddleEndTest{

	private final Backbone backbone = new Backbone(null, null);


	private void loadData(String affixFilePath) throws IOException{
		backbone.loadFile(affixFilePath);
	}

	private Production createProduction(String word, String continuationFlags, String morphologicalFields){
		FlagParsingStrategy strategy = backbone.getAffixData().getFlagParsingStrategy();
		return new Production(word, continuationFlags, morphologicalFields, null, strategy);
	}

	@Test
	void germanCompounding() throws IOException{
		String language = "ger";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"CHECKSHARPS",
			"COMPOUNDBEGIN U",
			"COMPOUNDMIDDLE V",
			"COMPOUNDEND W",
			"COMPOUNDPERMITFLAG P",
			"ONLYINCOMPOUND X",
			"CHECKCOMPOUNDCASE",
			"COMPOUNDMIN 1",
			"WORDCHARS -",
			"SFX A Y 3",
			"SFX A 0 s/UPX .",
			"SFX A 0 s/VPDX .",
			"SFX A 0 0/WXD .",
			"SFX B Y 2",
			"SFX B 0 0/UPX .",
			"SFX B 0 0/VWXDP .",
			"SFX C Y 1",
			"SFX C 0 n/WD .",
			"FORBIDDENWORD Z",
			"PFX - Y 1",
			"PFX - 0 -/P .",
			"PFX D Y 2",
			"PFX D A a/PX A",
			"PFX D C c/PX C");
		loadData(affFile.getAbsolutePath());

		String[] inputCompounds = new String[]{
			"Arbeit/A-",
			"Computer/BC-",
			"-/W",
			"Arbeitsnehmer/Z"
		};
		List<Production> words = backbone.getWordGenerator().applyCompoundBeginMiddleEnd(inputCompounds, 62);
words.forEach(System.out::println);

//good: Computerarbeits-, Computercomputern, Arbeitscomputern, Computerarbeitscomputerns, Computerarbeits-Computer,
//			Computerarbeits-Computern, Computer-Arbeit
//bad: Arbeitcomputer, Computerarbeitcomputer, Arbeitscomputerarbeits
		List<Production> expected = Arrays.asList(
			createProduction("Computercomputer", "-PUX", "pa:Computer st:Computer pa:Computer st:Computer"),
			createProduction("Computerarbeits", "-PUX", "pa:Computer st:Computer pa:Arbeits st:Arbeit"),
			createProduction("Arbeitscomputer", "-PUX", "pa:Arbeits st:Arbeit pa:Computer st:Computer"),
			createProduction("Arbeitsarbeits", "-PUX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("Computerarbeits", "-PUVX", "pa:Computer st:Computer pa:Arbeits st:Arbeit"),
			createProduction("Computercomputer", "-PUVWX", "pa:Computer st:Computer pa:Computer st:Computer"),
			createProduction("Arbeitsarbeits", "-PUVX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("Arbeitscomputer", "-PUVWX", "pa:Arbeits st:Arbeit pa:Computer st:Computer"),
			createProduction("Computerarbeit", "-PUWX", "pa:Computer st:Computer pa:Arbeit st:Arbeit"),
			createProduction("Arbeitsarbeit", "-PUWX", "pa:Arbeits st:Arbeit pa:Arbeit st:Arbeit"),
			createProduction("Arbeitsarbeits", "D-PVX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("Arbeitscomputer", "D-PVWX", "pa:Arbeits st:Arbeit pa:Computer st:Computer"),
			createProduction("Computerarbeits", "D-PVWX", "pa:Computer st:Computer pa:Arbeits st:Arbeit"),
			createProduction("Computercomputer", "D-PVWX", "pa:Computer st:Computer pa:Computer st:Computer"),
			createProduction("Arbeitsarbeit", "D-PVWX", "pa:Arbeits st:Arbeit pa:Arbeit st:Arbeit"),
			createProduction("Computerarbeit", "D-PVWX", "pa:Computer st:Computer pa:Arbeit st:Arbeit"),
			createProduction("Arbeitcomputer", "D-PVWX", "pa:Arbeit st:Arbeit pa:Computer st:Computer"),
			createProduction("Arbeitarbeit", "D-WX", "pa:Arbeit st:Arbeit pa:Arbeit st:Arbeit"),
			createProduction("Computercomputercomputer", "-PUX", "pa:Computer st:Computer pa:Computer st:Computer pa:Computer st:Computer"),
			createProduction("Computercomputerarbeits", "-PUX", "pa:Computer st:Computer pa:Computer st:Computer pa:Arbeits st:Arbeit"),
			createProduction("Computerarbeitscomputer", "-PUX", "pa:Computer st:Computer pa:Arbeits st:Arbeit pa:Computer st:Computer"),
			createProduction("Computerarbeitsarbeits", "-PUX", "pa:Computer st:Computer pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("Arbeitscomputercomputer", "-PUX", "pa:Arbeits st:Arbeit pa:Computer st:Computer pa:Computer st:Computer"),
			createProduction("Arbeitscomputerarbeits", "-PUX", "pa:Arbeits st:Arbeit pa:Computer st:Computer pa:Arbeits st:Arbeit"),
			createProduction("Arbeitsarbeitscomputer", "-PUX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit pa:Computer st:Computer"),
			createProduction("Arbeitsarbeitsarbeits", "-PUX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("Computercomputerarbeits", "-PUVX", "pa:Computer st:Computer pa:Computer st:Computer pa:Arbeits st:Arbeit"),
			createProduction("Computercomputercomputer", "-PUVWX", "pa:Computer st:Computer pa:Computer st:Computer pa:Computer st:Computer"),
			createProduction("Computerarbeitsarbeits", "-PUVX", "pa:Computer st:Computer pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("Computerarbeitscomputer", "-PUVWX", "pa:Computer st:Computer pa:Arbeits st:Arbeit pa:Computer st:Computer"),
			createProduction("Arbeitscomputerarbeits", "-PUVX", "pa:Arbeits st:Arbeit pa:Computer st:Computer pa:Arbeits st:Arbeit"),
			createProduction("Arbeitscomputercomputer", "-PUVWX", "pa:Arbeits st:Arbeit pa:Computer st:Computer pa:Computer st:Computer"),
			createProduction("Arbeitsarbeitsarbeits", "-PUVX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("Arbeitsarbeitscomputer", "-PUVWX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit pa:Computer st:Computer"),
			createProduction("Computercomputerarbeit", "-PUWX", "pa:Computer st:Computer pa:Computer st:Computer pa:Arbeit st:Arbeit"),
			createProduction("Computerarbeitsarbeit", "-PUWX", "pa:Computer st:Computer pa:Arbeits st:Arbeit pa:Arbeit st:Arbeit"),
			createProduction("Arbeitscomputerarbeit", "-PUWX", "pa:Arbeits st:Arbeit pa:Computer st:Computer pa:Arbeit st:Arbeit"),
			createProduction("Arbeitsarbeitsarbeit", "-PUWX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit pa:Arbeit st:Arbeit"),
			createProduction("Computerarbeitcomputer", "-PUVWX", "pa:Computer st:Computer pa:Arbeit st:Arbeit pa:Computer st:Computer"),
			createProduction("Computerarbeitarbeit", "-PUWX", "pa:Computer st:Computer pa:Arbeit st:Arbeit pa:Arbeit st:Arbeit"),
			createProduction("Arbeitsarbeitcomputer", "-PUVWX", "pa:Arbeits st:Arbeit pa:Arbeit st:Arbeit pa:Computer st:Computer"),
			createProduction("Arbeitsarbeitarbeit", "-PUWX", "pa:Arbeits st:Arbeit pa:Arbeit st:Arbeit pa:Arbeit st:Arbeit"),
			createProduction("Arbeitsarbeitsarbeits", "D-PVX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("Arbeitsarbeitscomputer", "D-PVWX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit pa:Computer st:Computer"),
			createProduction("Arbeitscomputerarbeits", "D-PVX", "pa:Arbeits st:Arbeit pa:Computer st:Computer pa:Arbeits st:Arbeit"),
			createProduction("Arbeitscomputercomputer", "D-PVWX", "pa:Arbeits st:Arbeit pa:Computer st:Computer pa:Computer st:Computer"),
			createProduction("Computerarbeitsarbeits", "D-PVWX", "pa:Computer st:Computer pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("Computerarbeitscomputer", "D-PVWX", "pa:Computer st:Computer pa:Arbeits st:Arbeit pa:Computer st:Computer"),
			createProduction("Computercomputerarbeits", "D-PVWX", "pa:Computer st:Computer pa:Computer st:Computer pa:Arbeits st:Arbeit"),
			createProduction("Computercomputercomputer", "D-PVWX", "pa:Computer st:Computer pa:Computer st:Computer pa:Computer st:Computer"),
			createProduction("Arbeitsarbeitsarbeit", "D-PVWX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit pa:Arbeit st:Arbeit"),
			createProduction("Arbeitscomputerarbeit", "D-PVWX", "pa:Arbeits st:Arbeit pa:Computer st:Computer pa:Arbeit st:Arbeit"),
			createProduction("Computerarbeitsarbeit", "D-PVWX", "pa:Computer st:Computer pa:Arbeits st:Arbeit pa:Arbeit st:Arbeit"),
			createProduction("Computercomputerarbeit", "D-PVWX", "pa:Computer st:Computer pa:Computer st:Computer pa:Arbeit st:Arbeit"),
			createProduction("Arbeitsarbeitcomputer", "D-PVWX", "pa:Arbeits st:Arbeit pa:Arbeit st:Arbeit pa:Computer st:Computer"),
			createProduction("Arbeitsarbeitarbeit", "D-PVWX", "pa:Arbeits st:Arbeit pa:Arbeit st:Arbeit pa:Arbeit st:Arbeit"),
			createProduction("Computerarbeitcomputer", "D-PVWX", "pa:Computer st:Computer pa:Arbeit st:Arbeit pa:Computer st:Computer"),
			createProduction("Computerarbeitarbeit", "D-PVWX", "pa:Computer st:Computer pa:Arbeit st:Arbeit pa:Arbeit st:Arbeit"),
			createProduction("Arbeitcomputercomputer", "D-PVWX", "pa:Arbeit st:Arbeit pa:Computer st:Computer pa:Computer st:Computer"),
			createProduction("Arbeitcomputerarbeit", "D-WX", "pa:Arbeit st:Arbeit pa:Computer st:Computer pa:Arbeit st:Arbeit"),
			createProduction("Arbeitarbeitcomputer", "D-PVWX", "pa:Arbeit st:Arbeit pa:Arbeit st:Arbeit pa:Computer st:Computer"),
			createProduction("Arbeitarbeitarbeit", "D-WX", "pa:Arbeit st:Arbeit pa:Arbeit st:Arbeit pa:Arbeit st:Arbeit")
		);
		Assertions.assertEquals(expected, words);
	}

}
