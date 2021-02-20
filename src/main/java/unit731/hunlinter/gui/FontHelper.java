/**
 * Copyright (c) 2019-2020 Mauro Trevisan
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
package unit731.hunlinter.gui;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.gui.dialogs.FontChooserDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import static unit731.hunlinter.services.system.LoopHelper.forEach;


public final class FontHelper{

	private static final Logger LOGGER = LoggerFactory.getLogger(FontHelper.class);

	private static final String CLIENT_PROPERTY_KEY_FONTABLE = "fontable";

	private static final float SAME_FONT_MAX_THRESHOLD = 0.000_6f;

	private static final String GRAPHEME_I = "i";
	private static final String GRAPHEME_M = "m";
	private static final FontRenderContext FRC = new FontRenderContext(null, RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT,
		RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT);


	private static String LANGUAGE_SAMPLE;
	private static final ArrayList<Font> ALL_FONTS;
	static{
		final String[] familyNames = GraphicsEnvironment.getLocalGraphicsEnvironment()
			.getAvailableFontFamilyNames();
		ALL_FONTS = new ArrayList<>(familyNames.length);
		for(final String familyName : familyNames){
			final Font font = new Font(familyName, Font.PLAIN, 20);
			//filter out non-plain fonts
			//filter out those fonts which have `I` equals to `1` or `l`, and 'O' to '0'
			if(font.isPlain()
					&& !GlyphComparator.someIdenticalGlyphs(font, SAME_FONT_MAX_THRESHOLD, 'l', 'I', '1')
					&& !GlyphComparator.allIdenticalGlyphs(font, SAME_FONT_MAX_THRESHOLD, 'O', '0'))
				ALL_FONTS.add(font);
			else
				LOGGER.trace("Font '{}' discarded because has some identical letters (l/I/1, or O/0)", font.getName());
		}
		ALL_FONTS.trimToSize();
	}
	private static final List<Font> FAMILY_NAMES_ALL = new ArrayList<>();
	private static final List<Font> FAMILY_NAMES_MONOSPACED = new ArrayList<>();
	private static Font CURRENT_FONT = FontChooserDialog.getDefaultFont();

	private static final class WidthFontPair{
		private final double width;
		private final Font font;

		public static WidthFontPair of(final double width, final Font font){
			return new WidthFontPair(width, font);
		}

		private WidthFontPair(final double width, final Font font){
			this.width = width;
			this.font = font;
		}

		public double getWidth(){
			return width;
		}

		public Font getFont(){
			return font;
		}

		@Override
		public String toString(){
			return width + ": " + font.getName();
		}

		@Override
		public boolean equals(final Object obj){
			if(obj == this)
				return true;
			if(obj == null || obj.getClass() != getClass())
				return false;

			final WidthFontPair rhs = (WidthFontPair)obj;
			return new EqualsBuilder()
				.append(font, rhs.font)
				.isEquals();
		}

		@Override
		public int hashCode(){
			return new HashCodeBuilder()
				.append(font)
				.toHashCode();
		}
	}


	private FontHelper(){}

	//FIXME should also consider DictionaryParser.COUNTER_GROUPING_SEPARATOR?
	public static Font chooseBestFont(final String languageSample){
		extractFonts(languageSample);

		final Function<Font, WidthFontPair> widthFontPair = font -> {
			final double w = getStringBounds(font, languageSample)
				.getWidth();
			return WidthFontPair.of(w, font);
		};
		final List<Font> fonts = (FAMILY_NAMES_MONOSPACED.isEmpty()? FAMILY_NAMES_ALL: FAMILY_NAMES_MONOSPACED);

		WidthFontPair bestPair = null;
		for(final Font font : fonts){
			final WidthFontPair doubleFontPair = widthFontPair.apply(font);
			if(bestPair == null || doubleFontPair.getWidth() > bestPair.getWidth())
				bestPair = doubleFontPair;
		}
		Font bestFont = (bestPair != null? bestPair.getFont(): CURRENT_FONT);
		bestFont = getDefaultHeightFont(bestFont);
		LOGGER.info("Best font: '{}', size {}", bestFont.getFontName(), bestFont.getSize());
		return bestFont;
	}

	public static Font getDefaultHeightFont(final Font font){
		final Rectangle2D bounds = getStringBounds(font, "I");
		return font.deriveFont(font.getSize() * 17.9f / (float)bounds.getHeight());
	}

	private static Rectangle2D getStringBounds(final Font font, final String text){
		return font.getStringBounds(text, FRC);
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

	private static boolean canCurrentFontDisplay(final String languageSample){
		return (CURRENT_FONT.canDisplayUpTo(languageSample) < 0);
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
