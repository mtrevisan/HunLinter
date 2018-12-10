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
import unit731.hunspeller.languages.valueobjects.LetterMatcherEntry;
import unit731.hunspeller.languages.valueobjects.RuleMatcherEntry;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.dtos.MorphologicalTag;
import unit731.hunspeller.parsers.hyphenation.hyphenators.AbstractHyphenator;


public class CorrectnessChecker{

	private static final MessageFormat WORD_WITH_RULE_CANNOT_HAVE = new MessageFormat("Word with rule {0} cannot have rule {1} for {2}");
	private static final MessageFormat WORD_WITH_LETTER_CANNOT_HAVE = new MessageFormat("Word with letter ''{0}'' cannot have rule {1} for {3}");
	private static final MessageFormat WORD_WITH_LETTER_CANNOT_HAVE_USE = new MessageFormat("Word with letter ''{0}'' cannot have rule {1}, use {2} for {3}");


	protected AffixParser affParser;
	protected final AbstractHyphenator hyphenator;

	protected boolean enableVerbCheck;
	protected final Map<String, Set<String>> dataFields = new HashMap<>();
	protected Set<String> unsyllabableWords;
	protected Set<String> multipleAccentedWords;
	protected final Map<String, Set<RuleMatcherEntry>> ruleAndRulesNotCombinable = new HashMap<>();
	protected final Map<String, Set<LetterMatcherEntry>> letterAndRulesNotCombinable = new HashMap<>();


	public CorrectnessChecker(AffixParser affParser, AbstractHyphenator hyphenator){
		Objects.requireNonNull(affParser);

		this.affParser = affParser;
		this.hyphenator = hyphenator;
	}

	protected final void loadRules(Properties rulesProperties) throws IOException{
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();

		enableVerbCheck = Boolean.getBoolean((String)rulesProperties.get("verbCheck"));

		dataFields.put(MorphologicalTag.TAG_PART_OF_SPEECH, readPropertyAsSet(rulesProperties, "partOfSpeeches", ','));
		dataFields.put(MorphologicalTag.TAG_INFLECTIONAL_SUFFIX, readPropertyAsSet(rulesProperties, "inflectionalSuffixes", ','));
		dataFields.put(MorphologicalTag.TAG_TERMINAL_SUFFIX, readPropertyAsSet(rulesProperties, "terminalSuffixes", ','));
		dataFields.put(MorphologicalTag.TAG_STEM, null);
		dataFields.put(MorphologicalTag.TAG_ALLOMORPH, null);

		unsyllabableWords = readPropertyAsSet(rulesProperties, "unsyllabableWords", ',');
		multipleAccentedWords = readPropertyAsSet(rulesProperties, "multipleAccentedWords", ',');

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
			boolean converse = false;
			if(elem.length() == 3 && elem.charAt(0) == '_' && elem.charAt(2) == '_')
				letter = String.valueOf(elem.charAt(1));
			else if(elem.length() == 3 && elem.charAt(0) == '^' && elem.charAt(2) == '^'){
				letter = String.valueOf(elem.charAt(1));
				converse = true;
			}
			else{
				String[] flags = strategy.parseFlags(elem);
				String correctRule = flags[flags.length - 1];
				String[] wrongFlags = ArrayUtils.remove(flags, flags.length - 1);
				letterAndRulesNotCombinable.computeIfAbsent(letter, k -> new HashSet<>())
					.add(new LetterMatcherEntry((StringUtils.isNotBlank(correctRule)? WORD_WITH_LETTER_CANNOT_HAVE_USE: WORD_WITH_LETTER_CANNOT_HAVE),
						letter, wrongFlags, correctRule));
				if(converse)
					letterAndRulesNotCombinable.computeIfAbsent(letter, k -> new HashSet<>())
						.add(new LetterMatcherEntry((StringUtils.isNotBlank(correctRule)? WORD_WITH_LETTER_CANNOT_HAVE_USE: WORD_WITH_LETTER_CANNOT_HAVE),
							letter, new String[]{correctRule}, wrongFlags[0]));
			}
		}
	}

	private Set<String> readPropertyAsSet(Properties rulesProperties, String key, char separator){
		String line = rulesProperties.getProperty(key, StringUtils.EMPTY);
		return (StringUtils.isNotEmpty(line)? new HashSet<>(Arrays.asList(StringUtils.split(line, separator))): Collections.<String>emptySet());
	}

	private Iterator<String> readPropertyAsIterator(Properties rulesProperties, String key, char separator){
		List<String> values = new ArrayList<>();
		Set<String> keys = (Set<String>)(Collection<?>)rulesProperties.keySet();
		for(String k : keys)
			if(k.equals(key) || k.startsWith(key) && StringUtils.isNumeric(k.substring(key.length()))){
				String line = rulesProperties.getProperty(k, StringUtils.EMPTY);
				if(StringUtils.isNotEmpty(line))
					values.addAll(Arrays.asList(StringUtils.split(line, separator)));
			}
		return values.iterator();
	}

	protected void letterToFlagIncompatibilityCheck(Production production, Map<String, Set<LetterMatcherEntry>> checks)
			throws IllegalArgumentException{
		for(Map.Entry<String, Set<LetterMatcherEntry>> check : checks.entrySet())
			if(StringUtils.containsAny(production.getWord(), check.getKey()))
				for(LetterMatcherEntry entry : check.getValue())
					entry.match(production);
	}

	protected void flagToFlagIncompatibilityCheck(Production production, Map<String, Set<RuleMatcherEntry>> checks)
			throws IllegalArgumentException{
		for(Map.Entry<String, Set<RuleMatcherEntry>> check : checks.entrySet())
			if(production.hasContinuationFlag(check.getKey()))
				for(RuleMatcherEntry entry : check.getValue())
					entry.match(production);
	}

	public AffixParser getAffParser(){
		return affParser;
	}

	public AbstractHyphenator getHyphenator(){
		return hyphenator;
	}

	//correctness worker:
	public void checkProduction(Production production) throws IllegalArgumentException{
		String forbidCompoundFlag = affParser.getForbidCompoundFlag();
		if(!production.hasProductionRules() && production.hasContinuationFlag(forbidCompoundFlag))
			throw new IllegalArgumentException("Non-affix entry contains COMPOUNDFORBIDFLAG");
	}

	//minimal pairs worker:
	public boolean isConsonant(char chr){
		return true;
	}

	//minimal pairs worker:
	public boolean shouldBeProcessedForMinimalPair(Production production){
		return true;
	}

}
