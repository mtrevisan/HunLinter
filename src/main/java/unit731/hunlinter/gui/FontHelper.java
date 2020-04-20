package unit731.hunlinter.gui;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
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

	private static final String CLIENT_PROPERTY_KEY_FONTABLE = "fontable";

	private static final String GRAPHEME_I = "i";
	private static final String GRAPHEME_M = "m";
	private static final FontRenderContext FRC = new FontRenderContext(null, RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT,
		RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT);


	private static String LANGUAGE_SAMPLE;
	private static final List<String> FAMILY_NAMES_ALL = new ArrayList<>();
	private static final List<String> FAMILY_NAMES_MONOSPACED = new ArrayList<>();
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
		Font bestFont = CURRENT_FONT;
		if(!canCurrentFontDisplay(languageSample)){
			//check to see if the error can be visualized, if not, change the font to one that can
			extractFonts(languageSample);

			final Function<String, WidthFontPair> widthFontPair = fontName -> {
				final Font currentFont = new Font(fontName, Font.PLAIN, FontHelper.CURRENT_FONT.getSize());
				final double w = getStringBounds(currentFont, languageSample)
					.getWidth();
				return WidthFontPair.of(w, currentFont);
			};
			final List<String> fontNames = (FAMILY_NAMES_MONOSPACED.isEmpty()? FAMILY_NAMES_ALL: FAMILY_NAMES_MONOSPACED);

			WidthFontPair bestPair = null;
			for(final String fontName : fontNames){
				final WidthFontPair doubleFontPair = widthFontPair.apply(fontName);
				if(bestPair == null || doubleFontPair.getWidth() > bestPair.getWidth())
					bestPair = doubleFontPair;
			}
			bestFont = (bestPair != null? bestPair.getFont(): CURRENT_FONT);
		}
		return getDefaultHeightFont(bestFont);
	}

	public static Font getDefaultHeightFont(final Font font){
		final Rectangle2D bounds = getStringBounds(font, "I");
		return font.deriveFont(Math.round(font.getSize() * 17.f / bounds.getHeight()));
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

			final String[] familyNames = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getAvailableFontFamilyNames();
			for(final String familyName : familyNames){
				final Font font = new Font(familyName, Font.PLAIN, 20);
				if(font.canDisplayUpTo(languageSample) < 0){
					FAMILY_NAMES_ALL.add(familyName);
					if(isMonospaced(font))
						FAMILY_NAMES_MONOSPACED.add(familyName);
				}
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
		return FAMILY_NAMES_ALL;
	}

	public static List<String> getFamilyNamesMonospaced(){
		return FAMILY_NAMES_MONOSPACED;
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
