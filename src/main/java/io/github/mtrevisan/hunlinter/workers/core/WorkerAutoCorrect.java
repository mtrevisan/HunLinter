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

import io.github.mtrevisan.hunlinter.parsers.autocorrect.AutoCorrectParser;
import io.github.mtrevisan.hunlinter.parsers.autocorrect.CorrectionEntry;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;


public class WorkerAutoCorrect extends WorkerAbstract<WorkerDataParser<AutoCorrectParser>>{

	protected WorkerAutoCorrect(final WorkerDataParser<AutoCorrectParser> workerData){
		super(workerData);
	}

	protected void processLines(final Consumer<CorrectionEntry> dataProcessor){
		Objects.requireNonNull(dataProcessor);

		//load autocorrect
		final List<CorrectionEntry> entries = loadAutoCorrect();

		//process autocorrect
		final Stream<CorrectionEntry> stream = (workerData.isParallelProcessing()
			? entries.parallelStream()
			: entries.stream());
		processAutoCorrect(stream, entries.size(), dataProcessor);

		CorrectionEntry data = null;
		try{
			final AutoCorrectParser acoParser = workerData.getParser();
			final List<CorrectionEntry> dictionary = acoParser.getCorrectionsDictionary();
			for(final CorrectionEntry acoEntry : dictionary){
				data = acoEntry;
				dataProcessor.accept(data);
			}
		}
		catch(final Exception e){
			manageException(new LinterException(e, data));
		}
	}

	private List<CorrectionEntry> loadAutoCorrect(){
		final AutoCorrectParser acoParser = workerData.getParser();
		return acoParser.getCorrectionsDictionary();
	}

	private void processAutoCorrect(final Stream<CorrectionEntry> entries, final int totalEntries,
			final Consumer<CorrectionEntry> dataProcessor){
		try{
			final Consumer<CorrectionEntry> innerProcessor = createInnerProcessor(dataProcessor, totalEntries);
			entries.forEach(innerProcessor);
		}
		catch(final LinterException e){
			manageException(e);

			throw e;
		}
	}

	private Consumer<CorrectionEntry> createInnerProcessor(final Consumer<CorrectionEntry> dataProcessor, final int totalEntries){
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
