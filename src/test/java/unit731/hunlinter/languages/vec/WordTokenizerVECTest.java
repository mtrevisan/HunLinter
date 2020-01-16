package unit731.hunlinter.languages.vec;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.languages.WordTokenizer;

import java.util.Arrays;
import java.util.List;


public class WordTokenizerVECTest{

	private static final WordTokenizer tokenizer = new WordTokenizerVEC();


	@Test
	void simpleApostrophe(){
		List<String> tokens = tokenizer.tokenize("So' drio 'ndar da mé nòna.");

		Assertions.assertEquals(Arrays.asList("Soʼ", " ", "drio", " ", "ʼndar", " ", "da", " ", "mé", " ", "nòna", "."), tokens);
	}

	@Test
	void rightApostrophe(){
		List<String> tokens = tokenizer.tokenize("Soʼ drio ʼndar da mé nòna.");

		Assertions.assertEquals(Arrays.asList("Soʼ", " ", "drio", " ", "ʼndar", " ", "da", " ", "mé", " ", "nòna", "."), tokens);
	}

}
