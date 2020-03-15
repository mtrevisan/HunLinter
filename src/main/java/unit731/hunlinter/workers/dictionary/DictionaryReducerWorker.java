package unit731.hunlinter.workers.dictionary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;


public class DictionaryReducerWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryReducerWorker.class);

	public static final String WORKER_NAME = "Dictionary reducer";


	public DictionaryReducerWorker(final DictionaryParser dicParser, final AffixData affixData){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser)
			.withParallelProcessing(true)
			.withRelaunchException(false));

		Objects.requireNonNull(affixData);

		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(indexData.getData(), affixData);

//TODO
//			for(final Production production : productions){
//				try{
//					checker.checkProduction(production);
//				}
//				catch(final Exception e){
//					throw wrapException(e, production);
//				}
//			}
		};

		final Function<Void, List<IndexDataPair<String>>> step1 = ignored -> {
			prepareProcessing("Execute " + workerData.getWorkerName());

			processLines(lineProcessor);

			finalizeProcessing("Successfully processed " + workerData.getWorkerName());

			return null;
		};
		setProcessor(step1);
	}

}
