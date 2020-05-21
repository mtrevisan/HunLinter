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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.function.Function;

import static unit731.hunlinter.services.system.LoopHelper.forEach;


public class FontHelper{

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
			//filter out those fonts which have `I` equals to `1` or `l`, and 'O' to '0'
			if(!GlyphComparator.someIdenticalGlyphs(font, SAME_FONT_MAX_THRESHOLD, 'l', 'I', '1')
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
		Objects.requireNonNull(languageSample);

		if(!languageSample.equals(FontHelper.LANGUAGE_SAMPLE)){
			FontHelper.LANGUAGE_SAMPLE = languageSample;

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
		final Stack<Component> stack = new Stack<>();
		stack.push(component);
		while(!stack.isEmpty()){
			final Component comp = stack.pop();

			if(comp instanceof JEditorPane)
				((JEditorPane)comp).putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
			if((comp instanceof JComponent) && ((JComponent)comp).getClientProperty(CLIENT_PROPERTY_KEY_FONTABLE) == Boolean.TRUE)
				comp.setFont(font);

			if(comp instanceof Container)
				forEach(((Container)comp).getComponents(), stack::push);
		}
	}

}
