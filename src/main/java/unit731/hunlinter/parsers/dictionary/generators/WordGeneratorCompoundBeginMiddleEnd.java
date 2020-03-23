package unit731.hunlinter.parsers.dictionary.generators;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.regexgenerator.HunSpellRegexWordGenerator;


class WordGeneratorCompoundBeginMiddleEnd extends WordGeneratorCompound{

	private static final MessageFormat NON_POSITIVE_LIMIT = new MessageFormat("Limit cannot be non–positive: was {0}");
	private static final MessageFormat MISSING_WORD = new MessageFormat("Missing word(s) for rule ''{0}'' in compound begin–middle–end");


	WordGeneratorCompoundBeginMiddleEnd(final AffixData affixData, final DictionaryParser dicParser, final WordGenerator wordGenerator){
		super(affixData, dicParser, wordGenerator);
	}

	/**
	 * Generates a list of stems for the provided flag from words in the dictionary marked with AffixOption.COMPOUND_BEGIN, AffixOption.COMPOUND_MIDDLE,
	 * and AffixOption.COMPOUND_END
	 *
	 * @param inputCompounds	List of compounds used to generate the inflection through the compound rule
	 * @param limit	Limit result count
	 * @return	The list of inflections
	 * @throws NoApplicableRuleException	If there is a rule that doesn't apply to the word
	 */
	Inflection[] applyCompoundBeginMiddleEnd(final String[] inputCompounds, final int limit){
		Objects.requireNonNull(inputCompounds);
		if(limit <= 0)
			throw new LinterException(NON_POSITIVE_LIMIT.format(new Object[]{limit}));

		final String compoundBeginFlag = affixData.getCompoundBeginFlag();
		final String compoundMiddleFlag = affixData.getCompoundMiddleFlag();
		final String compoundEndFlag = affixData.getCompoundEndFlag();

		loadDictionaryForInclusionTest();

		//extract map flag -> dictionary entries
		final Map<String, List<DictionaryEntry>> inputs = extractCompoundBeginMiddleEnd(inputCompounds, compoundBeginFlag, compoundMiddleFlag,
			compoundEndFlag);

		checkCompoundBeginMiddleEndInputCorrectness(inputs);

		final String[] compoundRule = new String[]{compoundBeginFlag, "?", compoundMiddleFlag, "?", compoundEndFlag, "?"};
		final HunSpellRegexWordGenerator regexWordGenerator = new HunSpellRegexWordGenerator(compoundRule);
		//generate all the words that matches the given regex
		final List<List<String>> permutations = regexWordGenerator.generateAll(2, limit);

		final List<List<Inflection[]>> entries = generateCompounds(permutations, inputs);

		return applyCompound(entries, limit);
	}

	private Map<String, List<DictionaryEntry>> extractCompoundBeginMiddleEnd(final String[] inputCompounds, final String compoundBeginFlag,
			final String compoundMiddleFlag, final String compoundEndFlag){
		final int compoundMinimumLength = affixData.getCompoundMinimumLength();
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();

		//extract map flag -> compounds
		Map<String, List<DictionaryEntry>> compoundRules = new HashMap<>();
		for(final String inputCompound : inputCompounds){
			final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(inputCompound, affixData);

			final Inflection[] inflections = applyAffixRules(dicEntry, false, null);
			for(final Inflection inflection : inflections){
				final Map<String, List<DictionaryEntry>> distribution = inflection.distributeByCompoundBeginMiddleEnd(compoundBeginFlag,
					compoundMiddleFlag, compoundEndFlag);
				compoundRules = mergeDistributions(compoundRules, distribution, compoundMinimumLength, forbiddenWordFlag);
			}
		}
		return compoundRules;
	}

	private void checkCompoundBeginMiddleEndInputCorrectness(final Map<String, List<DictionaryEntry>> inputs){
		for(final Map.Entry<String, List<DictionaryEntry>> entry : inputs.entrySet())
			if(entry.getValue().isEmpty())
				throw new LinterException(MISSING_WORD.format(new Object[]{entry.getKey()}));
	}

}
