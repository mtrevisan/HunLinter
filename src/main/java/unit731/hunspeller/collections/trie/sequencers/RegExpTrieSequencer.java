package unit731.hunspeller.collections.trie.sequencers;

import java.util.regex.Pattern;
import unit731.hunspeller.services.PatternService;


public class RegExpTrieSequencer implements TrieSequencer<String[]>{

	private static final Pattern REGEX_PATTERN = PatternService.pattern("(?<!\\[\\^?)(?!\\])");


	public static String[] extractCharacters(String sequence){
		return PatternService.split(sequence, REGEX_PATTERN);
	}

	@Override
	public int lengthOf(String[] sequence){
		return sequence.length;
	}

	@Override
	public boolean startsWith(String[] sequence, String[] prefix){
		int i = 0;
		int size = sequence.length;
		for(String p : prefix){
			if(i == size || !sequence[i].equals(p))
				return false;

			i ++;
		}
		return true;
	}

	@Override
	public String[] getTrueSequence(String[] sequence, int startIndex, int endIndex){
		int size = endIndex - startIndex;
		String[] response = new String[size];
		for(int i = 0; i < size; i ++)
			response[i] = sequence[startIndex + i];
		return response;
	}

	@Override
	public Object hashOf(String[] sequence, int index){
		return sequence[index];
	}

	@Override
	public int matches(String[] sequenceA, int indexA, String[] sequenceB, int indexB, int maxCount){
		int count = maxCount;
		for(int i = 0; i < maxCount; i ++)
			if(!sequenceA[indexA + i].equals(sequenceB[indexB + i])){
				count = i;
				break;
			}
		return count;
	}

}
