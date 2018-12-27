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
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			strategy.parseFlags("abc");
		});
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
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String[] flags = new String[]{"ab", "c"};
			strategy.joinFlags(flags);
		});
	}

	@Test
	public void joinFlagsWithEmpty(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String[] flags = new String[]{"ab", ""};
			strategy.joinFlags(flags);
		});
	}

	@Test
	public void joinFlagsWithNull(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String[] flags = new String[]{"ab", null};
			strategy.joinFlags(flags);
		});
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
