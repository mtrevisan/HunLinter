package unit731.hunlinter.gui.models;

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

	public static final String TAG_NEW_LINE = "<br>";
	private static final MessageFormat TAG = new MessageFormat("<html><body style=\"'white-space:nowrap'\">{0}</body></html>");


	private List<ThesaurusEntry> synonyms;


	public ThesaurusEntry getSynonymsAt(final int index){
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
		return switch(columnIndex){
			case 0 -> thesaurus.getDefinition();
			case 1 -> TAG.format(new Object[]{thesaurus.joinSynonyms(TAG_NEW_LINE)});
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
