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
package io.github.mtrevisan.hunlinter.parsers.hyphenation;

import io.github.mtrevisan.hunlinter.datastructures.ahocorasicktrie.AhoCorasickTrie;
import io.github.mtrevisan.hunlinter.datastructures.ahocorasicktrie.dtos.SearchResult;
import io.github.mtrevisan.hunlinter.languages.Orthography;
import io.github.mtrevisan.hunlinter.services.RegexHelper;
import io.github.mtrevisan.hunlinter.workers.core.IndexDataPair;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;


public class Hyphenator implements HyphenatorInterface{

	private final HyphenationParser hypParser;
	private final String breakCharacter;


	public Hyphenator(final HyphenationParser hypParser, final String breakCharacter){
		Objects.requireNonNull(hypParser, "Hyphenation parser cannot be null");
		Objects.requireNonNull(breakCharacter, "Break character cannot be null");

		this.hypParser = hypParser;
		this.breakCharacter = breakCharacter;
	}

	/**
	 * Performs hyphenation including an additional rule
	 * NOTE: Calling the method {@link Orthography#correctOrthography(String)} may be necessary
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
		//the old `stu3a` is lost, and it's necessary to re-add it
		if(oldRule != null)
			hypParser.addRule(oldRule, level);

		return hyph;
	}

	/**
	 * Performs hyphenation
	 * NOTE: Calling the method {@link Orthography#correctOrthography(String)} may be necessary
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
		List<String> syllabes = new ArrayList<>(compounds);
		String[] rules = hyphBreak.getRules();

		//if there is a second level
		if(hypParser.isSecondLevelPresent()){
			final List<String> syllabes2ndLevel = new ArrayList<>(0);
			String[] rules2ndLevel = new String[0];

			//apply second level hyphenation for the word parts
			int i = 0;
			final int parentRulesSize = rules.length;
			for(final String compound : compounds){
				options = hypParser.getOptions().getCompoundOptions();
				final HyphenationBreak subHyph = hyphenate(compound, patterns, HyphenationParser.Level.COMPOUND, options);

				syllabes2ndLevel.addAll(createHyphenatedWord(compound, subHyph));
				rules2ndLevel = ArrayUtils.addAll(rules2ndLevel, subHyph.getRules());
				if(i < parentRulesSize)
					rules2ndLevel = ArrayUtils.add(rules2ndLevel, rules[i ++]);
			}

			syllabes = syllabes2ndLevel;
			rules = rules2ndLevel;
		}

		//enforce no-hyphens
		hyphBreak.enforceNoHyphens(syllabes, hypParser.getOptions().getNoHyphen());

		return new Hyphenation(syllabes.toArray(new String[0]), compounds.toArray(new String[0]), rules, breakCharacter);
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
	private HyphenationBreak hyphenate(String word, final Map<HyphenationParser.Level, AhoCorasickTrie<String>> patterns,
			final HyphenationParser.Level level, final HyphenationOptions options){
		//clear already present word boundaries' characters
		word = RegexHelper.clear(word, HyphenationParser.PATTERN_WORD_BOUNDARIES);
		final int wordSize = word.length();

		final String customHyphenation = hypParser.getCustomHyphenations().get(level).get(word);
		final HyphenationBreak hyphBreak;
		if(customHyphenation != null){
			//hyphenation is custom, extract break point positions:
			final String[] hyphenations = StringUtils.split(customHyphenation, HyphenationParser.EQUALS_SIGN);
			final Map<Integer, IndexDataPair<String>> indexesAndRules = new HashMap<>(wordSize);
			int charCount = getNormalizedLength(hyphenations[0]);
			for(int i = 1; i < hyphenations.length; i ++){
				final String customRule = hyphenations[i - 1] + HyphenationParser.EQUALS_SIGN + hyphenations[i];
				indexesAndRules.put(charCount, IndexDataPair.of(1, customRule));

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
		final Map<Integer, IndexDataPair<String>> indexesAndRules = new HashMap<>(wordSize);
		if(patterns != null){
			final String w = HyphenationParser.WORD_BOUNDARY + word.toLowerCase(Locale.ROOT) + HyphenationParser.WORD_BOUNDARY;
			final int leftMin = options.getLeftMin();
			final int rightMin = options.getRightMin();

			final int normalizedWordSize = getNormalizedLength(word);
			final List<SearchResult<String>> itr = patterns.searchInText(w);
			for(final SearchResult<String> r : itr){
				final String rule = r.getValue();

				//number of non-letter characters
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
							if(dd > indexesAndRules.getOrDefault(idx, HyphenationBreak.EMPTY_PAIR).getIndex())
								indexesAndRules.put(idx, IndexDataPair.of(dd, rule));
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
			//apply first level hyphenation non-compound
			final HyphenationBreak hyphBreak = hyphenate(word, hypParser.getPatterns(), HyphenationParser.Level.NON_COMPOUND,
				hypParser.getOptions().getNonCompoundOptions());
			response = createHyphenatedWord(word, hyphBreak);

			manageNoHyphen(response);
		}
		else if(hypParser.getPatternNoHyphen() != null)
			//apply retro-compatibility word separators
			response = Arrays.asList(RegexHelper.split(word, hypParser.getPatternNoHyphen()));
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

	private void manageNoHyphenAtMiddle(final List<String> response, final String nohyp){
		final Iterator<String> itr = response.iterator();
		while(itr.hasNext())
			if(nohyp.equals(itr.next()))
				itr.remove();
	}

	private boolean isStarting(final CharSequence key){
		return (key.charAt(0) == '^');
	}

	private boolean isEnding(final CharSequence key){
		return (key.charAt(key.length() - 1) == '$');
	}

	private List<String> createHyphenatedWord(final String word, final HyphenationBreak hyphBreak){
		int startIndex = 0;
		int after = 0;
		String addAfter = null;
		final int size = word.length();
		final List<String> result = new ArrayList<>(0);
		final Matcher m = RegexHelper.matcher(StringUtils.EMPTY, HyphenationParser.PATTERN_AUGMENTED_RULE);
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
					final int index = HyphenationParser.getIndexOfBreakpoint(RegexHelper.clear(augmentedPatternData,
						HyphenationParser.PATTERN_WORD_INITIAL));

					m.reset(augmentedPatternData);
					if(m.find()){
						final String addBefore = m.group(HyphenationParser.PARAM_ADD_BEFORE);
						addAfter = m.group(HyphenationParser.PARAM_ADD_AFTER);
						String start = m.group(HyphenationParser.PARAM_START);
						String cut = m.group(HyphenationParser.PARAM_CUT);
						if(start == null){
							final String rule = m.group(HyphenationParser.PARAM_RULE);
							start = Integer.toString(1);
							cut = Integer.toString(RegexHelper.clear(rule, HyphenationParser.PATTERN_POINTS_AND_NUMBERS).length());
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

	private String removeNonStandardPart(final CharSequence rule){
		return RegexHelper.clear(rule, HyphenationParser.PATTERN_REDUCE);
	}

	private int getNormalizedLength(final CharSequence word){
		return Normalizer.normalize(word, Normalizer.Form.NFKC).length();
	}

	private int getNormalizedLength(final String word, final int endIndex){
		return Normalizer.normalize(word.substring(0, endIndex - 1), Normalizer.Form.NFKC).length() + 1;
	}

}
