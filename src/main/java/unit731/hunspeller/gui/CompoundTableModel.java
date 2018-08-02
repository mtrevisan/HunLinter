package unit731.hunspeller.gui;

import java.util.List;
import javax.swing.table.AbstractTableModel;


public class CompoundTableModel extends AbstractTableModel implements HunspellerTableModel<String>{

	private static final long serialVersionUID = -7276635232728680738L;

	private static final String[] COLUMN_NAMES = new String[]{"Production"};


	private List<String> productions;


	@Override
	public void setProductions(List<String> productions){
		this.productions = productions;

		fireTableDataChanged();
	}

	@Override
	public int getRowCount(){
		return (productions != null? productions.size(): 0);
	}

	@Override
	public int getColumnCount(){
		return COLUMN_NAMES.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex){
		if(productions == null || productions.size() <= rowIndex)
			return null;

		return productions.get(rowIndex);
	}

	@Override
	public String getColumnName(int column){
		return COLUMN_NAMES[column];
	}

	@Override
	public void clear(){
		setProductions(null);
	}

}
