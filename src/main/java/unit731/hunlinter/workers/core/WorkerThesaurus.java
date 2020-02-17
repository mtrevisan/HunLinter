package unit731.hunlinter.workers.core;

import org.apache.commons.lang3.tuple.Pair;
import unit731.hunlinter.parsers.thesaurus.ThesaurusEntry;
import unit731.hunlinter.parsers.thesaurus.ThesaurusParser;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class WorkerThesaurus extends WorkerAbstract<ThesaurusEntry, WorkerDataParser<ThesaurusParser>>{

	protected WorkerThesaurus(final WorkerDataParser<ThesaurusParser> workerData){
		super(workerData);
	}

	@Override
	protected Void doInBackground(){
		prepareProcessing("Start processing " + workerData.getWorkerName());

		final List<Pair<Integer, ThesaurusEntry>> entries = readEntries();

		processData(entries);

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
