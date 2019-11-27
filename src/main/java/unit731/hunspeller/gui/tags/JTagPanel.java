package unit731.hunspeller.gui.tags;

import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.JavaHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class JTagPanel extends JPanel{

	private final Object synchronizer = new Object();


	public JTagPanel(final int width, final int lines){
		final JTextField t = new JTextField();
		final Dimension ps = t.getPreferredSize();
		t.setPreferredSize(new Dimension(ps.width + 1, ps.height));
		t.setBorder(null);
		t.setOpaque(false);
		final JTagPanel parent = this;
		t.addKeyListener(new KeyAdapter(){
			@Override
			public void keyReleased(final KeyEvent evt){
				final String text = t.getText();
				if(StringUtils.isNotBlank(text)){
					synchronized(synchronizer){
						final JTagComponent tag = new JTagComponent(text.trim(), parent::removeTag);
						parent.add(tag, parent.getComponentCount() - 1);

						//reset input
						t.setText(StringUtils.EMPTY);

						forceRepaint();
					}
				}
			}
		});

		setLayout(new FlowLayout(FlowLayout.LEADING, 2, 4));
		setPreferredSize(new Dimension(width, ps.height * lines * 16 / 10));
		setBackground(UIManager.getColor("TextArea.background"));

		add(t);
	}

	private void removeTag(final JTagComponent tag){
		remove(tag);

		forceRepaint();
	}

	public void setTags(final List<String> tags){
		synchronized(synchronizer){
			for(final String tag : tags){
				final JTagComponent component = new JTagComponent(tag, this::removeTag);
				add(component, getComponentCount() - 1);
			}

			forceRepaint();
		}
	}

	private void forceRepaint(){
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
				GridBagConstraints gbc = new GridBagConstraints();
				setLayout(layout);
				gbc.gridx = 0;
				gbc.gridy = 0;

				JTagPanel panel = new JTagPanel(400, 3);
				panel.setTags(Arrays.asList("a", "b", "c"));
				add(panel, gbc);

				setVisible(true);
			}
		}

		//create and display the form
		EventQueue.invokeLater(() -> (new JTagPanelExample()).setVisible(true));
	}

}
