package unit731.hunspeller.languages.vec;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;


public class OrthographyVEC extends Orthography{

	private static class SingletonHelper{
		private static final OrthographyVEC INSTANCE = new OrthographyVEC();
	}


	public static synchronized Orthography getInstance(){
		return SingletonHelper.INSTANCE;
	}

	@Override
	public String correctOrthography(String word){
		//apply (and correct) stress
		word = StringUtils.replaceAll(word, "a\\\\", "à");
		word = StringUtils.replaceAll(word, "e\\\\", "è");
		word = StringUtils.replaceAll(word, "o\\\\", "ò");
		word = StringUtils.replaceAll(word, "e/", "é");
		word = StringUtils.replaceAll(word, "ì", "í");
		word = StringUtils.replaceAll(word, "i/", "í");
		word = StringUtils.replaceAll(word, "o/", "ó");
		word = StringUtils.replaceAll(word, "ù", "ú");
		word = StringUtils.replaceAll(word, "u/", "ú");

		//correct h occurrences not after d, f, k, j, l, n, p, s, t, x
		word = word.replaceAll("(^|[^dfkjlnpstx])h", "$1");
		//rewrite characters
		word = StringUtils.replaceAll(word, "dh", "đ");
		word = StringUtils.replaceAll(word, "jh", "ɉ");
		word = StringUtils.replaceAll(word, "lh", "ƚ");
		word = StringUtils.replaceAll(word, "nh", "ñ");
		word = StringUtils.replaceAll(word, "th", "ŧ");

		//correct mb/mp occurrences into nb/np
		word = word.replaceAll("m([bp])", "n$1");
		//correct i occurrences into j at the beginning of a word followed by a vowel and between vowels, correcting also the converse
		word = word.replace("^j(?=[^aeiouàèéíòóúh])", "i");
		word = word.replace("^i(?=[aeiouàèéíòóú])", "j");
		word = word.replaceAll("([aeiouàèéíòóú])i(?=[aeiouàèéíòóú])", "$1j");
		word = word.replaceAll("j(?=[^aeiouàèéíòóúh])", "i");
		//correct lh occurrences into l not at the beginning of a word and not between vowels
		word = word.replace("^ƚ(?=[^ʼ'aeiouàèéíòóú])", "l");
		word = word.replaceAll("([^ʼ'aeiouàèéíòóú])ƚ(?=[aeiouàèéíòóú])|([aeiouàèéíòóú])ƚ(?=[^aeiouàèéíòóú])", "$1l");
		//correct fh occurrences into f not before vowel
		word = word.replaceAll("fh(?=[^aeiouàèéíòóú])", "f");
		//correct x occurrences into s prior to c, f, k, p, t
		//correct s occurrences into x prior to m, n, ñ, b, d, g, j, ɉ, s, v, r, l
		word = word.replaceAll("x(?=[cfkpt])", "s");
		word = word.replaceAll("s(?=([mnñbdgjɉsvrlŧ]|jh))", "x");

		//correct morphologic error
		word = word.replace("([cjñ])i([aeiou])", "$1$2");

		word = eliminateConsonantGeminates(word);

		word = correctApostrophes(word);

		return word;
	}

	private String eliminateConsonantGeminates(String word){
		String prefix = null;
		boolean prefixRemoved = false;
		if(word.startsWith("dexskon")){
			word = word.substring(3);

			prefix = "dex";
			prefixRemoved = true;
		}

		word = word.replaceAll("([^aeiou]){1}\\1+", "$1");
		
		if(prefixRemoved)
			word = prefix + word;

		return word;
	}

	@Override
	public boolean[] getSyllabationErrors(List<String> syllabes){
		int size = syllabes.size();
		boolean[] errors = new boolean[size];
		for(int i = 0; i < size; i ++){
			String syllabe = syllabes.get(i);
			errors[i] = (!syllabe.contains(APOSTROPHE) && !syllabe.contains("'") && !HyphenationParser.HYPHEN.equals(syllabe)
				&& Word.getLastVowelIndex(syllabe) < 0);
		}
		return errors;
	}

}
