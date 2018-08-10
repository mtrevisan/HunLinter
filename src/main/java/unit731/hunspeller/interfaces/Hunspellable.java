package unit731.hunspeller.interfaces;

public interface Hunspellable{

	void loadFileInternal(String filePath);

	void dictionaryFileModified();

	void clearAffixParser();

	void clearHyphenationParser();

	void clearDictionaryParser();

	void clearAidParser();

	void clearThesaurusParser();

}
