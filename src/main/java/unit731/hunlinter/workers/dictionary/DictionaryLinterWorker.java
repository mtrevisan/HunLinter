package unit731.hunlinter.workers.dictionary;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.languages.DictionaryCorrectnessChecker;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;
import unit731.hunlinter.workers.exceptions.LinterException;


public class DictionaryLinterWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryLinterWorker.class);

	public static final String WORKER_NAME = "Dictionary linter";


	public DictionaryLinterWorker(final DictionaryParser dicParser, final DictionaryCorrectnessChecker checker,
			final WordGenerator wordGenerator){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser)
//FIXME to be uncommented
//			.withParallelProcessing(true)
			.withRelaunchException(false));

		Objects.requireNonNull(checker);
		Objects.requireNonNull(wordGenerator);


		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(indexData.getData());
//System.out.println("dicEntry: " + com.carrotsearch.sizeof.RamUsageEstimator.sizeOf(dicEntry));
			final Production[] productions = wordGenerator.applyAffixRules(dicEntry);
//System.out.println("base production: " + com.carrotsearch.sizeof.RamUsageEstimator.sizeOf(productions.get(0)));
//System.out.println("productions: " + com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(productions));
/*dicEntry: 576
dicEntry: 408
dicEntry: 592
dicEntry: 352
base production: 360 > 224
productions: 272*/

			for(final Production production : productions){
				try{
					checker.checkProduction(production);
				}
				catch(final Exception e){
					throw wrapException(e, production);
				}
			}
		};

		final Function<Void, List<IndexDataPair<String>>> step1 = ignored -> {
			prepareProcessing("Reading dictionary file (step 1/2)");

			return readLines();
		};
		final Function<List<IndexDataPair<String>>, Void> step2 = lines -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Execute " + workerData.getWorkerName() + " (step 2/2)");

			executeReadProcess(lineProcessor, lines);

			finalizeProcessing("Successfully processed " + workerData.getWorkerName());

			return null;
		};
		setProcessor(step1.andThen(step2));
	}

	private LinterException wrapException(final Exception e, final Production production){
		final StringBuffer sb = new StringBuffer(e.getMessage());
		if(production.hasProductionRules())
			sb.append(" (via ").append(production.getRulesSequence()).append(")");
		return new LinterException(sb.toString());
	}

}
