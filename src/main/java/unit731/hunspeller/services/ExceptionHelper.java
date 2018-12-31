package unit731.hunspeller.services;

import org.apache.commons.lang3.StringUtils;


public class ExceptionHelper{

	private static final String DOT = ".";


	private ExceptionHelper(){}

	public static String getMessage(Throwable t){
		String message = t.getMessage();
		if(message == null){
			StackTraceElement stackTrace = extractOwnCodeStackTrace(t);
			String className = extractClassName(stackTrace);
			message = String.join(StringUtils.EMPTY, className, DOT, stackTrace.getMethodName(), ":",
				Integer.toString(stackTrace.getLineNumber()));
		}
		return message;
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

	private static String extractClassName(StackTraceElement stackTrace){
		String classPackage = stackTrace.getFileName();
		return classPackage.substring(0, classPackage.indexOf('.'));
	}

}
