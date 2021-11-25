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
package io.github.mtrevisan.hunlinter.languages;

import io.github.mtrevisan.hunlinter.datastructures.SetHelper;
import io.github.mtrevisan.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import io.github.mtrevisan.hunlinter.parsers.enums.MorphologicalTag;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.services.system.PropertiesUTF8;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


public class RulesLoader{

	private static final String WORD_WITH_RULE_CANNOT_HAVE = "Word with rule {} cannot have rule {}";
	private static final String WORD_WITH_LETTER_CANNOT_HAVE = "Word with letter `{}` cannot have rule {}";
	private static final String WORD_WITH_LETTER_CANNOT_HAVE_USE = "Word with letter `{}` cannot have rule {}, use {}";
	private static final String WORD_WITHOUT_LETTER_CANNOT_HAVE = "Word without letter `{}` cannot have rule {}";
	private static final String WORD_WITHOUT_LETTER_CANNOT_HAVE_USE = "Word without letter `{}` cannot have rule {}, use {}";


	private final PropertiesUTF8 rulesProperties;

	private final boolean morphologicalFieldsCheck;
	private final boolean enableVerbSyllabationCheck;
	private final boolean wordCanHaveMultipleStresses;
	private final Map<MorphologicalTag, Set<String>> dataFields = new EnumMap<>(MorphologicalTag.class);
	private final Set<String> unsyllabableWords;
	private final Set<String> multipleStressedWords;
	private final Collection<String> hasToContainStress = new HashSet<>(0);
	private final Collection<String> cannotContainStress = new HashSet<>(0);
	private final Collection<String> canHaveNoInflections = new HashSet<>(0);
	private Character[] letterAndRulesNotCombinableKeys;
	private Map<Character, LetterMatcherEntry[]> letterAndRulesNotCombinable;
	private String[] ruleAndRulesNotCombinableKeys;
	private Map<String, RuleMatcherEntry[]> ruleAndRulesNotCombinable;
	private Character[] letterAndRulesCombinableKeys;
	private Map<Character, LetterMatcherEntry[]> letterAndRulesCombinable;


	public RulesLoader(final String language, final FlagParsingStrategy strategy){
		Objects.requireNonNull(language, "Language cannot be null");

		rulesProperties = BaseBuilder.getRulesProperties(language);

		morphologicalFieldsCheck = Boolean.parseBoolean(rulesProperties.getProperty("morphologicalFieldsCheck"));
		enableVerbSyllabationCheck = Boolean.parseBoolean(rulesProperties.getProperty("verbSyllabationCheck"));
		wordCanHaveMultipleStresses = Boolean.parseBoolean(rulesProperties.getProperty("wordCanHaveMultipleStresses"));

		dataFields.put(MorphologicalTag.STEM, null);
		dataFields.put(MorphologicalTag.ALLOMORPH, null);
		fillDataFields(MorphologicalTag.PART_OF_SPEECH, "partOfSpeeches");

		fillDataFields(MorphologicalTag.DERIVATIONAL_PREFIX, "derivationalPrefixes");

		fillDataFields(MorphologicalTag.DERIVATIONAL_SUFFIX, "derivationalSuffixes");
		fillDataFields(MorphologicalTag.INFLECTIONAL_SUFFIX, "inflectionalSuffixes");
		fillDataFields(MorphologicalTag.TERMINAL_SUFFIX, "terminalSuffixes");

		dataFields.put(MorphologicalTag.PART, null);

		unsyllabableWords = readPropertyAsSet("unsyllabableWords", ',');
		multipleStressedWords = readPropertyAsSet("multipleStressedWords", ',');

		if(strategy != null){
			String[] flags = strategy.parseFlags(readProperty("hasToContainStress"));
			if(flags != null)
				hasToContainStress.addAll(Arrays.asList(flags));
			flags = strategy.parseFlags(readProperty("cannotContainStress"));
			if(flags != null)
				cannotContainStress.addAll(Arrays.asList(flags));
			flags = strategy.parseFlags(readProperty("canHaveNoInflections"));
			if(flags != null)
				canHaveNoInflections.addAll(Arrays.asList(flags));

			final Map<String, List<RuleMatcherEntry>> ruleAndRulesNotCombinable = new HashMap<>(0);
			List<String> rules = readPropertyAsList("notCombinableRules", '/');
			for(int i = 0; i < rules.size(); i ++){
				final String masterFlag = rules.get(i ++);
				final String[] wrongFlags = strategy.parseFlags(rules.get(i));
				ruleAndRulesNotCombinable.computeIfAbsent(masterFlag, k -> new ArrayList<>(1))
					.add(new RuleMatcherEntry(WORD_WITH_RULE_CANNOT_HAVE, masterFlag, wrongFlags));
			}
			ruleAndRulesNotCombinableKeys = new String[ruleAndRulesNotCombinable.size()];
			this.ruleAndRulesNotCombinable = new HashMap<>(ruleAndRulesNotCombinable.size());
			int offset = 0;
			for(final Map.Entry<String, List<RuleMatcherEntry>> entry : ruleAndRulesNotCombinable.entrySet()){
				ruleAndRulesNotCombinableKeys[offset ++] = entry.getKey();
				this.ruleAndRulesNotCombinable.put(entry.getKey(), entry.getValue().toArray(new RuleMatcherEntry[0]));
			}

			Character letter = null;
			rules = readPropertyAsList("letterAndRulesCombinable", '/');
			final Map<Character, List<LetterMatcherEntry>> letterAndRulesCombinable = new HashMap<>(0);
			for(int i = 0; i < rules.size(); i ++){
				final String elem = rules.get(i);
				if(elem.length() == 1)
					letter = elem.charAt(0);
				else{
					flags = strategy.parseFlags(elem);
					final String correctRule = flags[flags.length - 1];
					final String[] wrongFlags = ArrayUtils.remove(flags, flags.length - 1);
					letterAndRulesCombinable.computeIfAbsent(letter, k -> new ArrayList<>(1))
						.add(new LetterMatcherEntry((StringUtils.isNotBlank(correctRule)
							? WORD_WITHOUT_LETTER_CANNOT_HAVE_USE
							: WORD_WITHOUT_LETTER_CANNOT_HAVE),
							letter, wrongFlags, correctRule));
				}
			}
			letterAndRulesCombinableKeys = new Character[letterAndRulesCombinable.size()];
			this.letterAndRulesCombinable = new HashMap<>(letterAndRulesCombinable.size());
			offset = 0;
			for(final Map.Entry<Character, List<LetterMatcherEntry>> entry : letterAndRulesCombinable.entrySet()){
				letterAndRulesCombinableKeys[offset ++] = entry.getKey();
				this.letterAndRulesCombinable.put(entry.getKey(), entry.getValue().toArray(new LetterMatcherEntry[0]));
			}

			rules = readPropertyAsList("letterAndRulesNotCombinable", '/');
			final Map<Character, List<LetterMatcherEntry>> letterAndRulesNotCombinable = new HashMap<>(0);
			for(int i = 0; i < rules.size(); i ++){
				final String elem = rules.get(i);
				if(elem.length() == 1)
					letter = elem.charAt(0);
				else{
					flags = strategy.parseFlags(elem);
					final String correctRule = flags[flags.length - 1];
					final String[] wrongFlags = ArrayUtils.remove(flags, flags.length - 1);
					letterAndRulesNotCombinable.computeIfAbsent(letter, k -> new ArrayList<>(1))
						.add(new LetterMatcherEntry((StringUtils.isNotBlank(correctRule)
								? WORD_WITH_LETTER_CANNOT_HAVE_USE
								: WORD_WITH_LETTER_CANNOT_HAVE),
							letter, wrongFlags, correctRule));
				}
			}
			letterAndRulesNotCombinableKeys = new Character[letterAndRulesNotCombinable.size()];
			this.letterAndRulesNotCombinable = new HashMap<>(letterAndRulesNotCombinable.size());
			offset = 0;
			for(final Map.Entry<Character, List<LetterMatcherEntry>> entry : letterAndRulesNotCombinable.entrySet()){
				letterAndRulesNotCombinableKeys[offset ++] = entry.getKey();
				this.letterAndRulesNotCombinable.put(entry.getKey(), entry.getValue().toArray(new LetterMatcherEntry[0]));
			}
		}
	}

	private void fillDataFields(final MorphologicalTag tag, final String property){
		final List<String> itr = readPropertyAsList(property, ',');
		final Set<String> set = new HashSet<>(itr.size());
		for(int i = 0; i < itr.size(); i ++)
			set.add(tag.getCode() + itr.get(i));
		dataFields.put(tag, set);
	}

	public final String readProperty(final String key){
		return rulesProperties.getProperty(key, StringUtils.EMPTY);
	}

	public final Set<String> readPropertyAsSet(final String key, final char separator){
		final String line = readProperty(key);
		return (StringUtils.isNotEmpty(line)? SetHelper.setOf(StringUtils.split(line, separator)): Collections.emptySet());
	}

	public final List<String> readPropertyAsList(final String key, final char separator){
		final List<String> list = new ArrayList<>(rulesProperties.size());
		for(final Object o : rulesProperties.keySet()){
			final String k = (String)o;
			if(k.equals(key) || k.startsWith(key) && StringUtils.isNumeric(k.substring(key.length()))){
				final String line = readProperty(k);
				list.addAll(Arrays.asList(StringUtils.split(line, separator)));
			}
		}
		return list;
	}


	public final boolean isMorphologicalFieldsCheck(){
		return morphologicalFieldsCheck;
	}

	public final boolean isEnableVerbSyllabationCheck(){
		return enableVerbSyllabationCheck;
	}

	public final boolean isWordCanHaveMultipleStresses(){
		return wordCanHaveMultipleStresses;
	}

	public final boolean containsDataField(final MorphologicalTag key){
		return dataFields.containsKey(key);
	}

	public final Set<String> getDataField(final MorphologicalTag key){
		return dataFields.get(key);
	}

	public final boolean containsUnsyllabableWords(final String word){
		return unsyllabableWords.contains(word);
	}

	public final boolean containsMultipleStressedWords(final String word){
		return multipleStressedWords.contains(word);
	}

	public final boolean containsHasToContainStress(final String flag){
		return hasToContainStress.contains(flag);
	}

	public final boolean containsCannotContainStress(final String flag){
		return cannotContainStress.contains(flag);
	}

	public final boolean containsCanHaveNoInflections(final String flag){
		return canHaveNoInflections.contains(flag);
	}

	public final void letterToFlagIncompatibilityCheck(final Inflection inflection){
		final String word = inflection.getWord();
		for(int i = 0; i < letterAndRulesNotCombinableKeys.length; i ++)
			if(StringUtils.containsAny(word, letterAndRulesNotCombinableKeys[i])){
				final LetterMatcherEntry[] letterMatcherEntries = letterAndRulesNotCombinable.get(letterAndRulesNotCombinableKeys[i]);
				for(int j = 0; j < letterMatcherEntries.length; j ++)
					letterMatcherEntries[j].match(inflection);
			}
	}

	public final void flagToFlagIncompatibilityCheck(final Inflection inflection){
		for(int i = 0; i < ruleAndRulesNotCombinableKeys.length; i ++)
			if(inflection.hasContinuationFlag(ruleAndRulesNotCombinableKeys[i])){
				final RuleMatcherEntry[] ruleMatcherEntries = ruleAndRulesNotCombinable.get(ruleAndRulesNotCombinableKeys[i]);
				for(int j = 0; j < ruleMatcherEntries.length; j ++)
					ruleMatcherEntries[j].match(inflection);
			}
	}

	public final void letterToFlagCompatibilityCheck(final Inflection inflection){
		final String word = inflection.getWord();
		for(int i = 0; i < letterAndRulesCombinableKeys.length; i ++)
			if(!StringUtils.containsAny(word, letterAndRulesCombinableKeys[i])){
				final LetterMatcherEntry[] letterMatcherEntries = letterAndRulesCombinable.get(letterAndRulesCombinableKeys[i]);
				for(int j = 0; j < letterMatcherEntries.length; j ++)
					letterMatcherEntries[j].match(inflection);
			}
	}

}
