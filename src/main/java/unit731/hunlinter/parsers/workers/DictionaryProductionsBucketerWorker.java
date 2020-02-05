package unit731.hunlinter.parsers.workers;

import unit731.hunlinter.languages.DictionaryCorrectnessChecker;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.parsers.workers.core.WorkerData;
import unit731.hunlinter.parsers.workers.core.WorkerDictionaryBase;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;


public class DictionaryProductionsBucketerWorker extends WorkerDictionaryBase{

	public static final String WORKER_NAME = "Dictionary productions bucketer";


	public DictionaryProductionsBucketerWorker(final DictionaryParser dicParser, final DictionaryCorrectnessChecker checker, final WordGenerator wordGenerator){
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(checker);

		final BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
			final List<Production> productions = wordGenerator.applyAffixRules(dicEntry);

			for(final Production production : productions){
				//TODO generate graphviz and bucket it along with the production
			}
		};
		final WorkerData data = WorkerData.createParallelPreventExceptionRelaunch(WORKER_NAME, dicParser);
		createReadWorker(data, lineProcessor);
	}

	@Override
	public String getWorkerName(){
		return WORKER_NAME;
	}

}
