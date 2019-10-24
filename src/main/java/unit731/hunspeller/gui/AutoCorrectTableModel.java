package unit731.hunspeller.gui;

import unit731.hunspeller.parsers.autocorrect.CorrectionEntry;

import javax.swing.table.AbstractTableModel;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;


public class AutoCorrectTableModel extends AbstractTableModel{

	private static final long serialVersionUID = -5235919012141465022L;

	private static final String[] COLUMN_NAMES = new String[]{"Incorrect form", "Correct form"};


	private List<CorrectionEntry> corrections;


	public void setCorrections(final List<CorrectionEntry> corrections){
		this.corrections = corrections;

		fireTableDataChanged();
	}

	@Override
	public int getRowCount(){
		return (corrections != null? corrections.size(): 0);
	}

	@Override
	public int getColumnCount(){
		return COLUMN_NAMES.length;
	}

	@Override
	public Object getValueAt(final int rowIndex, final int columnIndex){
		if(corrections == null || corrections.size() <= rowIndex)
			return null;

		final CorrectionEntry correction = corrections.get(rowIndex);
		switch(columnIndex){
			case 0:
				return correction.getIncorrectForm();

			case 1:
				return correction.getCorrectForm();

			default:
				return null;
		}
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
