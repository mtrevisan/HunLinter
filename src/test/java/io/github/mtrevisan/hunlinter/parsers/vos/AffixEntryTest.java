/**
 * Copyright (c) 2019-2021 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.parsers.vos;

import io.github.mtrevisan.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import io.github.mtrevisan.hunlinter.parsers.affix.strategies.ParsingStrategyFactory;
import io.github.mtrevisan.hunlinter.parsers.enums.AffixType;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;


class AffixEntryTest{

	@Test
	void notValidSuffix1(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		RuleEntry parent = new RuleEntry(AffixType.SUFFIX, "M", 'N');
		String line = "SFX M b i a";
		Throwable exception = Assertions.assertThrows(LinterException.class,
			() -> createAffixEntry(line, parent, strategy));
		Assertions.assertEquals("Condition part doesn't ends with removal part: `" + line + "`", exception.getMessage());
	}

	//FIXME an event is sent to the event bus
//	@Test
//	void notValidSuffix2(){
//		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
//		RuleEntry parent = new RuleEntry(AffixType.SUFFIX, "M", 'N');
//		String line = "SFX M a ai a";
//		Throwable exception = Assertions.assertThrows(LinterException.class,
//			() -> createAffixEntry(line, parent, strategy));
//		Assertions.assertEquals("Characters in common between removed and added part: `" + line + "`", exception.getMessage());
//	}

	@Test
	void notValidPrefix1(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		RuleEntry parent = new RuleEntry(AffixType.PREFIX, "M", 'N');
		String line = "PFX M b i a";
		Throwable exception = Assertions.assertThrows(LinterException.class,
			() -> createAffixEntry(line, parent, strategy));
		Assertions.assertEquals("Condition part doesn't starts with removal part: `" + line + "`", exception.getMessage());
	}

	//FIXME an event is sent to the event bus
//	@Test
//	void notValidPrefix2(){
//		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
//		RuleEntry parent = new RuleEntry(AffixType.PREFIX, "M", 'N');
//		String line = "PFX M a ia a";
//		Throwable exception = Assertions.assertThrows(LinterException.class,
//			() -> createAffixEntry(line, parent, strategy));
//		Assertions.assertEquals("Characters in common between removed and added part: `" + line + "`", exception.getMessage());
//	}

	@Test
	void hasContinuationFlag(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		RuleEntry parent = new RuleEntry(AffixType.SUFFIX, "M", 'N');
		AffixEntry entry = createAffixEntry("SFX M 0 i/A [^oaie]", parent, strategy);

		boolean matches = entry.hasContinuationFlag("A");

		Assertions.assertTrue(matches);
	}

	@Test
	void notHasContinuationFlag(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		RuleEntry parent = new RuleEntry(AffixType.SUFFIX, "M", 'N');
		AffixEntry entry = createAffixEntry("SFX M 0 i/A [^oaie]", parent, strategy);

		boolean matches = entry.hasContinuationFlag("B");

		Assertions.assertFalse(matches);
	}

	@Test
	void combineContinuationFlags(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		RuleEntry parent = new RuleEntry(AffixType.SUFFIX, "M", 'N');
		AffixEntry entry = createAffixEntry("SFX M 0 i/A [^oaie]", parent, strategy);

		final List<String> combinedFlags = entry.combineContinuationFlags(Arrays.asList("B", "A"));

		Assertions.assertEquals(Arrays.asList("B", "A"), combinedFlags);
	}

	@Test
	void isSuffix(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		RuleEntry parent = new RuleEntry(AffixType.SUFFIX, "M", 'N');
		AffixEntry entry = createAffixEntry("SFX M 0 i/A [^oaie]", parent, strategy);
		parent.setEntries(entry);

		AffixType type = entry.getType();

		Assertions.assertEquals(AffixType.SUFFIX, type);
	}

	@Test
	void isPrefix(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		RuleEntry parent = new RuleEntry(AffixType.PREFIX, "M", 'N');
		AffixEntry entry = createAffixEntry("PFX M 0 i/A [^oaie]", parent, strategy);
		parent.setEntries(entry);

		AffixType type = entry.getType();

		Assertions.assertEquals(AffixType.PREFIX, type);
	}

	@Test
	void matchOk(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		RuleEntry parent = new RuleEntry(AffixType.SUFFIX, "M", 'N');
		AffixEntry entry = createAffixEntry("SFX M 0 i/A [^oaie]", parent, strategy);
		parent.setEntries(entry);

		boolean matches = entry.canApplyTo("man");

		Assertions.assertTrue(matches);
	}

	@Test
	void matchNotOk(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		RuleEntry parent = new RuleEntry(AffixType.SUFFIX, "M", 'N');
		AffixEntry entry = createAffixEntry("SFX M 0 i/A [^oaie]", parent, strategy);
		parent.setEntries(entry);

		boolean matches = entry.canApplyTo("mano");

		Assertions.assertFalse(matches);
	}

	@Test
	void applyRuleSuffix(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		RuleEntry parent = new RuleEntry(AffixType.SUFFIX, "M", 'N');
		AffixEntry entry = createAffixEntry("SFX M 0 i/A [^oaie]", parent, strategy);
		parent.setEntries(entry);

		String inflection = entry.applyRule("man\\/man", true);

		Assertions.assertEquals("man\\/mani", inflection);
	}

	@Test
	void cannotApplyRuleSuffixFullstrip(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		RuleEntry parent = new RuleEntry(AffixType.SUFFIX, "M", 'N');
		AffixEntry entry = createAffixEntry("SFX M man i/A man", parent, strategy);

		Throwable exception = Assertions.assertThrows(LinterException.class,
			() -> entry.applyRule("man", false));
		Assertions.assertEquals("Cannot strip full word `man` without the FULLSTRIP option", exception.getMessage());
}

	@Test
	void applyRulePrefix(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		RuleEntry parent = new RuleEntry(AffixType.PREFIX, "T", 'N');
		AffixEntry entry = createAffixEntry("PFX T ŧ s ŧ	po:noun", parent, strategy);
		parent.setEntries(entry);

		String inflection = entry.applyRule("ŧinkue", true);

		Assertions.assertEquals("sinkue", inflection);
	}

	@Test
	void undoRuleSuffix(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		RuleEntry parent = new RuleEntry(AffixType.SUFFIX, "M", 'N');
		AffixEntry entry = createAffixEntry("SFX M 0 i [^oaie]	po:noun", parent, strategy);
		parent.setEntries(entry);

		String inflection = entry.undoRule("mani");

		Assertions.assertEquals("man", inflection);
	}

	@Test
	void undoRulePrefix(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		RuleEntry parent = new RuleEntry(AffixType.PREFIX, "T", 'N');
		AffixEntry entry = createAffixEntry("PFX T ŧ s ŧ	po:noun", parent, strategy);
		parent.setEntries(entry);

		String inflection = entry.undoRule("sinkue");

		Assertions.assertEquals("ŧinkue", inflection);
	}

	@Test
	void testToString(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		RuleEntry parent = new RuleEntry(AffixType.PREFIX, "T", 'N');
		AffixEntry entry = createAffixEntry("PFX T ŧ s ŧ	po:noun", parent, strategy);
		parent.setEntries(entry);

		String representation = entry.toString();

		Assertions.assertEquals("PFX T ŧ s ŧ po:noun", representation);
	}


	private AffixEntry createAffixEntry(final String line, final RuleEntry parent, final FlagParsingStrategy strategy){
		return new AffixEntry(line, 0, parent.getType(), parent.getFlag(), strategy, null, null);
	}

}
