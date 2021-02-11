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
package unit731.hunlinter.parsers.strategies;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.affix.strategies.ParsingStrategyFactory;
import unit731.hunlinter.workers.exceptions.LinterException;

import java.util.Arrays;


class ASCIIParsingStrategyTest{

	private final FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();


	@Test
	void ok(){
		String[] flags = strategy.parseFlags("ab");

		Assertions.assertEquals(Arrays.asList("a", "b"), Arrays.asList(flags));
	}

	@Test
	void empty(){
		String[] flags = strategy.parseFlags("");

		Assertions.assertNull(flags);
	}

	@Test
	void nullFlags(){
		String[] flags = strategy.parseFlags(null);

		Assertions.assertNull(flags);
	}

	@Test
	void joinFlags(){
		String[] flags = new String[]{"a", "b"};
		String continuationFlags = strategy.joinFlags(flags);

		Assertions.assertEquals("ab", continuationFlags);
	}

	@Test
	void joinFlagsWithError(){
		Throwable exception = Assertions.assertThrows(LinterException.class, () -> {
			String[] flags = new String[]{"a", "ab"};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Flag should be of length one and in US-ASCII encoding: was `ab`", exception.getMessage());
	}

	@Test
	void joinFlagsWithNoASCII(){
		Throwable exception = Assertions.assertThrows(LinterException.class, () -> {
			String[] flags = new String[]{"ŧ"};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Each flag should be in US-ASCII encoding: `ŧ`", exception.getMessage());
	}

	@Test
	void joinFlagsWithEmpty(){
		Throwable exception = Assertions.assertThrows(LinterException.class, () -> {
			String[] flags = new String[]{"a", ""};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Flag should be of length one and in US-ASCII encoding: was ``", exception.getMessage());
	}

	@Test
	void joinFlagsWithNull(){
		Throwable exception = Assertions.assertThrows(LinterException.class, () -> {
			String[] flags = new String[]{"a", null};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Flag should be of length one and in US-ASCII encoding: was `null`", exception.getMessage());
	}

	@Test
	void joinEmptyFlags(){
		String[] flags = new String[]{};
		String continuationFlags = strategy.joinFlags(flags);

		Assertions.assertTrue(continuationFlags.isEmpty());
	}

	@Test
	void joinNullFlags(){
		String continuationFlags = strategy.joinFlags((String[])null);

		Assertions.assertTrue(continuationFlags.isEmpty());
	}

}
