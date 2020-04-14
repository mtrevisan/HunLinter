package unit731.hunlinter.workers.dictionary;

import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.datastructures.bloomfilter.BloomFilterInterface;
import unit731.hunlinter.datastructures.bloomfilter.BloomFilterParameters;
import unit731.hunlinter.datastructures.bloomfilter.ScalableInMemoryBloomFilter;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;


public class WordCountWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordCountWorker.class);

	public static final String WORKER_NAME = "Word count";

	private final AtomicInteger totalInflections = new AtomicInteger(0);
	private final BloomFilterInterface<String> dictionary;


	public WordCountWorker(final String language, final DictionaryParser dicParser, final WordGenerator wordGenerator){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser));

		getWorkerData()
			.withParallelProcessing()
			.withCancelOnException();

		Objects.requireNonNull(wordGenerator);


		final BloomFilterParameters dictionaryBaseData = BaseBuilder.getDictionaryBaseData(language);
		dictionary = new ScalableInMemoryBloomFilter<>(dicParser.getCharset(), dictionaryBaseData);

		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(indexData.getData());
			final Inflection[] inflections = wordGenerator.applyAffixRules(dicEntry);

			totalInflections.addAndGet(inflections.length);
			for(final Inflection inflection : inflections)
				dictionary.add(inflection.getWord());
		};
		final Consumer<Exception> cancelled = exception -> dictionary.close();

		getWorkerData()
			.withDataCancelledCallback(cancelled);

		final Function<Void, Void> step1 = ignored -> {
			prepareProcessing("Execute " + workerData.getWorkerName());

			final Path dicPath = dicParser.getDicFile().toPath();
			final Charset charset = dicParser.getCharset();
			processLines(dicPath, charset, lineProcessor);

			finalizeProcessing("Successfully processed " + workerData.getWorkerName());

			return null;
		};
		final Function<Void, Void> step2 = ignored -> {
			dictionary.close();

			final int totalUniqueInflections = dictionary.getAddedElements();
			final double falsePositiveProbability = dictionary.getTrueFalsePositiveProbability();
			final int falsePositiveCount = (int)Math.ceil(totalUniqueInflections * falsePositiveProbability);
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Total inflections: {}", DictionaryParser.COUNTER_FORMATTER.format(totalInflections));
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Total unique inflections: {} ± {} ({})",
				DictionaryParser.COUNTER_FORMATTER.format(totalUniqueInflections),
				DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability),
				falsePositiveCount);

			return null;
		};
		setProcessor(step1.andThen(step2));
	}

}
