package unit731.hunspeller.parsers.hyphenation;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.collections.radixtree.tree.RadixTree;
import unit731.hunspeller.services.PatternService;


@Slf4j
public class HyphenationParser extends AbstractHyphenationParser{

	private static final Matcher MATCHER_WORD_BOUNDARIES = PatternService.matcher(Pattern.quote(WORD_BOUNDARY));


	public HyphenationParser(String language){
		super(language);
	}

	HyphenationParser(String language, Map<Level, RadixTree<String, String>> patterns, Map<Level, Map<String, String>> customHyphenations, HyphenationOptions options){
		super(language, patterns, customHyphenations, options);
	}

	@Override
	protected HyphenationInterface hyphenate(String word, Map<Level, RadixTree<String, String>> patterns, Level level, String breakCharacter,
			boolean isCompound){
		boolean[] uppercases = extractUppercases(word);

		//clear already present word boundaries' characters
		word = PatternService.clear(word, MATCHER_WORD_BOUNDARIES);

		List<String> hyphenatedWord;
		List<String> rules;
		boolean[] errors;
		String customHyphenation = customHyphenations.get(level).get(word);
		if(Objects.nonNull(customHyphenation)){
			//hyphenation is custom
			hyphenatedWord = Arrays.asList(StringUtils.split(customHyphenation, HYPHEN_EQUALS));

			rules = hyphenatedWord;
		}
		else if(Normalizer.normalize(word, Normalizer.Form.NFKC).length() < (isCompound? options.getMinimumCompoundLength():
				options.getMinimumLength())){
			//ignore short words (early out)
			hyphenatedWord = Arrays.asList(word);

			rules = hyphenatedWord;
		}
		else{
			HyphenationBreak hyphBreak = calculateBreakpoints(word, patterns, level, isCompound);

			hyphenatedWord = createHyphenatedWord(word, hyphBreak);

			rules = Arrays.asList(hyphBreak.getRules());
		}
		errors = orthography.getSyllabationErrors(hyphenatedWord);

		hyphenatedWord = restoreUppercases(hyphenatedWord, uppercases);

		return new Hyphenation(hyphenatedWord, rules, errors, breakCharacter);
	}

	private HyphenationBreak calculateBreakpoints(String word, Map<Level, RadixTree<String, String>> patterns, Level level, boolean isCompound){
		String w = WORD_BOUNDARY + word + WORD_BOUNDARY;

		int wordSize = word.length();
		int normalizedWordSize = getNormalizedLength(word);
		int size = wordSize + WORD_BOUNDARY.length() * 2;
		//stores the (maximum) break numbers
		int[] indexes = new int[wordSize];
		//the rules applied to the word
		String[] rules = new String[wordSize];
		//stores the augmented patterns
		String[] augmentedPatternData = new String[wordSize];
		int leftMin = (isCompound? options.getLeftCompoundMin(): options.getLeftMin());
		int rightMin = (isCompound? options.getRightCompoundMin(): options.getRightMin());
		for(int i = 0; i < size; i ++){
			//find all the prefixes of w.substring(i)
			List<String> prefixes = patterns.get(level).getValues(w.substring(i));

			for(String rule : prefixes){
				int j = -1;
				//remove non–standard part
				String reducedData = PatternService.clear(rule, MATCHER_REDUCE);
				int ruleSize = reducedData.length();
				//cycle the pattern's characters searching for numbers
				for(int k = 0; k < ruleSize; k ++){
					int idx = i + j;
					char chr = reducedData.charAt(k);
					if(!Character.isDigit(chr))
						j ++;
					else{
						//check if a break point should be skipped based on left and right min options
						int normalizedIdx = (normalizedWordSize != wordSize? getNormalizedLength(word, idx): idx);
						if(leftMin <= normalizedIdx && normalizedIdx <= normalizedWordSize - rightMin){
							int dd = Character.digit(chr, 10);
							//check if the break number is great than the one stored so far
							if(dd > indexes[idx]){
								indexes[idx] = dd;
								rules[idx] = rule;
								augmentedPatternData[idx] = (isAugmentedRule(rule)? rule: null);
							}
						}
					}
				}
			}
		}

		enforceNoHyphens(word, indexes, rules, augmentedPatternData);

		return new HyphenationBreak(indexes, rules, augmentedPatternData);
	}

}
