package unit731.hunspeller.parsers.strategies;

import unit731.hunspeller.parsers.affix.strategies.UTF8ParsingStrategy;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;


public class UTF8ParsingStrategyTest{

	private final FlagParsingStrategy strategy = new UTF8ParsingStrategy();


	@Test
	public void ok(){
		String[] flags = strategy.parseFlags("èŧ");

		Assert.assertEquals(Arrays.asList("è", "ŧ"), Arrays.asList(flags));
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
		String[] flags = new String[]{"è", "ŧ"};
		String continuationFlags = strategy.joinFlags(flags);

		Assert.assertEquals("/èŧ", continuationFlags);
	}

	@Test(expected = IllegalArgumentException.class)
	public void joinFlagsWithError(){
		String[] flags = new String[]{"è", "aŧ"};
		strategy.joinFlags(flags);
	}

	@Test(expected = IllegalArgumentException.class)
	public void joinFlagsWithNoUTF8(){
		String[] flags = new String[]{"\\x{FFFD}"};
		strategy.joinFlags(flags);
	}

	@Test(expected = IllegalArgumentException.class)
	public void joinFlagsWithEmpty(){
		String[] flags = new String[]{"è", ""};
		strategy.joinFlags(flags);
	}

	@Test(expected = IllegalArgumentException.class)
	public void joinFlagsWithNull(){
		String[] flags = new String[]{"ŧ", null};
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
