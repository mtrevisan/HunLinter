package unit731.hunspeller.parsers.hyphenation.hyphenators;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import unit731.hunspeller.parsers.hyphenation.dtos.HyphenationInterface;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
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
		boolean[] uppercases = extractUppercases(word);
		word = word.toLowerCase(Locale.ROOT);

		//apply first level hyphenation
		String breakCharacter = HyphenationParser.SOFT_HYPHEN;
		HyphenationBreak hyphBreak = hyphenate(word, patterns, HyphenationParser.Level.FIRST, breakCharacter, false);

		//if there is a second level
		if(hypParser.hasSecondLevel()){
			//apply second level hyphenation for the word parts
			List<String> syllabes = createHyphenatedWord(word, hyphBreak);
			List<HyphenationBreak> subHyphBreaks = syllabes.stream()
				.map(compound -> hyphenate(compound, patterns, HyphenationParser.Level.SECOND, breakCharacter, true))
				.collect(Collectors.toList());

			hyphBreak = HyphenationBreak.merge(hyphBreak, subHyphBreaks, breakCharacter);
		}

		List<String> hyphenatedWord = createHyphenatedWord(word, hyphBreak);
		boolean changed = hyphBreak.enforceNoHyphens(hyphenatedWord, hypParser.getOptions().getNoHyphen());
		if(changed)
			hyphenatedWord = createHyphenatedWord(word, hyphBreak);
		List<String> rules = Arrays.asList(hyphBreak.getRules());
		boolean[] errors = hypParser.getOrthography().getSyllabationErrors(hyphenatedWord);

		hyphenatedWord = restoreUppercases(hyphenatedWord, uppercases);

		return new Hyphenation(hyphenatedWord, rules, errors, breakCharacter);
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
			int[] indexes = new int[wordSize];
			for(int i = customHyphenation.indexOf(HyphenationParser.HYPHEN_EQUALS); i >= 0; i = customHyphenation.indexOf(HyphenationParser.HYPHEN_EQUALS, i + 1))
				indexes[i] = 1;
			hyphBreak = new HyphenationBreak(indexes);
		}
		else if(Normalizer.normalize(word, Normalizer.Form.NFKC).length() < (isCompound? hypParser.getOptions().getMinimumCompoundLength():
				hypParser.getOptions().getMinimumLength()))
			//ignore short words (early out):
			hyphBreak = new HyphenationBreak(wordSize);
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
				String augmentedPatternData = hyphBreak.getAugmentedPatternData(endIndex);
				if(Objects.nonNull(augmentedPatternData)){
					int index = HyphenationParser.getIndexOfBreakpoint(PatternService.clear(augmentedPatternData, MATCHER_WORD_INITIAL));

					Matcher m = MATCHER_AUGMENTED_RULE.reset(augmentedPatternData);
					m.find();
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

				result.add(subword);
				startIndex = endIndex;
			}

		String subword = word.substring(startIndex);
		if(StringUtils.isNotBlank(addAfter))
			subword = addAfter + subword.substring(Math.min(Math.max(after, 0), subword.length()));
		result.add(subword);

		return result;
	}


	protected static boolean[] extractUppercases(String word){
		int size = word.length();
		boolean[] uppercases = new boolean[size];
		for(int i = 0; i < size; i ++)
			if(Character.isUpperCase(word.charAt(i)))
				uppercases[i] = true;
		return uppercases;
	}

	protected static List<String> restoreUppercases(List<String> hyphenatedWord, boolean[] uppercases){
		int size = uppercases.length;
		for(int i = 0; i < size; i ++)
			if(uppercases[i]){
				int j = i;
				int indexSoFar = 0;
				String syll = hyphenatedWord.get(indexSoFar);
				while(j > syll.length()){
					j -= syll.length();
					indexSoFar ++;
					syll = hyphenatedWord.get(indexSoFar);
				}
				StringBuilder syllabe = new StringBuilder(syll);
				String chr = Character.valueOf(syllabe.charAt(j)).toString();
				syllabe.setCharAt(j, chr.toUpperCase(Locale.ROOT).charAt(0));
				hyphenatedWord.set(indexSoFar, syllabe.toString());
			}
		return hyphenatedWord;
	}

	protected static int getNormalizedLength(String word){
		return Normalizer.normalize(word, Normalizer.Form.NFKC).length();
	}

	protected static int getNormalizedLength(String word, int index){
		return Normalizer.normalize(word.substring(0, index - 1), Normalizer.Form.NFKC).length() + 1;
	}

}
