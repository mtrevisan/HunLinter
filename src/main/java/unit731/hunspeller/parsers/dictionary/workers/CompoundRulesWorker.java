package unit731.hunspeller.parsers.dictionary.workers;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryReadBase;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


public class CompoundRulesWorker extends WorkerDictionaryReadBase{

	public static final String WORKER_NAME = "Compound rules extraction";


	public CompoundRulesWorker(DictionaryParser dicParser, WordGenerator wordGenerator, BiConsumer<Production, Integer> productionReader,
			Runnable completed, ReadWriteLockable lockable){
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(productionReader);
		Objects.requireNonNull(lockable);

		BiConsumer<String, Integer> lineReader = (line, row) -> {
			List<Production> productions = wordGenerator.applyAffixRules(line);
			for(Production production : productions)
				productionReader.accept(production, row);
		};
		createWorker(WORKER_NAME, dicParser, lineReader, completed, null, lockable);
	}

}
