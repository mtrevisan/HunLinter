package unit731.hunspeller.parsers.hyphenation;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.collections.radixtree.tree.RadixTree;
import unit731.hunspeller.collections.radixtree.tree.SearchResult;
import unit731.hunspeller.services.PatternService;


@Slf4j
public class AhoCorasickHyphenationParser extends AbstractHyphenationParser{

	private static final Matcher MATCHER_WORD_BOUNDARIES = PatternService.matcher(Pattern.quote(WORD_BOUNDARY));


	public AhoCorasickHyphenationParser(String language){
		super(language);
	}

	AhoCorasickHyphenationParser(String language, Map<Level, RadixTree<String, String>> patterns, Map<Level, Map<String, String>> customHyphenations, HyphenationOptions options){
		super(language, patterns, customHyphenations, options);

		preparePatterns();
	}

	@Override
	protected void postParsingInitialization(){
		preparePatterns();
	}

	private void preparePatterns(){
		for(Level level : Level.values())
			patterns.get(level).prepare();
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
		int wordSize = word.length();
		int normalizedWordSize = getNormalizedLength(word);
		//stores the (maximum) break numbers
		int[] indexes = new int[wordSize];
		//the rules applied to the word
		String[] rules = new String[wordSize];
		//stores the augmented patterns
		String[] augmentedPatternData = new String[wordSize];
		int leftMin = (isCompound? options.getLeftCompoundMin(): options.getLeftMin());
		int rightMin = (isCompound? options.getRightCompoundMin(): options.getRightMin());
		Iterator<SearchResult<String, String>> itr = patterns.get(level).search(WORD_BOUNDARY + word + WORD_BOUNDARY);
		while(itr.hasNext()){
			SearchResult<String, String> r = itr.next();
			String rule = r.getNode().getValue();
			int i = r.getIndex();
			int delta = getKeyFromData(rule).length() - r.getNode().getKey().length();

			//remove non–standard part
			String reducedData = PatternService.clear(rule, MATCHER_REDUCE);
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
							augmentedPatternData[idx] = (isAugmentedRule(rule)? rule: null);
						}
					}
				}
			}

			List<String> rls = r.getNode().getAdditionalValues();
			if(Objects.nonNull(rls))
				for(String rl : rls){
					//remove non–standard part
					reducedData = PatternService.clear(rl, MATCHER_REDUCE);
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
							if(options.getLeftMin() <= idx && idx <= wordSize - options.getRightMin()){
								int dd = Character.digit(chr, 10);
								//check if the break number is great than the one stored so far
								if(dd > indexes[idx]){
									indexes[idx] = dd;
									rules[idx] = rl;
									augmentedPatternData[idx] = (rl.contains(AUGMENTED_RULE)? rl: null);
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
