package unit731.hunspeller.services.regexgenerator;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;


public class HunspellRegexWordGeneratorTest{

	@Test
	public void shouldGenerateAllWords(){
		String regex = "[abc]c[de]?";

		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		boolean infinite = generator.isInfinite();
		List<String> words = generator.generateAll(10);

		Assert.assertFalse(infinite);
		Assert.assertEquals(9l, words.size());
	}

	@Test
	public void shouldGenerateInfiniteWords(){
		String regex = "[abc]c[de]*";

		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		boolean infinite = generator.isInfinite();

		Assert.assertTrue(infinite);
	}

	@Test
	public void shouldGenerateEmptyWord(){
		String regex = "a?b?c?";

		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		boolean infinite = generator.isInfinite();

		Assert.assertFalse(infinite);
	}

	@Test
	public void shouldNotGenerateEmptyWord(){
		String regex = "a?b?c?";

		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		boolean infinite = generator.isInfinite();

		Assert.assertFalse(infinite);
	}

}
