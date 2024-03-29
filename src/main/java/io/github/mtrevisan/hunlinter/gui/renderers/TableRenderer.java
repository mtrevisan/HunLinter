/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.gui.renderers;

import com.carrotsearch.hppcrt.IntSet;
import com.carrotsearch.hppcrt.sets.IntHashSet;
import io.github.mtrevisan.hunlinter.gui.FontHelper;
import io.github.mtrevisan.hunlinter.gui.models.ThesaurusTableModel;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.io.Serial;


public class TableRenderer extends DefaultTableCellRenderer{

	@Serial
	private static final long serialVersionUID = -7581282504915833642L;

	private static final MatteBorder BORDER_ERROR = BorderFactory.createMatteBorder(1, 1, 1, 1, Color.RED);

	private final IntSet errors = new IntHashSet(0);


	public final void setErrorOnRow(final int line){
		errors.add(line);
	}

	public final void clearErrors(){
		errors.clear();
	}

	@Override
	public final Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
			final boolean hasFocus, final int row, final int column){
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		setText(value != null? String.valueOf(value): null);

		adjustRowHeight(table, row);

		//draw border on error
		setBorder(column == 0 && errors.contains(row)? BORDER_ERROR: null);

		return this;
	}

	private void adjustRowHeight(final JTable table, final int rowIndex){
		final int fontHeight = getFontMetrics(FontHelper.getCurrentFont()).getHeight();
		final String value = (String)table.getValueAt(rowIndex, 1);
		if(value != null){
			final int cellLines = StringUtils.countMatches(value.toString(), ThesaurusTableModel.TAG_NEW_LINE) + 1;
			final int rowHeight = fontHeight * cellLines + 4;
			if(rowHeight != table.getRowHeight(rowIndex))
				table.setRowHeight(rowIndex, rowHeight);
		}
	}

}
