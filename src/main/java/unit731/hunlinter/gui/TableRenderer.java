package unit731.hunlinter.gui;

import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;


public class TableRenderer extends JLabel implements TableCellRenderer{

	private static final long serialVersionUID = -7581282504915833642L;

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
		setFont(GUIUtils.getCurrentFont());
		setText(value != null? String.valueOf(value): StringUtils.SPACE);

		try{
			table.setRowHeight(row, getPreferredSize().height + 4);
		}
		catch(final ArrayIndexOutOfBoundsException ignored){}

		//draw border on error
		setBorder(column == 0 && errors.contains(row)?
			BorderFactory.createMatteBorder(1, 1, 1, 1, Color.RED): null);

		return this;
	}

}
