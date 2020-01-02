package unit731.hunspeller.parsers.workers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import morfologik.tools.DictCompile;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.generators.WordGenerator;
import unit731.hunspeller.parsers.vos.DictionaryEntry;
import unit731.hunspeller.parsers.vos.Production;
import unit731.hunspeller.parsers.workers.core.WorkerData;
import unit731.hunspeller.parsers.workers.core.WorkerDictionaryBase;
import unit731.hunspeller.parsers.workers.exceptions.HunspellException;
import unit731.hunspeller.services.FileHelper;


public class WordlistWorker extends WorkerDictionaryBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordlistWorker.class);

	public static final String WORKER_NAME = "Wordlist";

	public enum WorkerType{COMPLETE, PLAN_WORDS, MORFOLOGIK}


	public WordlistWorker(final DictionaryParser dicParser, final WordGenerator wordGenerator, final WorkerType type,
			final File outputFile){
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(outputFile);


		final Function<Production, List<String>> toString;
		switch(type){
			case COMPLETE:
				toString = p -> Collections.singletonList(p.toString());
				break;

			case MORFOLOGIK:
				toString = Production::toStringMorfologik;
				break;

			case PLAN_WORDS:
			default:
				toString = p -> Collections.singletonList(p.getWord());
		}
		final BiConsumer<BufferedWriter, Pair<Integer, String>> lineProcessor = (writer, line) -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line.getValue());
			final List<Production> productions = wordGenerator.applyAffixRules(dicEntry);

			try{
				for(final Production production : productions){
					for(final String text : toString.apply(production))
						writer.write(text);
					writer.newLine();
				}
			}
			catch(final IOException e){
				throw new HunspellException(e.getMessage());
			}
		};
		final Runnable completed = () -> {
			LOGGER.info(Backbone.MARKER_APPLICATION, "File written: {}", outputFile.getAbsolutePath());

			if(type == WorkerType.MORFOLOGIK){
				try{
					final File outputInfoFile = new File(FilenameUtils.removeExtension(outputFile.getAbsolutePath()) + ".info");
					final Charset charset = dicParser.getCharset();
					final List<String> content = Arrays.asList(
						"fsa.dict.separator=" + Production.MORFOLOGIK_SEPARATOR,
						"fsa.dict.encoding=" + charset.name().toLowerCase(),
						"fsa.dict.encoder=prefix");
					FileHelper.saveFile(outputInfoFile.toPath(), StringUtils.CR, charset, content);

					final String[] buildOptions = {
						"--overwrite",
						"--accept-cr",
						"--exit", "false",
						"--format", "FSA5",
//						"--format", "CFSA2",
						"--input", outputFile.toString()
					};
					DictCompile.main(buildOptions);

					FileHelper.openFolder(outputFile);

					Files.delete(outputFile.toPath());
				}
				catch(final IOException e){
					LOGGER.warn("Exception while creating the FSA file for Morfologik", e);
				}
			}
			else{
				try{
					FileHelper.openFileWithChosenEditor(outputFile);
				}
				catch(final IOException | InterruptedException e){
					LOGGER.warn("Exception while opening the resulting file", e);
				}
			}
		};
		final WorkerData data = WorkerData.create(WORKER_NAME, dicParser);
		data.setCompletedCallback(completed);
		createWriteWorker(data, lineProcessor, outputFile);
	}

	@Override
	public String getWorkerName(){
		return WORKER_NAME;
	}

}
