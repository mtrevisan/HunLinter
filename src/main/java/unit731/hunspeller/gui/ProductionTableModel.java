package unit731.hunspeller.gui;

import java.util.List;
import java.util.Objects;
import javax.swing.table.AbstractTableModel;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.parsers.dictionary.AffixEntry;
import unit731.hunspeller.parsers.dictionary.RuleProductionEntry;


public class ProductionTableModel extends AbstractTableModel{

	private static final long serialVersionUID = -7276635232728680738L;

	private static final String[] COLUMN_NAMES = new String[]{"Production", "Morphological fields", "Rule 1", "Rule 2", "Rule 3"};


	private List<RuleProductionEntry> productions;


	public void setProductions(List<RuleProductionEntry> productions){
		this.productions = productions;

		fireTableDataChanged();
	}

	@Override
	public int getRowCount(){
		return (Objects.nonNull(productions)? productions.size(): 0);
	}

	@Override
	public int getColumnCount(){
		return COLUMN_NAMES.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex){
		if(Objects.isNull(productions) || productions.size() <= rowIndex)
			return null;

		RuleProductionEntry production = productions.get(rowIndex);
		List<AffixEntry> rules = production.getAppliedRules();
		int rulesSize = (Objects.nonNull(rules)? rules.size(): 0);
		switch(columnIndex){
			case 0:
				return production.getWord();

			case 1:
				String[] morphologicalFields = production.getMorphologicalFields();
				return (Objects.nonNull(morphologicalFields)? String.join(StringUtils.SPACE, morphologicalFields): StringUtils.EMPTY);

			case 2:
				return (rulesSize > 0? rules.get(0): null);

			case 3:
				return (rulesSize > 1? rules.get(1): null);

			case 4:
				return (rulesSize > 2? rules.get(2): null);

			default:
				return null;
		}
	}

	@Override
	public String getColumnName(int column){
		return COLUMN_NAMES[column];
	}

}
