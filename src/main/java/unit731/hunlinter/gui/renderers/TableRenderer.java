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

		//FIXME
		try{
			table.setRowHeight(row, getPreferredSize().height + 4);
		}
		catch(final Exception e){
			e.printStackTrace();
		}

		//draw border on error
		setBorder(column == 0 && errors.contains(row)? BORDER_ERROR: null);

		return this;
	}

}
