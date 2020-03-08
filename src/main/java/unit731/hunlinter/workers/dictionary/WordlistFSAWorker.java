package unit731.hunlinter.workers.dictionary;

import morfologik.tools.FSACompile;
import morfologik.tools.SerializationFormat;
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
import unit731.hunlinter.services.system.JavaHelper;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;


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
				final File temporaryWordlist = writeProcess(filenameNoExtension, words);

				final File outputInfoFile = new File(filenameNoExtension + ".info");
				if(!outputInfoFile.exists()){
					final Charset charset = dicParser.getCharset();
					final List<String> content = Arrays.asList(
						"fsa.dict.separator=" + Production.POS_FSA_SEPARATOR,
						"fsa.dict.encoding=" + charset.name().toLowerCase(),
						"fsa.dict.encoder=prefix");
					FileHelper.saveFile(outputInfoFile.toPath(), StringUtils.CR, charset, content);
				}

				buildFSA(temporaryWordlist.toString(), filenameNoExtension + ".dict");

				Files.delete(temporaryWordlist.toPath());

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

	private File writeProcess(final String filenameNoExtension, final Set<String> words)
			throws IOException, InterruptedException{
		int writtenSoFar = 0;
		final int totalLines = words.size();
		final DictionaryParser dicParser = workerData.getParser();
		final Charset charset = dicParser.getCharset();
		final File temporaryWordlist = new File(filenameNoExtension + ".txt");
		try(final BufferedWriter writer = Files.newBufferedWriter(temporaryWordlist.toPath(), charset)){
			for(final String word : words){
				try{
					writtenSoFar ++;

					writer.write(word);
					writer.write(StringUtils.LF);

					setProcessingProgress(writtenSoFar, totalLines);

					sleepOnPause();
				}
				catch(final Exception e){
					if(!JavaHelper.isInterruptedException(e))
						LOGGER.info(ParserManager.MARKER_APPLICATION, "{}: {}", e.getMessage(), word);

					throw e;
				}
			}

			return temporaryWordlist;
		}
		catch(final Exception e){
			cancel(e);

			throw e;
		}
	}

	private void buildFSA(final String input, final String output) throws Exception{
		final Path inputPath = Path.of(input);
		final Path outputPath = Path.of(output);
		final SerializationFormat format = SerializationFormat.CFSA2;
		final FSACompile builder = new FSACompile(inputPath, outputPath, format,
			true, true, false);
		builder.call();
	}

}
