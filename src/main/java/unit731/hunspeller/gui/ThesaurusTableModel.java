package unit731.hunspeller.gui;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.MessageFormat;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.parsers.thesaurus.ThesaurusEntry;


public class ThesaurusTableModel extends AbstractTableModel{

	private static final long serialVersionUID = -2584004821296780108L;

	private static final String[] COLUMN_NAMES = new String[]{"Synonym", "Meanings"};

	private static final String TAG_START = ";\">";
	private static final String TAG_END = "</body></html>";
	private static final String TAG_NEW_LINE = "<br>";
	private static final MessageFormat TAG = new MessageFormat("<html><body style=\"'white-space:nowrap'" + TAG_START + "{0}" + TAG_END);


	private List<ThesaurusEntry> synonyms;


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
				return thesaurus.getSynonym();

			case 1:
				return TAG.format(new Object[]{thesaurus.joinMeanings(TAG_NEW_LINE)});

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
		if(synonyms != null){
			try{
				final int tagEndIndex = ((String)value).indexOf(TAG_END);
				final int tagStartIndex = ((String)value).lastIndexOf(TAG_START, tagEndIndex);
				final String text = ((String)value).substring(tagStartIndex + TAG_START.length(), tagEndIndex);

				final String[] lines = StringUtils.splitByWholeSeparator(text, TAG_NEW_LINE);
				synonyms.get(rowIndex)
					.setMeanings(lines);
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
