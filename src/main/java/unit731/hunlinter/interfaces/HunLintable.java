package unit731.hunlinter.interfaces;

import java.nio.file.Path;


public interface HunLintable{

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
