package unit731.hunspeller.parsers.dictionary.workers;

import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryReadBase;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;


public class CorrectnessWorker extends WorkerDictionaryReadBase{

	public static final String WORKER_NAME = "Correctness checking";


	public CorrectnessWorker(Backbone backbone){
		Objects.requireNonNull(backbone);
		Objects.requireNonNull(backbone.getDicParser());

		DictionaryParser dicParser = backbone.getDicParser();
		BiConsumer<String, Integer> body = (line, row) -> {
			List<RuleProductionEntry> productions = backbone.applyRules(line);

			productions.forEach(production -> dicParser.checkProduction(production));
		};
		createWorker(WORKER_NAME, dicParser, body, null);
	}

}
