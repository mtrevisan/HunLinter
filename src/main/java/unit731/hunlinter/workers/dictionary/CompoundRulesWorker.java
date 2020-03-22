package unit731.hunlinter.workers.dictionary;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;


public class CompoundRulesWorker extends WorkerDictionary{

	public static final String WORKER_NAME = "Compound rules extraction";


	public CompoundRulesWorker(final DictionaryParser dicParser, final WordGenerator wordGenerator,
			final BiConsumer<Production, Integer> productionReader, final Runnable completed){
		super((WorkerDataParser<DictionaryParser>)new WorkerDataParser<>(WORKER_NAME, dicParser)
			.withParallelProcessing());

		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(productionReader);


		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(indexData.getData());
			final Production[] productions = wordGenerator.applyAffixRules(dicEntry);
			for(final Production production : productions)
				productionReader.accept(production, indexData.getIndex());
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
