package unit731.hunspeller.parsers.dictionary.vos;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.affix.strategies.ParsingStrategyFactory;


public class AffixEntryTest{

	@Test
	public void notValidSuffix1(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		String line = "SFX M0 b i a";
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			AffixEntry entry = new AffixEntry(line, strategy, null, null);
		});
		Assertions.assertEquals("This line has the condition part that not ends with the removal part: " + line, exception.getMessage());
	}

	@Test
	public void notValidSuffix2(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		String line = "SFX M0 a ai a";
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			AffixEntry entry = new AffixEntry(line, strategy, null, null);
		});
		Assertions.assertEquals("This line has characters in common between removed and added part: " + line, exception.getMessage());
	}

	@Test
	public void notValidPrefix1(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		String line = "PFX M0 b i a";
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			AffixEntry entry = new AffixEntry(line, strategy, null, null);
		});
		Assertions.assertEquals("This line has the condition part that not starts with the removal part: " + line, exception.getMessage());
	}

	@Test
	public void notValidPrefix2(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		String line = "PFX M0 a ia a";
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			AffixEntry entry = new AffixEntry(line, strategy, null, null);
		});
		Assertions.assertEquals("This line has characters in common between removed and added part: " + line, exception.getMessage());
	}

	@Test
	public void hasContinuationFlag(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		AffixEntry entry = new AffixEntry("SFX M0 0 i/A [^oaie]", strategy, null, null);

		boolean matches = entry.hasContinuationFlag("A");

		Assertions.assertTrue(matches);
	}

	@Test
	public void notHasContinuationFlag(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		AffixEntry entry = new AffixEntry("SFX M0 0 i/A [^oaie]", strategy, null, null);

		boolean matches = entry.hasContinuationFlag("B");

		Assertions.assertFalse(matches);
	}

	@Test
	public void combineContinuationFlags(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		AffixEntry entry = new AffixEntry("SFX M0 0 i/A [^oaie]", strategy, null, null);

		String[] combinedFlags = entry.combineContinuationFlags(new String[]{"B", "A"});

		Assertions.assertArrayEquals(new String[]{"A", "B"}, combinedFlags);
	}

	@Test
	public void isSuffix(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		AffixEntry entry = new AffixEntry("SFX M0 0 i/A [^oaie]", strategy, null, null);

		boolean matches = entry.isSuffix();

		Assertions.assertTrue(matches);
	}

	@Test
	public void isPrefix(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		AffixEntry entry = new AffixEntry("PFX M0 0 i/A [^oaie]", strategy, null, null);

		boolean matches = entry.isSuffix();

		Assertions.assertFalse(matches);
	}

	@Test
	public void matchOk(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		AffixEntry entry = new AffixEntry("SFX M0 0 i/A [^oaie]", strategy, null, null);

		boolean matches = entry.match("man");

		Assertions.assertTrue(matches);
	}

	@Test
	public void matchNotOk(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		AffixEntry entry = new AffixEntry("SFX M0 0 i/A [^oaie]", strategy, null, null);

		boolean matches = entry.match("mano");

		Assertions.assertFalse(matches);
	}

	@Test
	public void applyRuleSuffix(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		AffixEntry entry = new AffixEntry("SFX M0 0 i/A [^oaie]", strategy, null, null);

		String production = entry.applyRule("man\\/man", true);

		Assertions.assertEquals("man\\/mani", production);
	}

	@Test
	public void cannotApplyRuleSuffixFullstrip(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		AffixEntry entry = new AffixEntry("SFX M0 man i/A man", strategy, null, null);

		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			entry.applyRule("man", false);
		});
		Assertions.assertEquals("Cannot strip full words without the FULLSTRIP tag", exception.getMessage());
}

	@Test
	public void applyRulePrefix(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		AffixEntry entry = new AffixEntry("PFX TB ŧ s ŧ	po:noun", strategy, null, null);

		String production = entry.applyRule("ŧinkue", true);

		Assertions.assertEquals("sinkue", production);
	}

	@Test
	public void undoRuleSuffix(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		AffixEntry entry = new AffixEntry("SFX M0 0 i [^oaie]	po:noun", strategy, null, null);

		String production = entry.undoRule("mani");

		Assertions.assertEquals("man", production);
	}

	@Test
	public void undoRulePrefix(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		AffixEntry entry = new AffixEntry("PFX TB ŧ s ŧ	po:noun", strategy, null, null);

		String production = entry.undoRule("sinkue");

		Assertions.assertEquals("ŧinkue", production);
	}

	@Test
	public void testToString(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		AffixEntry entry = new AffixEntry("PFX TB ŧ s ŧ	po:noun", strategy, null, null);

		String representation = entry.toString();

		Assertions.assertEquals("PFX TB ŧ s ŧ", representation);
	}

}
