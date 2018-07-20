package unit731.hunspeller.parsers.hyphenation.hyphenators;

import java.util.HashMap;
import unit731.hunspeller.parsers.hyphenation.dtos.HyphenationBreak;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunspeller.collections.radixtree.tree.RadixTree;
import unit731.hunspeller.collections.radixtree.tree.SearchResult;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.hyphenation.valueobjects.HyphenationOptions;
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
	protected HyphenationBreak calculateBreakpoints(String word, Map<HyphenationParser.Level, RadixTree<String, String>> patterns, HyphenationParser.Level level, HyphenationOptions options){
		int wordSize = word.length();
		int normalizedWordSize = getNormalizedLength(word);
		Map<Integer, Pair<Integer, String>> indexesAndRules = new HashMap<>(wordSize);
		int leftMin = options.getLeftMin();
		int rightMin = options.getRightMin();
		Iterator<SearchResult<String, String>> itr = patterns.get(level).search(HyphenationParser.WORD_BOUNDARY + word.toLowerCase(Locale.ROOT) + HyphenationParser.WORD_BOUNDARY);
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
						if(dd > indexesAndRules.getOrDefault(idx, HyphenationBreak.EMPTY_PAIR).getKey())
							indexesAndRules.put(idx, Pair.of(dd, rule));
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
							if(leftMin<= idx && idx <= wordSize - rightMin){
								int dd = Character.digit(chr, 10);
								//check if the break number is great than the one stored so far
								if(dd > indexesAndRules.getOrDefault(idx, HyphenationBreak.EMPTY_PAIR).getKey())
									indexesAndRules.put(idx, Pair.of(dd, rl));
							}
						}
					}
				}
		}

		return new HyphenationBreak(indexesAndRules, wordSize);
	}

}
