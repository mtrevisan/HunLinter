/**
 * Copyright (c) 2019-2022 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.workers.core;

import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.parsers.exceptions.ParserException;
import io.github.mtrevisan.hunlinter.parsers.exceptions.WriterException;
import io.github.mtrevisan.hunlinter.services.ParserHelper;
import io.github.mtrevisan.hunlinter.services.system.FileHelper;
import io.github.mtrevisan.hunlinter.services.text.StringHelper;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


public class WorkerDictionary extends WorkerAbstract<WorkerDataParser<DictionaryParser>>{

	protected WorkerDictionary(final WorkerDataParser<DictionaryParser> workerData){
		super(workerData);
	}

	protected final void processLines(final Path path, final Charset charset, final Consumer<IndexDataPair<String>> dataProcessor){
		Objects.requireNonNull(dataProcessor, "Data processor cannot be null");

		try{
			if(workerData.isParallelProcessing()){
				//load dictionary
				final List<IndexDataPair<String>> entries = loadFile(path, charset);

				//process dictionary
				processLinesParallel(entries, dataProcessor);
			}
			else
				processLinesSequential(path, charset, dataProcessor);
		}
		catch(final IOException ioe){
			throw new ParserException(ioe);
		}
	}


	/**
	 * Stream the file one line at a time (no intermediate list of raw lines is kept), so that only the entries to be
	 * processed are held in memory.
	 */
	@SuppressWarnings("OverlyBroadThrowsClause")
	private List<IndexDataPair<String>> loadFile(final Path path, final Charset charset) throws IOException{
		final ArrayList<IndexDataPair<String>> entries = new ArrayList<>();
		try(final BufferedReader reader = FileHelper.createReader(path, charset)){
			int lineIndex = 0;
			if(!workerData.isNoHeader()){
				assertHeader(reader.readLine());
				lineIndex = 1;
			}

			String line;
			while((line = reader.readLine()) != null){
				if(!ParserHelper.isDictionaryComment(line))
					entries.add(IndexDataPair.of(lineIndex, line));

				lineIndex ++;
			}
		}
		entries.trimToSize();
		return entries;
	}

	private void processLinesParallel(final Collection<IndexDataPair<String>> entries,
			final Consumer<IndexDataPair<String>> dataProcessor){
		final Consumer<IndexDataPair<String>> innerProcessor = createInnerProcessorByLines(dataProcessor, entries.size());
		entries.parallelStream()
			.forEach(innerProcessor);
	}

	/**
	 * Always read one line at a time: memory usage is constant regardless of the file size, and there is no need to
	 * estimate the available memory.
	 */
	private void processLinesSequential(final Path path, final Charset charset,
			final Consumer<IndexDataPair<String>> dataProcessor) throws IOException{
		final long fileSize = Math.max(1l, FileHelper.getFileSize(path.toFile()));

		try(final BufferedReader reader = FileHelper.createReader(path, charset)){
			long readSoFar = 0l;
			if(!workerData.isNoHeader()){
				final String header = reader.readLine();
				assertHeader(header);
				readSoFar = StringHelper.rawBytesLength(header);
			}

			final ProgressTracker tracker = new ProgressTracker(fileSize);
			final Consumer<IndexDataPair<String>> innerProcessor = createInnerProcessor(dataProcessor);

			int lineIndex = (workerData.isNoHeader()? 0: 1);
			String line;
			while((line = reader.readLine()) != null){
				readSoFar += StringHelper.rawBytesLength(line);
				if(!ParserHelper.isDictionaryComment(line)){
					innerProcessor.accept(IndexDataPair.of(lineIndex, line));

					tracker.update(readSoFar);
				}

				lineIndex ++;
			}
		}
	}

	private static void assertHeader(final String header) throws EOFException{
		if(header == null)
			throw new EOFException("Unexpected EOF while reading file");

		ParserHelper.assertLinesCount(List.of(header));
	}

	private final class ProgressTracker{
		private final long total;
		private int lastPercent;

		private ProgressTracker(final long total){
			this.total = total;
		}

		/** Single-threaded: report only when the percentage actually changes. */
		private void update(final long current){
			final int percent = (int)Math.min(current * 100l / total, 100l);
			if(percent > lastPercent){
				lastPercent = percent;
				setWorkerProgress(percent);
			}
		}
	}

	private Consumer<IndexDataPair<String>> createInnerProcessorByLines(final Consumer<IndexDataPair<String>> dataProcessor,
			final int totalEntries){
		final AtomicInteger processed = new AtomicInteger();
		final AtomicInteger reported = new AtomicInteger();
		final int progressStep = Math.max(1, (int)Math.ceil(totalEntries / 100.f));
		final Consumer<IndexDataPair<String>> innerProcessor = createInnerProcessor(dataProcessor);
		return data -> {
			innerProcessor.accept(data);

			//touch the shared counter only (no lock, no event) and report only increasing percentages
			final int done = processed.incrementAndGet();
			if(done % progressStep == 0){
				final int percent = Math.min(done / progressStep, 100);
				if(reported.getAndUpdate(previous -> Math.max(previous, percent)) < percent)
					setWorkerProgress(percent);
			}
		};
	}

	private Consumer<IndexDataPair<String>> createInnerProcessor(final Consumer<IndexDataPair<String>> dataProcessor){
		return data -> {
			try{
				dataProcessor.accept(data);

				sleepOnPause();
			}
			catch(final RuntimeException re){
				final LinterException le = new LinterException(re, re.getMessage())
					.withData(data);
				manageException(le);

				if(workerData.isCancelOnException())
					throw le;
			}
		};
	}


	protected static void writeLine(final BufferedWriter writer, final String line, final char[] lineSeparator){
		try{
			writer.write(line);
			writer.write(lineSeparator);
		}
		catch(final IOException ioe){
			throw new WriterException(ioe);
		}
	}

	protected static void closeWriter(final Writer writer){
		try{
			writer.close();
		}
		catch(final IOException ioe){
			throw new WriterException(ioe);
		}
	}

}
