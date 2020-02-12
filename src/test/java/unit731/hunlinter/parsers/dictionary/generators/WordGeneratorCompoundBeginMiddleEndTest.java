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
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.services.FileHelper;
import unit731.hunlinter.services.PermutationsWithRepetitions;


/** @see <a href="https://github.com/hunspell/hunspell/tree/master/tests/v1cmdline">Hunspell tests</a> */
class WordGeneratorCompoundBeginMiddleEndTest{

	private AffixData affixData;
	private WordGenerator wordGenerator;


	//FIXME manage `COMPOUNDPERMITFLAG`
	@Test
	void germanCompounding() throws IOException, SAXException{
		String language = "ger";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			//compound flags
			"COMPOUNDBEGIN U",
			"COMPOUNDMIDDLE V",
			"COMPOUNDEND W",
			//Prefixes are allowed at the beginning of compounds, suffixes are allowed at the end of compounds by default:
			//(prefix)?(root)+(affix)?
			//Affixes with COMPOUNDPERMITFLAG may be inside of compounds
			"COMPOUNDPERMITFLAG P",
			//for fogemorphemes (Fuge-element)
			//Hint: ONLYINCOMPOUND is not required everywhere, but the checking will be a little faster with it
			"ONLYINCOMPOUND X",
			//forbid uppercase characters at compound word bounds
			"CHECKCOMPOUNDCASE",
			//for handling Fuge-elements with dashes (Arbeits-) dash will be a special word
			"COMPOUNDMIN 1",
			"WORDCHARS -",
			//for forbid exceptions (*Arbeitsnehmer)
			"FORBIDDENWORD Z",
			//compound settings and fogemorpheme for `Arbeit'
			"SFX A Y 3",
			"SFX A 0 s/UPX .",
			"SFX A 0 s/VPDX .",
			"SFX A 0 0/WXD .",
			"SFX B Y 2",
			"SFX B 0 0/UPX .",
			"SFX B 0 0/VWXDP .",
			//a suffix for `Computer`
			"SFX C Y 1",
			"SFX C 0 n/WD .",
			//dash prefix for compounds with dash (Arbeits-Computer)
			"PFX - Y 1",
			"PFX - 0 -/P .",
			//decapitalizing prefix
			"PFX D Y 2",
			"PFX D A a/P A",
			"PFX D C c/P C");
		loadData(affFile, language);


		String line = "Arbeit/A-";
		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
		List<Production> words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(7, words.size());
		//base production
		Assertions.assertEquals(createProduction("Arbeit", "A-", "st:Arbeit"), words.get(0));
		//suffix productions
		//prefix productions
		//twofold productions
		Assertions.assertEquals(createProduction("-Arbeit", "PA", "st:Arbeit"), words.get(1));
		Assertions.assertEquals(createProduction("-Arbeits", "P", "st:Arbeit"), words.get(2));
		Assertions.assertEquals(createProduction("-Arbeits", "P", "st:Arbeit"), words.get(3));
		Assertions.assertEquals(createProduction("-Arbeit", "P", "st:Arbeit"), words.get(4));
		Assertions.assertEquals(createProduction("arbeits", "P", "st:Arbeit"), words.get(5));
		Assertions.assertEquals(createProduction("arbeit", "P", "st:Arbeit"), words.get(6));


		line = "Computer/BC-";
		dicEntry = wordGenerator.createFromDictionaryLine(line);
		words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(8, words.size());
		//base production
		Assertions.assertEquals(createProduction("Computer", "BC-", "st:Computer"), words.get(0));
		//suffix productions
		Assertions.assertEquals(createProduction("Computern", "DW-", "st:Computer"), words.get(1));
		//prefix productions
		//twofold productions
		Assertions.assertEquals(createProduction("-Computer", "PBC", "st:Computer"), words.get(2));
		Assertions.assertEquals(createProduction("-Computer", "P", "st:Computer"), words.get(3));
		Assertions.assertEquals(createProduction("-Computer", "P", "st:Computer"), words.get(4));
		Assertions.assertEquals(createProduction("-Computern", "P", "st:Computer"), words.get(5));
		Assertions.assertEquals(createProduction("computer", "P", "st:Computer"), words.get(6));
		Assertions.assertEquals(createProduction("computern", "P", "st:Computer"), words.get(7));


		line = "-/W";
		dicEntry = wordGenerator.createFromDictionaryLine(line);
		words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertEquals(1, words.size());
		//base production
		Assertions.assertEquals(createProduction("-", "W", "st:-"), words.get(0));
		//suffix productions
		//prefix productions
		//twofold productions


		line = "Arbeitsnehmer/Z";
		dicEntry = wordGenerator.createFromDictionaryLine(line);
		words = wordGenerator.applyAffixRules(dicEntry);

		Assertions.assertTrue(words.isEmpty());
		//base production
		//suffix productions
		//prefix productions
		//twofold productions


		String[] inputCompounds = new String[]{
			"Arbeit/A-",
			"Computer/BC-",
			"-/W",
			"Arbeitsnehmer/Z"
		};
		words = wordGenerator.applyCompoundFlag(inputCompounds, 154, PermutationsWithRepetitions.MAX_COMPOUNDS_INFINITY);

		List<Production> expected = Arrays.asList(
//			createProduction("Arbeitarbeit", "-A", "pa:Arbeit st:Arbeit pa:Arbeit st:Arbeit"),
//			createProduction("Arbeitarbeits", "-PUX", "pa:Arbeit st:Arbeit pa:Arbeits st:Arbeit"),
//			createProduction("Arbeitarbeits", "-PVX", "pa:Arbeit st:Arbeit pa:Arbeits st:Arbeit"),
//			createProduction("Arbeit-Arbeit", "-AP", "pa:Arbeit st:Arbeit pa:-Arbeit st:Arbeit"),
//			createProduction("Arbeit-Arbeits", "-P", "pa:Arbeit st:Arbeit pa:-Arbeits st:Arbeit"),
//			createProduction("Arbeitarbeits", "-P", "pa:Arbeit st:Arbeit pa:arbeits st:Arbeit"),
//			createProduction("Arbeitsarbeit", "-APUX", "pa:Arbeits st:Arbeit pa:Arbeit st:Arbeit"),
//			createProduction("Arbeitsarbeits", "-PUX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
//			createProduction("Arbeitsarbeits", "-PUVX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
//			createProduction("Arbeits-Arbeit", "-APUX", "pa:Arbeits st:Arbeit pa:-Arbeit st:Arbeit"),
//			createProduction("Arbeits-Arbeits", "-PUX", "pa:Arbeits st:Arbeit pa:-Arbeits st:Arbeit"),
//			createProduction("Arbeitsarbeits", "-PUX", "pa:Arbeits st:Arbeit pa:arbeits st:Arbeit"),
//			createProduction("Arbeitsarbeit", "D-APVX", "pa:Arbeits st:Arbeit pa:Arbeit st:Arbeit"),
//			createProduction("Arbeitsarbeits", "D-PUVX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
//			createProduction("Arbeitsarbeits", "D-PVX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
//			createProduction("Arbeits-Arbeit", "D-APVX", "pa:Arbeits st:Arbeit pa:-Arbeit st:Arbeit"),
//			createProduction("Arbeits-Arbeits", "D-PVX", "pa:Arbeits st:Arbeit pa:-Arbeits st:Arbeit"),
//			createProduction("Arbeitsarbeits", "D-PVX", "pa:Arbeits st:Arbeit pa:arbeits st:Arbeit"),
//			createProduction("-ArbeitArbeit", "AP", "pa:-Arbeit st:Arbeit pa:Arbeit st:Arbeit"),
//			createProduction("-ArbeitArbeits", "PUX", "pa:-Arbeit st:Arbeit pa:Arbeits st:Arbeit"),
//			createProduction("-ArbeitArbeits", "PVX", "pa:-Arbeit st:Arbeit pa:Arbeits st:Arbeit"),
//			createProduction("-Arbeit-Arbeit", "AP", "pa:-Arbeit st:Arbeit pa:-Arbeit st:Arbeit"),
//			createProduction("-Arbeit-Arbeits", "P", "pa:-Arbeit st:Arbeit pa:-Arbeits st:Arbeit"),
//			createProduction("-Arbeitarbeits", "P", "pa:-Arbeit st:Arbeit pa:arbeits st:Arbeit"),
//			createProduction("-ArbeitsArbeit", "AP", "pa:-Arbeits st:Arbeit pa:Arbeit st:Arbeit"),
//			createProduction("-ArbeitsArbeits", "PUX", "pa:-Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
//			createProduction("-ArbeitsArbeits", "PVX", "pa:-Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
//			createProduction("-Arbeits-Arbeit", "AP", "pa:-Arbeits st:Arbeit pa:-Arbeit st:Arbeit"),
//			createProduction("-Arbeits-Arbeits", "P", "pa:-Arbeits st:Arbeit pa:-Arbeits st:Arbeit"),
//			createProduction("-Arbeitsarbeits", "P", "pa:-Arbeits st:Arbeit pa:arbeits st:Arbeit"),
//			createProduction("arbeitsarbeit", "AP", "pa:arbeits st:Arbeit pa:Arbeit st:Arbeit"),
//			createProduction("arbeitsarbeits", "PUX", "pa:arbeits st:Arbeit pa:Arbeits st:Arbeit"),
//			createProduction("arbeitsarbeits", "PVX", "pa:arbeits st:Arbeit pa:Arbeits st:Arbeit"),
//			createProduction("arbeits-Arbeit", "AP", "pa:arbeits st:Arbeit pa:-Arbeit st:Arbeit"),
//			createProduction("arbeits-Arbeits", "P", "pa:arbeits st:Arbeit pa:-Arbeits st:Arbeit"),
//			createProduction("arbeitsarbeits", "P", "pa:arbeits st:Arbeit pa:arbeits st:Arbeit"),
//			createProduction("Arbeitcomputer", "-BC", "pa:Arbeit st:Arbeit pa:Computer st:Computer"),
//			createProduction("Arbeitcomputer", "-PUX", "pa:Arbeit st:Arbeit pa:Computer st:Computer"),
//			createProduction("Arbeitcomputer", "-PVWX", "pa:Arbeit st:Arbeit pa:Computer st:Computer"),
//			createProduction("Arbeit-Computer", "-BCP", "pa:Arbeit st:Arbeit pa:-Computer st:Computer"),
//			createProduction("Arbeit-Computer", "-P", "pa:Arbeit st:Arbeit pa:-Computer st:Computer"),
//			createProduction("Arbeitcomputer", "-P", "pa:Arbeit st:Arbeit pa:computer st:Computer"),
//			createProduction("Arbeitscomputer", "-BCPUX", "pa:Arbeits st:Arbeit pa:Computer st:Computer"),
//			createProduction("Arbeitscomputer", "-PUX", "pa:Arbeits st:Arbeit pa:Computer st:Computer"),
//			createProduction("Arbeitscomputer", "-PUVWX", "pa:Arbeits st:Arbeit pa:Computer st:Computer"),
//			createProduction("Arbeits-Computer", "-BCPUX", "pa:Arbeits st:Arbeit pa:-Computer st:Computer"),
//			createProduction("Arbeits-Computer", "-PUX", "pa:Arbeits st:Arbeit pa:-Computer st:Computer"),
//			createProduction("Arbeitscomputer", "-PUX", "pa:Arbeits st:Arbeit pa:computer st:Computer"),
//			createProduction("Arbeitscomputer", "D-BCPVX", "pa:Arbeits st:Arbeit pa:Computer st:Computer"),
//			createProduction("Arbeitscomputer", "D-PUVX", "pa:Arbeits st:Arbeit pa:Computer st:Computer"),
//			createProduction("Arbeitscomputer", "D-PVWX", "pa:Arbeits st:Arbeit pa:Computer st:Computer"),
//			createProduction("Arbeits-Computer", "D-BCPVX", "pa:Arbeits st:Arbeit pa:-Computer st:Computer"),
//			createProduction("Arbeits-Computer", "D-PVX", "pa:Arbeits st:Arbeit pa:-Computer st:Computer"),
//			createProduction("Arbeitscomputer", "D-PVX", "pa:Arbeits st:Arbeit pa:computer st:Computer"),
//			createProduction("-ArbeitComputer", "BCP", "pa:-Arbeit st:Arbeit pa:Computer st:Computer"),
//			createProduction("-ArbeitComputer", "PUX", "pa:-Arbeit st:Arbeit pa:Computer st:Computer"),
//			createProduction("-ArbeitComputer", "PVWX", "pa:-Arbeit st:Arbeit pa:Computer st:Computer"),
//			createProduction("-Arbeit-Computer", "BCP", "pa:-Arbeit st:Arbeit pa:-Computer st:Computer"),
//			createProduction("-Arbeit-Computer", "P", "pa:-Arbeit st:Arbeit pa:-Computer st:Computer"),
//			createProduction("-Arbeitcomputer", "P", "pa:-Arbeit st:Arbeit pa:computer st:Computer"),
//			createProduction("-ArbeitsComputer", "BCP", "pa:-Arbeits st:Arbeit pa:Computer st:Computer"),
//			createProduction("-ArbeitsComputer", "PUX", "pa:-Arbeits st:Arbeit pa:Computer st:Computer"),
//			createProduction("-ArbeitsComputer", "PVWX", "pa:-Arbeits st:Arbeit pa:Computer st:Computer"),
//			createProduction("-Arbeits-Computer", "BCP", "pa:-Arbeits st:Arbeit pa:-Computer st:Computer"),
//			createProduction("-Arbeits-Computer", "P", "pa:-Arbeits st:Arbeit pa:-Computer st:Computer"),
//			createProduction("-Arbeitscomputer", "P", "pa:-Arbeits st:Arbeit pa:computer st:Computer"),
//			createProduction("arbeitscomputer", "BCP", "pa:arbeits st:Arbeit pa:Computer st:Computer"),
//			createProduction("arbeitscomputer", "PUX", "pa:arbeits st:Arbeit pa:Computer st:Computer"),
//			createProduction("arbeitscomputer", "PVWX", "pa:arbeits st:Arbeit pa:Computer st:Computer"),
//			createProduction("arbeits-Computer", "BCP", "pa:arbeits st:Arbeit pa:-Computer st:Computer"),
//			createProduction("arbeits-Computer", "P", "pa:arbeits st:Arbeit pa:-Computer st:Computer"),
//			createProduction("arbeitscomputer", "P", "pa:arbeits st:Arbeit pa:computer st:Computer"),
//			createProduction("Arbeit-", "-W", "pa:Arbeit st:Arbeit pa:- st:-"),
//			createProduction("Arbeits-", "-PUWX", "pa:Arbeits st:Arbeit pa:- st:-"),
//			createProduction("Arbeits-", "D-PVWX", "pa:Arbeits st:Arbeit pa:- st:-"),
//			createProduction("-Arbeit-", "PW", "pa:-Arbeit st:Arbeit pa:- st:-"),
//			createProduction("-Arbeits-", "PW", "pa:-Arbeits st:Arbeit pa:- st:-"),
//			createProduction("arbeits-", "PW", "pa:arbeits st:Arbeit pa:- st:-"),
//			createProduction("Computerarbeit", "-A", "pa:Computer st:Computer pa:Arbeit st:Arbeit"),
//			createProduction("Computerarbeits", "-PUX", "pa:Computer st:Computer pa:Arbeits st:Arbeit"),
//			createProduction("Computerarbeits", "-PVX", "pa:Computer st:Computer pa:Arbeits st:Arbeit"),
//			createProduction("Computer-Arbeit", "-AP", "pa:Computer st:Computer pa:-Arbeit st:Arbeit"),
//			createProduction("Computer-Arbeits", "-P", "pa:Computer st:Computer pa:-Arbeits st:Arbeit"),
//			createProduction("Computerarbeits", "-P", "pa:Computer st:Computer pa:arbeits st:Arbeit"),
//			createProduction("Computerarbeit", "-APUX", "pa:Computer st:Computer pa:Arbeit st:Arbeit"),
//			createProduction("Computerarbeits", "-PUVX", "pa:Computer st:Computer pa:Arbeits st:Arbeit"),
//			createProduction("Computer-Arbeit", "-APUX", "pa:Computer st:Computer pa:-Arbeit st:Arbeit"),
//			createProduction("Computer-Arbeits", "-PUX", "pa:Computer st:Computer pa:-Arbeits st:Arbeit"),
//			createProduction("Computerarbeits", "-PUX", "pa:Computer st:Computer pa:arbeits st:Arbeit"),
//			createProduction("Computerarbeit", "D-APVWX", "pa:Computer st:Computer pa:Arbeit st:Arbeit"),
//			createProduction("Computerarbeits", "D-PUVWX", "pa:Computer st:Computer pa:Arbeits st:Arbeit"),
//			createProduction("Computerarbeits", "D-PVWX", "pa:Computer st:Computer pa:Arbeits st:Arbeit"),
//			createProduction("Computer-Arbeit", "D-APVWX", "pa:Computer st:Computer pa:-Arbeit st:Arbeit"),
//			createProduction("Computer-Arbeits", "D-PVWX", "pa:Computer st:Computer pa:-Arbeits st:Arbeit"),
//			createProduction("Computerarbeits", "D-PVWX", "pa:Computer st:Computer pa:arbeits st:Arbeit"),
//			createProduction("-ComputerArbeit", "AP", "pa:-Computer st:Computer pa:Arbeit st:Arbeit"),
//			createProduction("-ComputerArbeits", "PUX", "pa:-Computer st:Computer pa:Arbeits st:Arbeit"),
//			createProduction("-ComputerArbeits", "PVX", "pa:-Computer st:Computer pa:Arbeits st:Arbeit"),
//			createProduction("-Computer-Arbeit", "AP", "pa:-Computer st:Computer pa:-Arbeit st:Arbeit"),
//			createProduction("-Computer-Arbeits", "P", "pa:-Computer st:Computer pa:-Arbeits st:Arbeit"),
//			createProduction("-Computerarbeits", "P", "pa:-Computer st:Computer pa:arbeits st:Arbeit"),
//			createProduction("computerarbeit", "AP", "pa:computer st:Computer pa:Arbeit st:Arbeit"),
//			createProduction("computerarbeits", "PUX", "pa:computer st:Computer pa:Arbeits st:Arbeit"),
//			createProduction("computerarbeits", "PVX", "pa:computer st:Computer pa:Arbeits st:Arbeit"),
//			createProduction("computer-Arbeit", "AP", "pa:computer st:Computer pa:-Arbeit st:Arbeit"),
//			createProduction("computer-Arbeits", "P", "pa:computer st:Computer pa:-Arbeits st:Arbeit"),
//			createProduction("computerarbeits", "P", "pa:computer st:Computer pa:arbeits st:Arbeit"),
//			createProduction("Computercomputer", "-BC", "pa:Computer st:Computer pa:Computer st:Computer"),
//			createProduction("Computercomputer", "-PUX", "pa:Computer st:Computer pa:Computer st:Computer"),
//			createProduction("Computercomputer", "-PVWX", "pa:Computer st:Computer pa:Computer st:Computer"),
//			createProduction("Computer-Computer", "-BCP", "pa:Computer st:Computer pa:-Computer st:Computer"),
//			createProduction("Computer-Computer", "-P", "pa:Computer st:Computer pa:-Computer st:Computer"),
//			createProduction("Computercomputer", "-P", "pa:Computer st:Computer pa:computer st:Computer"),
//			createProduction("Computercomputer", "-BCPUX", "pa:Computer st:Computer pa:Computer st:Computer"),
//			createProduction("Computercomputer", "-PUVWX", "pa:Computer st:Computer pa:Computer st:Computer"),
//			createProduction("Computer-Computer", "-BCPUX", "pa:Computer st:Computer pa:-Computer st:Computer"),
//			createProduction("Computer-Computer", "-PUX", "pa:Computer st:Computer pa:-Computer st:Computer"),
//			createProduction("Computercomputer", "-PUX", "pa:Computer st:Computer pa:computer st:Computer"),
//			createProduction("Computercomputer", "D-BCPVWX", "pa:Computer st:Computer pa:Computer st:Computer"),
//			createProduction("Computercomputer", "D-PUVWX", "pa:Computer st:Computer pa:Computer st:Computer"),
//			createProduction("Computercomputer", "D-PVWX", "pa:Computer st:Computer pa:Computer st:Computer"),
//			createProduction("Computer-Computer", "D-BCPVWX", "pa:Computer st:Computer pa:-Computer st:Computer"),
//			createProduction("Computer-Computer", "D-PVWX", "pa:Computer st:Computer pa:-Computer st:Computer"),
//			createProduction("Computercomputer", "D-PVWX", "pa:Computer st:Computer pa:computer st:Computer"),
//			createProduction("-ComputerComputer", "BCP", "pa:-Computer st:Computer pa:Computer st:Computer"),
//			createProduction("-ComputerComputer", "PUX", "pa:-Computer st:Computer pa:Computer st:Computer"),
//			createProduction("-ComputerComputer", "PVWX", "pa:-Computer st:Computer pa:Computer st:Computer"),
//			createProduction("-Computer-Computer", "BCP", "pa:-Computer st:Computer pa:-Computer st:Computer"),
//			createProduction("-Computer-Computer", "P", "pa:-Computer st:Computer pa:-Computer st:Computer"),
//			createProduction("-Computercomputer", "P", "pa:-Computer st:Computer pa:computer st:Computer"),
//			createProduction("computercomputer", "BCP", "pa:computer st:Computer pa:Computer st:Computer"),
//			createProduction("computercomputer", "PUX", "pa:computer st:Computer pa:Computer st:Computer"),
//			createProduction("computercomputer", "PVWX", "pa:computer st:Computer pa:Computer st:Computer"),
//			createProduction("computer-Computer", "BCP", "pa:computer st:Computer pa:-Computer st:Computer"),
//			createProduction("computer-Computer", "P", "pa:computer st:Computer pa:-Computer st:Computer"),
//			createProduction("computercomputer", "P", "pa:computer st:Computer pa:computer st:Computer"),
//			createProduction("Computer-", "-W", "pa:Computer st:Computer pa:- st:-"),
//			createProduction("Computer-", "-PUWX", "pa:Computer st:Computer pa:- st:-"),
//			createProduction("Computer-", "D-PVWX", "pa:Computer st:Computer pa:- st:-"),
//			createProduction("-Computer-", "PW", "pa:-Computer st:Computer pa:- st:-"),
//			createProduction("computer-", "PW", "pa:computer st:Computer pa:- st:-"),
//			createProduction("-Arbeit", "AW", "pa:- st:- pa:Arbeit st:Arbeit"),
//			createProduction("-Arbeits", "PUWX", "pa:- st:- pa:Arbeits st:Arbeit"),
//			createProduction("-Arbeits", "PVWX", "pa:- st:- pa:Arbeits st:Arbeit"),
//			createProduction("--Arbeit", "APW", "pa:- st:- pa:-Arbeit st:Arbeit"),
//			createProduction("--Arbeits", "PW", "pa:- st:- pa:-Arbeits st:Arbeit"),
//			createProduction("-arbeits", "PW", "pa:- st:- pa:arbeits st:Arbeit"),
//			createProduction("-Computer", "BCW", "pa:- st:- pa:Computer st:Computer"),
//			createProduction("-Computer", "PUWX", "pa:- st:- pa:Computer st:Computer"),
//			createProduction("-Computer", "PVWX", "pa:- st:- pa:Computer st:Computer"),
//			createProduction("--Computer", "BCPW", "pa:- st:- pa:-Computer st:Computer"),
//			createProduction("--Computer", "PW", "pa:- st:- pa:-Computer st:Computer"),
//			createProduction("-computer", "PW", "pa:- st:- pa:computer st:Computer"),
//			createProduction("--", "W", "pa:- st:- pa:- st:-")
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
