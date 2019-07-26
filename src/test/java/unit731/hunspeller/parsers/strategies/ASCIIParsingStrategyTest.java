package unit731.hunspeller.parsers.strategies;

import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.affix.strategies.ParsingStrategyFactory;


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
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String[] flags = new String[]{"a", "ab"};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Flag must be of length 1 and in US-ASCII encoding: 'ab'", exception.getMessage());
	}

	@Test
	void joinFlagsWithNoASCII(){
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String[] flags = new String[]{"ŧ"};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Flag must be of length 1 and in US-ASCII encoding: 'ŧ'", exception.getMessage());
	}

	@Test
	void joinFlagsWithEmpty(){
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String[] flags = new String[]{"a", ""};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Flag must be of length 1 and in US-ASCII encoding: ''", exception.getMessage());
	}

	@Test
	void joinFlagsWithNull(){
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String[] flags = new String[]{"a", null};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Flag must be of length 1 and in US-ASCII encoding: 'null'", exception.getMessage());
	}

	@Test
	void joinEmptyFlags(){
		String[] flags = new String[]{};
		String continuationFlags = strategy.joinFlags(flags);

		Assertions.assertTrue(continuationFlags.isEmpty());
	}

	@Test
	void joinNullFlags(){
		String continuationFlags = strategy.joinFlags(null);

		Assertions.assertTrue(continuationFlags.isEmpty());
	}
	
}
