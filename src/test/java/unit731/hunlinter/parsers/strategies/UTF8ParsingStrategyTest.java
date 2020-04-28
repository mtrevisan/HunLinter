package unit731.hunlinter.parsers.strategies;

import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.affix.strategies.ParsingStrategyFactory;
import unit731.hunlinter.workers.exceptions.LinterException;


class UTF8ParsingStrategyTest{

	private final FlagParsingStrategy strategy = ParsingStrategyFactory.createUTF8ParsingStrategy();


	@Test
	void ok(){
		String[] flags = strategy.parseFlags("èŧ");

		Assertions.assertEquals(Arrays.asList("è", "ŧ"), Arrays.asList(flags));
	}

	@Test
	void empty(){
		String[] flags = strategy.parseFlags("");

		Assertions.assertNull(flags);
	}

	@Test
	void nullFlags(){
		String[] flags = strategy.parseFlags(null);

		Assertions.assertNull(flags);
	}

	@Test
	void joinFlags(){
		String[] flags = new String[]{"è", "ŧ"};
		String continuationFlags = strategy.joinFlags(flags);

		Assertions.assertEquals("èŧ", continuationFlags);
	}

	@Test
	void joinFlagsWithError(){
		Throwable exception = Assertions.assertThrows(LinterException.class, () -> {
			String[] flags = new String[]{"è", "aŧ"};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Flag should be of length one and in UTF-8 encoding: was 'aŧ'", exception.getMessage());
	}

	@Test
	void joinFlagsWithNoUTF8(){
		Throwable exception = Assertions.assertThrows(LinterException.class, () -> {
			String[] flags = new String[]{"\\x{FFFD}"};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Flag should be of length one and in UTF-8 encoding: was '\\x{FFFD}'", exception.getMessage());
	}

	@Test
	void joinFlagsWithEmpty(){
		Throwable exception = Assertions.assertThrows(LinterException.class, () -> {
			String[] flags = new String[]{"è", ""};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Flag should be of length one and in UTF-8 encoding: was ''", exception.getMessage());
	}

	@Test
	void joinFlagsWithNull(){
		Throwable exception = Assertions.assertThrows(LinterException.class, () -> {
			String[] flags = new String[]{"ŧ", null};
			strategy.joinFlags(flags);
		});
		Assertions.assertEquals("Flag should be of length one and in UTF-8 encoding: was 'null'", exception.getMessage());
	}

	@Test
	void joinEmptyFlags(){
		String[] flags = new String[]{};
		String continuationFlags = strategy.joinFlags(flags);

		Assertions.assertTrue(continuationFlags.isEmpty());
	}

	@Test
	void joinNullFlags(){
		String continuationFlags = strategy.joinFlags((String[])null);

		Assertions.assertTrue(continuationFlags.isEmpty());
	}

}
