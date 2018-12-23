package unit731.hunspeller.parsers.strategies;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.affix.strategies.ParsingStrategyFactory;


public class ASCIIParsingStrategyTest{

	private final FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();


	@Test
	public void ok(){
		String[] flags = strategy.parseFlags("ab");

		Assert.assertEquals(Arrays.asList("a", "b"), Arrays.asList(flags));
	}

	@Test
	public void empty(){
		String[] flags = strategy.parseFlags("");

		Assert.assertNull(flags);
	}

	@Test
	public void nullFlags(){
		String[] flags = strategy.parseFlags(null);

		Assert.assertNull(flags);
	}

	@Test
	public void joinFlags(){
		String[] flags = new String[]{"a", "b"};
		String continuationFlags = strategy.joinFlags(flags);

		Assert.assertEquals("ab", continuationFlags);
	}

	@Test(expected = IllegalArgumentException.class)
	public void joinFlagsWithError(){
		String[] flags = new String[]{"a", "ab"};
		strategy.joinFlags(flags);
	}

	@Test(expected = IllegalArgumentException.class)
	public void joinFlagsWithNoASCII(){
		String[] flags = new String[]{"ŧ"};
		strategy.joinFlags(flags);
	}

	@Test(expected = IllegalArgumentException.class)
	public void joinFlagsWithEmpty(){
		String[] flags = new String[]{"a", ""};
		strategy.joinFlags(flags);
	}

	@Test(expected = IllegalArgumentException.class)
	public void joinFlagsWithNull(){
		String[] flags = new String[]{"a", null};
		strategy.joinFlags(flags);
	}

	@Test
	public void joinEmptyFlags(){
		String[] flags = new String[]{};
		String continuationFlags = strategy.joinFlags(flags);

		Assert.assertTrue(continuationFlags.isEmpty());
	}

	@Test
	public void joinNullFlags(){
		String continuationFlags = strategy.joinFlags(null);

		Assert.assertTrue(continuationFlags.isEmpty());
	}
	
}
