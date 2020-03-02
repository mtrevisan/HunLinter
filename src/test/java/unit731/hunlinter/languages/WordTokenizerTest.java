package unit731.hunlinter.languages;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;


public class WordTokenizerTest{

	private static final WordTokenizer tokenizer = new WordTokenizer();


	@Test
	void simple(){
		List<String> tokens = tokenizer.tokenize("I am here! well, there. So... to speak.");

		Assertions.assertEquals(Arrays.asList("I", " ", "am", " ", "here", "!", " ", "well", ",", " ", "there", ".", " ",
			"So", "…", " ", "to", " ", "speak", "."), tokens);
	}

	@Test
	void dateISO8601(){
		List<String> tokens = tokenizer.tokenize("2009-12T12:34 2009 2009-05-19 | 20090519 2009123 2009-05 2009-123 2009-222 2009-001 2009-W01-1 2009-W51-1 2009-W511 2009-W33 2009W511 2009-05-19 2009-05-19 00:00 2009-05-19 14 2009-05-19 14:31 2009-05-19 14:39:22 2009-05-19T14:39Z 2009-W21-2 2009-W21-2T01:22 2009-139 2009-05-19 14:39:22-06:00 2009-05-19 14:39:22+0600 2009-05-19 14:39:22-01 20090621T0545Z 2007-04-06T00:00 2007-04-05T24:00 2010-02-18T16:23:48.5 2010-02-18T16:23:48,444 2010-02-18T16:23:48,3-06:00 2010-02-18T16:23.4 2010-02-18T16:23,25 2010-02-18T16:23.33+0600 2010-02-18T16.23334444 2010-02-18T16,2283 2009-05-19 143922.500 2009-05-19 1439,55");

		Assertions.assertEquals(Arrays.asList("2009-12T12:34", " ", "2009", " ", "2009-05-19", " ", "|", " ", "20090519", " ", "2009123", " ", "2009-05", " ", "2009-123", " ", "2009-222", " ", "2009-001", " ", "2009-W01-1", " ", "2009-W51-1", " ", "2009-W511", " ", "2009-W33", " ", "2009W511", " ", "2009-05-19", " ", "2009-05-19 00:00", " ", "2009-05-19 14", " ", "2009-05-19 14:31", " ", "2009-05-19 14:39:22", " ", "2009-05-19T14:39Z", " ", "2009-W21-2", " ", "2009-W21-2T01:22", " ", "2009-139", " ", "2009-05-19 14:39:22-06:00", " ", "2009-05-19 14:39:22+0600", " ", "2009-05-19 14:39:22-01", " ", "20090621T0545Z", " ", "2007-04-06T00:00", " ", "2007-04-05T24:00", " ", "2010-02-18T16:23:48.5", " ", "2010-02-18T16:23:48,444", " ", "2010-02-18T16:23:48,3-06:00", " ", "2010-02-18T16:23.4", " ", "2010-02-18T16:23,25", " ", "2010-02-18T16:23.33+0600", " ", "2010-02-18T16.23334444", " ", "2010-02-18T16,2283", " ", "2009-05-19 143922.500", " ", "2009-05-19 1439,55"), tokens);
	}

	@Test
	void time(){
		List<String> tokens = tokenizer.tokenize("10:12:12 am 13:56:00 8:14:00pM");

		Assertions.assertEquals(Arrays.asList("10:12:12 am", " ", "13:56:00", " ", "8:14:00pM"), tokens);
	}

	@Test
	void email(){
		List<String> tokens = tokenizer.tokenize("Here bla@blah.com is my email.");

		Assertions.assertEquals(Arrays.asList("Here", " ", "bla@blah.com", " ", "is", " ", "my", " ", "email", "."), tokens);
	}

	@Test
	void url(){
		List<String> tokens = tokenizer.tokenize("Here www.bla.com is my url.");

		Assertions.assertEquals(Arrays.asList("Here", " ", "www.bla.com", " ", "is", " ", "my", " ", "url", "."), tokens);


		tokens = tokenizer.tokenize("Here http://www.bla.com is another url.");

		Assertions.assertEquals(Arrays.asList("Here", " ", "http://www.bla.com", " ", "is", " ", "another", " ", "url", "."), tokens);
	}

}
