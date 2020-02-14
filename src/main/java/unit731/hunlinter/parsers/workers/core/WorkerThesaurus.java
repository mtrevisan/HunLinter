package unit731.hunlinter.parsers.workers.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.Backbone;
import unit731.hunlinter.parsers.thesaurus.SynonymsEntry;
import unit731.hunlinter.parsers.thesaurus.ThesaurusDictionary;
import unit731.hunlinter.parsers.thesaurus.ThesaurusEntry;
import unit731.hunlinter.parsers.thesaurus.ThesaurusParser;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class WorkerThesaurus extends WorkerAbstract<String, Integer>{

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerThesaurus.class);

	private static final MessageFormat MISSING_ENTRY = new MessageFormat("Thesaurus doesn't contain definition {0} with part-of-speech {1} (from entry {2})");


	protected WorkerThesaurus(final WorkerDataThesaurus workerData){
		Objects.requireNonNull(workerData);

		this.workerData = workerData;
	}

	@Override
	protected Void doInBackground(){
		prepareProcessing("Start thesaurus processing");

		dataProcess();

		return null;
	}

	private void dataProcess(){
		try{
			final ThesaurusParser theParser = ((WorkerDataThesaurus)workerData).getTheParser();
			final List<ThesaurusEntry> dictionary = theParser.getSynonymsDictionary();
			int i = 0;
			final int size = dictionary.size();
			for(final ThesaurusEntry entry : dictionary){
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

						waitIfPaused();
					}
				}

				setProgress(++ i * 100 / size);
			}

			finalizeProcessing("Successfully processed thesaurus file");
		}
		catch(final Exception e){
			cancel(e);
		}
	}

}
