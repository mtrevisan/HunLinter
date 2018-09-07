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
System.out.println(generator);
		List<String> words = generator.generateAll(6);
words.forEach(System.out::println);

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
