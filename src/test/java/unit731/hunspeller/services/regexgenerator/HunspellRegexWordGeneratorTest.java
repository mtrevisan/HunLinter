package unit731.hunspeller.services.regexgenerator;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;


public class HunspellRegexWordGeneratorTest{

	@Test
	public void shouldGenerateAllWords(){
		String regex = "[abc]c[de]?";

		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		List<String> words = generator.generateAll(10);

		Assert.assertEquals(9l, words.size());
	}

	@Test
	public void shouldNotGenerateEmptyWord(){
		String regex = "a?b?c?";

		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		List<String> words = generator.generateAll(10);

		Assert.assertFalse(words.isEmpty());
	}

}
