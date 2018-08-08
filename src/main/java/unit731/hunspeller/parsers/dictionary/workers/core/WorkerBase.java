package unit731.hunspeller.parsers.dictionary.workers.core;

import java.nio.charset.Charset;
import java.util.function.BiConsumer;
import javax.swing.SwingWorker;
import lombok.Getter;
import unit731.hunspeller.services.TimeWatch;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


public abstract class WorkerBase<S, T> extends SwingWorker<Void, Void>{

	@Getter
	protected String workerName;

	protected Charset charset;

	protected BiConsumer<S, T> lineaReader;
	protected Runnable done;
//	protected ReadWriteLockable lockable;

	protected TimeWatch watch = TimeWatch.start();

}
