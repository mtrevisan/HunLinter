package unit731.hunlinter.parsers.workers.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import unit731.hunlinter.Backbone;
import unit731.hunlinter.parsers.workers.exceptions.ProjectNotFoundException;
import unit731.hunlinter.services.Packager;
import unit731.hunlinter.services.log.ExceptionHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class WorkerProject extends WorkerAbstract<String, Integer>{

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerProject.class);

	@FunctionalInterface
	interface StageFunction{
		void execute() throws IOException, SAXException;
	}


	protected WorkerProject(final WorkerDataProject workerData){
		Objects.requireNonNull(workerData);

		this.workerData = workerData;
	}

	@Override
	protected Void doInBackground(){
		prepareProcessing("Opening project");

		dataProcess();

		return null;
	}

	private void dataProcess(){
		final Packager packager = ((WorkerDataProject)workerData).packager;
		try{
			final Backbone backbone = ((WorkerDataProject)workerData).backbone;
			final List<StageFunction> stages = Arrays.asList(
				() -> backbone.openAffixFile(packager.getAffixFile()),
				() -> backbone.openHyphenationFile(backbone.getHyphenationFile()),
				backbone::getCorrectnessChecker,
				() -> backbone.prepareDictionaryFile(backbone.getDictionaryFile()),
				() -> backbone.openAidFile(backbone.getAidFile()),
				() -> backbone.openThesaurusFile(backbone.getThesaurusDataFile()),
				() -> backbone.openAutoCorrectFile(backbone.getAutoCorrectFile()),
				() -> backbone.openSentenceExceptionsFile(backbone.getSentenceExceptionsFile()),
				() -> backbone.openWordExceptionsFile(backbone.getWordExceptionsFile()));
			for(int index = 0; index < stages.size(); index ++){
				stages.get(index).execute();
				//noinspection IntegerDivisionInFloatingPointContext
				setProgress((int)Math.ceil((index + 1) * 100 / stages.size()));

				waitIfPaused();
			}

			finalizeProcessing("Project loaded successfully");
		}
		catch(final Exception e){
			cancel(e instanceof FileNotFoundException? new ProjectNotFoundException(packager.getProjectPath(), e): e);

			if(!(e instanceof ClosedChannelException)){
				final String errorMessage = ExceptionHelper.getMessage(e);
				LOGGER.error(Backbone.MARKER_APPLICATION, "{}", errorMessage);
			}
		}
	}

}
