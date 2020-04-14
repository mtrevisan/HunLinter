package unit731.hunlinter.workers.dictionary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.datastructures.SimpleDynamicArray;
import unit731.hunlinter.datastructures.fsa.FSA;
import unit731.hunlinter.datastructures.fsa.builders.LexicographicalComparator;
import unit731.hunlinter.datastructures.fsa.builders.MetadataBuilder;
import unit731.hunlinter.datastructures.fsa.serializers.CFSA2Serializer;
import unit731.hunlinter.datastructures.fsa.builders.FSABuilder;
import unit731.hunlinter.services.sorters.SmoothSort;
import unit731.hunlinter.services.text.StringHelper;
import unit731.hunlinter.workers.WorkerManager;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;


public class WordlistFSAWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordlistFSAWorker.class);

	public static final String WORKER_NAME = "Wordlist FSA Extractor";


	public WordlistFSAWorker(final AffixData affixData, final DictionaryParser dicParser, final WordGenerator wordGenerator,
			final File outputFile){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser));

		getWorkerData()
			.withParallelProcessing()
			.withCancelOnException();

		Objects.requireNonNull(affixData);
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(outputFile);


		final Charset charset = dicParser.getCharset();
		try{
			final Path metadataPath = MetadataBuilder.getMetadataPath(outputFile);
			if(!metadataPath.toFile().exists())
				MetadataBuilder.create(affixData, "NONE", metadataPath, charset);
		}
		catch(final Exception e){
			throw new RuntimeException(e);
		}


		final SimpleDynamicArray<byte[]> encodings = new SimpleDynamicArray<>(byte[].class, 50_000_000, 1.2f);
		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final String line = indexData.getData();
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
			final Inflection[] inflections = wordGenerator.applyAffixRules(dicEntry);

			final byte[][] words = new byte[inflections.length][];
			for(int i = 0; i < inflections.length; i ++)
				words[i] = StringHelper.getRawBytes(inflections[i].getWord());
			encodings.addAll(words);

			sleepOnPause();
		};
		final FSABuilder builder = new FSABuilder();
		final Consumer<byte[]> fsaProcessor = builder::add;

		final Function<Void, SimpleDynamicArray<byte[]>> step1 = ignored -> {
			prepareProcessing("Reading dictionary file (step 1/4)");

			final Path dicPath = dicParser.getDicFile().toPath();
			processLines(dicPath, charset, lineProcessor);

			return encodings;
		};
		final Function<SimpleDynamicArray<byte[]>, SimpleDynamicArray<byte[]>> step2 = list -> {
			resetProcessing("Sorting (step 2/4)");

			//sort list
			SmoothSort.sort(list.data, 0, list.limit, LexicographicalComparator.lexicographicalComparator(),
				percent -> {
					setProgress(percent, 100);

					sleepOnPause();
				});

			return list;
		};
		final Function<SimpleDynamicArray<byte[]>, FSA> step3 = list -> {
			resetProcessing("Creating FSA (step 3/4)");

			getWorkerData()
				.withNoHeader()
				.withSequentialProcessing();

			int progress = 0;
			int progressIndex = 0;
			final int progressStep = (int)Math.ceil(list.limit / 100.f);
			for(int index = 0; index < list.limit; index ++){
				final byte[] encoding = list.data[index];
				fsaProcessor.accept(encoding);

				//release memory
				list.data[index] = null;

				if(++ progress % progressStep == 0)
					setProgress(++ progressIndex, 100);

				sleepOnPause();
			}

			//release memory
			list.clear();

			return builder.complete();
		};
		final Function<FSA, File> step4 = fsa -> {
			resetProcessing("Compress FSA (step 4/4)");

			final CFSA2Serializer serializer = new CFSA2Serializer();
			try(final ByteArrayOutputStream os = new ByteArrayOutputStream()){
				serializer.serialize(fsa, os, percent -> {
					setProgress(percent, 100);

					sleepOnPause();
				});

				Files.write(outputFile.toPath(), os.toByteArray());

				finalizeProcessing("Successfully processed " + workerData.getWorkerName() + ": " + outputFile.getAbsolutePath());

				return outputFile;
			}
			catch(final Exception e){
				throw new RuntimeException(e.getMessage());
			}
		};
		final Function<File, Void> step5 = WorkerManager.openFolderStep(LOGGER);
		setProcessor(step1.andThen(step2).andThen(step3).andThen(step4).andThen(step5));
	}

}
