package unit731.hunspeller.parsers.dictionary.workers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryReadBase;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


public class CompoundRulesWorker extends WorkerDictionaryReadBase{

	public static final String WORKER_NAME = "Compound rules extraction";


	private final Map<String, String> inputs = new HashMap<>();


	public CompoundRulesWorker(DictionaryParser dicParser, WordGenerator wordGenerator, BiConsumer<Production, Integer> productionReader, Runnable done,
			ReadWriteLockable lockable){
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(productionReader);
		Objects.requireNonNull(lockable);

		BiConsumer<String, Integer> lineReader = (line, row) -> {
			List<Production> productions = wordGenerator.applyRules(line);
			for(Production production : productions)
				productionReader.accept(production, row);
		};
		Runnable wrappedDone = () -> {
			if(!isCancelled() && done != null)
				done.run();
		};
		createWorker(WORKER_NAME, dicParser, lineReader, wrappedDone, lockable);
	}

	@Override
	public void clear(){
		inputs.clear();
	}

}
