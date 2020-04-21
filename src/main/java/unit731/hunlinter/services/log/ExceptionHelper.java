package unit731.hunlinter.services.log;

import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.services.system.LoopHelper;

import java.util.Optional;


public class ExceptionHelper{

	private ExceptionHelper(){}

	public static String getMessage(final Throwable t){
		return getMessage(t, true);
	}

	public static String getMessageNoLineNumber(final Throwable t){
		return getMessage(t, false);
	}

	private static String getMessage(final Throwable t, final boolean includeLineNumber){
		final StringBuffer sb = new StringBuffer(composeExceptionMessage(t, includeLineNumber));
		Throwable cause = t.getCause();
		while(cause != null){
			sb.append(System.lineSeparator())
				.append(composeExceptionMessage(cause, includeLineNumber));

			cause = cause.getCause();
		}
		return sb.toString();
	}

	private static String composeExceptionMessage(final Throwable t, final boolean includeLineNumber){
		final String exceptionType = extractExceptionName(t);
		final String msg = t.getMessage();
		final StringBuffer sb = new StringBuffer();
		sb.append(exceptionType);
		if(includeLineNumber){
			final String codePosition = extractExceptionPosition(t);
			sb.append(" at ").append(codePosition);
		}
		else
			sb.append(':');
		if(msg != null)
			sb.append(StringUtils.SPACE)
				.append(msg);
		return sb.toString();
	}

	private static String extractExceptionPosition(final Throwable t){
		final StackTraceElement stackTrace = extractOwnCodeStackTrace(t);
		String filename = stackTrace.getFileName();
		filename = filename.substring(0, filename.lastIndexOf('.'));
		return filename + "." + stackTrace.getMethodName() + ":" + stackTrace.getLineNumber();
	}

	private static StackTraceElement extractOwnCodeStackTrace(final Throwable t){
		final StackTraceElement[] stackTrace = t.getStackTrace();
		StackTraceElement stackTrace0 = null;
		if(stackTrace.length > 0){
			final String className = ExceptionHelper.class.getName();
			final String classPackage = className.substring(0, className.indexOf('.') + 1);
			stackTrace0 = Optional.ofNullable(LoopHelper.match(stackTrace, trace -> trace.getClassName().startsWith(classPackage)))
				.orElse(stackTrace[0]);
		}
		return stackTrace0;
	}

	private static String extractExceptionName(final Throwable t){
		return t.getClass().getSimpleName();
	}

}
