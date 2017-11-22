package unit731.hunspeller.interfaces;

import unit731.hunspeller.parsers.dictionary.WordGenerator;


public interface Productable{

	String getWord();
	String[] getRuleFlags();
	String[] getDataFields();

	boolean containsRuleFlag(String ruleFlag);
	boolean containsDataField(String dataField);

	default boolean isPartOfSpeech(String partOfSpeech){
		return containsDataField(WordGenerator.TAG_PART_OF_SPEECH + partOfSpeech);
	}

}
