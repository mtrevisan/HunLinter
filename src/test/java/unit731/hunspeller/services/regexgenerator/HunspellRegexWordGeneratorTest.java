package unit731.hunspeller.services.regexgenerator;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;


public class HunspellRegexWordGeneratorTest{

	@Test
	public void shouldGenerateRandomWord(){
		String regex = "[ab]{2,6}c";

		Matcher m = Pattern.compile(regex).matcher(StringUtils.EMPTY);

		Random random = new Random();
		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		for(int i = 0; i < 100; i ++){
			String word = generator.generate(random);

			Assert.assertTrue(m.reset(word).matches());
		}
	}

	@Test
	public void shouldGenerateAllWords(){
		String regex = "[abc]c[de]?";

		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		long wordCount = generator.wordCount();

		Assert.assertEquals(9l, wordCount);
	}

	@Test(expected = StackOverflowError.class)
	public void shouldGenerateInfiniteWords(){
		String regex = "[abc]c[de]*";

		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		generator.wordCount();
	}

}
