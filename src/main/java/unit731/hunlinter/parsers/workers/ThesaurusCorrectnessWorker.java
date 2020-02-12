package unit731.hunlinter.parsers.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.Backbone;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.thesaurus.SynonymsEntry;
import unit731.hunlinter.parsers.thesaurus.ThesaurusDictionary;
import unit731.hunlinter.parsers.thesaurus.ThesaurusEntry;
import unit731.hunlinter.parsers.thesaurus.ThesaurusParser;
import unit731.hunlinter.parsers.workers.core.WorkerBase;
import unit731.hunlinter.parsers.workers.core.WorkerData;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class ThesaurusCorrectnessWorker extends WorkerBase<Void, Void>{

	private static final Logger LOGGER = LoggerFactory.getLogger(ThesaurusCorrectnessWorker.class);

	public static final String WORKER_NAME = "Thesaurus correctness checking";

	private static final MessageFormat WORD_IS_NOT_SYLLABABLE = new MessageFormat("Word {0} ({1}) is not syllabable");


	private final ThesaurusParser theParser;


	public ThesaurusCorrectnessWorker(final ThesaurusParser theParser){
		Objects.requireNonNull(theParser);

		this.theParser = theParser;

		workerData = WorkerData.createParallelPreventExceptionRelaunch(WORKER_NAME);
	}

	@Override
	protected Void doInBackground(){
		try{
			exception = null;

			LOGGER.info(Backbone.MARKER_APPLICATION, WORKER_NAME);
			setProgress(0);

			final List<ThesaurusEntry> dictionary = theParser.getSynonymsDictionary();
			int i = 0;
			final int size = dictionary.size();
			for(final ThesaurusEntry entry : dictionary){
				//TODO check if each part of `entry`, with appropriate PoS, exists
				final List<SynonymsEntry> syns = entry.getSynonyms();
				for(final SynonymsEntry syn : syns){
					final List<String> definitions = syn.getSynonyms();
					final String[] partOfSpeeches = syn.getPartOfSpeeches();
					for(String definition : definitions){
						definition = ThesaurusDictionary.removeSynonymUse(definition);
						//TODO check also that the found pos has the original entry.getDefinition()
						if(!theParser.contains(definition, partOfSpeeches))
							LOGGER.info(Backbone.MARKER_APPLICATION, "Thesaurus does not contains definition {} with part-of-speech {}", definition, Arrays.toString(partOfSpeeches));
					}
				}

				setProgress(++ i * 100 / size);
			}


			watch.stop();

			setProgress(100);

			LOGGER.info(Backbone.MARKER_APPLICATION, "Successfully processed thesaurus (in {})", watch.toStringMinuteSeconds());
		}
		catch(final Exception e){
			exception = e;

			LOGGER.error(Backbone.MARKER_APPLICATION, e.getMessage());
			LOGGER.info(Backbone.MARKER_APPLICATION, "Stopped processing thesaurus");

			cancel(true);
		}

		return null;
	}

}
