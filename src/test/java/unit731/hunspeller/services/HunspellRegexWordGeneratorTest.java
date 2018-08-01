package unit731.hunspeller.services;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;


public class HunspellRegexWordGeneratorTest{

	private final Random random = new Random();


	@Test
	public void shouldGenerateTextCorrectly(){
		String regex = "[ab]{2,6}c";

		Matcher m = Pattern.compile(regex).matcher(StringUtils.EMPTY);

		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		for(int i = 0; i < 100; i ++){
			String text = generator.generate(random, 4, 6);

			Assert.assertTrue(text.length() >= 4);
			Assert.assertTrue(text.length() <= 6);
			Assert.assertTrue(m.reset(text).matches());
		}
	}

}
