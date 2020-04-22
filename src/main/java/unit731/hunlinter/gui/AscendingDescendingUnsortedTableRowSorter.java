package unit731.hunlinter.gui;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.util.List;


public class AscendingDescendingUnsortedTableRowSorter<M extends TableModel> extends TableRowSorter<M>{

	public AscendingDescendingUnsortedTableRowSorter(final M model){
		super(model);
	}

	@Override
	public void toggleSortOrder(final int column){
		final List<? extends SortKey> sortKeys = getSortKeys();
		if(sortKeys.size() > 0 && sortKeys.get(0).getSortOrder() == SortOrder.DESCENDING)
			setSortKeys(null);
		else
			super.toggleSortOrder(column);
	}

	/**
	 * {@inheritDoc}
	 *
	 * Overridden to cope with the annoying IOOBE
	 */
	@Override
	public int convertRowIndexToModel(final int index){
		try{
			return super.convertRowIndexToModel(index);
		}
		catch(final IndexOutOfBoundsException ignored){
			return -1;
		}
	}

}
