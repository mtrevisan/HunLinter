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
