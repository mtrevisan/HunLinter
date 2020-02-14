package unit731.hunlinter.parsers.workers;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.parsers.workers.core.WorkerDataDictionary;
import unit731.hunlinter.parsers.workers.core.WorkerDictionary;


public class CompoundRulesWorker extends WorkerDictionary{

	public static final String WORKER_NAME = "Compound rules extraction";


	public CompoundRulesWorker(final DictionaryParser dicParser, final WordGenerator wordGenerator,
			final BiConsumer<Production, Integer> productionReader, final Runnable completed){
		super(new WorkerDataDictionary(WORKER_NAME, dicParser)
			.withParallelProcessing(true));

		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(productionReader);


		final BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
			final List<Production> productions = wordGenerator.applyAffixRules(dicEntry);
			for(Production production : productions)
				productionReader.accept(production, row);
		};

		setReadDataProcessor(lineProcessor);
		getWorkerData()
			.withDataCompletedCallback(completed);
	}

}
