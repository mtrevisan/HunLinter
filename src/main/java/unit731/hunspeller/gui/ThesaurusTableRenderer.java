package unit731.hunspeller.gui;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;


public class ThesaurusTableRenderer extends JLabel implements TableCellRenderer{

	private static final long serialVersionUID = -7581282504915833642L;


	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
		String text = String.valueOf(value);
		setText(text);

		try{
			table.setRowHeight(row, getPreferredSize().height + 4);
		}
		catch(ArrayIndexOutOfBoundsException ignored){}

		return this;
	}

}
