package unit731.hunlinter.interfaces;

import java.nio.file.Path;


public interface HunLintable{

	void loadFileInternal(final Path basePath);

	default void clearAllParsers(){
		clearAffixParser();
		clearHyphenationParser();
		clearDictionaryParser();
		clearAidParser();
		clearThesaurusParser();
		clearAutoCorrectParser();
		clearSentenceExceptionsParser();
		clearWordExceptionsParser();
		clearAutoTextParser();
	}

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
