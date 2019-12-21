package unit731.hunspeller.gui;

import java.awt.*;
import java.io.Serializable;


/**
 * A flow layout arranges components in a directional flow, much like lines of text in a paragraph. The flow direction is
 * determined by the container's <code>componentOrientation</code> property and may be one of two values:
 * <ul>
 * <li><code>ComponentOrientation.TOP_TO_BOTTOM</code>
 * <li><code>ComponentOrientation.BOTTOM_TO_TOP</code>
 * </ul>
 * Flow layouts are typically used to arrange buttons in a panel. It arranges buttons
 * horizontally until no more buttons fit on the same line. The line alignment is determined by the <code>align</code>
 * property. The possible values are:
 * <ul>
 * <li>{@link Alignment#LEFT_JUSTIFIED TOP}
 * <li>{@link Alignment#RIGHT_JUSTIFIED BOTTOM}
 * <li>{@link Alignment#CENTERED CENTER}
 * </ul>
 * <p>
 */
public class HorizontalFlowLayout implements LayoutManager, Serializable{

	public enum Alignment{
		/** This value indicates that each row of components should be left-justified */
		LEFT_JUSTIFIED,
		/** This value indicates that each row of components should be centered */
		CENTERED,
		/** This value indicates that each row of components should be right-justified */
		RIGHT_JUSTIFIED
	}


	/**
	 * <code>align</code> is the property that determines
	 * how each column distributes empty space.
	 * It can be one of the following three values:
	 * <ul>
	 * <code>Alignment.LEFT_JUSTIFIED</code>
	 * <code>Alignment.CENTERED</code>
	 * <code>Alignment.RIGHT_JUSTIFIED</code>
	 * </ul>
	 */
	private final Alignment align;
	/**
	 * The flow layout manager allows a separation of components with gaps.  The horizontal gap will
	 * specify the space between components and between the components and the borders of the
	 * <code>Container</code>.
	 */
	private final int hgap;
	/**
	 * The flow layout manager allows a separation of components with gaps.  The vertical gap will
	 * specify the space between rows and between the the rows and the borders of the <code>Container</code>.
	 */
	private final int vgap;


	/**
	 * Creates a new flow layout manager with the indicated alignment
	 * and the indicated horizontal and vertical gaps.
	 * <p>
	 * The value of the alignment argument must be one of
	 * <code>HorizontalFlowLayout.TOP</code>, <code>HorizontalFlowLayout.BOTTOM</code>,
	 * or <code>HorizontalFlowLayout.CENTER</code>.
	 *
	 * @param align the alignment value
	 * @param hgap  the horizontal gap between components
	 *              and between the components and the
	 *              borders of the <code>Container</code>
	 * @param vgap  the vertical gap between components
	 *              and between the components and the
	 *              borders of the <code>Container</code>
	 */
	public HorizontalFlowLayout(final Alignment align, final int hgap, final int vgap){
		this.align = align;
		this.hgap = hgap;
		this.vgap = vgap;
	}

	/**
	 * Adds the specified component to the layout.<br />
	 * Not used by this class.
	 *
	 * @param name the name of the component
	 * @param component the component to be added
	 */
	@Override
	public void addLayoutComponent(final String name, final Component component){}

	/**
	 * Removes the specified component from the layout.<br />
	 * Not used by this class.
	 *
	 * @param component the component to remove
	 * @see java.awt.Container#removeAll
	 */
	@Override
	public void removeLayoutComponent(final Component component){}

	/**
	 * Returns the preferred dimensions for this layout given the
	 * <i>visible</i> components in the specified target container.
	 *
	 * @param target the container that needs to be laid out
	 * @return the preferred dimensions to lay out the
	 * subcomponents of the specified container
	 * @see Container
	 * @see #minimumLayoutSize
	 * @see java.awt.Container#getPreferredSize
	 */
	public Dimension preferredLayoutSize(final Container target){
		synchronized(target.getTreeLock()){
			final Dimension dim = new Dimension(0, 0);
			boolean firstVisibleComponent = true;
			final int size = target.getComponentCount();
			for(int i = 0; i < size; i ++){
				final Component m = target.getComponent(i);
				if(m.isVisible()){
					final Dimension d = m.getPreferredSize();
					dim.width = Math.max(dim.width, d.width);

					if(firstVisibleComponent)
						firstVisibleComponent = false;
					else
						dim.height += vgap;
					dim.height += d.height;
				}
			}

			final Insets insets = target.getInsets();
			dim.width += insets.left + insets.right + hgap * 2;
			dim.height += insets.top + insets.bottom + vgap * 2;
			return dim;
		}
	}

	/**
	 * Returns the minimum dimensions needed to layout the <i>visible</i>
	 * components contained in the specified target container.
	 *
	 * @param target the container that needs to be laid out
	 * @return the minimum dimensions to lay out the
	 * subcomponents of the specified container
	 * @see #preferredLayoutSize
	 * @see java.awt.Container
	 * @see java.awt.Container#doLayout
	 */
	public Dimension minimumLayoutSize(final Container target){
		synchronized(target.getTreeLock()){
			final Dimension dim = new Dimension(0, 0);
			boolean firstVisibleComponent = true;
			final int size = target.getComponentCount();
			for(int i = 0; i < size; i ++){
				final Component m = target.getComponent(i);
				if(m.isVisible()){
					final Dimension d = m.getMinimumSize();
					dim.width = Math.max(dim.width, d.width);

					if(firstVisibleComponent)
						firstVisibleComponent = false;
					else
						dim.height += vgap;
					dim.height += d.height;
				}
			}

			final Insets insets = target.getInsets();
			dim.width += insets.left + insets.right + hgap * 2;
			dim.height += insets.top + insets.bottom + vgap * 2;
			return dim;
		}
	}

	/**
	 * Lays out the container. This method lets each <i>visible</i> component take
	 * its preferred size by reshaping the components in the target container in order to satisfy the alignment of
	 * this <code>HorizontalFlowLayout</code> object.
	 *
	 * @param target the specified component being laid out
	 * @see Container
	 * @see java.awt.Container#doLayout
	 */
	public void layoutContainer(final Container target){
		synchronized(target.getTreeLock()){
			final Insets insets = target.getInsets();
			final int maxWidth = target.getSize().width - (insets.left + insets.right + hgap * 2);
			final boolean leftToRight = target.getComponentOrientation().isLeftToRight();
			int y = insets.top + vgap;
			int x = 0;
			int columnHeight = 0;
			int start = 0;

			final int size = target.getComponentCount();
			for(int i = 0; i < size; i ++){
				final Component m = target.getComponent(i);

				if(m.isVisible()){
					final Dimension d = m.getPreferredSize();
					m.setSize(d);

					if(x == 0 || x + d.width <= maxWidth){
						x += (x > 0? hgap: 0) + d.width;
						columnHeight = Math.max(columnHeight, d.height);
					}
					else{
						moveComponents(target, insets.left + hgap, y, maxWidth - x, columnHeight, start, i, leftToRight);

						x = d.width;
						y += vgap + columnHeight;
						columnHeight = d.height;
						start = i;
					}
				}
			}

			moveComponents(target, insets.left + hgap, y, maxWidth - x, columnHeight, start, size, leftToRight);
		}
	}

	/**
	 * Centers the elements in the specified row, if there is any slack.
	 *
	 * @param target      the component which needs to be moved
	 * @param x           the x coordinate
	 * @param y           the y coordinate
	 * @param width       the width dimensions
	 * @param height      the height dimensions
	 * @param columnStart the beginning of the column
	 * @param columnEnd   the the ending of the column
	 */
	private void moveComponents(final Container target, int x, final int y, final int width, final int height,
			final int columnStart, final int columnEnd, final boolean leftToRight){
		switch(align){
			case LEFT_JUSTIFIED:
				x += (leftToRight? 0: width);
				break;

			case CENTERED:
				x += width / 2;
				break;

			case RIGHT_JUSTIFIED:
				x += (leftToRight? width: 0);
		}

		for(int i = columnStart; i < columnEnd; i ++){
			final Component m = target.getComponent(i);
			if(m.isVisible()){
				final int cy = y + (height - m.getSize().height) / 2;
				m.setLocation((leftToRight? x: target.getSize().width - x - m.getSize().width), cy);

				x += m.getSize().width + hgap;
			}
		}
	}

	/**
	 * Returns a string representation of this <code>HorizontalFlowLayout</code>
	 * object and its values.
	 *
	 * @return a string representation of this layout
	 */
	public String toString(){
		String alignment;
		switch(align){
			case LEFT_JUSTIFIED:
				alignment = "top";
				break;

			case CENTERED:
				alignment = "center";
				break;

			case RIGHT_JUSTIFIED:
			default:
				alignment = "bottom";
		}
		return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap + ",align=" + alignment + "]";
	}

}
