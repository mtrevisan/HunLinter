package unit731.hunspeller.parsers.dictionary.workers;

import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryReadBase;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;


public class CorrectnessWorker extends WorkerDictionaryReadBase{

	public static final String WORKER_NAME = "Correctness checking";


	public CorrectnessWorker(DictionaryParser dicParser, WordGenerator wordGenerator){
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(wordGenerator);

		BiConsumer<String, Integer> body = (line, row) -> {
			List<RuleProductionEntry> productions = wordGenerator.applyRules(line);

			productions.forEach(production -> dicParser.checkProduction(production));
		};
		createWorker(WORKER_NAME, dicParser, body, null);
	}

}
