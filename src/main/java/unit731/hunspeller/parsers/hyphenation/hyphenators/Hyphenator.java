package unit731.hunspeller.parsers.hyphenation.hyphenators;

import java.util.HashMap;
import unit731.hunspeller.parsers.hyphenation.dtos.HyphenationBreak;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunspeller.collections.ahocorasicktrie.AhoCorasickTrie;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.hyphenation.vos.HyphenationOptions;


class Hyphenator extends AbstractHyphenator{

	Hyphenator(HyphenationParser hypParser, String breakCharacter){
		super(hypParser, breakCharacter);
	}

	@Override
	protected HyphenationBreak calculateBreakpoints(String word, AhoCorasickTrie<String> patterns, HyphenationOptions options){
		String w = HyphenationParser.WORD_BOUNDARY + word + HyphenationParser.WORD_BOUNDARY;

		int wordSize = word.length();
		int normalizedWordSize = getNormalizedLength(word);
		Map<Integer, Pair<Integer, String>> indexesAndRules = new HashMap<>(wordSize);
		int size = wordSize + HyphenationParser.WORD_BOUNDARY.length() * 2;
		for(int i = 0; i < size; i ++){
			//find all the prefixes of w.substring(i)
			List<AhoCorasickTrie.Hit<String>> prefixes = patterns.searchInText(w.substring(i).toLowerCase(Locale.ROOT));

			for(AhoCorasickTrie.Hit<String> rule : prefixes)
				indexesAndRules = extractSyllabe(rule.value, i, word, normalizedWordSize, options, indexesAndRules);
		}

		return new HyphenationBreak(indexesAndRules, wordSize);
	}

}
