package unit731.hunlinter.gui.renderers;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;


public class TableRenderer extends DefaultTableCellRenderer{

	private static final long serialVersionUID = -7581282504915833642L;

	private static final MatteBorder BORDER_ERROR = BorderFactory.createMatteBorder(1, 1, 1, 1, Color.RED);

	private final Set<Integer> errors = new HashSet<>();


	public void setErrorOnRow(final int line){
		errors.add(line);
	}

	public void clearErrors(){
		errors.clear();
	}

	@Override
	public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
			final boolean hasFocus, final int row, final int column){
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		setText(value != null? String.valueOf(value): null);

//		adjustRowHeight(table, row);

		//draw border on error
		setBorder(column == 0 && errors.contains(row)? BORDER_ERROR: null);

		return this;
	}

	public void adjustRowHeight(final JTable table, final int rowIndex){
		//FIXME
		//DON'T change the state of a table in your renderer, ever.
		//Instead, listen to changes on the tableModel - that's the only time a rowHeight might change - and update
		//the height/s as appropriate
		//https://www.codeproject.com/Messages/2665651/Resizing-dynamically-JTable-rows-with-setRowHeight.aspx
		final int rowHeight = getPreferredSize().height + 4;
		if(rowHeight != table.getRowHeight(rowIndex)){
			try{
				table.setRowHeight(rowIndex, rowHeight);
			}
			catch(final Exception e){
				e.printStackTrace();
			}
		}
	}

}
