package unit731.hunlinter.workers.core;

import unit731.hunlinter.parsers.thesaurus.ThesaurusEntry;
import unit731.hunlinter.parsers.thesaurus.ThesaurusParser;
import unit731.hunlinter.services.system.JavaHelper;
import unit731.hunlinter.services.text.StringHelper;
import unit731.hunlinter.workers.exceptions.LinterException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;


public class WorkerThesaurus extends WorkerAbstract<ThesaurusEntry, WorkerDataParser<ThesaurusParser>>{

	protected WorkerThesaurus(final WorkerDataParser<ThesaurusParser> workerData){
		super(workerData);
	}

	protected void processLines(final Consumer<ThesaurusEntry> dataProcessor){
		Objects.requireNonNull(dataProcessor);

		//load thesaurus
		final List<ThesaurusEntry> entries = loadThesaurus();

		//process thesaurus
		final Stream<ThesaurusEntry> stream = (workerData.isParallelProcessing()?
			entries.parallelStream(): entries.stream());
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
		final List<ThesaurusEntry> entries = new ArrayList<>();
		final ThesaurusParser theParser = workerData.getParser();
		final List<ThesaurusEntry> dictionary = theParser.getSynonymsDictionary();
		entries.addAll(dictionary);

		return entries;
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
final AtomicLong memoryUsage = new AtomicLong(0l);
		final AtomicInteger processingIndex = new AtomicInteger(0);
		return data -> {
			try{
				dataProcessor.accept(data);

final long currentMemoryUsage = JavaHelper.getUsedMemory();
if(currentMemoryUsage > memoryUsage.get()){
	memoryUsage.set(currentMemoryUsage);
	System.out.println("cip: " + StringHelper.byteCountToHumanReadable(currentMemoryUsage));

	System.gc();
}
//PoS FSA:
//?: 3.6 GiB
//dic linter:
//fsa6: 278/274 MiB
//fsa8: 322/272/286 MiB

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
