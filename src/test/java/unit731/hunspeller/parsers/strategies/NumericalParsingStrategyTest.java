package unit731.hunspeller.parsers.strategies;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;


public class NumericalParsingStrategyTest{

	private final FlagParsingStrategy strategy = new NumericalParsingStrategy();


	@Test
	public void ok(){
		String[] flags = strategy.parseRuleFlags("1,2");

		Assert.assertEquals(Arrays.asList("1", "2"), Arrays.asList(flags));
	}

	@Test(expected = IllegalArgumentException.class)
	public void notOk1(){
		strategy.parseRuleFlags("ab");
	}

	@Test(expected = IllegalArgumentException.class)
	public void notOk2(){
		strategy.parseRuleFlags("1.2");
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
		String[] flags = new String[]{"1", "2"};
		String ruleFlags = strategy.joinRuleFlags(flags);

		Assert.assertEquals("/1,2", ruleFlags);
	}

	@Test(expected = IllegalArgumentException.class)
	public void joinFlagsWithError1(){
		String[] flags = new String[]{"1", "c"};
		strategy.joinRuleFlags(flags);
	}

	@Test(expected = IllegalArgumentException.class)
	public void joinFlagsWithError2(){
		String[] flags = new String[]{"1", "1.2"};
		strategy.joinRuleFlags(flags);
	}

	@Test(expected = IllegalArgumentException.class)
	public void joinFlagsWithEmpty(){
		String[] flags = new String[]{"1", ""};
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
