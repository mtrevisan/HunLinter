package unit731.hunlinter.services.system;

import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.workers.core.RuntimeInterruptedException;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.channels.ClosedChannelException;


public class JavaHelper{

	private static final char QUOTATION_MARK = '"';


	private JavaHelper(){}

	/**
	 * This method calls the garbage collector and then returns the free memory.
	 * This avoids problems with applications where the GC hasn't reclaimed memory and reports no available memory.
	 *
	 * @return estimated available memory
	 */
	public static long estimateAvailableMemory(){
		System.gc();

		//http://stackoverflow.com/questions/12807797/java-get-available-memory
		final Runtime r = Runtime.getRuntime();
		final long allocatedMemory = r.totalMemory() - r.freeMemory();
		return r.maxMemory() - allocatedMemory;
	}

	public static boolean isInterruptedException(final Exception exception){
		final Throwable t = (exception != null && exception.getCause() != null? exception.getCause(): exception);
		return (t instanceof InterruptedException || t instanceof RuntimeInterruptedException || exception instanceof ClosedChannelException);
	}

	public static void executeOnEventDispatchThread(final Runnable runnable){
		if(SwingUtilities.isEventDispatchThread())
			runnable.run();
		else
			SwingUtilities.invokeLater(runnable);
	}

	/* Stop current running Java application and start a new one */
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
		//finally add program arguments
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
				catch(final IOException e){
					e.printStackTrace();
				}
			}));

		//exit
		System.exit(0);
	}

}
