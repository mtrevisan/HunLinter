/**
 * Copyright (c) 2019-2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package unit731.hunlinter.gui.components;

import unit731.hunlinter.gui.FontHelper;
import unit731.hunlinter.parsers.exceptions.ExceptionsParser;
import unit731.hunlinter.services.system.JavaHelper;

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

import static unit731.hunlinter.services.system.LoopHelper.forEach;


public class TagPanel extends JPanel{

	private static final long serialVersionUID = 665517573169978352L;


	private final BiConsumer<ExceptionsParser.TagChangeType, List<String>> tagsChanged;


	public TagPanel(){
		this(null);
	}

	public TagPanel(final BiConsumer<ExceptionsParser.TagChangeType, List<String>> tagsChanged){
		super();

		this.tagsChanged = tagsChanged;

		setLayout(new HorizontalFlowLayout(FlowLayout.LEFT, 2, 0));
	}

	@Override
	public Color getBackground(){
		return UIManager.getColor("TextField.background");
	}

	@Override
	public void setFont(final Font font){
		//FIXME find a way to dynamically change the font of each tag and re-adjust container layout
		synchronized(getTreeLock()){
			final int size = getComponents().length;
			if(size > 0 && getComponents()[0].getFont() != FontHelper.getCurrentFont()){
				//remove all tags
				final String[] tags = new String[size];
				int i = 0;
				for(final Component component : getComponents())
					tags[i ++] = ((JTagComponent)component).getTag();

				removeAll();

				//re-insert all tags
				for(final String tag : tags){
					final JTagComponent component = new JTagComponent(tag, this::removeTag);
					add(component, BorderLayout.LINE_END);
				}

				forceRepaint();
			}
		}
	}

	public void initializeTags(final List<String> tags){
		synchronized(getTreeLock()){
			if(tags == null)
				removeAll();
			else
				forEach(tags, tag -> {
					final JTagComponent component = new JTagComponent(tag, this::removeTag);
					add(component, BorderLayout.LINE_END);
				});

			forceRepaint();
		}
	}

	public void addTag(final String tag){
		synchronized(getTreeLock()){
			final JTagComponent component = new JTagComponent(tag, this::removeTag);
			add(component, BorderLayout.LINE_END);

			if(tagsChanged != null)
				tagsChanged.accept(ExceptionsParser.TagChangeType.ADD, Collections.singletonList(tag));

			forceRepaint();
		}
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
		JavaHelper.executeOnEventDispatchThread(() -> {
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

		private static final long serialVersionUID = -7410352884175789897L;


		JTagComponent(final String text, final Consumer<JTagComponent> tagRemover){
			Objects.requireNonNull(tagRemover);

			setLayout(new BorderLayout());
			setOpaque(false);

			final JLabel textLabel = new JLabel(text);
			textLabel.setFont(FontHelper.getCurrentFont());
			textLabel.setForeground(COLOR_TEXT);
			Dimension ps = textLabel.getPreferredSize();
			final Dimension textLabelSize = new Dimension(ps.width + PAD * 2, ps.height + PAD * 4);
			textLabel.setPreferredSize(textLabelSize);
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
			final JPanel closePanel = new JPanel(new GridLayout());
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
