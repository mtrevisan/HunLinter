package unit731.hunspeller.languages;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.dtos.MorphologicalTag;
import unit731.hunspeller.parsers.dictionary.vos.Production;


public class RulesLoader{

	private static final MessageFormat WORD_WITH_RULE_CANNOT_HAVE = new MessageFormat("Word with rule {0} cannot have rule {1}");
	private static final MessageFormat WORD_WITH_LETTER_CANNOT_HAVE = new MessageFormat("Word with letter ''{0}'' cannot have rule {1}");
	private static final MessageFormat WORD_WITH_LETTER_CANNOT_HAVE_USE = new MessageFormat("Word with letter ''{0}'' cannot have rule {1}, use {2}");


	private final Properties rulesProperties;
	private final boolean morphologicalFieldsCheck;
	private final boolean enableVerbSyllabationCheck;
	private final boolean wordCanHaveMultipleAccents;
	private final Map<String, Set<String>> dataFields = new HashMap<>();
	private final Set<String> unsyllabableWords;
	private final Set<String> multipleAccentedWords;
	private final Set<String> hasToContainAccent = new HashSet<>();
	private final Set<String> cannotContainAccent = new HashSet<>();
	private final Map<String, Set<LetterMatcherEntry>> letterAndRulesNotCombinable = new HashMap<>();
	private final Map<String, Set<RuleMatcherEntry>> ruleAndRulesNotCombinable = new HashMap<>();


	public RulesLoader(String language, FlagParsingStrategy strategy) throws IOException{
		Objects.requireNonNull(language);

		rulesProperties = BaseBuilder.getRulesProperties(language);

		morphologicalFieldsCheck = Boolean.parseBoolean((String)rulesProperties.get("morphologicalFieldsCheck"));
		enableVerbSyllabationCheck = Boolean.parseBoolean((String)rulesProperties.get("verbSyllabationCheck"));
		wordCanHaveMultipleAccents = Boolean.parseBoolean((String)rulesProperties.get("wordCanHaveMultipleAccents"));

		dataFields.put(MorphologicalTag.TAG_PART_OF_SPEECH, readPropertyAsSet(rulesProperties, "partOfSpeeches", ','));
		dataFields.put(MorphologicalTag.TAG_INFLECTIONAL_SUFFIX, readPropertyAsSet(rulesProperties, "inflectionalSuffixes", ','));
		dataFields.put(MorphologicalTag.TAG_TERMINAL_SUFFIX, readPropertyAsSet(rulesProperties, "terminalSuffixes", ','));
		dataFields.put(MorphologicalTag.TAG_STEM, null);
		dataFields.put(MorphologicalTag.TAG_ALLOMORPH, null);

		unsyllabableWords = readPropertyAsSet(rulesProperties, "unsyllabableWords", ',');
		multipleAccentedWords = readPropertyAsSet(rulesProperties, "multipleAccentedWords", ',');

		if(strategy != null){
			String[] flags = strategy.parseFlags(readProperty(rulesProperties, "hasToContainAccent"));
			if(flags != null)
				hasToContainAccent.addAll(Arrays.asList(flags));
			flags = strategy.parseFlags(readProperty(rulesProperties, "cannotContainAccent"));
			if(flags != null)
				cannotContainAccent.addAll(Arrays.asList(flags));

			Iterator<String> rules = readPropertyAsIterator(rulesProperties, "notCombinableRules", '/');
			while(rules.hasNext()){
				String masterFlag = rules.next();
				String[] wrongFlags = strategy.parseFlags(rules.next());
				ruleAndRulesNotCombinable.computeIfAbsent(masterFlag, k -> new HashSet<>())
					.add(new RuleMatcherEntry(WORD_WITH_RULE_CANNOT_HAVE, masterFlag, wrongFlags));
			}

			String letter = null;
			rules = readPropertyAsIterator(rulesProperties, "letterAndRulesNotCombinable", '/');
			while(rules.hasNext()){
				String elem = rules.next();
				if(elem.length() == 3 && elem.charAt(0) == '_' && elem.charAt(2) == '_')
					letter = String.valueOf(elem.charAt(1));
				else{
					flags = strategy.parseFlags(elem);
					String correctRule = flags[flags.length - 1];
					String[] wrongFlags = ArrayUtils.remove(flags, flags.length - 1);
					letterAndRulesNotCombinable.computeIfAbsent(letter, k -> new HashSet<>())
						.add(new LetterMatcherEntry((StringUtils.isNotBlank(correctRule)? WORD_WITH_LETTER_CANNOT_HAVE_USE: WORD_WITH_LETTER_CANNOT_HAVE),
							letter, wrongFlags, correctRule));
				}
			}
		}
	}

	public final String readProperty(Properties rulesProperties, String key){
		return rulesProperties.getProperty(key, StringUtils.EMPTY);
	}

	public final Set<String> readPropertyAsSet(Properties rulesProperties, String key, char separator){
		String line = readProperty(rulesProperties, key);
		return (StringUtils.isNotEmpty(line)? new HashSet<>(Arrays.asList(StringUtils.split(line, separator))): Collections.<String>emptySet());
	}

	public final Iterator<String> readPropertyAsIterator(Properties rulesProperties, String key, char separator){
		List<String> values = new ArrayList<>();
		@SuppressWarnings("unchecked")
		Set<String> keys = (Set<String>)(Collection<?>)rulesProperties.keySet();
		for(String k : keys)
			if(k.equals(key) || k.startsWith(key) && StringUtils.isNumeric(k.substring(key.length()))){
				String line = readProperty(rulesProperties, k);
				if(StringUtils.isNotEmpty(line))
					values.addAll(Arrays.asList(StringUtils.split(line, separator)));
			}
		return values.iterator();
	}


	public Properties getRulesPorperties(){
		return rulesProperties;
	}

	public boolean isMorphologicalFieldsCheck(){
		return morphologicalFieldsCheck;
	}

	public boolean isEnableVerbSyllabationCheck(){
		return enableVerbSyllabationCheck;
	}

	public boolean isWordCanHaveMultipleAccents(){
		return wordCanHaveMultipleAccents;
	}

	public boolean containsDataField(String key){
		return dataFields.containsKey(key);
	}

	public Set<String> getDataField(String key){
		return dataFields.get(key);
	}

	public boolean containsUnsyllabableWords(String word){
		return unsyllabableWords.contains(word);
	}

	public boolean containsMultipleAccentedWords(String word){
		return multipleAccentedWords.contains(word);
	}

	public boolean containsHasToContainAccent(String word){
		return hasToContainAccent.contains(word);
	}

	public boolean containsCannotContainAccent(String word){
		return cannotContainAccent.contains(word);
	}

	public void letterToFlagIncompatibilityCheck(Production production)
			throws IllegalArgumentException{
		for(Map.Entry<String, Set<LetterMatcherEntry>> check : letterAndRulesNotCombinable.entrySet())
			if(StringUtils.containsAny(production.getWord(), check.getKey()))
				for(LetterMatcherEntry entry : check.getValue())
					entry.match(production);
	}

	public void flagToFlagIncompatibilityCheck(Production production)
			throws IllegalArgumentException{
		for(Map.Entry<String, Set<RuleMatcherEntry>> check : ruleAndRulesNotCombinable.entrySet())
			if(production.hasContinuationFlag(check.getKey()))
				for(RuleMatcherEntry entry : check.getValue())
					entry.match(production);
	}

}
