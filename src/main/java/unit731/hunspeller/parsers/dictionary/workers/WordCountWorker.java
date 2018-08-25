package unit731.hunspeller.parsers.dictionary.workers;

import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryReadBase;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.languages.CorrectnessChecker;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


@Slf4j
public class WordCountWorker extends WorkerDictionaryReadBase{

	public static final String WORKER_NAME = "Word count";

	private long totalProductions;


	public WordCountWorker(DictionaryParser dicParser, WordGenerator wordGenerator, CorrectnessChecker checker, ReadWriteLockable lockable){
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(lockable);


		BiConsumer<String, Integer> lineReader = (line, row) -> {
			List<Production> productions = wordGenerator.applyRules(line);
			totalProductions += productions.size();
		};
		Runnable done = () -> {
			if(!isCancelled())
				log.info(Backbone.MARKER_APPLICATION, "Total unique productions: {}", DictionaryParser.COUNTER_FORMATTER.format(totalProductions));
		};
		createWorker(WORKER_NAME, dicParser, lineReader, done, lockable);
	}

	@Override
	public void execute(){
		totalProductions = 0l;

		super.execute();
	}

}
