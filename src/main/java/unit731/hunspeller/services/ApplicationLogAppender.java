package unit731.hunspeller.services;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.AppenderBase;
import javax.swing.JTextArea;
import org.apache.commons.lang3.StringUtils;


public class ApplicationLogAppender<T> extends AppenderBase<T>{

	private static JTextArea textArea;


	public static void setTextArea(JTextArea textArea){
		ApplicationLogAppender.textArea = textArea;
	}

	@Override
	protected void append(T eventObject){
		if(textArea != null)
			textArea.append(((LoggingEvent)eventObject).getFormattedMessage() + StringUtils.LF);
	}

}
