package unit731.hunlinter.services.system;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.workers.core.RuntimeInterruptedException;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;


public class JavaHelper{

	public static long getUsedMemory(){
		final Runtime runtime =Runtime.getRuntime();
		return runtime.totalMemory() - runtime.freeMemory();
	}

	public static <T> T[] removeIf(final T[] array, final Predicate<T> filter){
		Objects.requireNonNull(array);
		Objects.requireNonNull(filter);

		int index = indexOf(array, filter, 0);
		if(index == -1)
			return ArrayUtils.clone(array);

		final int[] indices = new int[array.length - index];
		indices[0] = index;

		int count;
		for(count = 1; (index = indexOf(array, filter, indices[count - 1] + 1)) != -1; indices[count ++] = index){}

		return ArrayUtils.removeAll(array, Arrays.copyOf(indices, count));
	}

	private static <T> int indexOf(final T[] array, final Predicate<T> filter, int startIndex){
		for(int i = startIndex; i < array.length; i ++)
			if(filter.test(array[i]))
				return i;
		return -1;
	}

	public static boolean isInterruptedException(final Exception exception){
		final Throwable t = (exception != null && exception.getCause() != null? exception.getCause(): exception);
		return (t instanceof InterruptedException || t instanceof RuntimeInterruptedException || exception instanceof ClosedChannelException);
	}

	public static <T> Stream<T> nullableToStream(final T... array){
		return (array != null? Arrays.stream(array): Stream.empty());
	}

	public static <T> Stream<T> nullableToStream(final Collection<T> collection){
		return (collection != null? collection.stream(): Stream.empty());
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
			.append("\"")
			//add java binary
			.append(System.getProperty("java.home")).append(fileSeparator).append("bin").append(fileSeparator).append("java.exe")
			.append("\"");
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
