package unit731.hunlinter.gui;

import unit731.hunlinter.parsers.exceptions.ExceptionsParser;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class JTagPanel extends JPanel{

	private final BiConsumer<ExceptionsParser.TagChangeType, List<String>> tagsChanged;


	public JTagPanel(){
		tagsChanged = null;
	}

	public JTagPanel(final BiConsumer<ExceptionsParser.TagChangeType, List<String>> tagsChanged){
		this.tagsChanged = tagsChanged;

		setLayout(new HorizontalFlowLayout(FlowLayout.LEFT, 2, 0));
		setBackground(UIManager.getColor("TextField.background"));
	}

	public void initializeTags(final List<String> tags){
		synchronized(getTreeLock()){
			if(tags == null)
				removeAll();
			else
				tags.forEach(this::createAndAddTag);

			forceRepaint();
		}
	}

	public void addTag(final String tag){
		synchronized(getTreeLock()){
			createAndAddTag(tag);

			if(tagsChanged != null)
				tagsChanged.accept(ExceptionsParser.TagChangeType.ADD, Collections.singletonList(tag));

			forceRepaint();
		}
	}

	private void createAndAddTag(final String tag){
		final JTagComponent component = new JTagComponent(tag, this::removeTag);
		add(component, BorderLayout.LINE_END);
	}

	private void removeTag(final JTagComponent tag){
		synchronized(getTreeLock()){
			remove(tag);

			if(tagsChanged != null)
				tagsChanged.accept(ExceptionsParser.TagChangeType.REMOVE, Collections.singletonList(tag.getTag()));

			forceRepaint();
		}
	}

	private void forceRepaint(){
		repaint();
		revalidate();
	}

	public void applyFilter(final String tag){
		EventQueue.invokeLater(() -> {
			if(tag == null || tag.isEmpty())
				for(final Component component : getComponents())
					component.setVisible(true);
			else
				for(final Component component : getComponents())
					component.setVisible(((JTagComponent)component).getTag().contains(tag));
		});
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

	private static class JTagComponent extends JComponent{

		public JTagComponent(final String text, final Consumer<JTagComponent> tagRemover){
			Objects.requireNonNull(tagRemover);

			setLayout(new BorderLayout());
			setOpaque(false);

			final JLabel textLabel = new JLabel(text);
			textLabel.setFont(GUIUtils.getCurrentFont());
			textLabel.setForeground(COLOR_TEXT);
			Dimension ps = textLabel.getPreferredSize();
			final Dimension textLabelSize = new Dimension(ps.width + PAD * 2, ps.height + PAD * 4);
			textLabel.setPreferredSize(textLabelSize);
			textLabel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			textLabel.setHorizontalAlignment(SwingConstants.CENTER);

			final JLabel closeLabel = new JLabel(TEXT_CROSS_MARK);
			final Font closeFont = closeLabel.getFont();
			closeLabel.setFont(closeFont.deriveFont(closeFont.getSize() * 3.f / 4.f));
			closeLabel.setForeground(COLOR_CLOSE);
			closeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			graphics.setColor(COLOR_BACKGROUND);
			graphics.fillRoundRect(0, PAD, width, height, CORNER_RADIUS.width, CORNER_RADIUS.height);
			graphics.setColor(COLOR_BORDER);
			graphics.setStroke(new BasicStroke(BORDER_THICKNESS));
			graphics.drawRoundRect(0, PAD, width, height, CORNER_RADIUS.width, CORNER_RADIUS.height);
			//reset strokes to default
			graphics.setStroke(new BasicStroke());
		}

		public String getTag(){
			return ((JLabel)getComponent(0)).getText();
		}

	}

}
