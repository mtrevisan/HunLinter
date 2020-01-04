package unit731.hunspeller.languages.vec;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunspeller.languages.WordTokenizer;

import java.util.Arrays;
import java.util.List;


public class WordTokenizerVECTest{

	private static WordTokenizer tokenizer = new WordTokenizerVEC();


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
