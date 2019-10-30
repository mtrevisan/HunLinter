package unit731.hunspeller.gui;

import java.awt.*;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;


public class TableRenderer extends JLabel implements TableCellRenderer{

	private static final long serialVersionUID = -7581282504915833642L;


	@Override
	public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column){
		final String text = String.valueOf(value);
		setText(text);

		final Font currentFont = GUIUtils.getCurrentFont();
		setFont(currentFont);
		table.setFont(currentFont);

		if(row >= 0 && table.convertRowIndexToModel(row) < table.getModel().getRowCount()){
			try{
				table.setRowHeight(row, getPreferredSize().height + 4);
			}
			catch(final ArrayIndexOutOfBoundsException ignored){
			}
		}

		return this;
	}

}