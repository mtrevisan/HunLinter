package unit731.hunlinter.parsers.workers;

import unit731.hunlinter.parsers.workers.core.WorkerDictionary;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.Backbone;
import unit731.hunlinter.collections.bloomfilter.BloomFilterInterface;
import unit731.hunlinter.collections.bloomfilter.BloomFilterParameters;
import unit731.hunlinter.collections.bloomfilter.ScalableInMemoryBloomFilter;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.parsers.workers.core.WorkerDataDictionary;


public class WordCountWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordCountWorker.class);

	public static final String WORKER_NAME = "Word count";

	private final AtomicInteger totalProductions = new AtomicInteger(0);
	private final BloomFilterInterface<String> dictionary;


	public WordCountWorker(final String language, final DictionaryParser dicParser, final WordGenerator wordGenerator){
		super((WorkerDataDictionary)new WorkerDataDictionary(WORKER_NAME, dicParser)
			.withParallelProcessing(true));

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

		setReadDataProcessor(lineProcessor);
		getWorkerData()
			.withCompletedCallback(completed)
			.withCancelledCallback(cancelled);
	}

}
