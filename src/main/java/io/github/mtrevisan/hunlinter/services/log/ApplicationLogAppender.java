/**
 * Copyright (c) 2019-2021 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.services.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import org.slf4j.Marker;
import io.github.mtrevisan.hunlinter.MainFrame;
import io.github.mtrevisan.hunlinter.actions.ReportWarningsAction;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.prefs.Preferences;


public class ApplicationLogAppender extends AppenderBase<ILoggingEvent>{

	private final Preferences preferences = Preferences.userNodeForPackage(MainFrame.class);

	private Encoder<ILoggingEvent> encoder;

	private static final Map<Marker, List<JTextArea>> TEXT_AREAS = new HashMap<>(0);
	private static final Map<Marker, List<JLabel>> LABELS = new HashMap<>(0);


	public static void addLabel(final JLabel label, final Marker... markers){
		addComponent(LABELS, label, markers);
	}

	public static void addTextArea(final JTextArea textArea, final Marker... markers){
		addComponent(TEXT_AREAS, textArea, markers);
	}

	private static <T> void addComponent(final Map<Marker, List<T>> map, final T component, final Marker... markers){
		Objects.requireNonNull(component, "Component cannot be null");

		if(markers != null)
			for(final Marker marker : markers)
				map.computeIfAbsent(marker, k -> new ArrayList<>(1))
					.add(component);
	}

	public final void setEncoder(final Encoder<ILoggingEvent> encoder){
		this.encoder = encoder;
	}

	@Override
	protected final void append(final ILoggingEvent eventObject){
		if((!TEXT_AREAS.isEmpty() || !LABELS.isEmpty()) && encoder != null){
			final byte[] encoded = encoder.encode(eventObject);
			final String message = new String(encoded, StandardCharsets.UTF_8);

			if(!preferences.getBoolean(ReportWarningsAction.REPORT_WARNINGS, true) && message.contains(" WARN: "))
				return;

			final Marker marker = eventObject.getMarker();
			JavaHelper.executeOnEventDispatchThread(() -> {
				final List<JTextArea> textAreas = TEXT_AREAS.get(marker);
				if(textAreas != null)
					for(final JTextArea textArea : textAreas){
						textArea.append(message);
						textArea.setCaretPosition(textArea.getDocument().getLength());
					}
				final List<JLabel> labels = LABELS.get(marker);
				if(labels != null)
					for(final JLabel label : labels)
						label.setText(message);
			});
		}
	}

}
