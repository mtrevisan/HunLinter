package unit731.hunspeller.parsers.dictionary.workers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.LineEntry;
import unit731.hunspeller.parsers.dictionary.RulesReducer;
import unit731.hunspeller.parsers.dictionary.generators.WordGenerator;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.dictionary.vos.AffixEntry;
import unit731.hunspeller.parsers.dictionary.vos.Production;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerData;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryBase;


public class RulesReducerWorker extends WorkerDictionaryBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(RulesReducerWorker.class);

	public static final String WORKER_NAME = "Rules reducer";


	private final RulesReducer rulesReducer;


	public RulesReducerWorker(final String flag, final boolean keepLongestCommonAffix, final AffixData affixData, final DictionaryParser dicParser,
			final WordGenerator wordGenerator){
		Objects.requireNonNull(flag);
		Objects.requireNonNull(affixData);
		Objects.requireNonNull(wordGenerator);

		rulesReducer = new RulesReducer(affixData, wordGenerator);

		final RuleEntry ruleToBeReduced = affixData.getData(flag);
		if(ruleToBeReduced == null)
			throw new IllegalArgumentException("Non-existent rule " + flag + ", cannot reduce");

		final AffixEntry.Type type = ruleToBeReduced.getType();

		final List<String> originalLines = new ArrayList<>();
		final List<LineEntry> originalRules = new ArrayList<>();
		final BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			final List<Production> productions = wordGenerator.applyAffixRules(line);

			final LineEntry compactedFilteredRule = rulesReducer.collectProductionsByFlag(productions, flag, type);
			if(compactedFilteredRule != null){
				originalLines.add(line);
				originalRules.add(compactedFilteredRule);
			}
		};
		final Runnable completed = () -> {
			try{
				List<LineEntry> compactedRules = rulesReducer.reduceRules(originalRules);

				List<String> reducedRules = rulesReducer.convertFormat(flag, keepLongestCommonAffix, compactedRules);

				rulesReducer.checkReductionCorrectness(flag, reducedRules, originalRules, originalLines);

				for(final String rule : reducedRules)
					LOGGER.info(Backbone.MARKER_RULE_REDUCER, rule);
			}
			catch(final Exception e){
				LOGGER.info(Backbone.MARKER_RULE_REDUCER, e.getMessage());

				e.printStackTrace();
			}
		};
		//FIXME
//		final WorkerData data = WorkerData.createParallel(WORKER_NAME, dicParser);
final WorkerData data = WorkerData.create(WORKER_NAME, dicParser);
		data.setCompletedCallback(completed);
		createReadWorker(data, lineProcessor);
	}

}
