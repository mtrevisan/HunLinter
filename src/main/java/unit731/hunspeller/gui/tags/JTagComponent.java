package unit731.hunspeller.gui.tags;

import unit731.hunspeller.services.JavaHelper;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


class JTagComponent extends JPanel{

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


	private final JPanel parent;


	JTagComponent(final String text, final JPanel parent){
		Objects.requireNonNull(parent);

		this.parent = parent;

		setLayout(new BorderLayout());
		setOpaque(false);

		final JLabel textLabel = new JLabel(text);
		textLabel.setForeground(COLOR_TEXT);
		Dimension ps = textLabel.getPreferredSize();
		textLabel.setPreferredSize(new Dimension(ps.width + PAD * 2, ps.height + PAD * 2));
		textLabel.setHorizontalAlignment(SwingConstants.CENTER);

		final JLabel closeLabel = new JLabel(TEXT_CROSS_MARK);
		final Font closeFont = closeLabel.getFont();
		closeLabel.setFont(closeFont.deriveFont(closeFont.getSize() * 3.f / 4.f));
		closeLabel.setForeground(COLOR_CLOSE);
		closeLabel.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent evt){
				parent.remove(JTagComponent.this);

				parent.repaint();
				parent.revalidate();
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
		closePanel.add(closeLabel);
		ps = closeLabel.getPreferredSize();
		closePanel.setPreferredSize(new Dimension(ps.width + PAD * 2, ps.height + PAD * 2));

		add(textLabel, BorderLayout.WEST);
		add(closePanel, BorderLayout.EAST);
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

	public Set<String> getTags(){
		return JavaHelper.nullableToStream(parent.getComponents())
			.filter(comp -> comp instanceof JTagComponent)
			.flatMap(comp -> Arrays.stream(((JTagComponent)comp).getComponents()))
			.filter(comp -> comp instanceof JLabel)
			.map(comp -> ((JLabel)comp).getText())
			.collect(Collectors.toSet());
	}

}
