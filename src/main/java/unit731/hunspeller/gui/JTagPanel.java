package unit731.hunspeller.gui;

import javax.swing.*;
import java.awt.*;


//https://github.com/richardeigenmann/TagCloud
//http://selectize.github.io/selectize.js/
public class JTagPanel extends JPanel{

	private static final Color COLOR_BACKGROUND = new Color(222, 231, 247);
	private static final Color COLOR_CLOSE = new Color(119, 119, 119);
	private static final Color COLOR_TEXT = new Color(85, 85, 85);

	private static final String TEXT_CROSS_MARK = "\u274C";


	//TODO
	//format GUI to look like `xgajho`
	//on enter in a jtextfield, create and place the tag, remove the text just inserted
	//on select and canc, remove the tag

	public JTagPanel(final String text){
		setLayout(new BorderLayout());

		final JLabel label = new JLabel(text);
		label.setBackground(COLOR_BACKGROUND);
		label.setForeground(COLOR_TEXT);
		label.setOpaque(true);

		final JLabel close = new JLabel(TEXT_CROSS_MARK);
		close.setOpaque(true);
		close.addMouseListener(new java.awt.event.MouseAdapter(){
			public void mousePressed(java.awt.event.MouseEvent evt){
				System.out.println("remove component " + this);
				//TODO
//				Example.example.removecomp(JTagPanel.this);
			}
		});
		add(close, BorderLayout.EAST);
		add(label, BorderLayout.WEST);
	}

}
