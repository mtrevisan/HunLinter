package unit731.hunspeller.gui;

import javax.swing.*;
import java.awt.*;


//https://github.com/richardeigenmann/TagCloud
//http://selectize.github.io/selectize.js/
public class JTagPanel extends JPanel{

	private static final Color COLOR_BACKGROUND = new Color(222, 231, 247);
	private static final Color COLOR_BORDER = new Color(202, 216, 242);
	private static final Color COLOR_TEXT = new Color(85, 85, 85);
	private static final Color COLOR_CLOSE = new Color(119, 119, 119);

	private static final String TEXT_CROSS_MARK = "\u274C";

	//values for horizontal and vertical radius of corner arcs
	private static final Dimension CORNER_RADIUS = new Dimension(5, 5);
	private static final int BORDER_THICKNESS = 1;


	//TODO
	//format GUI to look like `xgajho`
	//on enter in a jtextfield, create and place the tag, remove the text just inserted
	//on select and canc, remove the tag

	public JTagPanel(final String text){
		setLayout(new BorderLayout());
		setOpaque(false);

		final JLabel label = new JLabel(text);
		label.setForeground(COLOR_TEXT);

		final JLabel close = new JLabel(TEXT_CROSS_MARK);
		final Font closeFont = close.getFont();
		close.setFont(closeFont.deriveFont(closeFont.getSize() * 3.f / 4.f));
		label.setForeground(COLOR_CLOSE);
		close.addMouseListener(new java.awt.event.MouseAdapter(){
			public void mousePressed(java.awt.event.MouseEvent evt){
				System.out.println("remove component " + this);
				//TODO
//				Example.example.removecomp(JTagPanel.this);
			}
		});

		add(label, BorderLayout.WEST);
		add(close, BorderLayout.EAST);
	}

	@Override
	protected void paintComponent(final Graphics g){
		super.paintComponent(g);

		final int width = getWidth();
		final int height = getHeight();
		final Graphics2D graphics = (Graphics2D)g;
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		graphics.setColor(COLOR_BACKGROUND);
		graphics.fillRoundRect(0, 0, width - 1, height - 1, CORNER_RADIUS.width, CORNER_RADIUS.height);
		graphics.setColor(COLOR_BORDER);
		graphics.setStroke(new BasicStroke(BORDER_THICKNESS));
		graphics.drawRoundRect(0, 0, width - 1, height - 1, CORNER_RADIUS.width, CORNER_RADIUS.height);
		//reset strokes to default
		graphics.setStroke(new BasicStroke());
	}

}
