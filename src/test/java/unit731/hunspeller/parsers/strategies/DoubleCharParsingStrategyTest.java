package unit731.hunspeller.parsers.strategies;

import unit731.hunspeller.parsers.affix.strategies.DoubleASCIIParsingStrategy;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;


public class DoubleCharParsingStrategyTest{

	private final FlagParsingStrategy strategy = new DoubleASCIIParsingStrategy();


	@Test
	public void ok(){
		String[] flags = strategy.parseFlags("abcd");

		Assert.assertEquals(Arrays.asList("ab", "cd"), Arrays.asList(flags));
	}

	@Test(expected = IllegalArgumentException.class)
	public void notOk(){
		strategy.parseFlags("abc");
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
		String[] flags = new String[]{"ab", "cd"};
		String continuationFlags = strategy.joinFlags(flags);

		Assert.assertEquals("/abcd", continuationFlags);
	}

	@Test(expected = IllegalArgumentException.class)
	public void joinFlagsWithError(){
		String[] flags = new String[]{"ab", "c"};
		strategy.joinFlags(flags);
	}

	@Test(expected = IllegalArgumentException.class)
	public void joinFlagsWithEmpty(){
	String[] flags = new String[]{"ab", ""};
		strategy.joinFlags(flags);
	}

	@Test(expected = IllegalArgumentException.class)
	public void joinFlagsWithNull(){
		String[] flags = new String[]{"ab", null};
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
