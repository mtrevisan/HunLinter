package unit731.hunlinter.gui.models;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import javax.swing.table.AbstractTableModel;

import unit731.hunlinter.parsers.vos.Inflection;


public class InflectionTableModel extends AbstractTableModel implements HunLinterTableModelInterface<Inflection>{

	private static final long serialVersionUID = -7276635232728680738L;

	private static final String[] COLUMN_NAMES = new String[]{"Inflection", "Morphological fields", "Applied rule 1", "Applied rule 2", "Applied rule 3"};


	private List<Inflection> inflections;


	@Override
	public void setInflections(final List<Inflection> inflections){
		this.inflections = inflections;

		fireTableDataChanged();
	}

	@Override
	public int getRowCount(){
		return (inflections != null? inflections.size(): 0);
	}

	@Override
	public int getColumnCount(){
		return COLUMN_NAMES.length;
	}

	@Override
	public Object getValueAt(final int rowIndex, final int columnIndex){
		if(inflections == null || inflections.size() <= rowIndex)
			return null;

		final Inflection inflection = inflections.get(rowIndex);
		return switch(columnIndex){
			case 0 -> inflection.getWord();
			case 1 -> inflection.getMorphologicalFields();
			case 2 -> inflection.getAppliedRule(0);
			case 3 -> inflection.getAppliedRule(1);
			case 4 -> inflection.getAppliedRule(2);
			default -> null;
		};
	}

	@Override
	public String getColumnName(final int column){
		return COLUMN_NAMES[column];
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
