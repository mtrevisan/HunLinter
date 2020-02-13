package unit731.hunlinter.parsers.workers;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import unit731.hunlinter.languages.DictionaryCorrectnessChecker;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.parsers.workers.core.WorkerDataDictionary;
import unit731.hunlinter.parsers.workers.core.WorkerDictionary;


public class DictionaryLinterWorker extends WorkerDictionary{

	public static final String WORKER_NAME = "Dictionary linter";


	public DictionaryLinterWorker(final DictionaryParser dicParser, final DictionaryCorrectnessChecker checker,
			final WordGenerator wordGenerator){
		super((WorkerDataDictionary)new WorkerDataDictionary(WORKER_NAME, dicParser)
			.withParallelProcessing(true)
			.withPreventExceptionRelaunch(true));

		Objects.requireNonNull(checker);
		Objects.requireNonNull(wordGenerator);


		final BiConsumer<String, Integer> readLineProcessor = (line, row) -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
			final List<Production> productions = wordGenerator.applyAffixRules(dicEntry);

			for(final Production production : productions){
				try{
					checker.checkProduction(production);
				}
				catch(final Exception e){
					throw wrapException(e, production);
				}
			}
		};

		setReadDataProcessor(readLineProcessor);
	}

}
