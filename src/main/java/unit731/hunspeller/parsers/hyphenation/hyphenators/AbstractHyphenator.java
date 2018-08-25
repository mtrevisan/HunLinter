package unit731.hunspeller.parsers.hyphenation.hyphenators;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunspeller.collections.radixtree.tree.RadixTree;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.hyphenation.dtos.Hyphenation;
import unit731.hunspeller.parsers.hyphenation.dtos.HyphenationBreak;
import unit731.hunspeller.parsers.hyphenation.valueobjects.HyphenationOptions;
import unit731.hunspeller.services.PatternHelper;


@AllArgsConstructor
public abstract class AbstractHyphenator implements HyphenatorInterface{

	@NonNull
	protected final HyphenationParser hypParser;
	@NonNull
	private final String breakCharacter;


	/**
	 * Performs hyphenation
	 * NOTE: Calling the method {@link Orthography.#correctOrthography(String)} may be necessary
	 *
	 * @param word	String to hyphenate
	 * @return the hyphenation object(s)
	 */
	@Override
	public Hyphenation hyphenate(String word){
		hypParser.acquireReadLock();
		try{
			return hyphenate(word, hypParser.getPatterns());
		}
		finally{
			hypParser.releaseReadLock();
		}
	}

	/**
	 * Performs hyphenation including an additional rule
	 * NOTE: Calling the method {@link Orthography#correctOrthography(String)} may be necessary
	 *
	 * @param word	String to hyphenate
	 * @param addedRule	Rule to add to the set of rules that will generate the hyphenation
	 * @param level	The level to add the rule to
	 * @return the hyphenation object
	 */
	@Override
	public Hyphenation hyphenate(String word, String addedRule, HyphenationParser.Level level){
		hypParser.acquireReadLock();
		try{
			String key = HyphenationParser.getKeyFromData(addedRule);
			Map<HyphenationParser.Level, RadixTree<String, String>> patterns = hypParser.getPatterns();
			Hyphenation hyph = null;
			if(!patterns.get(level).containsKey(key)){
				patterns.get(level).put(key, addedRule);

				hyph = hyphenate(word, patterns);

				patterns.get(level).remove(key);
			}
			return hyph;
		}
		finally{
			hypParser.releaseReadLock();
		}
	}

	/**
	 * Performs hyphenation
	 *
	 * @param word	String to hyphenate
	 * @param patterns	The radix tree containing the patterns
	 * @return the hyphenation object
	 */
	private Hyphenation hyphenate(String word, Map<HyphenationParser.Level, RadixTree<String, String>> patterns){
		//apply first level hyphenation
		HyphenationBreak hyphBreak = hyphenate(word, patterns, HyphenationParser.Level.NON_COMPOUND, hypParser.getOptParser().getNonCompoundOptions());

		List<String> compounds = createHyphenatedWord(word, hyphBreak);
		List<String> syllabes = compounds;
		List<String> rules = hyphBreak.getRules();

		//if there is a second level
		if(hypParser.isSecondLevelPresent()){
			List<String> syllabes2ndLevel = new ArrayList<>();
			List<String> rules2ndLevel = new ArrayList<>();

			//apply second level hyphenation for the word parts
			int i = 0;
			int parentRulesSize = rules.size();
			for(String compound : compounds){
				HyphenationBreak subHyph = hyphenate(compound, patterns, HyphenationParser.Level.COMPOUND, hypParser.getOptParser().getCompoundOptions());

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

		boolean[] errors = hypParser.getOrthography().getSyllabationErrors(syllabes);

		return new Hyphenation(syllabes, compounds, rules, errors, breakCharacter);
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
	private HyphenationBreak hyphenate(String word, Map<HyphenationParser.Level, RadixTree<String, String>> patterns, HyphenationParser.Level level, HyphenationOptions options){
		//clear already present word boundaries' characters
		word = PatternHelper.clear(word, HyphenationParser.MATCHER_WORD_BOUNDARIES);
		int wordSize = word.length();

		String customHyphenation = hypParser.getCustomHyphenations().get(level).get(word);
		HyphenationBreak hyphBreak;
		if(customHyphenation != null){
			//hyphenation is custom, extract break point positions:
			Map<Integer, Pair<Integer, String>> indexesAndRules = new HashMap<>(wordSize);
			for(int i = customHyphenation.indexOf('='); i >= 0; i = customHyphenation.indexOf('=', i + 1))
				indexesAndRules.put(i, Pair.of(1, customHyphenation));
			hyphBreak = new HyphenationBreak(indexesAndRules, wordSize);
		}
		else if(Normalizer.normalize(word, Normalizer.Form.NFKC).length() < options.getMinimumLength())
			//ignore short words (early out):
			hyphBreak = new HyphenationBreak(Collections.<Integer, Pair<Integer, String>>emptyMap(), wordSize);
		else
			hyphBreak = calculateBreakpoints(word, patterns.get(level), options);

		return hyphBreak;
	}

	protected abstract HyphenationBreak calculateBreakpoints(String word, RadixTree<String, String> patterns, HyphenationOptions options);

	@Override
	public List<String> splitIntoCompounds(String word){
		hypParser.acquireReadLock();
		try{
			List<String> response;
			if(hypParser.isSecondLevelPresent()){
				//apply first level hyphenation non-compound
				HyphenationBreak hyphBreak = hyphenate(word, hypParser.getPatterns(), HyphenationParser.Level.NON_COMPOUND, hypParser.getOptParser().getNonCompoundOptions());
				response = createHyphenatedWord(word, hyphBreak);

				for(String nohyp : hypParser.getOptParser().getNoHyphen()){
					int nohypLength = nohyp.length();
					if(nohyp.charAt(0) == '^'){
						if(response.get(0).equals(nohyp.substring(1)))
							response.remove(0);
					}
					else if(nohyp.charAt(nohypLength - 1) == '$'){
						if(response.get(response.size() - 1).equals(nohyp.substring(0, nohypLength - 1)))
							response.remove(response.size() - 1);
					}
					else{
						Iterator<String> itr = response.iterator();
						while(itr.hasNext())
							if(nohyp.equals(itr.next()))
								itr.remove();
					}
				}
			}
			else if(hypParser.getPatternNoHyphen() != null)
				//apply retro-compatibility word separators
				response = Arrays.asList(PatternHelper.split(word, hypParser.getPatternNoHyphen()));
			else
				response = Collections.<String>emptyList();
			return response;
		}
		finally{
			hypParser.releaseReadLock();
		}
	}

	protected List<String> createHyphenatedWord(String word, HyphenationBreak hyphBreak){
		List<String> result = new ArrayList<>();

		int startIndex = 0;
		int size = word.length();
		int after = 0;
		String addAfter = null;
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
				String augmentedPatternData = hyphBreak.getRule(endIndex);
				if(augmentedPatternData != null && HyphenationParser.isAugmentedRule(augmentedPatternData)){
					int index = HyphenationParser.getIndexOfBreakpoint(PatternHelper.clear(augmentedPatternData, HyphenationParser.MATCHER_WORD_INITIAL));

					Matcher m = HyphenationParser.MATCHER_AUGMENTED_RULE.reset(augmentedPatternData);
					if(m.find()){
						String addBefore = m.group(HyphenationParser.PARAM_ADD_BEFORE);
						addAfter = m.group(HyphenationParser.PARAM_ADD_AFTER);
						String start = m.group(HyphenationParser.PARAM_START);
						String cut = m.group(HyphenationParser.PARAM_CUT);
						if(start == null){
							String rule = m.group(HyphenationParser.PARAM_RULE);
							start = Integer.toString(1);
							cut = Integer.toString(PatternHelper.clear(rule, HyphenationParser.MATCHER_POINTS_AND_NUMBERS).length());
						}

						//remove last characters from subword
						//  ll3a/aa=b,2,2
						//syll able
						//sylaa-bble
						int delta = index - Integer.parseInt(start) + 1;
						int end = subword.length() - delta;
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

	public static int getAugmentedRuleAddLength(String rule){
		int length = 0;
		Matcher m = HyphenationParser.MATCHER_AUGMENTED_RULE.reset(rule);
		if(m.find()){
			String addBefore = m.group(HyphenationParser.PARAM_ADD_BEFORE);
			String basicRule = m.group(HyphenationParser.PARAM_RULE);
			String start = m.group(HyphenationParser.PARAM_START);
			if(start == null)
				start = Integer.toString(1);
			length = addBefore.length() - Integer.parseInt(start) + breakpointIndex(basicRule) - 1;
		}
		return length;
	}

	private static int breakpointIndex(String rule){
		return StringUtils.indexOfAny(rule, '1', '3', '5', '7', '9');
	}


	protected static int getNormalizedLength(String word){
		return Normalizer.normalize(word, Normalizer.Form.NFKC).length();
	}

	protected static int getNormalizedLength(String word, int index){
		return Normalizer.normalize(word.substring(0, index - 1), Normalizer.Form.NFKC).length() + 1;
	}

}
