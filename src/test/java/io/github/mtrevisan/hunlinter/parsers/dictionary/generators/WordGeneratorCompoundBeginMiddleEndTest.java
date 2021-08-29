/**
 * Copyright (c) 2019-2021 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.parsers.dictionary.generators;


/** @see <a href="https://github.com/hunspell/hunspell/tree/master/tests/v1cmdline">Hunspell tests</a>. */
class WordGeneratorCompoundBeginMiddleEndTest extends TestBase{

//FIXME
//	@Test
//	void germanCompounding() throws IOException{
//		String language = "ger";
//		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
//			"SET UTF-8",
//			//compound flags
//			"COMPOUNDBEGIN U",
//			"COMPOUNDMIDDLE V",
//			"COMPOUNDEND W",
//			//Prefixes are allowed at the beginning of compounds, suffixes are allowed at the end of compounds by default:
//			//(prefix)?(root)+(affix)?
//			//Affixes with COMPOUNDPERMITFLAG may be inside of compounds
//			"COMPOUNDPERMITFLAG P",
//			//for fogemorphemes (Fuge-element)
//			//Hint: ONLYINCOMPOUND is not required everywhere, but the checking will be a little faster with it
//			"ONLYINCOMPOUND X",
//			//forbid uppercase characters at compound word bounds
//			"CHECKCOMPOUNDCASE",
//			//for handling Fuge-elements with dashes (Arbeits-) dash will be a special word
//			"COMPOUNDMIN 1",
//			"WORDCHARS -",
//			//for forbid exceptions (*Arbeitsnehmer)
//			"FORBIDDENWORD Z",
//			//compound settings and fogemorpheme for `Arbeit'
//			"SFX A Y 3",
//			"SFX A 0 s/UPX .",
//			"SFX A 0 s/VPDX .",
//			"SFX A 0 0/WXD .",
//			"SFX B Y 2",
//			"SFX B 0 0/UPX .",
//			"SFX B 0 0/VWXDP .",
//			//a suffix for `Computer`
//			"SFX C Y 1",
//			"SFX C 0 n/WD .",
//			//dash prefix for compounds with dash (Arbeits-Computer)
//			"PFX - Y 1",
//			"PFX - 0 -/P .",
//			//decapitalizing prefix
//			"PFX D Y 2",
//			"PFX D A a/P A",
//			"PFX D C c/P C");
//		loadData(affFile, language);
//
//
//		String line = "Arbeit/A-";
//		DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
//		List<Inflection> words = wordGenerator.applyAffixRules(dicEntry);
//
//		Assertions.assertEquals(2, words.size());
//		//base inflection
//		Assertions.assertEquals(createInflection("Arbeit", "A-", "st:Arbeit"), words.get(0));
//		//suffix inflections
//		//prefix inflections
//		//twofold inflections
//		Assertions.assertEquals(createInflection("-Arbeit", "PA", "st:Arbeit"), words.get(1));
//
//
//		line = "Computer/BC-";
//		dicEntry = wordGenerator.createFromDictionaryLine(line);
//		words = wordGenerator.applyAffixRules(dicEntry);
//
//		Assertions.assertEquals(5, words.size());
//		//base inflections
//		Assertions.assertEquals(createInflection("Computer", "BC-", "st:Computer"), words.get(0));
//		//suffix inflections
//		Assertions.assertEquals(createInflection("Computern", "DW-", "st:Computer"), words.get(1));
//		//prefix inflections
//		//twofold inflections
//		Assertions.assertEquals(createInflection("-Computer", "PBC", "st:Computer"), words.get(2));
//		Assertions.assertEquals(createInflection("-Computern", "P", "st:Computer"), words.get(3));
//		Assertions.assertEquals(createInflection("computern", "P", "st:Computer"), words.get(4));
//
//
//		line = "-/W";
//		dicEntry = wordGenerator.createFromDictionaryLine(line);
//		words = wordGenerator.applyAffixRules(dicEntry);
//
//		Assertions.assertEquals(1, words.size());
//		//base inflection
//		Assertions.assertEquals(createInflection("-", "W", "st:-"), words.get(0));
//		//suffix inflections
//		//prefix inflections
//		//twofold inflections
//
//
//		line = "Arbeitsnehmer/Z";
//		dicEntry = wordGenerator.createFromDictionaryLine(line);
//		words = wordGenerator.applyAffixRules(dicEntry);
//
//		Assertions.assertTrue(words.isEmpty());
//		//base inflection
//		//suffix inflections
//		//prefix inflections
//		//twofold inflections
//
//
//		String[] inputCompounds = new String[]{
//			"Arbeit/A-",
//			"Computer/BC-",
//			"-/W",
//			"Arbeitsnehmer/Z"
//		};
//		words = wordGenerator.applyCompoundFlag(inputCompounds, 1086, PermutationsWithRepetitions.MAX_COMPOUNDS_INFINITY);
//
//		//good:
///*
//Arbeit
//Arbeits-
//Computerarbeit
//Computerarbeits-
//Arbeitscomputer
//Computercomputer
//Computercomputern
//Arbeitscomputern
//Computerarbeitscomputer
//Computerarbeitscomputern
//Arbeitscomputercomputer
//Computercomputerarbeit
//Arbeitscomputerarbeit
//Arbeitsarbeitsarbeit
//Computerarbeitsarbeit
//Computerarbeits-Computer
//Computerarbeits-Computern
//Computer-Arbeit*/
//		//wrong:
///*computer
//computern
//arbeit
//Arbeits
//arbeits
//ComputerArbeit
//ComputernArbeit
//Computernarbeit
//ComputerArbeits
//Arbeitcomputer
//Arbeitcomputern
//ArbeitsComputer
//ArbeitsComputern
//Computerarbeitcomputer
//ComputerArbeitcomputer
//ComputerArbeitscomputer
//Computerarbeitcomputern
//ComputerArbeitcomputern
//ComputerArbeitscomputern
//Arbeitscomputerarbeits
//Arbeitscomputernarbeits
//Computerarbeits-computer
//Arbeitsnehmer
//computers
//computern
//computernarbeit
//computernArbeit
//computerArbeit
//computerArbeits
//arbeitcomputer
//arbeitsComputer
//computerarbeitcomputer
//computerArbeitcomputer
//computerArbeitscomputer
//arbeitscomputerarbeits
//computerarbeits-computer
//arbeitsnehmer
//computernarbeit
//computernArbeit
//arbeits-
//computerarbeit
//computerarbeits-
//arbeitscomputer
//arbeitscomputern
//computerarbeitscomputer
//computerarbeitscomputern
//computerarbeitscomputers
//arbeitscomputerarbeit
//computerarbeits-Computer
//computerarbeits-Computern*/
//		List<Inflection> expected = Arrays.asList(
////			createInflection("Arbeitarbeit", "-A", "pa:Arbeit st:Arbeit pa:Arbeit st:Arbeit"),
////			createInflection("Arbeitarbeits", "-PUX", "pa:Arbeit st:Arbeit pa:Arbeits st:Arbeit"),
////			createInflection("Arbeitarbeits", "-PVX", "pa:Arbeit st:Arbeit pa:Arbeits st:Arbeit"),
////			createInflection("Arbeit-Arbeit", "-AP", "pa:Arbeit st:Arbeit pa:-Arbeit st:Arbeit"),
////			createInflection("Arbeit-Arbeits", "-P", "pa:Arbeit st:Arbeit pa:-Arbeits st:Arbeit"),
////			createInflection("Arbeitarbeits", "-P", "pa:Arbeit st:Arbeit pa:arbeits st:Arbeit"),
////			createInflection("Arbeitsarbeit", "-APUX", "pa:Arbeits st:Arbeit pa:Arbeit st:Arbeit"),
////			createInflection("Arbeitsarbeits", "-PUX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
////			createInflection("Arbeitsarbeits", "-PUVX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
////			createInflection("Arbeits-Arbeit", "-APUX", "pa:Arbeits st:Arbeit pa:-Arbeit st:Arbeit"),
////			createInflection("Arbeits-Arbeits", "-PUX", "pa:Arbeits st:Arbeit pa:-Arbeits st:Arbeit"),
////			createInflection("Arbeitsarbeits", "-PUX", "pa:Arbeits st:Arbeit pa:arbeits st:Arbeit"),
////			createInflection("Arbeitsarbeit", "D-APVX", "pa:Arbeits st:Arbeit pa:Arbeit st:Arbeit"),
////			createInflection("Arbeitsarbeits", "D-PUVX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
////			createInflection("Arbeitsarbeits", "D-PVX", "pa:Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
////			createInflection("Arbeits-Arbeit", "D-APVX", "pa:Arbeits st:Arbeit pa:-Arbeit st:Arbeit"),
////			createInflection("Arbeits-Arbeits", "D-PVX", "pa:Arbeits st:Arbeit pa:-Arbeits st:Arbeit"),
////			createInflection("Arbeitsarbeits", "D-PVX", "pa:Arbeits st:Arbeit pa:arbeits st:Arbeit"),
////			createInflection("-ArbeitArbeit", "AP", "pa:-Arbeit st:Arbeit pa:Arbeit st:Arbeit"),
////			createInflection("-ArbeitArbeits", "PUX", "pa:-Arbeit st:Arbeit pa:Arbeits st:Arbeit"),
////			createInflection("-ArbeitArbeits", "PVX", "pa:-Arbeit st:Arbeit pa:Arbeits st:Arbeit"),
////			createInflection("-Arbeit-Arbeit", "AP", "pa:-Arbeit st:Arbeit pa:-Arbeit st:Arbeit"),
////			createInflection("-Arbeit-Arbeits", "P", "pa:-Arbeit st:Arbeit pa:-Arbeits st:Arbeit"),
////			createInflection("-Arbeitarbeits", "P", "pa:-Arbeit st:Arbeit pa:arbeits st:Arbeit"),
////			createInflection("-ArbeitsArbeit", "AP", "pa:-Arbeits st:Arbeit pa:Arbeit st:Arbeit"),
////			createInflection("-ArbeitsArbeits", "PUX", "pa:-Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
////			createInflection("-ArbeitsArbeits", "PVX", "pa:-Arbeits st:Arbeit pa:Arbeits st:Arbeit"),
////			createInflection("-Arbeits-Arbeit", "AP", "pa:-Arbeits st:Arbeit pa:-Arbeit st:Arbeit"),
////			createInflection("-Arbeits-Arbeits", "P", "pa:-Arbeits st:Arbeit pa:-Arbeits st:Arbeit"),
////			createInflection("-Arbeitsarbeits", "P", "pa:-Arbeits st:Arbeit pa:arbeits st:Arbeit"),
////			createInflection("arbeitsarbeit", "AP", "pa:arbeits st:Arbeit pa:Arbeit st:Arbeit"),
////			createInflection("arbeitsarbeits", "PUX", "pa:arbeits st:Arbeit pa:Arbeits st:Arbeit"),
////			createInflection("arbeitsarbeits", "PVX", "pa:arbeits st:Arbeit pa:Arbeits st:Arbeit"),
////			createInflection("arbeits-Arbeit", "AP", "pa:arbeits st:Arbeit pa:-Arbeit st:Arbeit"),
////			createInflection("arbeits-Arbeits", "P", "pa:arbeits st:Arbeit pa:-Arbeits st:Arbeit"),
////			createInflection("arbeitsarbeits", "P", "pa:arbeits st:Arbeit pa:arbeits st:Arbeit"),
////			createInflection("Arbeitcomputer", "-BC", "pa:Arbeit st:Arbeit pa:Computer st:Computer"),
////			createInflection("Arbeitcomputer", "-PUX", "pa:Arbeit st:Arbeit pa:Computer st:Computer"),
////			createInflection("Arbeitcomputer", "-PVWX", "pa:Arbeit st:Arbeit pa:Computer st:Computer"),
////			createInflection("Arbeit-Computer", "-BCP", "pa:Arbeit st:Arbeit pa:-Computer st:Computer"),
////			createInflection("Arbeit-Computer", "-P", "pa:Arbeit st:Arbeit pa:-Computer st:Computer"),
////			createInflection("Arbeitcomputer", "-P", "pa:Arbeit st:Arbeit pa:computer st:Computer"),
////			createInflection("Arbeitscomputer", "-BCPUX", "pa:Arbeits st:Arbeit pa:Computer st:Computer"),
////			createInflection("Arbeitscomputer", "-PUX", "pa:Arbeits st:Arbeit pa:Computer st:Computer"),
////			createInflection("Arbeitscomputer", "-PUVWX", "pa:Arbeits st:Arbeit pa:Computer st:Computer"),
////			createInflection("Arbeits-Computer", "-BCPUX", "pa:Arbeits st:Arbeit pa:-Computer st:Computer"),
////			createInflection("Arbeits-Computer", "-PUX", "pa:Arbeits st:Arbeit pa:-Computer st:Computer"),
////			createInflection("Arbeitscomputer", "-PUX", "pa:Arbeits st:Arbeit pa:computer st:Computer"),
////			createInflection("Arbeitscomputer", "D-BCPVX", "pa:Arbeits st:Arbeit pa:Computer st:Computer"),
////			createInflection("Arbeitscomputer", "D-PUVX", "pa:Arbeits st:Arbeit pa:Computer st:Computer"),
////			createInflection("Arbeitscomputer", "D-PVWX", "pa:Arbeits st:Arbeit pa:Computer st:Computer"),
////			createInflection("Arbeits-Computer", "D-BCPVX", "pa:Arbeits st:Arbeit pa:-Computer st:Computer"),
////			createInflection("Arbeits-Computer", "D-PVX", "pa:Arbeits st:Arbeit pa:-Computer st:Computer"),
////			createInflection("Arbeitscomputer", "D-PVX", "pa:Arbeits st:Arbeit pa:computer st:Computer"),
////			createInflection("-ArbeitComputer", "BCP", "pa:-Arbeit st:Arbeit pa:Computer st:Computer"),
////			createInflection("-ArbeitComputer", "PUX", "pa:-Arbeit st:Arbeit pa:Computer st:Computer"),
////			createInflection("-ArbeitComputer", "PVWX", "pa:-Arbeit st:Arbeit pa:Computer st:Computer"),
////			createInflection("-Arbeit-Computer", "BCP", "pa:-Arbeit st:Arbeit pa:-Computer st:Computer"),
////			createInflection("-Arbeit-Computer", "P", "pa:-Arbeit st:Arbeit pa:-Computer st:Computer"),
////			createInflection("-Arbeitcomputer", "P", "pa:-Arbeit st:Arbeit pa:computer st:Computer"),
////			createInflection("-ArbeitsComputer", "BCP", "pa:-Arbeits st:Arbeit pa:Computer st:Computer"),
////			createInflection("-ArbeitsComputer", "PUX", "pa:-Arbeits st:Arbeit pa:Computer st:Computer"),
////			createInflection("-ArbeitsComputer", "PVWX", "pa:-Arbeits st:Arbeit pa:Computer st:Computer"),
////			createInflection("-Arbeits-Computer", "BCP", "pa:-Arbeits st:Arbeit pa:-Computer st:Computer"),
////			createInflection("-Arbeits-Computer", "P", "pa:-Arbeits st:Arbeit pa:-Computer st:Computer"),
////			createInflection("-Arbeitscomputer", "P", "pa:-Arbeits st:Arbeit pa:computer st:Computer"),
////			createInflection("arbeitscomputer", "BCP", "pa:arbeits st:Arbeit pa:Computer st:Computer"),
////			createInflection("arbeitscomputer", "PUX", "pa:arbeits st:Arbeit pa:Computer st:Computer"),
////			createInflection("arbeitscomputer", "PVWX", "pa:arbeits st:Arbeit pa:Computer st:Computer"),
////			createInflection("arbeits-Computer", "BCP", "pa:arbeits st:Arbeit pa:-Computer st:Computer"),
////			createInflection("arbeits-Computer", "P", "pa:arbeits st:Arbeit pa:-Computer st:Computer"),
////			createInflection("arbeitscomputer", "P", "pa:arbeits st:Arbeit pa:computer st:Computer"),
////			createInflection("Arbeit-", "-W", "pa:Arbeit st:Arbeit pa:- st:-"),
////			createInflection("Arbeits-", "-PUWX", "pa:Arbeits st:Arbeit pa:- st:-"),
////			createInflection("Arbeits-", "D-PVWX", "pa:Arbeits st:Arbeit pa:- st:-"),
////			createInflection("-Arbeit-", "PW", "pa:-Arbeit st:Arbeit pa:- st:-"),
////			createInflection("-Arbeits-", "PW", "pa:-Arbeits st:Arbeit pa:- st:-"),
////			createInflection("arbeits-", "PW", "pa:arbeits st:Arbeit pa:- st:-"),
////			createInflection("Computerarbeit", "-A", "pa:Computer st:Computer pa:Arbeit st:Arbeit"),
////			createInflection("Computerarbeits", "-PUX", "pa:Computer st:Computer pa:Arbeits st:Arbeit"),
////			createInflection("Computerarbeits", "-PVX", "pa:Computer st:Computer pa:Arbeits st:Arbeit"),
////			createInflection("Computer-Arbeit", "-AP", "pa:Computer st:Computer pa:-Arbeit st:Arbeit"),
////			createInflection("Computer-Arbeits", "-P", "pa:Computer st:Computer pa:-Arbeits st:Arbeit"),
////			createInflection("Computerarbeits", "-P", "pa:Computer st:Computer pa:arbeits st:Arbeit"),
////			createInflection("Computerarbeit", "-APUX", "pa:Computer st:Computer pa:Arbeit st:Arbeit"),
////			createInflection("Computerarbeits", "-PUVX", "pa:Computer st:Computer pa:Arbeits st:Arbeit"),
////			createInflection("Computer-Arbeit", "-APUX", "pa:Computer st:Computer pa:-Arbeit st:Arbeit"),
////			createInflection("Computer-Arbeits", "-PUX", "pa:Computer st:Computer pa:-Arbeits st:Arbeit"),
////			createInflection("Computerarbeits", "-PUX", "pa:Computer st:Computer pa:arbeits st:Arbeit"),
////			createInflection("Computerarbeit", "D-APVWX", "pa:Computer st:Computer pa:Arbeit st:Arbeit"),
////			createInflection("Computerarbeits", "D-PUVWX", "pa:Computer st:Computer pa:Arbeits st:Arbeit"),
////			createInflection("Computerarbeits", "D-PVWX", "pa:Computer st:Computer pa:Arbeits st:Arbeit"),
////			createInflection("Computer-Arbeit", "D-APVWX", "pa:Computer st:Computer pa:-Arbeit st:Arbeit"),
////			createInflection("Computer-Arbeits", "D-PVWX", "pa:Computer st:Computer pa:-Arbeits st:Arbeit"),
////			createInflection("Computerarbeits", "D-PVWX", "pa:Computer st:Computer pa:arbeits st:Arbeit"),
////			createInflection("-ComputerArbeit", "AP", "pa:-Computer st:Computer pa:Arbeit st:Arbeit"),
////			createInflection("-ComputerArbeits", "PUX", "pa:-Computer st:Computer pa:Arbeits st:Arbeit"),
////			createInflection("-ComputerArbeits", "PVX", "pa:-Computer st:Computer pa:Arbeits st:Arbeit"),
////			createInflection("-Computer-Arbeit", "AP", "pa:-Computer st:Computer pa:-Arbeit st:Arbeit"),
////			createInflection("-Computer-Arbeits", "P", "pa:-Computer st:Computer pa:-Arbeits st:Arbeit"),
////			createInflection("-Computerarbeits", "P", "pa:-Computer st:Computer pa:arbeits st:Arbeit"),
////			createInflection("computerarbeit", "AP", "pa:computer st:Computer pa:Arbeit st:Arbeit"),
////			createInflection("computerarbeits", "PUX", "pa:computer st:Computer pa:Arbeits st:Arbeit"),
////			createInflection("computerarbeits", "PVX", "pa:computer st:Computer pa:Arbeits st:Arbeit"),
////			createInflection("computer-Arbeit", "AP", "pa:computer st:Computer pa:-Arbeit st:Arbeit"),
////			createInflection("computer-Arbeits", "P", "pa:computer st:Computer pa:-Arbeits st:Arbeit"),
////			createInflection("computerarbeits", "P", "pa:computer st:Computer pa:arbeits st:Arbeit"),
////			createInflection("Computercomputer", "-BC", "pa:Computer st:Computer pa:Computer st:Computer"),
////			createInflection("Computercomputer", "-PUX", "pa:Computer st:Computer pa:Computer st:Computer"),
////			createInflection("Computercomputer", "-PVWX", "pa:Computer st:Computer pa:Computer st:Computer"),
////			createInflection("Computer-Computer", "-BCP", "pa:Computer st:Computer pa:-Computer st:Computer"),
////			createInflection("Computer-Computer", "-P", "pa:Computer st:Computer pa:-Computer st:Computer"),
////			createInflection("Computercomputer", "-P", "pa:Computer st:Computer pa:computer st:Computer"),
////			createInflection("Computercomputer", "-BCPUX", "pa:Computer st:Computer pa:Computer st:Computer"),
////			createInflection("Computercomputer", "-PUVWX", "pa:Computer st:Computer pa:Computer st:Computer"),
////			createInflection("Computer-Computer", "-BCPUX", "pa:Computer st:Computer pa:-Computer st:Computer"),
////			createInflection("Computer-Computer", "-PUX", "pa:Computer st:Computer pa:-Computer st:Computer"),
////			createInflection("Computercomputer", "-PUX", "pa:Computer st:Computer pa:computer st:Computer"),
////			createInflection("Computercomputer", "D-BCPVWX", "pa:Computer st:Computer pa:Computer st:Computer"),
////			createInflection("Computercomputer", "D-PUVWX", "pa:Computer st:Computer pa:Computer st:Computer"),
////			createInflection("Computercomputer", "D-PVWX", "pa:Computer st:Computer pa:Computer st:Computer"),
////			createInflection("Computer-Computer", "D-BCPVWX", "pa:Computer st:Computer pa:-Computer st:Computer"),
////			createInflection("Computer-Computer", "D-PVWX", "pa:Computer st:Computer pa:-Computer st:Computer"),
////			createInflection("Computercomputer", "D-PVWX", "pa:Computer st:Computer pa:computer st:Computer"),
////			createInflection("-ComputerComputer", "BCP", "pa:-Computer st:Computer pa:Computer st:Computer"),
////			createInflection("-ComputerComputer", "PUX", "pa:-Computer st:Computer pa:Computer st:Computer"),
////			createInflection("-ComputerComputer", "PVWX", "pa:-Computer st:Computer pa:Computer st:Computer"),
////			createInflection("-Computer-Computer", "BCP", "pa:-Computer st:Computer pa:-Computer st:Computer"),
////			createInflection("-Computer-Computer", "P", "pa:-Computer st:Computer pa:-Computer st:Computer"),
////			createInflection("-Computercomputer", "P", "pa:-Computer st:Computer pa:computer st:Computer"),
////			createInflection("computercomputer", "BCP", "pa:computer st:Computer pa:Computer st:Computer"),
////			createInflection("computercomputer", "PUX", "pa:computer st:Computer pa:Computer st:Computer"),
////			createInflection("computercomputer", "PVWX", "pa:computer st:Computer pa:Computer st:Computer"),
////			createInflection("computer-Computer", "BCP", "pa:computer st:Computer pa:-Computer st:Computer"),
////			createInflection("computer-Computer", "P", "pa:computer st:Computer pa:-Computer st:Computer"),
////			createInflection("computercomputer", "P", "pa:computer st:Computer pa:computer st:Computer"),
////			createInflection("Computer-", "-W", "pa:Computer st:Computer pa:- st:-"),
////			createInflection("Computer-", "-PUWX", "pa:Computer st:Computer pa:- st:-"),
////			createInflection("Computer-", "D-PVWX", "pa:Computer st:Computer pa:- st:-"),
////			createInflection("-Computer-", "PW", "pa:-Computer st:Computer pa:- st:-"),
////			createInflection("computer-", "PW", "pa:computer st:Computer pa:- st:-"),
////			createInflection("-Arbeit", "AW", "pa:- st:- pa:Arbeit st:Arbeit"),
////			createInflection("-Arbeits", "PUWX", "pa:- st:- pa:Arbeits st:Arbeit"),
////			createInflection("-Arbeits", "PVWX", "pa:- st:- pa:Arbeits st:Arbeit"),
////			createInflection("--Arbeit", "APW", "pa:- st:- pa:-Arbeit st:Arbeit"),
////			createInflection("--Arbeits", "PW", "pa:- st:- pa:-Arbeits st:Arbeit"),
////			createInflection("-arbeits", "PW", "pa:- st:- pa:arbeits st:Arbeit"),
////			createInflection("-Computer", "BCW", "pa:- st:- pa:Computer st:Computer"),
////			createInflection("-Computer", "PUWX", "pa:- st:- pa:Computer st:Computer"),
////			createInflection("-Computer", "PVWX", "pa:- st:- pa:Computer st:Computer"),
////			createInflection("--Computer", "BCPW", "pa:- st:- pa:-Computer st:Computer"),
////			createInflection("--Computer", "PW", "pa:- st:- pa:-Computer st:Computer"),
////			createInflection("-computer", "PW", "pa:- st:- pa:computer st:Computer"),
////			createInflection("--", "W", "pa:- st:- pa:- st:-")
//		);
//		Assertions.assertEquals(expected, words);
//	}

}
