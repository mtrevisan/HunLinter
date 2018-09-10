package unit731.hunspeller.services.regexgenerator;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;


public class HunspellRegexWordGeneratorTest{

	@Test
	public void allOne(){
		String regex = "(abc)(de)(a)";

		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		List<List<String>> words = generator.generateAll(1, 6);

		List<List<String>> expected = Arrays.asList(
			Arrays.asList("abc", "de", "a")
		);
		Assert.assertEquals(expected, words);
	}

	@Test
	public void oneForEach(){
		String regex = "(abc)(de)?(a)*";

		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		List<List<String>> words = generator.generateAll(1, 6);

		List<List<String>> expected = Arrays.asList(
			Arrays.asList("abc"),
			Arrays.asList("abc", "de"),
			Arrays.asList("abc", "a"),
			Arrays.asList("abc", "de", "a"),
			Arrays.asList("abc", "a", "a"),
			Arrays.asList("abc", "de", "a", "a")
		);
		Assert.assertEquals(expected, words);
	}

	@Test
	public void allZeroOrOne(){
		String regex = "(abc)?(de)?(a)?";

		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		List<List<String>> words = generator.generateAll(1, 7);

		List<List<String>> expected = Arrays.asList(
			Arrays.asList("a"),
			Arrays.asList("de"),
			Arrays.asList("de", "a"),
			Arrays.asList("abc"),
			Arrays.asList("abc", "a"),
			Arrays.asList("abc", "de"),
			Arrays.asList("abc", "de", "a")
		);
		Assert.assertEquals(expected, words);
	}

	@Test
	public void allZeroOrOneWithZeroMinimum(){
		String regex = "(abc)?(de)?(a)?";

		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		List<List<String>> words = generator.generateAll(0, 7);

		List<List<String>> expected = Arrays.asList(
			Arrays.asList(),
			Arrays.asList("a"),
			Arrays.asList("de"),
			Arrays.asList("de", "a"),
			Arrays.asList("abc"),
			Arrays.asList("abc", "a"),
			Arrays.asList("abc", "de")
		);
		Assert.assertEquals(expected, words);
	}

	@Test
	public void allZeroOrOneWithTwoMinimum(){
		String regex = "(abc)?(de)?(a)?";

		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		List<List<String>> words = generator.generateAll(2, 7);

		List<List<String>> expected = Arrays.asList(
			Arrays.asList("de", "a"),
			Arrays.asList("abc", "a"),
			Arrays.asList("abc", "de"),
			Arrays.asList("abc", "de", "a")
		);
		Assert.assertEquals(expected, words);
	}

	@Test
	public void allZeroOrMore(){
		String regex = "(abc)*(de)*(a)*";

		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		List<List<String>> words = generator.generateAll(1, 7);

		List<List<String>> expected = Arrays.asList(
			Arrays.asList("abc"),
			Arrays.asList("de"),
			Arrays.asList("a"),
			Arrays.asList("abc", "abc"),
			Arrays.asList("abc", "de"),
			Arrays.asList("abc", "a"),
			Arrays.asList("de", "de")
		);
		Assert.assertEquals(expected, words);
	}

}
