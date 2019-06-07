package unit731.hunspeller.services;

import org.apache.commons.lang3.StringUtils;



public class ExceptionHelper{

	private ExceptionHelper(){}

	public static String getMessage(Throwable t){
		StringBuilder message = new StringBuilder(composeExceptionMessage(t));
		Throwable cause = t.getCause();
		while(cause != null){
			message.append(System.lineSeparator())
				.append(composeExceptionMessage(cause));

			cause = cause.getCause();
		}
		return message.toString();
	}

	private static String composeExceptionMessage(Throwable t){
		String exceptionType = extractExceptionName(t);
		String codePosition = extractExceptionPosition(t);
		String msg = t.getMessage();
		StringBuilder sb = new StringBuilder();
		sb.append(exceptionType)
			.append(" at ")
			.append(codePosition);
		if(msg != null)
			sb.append(StringUtils.SPACE)
				.append(msg);
		return sb.toString();
	}

	private static String extractExceptionPosition(Throwable t){
		StackTraceElement stackTrace = extractOwnCodeStackTrace(t);
		String filename = stackTrace.getFileName();
		filename = filename.substring(0, filename.lastIndexOf('.'));
		return filename + "." + stackTrace.getMethodName() + ":" + stackTrace.getLineNumber();
	}

	private static StackTraceElement extractOwnCodeStackTrace(Throwable t){
		StackTraceElement[] stackTrace = t.getStackTrace();
		StackTraceElement stackTrace0 = t.getStackTrace()[0];
		String classPackage = ExceptionHelper.class.getName();
		classPackage = classPackage.substring(0, classPackage.indexOf('.') + 1);
		for(StackTraceElement trace : stackTrace)
			if(trace.getClassName().startsWith(classPackage)){
				stackTrace0 = trace;
				break;
			}
		return stackTrace0;
	}

	private static String extractExceptionName(Throwable t){
		return t.getClass().getSimpleName();
	}

}
