package io.github.mtrevisan.hunlinter.languages.vec;

import io.github.mtrevisan.hunlinter.languages.Orthography;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class OrthographyVECTest{

	private Orthography orthography = OrthographyVEC.getInstance();


	@Test
	void trueIUVowel(){
		String input = "kaia";
		String word = orthography.correctOrthography(input);
		Assertions.assertEquals(input, word);

		input = "noialtri";
		word = orthography.correctOrthography(input);
		Assertions.assertEquals(input, word);

		input = "vüialtri";
		word = orthography.correctOrthography(input);
		Assertions.assertEquals(input, word);

		input = "argüio";
		word = orthography.correctOrthography(input);
		Assertions.assertEquals(input, word);
	}

	@Test
	void ciVowel(){
		String input = "cia";
		String word = orthography.correctOrthography(input);
		Assertions.assertEquals(input, word);
	}

	@Test
	void fh(){
		String input = "fhersora";
		String word = orthography.correctOrthography(input);
		Assertions.assertEquals(input, word);
	}

}
