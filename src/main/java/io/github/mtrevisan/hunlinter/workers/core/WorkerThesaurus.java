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

import io.github.mtrevisan.hunlinter.parsers.thesaurus.ThesaurusEntry;
import io.github.mtrevisan.hunlinter.parsers.thesaurus.ThesaurusParser;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;


public class WorkerThesaurus extends WorkerAbstract<WorkerDataParser<ThesaurusParser>>{

	protected WorkerThesaurus(final WorkerDataParser<ThesaurusParser> workerData){
		super(workerData);
	}

	protected final void processLines(final Consumer<ThesaurusEntry> dataProcessor){
		Objects.requireNonNull(dataProcessor, "Data processor cannot be null");

		//load thesaurus
		final List<ThesaurusEntry> entries = loadThesaurus();

		//process thesaurus
		final Stream<ThesaurusEntry> stream = (workerData.isParallelProcessing()
			? entries.parallelStream()
			: entries.stream());
		processThesaurus(stream, entries.size(), dataProcessor);

		ThesaurusEntry data = null;
		try{
			final ThesaurusParser theParser = workerData.getParser();
			final List<ThesaurusEntry> dictionary = theParser.getSynonymsDictionary();
			for(final ThesaurusEntry thesaurusEntry : dictionary){
				data = thesaurusEntry;
				dataProcessor.accept(data);
			}
		}
		catch(final Exception e){
			manageException(new LinterException(e, data));
		}
	}

	private List<ThesaurusEntry> loadThesaurus(){
		final ThesaurusParser theParser = workerData.getParser();
		return theParser.getSynonymsDictionary();
	}

	private void processThesaurus(final Stream<ThesaurusEntry> entries, final int totalEntries,
			final Consumer<ThesaurusEntry> dataProcessor){
		try{
			final Consumer<ThesaurusEntry> innerProcessor = createInnerProcessor(dataProcessor, totalEntries);
			entries.forEach(innerProcessor);
		}
		catch(final LinterException e){
			manageException(e);

			throw e;
		}
	}

	private Consumer<ThesaurusEntry> createInnerProcessor(final Consumer<ThesaurusEntry> dataProcessor, final int totalEntries){
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
