package unit731.hunspeller.parsers.dictionary.workers;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.generators.WordGenerator;
import unit731.hunspeller.parsers.dictionary.vos.Production;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerData;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryBase;


public class CompoundRulesWorker extends WorkerDictionaryBase{

	public static final String WORKER_NAME = "Compound rules extraction";


	public CompoundRulesWorker(DictionaryParser dicParser, WordGenerator wordGenerator, BiConsumer<Production, Integer> productionReader,
			Runnable completed){
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(productionReader);

		BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			List<Production> productions = wordGenerator.applyAffixRules(line);
			for(Production production : productions)
				productionReader.accept(production, row);
		};
		WorkerData data = WorkerData.createParallel(WORKER_NAME, dicParser);
		data.setCompletedCallback(completed);
		createReadWorker(data, lineProcessor);
	}

}
