package unit731.hunlinter.workers.dictionary;

import morfologik.tools.DictCompile;
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
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.FileHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;


public class PoSFSAWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(PoSFSAWorker.class);

	public static final String WORKER_NAME = "Part–of–Speech FSA Extractor";


	public PoSFSAWorker(final DictionaryParser dicParser, final WordGenerator wordGenerator, final File outputFile){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser)
			.withParallelProcessing(true));

		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(outputFile);


		final BiConsumer<BufferedWriter, Pair<Integer, String>> lineProcessor = (writer, line) -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line.getValue());
			final List<Production> productions = wordGenerator.applyAffixRules(dicEntry);

			try{
				for(final Production production : productions)
					for(final String text : production.toStringPoSFSA()){
						writer.write(text);
						writer.write(StringUtils.LF);
					}
			}
			catch(final IOException e){
				throw new LinterException(e.getMessage());
			}
		};
		final Runnable completed = () -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Begin post-processing");

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

				final String[] buildOptions = {
					"--overwrite",
					"--accept-cr",
					"--exit", "false",
					"--format", "CFSA2",
					"--input", outputFile.toString()
				};
				DictCompile.main(buildOptions);

				LOGGER.info(ParserManager.MARKER_APPLICATION, "File written: {}.dict", filenameNoExtension);

				FileHelper.browse(outputFile);

				Files.delete(outputFile.toPath());
			}
			catch(final Exception e){
				LOGGER.warn("Exception while creating the FSA file for Part–of–Speech", e);
			}
		};

		setWriteDataProcessor(lineProcessor, outputFile);
		getWorkerData()
			.withDataCompletedCallback(completed);
	}

}
