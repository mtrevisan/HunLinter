package unit731.hunspeller.parsers.strategies;

import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.affix.strategies.ParsingStrategyFactory;


public class NumericalParsingStrategyTest{

	private final FlagParsingStrategy strategy = ParsingStrategyFactory.createNumericalParsingStrategy();


	@Test
	public void ok(){
		String[] flags = strategy.parseFlags("1,2");

		Assertions.assertEquals(Arrays.asList("1", "2"), Arrays.asList(flags));
	}

	@Test
	public void notOk1(){
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			strategy.parseFlags("ab");
		});
		Assertions.assertEquals("Flag must be an integer number: ab from ab", exception.getMessage());
	}

	@Test
	public void notOk2(){
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			strategy.parseFlags("1.2");
		});
		Assertions.assertEquals("Flag must be an integer number: 1.2 from 1.2", exception.getMessage());
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
		String[] flags = new String[]{"1", "2"};
		String continuationFlags = strategy.joinFlags(flags);

		Assertions.assertEquals("1,2", continuationFlags);
	}

	@Test
	public void joinFlagsWithError1(){
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String[] flags = new String[]{"1", "c"};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Each flag must be an integer number: c from [1, c]", exception.getMessage());
	}

	@Test
	public void joinFlagsWithError2(){
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String[] flags = new String[]{"1", "1.2"};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Each flag must be an integer number: 1.2 from [1, 1.2]", exception.getMessage());
	}

	@Test
	public void joinFlagsWithEmpty(){
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String[] flags = new String[]{"1", ""};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Each flag must be an integer number:  from [1, ]", exception.getMessage());
	}

	@Test
	public void joinFlagsWithNull(){
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String[] flags = new String[]{"ab", null};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Each flag must be an integer number: ab from [ab, null]", exception.getMessage());
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
