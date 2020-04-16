package unit731.hunlinter.languages.vec;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.languages.WordTokenizer;

import java.util.Arrays;
import java.util.List;


class WordTokenizerVECTest{

	private static final WordTokenizer tokenizer = new WordTokenizerVEC();


	@Test
	void simpleApostrophe1(){
		List<String> tokens = tokenizer.tokenize("So' drio 'ndar da mé nòna.");

		Assertions.assertEquals(Arrays.asList("Soʼ", " ", "drio", " ", "ʼndar", " ", "da", " ", "mé", " ", "nòna", "."), tokens);
	}

	@Test
	void simpleApostrophe2(){
		List<String> tokens = tokenizer.tokenize("So'");

		Assertions.assertEquals(Arrays.asList("Soʼ"), tokens);
	}

	@Test
	void simpleApostrophe3(){
		List<String> tokens = tokenizer.tokenize("'So' drio 'ndar da mé nòna'.");

		Assertions.assertEquals(Arrays.asList("'", "Soʼ", " ", "drio", " ", "ʼndar", " ", "da", " ", "mé", " ", "nòna", "'", "."), tokens);
	}

	@Test
	void rightApostrophe1(){
		List<String> tokens = tokenizer.tokenize("Soʼ drio ʼndar da mé nòna.");

		Assertions.assertEquals(Arrays.asList("Soʼ", " ", "drio", " ", "ʼndar", " ", "da", " ", "mé", " ", "nòna", "."), tokens);
	}

	@Test
	void rightApostrophe2(){
		List<String> tokens = tokenizer.tokenize("ʼndar");

		Assertions.assertEquals(Arrays.asList("ʼndar"), tokens);
	}

	@Test
	void rightApostrophe3(){
		List<String> tokens = tokenizer.tokenize("'Soʼ drio ʼndar da mé nòna'.");

		Assertions.assertEquals(Arrays.asList("'", "Soʼ", " ", "drio", " ", "ʼndar", " ", "da", " ", "mé", " ", "nòna", "'", "."), tokens);
	}

}
