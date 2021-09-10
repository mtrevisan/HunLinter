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
package io.github.mtrevisan.hunlinter.gui;

import io.github.mtrevisan.hunlinter.gui.dialogs.FontChooserDialog;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
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
import java.util.concurrent.FutureTask;


public final class FontHelper{

	private static final Logger LOGGER = LoggerFactory.getLogger(FontHelper.class);

	private static final String CLIENT_PROPERTY_KEY_FONTABLE = "fontable";

	private static final float SAME_FONT_MAX_THRESHOLD = 0.000_6f;

	private static final String GRAPHEME_I = "i";
	private static final String GRAPHEME_M = "m";
	private static final FontRenderContext FRC = new FontRenderContext(null, RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT,
		RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT);


	private static final FutureTask<List<Font>> FUTURE_ALL_FONTS;
	static{
		FUTURE_ALL_FONTS = JavaHelper.createFuture(() -> {
			LOGGER.info("Load system fonts");
			final String[] familyNames = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getAvailableFontFamilyNames();

			LOGGER.info("System fonts loaded");
			final ArrayList<Font> allFonts = new ArrayList<>(familyNames.length);
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

			allFonts.trimToSize();
			return allFonts;
		});
	}
	private static final List<Font> FAMILY_NAMES_ALL = new ArrayList<>(0);
	private static final List<Font> FAMILY_NAMES_MONOSPACED = new ArrayList<>(0);

	@SuppressWarnings("StaticVariableMayNotBeInitialized")
	private static String languageSample;
	private static Font currentFont = FontChooserDialog.getDefaultFont();


	private FontHelper(){}

	public static Font chooseBestFont(final String languageSample){
		extractFonts(languageSample);

		final List<Font> fonts = (FAMILY_NAMES_MONOSPACED.isEmpty()? FAMILY_NAMES_ALL: FAMILY_NAMES_MONOSPACED);
		final Font defaultFont = FontChooserDialog.getDefaultFont();
		Font bestFont = (fonts.isEmpty()? defaultFont: fonts.get(0).deriveFont(15.f));
		if(!bestFont.equals(defaultFont)){
			for(final Font f : fonts){
				final String defaultFontName = defaultFont.getName();
				if(f.getName().equals(defaultFontName)){
					bestFont = defaultFont;
					break;
				}
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

			FAMILY_NAMES_ALL.clear();
			FAMILY_NAMES_MONOSPACED.clear();

			final List<Font> allFonts = JavaHelper.waitForFuture(FUTURE_ALL_FONTS);
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
		for(final Font font : FAMILY_NAMES_ALL)
			names.add(font.getName());
		return names;
	}

	public static List<String> getFamilyNamesMonospaced(){
		final List<String> names = new ArrayList<>(FAMILY_NAMES_MONOSPACED.size());
		for(final Font font : FAMILY_NAMES_MONOSPACED)
			names.add(font.getName());
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

	public static void setCurrentFont(final Font font, final Component... parentFrames){
		if(!font.equals(currentFont)){
			currentFont = font;

			if(parentFrames != null)
				for(final Component parentFrame : parentFrames)
					updateComponent(parentFrame, font);
		}
	}

	private static void updateComponent(final Component component, final Font font){
		final Deque<Component> stack = new LinkedList<>();
		stack.push(component);
		while(!stack.isEmpty()){
			final Component comp = stack.pop();

			if(comp instanceof JEditorPane)
				((JComponent)comp).putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
			if((comp instanceof JComponent) && ((JComponent)comp).getClientProperty(CLIENT_PROPERTY_KEY_FONTABLE) == Boolean.TRUE)
				comp.setFont(font);

			if(comp instanceof Container)
				for(final Component c : ((Container)comp).getComponents())
					stack.push(c);
		}
	}

}
