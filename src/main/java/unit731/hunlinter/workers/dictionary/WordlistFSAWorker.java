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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static unit731.hunlinter.services.system.LoopHelper.forEach;


public class WordlistFSAWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordlistFSAWorker.class);

	public static final String WORKER_NAME = "Wordlist FSA Extractor";


	public WordlistFSAWorker(final DictionaryParser dicParser, final WordGenerator wordGenerator, final File outputFile){
		super((WorkerDataParser<DictionaryParser>)new WorkerDataParser<>(WORKER_NAME, dicParser)
			.withParallelProcessing());

		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(outputFile);

		final Charset charset = dicParser.getCharset();


		final Set<String> words = new HashSet<>();
		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(indexData.getData());
			final Production[] productions = wordGenerator.applyAffixRules(dicEntry);

			forEach(productions, prod -> words.add(prod.toString()));
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
			.withRelaunchException();

		final Function<Void, Set<String>> step1 = ignored -> {
			prepareProcessing("Extract words (step 1/3)");

			final Path dicPath = dicParser.getDicFile().toPath();
			processLines(dicPath, charset, lineProcessor);

			return words;
		};
		final Function<Set<String>, FSA> step2 = uniqueWordsSet -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Create FSA (step 2/3)");

			//lexical order
			final List<String> uniqueWords = new ArrayList<>(uniqueWordsSet);
			Collections.sort(uniqueWords);

//			executeReadProcessNoIndex(fsaProcessor, uniqueWords);

			return builder.complete();
		};
		final Function<FSA, File> step3 = fsa -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Compress FSA (step 3/3)");

			//FIXME
			final CFSA2Serializer serializer = new CFSA2Serializer();
			try(final ByteArrayOutputStream os = new ByteArrayOutputStream()){
				serializer.serialize(fsa, os);

				Files.write(outputFile.toPath(), os.toByteArray());

				finalizeProcessing("Successfully processed " + workerData.getWorkerName() + ": " + outputFile.getAbsolutePath());

				return outputFile;
			}
			catch(final Exception e){
				throw new RuntimeException(e);
			}
		};
		final Function<File, Void> step4 = WorkerManager.openFolderStep(LOGGER);
		setProcessor(step1.andThen(step2).andThen(step3).andThen(step4));
	}

}
