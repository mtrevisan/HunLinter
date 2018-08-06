package unit731.hunspeller.services;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExceptionService{

	public static String getMessage(Exception e){
		String message = e.getMessage();
		if(message == null){
			StackTraceElement[] stackTrace = e.getStackTrace();
			StackTraceElement stackTrace0 = e.getStackTrace()[0];
			String classPackage = ExceptionService.class.getName();
			classPackage = classPackage.substring(0, classPackage.indexOf('.') + 1);
			for(StackTraceElement trace : stackTrace)
				if(trace.getClassName().startsWith(classPackage)){
					stackTrace0 = trace;
					break;
				}
			message = stackTrace0.getFileName();
			message = String.join(StringUtils.EMPTY, message.substring(0, message.indexOf('.')), ".", stackTrace0.getMethodName(), ":", Integer.toString(stackTrace0.getLineNumber()));
		}
		return message;
	}

}
