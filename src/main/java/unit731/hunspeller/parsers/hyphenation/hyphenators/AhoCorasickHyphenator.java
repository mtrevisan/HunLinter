package unit731.hunspeller.parsers.hyphenation.hyphenators;

import java.util.HashMap;
import unit731.hunspeller.parsers.hyphenation.dtos.HyphenationBreak;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunspeller.collections.radixtree.RadixTree;
import unit731.hunspeller.collections.radixtree.dtos.SearchResult;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.hyphenation.vos.HyphenationOptions;


class AhoCorasickHyphenator extends AbstractHyphenator{

	AhoCorasickHyphenator(HyphenationParser hypParser, String breakCharacter){
		super(hypParser, breakCharacter);

		preparePatterns();
	}

	private void preparePatterns(){
		for(HyphenationParser.Level level : HyphenationParser.Level.values())
			hypParser.getPatterns().get(level).prepare();
	}

	@Override
	protected HyphenationBreak calculateBreakpoints(String word, RadixTree<String, String> patterns, HyphenationOptions options){
		String w = HyphenationParser.WORD_BOUNDARY + word.toLowerCase(Locale.ROOT) + HyphenationParser.WORD_BOUNDARY;

		int wordSize = word.length();
		int normalizedWordSize = getNormalizedLength(word);
		Map<Integer, Pair<Integer, String>> indexesAndRules = new HashMap<>(wordSize);
		Iterator<SearchResult<String, String>> itr = patterns.searchPrefixedBy(w);
		while(itr.hasNext()){
			SearchResult<String, String> r = itr.next();

			String rule = r.getNode().getValue();
			int i = r.getIndex();

			int delta = HyphenationParser.getKeyFromData(rule).length() - r.getNode().getKey().length();
			indexesAndRules = extractSyllabe(rule, i - delta, word, normalizedWordSize, options, indexesAndRules);

			indexesAndRules = manageAdditionalValues(r.getNode().getAdditionalValues(), i, word, normalizedWordSize, options, indexesAndRules);
		}

		return new HyphenationBreak(indexesAndRules, wordSize);
	}

	private Map<Integer, Pair<Integer, String>> manageAdditionalValues(List<String> rules, int i, String word, int normalizedWordSize, HyphenationOptions options,
			Map<Integer, Pair<Integer, String>> indexesAndRules){
		if(rules != null)
			for(String rl : rules){
				String key = HyphenationParser.getKeyFromData(rl);
				int ruleLastIndex = key.length() - 1;
				boolean endingKey = (rl.charAt(ruleLastIndex) == '.');
				if(!endingKey && word.substring(i - HyphenationParser.WORD_BOUNDARY.length()).startsWith(key)
						|| endingKey && word.endsWith(key.substring(0, ruleLastIndex)))
					indexesAndRules = extractSyllabe(rl, i, word, normalizedWordSize, options, indexesAndRules);
			}
		return indexesAndRules;
	}

}
