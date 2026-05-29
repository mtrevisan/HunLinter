/**
 * Copyright (c) 2019-2026 Mauro Trevisan
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

import java.awt.Font;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;


public final class GlyphComparator{

	private GlyphComparator(){}

	/**
	 * Perform glyph comparison, returning a list of matching codepoint tuples.
	 * Stops prematurely (or never starts) if run == false.
	 *
	 * @param font	The font to render the character in.
	 * @param maxDifferenceThreshold	Maximum allowed difference.
	 * @param chrs	Characters to be checked for equality.
	 * @return	Whether some given characters are identical in their glyph representation.
	 */
	public static boolean haveIdenticalGlyphs(Font font, float maxDifferenceThreshold, char... chrs){
		final FontRenderContext frc = new FontRenderContext(null, true, true);
		final boolean[][] base = rasterizeGlyph(font, frc, chrs[0]);
		for(int i = 1; i < chrs.length; i ++){
			final boolean[][] other = rasterizeGlyph(font, frc, chrs[i]);
			if(visualSimilarity(base, other, maxDifferenceThreshold))
				return true;
		}
		return false;
	}

	// Converts a glyph into a small normalized bitmap (boolean grid)
	private static boolean[][] rasterizeGlyph(final Font font, final FontRenderContext frc, final char chr){
		final int size = 32;
		final boolean[][] grid = new boolean[size][size];
		final GlyphVector gv = font.createGlyphVector(frc, new char[]{chr});
		final Shape shape = gv.getOutline();
		final Rectangle2D bounds = shape.getBounds2D();
		if(bounds.isEmpty())
			return grid;

		final double scaleX = size / bounds.getWidth();
		final double scaleY = size / bounds.getHeight();
		final AffineTransform at = new AffineTransform();
		at.translate(-bounds.getX(), -bounds.getY());
		at.scale(scaleX, scaleY);
		final Shape normalized = at.createTransformedShape(shape);

		for(int y = 0; y < size; y ++)
			for(int x = 0; x < size; x ++)
				if(normalized.contains(x, y))
					grid[y][x] = true;
		return grid;
	}

	private static boolean visualSimilarity(final boolean[][] a, final boolean[][] b, final float threshold){
		int diff = 0;
		int total = a.length * a[0].length;
		for(int y = 0; y < a.length; y ++)
			for(int x = 0; x < a[y].length; x ++)
				if(a[y][x] != b[y][x]){
					diff ++;
					if((float)diff / total > threshold)
						return false;
				}
		return (float)diff / total < threshold;
	}

}
