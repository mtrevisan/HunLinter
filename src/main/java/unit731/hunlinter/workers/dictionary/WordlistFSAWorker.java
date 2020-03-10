package unit731.hunlinter.workers.dictionary;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.services.FileHelper;
import unit731.hunlinter.services.fsa.FSA;
import unit731.hunlinter.services.fsa.builders.CFSA2Serializer;
import unit731.hunlinter.services.fsa.builders.FSABuilder;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;


public class WordlistFSAWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordlistFSAWorker.class);

	public static final String WORKER_NAME = "Wordlist FSA Extractor";


	public WordlistFSAWorker(final DictionaryParser dicParser, final WordGenerator wordGenerator, final File outputFile){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser)
			.withParallelProcessing(true));

		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(outputFile);


		final Set<String> words = new HashSet<>();
		final BiConsumer<BufferedWriter, Pair<Integer, String>> lineProcessor = (writer, line) -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line.getValue());
			final List<Production> productions = wordGenerator.applyAffixRules(dicEntry);

			productions.stream()
				.map(Production::getWord)
				.forEach(words::add);
		};
		final Runnable completed = () -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Post-processing");

			try{
				final String filenameNoExtension = FilenameUtils.removeExtension(outputFile.getAbsolutePath());

				final File outputInfoFile = new File(filenameNoExtension + ".info");
				if(!outputInfoFile.exists()){
					final Charset charset = dicParser.getCharset();
					final List<String> content = Arrays.asList(
						"fsa.dict.separator=" + Production.POS_FSA_SEPARATOR,
						"fsa.dict.encoding=" + charset.name().toLowerCase(),
						"fsa.dict.encoder=prefix");
					FileHelper.saveFile(outputInfoFile.toPath(), StringUtils.CR, charset, content);
				}

				buildFSA(new ArrayList<>(words), filenameNoExtension + ".dict");

				finalizeProcessing("File written: " + outputFile.getAbsolutePath());

				FileHelper.browse(outputFile);
			}
			catch(final Exception e){
				LOGGER.warn("Exception while creating the FSA file for wordlist", e);
			}
		};

		setWriteDataProcessor(lineProcessor, outputFile);
		getWorkerData()
			.withDataCompletedCallback(completed);
	}

	private void buildFSA(final List<String> words, final String output) throws Exception{
		//lexical order
		Collections.sort(words);

		final Path outputPath = Path.of(output);

		List<byte[]> input = words.stream()
			.map(word -> word.getBytes(StandardCharsets.UTF_8))
			.collect(Collectors.toList());
		final FSA fsa = FSABuilder.build(input);

		final CFSA2Serializer serializer = new CFSA2Serializer();
		try(final OutputStream os = new BufferedOutputStream(Files.newOutputStream(outputPath))){
			serializer.serialize(fsa, os);
		}
	}

}
