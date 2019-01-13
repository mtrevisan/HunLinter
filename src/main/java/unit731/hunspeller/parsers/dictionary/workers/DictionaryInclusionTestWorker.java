package unit731.hunspeller.parsers.dictionary.workers;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.collections.bloomfilter.BloomFilterInterface;
import unit731.hunspeller.collections.bloomfilter.BloomFilterParameters;
import unit731.hunspeller.collections.bloomfilter.ScalableInMemoryBloomFilter;
import unit731.hunspeller.languages.BaseBuilder;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.generators.WordGenerator;
import unit731.hunspeller.parsers.dictionary.vos.Production;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerData;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryBase;


public class DictionaryInclusionTestWorker extends WorkerDictionaryBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryInclusionTestWorker.class);

	public static final String WORKER_NAME = "Dictionary inclusion test";

	private final BloomFilterInterface<String> dictionary;


	public DictionaryInclusionTestWorker(String language, DictionaryParser dicParser, WordGenerator wordGenerator){
		Objects.requireNonNull(language);
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(wordGenerator);

		BloomFilterParameters dictionaryBaseData = BaseBuilder.getDictionaryBaseData(language);
		dictionary = new ScalableInMemoryBloomFilter<>(dicParser.getCharset(), dictionaryBaseData);

		BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			List<Production> productions = wordGenerator.applyAffixRules(line);

			productions.forEach(production -> dictionary.add(production.getWord()));
		};
		Runnable completed = () -> {
			dictionary.close();

			int totalUniqueProductions = dictionary.getAddedElements();
			double falsePositiveProbability = dictionary.getTrueFalsePositiveProbability();
			int falsePositiveCount = (int)Math.ceil(totalUniqueProductions * falsePositiveProbability);
			LOGGER.info(Backbone.MARKER_APPLICATION, "Total unique productions: {} ± {} ({})",
				DictionaryParser.COUNTER_FORMATTER.format(totalUniqueProductions),
				DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability),
				falsePositiveCount);
		};
		Runnable cancelled = () -> {
			dictionary.close();
		};
		WorkerData data = WorkerData.createParallel(WORKER_NAME, dicParser);
		data.setCompletedCallback(completed);
		data.setCancelledCallback(cancelled);
		createReadWorker(data, lineProcessor);
	}

	@Override
	public void clear(){
		dictionary.clear();
	}

	public boolean isInDictionary(String word){
		return dictionary.contains(word);
	}

}
