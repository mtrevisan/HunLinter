package unit731.hunlinter.services.log;

import org.apache.commons.lang3.StringUtils;


public class ExceptionHelper{

	private ExceptionHelper(){}

	public static String getMessage(final Throwable t){
		final StringBuffer sb = new StringBuffer(composeExceptionMessage(t));
		Throwable cause = t.getCause();
		while(cause != null){
			sb.append(System.lineSeparator())
				.append(composeExceptionMessage(cause));

			cause = cause.getCause();
		}
		return sb.toString();
	}

	private static String composeExceptionMessage(final Throwable t){
		final String exceptionType = extractExceptionName(t);
		final String codePosition = extractExceptionPosition(t);
		final String msg = t.getMessage();
		final StringBuffer sb = new StringBuffer();
		sb.append(exceptionType)
			.append(" at ")
			.append(codePosition);
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
			stackTrace0 = stackTrace[0];
			String classPackage = ExceptionHelper.class.getName();
			classPackage = classPackage.substring(0, classPackage.indexOf('.') + 1);
			for(final StackTraceElement trace : stackTrace)
				if(trace.getClassName().startsWith(classPackage)){
					stackTrace0 = trace;
					break;
				}
		}
		return stackTrace0;
	}

	private static String extractExceptionName(final Throwable t){
		return t.getClass().getSimpleName();
	}

}
