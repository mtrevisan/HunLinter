package unit731.hunspeller.services;

import org.apache.commons.lang3.StringUtils;


public class ExceptionHelper{

	private static final String DOT = ".";


	private ExceptionHelper(){}

	public static String getMessage(Throwable t){
		String message = t.getMessage();
		if(message == null){
			StackTraceElement[] stackTrace = t.getStackTrace();
			StackTraceElement stackTrace0 = t.getStackTrace()[0];
			String classPackage = ExceptionHelper.class.getName();
			classPackage = classPackage.substring(0, classPackage.indexOf('.') + 1);
			for(StackTraceElement trace : stackTrace)
				if(trace.getClassName().startsWith(classPackage)){
					stackTrace0 = trace;
					break;
				}
			message = stackTrace0.getFileName();
			message = String.join(StringUtils.EMPTY, message.substring(0, message.indexOf('.')), DOT, stackTrace0.getMethodName(), ":", Integer.toString(stackTrace0.getLineNumber()));
		}
		return message;
	}

}
