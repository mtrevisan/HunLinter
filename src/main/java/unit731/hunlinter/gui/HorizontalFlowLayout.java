package unit731.hunlinter.gui;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
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
 *
 * @see <a href="http://www.camick.com/java/source/WrapLayout.java">WrapLayout.java</a>
 */
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
	 * Creates a new flow layout manager with the indicated alignment and the indicated horizontal and vertical gaps.<p>
	 * The value of the alignment argument must be one of <code>HorizontalFlowLayout.TOP</code>,
	 * <code>HorizontalFlowLayout.BOTTOM</code>, or <code>HorizontalFlowLayout.CENTER</code>.
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
	 * Returns the preferred dimensions for this layout given the <i>visible</i> components in the specified target container.
	 *
	 * @param target	The container that needs to be laid out
	 * @return	The preferred dimensions to lay out the subcomponents of the specified container
	 * @see #minimumLayoutSize
	 * @see Container
	 * @see Container#getPreferredSize
	 */
	@Override
	public Dimension preferredLayoutSize(final Container target){
		return layoutSize(target, Component::getPreferredSize);
	}

	/**
	 * Returns the minimum dimensions needed to layout the <i>visible</i> components contained in the specified target container.
	 *
	 * @param target	The container that needs to be laid out
	 * @return	The minimum dimensions to lay out the subcomponents of the specified container
	 * @see #preferredLayoutSize
	 * @see Container
	 * @see Container#doLayout
	 */
	@Override
	public Dimension minimumLayoutSize(final Container target){
		final Dimension dimension = layoutSize(target, Component::getMinimumSize);
		dimension.width -= getHgap() + 1;
		return dimension;
	}

	private Dimension layoutSize(final Container target, final Function<Component, Dimension> sizeSupplier){
		synchronized(target.getTreeLock()){
			//each row must fit with the width allocated to the container
			Container container = target;
			while(container.getSize().width == 0 && container.getParent() != null)
				container = container.getParent();
			final int targetWidth = container.getSize().width;

			final Insets insets = target.getInsets();
			final int horizontalInsetsAndGap = insets.left + insets.right + getHgap() * 2;
			//when the container height is 0, the preferred height of the container has not yet been calculated
			//so lets ask for the maximum
			final int maxWidth = (targetWidth > 0? targetWidth - horizontalInsetsAndGap: Integer.MAX_VALUE);

			//fit components into the allowed width
			final Dimension finalDimension = new Dimension(0, 0);
			int rowWidth = 0;
			int rowHeight = 0;
			for(int i = 0; i < target.getComponentCount(); i ++){
				final Component m = target.getComponent(i);
				if(m.isVisible()){
					final Dimension d = sizeSupplier.apply(m);
					//can't add the component to current row: start a new row
					if(rowWidth + d.width > maxWidth){
						addRow(finalDimension, rowWidth, rowHeight);

						rowWidth = 0;
						rowHeight = 0;
					}
					//add an horizontal gap for all components after the first
					rowWidth += (rowWidth > 0? getHgap(): 0) + d.width;
					rowHeight = Math.max(rowHeight, d.height);
				}
			}

			addRow(finalDimension, rowWidth, rowHeight);

			finalDimension.width += horizontalInsetsAndGap;
			finalDimension.height += insets.top + insets.bottom + getVgap() * 2;

			//when using a scroll pane or the DecoratedLookAndFeel we need to make sure the preferred size
			//is less than the size of the target containter so shrinking the container size works
			//correctly: removing the horizontal gap is an easy way to do this
			final Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
			if(scrollPane != null && target.isValid())
				finalDimension.width -= getHgap() + 1;
			return finalDimension;
		}
	}

	/**
	 *  A new row has been completed. Use the dimensions of this row to update the preferred size for the container.
	 *
	 *  @param dimension	Update the width and height when appropriate
	 *  @param rowWidth	The width of the row to add
	 *  @param rowHeight	The height of the row to add
	 */
	private void addRow(final Dimension dimension, final int rowWidth, final int rowHeight){
		dimension.width = Math.max(dimension.width, rowWidth);
		dimension.height += (dimension.height > 0? getVgap(): 0) + rowHeight;
	}

}
