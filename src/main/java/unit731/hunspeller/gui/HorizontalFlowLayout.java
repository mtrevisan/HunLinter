package unit731.hunspeller.gui;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;


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
 * <li>{@link FlowLayout#LEFT LEFT}
 * <li>{@link FlowLayout#CENTER CENTER}
 * <li>{@link FlowLayout#RIGHT RIGHT}
 * </ul>
 * <p>
 */
//http://www.camick.com/java/source/WrapLayout.java
public class HorizontalFlowLayout extends FlowLayout{

	/** Constructs a new <code>WrapLayout</code> with a left alignment and a default 5-unit horizontal and vertical gap. */
	public HorizontalFlowLayout(){
		super();
	}

	/**
	 * Constructs a new <code>FlowLayout</code> with the specified alignment and a default 5-unit horizontal and vertical gap.
	 * The value of the alignment argument must be one of <code>WrapLayout</code>, <code>WrapLayout</code>,
	 * or <code>WrapLayout</code>.
	 *
	 * @param align	The alignment value
	 */
	public HorizontalFlowLayout(final int align){
		super(align);
	}

	/**
	 * Creates a new flow layout manager with the indicated alignment
	 * and the indicated horizontal and vertical gaps.
	 * <p>
	 * The value of the alignment argument must be one of
	 * <code>HorizontalFlowLayout.TOP</code>, <code>HorizontalFlowLayout.BOTTOM</code>,
	 * or <code>HorizontalFlowLayout.CENTER</code>.
	 *
	 * @param align The alignment value
	 * @param hgap The horizontal gap between components and between the components and the
	 * 				borders of the <code>Container</code>
	 * @param vgap	The vertical gap between components and between the components and the
	 * 				borders of the <code>Container</code>
	 */
	public HorizontalFlowLayout(final int align, final int hgap, final int vgap){
		super(align, hgap, vgap);
	}

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
	@Override
	public Dimension preferredLayoutSize(final Container target){
		return layoutSize(target, Component::getPreferredSize);
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
	@Override
	public Dimension minimumLayoutSize(final Container target){
		final Dimension minimum = layoutSize(target, Component::getMinimumSize);
		minimum.width -= getHgap() + 1;
		return minimum;
	}

	private Dimension layoutSize(final Container target, final Function<Component, Dimension> sizeSupplier){
		synchronized(target.getTreeLock()){
			//each row must fit with the width allocated to the container
			final int targetWidth = target.getSize().width;

			final Insets insets = target.getInsets();
			final int horizontalInsetsAndGap = insets.left + insets.right + getHgap() * 2;
			//when the container height is 0, the preferred height of the container has not yet been calculated
			//so lets ask for the maximum
			final int maxWidth = (targetWidth > 0? targetWidth: Integer.MAX_VALUE) - horizontalInsetsAndGap;

			//fit components into the allowed height
			final Dimension finalDimension = new Dimension(0, 0);
			int rowWidth = 0;
			int rowHeight = 0;
			for(int i = 0; i < target.getComponentCount(); i ++){
				final Component m = target.getComponent(i);
				if(m.isVisible()){
					final Dimension d = sizeSupplier.apply(m);
					//can't add the component to current row: start a new row
					if(rowWidth + d.width > maxWidth){
						addRow(d, rowWidth, rowHeight);

						rowWidth = 0;
						rowHeight = 0;
					}
					//add an horizontal gap for all components after the first
					rowWidth += (rowWidth > 0? getHgap(): 0) + d.width;
					rowHeight = Math.max(rowHeight, d.height);

//					final Dimension d = sizeSupplier.apply(m);
//					finalDimension.width = Math.max(finalDimension.width, d.width);
//					finalDimension.height += (i > 0? vgap: 0) + d.height;
//					finalDimension.height = Math.max(finalDimension.height, d.height);
//					finalDimension.width += (i > 0? hgap: 0) + d.width;
				}
			}

			addRow(finalDimension, rowWidth, rowHeight);

			finalDimension.width += horizontalInsetsAndGap;
			finalDimension.height += insets.top + insets.bottom + getVgap() * 2;

			//when using a scroll pane or the DecoratedLookAndFeel we need to make sure the preferred size
			//is less than the size of the target containter so shrinking the container size works
			//correctly: removing the horizontal gap is an easy way to do this
			final Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
			if(scrollPane != null)
				finalDimension.width -= getHgap() + 1;
			return finalDimension;

//			finalDimension.width += horizontalInsetsAndGap;
//			finalDimension.height += insets.top + insets.bottom + vgap * 2;
//			return finalDimension;
		}
	}

	/*
	 *  A new row has been completed. Use the dimensions of this row to update the preferred size for the container.
	 *
	 *  @param dimension	Update the width and height when appropriate
	 *  @param rowWidth	The width of the row to add
	 *  @param rowHeight	The height of the row to add
	 */
	private void addRow(final Dimension dimension, final int rowWidth, final int rowHeight){
		dimension.width = Math.max(dimension.width, rowWidth);
		if(dimension.height > 0)
			dimension.height += getVgap();
		dimension.height += rowHeight;
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
//	@Override
//	public void layoutContainer(final Container target){
//		synchronized(target.getTreeLock()){
//			final Insets insets = target.getInsets();
//			final int maxWidth = target.getSize().width - (insets.left + insets.right + hgap * 2);
//			final boolean leftToRight = target.getComponentOrientation().isLeftToRight();
//			int y = insets.top + vgap;
//			int x = 0;
//			int rowHeight = 0;
//			int start = 0;
//
//			final int size = target.getComponentCount();
//			for(int i = 0; i < size; i ++){
//				final Component m = target.getComponent(i);
//				if(m.isVisible()){
//					final Dimension d = m.getPreferredSize();
//					m.setSize(d);
//
//					if(x == 0 || x + d.width <= maxWidth){
//						x += (x > 0? hgap: 0) + d.width;
//						rowHeight = Math.max(rowHeight, d.height);
//					}
//					else{
//						moveComponents(target, insets.left + hgap, y, maxWidth - x, rowHeight, start, i, leftToRight);
//
//						x = d.width;
//						y += vgap + rowHeight;
//						rowHeight = d.height;
//						start = i;
//					}
//				}
//			}
//
//			moveComponents(target, insets.left + hgap, y, maxWidth - x, rowHeight, start, size, leftToRight);
//		}
//	}

	/**
	 * Centers the elements in the specified row, if there is any slack.
	 *
	 * @param target	The component which needs to be moved
	 * @param x	The x coordinate
	 * @param y	The y coordinate
	 * @param width	The width dimensions
	 * @param height	The height dimensions
	 * @param columnStart	The beginning of the column
	 * @param columnEnd	The the ending of the column
	 */
//	private void moveComponents(final Container target, int x, final int y, final int width, final int height,
//			final int columnStart, final int columnEnd, final boolean leftToRight){
//		x += startingX(leftToRight, width);
//		for(int i = columnStart; i < columnEnd; i ++){
//			final Component m = target.getComponent(i);
//			if(m.isVisible()){
//				final Dimension size = m.getSize();
//				final int cy = y + (height - size.height) / 2;
//				m.setLocation((leftToRight? x: target.getSize().width - x - size.width), cy);
//
//				x += size.width + getHgap();
//			}
//		}
//	}

//	private int startingX(final boolean leftToRight, final int width){
//		int x = 0;
//		switch(getAlignment()){
//			case LEFT:
//				x = (leftToRight? 0: width);
//				break;
//
//			case CENTER:
//				x = width / 2;
//				break;
//
//			case RIGHT:
//				x = (leftToRight? width: 0);
//		}
//		return x;
//	}

	/**
	 * Returns a string representation of this <code>HorizontalFlowLayout</code>
	 * object and its values.
	 *
	 * @return a string representation of this layout
	 */
//	public String toString(){
//		String alignment;
//		switch(align){
//			case LEFT:
//				alignment = "top";
//				break;
//
//			case CENTER:
//				alignment = "center";
//				break;
//
//			case RIGHT:
//			default:
//				alignment = "bottom";
//		}
//		return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap + ",align=" + alignment + "]";
//	}

}
