package unit731.hunspeller.parsers.dictionary.workers;

import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryReadBase;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import unit731.hunspeller.languages.CorrectnessChecker;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;


public class CorrectnessWorker extends WorkerDictionaryReadBase{

	public static final String WORKER_NAME = "Correctness checking";


	public CorrectnessWorker(DictionaryParser dicParser, CorrectnessChecker checker, WordGenerator wordGenerator){
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(wordGenerator);

		BiConsumer<String, Integer> lineaReader = (line, row) -> {
			List<Production> productions = wordGenerator.applyRules(line);

			productions.forEach(production -> checker.checkProduction(production));
		};
		createWorker(WORKER_NAME, dicParser, lineaReader, null);
	}

}
