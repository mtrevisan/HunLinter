package unit731.hunspeller.services;


public class ExceptionService{

	public static String getMessage(Exception e, Class<?> cl){
		String message = e.getMessage();
		if(message == null){
			StackTraceElement[] stackTrace = e.getStackTrace();
			StackTraceElement stackTrace0 = e.getStackTrace()[0];
			String classPackage = cl.getName();
			classPackage = classPackage.substring(0, classPackage.indexOf('.') + 1);
			for(StackTraceElement trace : stackTrace)
				if(trace.getClassName().startsWith(classPackage)){
					stackTrace0 = trace;
					break;
				}
			message = stackTrace0.getFileName();
			message = message.substring(0, message.indexOf('.')) + "." + stackTrace0.getMethodName() + ":" + stackTrace0.getLineNumber();
		}
		return message;
	}

}
