package unit731.hunspeller.parsers.dictionary.workers;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.generators.WordGenerator;
import unit731.hunspeller.parsers.vos.DictionaryEntry;
import unit731.hunspeller.parsers.vos.Production;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerData;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryBase;


public class CompoundRulesWorker extends WorkerDictionaryBase{

	public static final String WORKER_NAME = "Compound rules extraction";


	public CompoundRulesWorker(final DictionaryParser dicParser, final WordGenerator wordGenerator,
			final BiConsumer<Production, Integer> productionReader, final Runnable completed){
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(productionReader);

		final BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
			final List<Production> productions = wordGenerator.applyAffixRules(dicEntry);
			for(Production production : productions)
				productionReader.accept(production, row);
		};
		final WorkerData data = WorkerData.createParallel(WORKER_NAME, dicParser);
		data.setCompletedCallback(completed);
		createReadWorker(data, lineProcessor);
	}

	@Override
	public String getWorkerName(){
		return WORKER_NAME;
	}

}
