package unit731.hunspeller.parsers.hyphenation.hyphenators;

import java.util.HashMap;
import unit731.hunspeller.parsers.hyphenation.dtos.HyphenationBreak;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunspeller.collections.radixtree.tree.RadixTree;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.hyphenation.vos.HyphenationOptions;


public class Hyphenator extends AbstractHyphenator{

	public Hyphenator(HyphenationParser hypParser, String breakCharacter){
		super(hypParser, breakCharacter);
	}

	@Override
	protected HyphenationBreak calculateBreakpoints(String word, RadixTree<String, String> patterns, HyphenationOptions options){
		String w = HyphenationParser.WORD_BOUNDARY + word + HyphenationParser.WORD_BOUNDARY;

		int wordSize = word.length();
		int normalizedWordSize = getNormalizedLength(word);
		int size = wordSize + HyphenationParser.WORD_BOUNDARY.length() * 2;
		Map<Integer, Pair<Integer, String>> indexesAndRules = new HashMap<>(wordSize);
		for(int i = 0; i < size; i ++){
			//find all the prefixes of w.substring(i)
			List<String> prefixes = patterns.getValues(w.substring(i).toLowerCase(Locale.ROOT));

			for(String rule : prefixes)
				indexesAndRules = extractSyllabe(rule, i, word, normalizedWordSize, options, indexesAndRules);
		}

		return new HyphenationBreak(indexesAndRules, wordSize);
	}

}
