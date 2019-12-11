package unit731.hunspeller.gui;

import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.JavaHelper;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class JTagPanel extends JPanel{

	private final Object synchronizer = new Object();


	public JTagPanel(){
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

		setLayout(new FlowLayout(FlowLayout.LEADING, 2, 0));
		setBackground(UIManager.getColor("TextArea.background"));

		add(t);
	}

	private void removeTag(final JTagComponent tag){
		remove(tag);

		forceRepaint();
	}

	public void setTags(final List<String> tags){
		synchronized(synchronizer){
			if(tags == null)
				removeAll();
			else
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

				JTagPanel panel = new JTagPanel();
				panel.setPreferredSize(new Dimension(400, 100));
				panel.setTags(Arrays.asList("a", "b", "c"));
				add(panel, gbc);

				setVisible(true);
			}
		}

		//create and display the form
		EventQueue.invokeLater(() -> (new JTagPanelExample()).setVisible(true));
	}


	private static final Color COLOR_BACKGROUND = new Color(222, 231, 247);
	private static final Color COLOR_BORDER = new Color(202, 216, 242);
	private static final Color COLOR_TEXT = new Color(85, 85, 85);
	private static final Color COLOR_CLOSE = new Color(119, 119, 119);

	private static final String TEXT_CROSS_MARK = "\u274C";

	//values for horizontal and vertical radius of corner arcs
	private static final Dimension CORNER_RADIUS = new Dimension(5, 5);
	private static final int BORDER_THICKNESS = 1;
	private static final int PAD = 3;

	private static final Border CLOSE_BORDER = BorderFactory.createLineBorder(COLOR_CLOSE, 1);

	private class JTagComponent extends JPanel{

		private JTagComponent(final String text, final Consumer<JTagComponent> tagRemover){
			Objects.requireNonNull(tagRemover);

			setLayout(new BorderLayout());
			setOpaque(false);

			final JLabel textLabel = new JLabel(text);
			textLabel.setForeground(COLOR_TEXT);
			Dimension ps = textLabel.getPreferredSize();
			final Dimension textLabelSize = new Dimension(ps.width + PAD * 2, ps.height + PAD * 4);
			textLabel.setPreferredSize(textLabelSize);
			textLabel.setHorizontalAlignment(SwingConstants.CENTER);

			final JLabel closeLabel = new JLabel(TEXT_CROSS_MARK);
			final Font closeFont = closeLabel.getFont();
			closeLabel.setFont(closeFont.deriveFont(closeFont.getSize() * 3.f / 4.f));
			closeLabel.setForeground(COLOR_CLOSE);
			closeLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mousePressed(final MouseEvent evt){
					tagRemover.accept(JTagComponent.this);
				}

				@Override
				public void mouseEntered(final MouseEvent evt){
					closeLabel.setBorder(CLOSE_BORDER);
				}

				@Override
				public void mouseExited(final MouseEvent evt){
					closeLabel.setBorder(null);
				}
			});
			final JPanel closePanel = new JPanel(new GridBagLayout());
			closePanel.setOpaque(false);
			ps = closeLabel.getPreferredSize();
			final Dimension closePanelSize = new Dimension(ps.width + PAD * 2, ps.height + PAD * 4);
			closePanel.setPreferredSize(closePanelSize);
			closePanel.add(closeLabel);

			add(textLabel, BorderLayout.WEST);
			add(closePanel, BorderLayout.EAST);
		}

		@Override
		protected void paintComponent(final Graphics g){
			super.paintComponent(g);

			final int width = getWidth() - 1;
			final int height = getHeight() - PAD * 2 - 1;
			final Graphics2D graphics = (Graphics2D)g;
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			graphics.setColor(COLOR_BACKGROUND);
			graphics.fillRoundRect(0, PAD, width, height, CORNER_RADIUS.width, CORNER_RADIUS.height);
			graphics.setColor(COLOR_BORDER);
			graphics.setStroke(new BasicStroke(BORDER_THICKNESS));
			graphics.drawRoundRect(0, PAD, width, height, CORNER_RADIUS.width, CORNER_RADIUS.height);
			//reset strokes to default
			graphics.setStroke(new BasicStroke());
		}

	}

}
