package unit731.hunspeller.gui;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.MessageFormat;
import java.util.List;


public class AutoCorrectTableModel extends AbstractTableModel{

	private static final long serialVersionUID = -5235919012141465022L;

	private static final String[] COLUMN_NAMES = new String[]{"Incorrect form", "Correct form"};

	private static final String TAG_START = "\";>";
	private static final String TAG_END = "</body></html>";
	private static final String TAG_NEW_LINE = "<br>";
	private static final MessageFormat TAG = new MessageFormat(
		"<html><body style=\"'white-space:nowrap'; font-family:{0}" + TAG_START + "{1}" + TAG_END);


	private List<Pair<String, String>> corrections;


	public void setCorrections(final List<Pair<String, String>> corrections){
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

		final Font font = GUIUtils.getCurrentFont();
		final Pair<String, String> correction = corrections.get(rowIndex);
		switch(columnIndex){
			case 0:
				return TAG.format(new Object[]{font.getName(), correction.getLeft()});

			case 1:
				return TAG.format(new Object[]{font.getName(), correction.getRight()});

			default:
				return null;
		}
	}

	@Override
	public String getColumnName(final int column){
		return COLUMN_NAMES[column];
	}

	@Override
	public void setValueAt(final Object value, final int rowIndex, final int columnIndex){
		if(corrections != null){
			try{
				final int tagEndIndex = ((String)value).indexOf(TAG_END);
				final int tagStartIndex = ((String)value).lastIndexOf(TAG_START, tagEndIndex);
				final String text = ((String)value).substring(tagStartIndex + TAG_START.length(), tagEndIndex);

				//TODO
				final String[] lines = StringUtils.splitByWholeSeparator(text, TAG_NEW_LINE);
				corrections.get(rowIndex)
					.setValue(lines);
			}
			catch(final IllegalArgumentException ignored){}
		}
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
