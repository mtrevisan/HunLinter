package unit731.hunlinter.workers.dictionary;

import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;

import java.util.Objects;
import java.util.function.BiConsumer;


public class DictionaryReducerWorker extends WorkerDictionary{

	public static final String WORKER_NAME = "Dictionary reducer";


	public DictionaryReducerWorker(final DictionaryParser dicParser, final AffixData affixData){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser)
			.withParallelProcessing(true)
			.withRelaunchException(false));

		Objects.requireNonNull(affixData);

		final BiConsumer<Integer, String> lineProcessor = (row, line) -> {
			final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(line, affixData);

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

		setReadDataProcessor(lineProcessor);
	}

}