package unit731.hunspeller.services.regexgenerator;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;


public class HunspellRegexWordGeneratorTest{

	@Test
	public void shouldGenerateAllWords(){
		String regex = "(abc)(de)?(a)*";

		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		List<String> words = generator.generateAll(6);
words.forEach(stem -> System.out.println(stem));

		List<String> expected = Arrays.asList(
			"abc",
			"abca",
			"abcaa",
			"abcde",
			"abcdea",
			"abcdeaa"
		);
		Assert.assertEquals(expected, words);
	}

}
