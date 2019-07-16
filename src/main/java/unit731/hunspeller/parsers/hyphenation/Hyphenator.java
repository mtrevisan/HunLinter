package unit731.hunspeller.parsers.hyphenation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunspeller.collections.ahocorasicktrie.AhoCorasickTrie;
import unit731.hunspeller.collections.ahocorasicktrie.dtos.SearchResult;
import unit731.hunspeller.services.PatternHelper;


public class Hyphenator implements HyphenatorInterface{

	private final HyphenationParser hypParser;
	private final String breakCharacter;


	public Hyphenator(final HyphenationParser hypParser, final String breakCharacter){
		Objects.requireNonNull(hypParser);
		Objects.requireNonNull(breakCharacter);

		this.hypParser = hypParser;
		this.breakCharacter = breakCharacter;
	}

	/**
	 * Performs hyphenation including an additional rule
	 * NOTE: Calling the method {@link unit731.hunspeller.languages.Orthography#correctOrthography(String)} may be necessary
	 *
	 * @param word	String to hyphenate
	 * @param additionalRule	Rule to add to the set of rules that will generate the hyphenation
	 * @param level	The level to add the rule to
	 * @return the hyphenation object
	 */
	@Override
	public Hyphenation hyphenate(final String word, final String additionalRule, final HyphenationParser.Level level){
		hypParser.addRule(additionalRule, level);
		final Hyphenation hyph = hyphenate(word);
		hypParser.removeRule(additionalRule, level);
		return hyph;
	}

	/**
	 * Performs hyphenation
	 * NOTE: Calling the method {@link unit731.hunspeller.languages.Orthography#correctOrthography(String)} may be necessary
	 *
	 * @param word	String to hyphenate
	 * @return the hyphenation object
	 */
	@Override
	public Hyphenation hyphenate(final String word){
		//apply first level hyphenation
		final Map<HyphenationParser.Level, AhoCorasickTrie<String>> patterns = hypParser.getPatterns();
		HyphenationOptions options = hypParser.getOptParser().getNonCompoundOptions();
		final HyphenationBreak hyphBreak = hyphenate(word, patterns, HyphenationParser.Level.NON_COMPOUND, options);

		final List<String> compounds = createHyphenatedWord(word, hyphBreak);
		List<String> syllabes = compounds;
		List<String> rules = hyphBreak.getRules();

		//if there is a second level
		if(hypParser.isSecondLevelPresent()){
			final List<String> syllabes2ndLevel = new ArrayList<>();
			final List<String> rules2ndLevel = new ArrayList<>();

			//apply second level hyphenation for the word parts
			int i = 0;
			final int parentRulesSize = rules.size();
			for(final String compound : compounds){
				options = hypParser.getOptParser().getCompoundOptions();
				final HyphenationBreak subHyph = hyphenate(compound, patterns, HyphenationParser.Level.COMPOUND, options);

				syllabes2ndLevel.addAll(createHyphenatedWord(compound, subHyph));
				rules2ndLevel.addAll(subHyph.getRules());
				if(i < parentRulesSize)
					rules2ndLevel.add(rules.get(i ++));
			}

			syllabes = syllabes2ndLevel;
			rules = rules2ndLevel;
		}

		//enforce no-hyphens
		hyphBreak.enforceNoHyphens(syllabes, hypParser.getOptParser().getNoHyphen());

		return new Hyphenation(syllabes, compounds, rules, breakCharacter);
	}

	/**
	 * Performs hyphenation
	 *
	 * @param word	String to hyphenate
	 * @param patterns	The radix tree containing the patterns
	 * @param level	Level at which to hyphenate
	 * @param options	The hyphenation options
	 * @return the hyphenation breakpoints object
	 */
	private HyphenationBreak hyphenate(String word, Map<HyphenationParser.Level, AhoCorasickTrie<String>> patterns,
			final HyphenationParser.Level level, final HyphenationOptions options){
		//clear already present word boundaries' characters
		word = PatternHelper.clear(word, HyphenationParser.PATTERN_WORD_BOUNDARIES);
		final int wordSize = word.length();

		final String customHyphenation = hypParser.getCustomHyphenations().get(level).get(word);
		final HyphenationBreak hyphBreak;
		if(customHyphenation != null){
			//hyphenation is custom, extract break point positions:
			final String[] hyphenations = customHyphenation.split(HyphenationParser.HYPHEN_EQUALS);
			final Map<Integer, Pair<Integer, String>> indexesAndRules = new HashMap<>(wordSize);
			int charCount = getNormalizedLength(hyphenations[0]);
			for(int i = 1; i < hyphenations.length; i ++){
				final String customRule = hyphenations[i - 1] + "1" + hyphenations[i];
				indexesAndRules.put(charCount, Pair.of(1, customRule));

				charCount += getNormalizedLength(hyphenations[i]);
			}
			hyphBreak = new HyphenationBreak(indexesAndRules, wordSize);
		}
		else if(getNormalizedLength(word) < options.getMinimumLength())
			//ignore short words (early out):
			hyphBreak = new HyphenationBreak(Collections.emptyMap(), wordSize);
		else
			hyphBreak = calculateBreakpoints(word, patterns.get(level), options);

		return hyphBreak;
	}

	private HyphenationBreak calculateBreakpoints(final String word, final AhoCorasickTrie<String> patterns,
			final HyphenationOptions options){
		final String w = HyphenationParser.WORD_BOUNDARY + word + HyphenationParser.WORD_BOUNDARY;

		final int wordSize = word.length();
		final Map<Integer, Pair<Integer, String>> indexesAndRules = new HashMap<>(wordSize);
		if(patterns != null){
			final int normalizedWordSize = getNormalizedLength(word);
			final List<SearchResult<String>> itr = patterns.searchInText(w);
			for(final SearchResult<String> r : itr){
				final String rule = r.getValue();
				final int i = r.getIndexBegin();

				//number of non-letter characters
				final int delta = HyphenationParser.getKeyFromData(rule).length() - HyphenationParser.getKeyFromData(rule).length();

				extractSyllabe(rule, i - delta, word, normalizedWordSize, options, indexesAndRules);
			}
		}

		return new HyphenationBreak(indexesAndRules, wordSize);
	}

	@Override
	public List<String> splitIntoCompounds(final String word){
		final List<String> response;
		if(hypParser.isSecondLevelPresent()){
			//apply first level hyphenation non-compound
			final HyphenationBreak hyphBreak = hyphenate(word, hypParser.getPatterns(), HyphenationParser.Level.NON_COMPOUND,
				hypParser.getOptParser().getNonCompoundOptions());
			response = createHyphenatedWord(word, hyphBreak);

			manageNoHyphen(response);
		}
		else if(hypParser.getPatternNoHyphen() != null)
			//apply retro-compatibility word separators
			response = Arrays.asList(PatternHelper.split(word, hypParser.getPatternNoHyphen()));
		else
			response = Collections.emptyList();
		return response;
	}

	private void manageNoHyphen(final List<String> response){
		for(final String nohyp : hypParser.getOptParser().getNoHyphen()){
			if(isStarting(nohyp))
				manageNoHyphenAtStart(response, nohyp);
			else if(isEnding(nohyp))
				manageNoHyphenAtEnd(response, nohyp);
			else
				manageNoHyphenAtMiddle(response, nohyp);
		}
	}

	private void manageNoHyphenAtStart(final List<String> response, final String nohyp){
		if(response.get(0).equals(nohyp.substring(1)))
			response.remove(0);
	}

	private void manageNoHyphenAtEnd(final List<String> response, final String nohyp){
		if(response.get(response.size() - 1).equals(nohyp.substring(0, nohyp.length() - 1)))
			response.remove(response.size() - 1);
	}

	private void manageNoHyphenAtMiddle(List<String> response, String nohyp){
		Iterator<String> itr = response.iterator();
		while(itr.hasNext())
			if(nohyp.equals(itr.next()))
				itr.remove();
	}

	private boolean isStarting(final String key){
		return (key.charAt(0) == '^');
	}

	private boolean isEnding(final String key){
		return (key.charAt(key.length() - 1) == '$');
	}

	protected List<String> createHyphenatedWord(final String word, final HyphenationBreak hyphBreak){
		final List<String> result = new ArrayList<>();

		int startIndex = 0;
		int after = 0;
		String addAfter = null;
		final int size = word.length();
		for(int endIndex = 0; endIndex < size; endIndex ++)
			if(hyphBreak.isBreakpoint(endIndex)){
				String subword = word.substring(startIndex, endIndex);

				if(StringUtils.isNotBlank(addAfter)){
					//append first characters to next subword
					subword = addAfter + subword.substring(after);
					after = 0;
					addAfter = null;
				}

				//manage augmented patterns:
				final String augmentedPatternData = hyphBreak.getRule(endIndex);
				if(augmentedPatternData != null && HyphenationParser.isAugmentedRule(augmentedPatternData)){
					final int index = HyphenationParser.getIndexOfBreakpoint(PatternHelper.clear(augmentedPatternData,
						HyphenationParser.PATTERN_WORD_INITIAL));

					final Matcher m = HyphenationParser.PATTERN_AUGMENTED_RULE.matcher(augmentedPatternData);
					if(m.find()){
						final String addBefore = m.group(HyphenationParser.PARAM_ADD_BEFORE);
						addAfter = m.group(HyphenationParser.PARAM_ADD_AFTER);
						String start = m.group(HyphenationParser.PARAM_START);
						String cut = m.group(HyphenationParser.PARAM_CUT);
						if(start == null){
							final String rule = m.group(HyphenationParser.PARAM_RULE);
							start = Integer.toString(1);
							cut = Integer.toString(PatternHelper.clear(rule, HyphenationParser.PATTERN_POINTS_AND_NUMBERS).length());
						}

						//remove last characters from subword
						//  ll3a/aa=b,2,2
						//syll able
						//sylaa-bble
						final int delta = index - Integer.parseInt(start) + 1;
						final int end = subword.length() - delta;
						after = Integer.parseInt(cut) - delta;
						subword = subword.substring(0, end) + addBefore;
					}
				}

				result.add(subword);
				startIndex = endIndex;
			}

		String subword = word.substring(startIndex);
		if(StringUtils.isNotBlank(addAfter))
			subword = addAfter + subword.substring(Math.min(Math.max(after, 0), subword.length()));
		result.add(subword);

		return result;
	}

	public static int getAugmentedRuleAddLength(final String rule){
		int length = 0;
		final Matcher m = HyphenationParser.PATTERN_AUGMENTED_RULE.matcher(rule);
		if(m.find()){
			final String addBefore = m.group(HyphenationParser.PARAM_ADD_BEFORE);
			final String basicRule = m.group(HyphenationParser.PARAM_RULE);
			String start = m.group(HyphenationParser.PARAM_START);
			if(start == null)
				start = Integer.toString(1);
			length = addBefore.length() - Integer.parseInt(start) + breakpointIndex(basicRule) - 1;
		}
		return length;
	}

	private static int breakpointIndex(final String rule){
		return StringUtils.indexOfAny(rule, '1', '3', '5', '7', '9');
	}


	//FIXME method signature is awful
	protected Map<Integer, Pair<Integer, String>> extractSyllabe(final String rule, final int startingIndex, final String word,
			final int normalizedWordSize, final HyphenationOptions options, final Map<Integer, Pair<Integer, String>> indexesAndRules){
		final int leftMin = options.getLeftMin();
		final int rightMin = options.getRightMin();

		final int wordSize = word.length();
		final String reducedRule = removeNonStandardPart(rule);

		//cycle the pattern's characters searching for numbers
		//start from -1 since the initial dot has to be skipped
		int j = -1;
		for(final char chr : reducedRule.toCharArray()){
			if(!Character.isDigit(chr))
				j ++;
			else{
				//check if a break point should be skipped based on left and right min options
				final int idx = startingIndex + j;
				final int normalizedIdx = (normalizedWordSize != wordSize? getNormalizedLength(word, idx): idx);
				if(leftMin <= normalizedIdx && normalizedIdx <= normalizedWordSize - rightMin){
					final int dd = Character.digit(chr, 10);
					//check if the break number is great than the one stored so far
					if(dd > indexesAndRules.getOrDefault(idx, HyphenationBreak.EMPTY_PAIR).getKey())
						indexesAndRules.put(idx, Pair.of(dd, rule));
				}
			}
		}
		return indexesAndRules;
	}

	private String removeNonStandardPart(final String rule){
		return PatternHelper.clear(rule, HyphenationParser.PATTERN_REDUCE);
	}

	protected int getNormalizedLength(final String word){
		return Normalizer.normalize(word, Normalizer.Form.NFKC).length();
	}

	private static int getNormalizedLength(final String word, final int index){
		return Normalizer.normalize(word.substring(0, index - 1), Normalizer.Form.NFKC).length() + 1;
	}

}
