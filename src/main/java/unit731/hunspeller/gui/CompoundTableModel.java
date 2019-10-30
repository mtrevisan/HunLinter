package unit731.hunspeller.gui;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import unit731.hunspeller.parsers.vos.Production;


public class CompoundTableModel extends AbstractTableModel implements HunspellerTableModel<Production>{

	private static final long serialVersionUID = -7276635232728680738L;

	private static final String[] COLUMN_NAMES = new String[]{"Production", "Morphological fields"};


	private List<Production> productions;


	@Override
	public void setProductions(final List<Production> productions){
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
	public Object getValueAt(final int rowIndex, final int columnIndex){
		if(productions == null || productions.size() <= rowIndex)
			return null;

		final Production production = productions.get(rowIndex);
		switch(columnIndex){
			case 0:
				return production.getWord();

			case 1:
				return production.getMorphologicalFields();

			default:
				return null;
		}
	}

	@Override
	public String getColumnName(final int column){
		return COLUMN_NAMES[column];
	}

	@Override
	public void clear(){
		setProductions(null);
	}

	@SuppressWarnings("unused")
	private void writeObject(final ObjectOutputStream os) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

	@SuppressWarnings("unused")
	private void readObject(final ObjectInputStream is) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

}
