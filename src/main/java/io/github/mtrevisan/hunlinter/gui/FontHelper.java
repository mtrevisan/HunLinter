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
package io.github.mtrevisan.hunlinter.gui;

import io.github.mtrevisan.hunlinter.gui.dialogs.FontChooserDialog;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;


public final class FontHelper{

	private static final Logger LOGGER = LoggerFactory.getLogger(FontHelper.class);

	private static final String CLIENT_PROPERTY_KEY_FONTABLE = "fontable";
	private static final String CLIENT_PROPERTY_KEY_LOGGABLE = "loggable";

	private static final float SAME_FONT_MAX_THRESHOLD = 0.000_6f;

	private static final String GRAPHEME_I = "i";
	private static final String GRAPHEME_M = "m";
	private static final FontRenderContext FRC = new FontRenderContext(null, RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT,
		RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT);


	@SuppressWarnings("StaticVariableMayNotBeInitialized")
	private static Future<List<Font>> futureAllFonts;
	private static final ArrayList<Font> FAMILY_NAMES_ALL = new ArrayList<>(0);
	private static final ArrayList<Font> FAMILY_NAMES_MONOSPACED = new ArrayList<>(0);

	@SuppressWarnings("StaticVariableMayNotBeInitialized")
	private static String languageSample;
	private static Font currentFont = FontChooserDialog.getDefaultFont();


	private FontHelper(){}

	public static void loadAllFonts(){
		futureAllFonts = JavaHelper.executeFuture(() -> {
			LOGGER.info("Load system fonts");
			final String[] familyNames = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getAvailableFontFamilyNames();

			LOGGER.info("System fonts loaded");
			final List<Font> allFonts = new ArrayList<>(familyNames.length);
			for(final String familyName : familyNames){
				final Font font = new Font(familyName, Font.PLAIN, 20);
				//filter out non-plain fonts
				//filter out those fonts which have `I` equals to `1` or `l`, and 'O' to '0'
				if(font.isPlain()
						&& !GlyphComparator.haveIdenticalGlyphs(font, SAME_FONT_MAX_THRESHOLD, 'l', 'I', '1')
						&& !GlyphComparator.haveIdenticalGlyphs(font, SAME_FONT_MAX_THRESHOLD, 'O', '0'))
					allFonts.add(font);
				else
					LOGGER.debug("Font '{}' discarded because has some identical letters (l/I/1, or O/0)", font.getName());
			}
			LOGGER.info("System fonts filtered");

			return allFonts;
		});
	}

	public static Font chooseBestFont(final String languageSample){
		extractFonts(languageSample);

		final List<Font> fonts = (FAMILY_NAMES_MONOSPACED.isEmpty()? FAMILY_NAMES_ALL: FAMILY_NAMES_MONOSPACED);
		final Font defaultFont = FontChooserDialog.getDefaultFont();
		Font bestFont = (fonts.isEmpty()? defaultFont: fonts.get(0).deriveFont(15.f));
		if(!bestFont.equals(defaultFont))
			for(final Font f : fonts){
				final String defaultFontName = defaultFont.getName();
				if(f.getName().equals(defaultFontName)){
					bestFont = defaultFont;
					break;
				}
			}

		LOGGER.info("Best font: '{}', size {}", bestFont.getFontName(), bestFont.getSize());

		return bestFont;
	}

	@SuppressWarnings("StaticVariableUsedBeforeInitialization")
	public static void extractFonts(final String languageSample){
		Objects.requireNonNull(languageSample, "Language sample cannot be null");

		if(!languageSample.equals(FontHelper.languageSample)){
			FontHelper.languageSample = languageSample;

			final List<Font> allFonts = JavaHelper.waitForFuture(futureAllFonts);
			if(FAMILY_NAMES_ALL.isEmpty()){
				FAMILY_NAMES_ALL.ensureCapacity(allFonts.size());
				FAMILY_NAMES_MONOSPACED.ensureCapacity(allFonts.size());
			}
			else{
				FAMILY_NAMES_ALL.clear();
				FAMILY_NAMES_MONOSPACED.clear();
			}
			for(final Font font : allFonts)
				if(font.canDisplayUpTo(languageSample) < 0){
					FAMILY_NAMES_ALL.add(font);
					if(isMonospaced(font))
						FAMILY_NAMES_MONOSPACED.add(font);
				}
		}
	}

	private static boolean isMonospaced(final Font font){
		final double iWidth = font.getStringBounds(GRAPHEME_I, FRC).getWidth();
		final double mWidth = font.getStringBounds(GRAPHEME_M, FRC).getWidth();
		return (Math.abs(iWidth - mWidth) <= 1);
	}

	public static List<String> getFamilyNamesAll(){
		final List<String> names = new ArrayList<>(FAMILY_NAMES_ALL.size());
		for(int i = 0; i < FAMILY_NAMES_ALL.size(); i ++)
			names.add(FAMILY_NAMES_ALL.get(i).getName());
		return names;
	}

	public static List<String> getFamilyNamesMonospaced(){
		final List<String> names = new ArrayList<>(FAMILY_NAMES_MONOSPACED.size());
		for(int i = 0; i < FAMILY_NAMES_MONOSPACED.size(); i ++)
			names.add(FAMILY_NAMES_MONOSPACED.get(i).getName());
		return names;
	}

	public static Font getCurrentFont(){
		return currentFont;
	}

	public static void addFontableProperty(final JComponent... components){
		if(components != null)
			for(final JComponent component : components)
				component.putClientProperty(CLIENT_PROPERTY_KEY_FONTABLE, Boolean.TRUE);
	}

	public static void addLoggableProperty(final JComponent... components){
		if(components != null)
			for(final JComponent component : components)
				component.putClientProperty(CLIENT_PROPERTY_KEY_LOGGABLE, Boolean.TRUE);
	}

	public static void setCurrentFont(final Font font, final Component parentFrame){
		if(!font.equals(currentFont)){
			currentFont = font;

			if(parentFrame != null)
				updateComponent(parentFrame, font);
		}
	}

	private static void updateComponent(final Component component, final Font font){
		final Deque<Component> stack = new LinkedList<>();
		stack.push(component);
		while(!stack.isEmpty()){
			final Component comp = stack.pop();

			if(comp instanceof JEditorPane editorPane)
				editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
			if((comp instanceof JComponent trueComponent) && trueComponent.getClientProperty(CLIENT_PROPERTY_KEY_FONTABLE) == Boolean.TRUE)
				comp.setFont(font);
			//reaffirms the positioning (sometimes it returns to the principle of the text area)
			if((comp instanceof JTextArea textArea) && textArea.getClientProperty(CLIENT_PROPERTY_KEY_LOGGABLE) == Boolean.TRUE)
				SwingUtilities.invokeLater(() -> textArea.setCaretPosition(textArea.getDocument().getLength()));

			if(comp instanceof Container container)
				for(final Component c : container.getComponents())
					stack.push(c);
		}
	}

}
