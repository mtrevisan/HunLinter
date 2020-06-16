/**
 * Copyright (c) 2019-2020 Mauro Trevisan
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
package unit731.hunlinter.services.regexgenerator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class HunSpellRegexWordGeneratorTest{

	@Test
	void allOne(){
		String[] regex = new String[]{"abc", "de", "a"};

		HunSpellRegexWordGenerator generator = new HunSpellRegexWordGenerator(regex);
		List<List<String>> words = generator.generateAll(1, 6);

		List<List<String>> expected = Collections.singletonList(
			Arrays.asList("abc", "de", "a")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	void oneForEach(){
		String[] regex = new String[]{"abc", "de", "?", "a", "*"};

		HunSpellRegexWordGenerator generator = new HunSpellRegexWordGenerator(regex);
		List<List<String>> words = generator.generateAll(1, 6);

		List<List<String>> expected = Arrays.asList(
			Collections.singletonList("abc"),
			Arrays.asList("abc", "de"),
			Arrays.asList("abc", "a"),
			Arrays.asList("abc", "de", "a"),
			Arrays.asList("abc", "a", "a"),
			Arrays.asList("abc", "de", "a", "a")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	void allZeroOrOne(){
		String[] regex = new String[]{"abc", "?", "de", "?", "a", "?"};

		HunSpellRegexWordGenerator generator = new HunSpellRegexWordGenerator(regex);
		List<List<String>> words = generator.generateAll(1, 7);

		List<List<String>> expected = Arrays.asList(
			Collections.singletonList("a"),
			Collections.singletonList("de"),
			Arrays.asList("de", "a"),
			Collections.singletonList("abc"),
			Arrays.asList("abc", "a"),
			Arrays.asList("abc", "de"),
			Arrays.asList("abc", "de", "a")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	void allZeroOrOneWithZeroMinimum(){
		String[] regex = new String[]{"abc", "?", "de", "?", "a", "?"};

		HunSpellRegexWordGenerator generator = new HunSpellRegexWordGenerator(regex);
		List<List<String>> words = generator.generateAll(0, 7);

		List<List<String>> expected = Arrays.asList(
			Collections.emptyList(),
			Collections.singletonList("a"),
			Collections.singletonList("de"),
			Arrays.asList("de", "a"),
			Collections.singletonList("abc"),
			Arrays.asList("abc", "a"),
			Arrays.asList("abc", "de")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	void allZeroOrOneWithTwoMinimum(){
		String[] regex = new String[]{"abc", "?", "de", "?", "a", "?"};

		HunSpellRegexWordGenerator generator = new HunSpellRegexWordGenerator(regex);
		List<List<String>> words = generator.generateAll(2, 7);

		List<List<String>> expected = Arrays.asList(
			Arrays.asList("de", "a"),
			Arrays.asList("abc", "a"),
			Arrays.asList("abc", "de"),
			Arrays.asList("abc", "de", "a")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	void allZeroOrMore(){
		String[] regex = new String[]{"abc", "*", "de", "*", "a", "*"};

		HunSpellRegexWordGenerator generator = new HunSpellRegexWordGenerator(regex);
		List<List<String>> words = generator.generateAll(1, 7);

		List<List<String>> expected = Arrays.asList(
			Collections.singletonList("abc"),
			Collections.singletonList("de"),
			Collections.singletonList("a"),
			Arrays.asList("abc", "abc"),
			Arrays.asList("abc", "de"),
			Arrays.asList("abc", "a"),
			Arrays.asList("de", "de")
		);
		Assertions.assertEquals(expected, words);
	}

}
