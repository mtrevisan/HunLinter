package unit731.hunspeller.parsers.dictionary.workers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryReadBase;


@Slf4j
public class CompoundRulesWorker extends WorkerDictionaryReadBase{

	public static final String WORKER_NAME = "Compound rules extraction";


	private final Map<String, String> inputs = new HashMap<>();


	public CompoundRulesWorker(AffixParser affParser, DictionaryParser dicParser, WordGenerator wordGenerator, BiConsumer<Production, Integer> productionReader, Runnable done){
		Objects.requireNonNull(affParser);
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(wordGenerator);

		BiConsumer<String, Integer> lineReader = (line, row) -> {
			List<Production> productions = wordGenerator.applyRules(line);
			for(Production production : productions)
				productionReader.accept(production, row);
		};
		createWorker(WORKER_NAME, dicParser, lineReader, done, affParser);
	}

	@Override
	public void execute(){
		inputs.clear();

		super.execute();
	}

}
