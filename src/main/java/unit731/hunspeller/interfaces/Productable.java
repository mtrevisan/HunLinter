package unit731.hunspeller.interfaces;

import unit731.hunspeller.parsers.dictionary.WordGenerator;


public interface Productable{

	String getWord();
	String[] getRemainingRuleFlags();
	String[] getDataFields();

	boolean containsDataField(String dataField);
	boolean isCombineable();

	default String getDataFieldPrefixedBy(String typePrefix){
		String[] dataFields = getDataFields();
		if(dataFields != null)
			for(String field : dataFields)
				if(field.startsWith(typePrefix))
					return field;
		return null;
	}

	default boolean isPartOfSpeech(String partOfSpeech){
		return containsDataField(WordGenerator.TAG_PART_OF_SPEECH + partOfSpeech);
	}

}
