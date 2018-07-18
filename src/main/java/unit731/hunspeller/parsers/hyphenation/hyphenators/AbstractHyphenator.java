package unit731.hunspeller.parsers.hyphenation.hyphenators;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import unit731.hunspeller.parsers.hyphenation.dtos.HyphenationInterface;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunspeller.collections.radixtree.tree.RadixTree;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.hyphenation.dtos.Hyphenation;
import unit731.hunspeller.parsers.hyphenation.dtos.HyphenationBreak;
import unit731.hunspeller.services.PatternService;


@AllArgsConstructor
public abstract class AbstractHyphenator implements HyphenatorInterface{

	private static final Matcher MATCHER_AUGMENTED_RULE = PatternService.matcher("^(?<rule>[^/]+)/(?<addBefore>.*?)(?:=|(?<hyphen>.)_)(?<addAfter>[^,]*)(?:,(?<start>\\d+),(?<cut>\\d+))?$");
	private static final Matcher MATCHER_POINTS_AND_NUMBERS = PatternService.matcher("[.\\d]");
	private static final Matcher MATCHER_WORD_INITIAL = PatternService.matcher("^" + Pattern.quote(HyphenationParser.WORD_BOUNDARY));

	protected static final Matcher MATCHER_WORD_BOUNDARIES = PatternService.matcher(Pattern.quote(HyphenationParser.WORD_BOUNDARY));


	@NonNull
	protected final HyphenationParser hypParser;


	/**
	 * Performs hyphenation
	 * NOTE: Calling the method {@link Orthography.#correctOrthography(String)} may be necessary
	 *
	 * @param word	String to hyphenate
	 * @return the hyphenation object(s)
	 */
	@Override
	public HyphenationInterface hyphenate(String word){
		hypParser.acquireLock();

		try{
			return hyphenate(word, hypParser.getPatterns());
		}
		finally{
			hypParser.releaseLock();
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
	 * @throws CloneNotSupportedException	If the radix tree does not support the {@code Cloneable} interface
	 */
	@Override
	public HyphenationInterface hyphenate(String word, String addedRule, HyphenationParser.Level level) throws CloneNotSupportedException{
		hypParser.acquireLock();

		try{
			String key = HyphenationParser.getKeyFromData(addedRule);
			Map<HyphenationParser.Level, RadixTree<String, String>> patterns = hypParser.getPatterns();
			HyphenationInterface hyph = null;
			if(!patterns.get(level).containsKey(key)){
				patterns.get(level).put(key, addedRule);

				hyph = hyphenate(word, patterns);

				patterns.get(level).remove(key);
			}
			return hyph;
		}
		finally{
			hypParser.releaseLock();
		}
	}

	/**
	 * Performs hyphenation
	 *
	 * @param word	String to hyphenate
	 * @param patterns	The radix tree containing the patterns
	 * @return the hyphenation object
	 */
	private HyphenationInterface hyphenate(String word, Map<HyphenationParser.Level, RadixTree<String, String>> patterns){
		//apply first level hyphenation
		String breakCharacter = HyphenationParser.SOFT_HYPHEN;
		HyphenationBreak hyphBreak = hyphenate(word, patterns, HyphenationParser.Level.FIRST, breakCharacter, false);

		List<String> syllabes = createHyphenatedWord(word, hyphBreak);
		List<String> rules = hyphBreak.getRules();

		//if there is a second level
		if(hypParser.hasSecondLevel()){
			List<String> syllabes2ndLevel = new ArrayList<>();
			List<String> rules2ndLevel = new ArrayList<>();

			//apply second level hyphenation for the word parts
			int i = 0;
			int parentRulesSize = rules.size();
			for(String compound : syllabes){
				HyphenationBreak subHyph = hyphenate(compound, patterns, HyphenationParser.Level.SECOND, breakCharacter, true);

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

		boolean[] errors = hypParser.getOrthography().getSyllabationErrors(syllabes);

		return new Hyphenation(syllabes, rules, errors, breakCharacter);
	}

	/**
	 * Performs hyphenation
	 *
	 * @param word	String to hyphenate
	 * @param patterns	The radix tree containing the patterns
	 * @param level	Level at which to hyphenate
	 * @param breakCharacter	Character to add to mark breakpoints
	 * @param isCompound	Whether the word is part of a compounded word
	 * @return the hyphenation breakpoints object
	 */
	private HyphenationBreak hyphenate(String word, Map<HyphenationParser.Level, RadixTree<String, String>> patterns, HyphenationParser.Level level, String breakCharacter,
			boolean isCompound){
		//clear already present word boundaries' characters
		word = PatternService.clear(word, MATCHER_WORD_BOUNDARIES);
		int wordSize = word.length();

		String customHyphenation = hypParser.getCustomHyphenations().get(level).get(word);
		HyphenationBreak hyphBreak;
		if(Objects.nonNull(customHyphenation)){
			//hyphenation is custom, extract break point positions:
			Map<Integer, Pair<Integer, String>> indexesAndRules = new HashMap<>(wordSize);
			for(int i = customHyphenation.indexOf(HyphenationParser.HYPHEN_EQUALS); i >= 0; i = customHyphenation.indexOf(HyphenationParser.HYPHEN_EQUALS, i + 1))
				indexesAndRules.put(i, Pair.of(1, customHyphenation));
			hyphBreak = new HyphenationBreak(indexesAndRules, wordSize);
		}
		else if(Normalizer.normalize(word, Normalizer.Form.NFKC).length() < (isCompound? hypParser.getOptions().getMinimumCompoundLength():
				hypParser.getOptions().getMinimumLength()))
			//ignore short words (early out):
			hyphBreak = new HyphenationBreak(Collections.<Integer, Pair<Integer, String>>emptyMap(), wordSize);
		else
			hyphBreak = calculateBreakpoints(word, patterns, level, isCompound);

		return hyphBreak;
	}

	protected abstract HyphenationBreak calculateBreakpoints(String word, Map<HyphenationParser.Level, RadixTree<String, String>> patterns, HyphenationParser.Level level, boolean isCompound);

	@Override
	public List<String> splitIntoCompounds(String word){
		List<String> response = Collections.<String>emptyList();
		if(hypParser.hasSecondLevel()){
			//apply first level hyphenation non-compound
			HyphenationBreak hyphBreak = hyphenate(word, hypParser.getPatterns(), HyphenationParser.Level.FIRST, HyphenationParser.SOFT_HYPHEN, false);
			response = createHyphenatedWord(word, hyphBreak);
		}
		return response;
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
					int index = HyphenationParser.getIndexOfBreakpoint(PatternService.clear(augmentedPatternData, MATCHER_WORD_INITIAL));

					Matcher m = MATCHER_AUGMENTED_RULE.reset(augmentedPatternData);
					if(m.find()){
						String addBefore = m.group("addBefore");
						addAfter = m.group("addAfter");
						String start = m.group("start");
						String cut = m.group("cut");
						if(Objects.isNull(start)){
							String rule = m.group("rule");
							start = Integer.toString(1);
							cut = Integer.toString(PatternService.clear(rule, MATCHER_POINTS_AND_NUMBERS).length());
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
		Matcher m = MATCHER_AUGMENTED_RULE.reset(rule);
		if(m.find()){
			String addBefore = m.group("addBefore");
			String basicRule = m.group("rule");
			String start = m.group("start");
			if(Objects.isNull(start))
				start = Integer.toString(1);
			length = addBefore.length() - Integer.parseInt(start) + StringUtils.indexOfAny(basicRule, "1", "3", "5", "7", "9") - 1;
		}
		return length;
	}


	protected static int getNormalizedLength(String word){
		return Normalizer.normalize(word, Normalizer.Form.NFKC).length();
	}

	protected static int getNormalizedLength(String word, int index){
		return Normalizer.normalize(word.substring(0, index - 1), Normalizer.Form.NFKC).length() + 1;
	}

}
