package unit731.hunspeller.parsers.hyphenation.hyphenators;

import unit731.hunspeller.parsers.hyphenation.dtos.HyphenationBreak;
import unit731.hunspeller.parsers.hyphenation.dtos.Hyphenation;
import unit731.hunspeller.parsers.hyphenation.dtos.HyphenationInterface;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.collections.radixtree.tree.RadixTree;
import unit731.hunspeller.collections.radixtree.tree.SearchResult;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.services.PatternService;


public class AhoCorasickHyphenator extends AbstractHyphenator{

	public AhoCorasickHyphenator(HyphenationParser hypParser){
		super(hypParser);

		preparePatterns();
	}

	private void preparePatterns(){
		for(HyphenationParser.Level level : HyphenationParser.Level.values())
			hypParser.getPatterns().get(level).prepare();
	}

	@Override
	protected HyphenationInterface hyphenate(String word, Map<HyphenationParser.Level, RadixTree<String, String>> patterns, HyphenationParser.Level level, String breakCharacter,
			boolean isCompound){
		boolean[] uppercases = extractUppercases(word);

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

			hyphenatedWord = createHyphenatedWord(word, hyphBreak);

			rules = Arrays.asList(hyphBreak.getRules());
		}
		errors = hypParser.getOrthography().getSyllabationErrors(hyphenatedWord);

		hyphenatedWord = restoreUppercases(hyphenatedWord, uppercases);

		return new Hyphenation(hyphenatedWord, rules, errors, breakCharacter);
	}

	private HyphenationBreak calculateBreakpoints(String word, Map<HyphenationParser.Level, RadixTree<String, String>> patterns, HyphenationParser.Level level, boolean isCompound){
		int wordSize = word.length();
		int normalizedWordSize = getNormalizedLength(word);
		//stores the (maximum) break numbers
		int[] indexes = new int[wordSize];
		//the rules applied to the word
		String[] rules = new String[wordSize];
		//stores the augmented patterns
		String[] augmentedPatternData = new String[wordSize];
		int leftMin = (isCompound? hypParser.getOptions().getLeftCompoundMin(): hypParser.getOptions().getLeftMin());
		int rightMin = (isCompound? hypParser.getOptions().getRightCompoundMin(): hypParser.getOptions().getRightMin());
		Iterator<SearchResult<String, String>> itr = patterns.get(level).search(HyphenationParser.WORD_BOUNDARY + word + HyphenationParser.WORD_BOUNDARY);
		while(itr.hasNext()){
			SearchResult<String, String> r = itr.next();
			String rule = r.getNode().getValue();
			int i = r.getIndex();
			int delta = HyphenationParser.getKeyFromData(rule).length() - r.getNode().getKey().length();

			//remove non–standard part
			String reducedData = PatternService.clear(rule, HyphenationParser.MATCHER_REDUCE);
			int ruleSize = reducedData.length();
			//cycle the pattern's characters searching for numbers
			int j = -1;
			for(int k = 0; k < ruleSize; k ++){
				char chr = reducedData.charAt(k);
				if(!Character.isDigit(chr))
					j ++;
				//check if a break point should be skipped based on left and right min options
				else{
					int idx = i + j - delta;
					int normalizedIdx = (normalizedWordSize != wordSize? getNormalizedLength(word, idx): idx);
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

			List<String> rls = r.getNode().getAdditionalValues();
			if(Objects.nonNull(rls))
				for(String rl : rls){
					//remove non–standard part
					reducedData = PatternService.clear(rl, HyphenationParser.MATCHER_REDUCE);
					ruleSize = reducedData.length();
					//cycle the pattern's characters searching for numbers
					j = -1;
					//cycle the pattern's characters searching for numbers
					for(int k = 0; k < ruleSize; k ++){
						char chr = reducedData.charAt(k);
						if(!Character.isDigit(chr))
							j ++;
						//check if a break point should be skipped based on left and right min options
						else{
							int idx = i + j;
							if(hypParser.getOptions().getLeftMin() <= idx && idx <= wordSize - hypParser.getOptions().getRightMin()){
								int dd = Character.digit(chr, 10);
								//check if the break number is great than the one stored so far
								if(dd > indexes[idx]){
									indexes[idx] = dd;
									rules[idx] = rl;
									augmentedPatternData[idx] = (rl.contains(HyphenationParser.AUGMENTED_RULE)? rl: null);
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
