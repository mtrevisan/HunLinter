package unit731.hunspeller.gui.tags;

import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;


public class JTagPanel extends JPanel{

	public JTagPanel(){
		JTextField t = new JTextField("type here", 10);
		Dimension ps = t.getPreferredSize();
		int height = 40;
		t.setPreferredSize(new Dimension(ps.width, height));
		setPreferredSize(new Dimension(400, height));
		setLayout(new FlowLayout(1, 2, 2));
		setBackground(Color.white);
		setBorder(javax.swing.BorderFactory.createLineBorder(Color.magenta));
		t.setBorder(null);
		t.setOpaque(false);
		JPanel parent = this;
		t.addKeyListener(new KeyAdapter(){
			public void keyReleased(final KeyEvent evt){
				final String s = t.getText();
				if(s.length() > 0){
					final JTagComponent tagp1 = new JTagComponent(s, parent);
					parent.add(tagp1, parent.getComponentCount() - 1);

					t.setText(StringUtils.EMPTY);

					repaint();
					revalidate();
				}
			}
		});

		add(t);
	}

}
