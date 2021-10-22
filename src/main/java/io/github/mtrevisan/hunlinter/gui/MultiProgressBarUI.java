package io.github.mtrevisan.hunlinter.gui;

import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;


public class MultiProgressBarUI extends BasicProgressBarUI{

//	private Color foregroundColorTop = new Color(205, 255, 205);
//	private Color foregroundColorMiddleTop = new Color(156, 237, 172);
//	private Color foregroundColorMiddleBottom = new Color(1, 211, 41);
//	private Color foregroundColorBottom = new Color(28, 225, 51);
//
//	private Color errorColorTop = new Color(255, 205, 205);
//	private Color errorColorMiddleTop = new Color(237, 156, 172);
//	private Color errorColorMiddleBottom = new Color(211, 1, 41);
//	public Color errorColorBottom = new Color(225, 28, 51);

	public static final Color MAIN_COLOR = new Color(6, 176, 37);
	public static final Color ERROR_COLOR = new Color(225, 28, 51);


	@Override
	protected void paintDeterminate(final Graphics g, final JComponent c){
		if(!(g instanceof final Graphics2D g2))
			return;

		//calculate the actual dimensions of the progress bar area, discounting the insets, etc.
		final Insets b = progressBar.getInsets();
		final int barRectWidth = progressBar.getWidth() - (b.right + b.left);
		final int barRectHeight = 2 * ((progressBar.getHeight() - (b.top + b.bottom)) / 2);
		if(barRectWidth <= 0 || barRectHeight <= 0)
			return;

		//amount of progress to draw
		final int amountFull = getAmountFull(b, barRectWidth, barRectHeight);

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(progressBar.getForeground());

		if(progressBar.getOrientation() == SwingConstants.HORIZONTAL){
			g2.setStroke(new BasicStroke(barRectHeight, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));

			final int y = b.top + barRectHeight / 2;
			if(c.getComponentOrientation().isLeftToRight())
				g2.drawLine(b.left, y, b.left + amountFull, y);
			else{
				final int x = b.left + barRectWidth;
				g2.drawLine(x, y, x - amountFull, y);
			}
		}
		else{
			g2.setStroke(new BasicStroke(barRectWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));

			final int x = b.left + barRectWidth / 2;
			final int y = b.top + barRectHeight;
			g2.drawLine(x, y, x, y - amountFull);
		}
	}

}
