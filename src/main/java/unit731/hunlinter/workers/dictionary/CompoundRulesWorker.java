package unit731.hunlinter.workers.dictionary;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;


public class CompoundRulesWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(CompoundRulesWorker.class);

	public static final String WORKER_NAME = "Compound rules extraction";


	public CompoundRulesWorker(final DictionaryParser dicParser, final WordGenerator wordGenerator,
			final BiConsumer<Production, Integer> productionReader, final Runnable completed){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser)
			.withParallelProcessing(true));

		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(productionReader);


		final BiConsumer<Integer, String> lineProcessor = (row, line) -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
			final List<Production> productions = wordGenerator.applyAffixRules(dicEntry);
			for(Production production : productions)
				productionReader.accept(production, row);
		};

		setReadDataProcessor(lineProcessor);
		getWorkerData()
			.withDataCompletedCallback(completed);

		final Function<Void, List<Pair<Integer, String>>> step1 = ignored -> {
			prepareProcessing("Reading dictionary file (step 1/2)");

			return readLines();
		};
		final Function<List<Pair<Integer, String>>, Void> step2 = param -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Execute " + workerData.getWorkerName() + " (step 2/2)");

			executeReadProcess(param);

			return null;
		};
		setProcessor(step1.andThen(step2));
	}

}
