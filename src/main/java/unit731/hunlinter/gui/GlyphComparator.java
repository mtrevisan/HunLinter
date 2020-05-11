package unit731.hunlinter.gui;

import java.awt.*;
import java.awt.image.BufferedImage;


public class GlyphComparator{

	private GlyphComparator(){}

	/**
	 * Perform glyph comparison, returning a list of matching codepoint tuples.
	 * Stops prematurely (or never starts) if run == false.
	 *
	 * @param	chrs	Characters to be check for equality
	 * @return	Whether all the given characters are identical in theior glyph representation.
	 */
	public static boolean allIdenticalGlyphs(final Font font, final float maxDifferenceThreshold, final char... chrs){
		validateCharacters(chrs);

		final BufferedImage glyph0 = renderImage(font, chrs[0]);

		for(int i = 1; i < chrs.length; i ++){
			final BufferedImage glyph = renderImage(font, chrs[i]);
			if(visualSimilarity(glyph0, glyph) < maxDifferenceThreshold)
				return true;
		}
		return false;
	}

	/**
	 * Perform glyph comparison, returning a list of matching codepoint tuples.
	 * Stops prematurely (or never starts) if run == false.
	 *
	 * @param	chrs	Characters to be check for equality
	 * @return	Whether some of the given characters are identical in their glyph representation.
	 */
	public static boolean someIdenticalGlyphs(final Font font, final float maxDifferenceThreshold, final char... chrs){
		validateCharacters(chrs);

		final BufferedImage[] glyphs = new BufferedImage[chrs.length];
		glyphs[0] = renderImage(font, chrs[0]);
		for(int i = 0; i < chrs.length - 1; i ++)
			for(int j = i + 1; j < chrs.length; j ++){
				if(glyphs[j] == null)
					glyphs[j] = renderImage(font, chrs[j]);

				if(visualSimilarity(glyphs[i], glyphs[j]) < maxDifferenceThreshold)
					return true;
			}

		return false;
	}

	private static void validateCharacters(final char[] chrs){
		for(int i = 0; i < chrs.length; i++)
			if(! Character.isLetterOrDigit(chrs[i]))
				throw new IllegalArgumentException("Only letters or numbers chan be compared");
	}

	/**
	 * Render a BufferedImage containing the given character.
	 *
	 * @param chr	Character to render
	 * @return	The image of the given character
	 */
	private static BufferedImage renderImage(final Font font, final char chr){
		final BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
		final Graphics g = img.createGraphics();
		g.setColor(Color.BLACK);
		g.setFont(font);
		final int baseline = g.getFontMetrics().getMaxAscent() + 1;
		g.drawString(Character.toString(chr), 1, baseline);
		g.dispose();
		return img;
	}

	private static float visualSimilarity(final BufferedImage img1, final BufferedImage img2){
		int difference = 0;
		final int width = img1.getWidth();
		final int height = img1.getHeight();
		for(int x = width - 1; x >= 0; x --)
			for(int y = height - 1; y >= 0; y --)
				if(img1.getRGB(x, y) != img2.getRGB(x, y))
					difference ++;
		return (float)difference / (width * height);
	}

}
