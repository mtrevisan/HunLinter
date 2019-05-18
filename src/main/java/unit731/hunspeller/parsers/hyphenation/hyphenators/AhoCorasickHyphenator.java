package unit731.hunspeller.parsers.hyphenation.hyphenators;

import java.util.HashMap;
import unit731.hunspeller.parsers.hyphenation.dtos.HyphenationBreak;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunspeller.collections.ahocorasicktrie.AhoCorasickTrie;
import unit731.hunspeller.collections.ahocorasicktrie.dtos.SearchResult;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.hyphenation.vos.HyphenationOptions;


class AhoCorasickHyphenator extends AbstractHyphenator{

	AhoCorasickHyphenator(HyphenationParser hypParser, String breakCharacter){
		super(hypParser, breakCharacter);
	}

	@Override
	protected HyphenationBreak calculateBreakpoints(String word, AhoCorasickTrie<String> patterns, HyphenationOptions options){
		String w = HyphenationParser.WORD_BOUNDARY + word.toLowerCase(Locale.ROOT) + HyphenationParser.WORD_BOUNDARY;

		int wordSize = word.length();
		int normalizedWordSize = getNormalizedLength(word);
		Map<Integer, Pair<Integer, String>> indexesAndRules = new HashMap<>(wordSize);
		List<SearchResult<String>> itr = patterns.searchInText(w);
		for(SearchResult<String> r : itr){
			String rule = r.getValue();
			int i = r.getIndexBegin();

			//number of non-letter characters
			int delta = HyphenationParser.getKeyFromData(rule).length() - HyphenationParser.getKeyFromData(rule).length();
			indexesAndRules = extractSyllabe(rule, i - delta, word, normalizedWordSize, options, indexesAndRules);
		}

		return new HyphenationBreak(indexesAndRules, wordSize);
	}

}
