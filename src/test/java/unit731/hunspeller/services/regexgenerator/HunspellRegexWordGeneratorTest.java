package unit731.hunspeller.services.regexgenerator;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;


public class HunspellRegexWordGeneratorTest{

	@Test
	public void oneForEach(){
		String regex = "(abc)(de)?(a)*";

		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		List<String> words = generator.generateAll(6);
words.forEach(System.out::println);

		List<String> expected = Arrays.asList(
			"(abc)",
			"(abc)(de)",
			"(abc)(a)",
			"(abc)(de)(a)",
			"(abc)(a)(a)",
			"(abc)(de)(a)(a)"
		);
		Assert.assertEquals(expected, words);
	}

	@Test
	public void allZeroOrOne(){
		String regex = "(abc)?(de)?(a)?";

		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		List<String> words = generator.generateAll(7);
words.forEach(System.out::println);

		List<String> expected = Arrays.asList(
			"(a)",
			"(de)",
			"(de)(a)",
			"(abc)",
			"(abc)(a)",
			"(abc)(de)",
			"(abc)(de)(a)"
		);
		Assert.assertEquals(expected, words);
	}

	@Test
	public void allZeroOrMore(){
		String regex = "(abc)*(de)*(a)*";

		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		List<String> words = generator.generateAll(7);
words.forEach(System.out::println);

		List<String> expected = Arrays.asList(
			"(abc)",
			"(de)",
			"(a)",
			"(abc)(abc)",
			"(abc)(de)",
			"(abc)(a)",
			"(de)(de)"
		);
		Assert.assertEquals(expected, words);
	}

}
