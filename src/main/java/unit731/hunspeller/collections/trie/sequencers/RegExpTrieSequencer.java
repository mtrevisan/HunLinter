package unit731.hunspeller.collections.trie.sequencers;

import java.util.regex.Matcher;
import unit731.hunspeller.services.PatternService;



public class RegExpTrieSequencer implements TrieSequencer<String>{

	@Override
	public int hashOf(String sequence, int index){
		return sequence.charAt(index);
	}

	/* Find the Longest Common Prefix length */
	@Override
	public int matches(String sequenceA, int indexA, String sequenceB, int indexB, int maxCount){
		Matcher matcher = PatternService.matcher(sequenceA, sequenceB);
		return matcher.end();
	}

}
