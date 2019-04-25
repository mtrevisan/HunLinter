package unit731.hunspeller.services;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTextArea;
import org.slf4j.Marker;


public class ApplicationLogAppender extends AppenderBase<ILoggingEvent>{

	private Encoder<ILoggingEvent> encoder;

	private static final Map<Marker, List<JTextArea>> TEXT_AREAS = new HashMap<>();


	public static void addTextArea(JTextArea textArea, Marker... markers){
		for(Marker marker : markers)
			ApplicationLogAppender.TEXT_AREAS.computeIfAbsent(marker, k -> new ArrayList<>())
				.add(textArea);
	}

	public void setEncoder(Encoder<ILoggingEvent> encoder){
		this.encoder = encoder;
	}

	@Override
	protected void append(ILoggingEvent eventObject){
		if(!TEXT_AREAS.isEmpty() && encoder != null){
			byte[] encoded = encoder.encode(eventObject);
			String message = new String(encoded, StandardCharsets.UTF_8);
			Marker marker = eventObject.getMarker();
			for(JTextArea textArea : TEXT_AREAS.get(marker))
				textArea.append(message);
		}
	}

}
