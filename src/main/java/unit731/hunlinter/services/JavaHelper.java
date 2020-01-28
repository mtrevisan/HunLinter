package unit731.hunlinter.services;

import org.apache.commons.lang3.StringUtils;

import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;


public class JavaHelper{

	public enum OS{
		WINDOWS, MAC, LINUX, OTHER;

		public static final OS CURRENT;

		static{
			final String os = System.getProperty("os.name", "generic")
				.toLowerCase(Locale.ROOT);
			if(os.contains("mac") || os.contains("darwin"))
				CURRENT = MAC;
			else if(os.contains("win"))
				CURRENT = WINDOWS;
			else if(os.contains("nux"))
				CURRENT = LINUX;
			else
				CURRENT = OTHER;
		}
	}


	public static <T> Stream<T> nullableToStream(final T[] array){
		return Optional.ofNullable(array).stream()
			.flatMap(Arrays::stream);
	}

	public static <T> Stream<T> nullableToStream(final Collection<T> collection){
		return Optional.ofNullable(collection).stream()
			.flatMap(Collection::stream);
	}

	public static void executeOnEventDispatchThread(final Runnable runnable){
		if(SwingUtilities.isEventDispatchThread())
			runnable.run();
		else
			SwingUtilities.invokeLater(runnable);
	}

	/** Restart the current Java application */
	public static void restartApplication(){
		//java binary
		final String java = System.getProperty("java.home") + "/bin/java";
		//vm arguments
		final List<String> vmArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
		final StringBuffer vmArgsOneLine = new StringBuffer();
		for(final String arg : vmArguments)
			//if it's the agent argument we ignore it, otherwise the address of the old application and the new one will be in conflict
			if(!arg.contains("-agentlib")){
				vmArgsOneLine.append(arg);
				vmArgsOneLine.append(StringUtils.SPACE);
			}
		//init the command to execute, add the vm args
		final StringBuffer cmd = new StringBuffer("\"" + java + "\" " + vmArgsOneLine);
		//program main and program arguments (be careful a sun property. might not be supported by all JVM)
		final String[] mainCommand = System.getProperty("sun.java.command")
			.split(StringUtils.SPACE);
		//program main is a jar
		if(mainCommand[0].endsWith(".jar"))
			//if it's a jar, add -jar mainJar
			cmd.append("-jar " + new File(mainCommand[0]).getPath());
		else
			//else it's a .class, add the classpath and mainClass
			cmd.append("-cp \"" + System.getProperty("java.class.path") + "\" " + mainCommand[0]);
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
