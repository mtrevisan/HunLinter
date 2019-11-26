package unit731.hunspeller.gui.tags;

import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.JavaHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;


public class JTagPanel extends JPanel{

	/**
	 * @param text	The text to be displayed, or <code>null</code>
	 */
	public JTagPanel(final String text){
		final JTextField t = new JTextField(text);
		final Dimension ps = t.getPreferredSize();
		final int height = 40;
		t.setPreferredSize(new Dimension(ps.width, height));
		t.setBorder(null);
		t.setOpaque(false);
		final JTagPanel parent = this;
		t.addKeyListener(new KeyAdapter(){
			public void keyReleased(final KeyEvent evt){
				final String text = t.getText();
				if(StringUtils.isNotBlank(text)){
					final JTagComponent tag = new JTagComponent(text.trim(), parent::removeTag);
					parent.add(tag, parent.getComponentCount() - 1);

					//reset input
					t.setText(StringUtils.EMPTY);

					//force repaint of the component
					repaint();
					revalidate();
				}
			}
		});

		setPreferredSize(new Dimension(400, height));
//		setLayout(new FlowLayout(1, 2, 2));
//		setBackground(Color.WHITE);
//		setBorder(javax.swing.BorderFactory.createLineBorder(Color.magenta));

		add(t);
	}

	private void removeTag(final JTagComponent tag){
		remove(tag);

		repaint();
		revalidate();
	}

	public Set<String> getTags(){
		return JavaHelper.nullableToStream(getComponents())
			.filter(comp -> comp instanceof JTagComponent)
			.flatMap(comp -> Arrays.stream(((JTagComponent)comp).getComponents()))
			.filter(comp -> comp instanceof JLabel)
			.map(comp -> ((JLabel)comp).getText())
			.collect(Collectors.toSet());
	}

}
