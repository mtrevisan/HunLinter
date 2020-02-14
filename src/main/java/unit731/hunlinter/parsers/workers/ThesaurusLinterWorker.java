package unit731.hunlinter.parsers.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.Backbone;
import unit731.hunlinter.parsers.thesaurus.SynonymsEntry;
import unit731.hunlinter.parsers.thesaurus.ThesaurusDictionary;
import unit731.hunlinter.parsers.thesaurus.ThesaurusEntry;
import unit731.hunlinter.parsers.thesaurus.ThesaurusParser;
import unit731.hunlinter.parsers.workers.core.WorkerDataParser;
import unit731.hunlinter.parsers.workers.core.WorkerThesaurus;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;


public class ThesaurusLinterWorker extends WorkerThesaurus{

	private static final Logger LOGGER = LoggerFactory.getLogger(ThesaurusLinterWorker.class);

	public static final String WORKER_NAME = "Thesaurus correctness checking";

	private static final MessageFormat MISSING_ENTRY = new MessageFormat("Thesaurus doesn't contain definition {0} with part-of-speech {1} (from entry {2})");


	public ThesaurusLinterWorker(final ThesaurusParser theParser){
		super(new WorkerDataParser<>(WORKER_NAME, theParser)
			.withParallelProcessing(true)
		);

		final BiConsumer<Integer, ThesaurusEntry> dataProcessor = (row, entry) -> {
			final String originalDefinition = entry.getDefinition();
			//check if each part of `entry`, with appropriate PoS, exists
			final List<SynonymsEntry> syns = entry.getSynonyms();
			for(final SynonymsEntry syn : syns){
				final List<String> definitions = syn.getSynonyms();
				final String[] partOfSpeeches = syn.getPartOfSpeeches();
				for(String definition : definitions){
					definition = ThesaurusDictionary.removeSynonymUse(definition);
					//check also that the found PoS has `originalDefinition` among its synonyms
					if(!theParser.contains(definition, partOfSpeeches, originalDefinition))
						LOGGER.info(Backbone.MARKER_APPLICATION, MISSING_ENTRY.format(new Object[]{definition, Arrays.toString(partOfSpeeches), originalDefinition}));
				}
			}
		};
		setReadDataProcessor(dataProcessor);
	}

}
