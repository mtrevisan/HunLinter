package unit731.hunlinter.languages;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
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
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.enums.MorphologicalTag;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.services.SetHelper;


public class RulesLoader{

	private static final MessageFormat WORD_WITH_RULE_CANNOT_HAVE = new MessageFormat("Word with rule {0} cannot have rule {1}");
	private static final MessageFormat WORD_WITH_LETTER_CANNOT_HAVE = new MessageFormat("Word with letter ''{0}'' cannot have rule {1}");
	private static final MessageFormat WORD_WITH_LETTER_CANNOT_HAVE_USE = new MessageFormat("Word with letter ''{0}'' cannot have rule {1}, use {2}");


	private final Properties rulesProperties;

	private final boolean morphologicalFieldsCheck;
	private final boolean enableVerbSyllabationCheck;
	private final boolean wordCanHaveMultipleAccents;
	private final Map<MorphologicalTag, Set<String>> dataFields = new EnumMap<>(MorphologicalTag.class);
	private final Set<String> unsyllabableWords;
	private final Set<String> multipleAccentedWords;
	private final Set<String> hasToContainAccent = new HashSet<>();
	private final Set<String> cannotContainAccent = new HashSet<>();
	private final Map<String, Set<LetterMatcherEntry>> letterAndRulesNotCombinable = new HashMap<>();
	private final Map<String, Set<RuleMatcherEntry>> ruleAndRulesNotCombinable = new HashMap<>();


	public RulesLoader(final String language, final FlagParsingStrategy strategy){
		Objects.requireNonNull(language);

		rulesProperties = BaseBuilder.getRulesProperties(language);

		morphologicalFieldsCheck = Boolean.parseBoolean((String)rulesProperties.get("morphologicalFieldsCheck"));
		enableVerbSyllabationCheck = Boolean.parseBoolean((String)rulesProperties.get("verbSyllabationCheck"));
		wordCanHaveMultipleAccents = Boolean.parseBoolean((String)rulesProperties.get("wordCanHaveMultipleAccents"));

		fillDataFields(MorphologicalTag.TAG_PART_OF_SPEECH, "partOfSpeeches");
		fillDataFields(MorphologicalTag.TAG_DERIVATIONAL_SUFFIX, "derivationalSuffixes");
		fillDataFields(MorphologicalTag.TAG_INFLECTIONAL_SUFFIX, "inflectionalSuffixes");
		fillDataFields(MorphologicalTag.TAG_TERMINAL_SUFFIX, "terminalSuffixes");
		dataFields.put(MorphologicalTag.TAG_STEM, null);
		dataFields.put(MorphologicalTag.TAG_ALLOMORPH, null);

		unsyllabableWords = readPropertyAsSet("unsyllabableWords", ',');
		multipleAccentedWords = readPropertyAsSet("multipleAccentedWords", ',');

		if(strategy != null){
			String[] flags = strategy.parseFlags(readProperty("hasToContainAccent"));
			if(flags != null)
				hasToContainAccent.addAll(Arrays.asList(flags));
			flags = strategy.parseFlags(readProperty("cannotContainAccent"));
			if(flags != null)
				cannotContainAccent.addAll(Arrays.asList(flags));

			Iterator<String> rules = readPropertyAsIterator("notCombinableRules", '/');
			while(rules.hasNext()){
				final String masterFlag = rules.next();
				final String[] wrongFlags = strategy.parseFlags(rules.next());
				ruleAndRulesNotCombinable.computeIfAbsent(masterFlag, k -> new HashSet<>(1))
					.add(new RuleMatcherEntry(WORD_WITH_RULE_CANNOT_HAVE, masterFlag, wrongFlags));
			}

			String letter = null;
			rules = readPropertyAsIterator("letterAndRulesNotCombinable", '/');
			while(rules.hasNext()){
				final String elem = rules.next();
				if(elem.length() == 2 && elem.charAt(1) == ':')
					letter = String.valueOf(elem.charAt(1));
				else{
					flags = strategy.parseFlags(elem);
					final String correctRule = flags[flags.length - 1];
					final String[] wrongFlags = ArrayUtils.remove(flags, flags.length - 1);
					letterAndRulesNotCombinable.computeIfAbsent(letter, k -> new HashSet<>(1))
						.add(new LetterMatcherEntry((StringUtils.isNotBlank(correctRule)? WORD_WITH_LETTER_CANNOT_HAVE_USE: WORD_WITH_LETTER_CANNOT_HAVE),
							letter, wrongFlags, correctRule));
				}
			}
		}
	}

	private void fillDataFields(final MorphologicalTag tag, final String property){
		final Iterator<String> itr = readPropertyAsIterator(property, ',');
		final Set<String> set = new HashSet<>();
		while(itr.hasNext())
			set.add(tag.getCode() + itr.next());
		dataFields.put(tag, set);
	}

	public final String readProperty(final String key){
		return rulesProperties.getProperty(key, StringUtils.EMPTY);
	}

	public final Set<String> readPropertyAsSet(final String key, final char separator){
		final String line = readProperty(key);
		return (StringUtils.isNotEmpty(line)? SetHelper.setOf(StringUtils.split(line, separator)): Collections.emptySet());
	}

	public final Iterator<String> readPropertyAsIterator(final String key, final char separator){
		final List<String> list = new ArrayList<>();
		for(final Object o : rulesProperties.keySet()){
			final String k = (String)o;
			if(k.equals(key) || k.startsWith(key) && StringUtils.isNumeric(k.substring(key.length()))){
				final String line = readProperty(k);
				list.addAll(Arrays.asList(StringUtils.split(line, separator)));
			}
		}
		return list.iterator();
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

	public boolean containsDataField(final MorphologicalTag key){
		return dataFields.containsKey(key);
	}

	public Set<String> getDataField(final MorphologicalTag key){
		return dataFields.get(key);
	}

	public boolean containsUnsyllabableWords(final String word){
		return unsyllabableWords.contains(word);
	}

	public boolean containsMultipleAccentedWords(final String word){
		return multipleAccentedWords.contains(word);
	}

	public boolean containsHasToContainAccent(final String word){
		return hasToContainAccent.contains(word);
	}

	public boolean containsCannotContainAccent(final String word){
		return cannotContainAccent.contains(word);
	}

	public void letterToFlagIncompatibilityCheck(final Production production){
		for(final Map.Entry<String, Set<LetterMatcherEntry>> entry : letterAndRulesNotCombinable.entrySet())
			if(StringUtils.containsAny(production.getWord(), entry.getKey()))
				for(final LetterMatcherEntry letterMatcherEntry : entry.getValue())
					letterMatcherEntry.match(production);
	}

	public void flagToFlagIncompatibilityCheck(final Production production){
		for(final Map.Entry<String, Set<RuleMatcherEntry>> check : ruleAndRulesNotCombinable.entrySet())
			if(production.hasContinuationFlag(check.getKey()))
				for(final RuleMatcherEntry entry : check.getValue())
					entry.match(production);
	}

}
