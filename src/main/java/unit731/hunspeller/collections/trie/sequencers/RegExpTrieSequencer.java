package unit731.hunspeller.collections.trie.sequencers;

import java.util.regex.Pattern;
import unit731.hunspeller.services.PatternService;


public class RegExpTrieSequencer implements TrieSequencer<String>{

	private static final Pattern REGEX_PATTERN = PatternService.pattern("(?<!\\[\\^?)(?!\\])");

	private static final char CIRCUMFLEX = '^';


	@Override
	public int hashOf(String sequence, int index){
		return sequence.charAt(index);
	}

	@Override
	public int matches(String sequenceA, int indexA, String sequenceB, int indexB, int maxCount){
		String[] charsA = PatternService.split(sequenceA, REGEX_PATTERN);
		int count = maxCount;
		for(int i = 0; i < maxCount; i ++){
			String chrA = charsA[indexA + i];
			int size = chrA.length();
			if(size == 1 && chrA.charAt(0) != sequenceB.charAt(indexB + i)
					|| size > 1 && (chrA.charAt(1) == CIRCUMFLEX) ^ !chrA.contains(Character.toString(sequenceB.charAt(indexB + i)))){
				count = i;
				break;
			}
		}
		return count;
	}

}
