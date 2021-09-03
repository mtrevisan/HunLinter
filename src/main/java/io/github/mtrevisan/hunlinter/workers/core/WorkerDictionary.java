/**
 * Copyright (c) 2019-2021 Mauro Trevisan
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
import io.github.mtrevisan.hunlinter.services.ParserHelper;
import io.github.mtrevisan.hunlinter.services.system.FileHelper;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import io.github.mtrevisan.hunlinter.services.text.StringHelper;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class WorkerDictionary extends WorkerAbstract<WorkerDataParser<DictionaryParser>>{

	protected WorkerDictionary(final WorkerDataParser<DictionaryParser> workerData){
		super(workerData);
	}

	protected void processLines(final Path path, final Charset charset, final Consumer<IndexDataPair<String>> dataProcessor){
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
		catch(final LinterException e){
			throw e;
		}
		catch(final Exception e){
			throw new RuntimeException(e);
		}
	}


	private List<IndexDataPair<String>> loadFile(final Path path, final Charset charset) throws IOException{
		//read entire file in memory
		final List<String> lines = Files.readAllLines(path, charset);
		if(!workerData.isNoHeader())
			ParserHelper.assertLinesCount(lines);

		final List<IndexDataPair<String>> entries = new ArrayList<>(lines.size());
		for(int lineIndex = (workerData.isNoHeader()? 0: 1); lineIndex < lines.size(); lineIndex ++){
			final String line = lines.get(lineIndex);
			if(ParserHelper.isDictionaryComment(line))
				continue;

			entries.add(IndexDataPair.of(lineIndex, line));
		}
		return entries;
	}

	private void processLinesParallel(final Collection<IndexDataPair<String>> entries,
			final Consumer<IndexDataPair<String>> dataProcessor){
		final Consumer<IndexDataPair<String>> innerProcessor = createInnerProcessorByLines(dataProcessor, entries.size());
		entries.parallelStream()
			.forEach(innerProcessor);
	}

	private void processLinesSequential(final Path path, final Charset charset,
			final Consumer<IndexDataPair<String>> dataProcessor) throws IOException{
		final long fileSize = path.toFile().length();
		final long availableMemory = JavaHelper.estimateAvailableMemory();

		//choose between load all file in memory and read one line at a time:
		if((fileSize << 1) < availableMemory)
			processLinesSequentialByLine(path, charset, dataProcessor);
		else
			processLinesSequentialBySize(path, charset, dataProcessor, fileSize);
	}

	private void processLinesSequentialByLine(final Path path, final Charset charset,
			final Consumer<IndexDataPair<String>> dataProcessor) throws IOException{
		//read entire file in memory
		final List<String> lines = FileHelper.readAllLines(path, charset);
		if(!workerData.isNoHeader())
			ParserHelper.assertLinesCount(lines);

		final Consumer<IndexDataPair<String>> innerProcessor = createInnerProcessorByLines(dataProcessor, lines.size());

		for(int lineIndex = (workerData.isNoHeader()? 0: 1); lineIndex < lines.size(); lineIndex ++){
			final String line = lines.get(lineIndex);
			if(ParserHelper.isDictionaryComment(line))
				continue;

			innerProcessor.accept(IndexDataPair.of(lineIndex, line));
		}
	}

	private void processLinesSequentialBySize(final Path path, final Charset charset,
			final Consumer<IndexDataPair<String>> dataProcessor, final long fileSize) throws IOException{
		//read one line at a time
		try(final Scanner scanner = FileHelper.createScanner(path, charset)){
			long lineSize = 0l;
			if(!workerData.isNoHeader()){
				final String line = ParserHelper.assertLinesCount(scanner);
				lineSize = StringHelper.rawBytesLength(line);
			}

			final BiConsumer<IndexDataPair<String>, Long> innerProcessor = createInnerProcessorBySize(dataProcessor, fileSize);

			int lineIndex = (workerData.isNoHeader()? 0: 1);
			while(scanner.hasNextLine()){
				final String line = scanner.nextLine();
				lineSize += StringHelper.rawBytesLength(line);
				if(ParserHelper.isDictionaryComment(line))
					continue;

				innerProcessor.accept(IndexDataPair.of(lineIndex ++, line), lineSize);
			}
		}
	}

	private Consumer<IndexDataPair<String>> createInnerProcessorByLines(final Consumer<IndexDataPair<String>> dataProcessor,
			final long totalEntries){
		final AtomicInteger progress = new AtomicInteger(1);
		final AtomicInteger progressIndex = new AtomicInteger(1);
		final int progressStep = (int)Math.ceil(totalEntries / 100.f);
		return data -> {
			try{
				dataProcessor.accept(data);

				if(progress.incrementAndGet() % progressStep == 0)
					setProgress(progressIndex.incrementAndGet(), 100);

				sleepOnPause();
			}
			catch(final Exception e){
				final LinterException le = new LinterException(e.getMessage(), e.getCause(), data);
				manageException(le);

				if(workerData.isCancelOnException())
					throw le;
			}
		};
	}

	private BiConsumer<IndexDataPair<String>, Long> createInnerProcessorBySize(
			final Consumer<IndexDataPair<String>> dataProcessor, final long fileSize){
		final AtomicLong progress = new AtomicLong(1);
		final AtomicInteger progressIndex = new AtomicInteger(1);
		final int progressStep = (int)Math.ceil(fileSize / 100.f);
		return (data, readSoFar) -> {
			try{
				dataProcessor.accept(data);

				if((int)(progress.get() / progressStep) < (int)(progress.addAndGet(readSoFar) / progressStep))
					setProgress(progressIndex.incrementAndGet(), 100);

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


	protected void writeLine(final BufferedWriter writer, final String line, final char[] lineSeparator){
		try{
			writer.write(line);
			writer.write(lineSeparator);
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
