package unit731.hunspeller.parsers.dictionary.workers;

import unit731.hunspeller.parsers.dictionary.workers.exceptions.ProjectFileNotFoundException;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.channels.ClosedChannelException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerBase;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerData;
import unit731.hunspeller.services.ExceptionHelper;


public class ProjectLoaderWorker extends WorkerBase<Void, Void>{

	private static final Logger LOGGER = LoggerFactory.getLogger(ProjectLoaderWorker.class);


	public static final String WORKER_NAME = "Project loader";


	private final String affixFilePath;
	private final Backbone backbone;

	private final AtomicBoolean paused = new AtomicBoolean(false);


	public ProjectLoaderWorker(final String affixFilePath, final Backbone backbone, final Runnable completed, final Consumer<Exception> cancelled){
		Objects.requireNonNull(affixFilePath);
		Objects.requireNonNull(backbone);

		this.affixFilePath = affixFilePath;
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

			backbone.clear();

			while(paused.get())
				Thread.sleep(500l);

			backbone.openAffixFile(affixFilePath);

			setProgress(14);

			while(paused.get())
				Thread.sleep(500l);

			final File hypFile = backbone.getHyphenationFile();
			backbone.openHyphenationFile(hypFile);

			setProgress(29);

			while(paused.get())
				Thread.sleep(500l);

			backbone.getCorrectnessChecker();

			setProgress(43);

			while(paused.get())
				Thread.sleep(500l);

			final File dicFile = backbone.getDictionaryFile();
			backbone.prepareDictionaryFile(dicFile);

			setProgress(57);

			while(paused.get())
				Thread.sleep(500l);

			final File aidFile = backbone.getAidFile();
			backbone.openAidFile(aidFile);

			setProgress(71);

			while(paused.get())
				Thread.sleep(500l);

			final File theDataFile = backbone.getThesaurusDataFile();
			backbone.openThesaurusFile(theDataFile);

			setProgress(86);

			while(paused.get())
				Thread.sleep(500l);

			final File acoDataFile = backbone.getAutoCorrectFile();
			backbone.openAutoCorrectFile(acoDataFile);

			setProgress(100);

			watch.stop();

			LOGGER.info(Backbone.MARKER_APPLICATION, "Project loaded successfully (in {})", watch.toStringMinuteSeconds());
		}
		catch(final Exception t){
			exception = (t instanceof FileNotFoundException? new ProjectFileNotFoundException(affixFilePath, t): t);

			if(t instanceof ClosedChannelException)
				LOGGER.warn(Backbone.MARKER_APPLICATION, "Project loader thread interrupted");
			else
				LOGGER.error(Backbone.MARKER_APPLICATION, "{}", t.getMessage());
			final String errorMessage = ExceptionHelper.getMessage(t);
			LOGGER.trace("{}: {}", t.getClass().getSimpleName(), errorMessage);

			LOGGER.info(Backbone.MARKER_APPLICATION, "Stopped opening project");

			cancel();
		}

		return null;
	}

	@Override
	protected void done(){
		if(!isCancelled() && getCompleted() != null)
			getCompleted().run();
		else if(isCancelled() && getCancelled() != null)
			getCancelled().accept(exception);
	}

	public final void pause(){
		if(!isDone() && paused.compareAndSet(false, true))
			firePropertyChange("paused", false, true);
	}

	public final void resume(){
		if(!isDone() && paused.compareAndSet(true, false))
			firePropertyChange("paused", true, false);
	}

	public void cancel(){
		cancel(true);
	}

}
