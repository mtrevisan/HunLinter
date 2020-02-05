package unit731.hunlinter.parsers.workers;

import unit731.hunlinter.languages.DictionaryCorrectnessChecker;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.AffixEntry;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.parsers.workers.core.WorkerData;
import unit731.hunlinter.parsers.workers.core.WorkerDictionaryBase;
import unit731.hunlinter.services.SetHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;


public class DictionaryProductionsBucketerWorker extends WorkerDictionaryBase{

	public static final String WORKER_NAME = "Dictionary productions bucketer";


	public DictionaryProductionsBucketerWorker(final DictionaryParser dicParser, final DictionaryCorrectnessChecker checker, final WordGenerator wordGenerator){
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(checker);

		final Map<String, List<Production>> bucket = new HashMap<>();
		final BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
			final List<Production> productions = wordGenerator.applyAffixRules(dicEntry);

			//generate a graphviz-like structure and bucket it along with the production
			final Map<String, List<Production>> entryBucket = SetHelper.bucket(productions,
				production -> production.getAppliedRules().stream().map(AffixEntry::getFlag).collect(Collectors.joining("\t")));
			bucket.putAll(entryBucket);
		};
		final Runnable completed = () -> {
			bucket.size();

			//TODO analize data
		};
		final WorkerData data = WorkerData.createParallelPreventExceptionRelaunch(WORKER_NAME, dicParser);
		data.setCompletedCallback(completed);
		createReadWorker(data, lineProcessor);
	}

	@Override
	public String getWorkerName(){
		return WORKER_NAME;
	}

}
