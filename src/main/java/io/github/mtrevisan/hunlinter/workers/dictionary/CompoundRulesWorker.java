package io.github.mtrevisan.hunlinter.workers.dictionary;

import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.parsers.dictionary.generators.WordGenerator;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.workers.core.IndexDataPair;
import io.github.mtrevisan.hunlinter.workers.core.WorkerDataParser;
import io.github.mtrevisan.hunlinter.workers.core.WorkerDictionary;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;


public class CompoundRulesWorker extends WorkerDictionary{

	public static final String WORKER_NAME = "Compound rules extraction";


	public CompoundRulesWorker(final ParserManager parserManager, final BiConsumer<Inflection, Integer> inflectionReader,
			final Runnable completed){
		this(parserManager.getDicParser(), parserManager.getWordGenerator(), inflectionReader, completed);
	}

	public CompoundRulesWorker(final DictionaryParser dicParser, final WordGenerator wordGenerator,
			final BiConsumer<Inflection, Integer> inflectionReader, final Runnable completed){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser));

		getWorkerData()
			.withParallelProcessing()
			.withCancelOnException();

		Objects.requireNonNull(wordGenerator, "Word generator cannot be null");
		Objects.requireNonNull(inflectionReader, "Inflection reader cannot be null");


		//TODO instead of generating each inflection, try only the flag tree that would generate the compound words (that is the path able to generate a compound flag)
		//TODO scan the affix file and for each SFX/PFX, collect all the flags that can be generated

		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(indexData.getData());
			final Inflection[] inflections = wordGenerator.applyAffixRules(dicEntry);
			for(final Inflection inflection : inflections)
				inflectionReader.accept(inflection, indexData.getIndex());
		};

		getWorkerData()
			.withDataCompletedCallback(completed);

		final Function<Void, List<IndexDataPair<String>>> step1 = ignored -> {
			prepareProcessing("Execute " + workerData.getWorkerName());

			final Path dicPath = dicParser.getDicFile().toPath();
			final Charset charset = dicParser.getCharset();
			processLines(dicPath, charset, lineProcessor);

			finalizeProcessing("Successfully processed " + workerData.getWorkerName());

			return null;
		};
		setProcessor(step1);
	}

}