package unit731.hunspeller.interfaces;

import java.nio.file.Path;


public interface Hunspellable{

	void loadFileInternal(final Path basePath);

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
