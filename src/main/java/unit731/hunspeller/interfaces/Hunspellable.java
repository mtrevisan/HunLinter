package unit731.hunspeller.interfaces;


public interface Hunspellable{

	void loadFileInternal(String filePath);

	void clearAffixParser();

	void clearHyphenationParser();

	void clearDictionaryParser();

	void clearAidParser();

	void clearThesaurusParser();

	void clearAutoCorrectParser();

	void clearSentenceExceptionsParser();

	void clearWordExceptionsParser();

	void clearAutoTextParser();

}
