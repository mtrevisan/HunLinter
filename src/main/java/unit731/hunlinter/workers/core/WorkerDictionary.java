package unit731.hunlinter.workers.core;

import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.FileHelper;
import unit731.hunlinter.services.ParserHelper;


public class WorkerDictionary extends WorkerAbstract<String, WorkerDataParser<DictionaryParser>>{

	private static final MessageFormat WRONG_FILE_FORMAT = new MessageFormat("Dictionary file malformed, the first line is not a number, was ''{0}''");


	protected WorkerDictionary(final WorkerDataParser<DictionaryParser> workerData){
		super(workerData);
	}

	protected void processLines(final Consumer<IndexDataPair<String>> dataProcessor){
		Objects.requireNonNull(dataProcessor);

		setProgress(0);

		//load dictionary
		final List<IndexDataPair<String>> entries = loadDictionary();

		//process dictionary
		final Stream<IndexDataPair<String>> stream = (workerData.isParallelProcessing()?
			entries.parallelStream(): entries.stream());
		processDictionary(stream, entries.size(), dataProcessor);
	}

	private List<IndexDataPair<String>> loadDictionary(){
		final List<IndexDataPair<String>> entries = new ArrayList<>();
		final DictionaryParser dicParser = workerData.getParser();
		final Path dicPath = dicParser.getDicFile().toPath();
		final Charset charset = dicParser.getCharset();
		try(final LineNumberReader br = FileHelper.createReader(dicPath, charset)){
			String line = ParserHelper.extractLine(br);
			if(!NumberUtils.isCreatable(line))
				throw new LinterException(WRONG_FILE_FORMAT.format(new Object[]{line}));

			while((line = br.readLine()) != null){
				line = ParserHelper.cleanLine(line);
				if(line.isEmpty())
					continue;

				entries.add(IndexDataPair.of(br.getLineNumber(), line));

				sleepOnPause();
			}
		}
		catch(final Exception e){
			manageException(new LinterException(e, null));

			throw new RuntimeException(e);
		}

		return entries;
	}

	private void processDictionary(final Stream<IndexDataPair<String>> entries, final int totalEntries,
			final Consumer<IndexDataPair<String>> dataProcessor){
		try{
			final Consumer<IndexDataPair<String>> innerProcessor = createInnerProcessor(dataProcessor, totalEntries);
			entries.forEach(innerProcessor);
		}
		catch(final LinterException e){
			manageException(e);

			throw e;
		}
	}

	private Consumer<IndexDataPair<String>> createInnerProcessor(final Consumer<IndexDataPair<String>> dataProcessor,
			final int totalEntries){
		final AtomicInteger processingIndex = new AtomicInteger(0);
		return data -> {
			try{
				dataProcessor.accept(data);

				setProgress(processingIndex.incrementAndGet(), totalEntries);

				sleepOnPause();
			}
			catch(final LinterException e){
				throw e;
			}
			catch(final Exception e){
				throw new LinterException(e, data);
			}
		};
	}

}
