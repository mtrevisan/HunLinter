package unit731.hunspeller.parsers.dictionary.workers;

import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryBase;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.collections.bloomfilter.BloomFilterInterface;
import unit731.hunspeller.collections.bloomfilter.ScalableInMemoryBloomFilter;
import unit731.hunspeller.languages.DictionaryBaseData;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.vos.Production;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


public class WordCountWorker extends WorkerDictionaryBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordCountWorker.class);

	public static final String WORKER_NAME = "Word count";

	private final AtomicInteger totalProductions = new AtomicInteger(0);
	private final BloomFilterInterface<String> dictionary;


	public WordCountWorker(DictionaryParser dicParser, WordGenerator wordGenerator, DictionaryBaseData dictionaryBaseData,
			ReadWriteLockable lockable){
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(lockable);

		dictionary = new ScalableInMemoryBloomFilter<>(dicParser.getCharset(), dictionaryBaseData.getExpectedNumberOfElements(),
			dictionaryBaseData.getFalsePositiveProbability(), dictionaryBaseData.getGrowRatioWhenFull());

		BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			List<Production> productions = wordGenerator.applyAffixRules(line);

			totalProductions.addAndGet(productions.size());
			for(Production production : productions)
				dictionary.add(production.getWord());
		};
		Runnable completed = () -> {
			dictionary.close();

			int totalUniqueProductions = dictionary.getAddedElements();
			double falsePositiveProbability = dictionary.getTrueFalsePositiveProbability();
			int falsePositiveCount = (int)Math.ceil(totalUniqueProductions * falsePositiveProbability);
			LOGGER.info(Backbone.MARKER_APPLICATION, "Total productions: {}", DictionaryParser.COUNTER_FORMATTER.format(totalProductions));
			LOGGER.info(Backbone.MARKER_APPLICATION, "Total unique productions: {} ± {} ({})",
				DictionaryParser.COUNTER_FORMATTER.format(totalUniqueProductions),
				DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability),
				falsePositiveCount);
		};
		Runnable cancelled = () -> {
			dictionary.close();
		};
		createReadParallelWorker(WORKER_NAME, dicParser, lineProcessor, completed, cancelled, lockable);
	}

	@Override
	public void clear(){
		totalProductions.set(0);
		dictionary.clear();
	}

}
