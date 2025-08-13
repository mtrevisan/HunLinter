/**
 * Copyright (c) 2019-2022 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.languages.vec;

import io.github.mtrevisan.hunlinter.languages.WordTokenizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;


class WordTokenizerVECTest{

	private static final WordTokenizer tokenizer = new WordTokenizerVEC();


	@Test
	void simpleApostrophe1(){
		List<String> tokens = tokenizer.tokenize("So' drio 'ndar da mé nòna.");

		Assertions.assertEquals(Arrays.asList("So’", " ", "drio", " ", "’ndar", " ", "da", " ", "mé", " ", "nòna", "."), tokens);
	}

	@Test
	void simpleApostrophe2(){
		List<String> tokens = tokenizer.tokenize("So'");

		Assertions.assertEquals(Arrays.asList("So’"), tokens);
	}

	@Test
	void simpleApostrophe3(){
		List<String> tokens = tokenizer.tokenize("'So' drio 'ndar da mé nòna'.");

		Assertions.assertEquals(Arrays.asList("'", "So’", " ", "drio", " ", "’ndar", " ", "da", " ", "mé", " ", "nòna", "'", "."), tokens);
	}

	@Test
	void rightApostrophe1(){
		List<String> tokens = tokenizer.tokenize("So’ drio ’ndar da mé nòna.");

		Assertions.assertEquals(Arrays.asList("So’", " ", "drio", " ", "’ndar", " ", "da", " ", "mé", " ", "nòna", "."), tokens);
	}

	@Test
	void rightApostrophe2(){
		List<String> tokens = tokenizer.tokenize("’ndar");

		Assertions.assertEquals(Arrays.asList("’ndar"), tokens);
	}

	@Test
	void rightApostrophe3(){
		List<String> tokens = tokenizer.tokenize("'So’ drio ’ndar da mé nòna'.");

		Assertions.assertEquals(Arrays.asList("'", "So’", " ", "drio", " ", "’ndar", " ", "da", " ", "mé", " ", "nòna", "'", "."), tokens);
	}

}
