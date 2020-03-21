package unit731.hunlinter.workers.core;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.FileHelper;
import unit731.hunlinter.services.ParserHelper;


public class WorkerDictionary extends WorkerAbstract<WorkerDataParser<DictionaryParser>>{

	protected WorkerDictionary(final WorkerDataParser<DictionaryParser> workerData){
		super(workerData);
	}

	protected void processLines(final Path path, final Charset charset, final Consumer<IndexDataPair<String>> dataProcessor){
		Objects.requireNonNull(dataProcessor);

		setProgress(0);

		if(workerData.isParallelProcessing()){
			//load dictionary
			final List<IndexDataPair<String>> entries = loadFile(path, charset);

			//process dictionary
			processLinesParallel(entries, entries.size(), dataProcessor);
		}
		else
			processLinesSequential(path, charset, dataProcessor);
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

	private List<IndexDataPair<String>> loadFile(final Path path, final Charset charset){
		final List<IndexDataPair<String>> entries = new ArrayList<>();
		try(final Scanner scanner = FileHelper.createScanner(path, charset)){
			ParserHelper.assertLinesCount(scanner);

			int lineIndex = 1;
			while(scanner.hasNextLine()){
				final String line = scanner.nextLine();
				if(ParserHelper.isComment(line, ParserHelper.COMMENT_MARK_SHARP, ParserHelper.COMMENT_MARK_SLASH))
					continue;

				entries.add(IndexDataPair.of(lineIndex ++, line));

				sleepOnPause();
			}
		}
		catch(final Exception e){
			manageException(new LinterException(e, null));

			throw new RuntimeException(e);
		}

		return entries;
	}

	private void processLinesParallel(final List<IndexDataPair<String>> entries, final int totalEntries,
			final Consumer<IndexDataPair<String>> dataProcessor){
		try{
			final Consumer<IndexDataPair<String>> innerProcessor = createInnerProcessor(dataProcessor, totalEntries);
			entries.parallelStream()
				.forEach(innerProcessor);
		}
		catch(final LinterException e){
			manageException(e);

			throw e;
		}
	}

	private void processLinesSequential(final Path path, final Charset charset,
			final Consumer<IndexDataPair<String>> dataProcessor){
		final int totalEntries = FileHelper.countLines(path);
		final Consumer<IndexDataPair<String>> innerProcessor = createInnerProcessor(dataProcessor, totalEntries);

		LineIterator itr = null;
		try{
			itr = FileUtils.lineIterator(path.toFile(), charset.name());
			ParserHelper.assertLinesCount(itr);

			int lineIndex = 1;
			while(itr.hasNext()){
				final String line = itr.nextLine();
				if(ParserHelper.isComment(line, ParserHelper.COMMENT_MARK_SHARP, ParserHelper.COMMENT_MARK_SLASH))
					continue;

				innerProcessor.accept(IndexDataPair.of(lineIndex ++, line));
			}
		}
		catch(final Exception e){
			manageException(new LinterException(e, null));

			throw new RuntimeException(e);
		}
		finally{
			try{
				if(itr != null)
					itr.close();
			}
			catch(final Exception ignored){}
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
