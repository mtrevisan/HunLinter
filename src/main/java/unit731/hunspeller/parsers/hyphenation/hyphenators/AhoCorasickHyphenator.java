package unit731.hunspeller.parsers.hyphenation.hyphenators;

import java.util.HashMap;
import unit731.hunspeller.parsers.hyphenation.dtos.HyphenationBreak;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunspeller.collections.radixtree.tree.RadixTree;
import unit731.hunspeller.collections.radixtree.tree.dtos.SearchResult;
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
		int wordSize = word.length();
		int normalizedWordSize = getNormalizedLength(word);
		Map<Integer, Pair<Integer, String>> indexesAndRules = new HashMap<>(wordSize);
		Iterator<SearchResult<String, String>> itr = patterns.search(HyphenationParser.WORD_BOUNDARY + word.toLowerCase(Locale.ROOT)
			+ HyphenationParser.WORD_BOUNDARY);
		while(itr.hasNext()){
			SearchResult<String, String> r = itr.next();

			String rule = r.getNode().getValue();
			int i = r.getIndex();

			int delta = HyphenationParser.getKeyFromData(rule).length() - r.getNode().getKey().length();
			indexesAndRules = extractSyllabe(rule, i - delta, word, normalizedWordSize, options, indexesAndRules);

			List<String> rules = r.getNode().getAdditionalValues();
			if(rules != null)
				for(String rl : rules)
					indexesAndRules = extractSyllabe(rl, i, word, normalizedWordSize, options, indexesAndRules);
		}

		return new HyphenationBreak(indexesAndRules, wordSize);
	}

}
