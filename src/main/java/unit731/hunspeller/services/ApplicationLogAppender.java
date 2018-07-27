package unit731.hunspeller.services;

import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import java.nio.charset.StandardCharsets;
import javax.swing.JTextArea;
import lombok.Setter;


public class ApplicationLogAppender<T> extends AppenderBase<T>{

	@Setter
	private Encoder<T> encoder;

	private static JTextArea textArea;


	public static void setTextArea(JTextArea textArea){
		ApplicationLogAppender.textArea = textArea;
	}

	@Override
	protected void append(T eventObject){
		if(textArea != null){
			byte[] encoded = encoder.encode(eventObject);
			String message = new String(encoded, StandardCharsets.UTF_8);
			textArea.append(message);
		}
	}

}
