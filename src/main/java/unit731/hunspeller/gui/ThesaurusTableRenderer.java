package unit731.hunspeller.gui;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import org.apache.commons.lang3.StringUtils;


public class ThesaurusTableRenderer extends JLabel implements TableCellRenderer{

	private static final long serialVersionUID = -7581282504915833642L;

	private static final String TAG_NEW_LINE = "<br>";


	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
		String text = String.valueOf(value);
		setText(text);

		int lines = StringUtils.countMatches(text, TAG_NEW_LINE);
		int height = getPreferredSize().height * Math.max(lines, 1) + 4;
		try{
			table.setRowHeight(row, height);
		}
		catch(ArrayIndexOutOfBoundsException ignored){}

		return this;
	}

}
