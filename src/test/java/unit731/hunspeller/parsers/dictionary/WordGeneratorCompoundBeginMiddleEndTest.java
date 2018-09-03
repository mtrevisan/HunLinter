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
		String language = "de";
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
			"PFX D Y 29",
			"PFX D A a/PX A",
			"PFX D Ä ä/PX Ä",
			"PFX D B b/PX B",
			"PFX D C c/PX C",
			"PFX D D d/PX D",
			"PFX D E e/PX E",
			"PFX D F f/PX F",
			"PFX D G g/PX G",
			"PFX D H h/PX H",
			"PFX D I i/PX I",
			"PFX D J j/PX J",
			"PFX D K k/PX K",
			"PFX D L l/PX L",
			"PFX D M m/PX M",
			"PFX D N n/PX N",
			"PFX D O o/PX O",
			"PFX D Ö ö/PX Ö",
			"PFX D P p/PX P",
			"PFX D Q q/PX Q",
			"PFX D R r/PX R",
			"PFX D S s/PX S",
			"PFX D T t/PX T",
			"PFX D U u/PX U",
			"PFX D Ü ü/PX Ü",
			"PFX D V v/PX V",
			"PFX D W w/PX W",
			"PFX D X x/PX X",
			"PFX D Y y/PX Y",
			"PFX D Z z/PX Z");
		loadData(affFile.getAbsolutePath());

		String line = "vw";
		String[] inputCompounds = new String[]{
			"Arbeit/A-",
			"Computer/BC-",
			"-/W",
			"Arbeitsnehmer/Z"
		};
		List<Production> words = backbone.getWordGenerator().applyCompoundRules(inputCompounds, line, 10);

//good: Computer, Computern, Arbeit, Arbeits-, Computerarbeit, Computerarbeits-, Arbeitscomputer, Computercomputer, Computercomputern, Arbeitscomputern, Computerarbeitscomputer, Computerarbeitscomputern, Arbeitscomputercomputer, Computercomputerarbeit, Arbeitscomputerarbeit, Arbeitsarbeitsarbeit, Computerarbeitsarbeit, Computerarbeits-Computer, Computerarbeits-Computern, Computer-Arbeit
//bad: computer, computern, arbeit, Arbeits, arbeits, ComputerArbeit, ComputernArbeit, Computernarbeit, ComputerArbeits, Arbeitcomputer, Arbeitcomputern, ArbeitsComputer, ArbeitsComputern, Computerarbeitcomputer, ComputerArbeitcomputer, ComputerArbeitscomputer, Computerarbeitcomputern, ComputerArbeitcomputern, ComputerArbeitscomputern, Arbeitscomputerarbeits, Arbeitscomputernarbeits, Computerarbeits-computer, Arbeitsnehmer, computers, computern, computernarbeit, computernArbeit, computerArbeit, computerArbeits, arbeitcomputer, arbeitsComputer, computerarbeitcomputer, computerArbeitcomputer, computerArbeitscomputer, arbeitscomputerarbeits, computerarbeits-computer, arbeitsnehmer, computernarbeit, computernArbeit, arbeits-, computerarbeit, computerarbeits-, arbeitscomputer, arbeitscomputern, computerarbeitscomputer, computerarbeitscomputern, computerarbeitscomputers, arbeitscomputerarbeit, computerarbeits-Computer, computerarbeits-Computern
		List<Production> expected = Arrays.asList(
			createProduction("Computer", "BC-", "st:Computer"),
			createProduction("Computern", null, "pa:arbeits st:arbeits pa:scheu st:scheu"),
			createProduction("Arbeit", null, "pa:arbeits st:arbeits pa:scheu st:scheu"),
			createProduction("Arbeits-", null, "pa:arbeits st:arbeits pa:scheu st:scheu"),
			createProduction("Computerarbeit", null, "pa:arbeits st:arbeits pa:scheu st:scheu"),
			createProduction("Computerarbeits-", null, "pa:arbeits st:arbeits pa:scheu st:scheu")
		);
		Assert.assertEquals(expected, words);
	}

}
