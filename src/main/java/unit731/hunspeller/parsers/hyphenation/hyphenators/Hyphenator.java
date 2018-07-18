package unit731.hunspeller.parsers.hyphenation.hyphenators;

import java.util.HashMap;
import unit731.hunspeller.parsers.hyphenation.dtos.HyphenationBreak;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunspeller.collections.radixtree.tree.RadixTree;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.services.PatternService;


public class Hyphenator extends AbstractHyphenator{

	public Hyphenator(HyphenationParser hypParser){
		super(hypParser);
	}

	@Override
	protected HyphenationBreak calculateBreakpoints(String word, Map<HyphenationParser.Level, RadixTree<String, String>> patterns, HyphenationParser.Level level, boolean isCompound){
		String w = HyphenationParser.WORD_BOUNDARY + word + HyphenationParser.WORD_BOUNDARY;

		int wordSize = word.length();
		int normalizedWordSize = getNormalizedLength(word);
		int size = wordSize + HyphenationParser.WORD_BOUNDARY.length() * 2;
		Map<Integer, Pair<Integer, String>> indexesAndRules = new HashMap<>(wordSize);
		int leftMin = (isCompound? hypParser.getOptions().getLeftCompoundMin(): hypParser.getOptions().getLeftMin());
		int rightMin = (isCompound? hypParser.getOptions().getRightCompoundMin(): hypParser.getOptions().getRightMin());
		for(int i = 0; i < size; i ++){
			//find all the prefixes of w.substring(i)
			List<String> prefixes = patterns.get(level).getValues(w.substring(i).toLowerCase(Locale.ROOT));

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
						int normalizedIdx = (normalizedWordSize != wordSize? getNormalizedLength(word, idx): idx);
						if(leftMin <= normalizedIdx && normalizedIdx <= normalizedWordSize - rightMin){
							int dd = Character.digit(chr, 10);
							//check if the break number is great than the one stored so far
							if(dd > indexesAndRules.getOrDefault(idx, HyphenationBreak.EMPTY_PAIR).getKey())
								indexesAndRules.put(idx, Pair.of(dd, rule));
						}
					}
				}
			}
		}

		return new HyphenationBreak(indexesAndRules, wordSize);
	}

}
