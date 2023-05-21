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

	@Test
	void ss(){
		String input = "desŧestadura";
		String word = orthography.correctOrthography(input);
		Assertions.assertEquals(input, word);

		input = "dessernidor";
		word = orthography.correctOrthography(input);
		Assertions.assertEquals(input, word);
	}

	@Test
	void h(){
		String input = "h";
		String word = orthography.correctOrthography(input);
		Assertions.assertEquals(input, word);
	}

	@Test
	void geminates(){
		String input = "dessestadura";
		String word = orthography.correctOrthography(input);
		Assertions.assertEquals(input, word);
		word = orthography.correctOrthography("dessestaddurra");
		Assertions.assertEquals(input, word);

		input = "dissernidor";
		word = orthography.correctOrthography(input);
		Assertions.assertEquals(input, word);
		word = orthography.correctOrthography("dissernniddor");
		Assertions.assertEquals(input, word);

		input = "disernidonne";
		word = orthography.correctOrthography(input);
		Assertions.assertEquals(input, word);
		word = orthography.correctOrthography("diserrnnidonne");
		Assertions.assertEquals(input, word);

		input = "disernidenne";
		word = orthography.correctOrthography(input);
		Assertions.assertEquals(input, word);
		word = orthography.correctOrthography("disernniddenne");
		Assertions.assertEquals(input, word);

		input = "innosente";
		word = orthography.correctOrthography(input);
		Assertions.assertEquals(input, word);

		input = "nodopie";
		word = orthography.correctOrthography(input);
		Assertions.assertEquals(input, word);
	}

}
