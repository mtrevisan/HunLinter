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

		List<String> expected = Arrays.asList(
			"(abc)",
			"(abc)(a)",
			"(abc)(de)",
			"(abc)(a)(a)",
			"(abc)(de)(a)",
			"(abc)(a)(a)(a)"
		);
		Assert.assertEquals(expected, words);
	}

}
