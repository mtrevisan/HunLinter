package unit731.hunspeller.services;


public class ExceptionHelper{

	private ExceptionHelper(){}

	public static String getMessage(Throwable t){
		String message = composeExceptionMessage(t);
		Throwable cause = t.getCause();
		while(cause != null){
			message += System.lineSeparator() + composeExceptionMessage(cause);

			cause = cause.getCause();
		}
		return message;
	}

	private static String composeExceptionMessage(Throwable t){
		String codePosition = extractExceptionPosition(t);
		return extractExceptionName(t) + " at " + codePosition + " " + t.getMessage();
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
