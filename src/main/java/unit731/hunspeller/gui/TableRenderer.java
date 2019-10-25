package unit731.hunspeller.gui;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;


public class TableRenderer extends JLabel implements TableCellRenderer{

	private static final long serialVersionUID = -7581282504915833642L;


	@Override
	public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column){
		final String text = String.valueOf(value);
		setText(text);
		setFont(GUIUtils.getCurrentFont());

		if(row >= 0 && row < table.getRowCount()){
			try{
				table.setRowHeight(row, getPreferredSize().height + 4);
			}
			catch(final ArrayIndexOutOfBoundsException ignored){
			}
		}

		return this;
	}

}
