package unit731.hunspeller.parsers.strategies;

import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.affix.strategies.ParsingStrategyFactory;


class DoubleCharParsingStrategyTest{

	private final FlagParsingStrategy strategy = ParsingStrategyFactory.createDoubleASCIIParsingStrategy();


	@Test
	void ok(){
		String[] flags = strategy.parseFlags("abcd");

		Assertions.assertEquals(Arrays.asList("ab", "cd"), Arrays.asList(flags));
	}

	@Test
	void notOk(){
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> strategy.parseFlags("abc"));
		Assertions.assertEquals("Flag must be of length multiple of two: abc", exception.getMessage());
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
		String[] flags = new String[]{"ab", "cd"};
		String continuationFlags = strategy.joinFlags(flags);

		Assertions.assertEquals("abcd", continuationFlags);
	}

	@Test
	void joinFlagsWithError(){
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String[] flags = new String[]{"ab", "c"};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Flag must be of length two: c from [ab, c]", exception.getMessage());
	}

	@Test
	void joinFlagsWithEmpty(){
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String[] flags = new String[]{"ab", ""};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Flag must be of length two:  from [ab, ]", exception.getMessage());
	}

	@Test
	void joinFlagsWithNull(){
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String[] flags = new String[]{"ab", null};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Flag must be of length two: null from [ab, null]", exception.getMessage());
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
