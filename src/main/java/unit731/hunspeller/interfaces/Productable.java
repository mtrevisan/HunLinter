package unit731.hunspeller.interfaces;

import java.util.Objects;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;


public interface Productable{

	String getWord();
	String[] getRuleFlags();
	String[] getDataFields();

	boolean containsRuleFlag(String ruleFlag);
	boolean containsDataField(String dataField);
	boolean isCombineable();
	String toStringBasic(FlagParsingStrategy strategy);

	default String getDataFieldPrefixedBy(String typePrefix){
		String[] dataFields = getDataFields();
		if(Objects.nonNull(dataFields))
			for(String field : dataFields)
				if(field.startsWith(typePrefix))
					return field;
		return null;
	}

	default boolean isPartOfSpeech(String partOfSpeech){
		return containsDataField(WordGenerator.TAG_PART_OF_SPEECH + partOfSpeech);
	}

}
