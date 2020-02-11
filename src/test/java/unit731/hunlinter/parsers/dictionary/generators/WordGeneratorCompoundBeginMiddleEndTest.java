package unit731.hunlinter.parsers.dictionary.generators;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.affix.AffixParser;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.services.FileHelper;


/** @see <a href="https://github.com/hunspell/hunspell/tree/master/tests/v1cmdline">Hunspell tests</a> */
class WordGeneratorCompoundBeginMiddleEndTest{

	private AffixData affixData;
	private WordGenerator wordGenerator;


//FIXME
	@Test
	void germanCompounding() throws IOException, SAXException{
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
		loadData(affFile, language);

		String[] inputCompounds = new String[]{
			"Arbeit/A-",
			"Computer/BC-",
			"-/W",
			"Arbeitsnehmer/Z"
		};
		List<Production> words = wordGenerator.applyCompoundBeginMiddleEnd(inputCompounds, 62);
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
			createProduction("Computercomputern", "-PUWX", "pa:Computer st:Computer pa:Computern st:Computer"),
			createProduction("Computer-", "-PUWX", "pa:Computer st:Computer pa:- st:-"),
			createProduction("Computerarbeit", "-PUWX", "pa:Computer st:Computer pa:Arbeit st:Arbeit"),
			createProduction("Arbeitscomputern", "-PUWX", "pa:Arbeits st:Arbeit pa:Computern st:Computer"),
			createProduction("Arbeits-", "-PUWX", "pa:Arbeits st:Arbeit pa:- st:-"),
			createProduction("Arbeitsarbeit", "-PUWX", "pa:Arbeits st:Arbeit pa:Arbeit st:Arbeit"),
			createProduction("Arbeitsarbeits", "D-PVX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("Arbeitscomputer", "D-PVWX", "pa:Arbeits st:Arbeit pa:Computer st:Computer"),
			createProduction("Computerarbeits", "D-PVWX", "pa:Computer st:Computer pa:Arbeits st:Arbeit"),
			createProduction("Computercomputer", "D-PVWX", "pa:Computer st:Computer pa:Computer st:Computer"),
			createProduction("Arbeitscomputern", "D-PVWX", "pa:Arbeits st:Arbeit pa:Computern st:Computer"),
			createProduction("Arbeits-", "D-PVWX", "pa:Arbeits st:Arbeit pa:- st:-"),
			createProduction("Arbeitsarbeit", "D-PVWX", "pa:Arbeits st:Arbeit pa:Arbeit st:Arbeit"),
			createProduction("Computercomputern", "D-PVWX", "pa:Computer st:Computer pa:Computern st:Computer"),
			createProduction("Computer-", "D-PVWX", "pa:Computer st:Computer pa:- st:-"),
			createProduction("Computerarbeit", "D-PVWX", "pa:Computer st:Computer pa:Arbeit st:Arbeit"),
			createProduction("Computerncomputern", "D-W", "pa:Computern st:Computer pa:Computern st:Computer"),
			createProduction("Computerncomputer", "D-PVWX", "pa:Computern st:Computer pa:Computer st:Computer"),
			createProduction("Computern-", "D-W", "pa:Computern st:Computer pa:- st:-"),
			createProduction("Computernarbeit", "D-WX", "pa:Computern st:Computer pa:Arbeit st:Arbeit"),
			createProduction("-Computern", "W", "pa:- st:- pa:Computern st:Computer"),
			createProduction("-Computer", "PVWX", "pa:- st:- pa:Computer st:Computer"),
			createProduction("--", "W", "pa:- st:- pa:- st:-"),
			createProduction("-Arbeit", "WX", "pa:- st:- pa:Arbeit st:Arbeit"),
			createProduction("Arbeitcomputern", "D-WX", "pa:Arbeit st:Arbeit pa:Computern st:Computer"),
//			createProduction("Arbeitcomputer", "D-PVWX", "pa:Arbeit st:Arbeit pa:Computer st:Computer"),
			createProduction("Arbeit-", "D-WX", "pa:Arbeit st:Arbeit pa:- st:-"),
			createProduction("Arbeitarbeit", "D-WX", "pa:Arbeit st:Arbeit pa:Arbeit st:Arbeit"),
			createProduction("Computercomputercomputer", "-PUX", "pa:Computer st:Computer pa:Computer st:Computer pa:Computer st:Computer"),
			createProduction("Computercomputerarbeits", "-PUX", "pa:Computer st:Computer pa:Computer st:Computer pa:Arbeits st:Arbeit"),
			createProduction("Computerarbeitscomputer", "-PUX", "pa:Computer st:Computer pa:Arbeits st:Arbeit pa:Computer st:Computer"),
			createProduction("Computerarbeitsarbeits", "-PUX", "pa:Computer st:Computer pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("Arbeitscomputercomputer", "-PUX", "pa:Arbeits st:Arbeit pa:Computer st:Computer pa:Computer st:Computer"),
//			createProduction("Arbeitscomputerarbeits", "-PUX", "pa:Arbeits st:Arbeit pa:Computer st:Computer pa:Arbeits st:Arbeit"),
			createProduction("Arbeitsarbeitscomputer", "-PUX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit pa:Computer st:Computer"),
			createProduction("Arbeitsarbeitsarbeits", "-PUX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("Computercomputerarbeits", "-PUVX", "pa:Computer st:Computer pa:Computer st:Computer pa:Arbeits st:Arbeit"),
			createProduction("Computercomputercomputer", "-PUVWX", "pa:Computer st:Computer pa:Computer st:Computer pa:Computer st:Computer"),
			createProduction("Computerarbeitsarbeits", "-PUVX", "pa:Computer st:Computer pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("Computerarbeitscomputer", "-PUVWX", "pa:Computer st:Computer pa:Arbeits st:Arbeit pa:Computer st:Computer"),
//			createProduction("Arbeitscomputerarbeits", "-PUVX", "pa:Arbeits st:Arbeit pa:Computer st:Computer pa:Arbeits st:Arbeit"),
			createProduction("Arbeitscomputercomputer", "-PUVWX", "pa:Arbeits st:Arbeit pa:Computer st:Computer pa:Computer st:Computer"),
			createProduction("Arbeitsarbeitsarbeits", "-PUVX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
			createProduction("Arbeitsarbeitscomputer", "-PUVWX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit pa:Computer st:Computer"),
			createProduction("Computercomputercomputern", "-PUWX", "pa:Computer st:Computer pa:Computer st:Computer pa:Computern st:Computer"),
			createProduction("Computercomputer-", "-PUWX", "pa:Computer st:Computer pa:Computer st:Computer pa:- st:-"),
			createProduction("Computercomputerarbeit", "-PUWX", "pa:Computer st:Computer pa:Computer st:Computer pa:Arbeit st:Arbeit"),
			createProduction("Computerarbeitscomputern", "-PUWX", "pa:Computer st:Computer pa:Arbeits st:Arbeit pa:Computern st:Computer"),
			createProduction("Computerarbeits-", "-PUWX", "pa:Computer st:Computer pa:Arbeits st:Arbeit pa:- st:-"),
			createProduction("Computerarbeitsarbeit", "-PUWX", "pa:Computer st:Computer pa:Arbeits st:Arbeit pa:Arbeit st:Arbeit"),
			createProduction("Arbeitscomputercomputern", "-PUWX", "pa:Arbeits st:Arbeit pa:Computer st:Computer pa:Computern st:Computer"),
			createProduction("Arbeitscomputer-", "-PUWX", "pa:Arbeits st:Arbeit pa:Computer st:Computer pa:- st:-"),
			createProduction("Arbeitscomputerarbeit", "-PUWX", "pa:Arbeits st:Arbeit pa:Computer st:Computer pa:Arbeit st:Arbeit"),
			createProduction("Arbeitsarbeitscomputern", "-PUWX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit pa:Computern st:Computer")
		);
		Assertions.assertEquals(expected, words);
	}

	private void loadData(File affFile, String language) throws IOException, SAXException{
		AffixParser affParser = new AffixParser();
		affParser.parse(affFile, language);
		affixData = affParser.getAffixData();
		wordGenerator = new WordGenerator(affixData, null);
	}

	private Production createProduction(String word, String continuationFlags, String morphologicalFields){
		FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();
		return new Production(word, continuationFlags, morphologicalFields, null, strategy);
	}

}
