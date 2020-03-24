package unit731.hunlinter.workers.core;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.ParserHelper;


public class WorkerDictionary extends WorkerAbstract<WorkerDataParser<DictionaryParser>>{

	protected WorkerDictionary(final WorkerDataParser<DictionaryParser> workerData){
		super(workerData);
	}

	protected void processLines(final Path path, final Charset charset, final Consumer<IndexDataPair<String>> dataProcessor){
		Objects.requireNonNull(dataProcessor);

		setProgress(0);

		try{
			if(workerData.isParallelProcessing()){
				//load dictionary
				final List<IndexDataPair<String>> entries = loadFile(path, charset);

				//process dictionary
				processLinesParallel(entries, entries.size(), dataProcessor);
			}
			else
				processLinesSequential(path, charset, dataProcessor);
		}
		catch(final LinterException e){
			manageException(e);

			throw e;
		}
		catch(final Exception e){
			manageException(new LinterException(e, null));

			throw new RuntimeException(e);
		}
	}

	private List<IndexDataPair<String>> loadFile(final Path path, final Charset charset) throws IOException{
		//read entire file in memory
		final List<String> lines = Files.readAllLines(path, charset);
		if(!workerData.isNoHeader())
			ParserHelper.assertLinesCount(lines);

		final List<IndexDataPair<String>> entries = new ArrayList<>();
		for(int lineIndex = (workerData.isNoHeader()? 0: 1); lineIndex < lines.size(); lineIndex ++){
			final String line = lines.get(lineIndex);
			if(ParserHelper.isComment(line, ParserHelper.COMMENT_MARK_SHARP, ParserHelper.COMMENT_MARK_SLASH))
				continue;

			entries.add(IndexDataPair.of(lineIndex, line));
		}
		return entries;
	}

	private void processLinesParallel(final List<IndexDataPair<String>> entries, final int totalEntries,
		final Consumer<IndexDataPair<String>> dataProcessor){
		final Consumer<IndexDataPair<String>> innerProcessor = createInnerProcessor(dataProcessor, totalEntries);
		entries.parallelStream()
			.forEach(innerProcessor);
	}

	private void processLinesSequential(final Path path, final Charset charset,
			final Consumer<IndexDataPair<String>> dataProcessor) throws IOException{
		//read entire file in memory
		final List<String> lines = Files.readAllLines(path, charset);
		if(!workerData.isNoHeader())
			ParserHelper.assertLinesCount(lines);

		final Consumer<IndexDataPair<String>> innerProcessor = createInnerProcessor(dataProcessor, lines.size());
		for(int lineIndex = (workerData.isNoHeader()? 0: 1); lineIndex < lines.size(); lineIndex ++){
			final String line = lines.get(lineIndex);
			if(ParserHelper.isComment(line, ParserHelper.COMMENT_MARK_SHARP, ParserHelper.COMMENT_MARK_SLASH))
				continue;

			innerProcessor.accept(IndexDataPair.of(lineIndex, line));
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
