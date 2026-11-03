package io.github.mtrevisan.hunlinter.parsers.dictionary;

import io.github.mtrevisan.hunlinter.languages.BaseBuilder;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixParser;
import io.github.mtrevisan.hunlinter.parsers.dictionary.generators.WordGenerator;
import io.github.mtrevisan.hunlinter.parsers.enums.AffixType;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.HyphenationParser;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.Hyphenator;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntryFactory;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.parsers.vos.RuleEntry;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Confronta il numero totale di righe prodotte dal {@code RulesReducer} originale e dalla versione
 * modificata {@code RulesReducer2}, eseguendo la riduzione su <b>ogni</b> flag SFX e PFX del dizionario veneto.
 */
class RulesReducerComparisonTest{

	private static final String LANGUAGE = "vec-IT";
	private static final File AFF_FILE = new File("C:\\mauro\\personal\\projects\\VenetanSpellChecker\\dict-vec-IT-oxt\\dictionaries\\venetan.aff");
	private static final File HYP_FILE = new File("C:\\mauro\\personal\\projects\\VenetanSpellChecker\\dict-vec-IT-oxt\\dictionaries\\hyph_vec.dic");
	private static final File DIC_FILE = new File("C:\\mauro\\personal\\projects\\VenetanSpellChecker\\dict-vec-IT-oxt\\dictionaries\\venetan.dic");


	@Test
	void compareOriginalVsModified() throws IOException{
		Assumptions.assumeTrue(AFF_FILE.isFile() && DIC_FILE.isFile(), "Venetan dictionary files not found");

		final AffixParser affParser = new AffixParser();
		affParser.parse(AFF_FILE, LANGUAGE);
		final AffixData affixData = affParser.getAffixData();
		final Charset charset = affixData.getCharset();
		final DictionaryParser dicParser = new DictionaryParser(DIC_FILE, affixData.getLanguage(), charset);
		//same checker the application uses (it tolerates continuation flags without applicable rules)
		final HyphenationParser hypParser = new HyphenationParser(BaseBuilder.getComparator(affixData.getLanguage()));
		hypParser.parse(HYP_FILE);
		final Hyphenator hyphenator = new Hyphenator(hypParser, HyphenationParser.BREAK_CHARACTER);
		final WordGenerator wordGenerator = new WordGenerator(affixData, dicParser,
			BaseBuilder.getCorrectnessChecker(affixData, hyphenator));
		final RulesReducerLegacy reducer1 = new RulesReducerLegacy(affixData, wordGenerator);
		final RulesReducer reducer2 = new RulesReducer(affixData, wordGenerator);
		final DictionaryEntryFactory factory = new DictionaryEntryFactory(affixData);

		//skip the first line (the number of entries) and the empty ones
		final List<String> dicLines = new ArrayList<>();
		final List<String> allLines = Files.readAllLines(DIC_FILE.toPath(), charset);
		for(int i = 1; i < allLines.size(); i ++){
			final String line = allLines.get(i).trim();
			if(!line.isEmpty() && !line.startsWith("#") && !line.startsWith("\t")){
				try{
					factory.createFromDictionaryLine(line);
					dicLines.add(line);
				}
				catch(final Exception e){
					System.out.println("Skipped malformed dictionary line " + (i + 1) + ": " + line);
				}
			}
		}

		long total1 = 0;
		long total2 = 0;
		int equivalent = 0;
		int different = 0;
		int failed = 0;
		int processed = 0;
		final StringBuilder report = new StringBuilder();
		report.append(String.format("%-8s %-4s %10s %10s %6s  %s%n", "FLAG", "TYPE", "ORIGINAL", "REDUCER2", "DIFF", "SAME OUTPUT"));

		for(final RuleEntry rule : affixData.getRuleEntries()){
			final String flag = rule.getFlag();
			//optional restriction to some flags: -Dcmp.flags=A0,Pd,...
			final String onlyFlags = System.getProperty("cmp.flags");
			if(onlyFlags != null && !Set.of(onlyFlags.split(",")).contains(flag))
				continue;
			final AffixType type = rule.getType();
			//progress and partial report, written before each flag (the run can take very long)
			Files.writeString(new File("target", "RulesReducerComparisonProgress.txt").toPath(),
				java.time.LocalTime.now().withNano(0) + " flag " + flag + " (" + (++ processed) + ")" + System.lineSeparator(),
				java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
			Files.writeString(new File("target", "RulesReducerComparisonReport.txt").toPath(), report.toString());
			try{
				final Set<String> leadingFlags = reducer1.collectFlagsLeadingTo(flag);
				final Set<String> originalLines = ConcurrentHashMap.newKeySet();
				final Set<String> inertWords = ConcurrentHashMap.newKeySet();
				final RulesReducer.FactCollector originalRules = new RulesReducer.FactCollector();
				dicLines.parallelStream().forEach(line -> {
					final DictionaryEntry dicEntry = factory.createFromDictionaryLine(line);
					if(!RulesReducerLegacy.hasAnyFlag(dicEntry, leadingFlags))
						return;

					final List<Inflection> inflections = wordGenerator.applyAffixRulesWithoutOutputConversion(dicEntry);
					final List<LineEntry> rules = reducer2.collectInflectionsByFlag(inflections, flag, type);
					inertWords.addAll(reducer2.collectInertWords(inflections, flag, type, rules));
					if(rules != null && !rules.isEmpty()){
						originalLines.add(line);
						originalRules.add(rules);
					}
				});
				if(originalRules.isEmpty())
					continue;

				final List<String> out1 = reducer1.convertFormat(flag, false,
					new ArrayList<>(reducer1.reduceRules(originalRules.toRules(), inertWords, null)));
				final List<String> out2 = reducer2.convertFormat(flag, false,
					new ArrayList<>(reducer2.reduceRules(originalRules.toRules(), inertWords, null)));
				reducer1.checkReductionCorrectness(flag, out1, originalLines, null);
				reducer2.checkReductionCorrectness(flag, out2, originalLines, null);

				//the first line is the header
				final int size1 = out1.size() - 1;
				final int size2 = out2.size() - 1;
				total1 += size1;
				total2 += size2;
				final boolean same = new HashSet<>(out1.subList(1, out1.size()))
					.equals(new HashSet<>(out2.subList(1, out2.size())));
				if(same)
					equivalent ++;
				else
					different ++;
				report.append(String.format("%-8s %-4s %10d %10d %+6d  %s%n", flag, type == AffixType.SUFFIX? "SFX": "PFX",
					size1, size2, size2 - size1, same? "yes": "no"));
			}
			catch(final Exception e){
				failed ++;
				report.append(String.format("%-8s %-4s FAILED: %s%n", flag, type == AffixType.SUFFIX? "SFX": "PFX", e.getMessage()));
				}
				catch(final OutOfMemoryError e){
					failed ++;
					report.append(String.format("%-8s %-4s FAILED: %s%n", flag, type == AffixType.SUFFIX? "SFX": "PFX", e));
			}
		}

		report.append(String.format("%nTOTAL rule lines: RulesReducer=%d, RulesReducer2=%d (diff %+d)%n", total1, total2, total2 - total1));
		report.append(String.format("Flags with identical output=%d, different=%d, failed=%d%n", equivalent, different, failed));
		System.out.println(report);
		Files.writeString(new File("target", "RulesReducerComparisonReport.txt").toPath(), report.toString());
	}

}
