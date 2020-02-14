package unit731.hunlinter.parsers.workers.core;

import org.apache.commons.lang3.tuple.Pair;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.thesaurus.ThesaurusEntry;
import unit731.hunlinter.parsers.thesaurus.ThesaurusParser;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class WorkerThesaurus extends WorkerAbstract<ThesaurusEntry, Integer, WorkerDataParser<ThesaurusParser>>{

	private final AtomicInteger processingIndex = new AtomicInteger(0);


	protected WorkerThesaurus(final WorkerDataParser<ThesaurusParser> workerData){
		super(workerData);
	}

	@Override
	protected Void doInBackground(){
		prepareProcessing("Start thesaurus processing");

		final List<Pair<Integer, ThesaurusEntry>> entries = readEntries();
		final int totalLines = entries.size();
		final Consumer<Pair<Integer, ThesaurusEntry>> processor = rowLine -> {
			processingIndex.incrementAndGet();

			readDataProcessor.accept(rowLine.getValue(), rowLine.getKey());

			setProcessingProgress(processingIndex.get(), totalLines);
		};
		processData(entries, processor);

		return null;
	}

	private List<Pair<Integer, ThesaurusEntry>> readEntries(){
		final ThesaurusParser theParser = workerData.getParser();
		final List<ThesaurusEntry> dictionary = theParser.getSynonymsDictionary();
		return IntStream.range(0, dictionary.size())
			.mapToObj(index -> Pair.of(index, dictionary.get(index)))
			.collect(Collectors.toList());
	}

}
