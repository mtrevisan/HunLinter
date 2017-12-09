package unit731.hunspeller.interfaces;

import unit731.hunspeller.parsers.dictionary.WordGenerator;


public interface Productable{

	String getWord();
	String[] getRuleFlags();
	String[] getDataFields();

	boolean containsRuleFlag(String ruleFlag);
	boolean containsDataField(String dataField);

	default String getDataField(String type){
		String[] dataFields = getDataFields();
		if(dataFields != null)
			for(String field : dataFields)
				if(field.startsWith(type))
					return field;
		return null;
	}

	default boolean isPartOfSpeech(String partOfSpeech){
		return containsDataField(WordGenerator.TAG_PART_OF_SPEECH + partOfSpeech);
	}

}
