package unit731.hunspeller.gui;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import unit731.hunspeller.parsers.dictionary.vos.AffixEntry;
import unit731.hunspeller.parsers.dictionary.vos.Production;


public class ProductionTableModel extends AbstractTableModel implements HunspellerTableModel<Production>{

	private static final long serialVersionUID = -7276635232728680738L;

	private static final String[] COLUMN_NAMES = new String[]{"Production", "Morphological fields", "Rule 1", "Rule 2", "Rule 3"};


	private List<Production> productions;


	@Override
	public void setProductions(List<Production> productions){
		this.productions = productions;

		fireTableDataChanged();
	}

	@Override
	public int getRowCount(){
		return (productions != null? productions.size(): 0);
	}

	@Override
	public int getColumnCount(){
		return COLUMN_NAMES.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex){
		if(productions == null || productions.size() <= rowIndex)
			return null;

		Production production = productions.get(rowIndex);
		List<AffixEntry> rules = production.getAppliedRules();
		int rulesSize = (rules != null? rules.size(): 0);
		switch(columnIndex){
			case 0:
				return production.getWord();

			case 1:
				return production.getMorphologicalFields();

			case 2:
				return (rules != null && rulesSize > 0? rules.get(0): null);

			case 3:
				return (rules != null && rulesSize > 1? rules.get(1): null);

			case 4:
				return (rules != null && rulesSize > 2? rules.get(2): null);

			default:
				return null;
		}
	}

	@Override
	public String getColumnName(int column){
		return COLUMN_NAMES[column];
	}

	private void writeObject(ObjectOutputStream os) throws IOException{
		throw new NotSerializableException(ProductionTableModel.class.getName());
	}

	private void readObject(ObjectInputStream is) throws IOException{
		throw new NotSerializableException(ProductionTableModel.class.getName());
	}

}
