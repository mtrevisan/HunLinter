package unit731.hunlinter.parsers.workers;

import org.xml.sax.SAXException;
import unit731.hunlinter.parsers.workers.exceptions.ProjectNotFoundException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.Backbone;
import unit731.hunlinter.parsers.workers.core.WorkerBase;
import unit731.hunlinter.parsers.workers.core.WorkerData;
import unit731.hunlinter.services.log.ExceptionHelper;
import unit731.hunlinter.services.Packager;


public class ProjectLoaderWorker extends WorkerBase<Void, Void>{

	private static final Logger LOGGER = LoggerFactory.getLogger(ProjectLoaderWorker.class);


	public static final String WORKER_NAME = "Project loader";

	@FunctionalInterface
	interface StageFunction{
		void execute() throws IOException, SAXException;
	}


	private final Packager packager;
	private final Backbone backbone;


	public ProjectLoaderWorker(final Packager packager, final Backbone backbone, final Runnable completed,
			final Consumer<Exception> cancelled){
		Objects.requireNonNull(packager);
		Objects.requireNonNull(backbone);

		this.packager = packager;
		this.backbone = backbone;

		workerData = WorkerData.createParallelPreventExceptionRelaunch(WORKER_NAME);
		workerData.setCompletedCallback(completed);
		workerData.setCancelledCallback(cancelled);
	}

	@Override
	protected Void doInBackground(){
		try{
			exception = null;

			LOGGER.info(Backbone.MARKER_APPLICATION, "Opening project");
			setProgress(0);

			watch.reset();

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
				waitIfPaused();

				stages.get(index).execute();
				//noinspection IntegerDivisionInFloatingPointContext
				setProgress((int)Math.ceil((index + 1) * 100 / stages.size()));
			}

			watch.stop();

			LOGGER.info(Backbone.MARKER_APPLICATION, "Project loaded successfully (in {})", watch.toStringMinuteSeconds());
		}
		catch(final Exception e){
			exception = (e instanceof FileNotFoundException? new ProjectNotFoundException(packager.getProjectPath(), e): e);

			final String errorMessage = ExceptionHelper.getMessage(e);
			LOGGER.trace("{}: {}", e.getClass().getSimpleName(), errorMessage);
			if(e instanceof ClosedChannelException)
				LOGGER.warn(Backbone.MARKER_APPLICATION, "Project loader thread interrupted");
			else
				LOGGER.error(Backbone.MARKER_APPLICATION, "{}", (e.getMessage() != null? e.getMessage(): errorMessage));

			LOGGER.info(Backbone.MARKER_APPLICATION, "Stopped opening project");

			cancel();
		}

		return null;
	}

}
