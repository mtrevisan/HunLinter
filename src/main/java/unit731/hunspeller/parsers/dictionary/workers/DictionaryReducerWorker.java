package unit731.hunspeller.parsers.dictionary.workers;

import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerData;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryBase;
import unit731.hunspeller.parsers.vos.DictionaryEntry;
import unit731.hunspeller.parsers.vos.Production;

import java.util.Objects;
import java.util.function.BiConsumer;


public class DictionaryReducerWorker extends WorkerDictionaryBase{

	public static final String WORKER_NAME = "Dictionary reducer";


	public DictionaryReducerWorker(final DictionaryParser dicParser, final AffixData affixData){
		Objects.requireNonNull(affixData);

		final BiConsumer<String, Integer> lineProcessor = (line, row) -> {
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
		final WorkerData data = WorkerData.createParallelPreventExceptionRelaunch(WORKER_NAME, dicParser);
		createReadWorker(data, lineProcessor);
	}

	private IllegalArgumentException wrapException(final Exception e, final Production production){
		final StringBuffer sb = new StringBuffer(e.getMessage());
		if(production.hasProductionRules())
			sb.append(" (via ").append(production.getRulesSequence()).append(")");
		return new IllegalArgumentException(sb.toString());
	}

	@Override
	public String getWorkerName(){
		return WORKER_NAME;
	}

}
