package unit731.hunspeller.parsers.strategies;

import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.affix.strategies.ParsingStrategyFactory;


public class DoubleCharParsingStrategyTest{

	private final FlagParsingStrategy strategy = ParsingStrategyFactory.createDoubleASCIIParsingStrategy();


	@Test
	public void ok(){
		String[] flags = strategy.parseFlags("abcd");

		Assertions.assertEquals(Arrays.asList("ab", "cd"), Arrays.asList(flags));
	}

	@Test
	public void notOk(){
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			strategy.parseFlags("abc");
		});
		Assertions.assertEquals("Flag must be of length multiple of two: abc", exception.getMessage());
	}

	@Test
	public void empty(){
		String[] flags = strategy.parseFlags("");

		Assertions.assertNull(flags);
	}

	@Test
	public void nullFlags(){
		String[] flags = strategy.parseFlags(null);

		Assertions.assertNull(flags);
	}

	@Test
	public void joinFlags(){
		String[] flags = new String[]{"ab", "cd"};
		String continuationFlags = strategy.joinFlags(flags);

		Assertions.assertEquals("abcd", continuationFlags);
	}

	@Test
	public void joinFlagsWithError(){
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String[] flags = new String[]{"ab", "c"};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Each flag must be of length two: c from [ab, c]", exception.getMessage());
	}

	@Test
	public void joinFlagsWithEmpty(){
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String[] flags = new String[]{"ab", ""};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Each flag must be of length two:  from [ab, ]", exception.getMessage());
	}

	@Test
	public void joinFlagsWithNull(){
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String[] flags = new String[]{"ab", null};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Each flag must be of length two: null from [ab, null]", exception.getMessage());
	}

	@Test
	public void joinEmptyFlags(){
		String[] flags = new String[]{};
		String continuationFlags = strategy.joinFlags(flags);

		Assertions.assertTrue(continuationFlags.isEmpty());
	}

	@Test
	public void joinNullFlags(){
		String continuationFlags = strategy.joinFlags(null);

		Assertions.assertTrue(continuationFlags.isEmpty());
	}
	
}
