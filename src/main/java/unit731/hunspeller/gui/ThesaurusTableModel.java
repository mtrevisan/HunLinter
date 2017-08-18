package unit731.hunspeller.gui;

import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.resources.MeaningEntry;
import unit731.hunspeller.resources.ThesaurusEntry;


public class ThesaurusTableModel extends AbstractTableModel{

	private static final long serialVersionUID = -2584004821296780108L;

	private static final String[] COLUMN_NAMES = new String[]{"Synonym", "Meanings"};

	private static final String START_TAG = "<html>";
	private static final String END_TAG = "</html>";
	public static final String NEW_LINE_TAG = "<br>";

	private static final String REPLACE_REGEX = "^" + START_TAG + "|" + END_TAG + "$";


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
		List<MeaningEntry> meanings = thesaurus.getMeanings();
		switch(columnIndex){
			case 0:
				return thesaurus.getSynonym();

			case 1:
				return START_TAG + StringUtils.join(meanings, NEW_LINE_TAG) + END_TAG;

			default:
				return null;
		}
	}

	@Override
	public String getColumnName(int column){
		return COLUMN_NAMES[column];
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex){
		try{
			String text = ((String)aValue).replaceAll(REPLACE_REGEX, StringUtils.EMPTY);

			List<MeaningEntry> meanings = synonyms.get(rowIndex).getMeanings();
			meanings.clear();

			String[] lines = text.split(NEW_LINE_TAG);
			for(String line : lines)
				meanings.add(new MeaningEntry(line));
		}
		catch(IllegalArgumentException e){}
	}

}
