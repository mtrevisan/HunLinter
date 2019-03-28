package unit731.hunspeller.parsers.dictionary.workers;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import unit731.hunspeller.languages.DictionaryCorrectnessChecker;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.generators.WordGenerator;
import unit731.hunspeller.parsers.dictionary.vos.Production;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerData;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryBase;


public class DictionaryCorrectnessWorker extends WorkerDictionaryBase{

	public static final String WORKER_NAME = "Dictionary correctness checking";


	public DictionaryCorrectnessWorker(DictionaryParser dicParser, DictionaryCorrectnessChecker checker, WordGenerator wordGenerator){
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(checker);

		BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			List<Production> productions = wordGenerator.applyAffixRules(line);

			for(Production production : productions){
				try{
					checker.checkProduction(production);
				}
				catch(Exception e){
					throw wrapException(e, production);
				}
			}
		};
		WorkerData data = WorkerData.createParallelPreventExceptionRelaunch(WORKER_NAME, dicParser);
		createReadWorker(data, lineProcessor);
	}

	private IllegalArgumentException wrapException(Exception e, Production production){
		StringBuffer sb = new StringBuffer(e.getMessage());
		if(production.hasProductionRules())
			sb.append(" (via ").append(production.getRulesSequence()).append(")");
		return new IllegalArgumentException(sb.toString());
	}

}
