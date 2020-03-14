package unit731.hunlinter.workers.core;

import unit731.hunlinter.parsers.thesaurus.ThesaurusEntry;
import unit731.hunlinter.parsers.thesaurus.ThesaurusParser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class WorkerThesaurus extends WorkerAbstract<ThesaurusEntry, WorkerDataParser<ThesaurusParser>>{

	protected WorkerThesaurus(final WorkerDataParser<ThesaurusParser> workerData){
		super(workerData);
	}

	protected List<IndexDataPair<ThesaurusEntry>> readEntries(){
		final ThesaurusParser theParser = workerData.getParser();
		final List<ThesaurusEntry> dictionary = theParser.getSynonymsDictionary();
		final List<IndexDataPair<ThesaurusEntry>> list = new ArrayList<>(dictionary.size());
		for(int index = 0; index < dictionary.size(); index ++)
			list.add(IndexDataPair.of(index, dictionary.get(index)));
		return list;
	}

}
