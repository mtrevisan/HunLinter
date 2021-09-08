/**
 * Copyright (c) 2019-2021 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.workers.core;

import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.services.log.ExceptionHelper;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import io.github.mtrevisan.hunlinter.services.system.TimeWatch;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;


public abstract class WorkerAbstract<WD extends WorkerData> extends SwingWorker<Void, Void>{

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerAbstract.class);


	protected final WD workerData;

	private final AtomicBoolean paused = new AtomicBoolean(false);
	private static final Lock PAUSE_LOCK = new ReentrantLock();
	private static final Condition UNPAUSE = PAUSE_LOCK.newCondition();

	private final TimeWatch watch = TimeWatch.start();

	private Function<?, ?> processor;


	WorkerAbstract(final WD workerData){
		Objects.requireNonNull(workerData, "Worker data cannot be null");

		this.workerData = workerData;
	}

	public final void setProcessor(final Function<?, ?> processor){
		Objects.requireNonNull(processor, "Processor cannot be null");

		this.processor = processor;
	}

	public final WD getWorkerData(){
		return workerData;
	}

	@Override
	@SuppressWarnings("DesignForExtension")
	protected Void doInBackground(){
		try{
			processor.apply(null);
		}
		catch(final Exception e){
			if(workerData.isCancelOnException())
				cancel(e);
		}
		return null;
	}

	@SuppressWarnings("SameReturnValue")
	protected final Void prepareProcessing(final String message){
		setProgress(0);
		LOGGER.info(ParserManager.MARKER_APPLICATION, message);

		watch.reset();
		return null;
	}

	@SuppressWarnings("SameReturnValue")
	protected final Void resetProcessing(final String message, final Object... params){
		setProgress(0);
		LOGGER.info(ParserManager.MARKER_APPLICATION, message, params);

		return null;
	}

	protected final void finalizeProcessing(final String message){
		watch.stop();

		setProgress(100);
		LOGGER.info(ParserManager.MARKER_APPLICATION, "{} (in {})", message, watch.toStringMinuteSeconds());
	}

	public final void executeSynchronously(){
		doInBackground();
	}


	protected final void manageException(final LinterException e){
		if(JavaHelper.isInterruptedException(e))
			cancel(e);
		else if(e.getData() != null){
			final String errorMessage = ExceptionHelper.getMessage(e);
			final IndexDataPair<?> data = e.getData();
			final int index = data.getIndex();
			final String lineText = (index >= 0? ", line " + index: StringUtils.EMPTY);
			LOGGER.trace("{}{}: {}", errorMessage, lineText, data.getData());
			LOGGER.info(ParserManager.MARKER_APPLICATION, (data.getData() != null? "{}{}: {}": "{}{}"), e.getMessage(),
				lineText, data.getData());
		}
		else
			e.printStackTrace();
	}


	protected final void setProgress(final long index, final long total){
		final int progress = calculateProgress(index, total);
		setProgress(Math.min(progress, 100));
	}

	private int calculateProgress(final long index, final long total){
		return Math.min((int)Math.floor((index * 100.) / total), 100);
	}

	@Override
	protected final void done(){
		if(!isCancelled())
			workerData.callCompletedCallback();
	}

	public final void pause(){
		if(!isDone() && paused.compareAndSet(false, true))
			firePropertyChange("paused", Boolean.FALSE, Boolean.TRUE);
	}

	public final boolean isPaused(){
		return paused.get();
	}

	public final void resume(){
		if(!isDone() && paused.compareAndSet(true, false)){
			PAUSE_LOCK.lock();
			try{
				UNPAUSE.signalAll();
			}
			finally{
				PAUSE_LOCK.unlock();
			}

			firePropertyChange("paused", Boolean.TRUE, Boolean.FALSE);
		}
	}

	/**
	 * NOTE: this should be called inside `SwingWorker.doInBackground()` to allow process abortion
	 */
	@SuppressWarnings("AwaitNotInLoop")
	protected final void sleepOnPause(){
		if(paused.get()){
			PAUSE_LOCK.lock();
			try{
				try{
					UNPAUSE.await();
				}
				catch(final InterruptedException ignored){}
			}
			finally{
				PAUSE_LOCK.unlock();
			}
		}
	}

	/**
	 * Worker cancels itself due to an internal exception
	 *
	 * @param exception	Exception that causes the cancellation
	 */
	protected final void cancel(final Exception exception){
		if(!JavaHelper.isInterruptedException(exception)){
			if(exception != null){
				final String errorMessage = ExceptionHelper.getMessage(exception);
				LOGGER.error(errorMessage, exception);

				JOptionPane.showOptionDialog(null,
					"Something very bad happened", "Error", JOptionPane.DEFAULT_OPTION,
					JOptionPane.ERROR_MESSAGE, null, null, null);
			}
			else
				LOGGER.error("Generic error");

			LOGGER.info(ParserManager.MARKER_APPLICATION, "Process {} stopped with error", workerData.getWorkerName());

			workerData.callCancelledCallback(exception);

			cancel(true);
		}
	}

	/** User cancelled worker. */
	public final void cancel(){
		LOGGER.info(ParserManager.MARKER_APPLICATION, "Process {} aborted", workerData.getWorkerName());

		workerData.callCancelledCallback(null);

		cancel(true);
	}

	@Override
	public final boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final WorkerAbstract<?> rhs = (WorkerAbstract<?>)obj;
		return workerData.equals(rhs.workerData);
	}

	@Override
	public final int hashCode(){
		return workerData.hashCode();
	}

}
