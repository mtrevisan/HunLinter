package unit731.hunlinter.parsers.workers.core;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.Backbone;
import unit731.hunlinter.parsers.thesaurus.ThesaurusEntry;
import unit731.hunlinter.parsers.thesaurus.ThesaurusParser;
import unit731.hunlinter.services.log.ExceptionHelper;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class WorkerThesaurus extends WorkerAbstract<ThesaurusEntry, Integer, WorkerDataThesaurus>{

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerThesaurus.class);


	private final AtomicInteger processingIndex = new AtomicInteger(0);


	protected WorkerThesaurus(final WorkerDataThesaurus workerData){
		super(workerData);
	}

	@Override
	protected Void doInBackground(){
		prepareProcessing("Start thesaurus processing");

		final List<Pair<Integer, ThesaurusEntry>> entries = readEntries();
		final int totalLines = entries.size();
		final Consumer<Pair<Integer, ThesaurusEntry>> processor = rowLine -> {
			try{
				processingIndex.incrementAndGet();

				readDataProcessor.accept(rowLine.getValue(), rowLine.getKey());

				setProcessingProgress(processingIndex.get(), totalLines);
			}
			catch(final Exception e){
				final String errorMessage = ExceptionHelper.getMessage(e);
				LOGGER.trace("{}, line {}: {}", errorMessage, rowLine.getKey(), rowLine.getValue());
				LOGGER.info(Backbone.MARKER_APPLICATION, "{}, line {}: {}", e.getMessage(), rowLine.getKey(), rowLine.getValue());

				if(workerData.isRelaunchException())
					throw e;
			}
		};
		processData(entries, processor);

		return null;
	}

	private List<Pair<Integer, ThesaurusEntry>> readEntries(){
		final ThesaurusParser theParser = workerData.getTheParser();
		final List<ThesaurusEntry> dictionary = theParser.getSynonymsDictionary();
		return IntStream.range(0, dictionary.size())
			.mapToObj(index -> Pair.of(index, dictionary.get(index)))
			.collect(Collectors.toList());
	}

}
