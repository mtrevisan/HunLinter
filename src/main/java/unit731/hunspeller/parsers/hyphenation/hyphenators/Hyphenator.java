package unit731.hunspeller.parsers.hyphenation.hyphenators;

import unit731.hunspeller.parsers.hyphenation.dtos.HyphenationBreak;
import unit731.hunspeller.parsers.hyphenation.dtos.Hyphenation;
import unit731.hunspeller.parsers.hyphenation.dtos.HyphenationInterface;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.collections.radixtree.tree.RadixTree;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.services.PatternService;


//TODO manage ligatures!
public class Hyphenator extends AbstractHyphenator{

	public Hyphenator(HyphenationParser hypParser){
		super(hypParser);
	}

	@Override
	protected HyphenationInterface hyphenate(String word, Map<HyphenationParser.Level, RadixTree<String, String>> patterns, HyphenationParser.Level level, String breakCharacter,
			boolean isCompound){
		boolean[] uppercases = HyphenationParser.extractUppercases(word);

		//clear already present word boundaries' characters
		word = PatternService.clear(word, MATCHER_WORD_BOUNDARIES);

		List<String> hyphenatedWord;
		List<String> rules;
		boolean[] errors;
		String customHyphenation = hypParser.getCustomHyphenations().get(level).get(word);
		if(Objects.nonNull(customHyphenation)){
			//hyphenation is custom
			hyphenatedWord = Arrays.asList(StringUtils.split(customHyphenation, HyphenationParser.HYPHEN_EQUALS));

			rules = hyphenatedWord;
		}
		else if(Normalizer.normalize(word, Normalizer.Form.NFKC).length() < (isCompound? hypParser.getOptions().getMinimumCompoundLength():
				hypParser.getOptions().getMinimumLength())){
			//ignore short words (early out)
			hyphenatedWord = Arrays.asList(word);

			rules = hyphenatedWord;
		}
		else{
			HyphenationBreak hyphBreak = calculateBreakpoints(word, patterns, level, isCompound);

			hyphenatedWord = HyphenationParser.createHyphenatedWord(word, hyphBreak);

			rules = Arrays.asList(hyphBreak.getRules());
		}
		errors = hypParser.getOrthography().getSyllabationErrors(hyphenatedWord);

		hyphenatedWord = HyphenationParser.restoreUppercases(hyphenatedWord, uppercases);

		return new Hyphenation(hyphenatedWord, rules, errors, breakCharacter);
	}

	private HyphenationBreak calculateBreakpoints(String word, Map<HyphenationParser.Level, RadixTree<String, String>> patterns, HyphenationParser.Level level, boolean isCompound){
		String w = HyphenationParser.WORD_BOUNDARY + word + HyphenationParser.WORD_BOUNDARY;

		int wordSize = word.length();
		int normalizedWordSize = HyphenationParser.getNormalizedLength(word);
		int size = wordSize + HyphenationParser.WORD_BOUNDARY.length() * 2;
		//stores the (maximum) break numbers
		int[] indexes = new int[wordSize];
		//the rules applied to the word
		String[] rules = new String[wordSize];
		//stores the augmented patterns
		String[] augmentedPatternData = new String[wordSize];
		int leftMin = (isCompound? hypParser.getOptions().getLeftCompoundMin(): hypParser.getOptions().getLeftMin());
		int rightMin = (isCompound? hypParser.getOptions().getRightCompoundMin(): hypParser.getOptions().getRightMin());
		for(int i = 0; i < size; i ++){
			//find all the prefixes of w.substring(i)
			List<String> prefixes = patterns.get(level).getValues(w.substring(i));

			for(String rule : prefixes){
				int j = -1;
				//remove non–standard part
				String reducedData = PatternService.clear(rule, HyphenationParser.MATCHER_REDUCE);
				int ruleSize = reducedData.length();
				//cycle the pattern's characters searching for numbers
				for(int k = 0; k < ruleSize; k ++){
					int idx = i + j;
					char chr = reducedData.charAt(k);
					if(!Character.isDigit(chr))
						j ++;
					else{
						//check if a break point should be skipped based on left and right min options
						int normalizedIdx = (normalizedWordSize != wordSize? HyphenationParser.getNormalizedLength(word, idx): idx);
						if(leftMin <= normalizedIdx && normalizedIdx <= normalizedWordSize - rightMin){
							int dd = Character.digit(chr, 10);
							//check if the break number is great than the one stored so far
							if(dd > indexes[idx]){
								indexes[idx] = dd;
								rules[idx] = rule;
								augmentedPatternData[idx] = (HyphenationParser.isAugmentedRule(rule)? rule: null);
							}
						}
					}
				}
			}
		}

		hypParser.enforceNoHyphens(word, indexes, rules, augmentedPatternData);

		return new HyphenationBreak(indexes, rules, augmentedPatternData);
	}

}
