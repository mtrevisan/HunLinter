/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.services.system;

import io.github.mtrevisan.hunlinter.workers.core.RuntimeInterruptedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.channels.ClosedChannelException;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;


public final class JavaHelper{

	private static final Logger LOGGER = LoggerFactory.getLogger(JavaHelper.class);


	private static final char QUOTATION_MARK = '"';

	private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(10);


	private JavaHelper(){}


	public static <T> void addIfNotNull(final Collection<T> set, final T data){
		if(data != null)
			set.add(data);
	}


	public static String textFormat(final String message, final Object... parameters){
		return MessageFormatter.arrayFormat(message, parameters)
			.getMessage();
	}


	public static <T> Future<T> executeFuture(final Callable<T> callable){
		final FutureTask<T> future = new FutureTask<>(callable);
		EXECUTOR_SERVICE.execute(future);
		return future;
	}

	public static <T> T waitForFuture(final Future<T> future){
		while(true){
			try{
				if(future.isDone())
					return future.get();
			}
			catch(final InterruptedException | ExecutionException ignored){}
		}
	}

	/**
	 * This method calls the garbage collector and then returns the free memory.
	 * This avoids problems with applications where the GC hasn't reclaimed memory and reports no available memory.
	 *
	 * @return estimated available memory
	 */
	@SuppressWarnings("CallToSystemGC")
	public static long estimateAvailableMemory(){
		System.gc();

		//http://stackoverflow.com/questions/12807797/java-get-available-memory
		final Runtime r = Runtime.getRuntime();
		final long allocatedMemory = r.totalMemory() - r.freeMemory();
		return r.maxMemory() - allocatedMemory;
	}

	public static boolean isInterruptedException(final Exception exception){
		final Throwable t = (exception != null && exception.getCause() != null? exception.getCause(): exception);
		return (t instanceof InterruptedException || t instanceof RuntimeInterruptedException
			|| exception instanceof ClosedChannelException);
	}

	public static void executeOnEventDispatchThread(final Runnable runnable){
		if(SwingUtilities.isEventDispatchThread())
			runnable.run();
		else
			SwingUtilities.invokeLater(runnable);
	}

	public static void exit(final int status){
		new Thread(() -> System.exit(status)).start();
	}

	@SuppressWarnings("BusyWait")
	public static void delayedRun(final Runnable runnable, final long delayMillis){
		final long requestedStartTime = System.currentTimeMillis() + delayMillis;
		new Thread(() -> {
			while(!Thread.interrupted()){
				try{
					final long leftToSleep = requestedStartTime - System.currentTimeMillis();
					if(leftToSleep > 0l)
						Thread.sleep(leftToSleep);

					break;
				}
				catch(final InterruptedException ignored){}
			}

			runnable.run();
		}).start();
	}

	/** Stop current running Java application and start a new one. */
	@SuppressWarnings("CallToRuntimeExecWithNonConstantString")
	public static void closeAndStartAnotherApplication(final String jarURL){
		//init the command to execute, add the vm args
		final String fileSeparator = System.getProperty("file.separator");
		final StringBuffer cmd = new StringBuffer()
			.append(QUOTATION_MARK)
			//add java binary
			.append(System.getProperty("java.home")).append(fileSeparator).append("bin").append(fileSeparator).append("java.exe")
			.append(QUOTATION_MARK);
		for(final String arg : ManagementFactory.getRuntimeMXBean().getInputArguments())
			//if it's the agent argument we ignore it, otherwise the address of the old application and the new one will be in conflict
			if(!arg.startsWith("-agentlib") && !arg.startsWith("-javaagent"))
				cmd.append(StringUtils.SPACE)
					.append(arg);
		//program main and program arguments (be careful, this ia a Sun property, it might not be supported by all JVM!)
		final String[] mainCommand = System.getProperty("sun.java.command")
			.split(StringUtils.SPACE);
		cmd.append(" -jar ")
			.append(jarURL);
		//finally, add program arguments
		for(int i = 1; i < mainCommand.length; i ++)
			cmd.append(StringUtils.SPACE)
				.append(mainCommand[i]);

		//execute the command in a shutdown hook, to be sure that all the resources have been disposed before restarting the application
		Runtime.getRuntime()
			.addShutdownHook(new Thread(() -> {
				try{
					Runtime.getRuntime()
						.exec(cmd.toString());
				}
				catch(final IOException ioe){
					ioe.printStackTrace();
				}
			}));

		//exit
		exit(0);
	}

	public static void shutdownExecutor(){
		//disable new tasks from being submitted
		EXECUTOR_SERVICE.shutdown();

		LOGGER.info("Shutting down java executor");

		try{
			//wait a while for existing tasks to terminate
			if(!EXECUTOR_SERVICE.awaitTermination(20, TimeUnit.SECONDS)){
				//cancel currently executing tasks
				EXECUTOR_SERVICE.shutdownNow();
				//wait a while for tasks to respond to being cancelled
				if(!EXECUTOR_SERVICE.awaitTermination(20, TimeUnit.SECONDS))
					LOGGER.error("Java executor did not terminate");
			}
		}
		catch(InterruptedException ie){
			//(re-)cancel if current thread also interrupted
			EXECUTOR_SERVICE.shutdownNow();
			//preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}

}
