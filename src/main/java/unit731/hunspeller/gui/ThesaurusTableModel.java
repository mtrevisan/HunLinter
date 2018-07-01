package unit731.hunspeller.gui;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.table.AbstractTableModel;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.parsers.thesaurus.MeaningEntry;
import unit731.hunspeller.parsers.thesaurus.ThesaurusEntry;
import unit731.hunspeller.services.PatternService;


public class ThesaurusTableModel extends AbstractTableModel{

	private static final long serialVersionUID = -2584004821296780108L;

	private static final String[] COLUMN_NAMES = new String[]{"Synonym", "Meanings"};

	private static final String START_TAG = "<html>";
	private static final String END_TAG = "</html>";
	public static final String NEW_LINE_TAG = "<br>";

	private static final Matcher REPLACE = PatternService.matcher("^" + START_TAG + "|" + END_TAG + "$");

	private static final Pattern PATTERN_NEW_LINE = PatternService.pattern(NEW_LINE_TAG);


	private List<ThesaurusEntry> synonyms;


	public void setSynonyms(List<ThesaurusEntry> synonyms){
		this.synonyms = synonyms;

		fireTableDataChanged();
	}

	@Override
	public int getRowCount(){
		return (Objects.nonNull(synonyms)? synonyms.size(): 0);
	}

	@Override
	public int getColumnCount(){
		return COLUMN_NAMES.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex){
		if(Objects.isNull(synonyms) || synonyms.size() <= rowIndex)
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
			String text = PatternService.clear((String)aValue, REPLACE);

			List<MeaningEntry> meanings = synonyms.get(rowIndex).getMeanings();
			meanings.clear();

			String[] lines = PatternService.split(text, PATTERN_NEW_LINE);
			for(String line : lines)
				meanings.add(new MeaningEntry(line));
		}
		catch(IllegalArgumentException e){}
	}

}
