package unit731.hunlinter.parsers.dictionary.generators;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.parsers.workers.exceptions.HunLintException;
import unit731.hunlinter.services.regexgenerator.HunSpellRegexWordGenerator;


class WordGeneratorCompoundRules extends WordGeneratorCompound{

	private static final MessageFormat NON_POSITIVE_LIMIT = new MessageFormat("Limit cannot be non–positive: was {0}");
	private static final MessageFormat MISSING_WORD = new MessageFormat("Missing word(s) for rule {0} in compound rule {1}");


	WordGeneratorCompoundRules(final AffixData affixData, final DictionaryParser dicParser, final WordGenerator wordGenerator){
		super(affixData, dicParser, wordGenerator);
	}

	/**
	 * Generates a list of stems for the provided rule from words in the dictionary marked with AffixOption.COMPOUND_RULE
	 *
	 * @param inputCompounds	List of compounds used to generate the production through the compound rule
	 * @param compoundRule	Rule used to generate the productions for
	 * @param limit	Limit result count
	 * @return	The list of productions for the given rule
	 * @throws NoApplicableRuleException	If there is a rule that does not apply to the word
	 */
	List<Production> applyCompoundRules(final String[] inputCompounds, final String compoundRule, final int limit){
		Objects.requireNonNull(inputCompounds);
		Objects.requireNonNull(compoundRule);
		if(limit <= 0)
			throw new HunLintException(NON_POSITIVE_LIMIT.format(new Object[]{limit}));

		final FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();

		loadDictionaryForInclusionTest();

		//extract map flag -> dictionary entries
		final Map<String, Set<DictionaryEntry>> inputs = extractCompoundRules(inputCompounds);

		final String[] compoundRuleComponents = strategy.extractCompoundRule(compoundRule);

		checkCompoundRuleInputCorrectness(inputs, compoundRuleComponents);

		final HunSpellRegexWordGenerator regexWordGenerator = new HunSpellRegexWordGenerator(compoundRuleComponents);
		//generate all the words that matches the given regex
		final List<List<String>> permutations = regexWordGenerator.generateAll(2, limit);

		final List<List<List<Production>>> entries = generateCompounds(permutations, inputs);

		return applyCompound(entries, limit);
	}

	/** Extract a map of flag > dictionary entry from input compounds */
	private Map<String, Set<DictionaryEntry>> extractCompoundRules(final String[] inputCompounds){
		final int compoundMinimumLength = affixData.getCompoundMinimumLength();
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();

		//extract map flag -> compounds
		Map<String, Set<DictionaryEntry>> compoundRules = new HashMap<>();
		for(final String inputCompound : inputCompounds){
			final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(inputCompound, affixData);
			final Map<String, Set<DictionaryEntry>> distribution = dicEntry.distributeByCompoundRule(affixData);
			compoundRules = mergeDistributions(compoundRules, distribution, compoundMinimumLength, forbiddenWordFlag);
		}
		return compoundRules;
	}

	private void checkCompoundRuleInputCorrectness(final Map<String, Set<DictionaryEntry>> inputs, final String[] compoundRuleComponents){
		for(final String component : compoundRuleComponents)
			if(raiseError(inputs, component))
				throw new HunLintException(MISSING_WORD.format(new Object[]{component,
					StringUtils.join(compoundRuleComponents)}));
	}

	private boolean raiseError(final Map<String, Set<DictionaryEntry>> inputs, final String component){
		final char chr = (component.length() == 1? component.charAt(0): 0);
		return (chr != '*' && chr != '?' && inputs.get(component) == null);
	}

}
