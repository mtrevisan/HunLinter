package unit731.hunspeller.interfaces;

import java.util.Objects;
import unit731.hunspeller.parsers.dictionary.WordGenerator;


public interface Productable{

	String getWord();
	void setWord(String word);
	String[] getContinuationFlags();
	String[] getMorphologicalFields();

	boolean containsContinuationFlag(String ... continuationFlags);
	boolean containsMorphologicalField(String morphologicalField);
	boolean isCombineable();

	default String getMorphologicalFieldPrefixedBy(String typePrefix){
		String[] morphologicalFields = getMorphologicalFields();
		if(Objects.nonNull(morphologicalFields))
			for(String field : morphologicalFields)
				if(field.startsWith(typePrefix))
					return field;
		return null;
	}

	default boolean isPartOfSpeech(String partOfSpeech){
		return containsMorphologicalField(WordGenerator.TAG_PART_OF_SPEECH + partOfSpeech);
	}

}
