/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import io.github.mtrevisan.hunlinter.MainFrame;
import io.github.mtrevisan.hunlinter.actions.ReportWarningsAction;
import io.github.mtrevisan.hunlinter.gui.FontHelper;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import org.slf4j.Marker;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.awt.Font;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.prefs.Preferences;


public class ApplicationLogAppender extends AppenderBase<ILoggingEvent>{

	private static final Color COLOR_SAFETY_ORANGE = Color.getHSBColor(28.f / 256.f, 1.f, 1.f);

	private static final int INDENTATION = 30;

	private final Preferences preferences = Preferences.userNodeForPackage(MainFrame.class);

	private static final Map<Marker, List<JTextArea>> TEXT_AREAS = new HashMap<>(0);
	private static final Map<Marker, List<JTextPane>> TEXT_PANES = new HashMap<>(0);
	private static final Map<Marker, List<JLabel>> LABELS = new HashMap<>(0);


	private Encoder<ILoggingEvent> encoder;


	public static void addLabel(final JLabel label, final Marker... markers){
		addComponent(LABELS, label, markers);
	}

	public static void addTextArea(final JTextArea textArea, final Marker... markers){
		addComponent(TEXT_AREAS, textArea, markers);
	}

	public static void addTextPane(final JTextPane textPane, final Marker... markers){
		addComponent(TEXT_PANES, textPane, markers);
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
		if((!TEXT_AREAS.isEmpty() || !TEXT_PANES.isEmpty() || !LABELS.isEmpty()) && encoder != null){
			final Level level = eventObject.getLevel();
			if(level == Level.WARN && !preferences.getBoolean(ReportWarningsAction.REPORT_WARNINGS, true))
				return;

			final byte[] encoded = encoder.encode(eventObject);
			final String message = new String(encoded, StandardCharsets.UTF_8);

			final List<Marker> markerList = eventObject.getMarkerList();
			final Marker marker = (!markerList.isEmpty()? markerList.get(0): null);
			JavaHelper.executeOnEventDispatchThread(() -> {
				final List<JTextArea> textAreas = TEXT_AREAS.get(marker);
				if(textAreas != null)
					for(int i = 0; i < textAreas.size(); i ++){
						final JTextArea textArea = textAreas.get(i);
						final Document doc = textArea.getDocument();
						textArea.setCaretPosition(doc.getLength());
						textArea.append(message);
					}

				final List<JTextPane> textPanes = TEXT_PANES.get(marker);
				if(textPanes != null)
					for(int i = 0; i < textPanes.size(); i ++){
						final Color color;
						if(level == Level.ERROR)
							color = Color.RED;
						else if(level == Level.WARN)
							color = COLOR_SAFETY_ORANGE;
						else
							color = Color.BLACK;
						appendToPane(textPanes.get(i), message, color);
					}

				final List<JLabel> labels = LABELS.get(marker);
				if(labels != null)
					for(int i = 0; i < labels.size(); i ++)
						labels.get(i).setText(message);
			});
		}
	}

	private static void appendToPane(final JTextPane textPane, final String message, final Color color){
		final SimpleAttributeSet style = getStyle(color);

		final StyledDocument doc = textPane.getStyledDocument();
		final int start = doc.getLength();
		textPane.setCaretPosition(start);
		try{
			doc.insertString(start, message, style);
			doc.setParagraphAttributes(start, doc.getLength() - start, style, false);
		}
		catch(final BadLocationException ble){
			ble.printStackTrace();
		}
		textPane.replaceSelection(System.lineSeparator());
	}

	private static SimpleAttributeSet getStyle(final Color foreground){
		final SimpleAttributeSet style = new SimpleAttributeSet();
		final Font currentFont = FontHelper.getCurrentFont();
		StyleConstants.setFontFamily(style, currentFont.getFontName());
		StyleConstants.setFontSize(style, currentFont.getSize());
		StyleConstants.setForeground(style, foreground);
		StyleConstants.setFirstLineIndent(style, -INDENTATION);
		StyleConstants.setLeftIndent(style, INDENTATION);
		return style;
	}

}
