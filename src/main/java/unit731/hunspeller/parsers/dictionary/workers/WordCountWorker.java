package unit731.hunspeller.parsers.dictionary.workers;

import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryBase;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.collections.bloomfilter.BloomFilterInterface;
import unit731.hunspeller.collections.bloomfilter.BloomFilterParameters;
import unit731.hunspeller.collections.bloomfilter.ScalableInMemoryBloomFilter;
import unit731.hunspeller.languages.BaseBuilder;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.generators.WordGenerator;
import unit731.hunspeller.parsers.vos.DictionaryEntry;
import unit731.hunspeller.parsers.vos.Production;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerData;


public class WordCountWorker extends WorkerDictionaryBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordCountWorker.class);

	public static final String WORKER_NAME = "Word count";

	private final AtomicInteger totalProductions = new AtomicInteger(0);
	private final BloomFilterInterface<String> dictionary;


	public WordCountWorker(final String language, final DictionaryParser dicParser, final WordGenerator wordGenerator){
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(wordGenerator);

		final BloomFilterParameters dictionaryBaseData = BaseBuilder.getDictionaryBaseData(language);
		dictionary = new ScalableInMemoryBloomFilter<>(dicParser.getCharset(), dictionaryBaseData);

		final BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
			final List<Production> productions = wordGenerator.applyAffixRules(dicEntry);

			totalProductions.addAndGet(productions.size());
			for(Production production : productions)
				dictionary.add(production.getWord());
		};
		final Runnable completed = () -> {
			dictionary.close();

			final int totalUniqueProductions = dictionary.getAddedElements();
			final double falsePositiveProbability = dictionary.getTrueFalsePositiveProbability();
			final int falsePositiveCount = (int)Math.ceil(totalUniqueProductions * falsePositiveProbability);
			LOGGER.info(Backbone.MARKER_APPLICATION, "Total productions: {}", DictionaryParser.COUNTER_FORMATTER.format(totalProductions));
			LOGGER.info(Backbone.MARKER_APPLICATION, "Total unique productions: {} ± {} ({})",
				DictionaryParser.COUNTER_FORMATTER.format(totalUniqueProductions),
				DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability),
				falsePositiveCount);
		};
		final Consumer<Exception> cancelled = exception -> dictionary.close();
		final WorkerData data = WorkerData.createParallel(WORKER_NAME, dicParser);
		data.setCompletedCallback(completed);
		data.setCancelledCallback(cancelled);
		createReadWorker(data, lineProcessor);
	}

	@Override
	public String getWorkerName(){
		return WORKER_NAME;
	}

	@Override
	public void clear(){
		totalProductions.set(0);
		dictionary.clear();
	}

}
