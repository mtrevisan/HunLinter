package unit731.hunspeller.gui;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.parsers.thesaurus.dtos.ThesaurusEntry;


public class ThesaurusTableModel extends AbstractTableModel{

	private static final long serialVersionUID = -2584004821296780108L;

	private static final String[] COLUMN_NAMES = new String[]{"Synonym", "Meanings"};

	private static final String TAG_START = "<html><body style='white-space:nowrap'>";
	private static final String TAG_END = "</body></html>";
	private static final String TAG_NEW_LINE = "<br>";


	private List<ThesaurusEntry> synonyms;


	public void setSynonyms(List<ThesaurusEntry> synonyms){
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
	public Object getValueAt(int rowIndex, int columnIndex){
		if(synonyms == null || synonyms.size() <= rowIndex)
			return null;

		ThesaurusEntry thesaurus = synonyms.get(rowIndex);
		switch(columnIndex){
			case 0:
				return thesaurus.getSynonym();

			case 1:
				return TAG_START + thesaurus.joinMeanings(TAG_NEW_LINE) + TAG_END;

			default:
				return null;
		}
	}

	@Override
	public String getColumnName(int column){
		return COLUMN_NAMES[column];
	}

	@Override
	public void setValueAt(Object value, int rowIndex, int columnIndex){
		if(synonyms != null){
			try{
				String text = StringUtils.replace((String)value, TAG_START, StringUtils.EMPTY);
				text = StringUtils.replace(text, TAG_END, StringUtils.EMPTY);

				String[] lines = StringUtils.splitByWholeSeparator(text, TAG_NEW_LINE);
				synonyms.get(rowIndex)
					.setMeanings(lines);
			}
			catch(IllegalArgumentException ignored){}
		}
	}

	private void writeObject(ObjectOutputStream os) throws IOException{
		throw new NotSerializableException(ThesaurusTableModel.class.getName());
	}

	private void readObject(ObjectInputStream is) throws IOException{
		throw new NotSerializableException(ThesaurusTableModel.class.getName());
	}

}
