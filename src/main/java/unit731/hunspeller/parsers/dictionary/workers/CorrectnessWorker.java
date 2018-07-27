package unit731.hunspeller.parsers.dictionary.workers;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;


public class CorrectnessWorker extends WorkerDictionaryReadBase{

	public CorrectnessWorker(Backbone backbone){
		Objects.requireNonNull(backbone);


		Consumer<String> body = (line) -> {
			List<RuleProductionEntry> productions = backbone.applyRules(line);

			productions.forEach(production -> backbone.checkDictionaryProduction(production));
		};
		createWorker(backbone, body, null);
	}

}
