package unit731.hunspeller.languages;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;


public class WordTokenizerTest{

	private static WordTokenizer tokenizer = new WordTokenizer();


	@Test
	void simple(){
		List<String> tokens = tokenizer.tokenize("I am here! well, there. So... to speak.");

		Assertions.assertEquals(Arrays.asList("I", " ", "am", " ", "here", "!", " ", "well", ",", " ", "there", ".", " ",
			"So", "…", " ", "to", " ", "speak", "."), tokens);
	}

	@Test
	void url(){
		List<String> tokens = tokenizer.tokenize("Here www.bla.com is my url.");

		Assertions.assertEquals(Arrays.asList("Here", " ", "www.bla.com", " ", "is", " ", "my", " ", "url", "."), tokens);


		tokens = tokenizer.tokenize("Here http://www.bla.com is another url.");

		Assertions.assertEquals(Arrays.asList("Here", " ", "http://www.bla.com", " ", "is", " ", "another", " ", "url", "."), tokens);
	}

	@Test
	void mail(){
		List<String> tokens = tokenizer.tokenize("Here bla@blah.com is my email.");

		Assertions.assertEquals(Arrays.asList("Here", " ", "bla@blah.com", " ", "is", " ", "my", " ", "email", "."), tokens);
	}

}
