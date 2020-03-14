package unit731.hunlinter.workers.dictionary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.services.fsa.FSA;
import unit731.hunlinter.services.fsa.serializers.CFSA2Serializer;
import unit731.hunlinter.services.fsa.builders.FSABuilder;
import unit731.hunlinter.services.system.JavaHelper;
import unit731.hunlinter.services.text.StringHelper;
import unit731.hunlinter.workers.WorkerManager;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;


public class WordlistFSAWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordlistFSAWorker.class);

	public static final String WORKER_NAME = "Wordlist FSA Extractor";


	public WordlistFSAWorker(final DictionaryParser dicParser, final WordGenerator wordGenerator, final File outputFile){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser)
			.withParallelProcessing(true));

		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(outputFile);

		final Charset charset = dicParser.getCharset();


		final Set<String> words = new HashSet<>();
		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(indexData.getData());
			final List<Production> productions = wordGenerator.applyAffixRules(dicEntry);

			productions.stream()
				.map(Production::getWord)
				.forEach(words::add);
		};
		final FSABuilder builder = new FSABuilder();
		final Consumer<String> fsaProcessor = word -> {
			final byte[] chs = StringHelper.getRawBytes(word);
			builder.add(chs);
		};
//		final Runnable completed = () -> {
//			LOGGER.info(ParserManager.MARKER_APPLICATION, "Post-processing");
//
//			try{
//				final String filenameNoExtension = FilenameUtils.removeExtension(outputFile.getAbsolutePath());
//
//				final File outputInfoFile = new File(filenameNoExtension + ".info");
//				if(!outputInfoFile.exists()){
//					final List<String> content = Arrays.asList(
//						"fsa.dict.separator=" + Production.POS_FSA_SEPARATOR,
//						"fsa.dict.encoding=" + charset.name().toLowerCase(),
//						"fsa.dict.encoder=prefix");
//					FileHelper.saveFile(outputInfoFile.toPath(), StringUtils.CR, charset, content);
//				}
//
//				buildFSA(new ArrayList<>(words), filenameNoExtension + ".dict");
//
//				finalizeProcessing("File written: " + outputFile.getAbsolutePath());
//
//				FileHelper.browse(outputFile);
//			}
//			catch(final Exception e){
//				LOGGER.warn("Exception while creating the FSA file for wordlist", e);
//			}
//		};
//
		getWorkerData()
//			.withDataCompletedCallback(completed)
			.withRelaunchException(true);

		final Function<Void, List<IndexDataPair<String>>> step1 = ignored -> {
			prepareProcessing("Reading dictionary file (step 1/4)");

			return readLines();
		};
		final Function<List<IndexDataPair<String>>, Set<String>> step2 = lines -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Extract words (step 2/4)");

			executeReadProcess(lineProcessor, lines);

			return words;
		};
		final Function<Set<String>, FSA> step3 = uniqueWordsSet -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Create FSA (step 3/4)");

			//lexical order
			final List<String> uniqueWords = new ArrayList<>(uniqueWordsSet);
			Collections.sort(uniqueWords);

			executeReadProcessNoIndex(fsaProcessor, uniqueWords);

			return builder.complete();
		};
		final Function<FSA, File> step4 = fsa -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Compress FSA (step 4/4)");

			//FIXME
final AtomicLong memoryUsage = new AtomicLong(0l);
			final CFSA2Serializer serializer = new CFSA2Serializer();
			try(final OutputStream os = new BufferedOutputStream(Files.newOutputStream(outputFile.toPath()))){
				serializer.serialize(fsa, os);

final long currentMemoryUsage = JavaHelper.getUsedMemory();
if(currentMemoryUsage > memoryUsage.get()){
	memoryUsage.set(currentMemoryUsage);
	System.out.println("cipni: " + StringHelper.byteCountToHumanReadable(currentMemoryUsage));

	System.gc();
}//? GiB
			}
			catch(final Exception e){
				throw new RuntimeException(e);
			}

			return outputFile;
		};
		final Function<File, Void> step5 = file -> {
			finalizeProcessing("Successfully processed " + workerData.getWorkerName() + ": " + file.getAbsolutePath());

			WorkerManager.openFolderStep(LOGGER).apply(file);

			return null;
		};
		setProcessor(step1.andThen(step2).andThen(step3).andThen(step4).andThen(step5));
	}

}
