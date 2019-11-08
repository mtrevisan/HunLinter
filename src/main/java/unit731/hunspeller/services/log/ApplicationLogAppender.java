package unit731.hunspeller.services.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTextArea;
import org.slf4j.Marker;


public class ApplicationLogAppender extends AppenderBase<ILoggingEvent>{

	private Encoder<ILoggingEvent> encoder;

	private static final Map<Marker, List<JTextArea>> TEXT_AREAS = new HashMap<>();


	public static void addTextArea(final JTextArea textArea, final Marker... markers){
		Arrays.stream(markers)
			.forEach(marker -> ApplicationLogAppender.TEXT_AREAS.computeIfAbsent(marker, k -> new ArrayList<>()).add(textArea));
	}

	public void setEncoder(final Encoder<ILoggingEvent> encoder){
		this.encoder = encoder;
	}

	@Override
	protected void append(final ILoggingEvent eventObject){
		if(!TEXT_AREAS.isEmpty() && encoder != null){
			final byte[] encoded = encoder.encode(eventObject);
			final String message = new String(encoded, StandardCharsets.UTF_8);
			final Marker marker = eventObject.getMarker();
			TEXT_AREAS.get(marker)
				.forEach(textArea -> textArea.append(message));
		}
	}

}
