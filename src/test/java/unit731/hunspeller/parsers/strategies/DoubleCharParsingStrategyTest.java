package unit731.hunspeller.parsers.strategies;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;


public class DoubleCharParsingStrategyTest{

	private final FlagParsingStrategy strategy = new DoubleCharParsingStrategy();


	@Test
	public void ok(){
		String[] flags = strategy.parseRuleFlags("abcd");

		Assert.assertEquals(Arrays.asList("ab", "cd"), Arrays.asList(flags));
	}

	@Test(expected = IllegalArgumentException.class)
	public void notOk(){
		strategy.parseRuleFlags("abc");
	}

	@Test
	public void empty(){
		String[] flags = strategy.parseRuleFlags("");

		Assert.assertEquals(0, flags.length);
	}

	@Test
	public void nullFlags(){
		String[] flags = strategy.parseRuleFlags(null);

		Assert.assertEquals(0, flags.length);
	}

	@Test
	public void joinFlags(){
		String[] flags = new String[]{"ab", "cd"};
		String ruleFlags = strategy.joinRuleFlags(flags);

		Assert.assertEquals("/abcd", ruleFlags);
	}

	@Test(expected = IllegalArgumentException.class)
	public void joinFlagsWithError(){
		String[] flags = new String[]{"ab", "c"};
		strategy.joinRuleFlags(flags);
	}

	@Test(expected = IllegalArgumentException.class)
	public void joinFlagsWithEmpty(){
	String[] flags = new String[]{"ab", ""};
		strategy.joinRuleFlags(flags);
	}

	@Test(expected = IllegalArgumentException.class)
	public void joinFlagsWithNull(){
		String[] flags = new String[]{"ab", null};
		strategy.joinRuleFlags(flags);
	}

	@Test
	public void joinEmptyFlags(){
		String[] flags = new String[]{};
		String ruleFlags = strategy.joinRuleFlags(flags);

		Assert.assertTrue(ruleFlags.isEmpty());
	}

	@Test
	public void joinNullFlags(){
		String ruleFlags = strategy.joinRuleFlags(null);

		Assert.assertTrue(ruleFlags.isEmpty());
	}
	
}
