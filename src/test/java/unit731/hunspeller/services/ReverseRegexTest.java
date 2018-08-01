package unit731.hunspeller.services;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;


public class ReverseRegexTest{

	private final Random random = new Random();


	@Test
	public void shouldGenerateTextCorrectly(){
		String regex = "[ab]{4,6}c";

		Matcher m = Pattern.compile(regex).matcher(StringUtils.EMPTY);

		ReverseRegex generator = new ReverseRegex(regex);
		for(int i = 0; i < 100; i ++){
			String text = generator.generateRandom(random);

			Assert.assertTrue(m.reset(text).matches());
		}
	}

}
