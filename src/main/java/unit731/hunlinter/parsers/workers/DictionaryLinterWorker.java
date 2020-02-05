package unit731.hunlinter.parsers.workers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import unit731.hunlinter.languages.DictionaryCorrectnessChecker;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.AffixEntry;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.parsers.workers.core.WorkerData;
import unit731.hunlinter.parsers.workers.core.WorkerDictionaryBase;
import unit731.hunlinter.services.system.JavaHelper;


public class DictionaryLinterWorker extends WorkerDictionaryBase{

	public static final String WORKER_NAME = "Dictionary linter";


	public DictionaryLinterWorker(final DictionaryParser dicParser, final DictionaryCorrectnessChecker checker,
			final WordGenerator wordGenerator){
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(checker);

		final Map<String, List<Production>> bucket = new HashMap<>();
		final BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
			final List<Production> productions = wordGenerator.applyAffixRules(dicEntry);

			for(final Production production : productions){
				try{
					checker.checkProduction(production);
				}
				catch(final Exception e){
					throw wrapException(e, production);
				}
			}

			//generate a graphviz-like structure and bucket it along with the production
			final String key = JavaHelper.nullableToStream(productions.get(0).getAppliedRules())
				.map(AffixEntry::getFlag)
				.collect(Collectors.joining("\t"));
			bucket.computeIfAbsent(key, k -> new ArrayList<>())
				.add(productions.get(0));
		};
		final Runnable completed = () -> {
			bucket.size();
			int sum = bucket.values().stream()
				.map(v -> v.size()).mapToInt(v -> v)
				.sum();
System.out.println();
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
