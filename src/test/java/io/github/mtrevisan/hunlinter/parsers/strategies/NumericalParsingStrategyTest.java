/**
 * Copyright (c) 2019-2021 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.parsers.strategies;

import io.github.mtrevisan.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import io.github.mtrevisan.hunlinter.parsers.affix.strategies.ParsingStrategyFactory;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;


class NumericalParsingStrategyTest{

	private final FlagParsingStrategy strategy = ParsingStrategyFactory.createNumericalParsingStrategy();


	@Test
	void ok(){
		Character[] flags = strategy.parseFlags("1,2");

		Assertions.assertEquals(Arrays.asList("1", "2"), Arrays.asList(flags));
	}

	@Test
	void notOk1(){
		Throwable exception = Assertions.assertThrows(LinterException.class, () -> strategy.parseFlags("ab"));
		Assertions.assertEquals("Flag must be an integer number: `ab`", exception.getMessage());
	}

	@Test
	void notOk2(){
		Throwable exception = Assertions.assertThrows(LinterException.class, () -> strategy.parseFlags("1.2"));
		Assertions.assertEquals("Flag must be an integer number: `1.2`", exception.getMessage());
	}

	@Test
	void empty(){
		Character[] flags = strategy.parseFlags("");

		Assertions.assertNull(flags);
	}

	@Test
	void nullFlags(){
		Character[] flags = strategy.parseFlags(null);

		Assertions.assertNull(flags);
	}

	@Test
	void joinFlags(){
		Character[] flags = new String[]{"1", "2"};
		String continuationFlags = strategy.joinFlags(flags);

		Assertions.assertEquals("1,2", continuationFlags);
	}

	@Test
	void joinFlagsWithError1(){
		Throwable exception = Assertions.assertThrows(LinterException.class, () -> {
			Character[] flags = new String[]{"1", "c"};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Flag must be an integer number: `c`", exception.getMessage());
	}

	@Test
	void joinFlagsWithError2(){
		Throwable exception = Assertions.assertThrows(LinterException.class, () -> {
			Character[] flags = new String[]{"1", "1.2"};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Flag must be an integer number: `1.2`", exception.getMessage());
	}

	@Test
	void joinFlagsWithEmpty(){
		Throwable exception = Assertions.assertThrows(LinterException.class, () -> {
			Character[] flags = new String[]{"1", ""};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Flag must be an integer number: ``", exception.getMessage());
	}

	@Test
	void joinFlagsWithNull(){
		Throwable exception = Assertions.assertThrows(LinterException.class, () -> {
			Character[] flags = new String[]{"ab", null};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Flag must be an integer number: `ab`", exception.getMessage());
	}

	@Test
	void joinEmptyFlags(){
		Character[] flags = new String[]{};
		String continuationFlags = strategy.joinFlags(flags);

		Assertions.assertTrue(continuationFlags.isEmpty());
	}

	@Test
	void joinNullFlags(){
		String continuationFlags = strategy.joinFlags((Character[])null);

		Assertions.assertTrue(continuationFlags.isEmpty());
	}

}
