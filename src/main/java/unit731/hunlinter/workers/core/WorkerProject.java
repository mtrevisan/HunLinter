package unit731.hunlinter.workers.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.services.system.JavaHelper;
import unit731.hunlinter.workers.exceptions.ProjectNotFoundException;
import unit731.hunlinter.services.Packager;
import unit731.hunlinter.services.log.ExceptionHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class WorkerProject extends WorkerAbstract<WorkerDataProject>{

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerProject.class);

	@FunctionalInterface
	interface StageFunction{
		void execute() throws IOException, SAXException;
	}


	protected WorkerProject(final WorkerDataProject workerData){
		super(workerData);
	}

	@Override
	protected Void doInBackground(){
		prepareProcessing("Opening project");

		final Packager packager = workerData.getPackager();
		try{
			final ParserManager parserManager = workerData.getParserManager();
			final List<StageFunction> stages = Arrays.asList(
				() -> parserManager.openAffixFile(packager.getAffixFile()),
				() -> parserManager.openHyphenationFile(packager.getHyphenationFile()),
				parserManager::getCorrectnessChecker,
				() -> parserManager.prepareDictionaryFile(packager.getDictionaryFile()),
				() -> parserManager.openAidFile(parserManager.getAidFile()),
				() -> parserManager.openThesaurusFile(packager.getThesaurusDataFile()),
				() -> parserManager.openAutoCorrectFile(packager.getAutoCorrectFile()),
				() -> parserManager.openSentenceExceptionsFile(packager.getSentenceExceptionsFile()),
				() -> parserManager.openWordExceptionsFile(packager.getWordExceptionsFile()));
			for(int index = 0; index < stages.size(); index ++){
				stages.get(index).execute();
				//noinspection IntegerDivisionInFloatingPointContext
				setProgress((int)Math.ceil((index + 1) * 100 / stages.size()));

				sleepOnPause();
			}

			finalizeProcessing("Project loaded successfully");
		}
		catch(final Exception e){
			cancel(e instanceof FileNotFoundException? new ProjectNotFoundException(packager.getProjectPath(), e): e);

			if(!JavaHelper.isInterruptedException(e)){
				final String errorMessage = ExceptionHelper.getMessage(e);
				LOGGER.error(ParserManager.MARKER_APPLICATION, "{}", errorMessage);
			}
		}

		return null;
	}

}
