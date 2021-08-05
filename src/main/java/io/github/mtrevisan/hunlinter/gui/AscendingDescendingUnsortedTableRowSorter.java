/**
 * Copyright (c) 2019-2021 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.gui;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.util.List;


class AscendingDescendingUnsortedTableRowSorter<M extends TableModel> extends TableRowSorter<M>{

	public AscendingDescendingUnsortedTableRowSorter(final M model){
		super(model);
	}

	@Override
	public void toggleSortOrder(final int column){
		final List<? extends SortKey> sortKeys = getSortKeys();
		if(!sortKeys.isEmpty() && sortKeys.get(0).getSortOrder() == SortOrder.DESCENDING)
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
