package unit731.hunspeller.parsers.dictionary;

import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.FileHelper;


/** @see <a href="https://github.com/hunspell/hunspell/tree/master/tests/v1cmdline">Hunspell tests</a> */
public class WordGeneratorCompoundBeginMiddleEndTest{

	private final Backbone backbone = new Backbone(null, null);


	private void loadData(String affixFilePath) throws IOException{
		backbone.loadFile(affixFilePath);
	}

	private Production createProduction(String word, String continuationFlags, String morphologicalFields){
		FlagParsingStrategy strategy = backbone.getAffParser().getFlagParsingStrategy();
		return new Production(word, continuationFlags, morphologicalFields, null, strategy);
	}

	@Test
	public void germanCompounding() throws IOException{
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
		List<Production> words = backbone.getWordGenerator().applyCompoundBeginMiddleEnd(inputCompounds, 33);
words.forEach(System.out::println);

//good: Arbeits-, Computerarbeit, Computerarbeits-, Arbeitscomputer, Computercomputer, Computercomputern,
//			Arbeitscomputern, Computerarbeitscomputer, Computerarbeitscomputern, Arbeitscomputercomputer, Computercomputerarbeit,
//			Arbeitscomputerarbeit, Arbeitsarbeitsarbeit, Computerarbeitsarbeit, Computerarbeits-Computer, Computerarbeits-Computern, Computer-Arbeit
//bad: computer, computern, arbeit, Arbeits, arbeits, ComputerArbeit, ComputernArbeit, Computernarbeit, ComputerArbeits, Arbeitcomputer,
//			Arbeitcomputern, ArbeitsComputer, ArbeitsComputern, Computerarbeitcomputer, ComputerArbeitcomputer, ComputerArbeitscomputer,
//			Computerarbeitcomputern, ComputerArbeitcomputern, ComputerArbeitscomputern, Arbeitscomputerarbeits, Arbeitscomputernarbeits,
//			Computerarbeits-computer, Arbeitsnehmer, computers, computern, computernarbeit, computernArbeit, computerArbeit, computerArbeits,
//			arbeitcomputer, arbeitsComputer, computerarbeitcomputer, computerArbeitcomputer, computerArbeitscomputer, arbeitscomputerarbeits,
//			computerarbeits-computer, arbeitsnehmer, computernarbeit, computernArbeit, arbeits-, computerarbeit, computerarbeits-, arbeitscomputer,
//			arbeitscomputern, computerarbeitscomputer, computerarbeitscomputern, computerarbeitscomputers, arbeitscomputerarbeit,
//			computerarbeits-Computer, computerarbeits-Computern
		List<Production> expected = Arrays.asList(
			createProduction("Arbeitsarbeits", "-PX", "pa:Arbeits st:Arbeit pa:arbeits st:Arbeit"),
			createProduction("Arbeitscomputer", "-PX", "pa:Arbeits st:Arbeit pa:computer st:Computer"),
			createProduction("Computerarbeits", "-PX", "pa:Computer st:Computer pa:arbeits st:Arbeit"),
			createProduction("Computercomputer", "-PX", "pa:Computer st:Computer pa:computer st:Computer"),
			createProduction("Arbeitscomputern", "-PX", "pa:Arbeits st:Arbeit pa:computern st:Computer"),
			createProduction("Arbeitsarbeit", "-PX", "pa:Arbeits st:Arbeit pa:arbeit st:Arbeit"),
			createProduction("Computercomputern", "-PX", "pa:Computer st:Computer pa:computern st:Computer"),
			createProduction("Computerarbeit", "-PX", "pa:Computer st:Computer pa:arbeit st:Arbeit"),
			createProduction("Arbeitsarbeits", "D-PX", "pa:Arbeits st:Arbeit pa:arbeits st:Arbeit"),
			createProduction("Arbeitscomputer", "D-PX", "pa:Arbeits st:Arbeit pa:computer st:Computer"),
			createProduction("Computerarbeits", "D-PX", "pa:Computer st:Computer pa:arbeits st:Arbeit"),
			createProduction("Computercomputer", "D-PX", "pa:Computer st:Computer pa:computer st:Computer"),
			createProduction("Arbeitsarbeit", "D-PX", "pa:Arbeits st:Arbeit pa:arbeit st:Arbeit"),
			createProduction("Arbeitscomputern", "D-PX", "pa:Arbeits st:Arbeit pa:computern st:Computer"),
			createProduction("Computerarbeit", "D-PX", "pa:Computer st:Computer pa:arbeit st:Arbeit"),
			createProduction("Computercomputern", "D-PX", "pa:Computer st:Computer pa:computern st:Computer"),
			createProduction("Arbeitarbeit", "D-PX", "pa:Arbeit st:Arbeit pa:arbeit st:Arbeit"),
			createProduction("Arbeitcomputern", "D-PX", "pa:Arbeit st:Arbeit pa:computern st:Computer"),
			createProduction("Arbeitcomputer", "D-PX", "pa:Arbeit st:Arbeit pa:computer st:Computer")
		);
		Assert.assertEquals(expected, words);
	}

}
