package unit731.hunspeller.parsers.strategies;

import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.affix.strategies.ParsingStrategyFactory;


public class ASCIIParsingStrategyTest{

	private final FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();


	@Test
	public void ok(){
		String[] flags = strategy.parseFlags("ab");

		Assertions.assertEquals(Arrays.asList("a", "b"), Arrays.asList(flags));
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
		String[] flags = new String[]{"a", "b"};
		String continuationFlags = strategy.joinFlags(flags);

		Assertions.assertEquals("ab", continuationFlags);
	}

	@Test
	public void joinFlagsWithError(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String[] flags = new String[]{"a", "ab"};
			strategy.joinFlags(flags);
		});
	}

	@Test
	public void joinFlagsWithNoASCII(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String[] flags = new String[]{"ŧ"};
			strategy.joinFlags(flags);
		});
	}

	@Test
	public void joinFlagsWithEmpty(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String[] flags = new String[]{"a", ""};
			strategy.joinFlags(flags);
		});
	}

	@Test
	public void joinFlagsWithNull(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String[] flags = new String[]{"a", null};
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
