package unit731.hunspeller.parsers.strategies;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;


public class UTF8ParsingStrategyTest{

	private final FlagParsingStrategy strategy = new UTF8ParsingStrategy();


	@Test
	public void ok(){
		String[] flags = strategy.parseRuleFlags("èŧ");

		Assert.assertEquals(Arrays.asList("ŧ", "è"), Arrays.asList(flags));
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
		String[] flags = new String[]{"è", "ŧ"};
		String ruleFlags = strategy.joinRuleFlags(flags);

		Assert.assertEquals("/èŧ", ruleFlags);
	}

	@Test(expected = IllegalArgumentException.class)
	public void joinFlagsWithError(){
		String[] flags = new String[]{"è", "aŧ"};
		strategy.joinRuleFlags(flags);
	}

	@Test(expected = IllegalArgumentException.class)
	public void joinFlagsWithEmpty(){
		String[] flags = new String[]{"è", ""};
		strategy.joinRuleFlags(flags);
	}

	@Test(expected = IllegalArgumentException.class)
	public void joinFlagsWithNull(){
		String[] flags = new String[]{"ŧ", null};
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
