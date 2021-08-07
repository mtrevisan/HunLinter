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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static io.github.mtrevisan.hunlinter.services.system.LoopHelper.forEach;


public final class FontHelper{

	private static final Logger LOGGER = LoggerFactory.getLogger(FontHelper.class);

	private static final String CLIENT_PROPERTY_KEY_FONTABLE = "fontable";

//	private static final float SAME_FONT_MAX_THRESHOLD = 0.000_6f;

	private static final String GRAPHEME_I = "i";
	private static final String GRAPHEME_M = "m";
	private static final FontRenderContext FRC = new FontRenderContext(null, RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT,
		RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT);


	private static String LANGUAGE_SAMPLE;
	private static final Font[] ALL_FONTS;
	static{
		LOGGER.info("Load system fonts");
		ALL_FONTS = GraphicsEnvironment.getLocalGraphicsEnvironment()
			.getAllFonts();
		LOGGER.info("System fonts loaded");
	}
//	static{
//final TimeWatch watch = TimeWatch.start();
//		LOGGER.info("Load system fonts");
//		final String[] familyNames = GraphicsEnvironment.getLocalGraphicsEnvironment()
//			.getAvailableFontFamilyNames();
//
//		LOGGER.info("System fonts loaded");
//watch.stop();
//System.out.println("1: " + watch.toStringMillis());
//watch.reset();
//		ALL_FONTS = new ArrayList<>(familyNames.length);
//		for(final String familyName : familyNames){
//			final Font font = new Font(familyName, Font.PLAIN, 20);
//			//filter out non-plain fonts
//			//filter out those fonts which have `I` equals to `1` or `l`, and 'O' to '0'
//			if(font.isPlain()
//				&& !GlyphComparator.someIdenticalGlyphs(font, SAME_FONT_MAX_THRESHOLD, 'l', 'I', '1')
//				&& !GlyphComparator.someIdenticalGlyphs(font, SAME_FONT_MAX_THRESHOLD, 'O', '0'))
//				ALL_FONTS.add(font);
//			else
//				LOGGER.debug("Font '{}' discarded because has some identical letters (l/I/1, or O/0)", font.getName());
//		}
//		LOGGER.info("System fonts filtered");
//watch.stop();
//System.out.println("2: " + watch.toStringMillis());
//
//		ALL_FONTS.trimToSize();
//	}
	private static final List<Font> FAMILY_NAMES_ALL = new ArrayList<>();
	private static final List<Font> FAMILY_NAMES_MONOSPACED = new ArrayList<>();
	private static Font CURRENT_FONT = FontChooserDialog.getDefaultFont();


	private FontHelper(){}

	public static Font chooseBestFont(final String languageSample){
		extractFonts(languageSample);

		final List<Font> fonts = (FAMILY_NAMES_MONOSPACED.isEmpty()? FAMILY_NAMES_ALL: FAMILY_NAMES_MONOSPACED);
		final Font defaultFont = FontChooserDialog.getDefaultFont();
		Font bestFont = (fonts.isEmpty()? defaultFont: fonts.get(0));
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

	public static void extractFonts(final String languageSample){
		Objects.requireNonNull(languageSample, "Language sample cannot be null");

		if(!languageSample.equals(LANGUAGE_SAMPLE)){
			LANGUAGE_SAMPLE = languageSample;

			FAMILY_NAMES_ALL.clear();
			FAMILY_NAMES_MONOSPACED.clear();

			for(final Font font : ALL_FONTS)
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
		return CURRENT_FONT;
	}

	public static void addFontableProperty(final JComponent... components){
		forEach(components, component -> component.putClientProperty(CLIENT_PROPERTY_KEY_FONTABLE, true));
	}

	public static void setCurrentFont(final Font font, final Component... parentFrames){
		if(!font.equals(CURRENT_FONT)){
			CURRENT_FONT = font;

			forEach(parentFrames, parentFrame -> updateComponent(parentFrame, font));
		}
	}

	private static void updateComponent(final Component component, final Font font){
		final Deque<Component> stack = new ArrayDeque<>();
		stack.push(component);
		final Consumer<Component> push = stack::push;
		while(!stack.isEmpty()){
			final Component comp = stack.pop();

			if(comp instanceof JEditorPane)
				((JComponent)comp).putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
			if((comp instanceof JComponent) && ((JComponent)comp).getClientProperty(CLIENT_PROPERTY_KEY_FONTABLE) == Boolean.TRUE)
				comp.setFont(font);

			if(comp instanceof Container)
				forEach(((Container)comp).getComponents(), push);
		}
	}

}
