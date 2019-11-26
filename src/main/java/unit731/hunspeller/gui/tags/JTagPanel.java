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

	private final Object synchronizer = new Object();


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
					synchronized(synchronizer){
						final JTagComponent tag = new JTagComponent(text.trim(), parent::removeTag);
						parent.add(tag, parent.getComponentCount() - 1);

						//reset input
						t.setText(StringUtils.EMPTY);

						//force repaint of the component
						repaint();
						revalidate();
					}
				}
			}
		});

		//FIXME
		setPreferredSize(new Dimension(400, height));
		setLayout(new FlowLayout(1, 2, 2));
		setBackground(UIManager.getColor("TextArea.background"));
		setBorder(javax.swing.BorderFactory.createLineBorder(Color.BLACK));

		add(t);
	}

	private void removeTag(final JTagComponent tag){
		remove(tag);

		repaint();
		revalidate();
	}

	public Set<String> getTags(){
		synchronized(synchronizer){
			return JavaHelper.nullableToStream(getComponents())
				.filter(comp -> comp instanceof JTagComponent)
				.flatMap(comp -> Arrays.stream(((JTagComponent)comp).getComponents()))
				.filter(comp -> comp instanceof JLabel)
				.map(comp -> ((JLabel)comp).getText())
				.collect(Collectors.toSet());
		}
	}


	public static void main(String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e){}

		class JTagPanelExample extends JFrame{
			public JTagPanelExample(){
				setSize(new Dimension(500, 180));
				GridBagLayout layout = new GridBagLayout();
				GridBagConstraints c = new GridBagConstraints();
				setLayout(layout);
				c.gridx = 0;
				c.gridy = 0;

				JPanel panel = new JTagPanel("type");
				add(panel, c);
				setVisible(true);
			}
		}

		//create and display the form
		EventQueue.invokeLater(() -> (new JTagPanelExample()).setVisible(true));
	}

}
