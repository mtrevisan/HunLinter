package unit731.hunspeller.languages;

import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;


@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Orthography{

	private static class SingletonHelper{
		private static final Orthography INSTANCE = new Orthography();
	}


	public static Orthography getInstance(){
		return SingletonHelper.INSTANCE;
	}

	public String correctOrthography(String word){
		//apply stress
		word = StringUtils.replaceAll(word, "a\\\\", "à");
		word = StringUtils.replaceAll(word, "e\\\\", "è");
		word = StringUtils.replaceAll(word, "i\\\\", "ì");
		word = StringUtils.replaceAll(word, "o\\\\", "ò");
		word = StringUtils.replaceAll(word, "u\\\\", "ù");
		word = StringUtils.replaceAll(word, "a/", "á");
		word = StringUtils.replaceAll(word, "e/", "é");
		word = StringUtils.replaceAll(word, "i/", "í");
		word = StringUtils.replaceAll(word, "o/", "ó");
		word = StringUtils.replaceAll(word, "u/", "ú");
		word = StringUtils.replaceAll(word, "a:", "ä");
		word = StringUtils.replaceAll(word, "e:", "ë");
		word = StringUtils.replaceAll(word, "i:", "ï");
		word = StringUtils.replaceAll(word, "o:", "ö");
		word = StringUtils.replaceAll(word, "u:", "ü");

		word = correctApostrophes(word);

		return word;
	}

	protected String correctApostrophes(String word){
		word = StringUtils.replace(word, "'", "ʼ");
		word = StringUtils.replace(word, "‘", "ʼ");
		word = StringUtils.replace(word, "’", "ʼ");
		return word;
	}

	public boolean[] getSyllabationErrors(List<String> syllabes){
		return new boolean[syllabes.size()];
}

}
