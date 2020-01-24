package unit731.hunlinter.gui;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.MessageFormat;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import unit731.hunlinter.parsers.thesaurus.ThesaurusEntry;


public class ThesaurusTableModel extends AbstractTableModel{

	private static final long serialVersionUID = -2584004821296780108L;

	private static final String[] COLUMN_NAMES = new String[]{"Definition", "Synonyms"};

	private static final String TAG_START = ";\">";
	private static final String TAG_END = "</body></html>";
	public static final String TAG_NEW_LINE = "<br>";
	private static final MessageFormat TAG = new MessageFormat("<html><body style=\"'white-space:nowrap'" + TAG_START + "{0}" + TAG_END);


	private List<ThesaurusEntry> synonyms;


	public ThesaurusEntry getSynonymAt(final int index){
		return synonyms.get(index);
	}

	public void setSynonyms(final List<ThesaurusEntry> synonyms){
		this.synonyms = synonyms;

		fireTableDataChanged();
	}

	@Override
	public int getRowCount(){
		return (synonyms != null? synonyms.size(): 0);
	}

	@Override
	public int getColumnCount(){
		return COLUMN_NAMES.length;
	}

	@Override
	public Object getValueAt(final int rowIndex, final int columnIndex){
		if(synonyms == null || synonyms.size() <= rowIndex)
			return null;

		final ThesaurusEntry thesaurus = synonyms.get(rowIndex);
		switch(columnIndex){
			case 0:
				return thesaurus.getDefinition();

			case 1:
				return TAG.format(new Object[]{thesaurus.joinSynonyms(TAG_NEW_LINE)});

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
