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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;


public final class GlyphComparator{

	private GlyphComparator(){}

	/**
	 * Perform glyph comparison, returning a list of matching codepoint tuples.
	 * Stops prematurely (or never starts) if run == false.
	 *
	 * @param font	The font to render the character in
	 * @param maxDifferenceThreshold	Maximum allowed difference
	 * @param chrs	Characters to be check for equality
	 * @return	Whether some given characters are identical in their glyph representation.
	 */
	public static boolean someIdenticalGlyphs(final Font font, final float maxDifferenceThreshold, final char... chrs){
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

	/**
	 * Render a BufferedImage containing the given character.
	 *
	 * @param font	The font to render the character in
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
		final DataBuffer data1 = img1.getData().getDataBuffer();
		final DataBuffer data2 = img2.getData().getDataBuffer();
		final int size = data1.getSize();
		int difference = 0;
		for(int i = 0; i < size; i ++)
			difference += (data1.getElem(i) != data2.getElem(i)? 1: 0);
		return (float)difference / size;
	}

}
