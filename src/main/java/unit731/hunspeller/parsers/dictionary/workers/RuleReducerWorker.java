package unit731.hunspeller.parsers.dictionary.workers;

import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryBase;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.apache.commons.text.similarity.LevenshteinDistance;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


public class RuleReducerWorker extends WorkerDictionaryBase{

	public static final String WORKER_NAME = "Rule reducer";

	private static final LevenshteinDistance LEVENSHTEIN_DISTANCE = LevenshteinDistance.getDefaultInstance();


	public RuleReducerWorker(AffixParser affParser, DictionaryParser dicParser, WordGenerator wordGenerator, ReadWriteLockable lockable){
		Objects.requireNonNull(affParser);
		Objects.requireNonNull(wordGenerator);

String rule = "&0";
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		List<String> aliasesFlag = affParser.getData(AffixTag.ALIASES_FLAG);
		List<String> aliasesMorphologicalField = affParser.getData(AffixTag.ALIASES_MORPHOLOGICAL_FIELD);
		BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLineWithAliases(line, strategy, aliasesFlag, aliasesMorphologicalField);
			dicEntry.applyConversionTable(affParser::applyInputConversionTable);

			if(dicEntry.hasContinuationFlag(rule)){
				String word = dicEntry.getWord();
				List<Production> productions = wordGenerator.applySingleAffixRule(word + "/" + rule);

				//TODO
productions.forEach(production -> {
	int distance = LEVENSHTEIN_DISTANCE.apply(word, production.getWord());
	System.out.println(word + " / " + production.getWord());
});
				//productions.forEach(production -> checker.checkProduction(production));
			}
		};
		createReadParallelWorkerPreventExceptionRelaunch(WORKER_NAME, dicParser, lineProcessor, null, null, lockable);
	}

}
