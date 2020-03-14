package unit731.hunlinter.parsers.hyphenation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunlinter.collections.ahocorasicktrie.AhoCorasickTrie;
import unit731.hunlinter.collections.ahocorasicktrie.dtos.SearchResult;
import unit731.hunlinter.services.PatternHelper;


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
	 * NOTE: Calling the method {@link unit731.hunlinter.languages.Orthography#correctOrthography(String)} may be necessary
	 *
	 * @param word	String to hyphenate
	 * @param additionalRule	Rule to add to the set of rules that will generate the hyphenation
	 * @param level	The level to add the rule to
	 * @return	The hyphenation object
	 */
	@Override
	public Hyphenation hyphenate(final String word, final String additionalRule, final HyphenationParser.Level level){
		//FIXME return the rule that matches additionalRule removed by the breakpoints
		final String oldRule = hypParser.addRule(additionalRule, level);

		final Hyphenation hyph = hyphenate(word);
		hypParser.removeRule(additionalRule, level);

		//if there is an already present rule, say, `stu3a` and we overwrite it with, say, `stu4a`,
		//the old `stu3a` is lost and it's necessary to re-add it
		if(oldRule != null)
			hypParser.addRule(oldRule, level);

		return hyph;
	}

	/**
	 * Performs hyphenation
	 * NOTE: Calling the method {@link unit731.hunlinter.languages.Orthography#correctOrthography(String)} may be necessary
	 *
	 * @param word	String to hyphenate
	 * @return the hyphenation object
	 */
	@Override
	public Hyphenation hyphenate(final String word){
		//apply first level hyphenation
		final Map<HyphenationParser.Level, AhoCorasickTrie<String>> patterns = hypParser.getPatterns();
		HyphenationOptions options = hypParser.getOptions().getNonCompoundOptions();
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
				options = hypParser.getOptions().getCompoundOptions();
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
		hyphBreak.enforceNoHyphens(syllabes, hypParser.getOptions().getNoHyphen());

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
			final String[] hyphenations = StringUtils.split(customHyphenation, HyphenationParser.HYPHEN_EQUALS);
			final Map<Integer, Pair<Integer, String>> indexesAndRules = new HashMap<>(wordSize);
			int charCount = getNormalizedLength(hyphenations[0]);
			for(int i = 1; i < hyphenations.length; i ++){
				final String customRule = hyphenations[i - 1] + HyphenationParser.HYPHEN_EQUALS + hyphenations[i];
				indexesAndRules.put(charCount, Pair.of(1, customRule));

				charCount += getNormalizedLength(hyphenations[i]);
			}
			hyphBreak = new HyphenationBreak(indexesAndRules);
		}
		else if(getNormalizedLength(word) < options.getMinimumLength())
			//ignore short words (early out):
			hyphBreak = new HyphenationBreak(Collections.emptyMap());
		else
			hyphBreak = calculateBreakpoints(word, patterns.get(level), options);

		return hyphBreak;
	}

	private HyphenationBreak calculateBreakpoints(final String word, final AhoCorasickTrie<String> patterns,
			final HyphenationOptions options){
		final int wordSize = word.length();
		final Map<Integer, Pair<Integer, String>> indexesAndRules = new HashMap<>(wordSize);
		if(patterns != null){
			final String w = HyphenationParser.WORD_BOUNDARY + word.toLowerCase(Locale.ROOT) + HyphenationParser.WORD_BOUNDARY;
			final int leftMin = options.getLeftMin();
			final int rightMin = options.getRightMin();

			final int normalizedWordSize = getNormalizedLength(word);
			final List<SearchResult<String>> itr = patterns.searchInText(w);
			for(final SearchResult<String> r : itr){
				final String rule = r.getValue();

				//number of non–letter characters
				final int delta = HyphenationParser.getKeyFromData(rule).length() - HyphenationParser.getKeyFromData(rule).length();
				final int startingIndex = r.getIndexBegin() - delta;

				//cycle the pattern's characters searching for numbers
				//start from -1 since the initial dot has to be skipped
				int j = -1;
				final String reducedRule = removeNonStandardPart(rule);
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
			}
		}

		return new HyphenationBreak(indexesAndRules);
	}

	@Override
	public List<String> splitIntoCompounds(final String word){
		final List<String> response;
		if(hypParser.isSecondLevelPresent()){
			//apply first level hyphenation non–compound
			final HyphenationBreak hyphBreak = hyphenate(word, hypParser.getPatterns(), HyphenationParser.Level.NON_COMPOUND,
				hypParser.getOptions().getNonCompoundOptions());
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
		for(final String nohyp : hypParser.getOptions().getNoHyphen()){
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
		response.removeIf(nohyp::equals);
	}

	private boolean isStarting(final String key){
		return (key.charAt(0) == '^');
	}

	private boolean isEnding(final String key){
		return (key.charAt(key.length() - 1) == '$');
	}

	private List<String> createHyphenatedWord(final String word, final HyphenationBreak hyphBreak){
		int startIndex = 0;
		int after = 0;
		String addAfter = null;
		final int size = word.length();
		final ArrayList<String> result = new ArrayList<>(size);
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
		result.trimToSize();

		return result;
	}

	private String removeNonStandardPart(final String rule){
		return PatternHelper.clear(rule, HyphenationParser.PATTERN_REDUCE);
	}

	private int getNormalizedLength(final String word){
		return Normalizer.normalize(word, Normalizer.Form.NFKC).length();
	}

	private int getNormalizedLength(final String word, final int endIndex){
		return Normalizer.normalize(word.substring(0, endIndex - 1), Normalizer.Form.NFKC).length() + 1;
	}

}
