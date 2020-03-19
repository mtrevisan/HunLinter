package unit731.hunlinter.workers.core;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
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

	protected void processLines(final Path path, final Charset charset, final Consumer<IndexDataPair<String>> dataProcessor){
		Objects.requireNonNull(dataProcessor);

		setProgress(0);

		//load dictionary
		final List<IndexDataPair<String>> entries = loadFile(path, charset);

		//process dictionary
		final Stream<IndexDataPair<String>> stream = (workerData.isParallelProcessing()?
			entries.parallelStream(): entries.stream());
		processDictionary(stream, entries.size(), dataProcessor);
	}

//	protected void writeLines(final BiConsumer<Writer, String> dataProcessor, final int totalEntries, final File outputFile,
//			final Charset charset){
//		Objects.requireNonNull(dataProcessor);
//		Objects.requireNonNull(outputFile);
//		Objects.requireNonNull(charset);
//
//		setProgress(0);
//
//		try{
//			final BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), charset);
//			try{
//				final BiConsumer<Writer, String> innerProcessor = createInnerProcessor(dataProcessor, totalEntries);
//				innerProcessor.accept(writer, line);
//			}
//			finally{
//				writer.close();
//			}
//		}
//		catch(final Exception e){
//			cancel(e);
//		}
//	}

	protected List<IndexDataPair<String>> loadFile(final Path path, final Charset charset){
		final List<IndexDataPair<String>> entries = new ArrayList<>();
		try(final LineNumberReader br = FileHelper.createReader(path, charset)){
			String line = ParserHelper.extractLine(br);
			if(!NumberUtils.isCreatable(line))
				throw new LinterException(WRONG_FILE_FORMAT.format(new Object[]{line}));

			while((line = br.readLine()) != null){
				if(ParserHelper.isComment(line, ParserHelper.COMMENT_MARK_SHARP, ParserHelper.COMMENT_MARK_SLASH))
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

	protected void writeLine(final BufferedWriter writer, final String line){
		try{
			writer.write(line);
			writer.newLine();
		}
		catch(final IOException e){
			throw new RuntimeException(e);
		}
	}

	protected void closeWriter(final Writer writer){
		try{
			writer.close();
		}
		catch(final IOException ignored){}
	}

}
