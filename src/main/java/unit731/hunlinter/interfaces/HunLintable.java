package unit731.hunlinter.interfaces;

import java.nio.file.Path;


public interface HunLintable{

	void loadFileInternal(final Path basePath);

	default void clearAllParsers(){
		clearAffixParser();
		clearDictionaryParser();
		clearAidParser();
		clearThesaurusParser();
		clearHyphenationParser();
		clearAutoCorrectParser();
		clearSentenceExceptionsParser();
		clearWordExceptionsParser();
		clearAutoTextParser();
	}

	void clearAffixParser();

	void clearDictionaryParser();

	void clearAidParser();

	void clearThesaurusParser();

	void clearHyphenationParser();

	void clearAutoCorrectParser();

	void clearSentenceExceptionsParser();

	void clearWordExceptionsParser();

	void clearAutoTextParser();

}
