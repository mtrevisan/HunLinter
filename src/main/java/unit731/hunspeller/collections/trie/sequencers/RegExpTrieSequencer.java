package unit731.hunspeller.collections.trie.sequencers;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import unit731.hunspeller.collections.trie.TrieNode;
import unit731.hunspeller.services.PatternService;


public class RegExpTrieSequencer implements TrieSequencer<String[], String>{

	private static final Pattern REGEX_PATTERN = PatternService.pattern("(?<!\\[\\^?)(?![^\\[]*\\])");

	private static final String NEGATED_CLASS_START = "[^";


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
			if(i == size || matches(sequence[i], p))
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
	public String hashOf(String[] sequence, int index){
		return sequence[index];
	}

	@Override
	public <V> TrieNode<String[], String, V> getChild(Map<String, TrieNode<String[], String, V>> children, String stem){
		Set<String> keys = children.keySet();
		for(String key : keys)
			if(!matches(stem, key))
				return children.get(key);
		return null;
	}

	@Override
	public int matchesPut(String[] sequenceA, int indexA, String[] sequenceB, int indexB, int maxCount){
		int count = maxCount;
		for(int i = 0; i < maxCount; i ++)
			if(!sequenceA[indexA + i].equals(sequenceB[indexB + i])){
				count = i;
				break;
			}
		return count;
	}

	@Override
	public int matchesGet(String[] nodeSequence, int nodeIndex, String[] searchSequence, int searchIndex, int maxCount){
		int count = maxCount;
		for(int i = 0; i < maxCount; i ++)
			if(matches(searchSequence[searchIndex + i], nodeSequence[nodeIndex + i])){
				count = i;
				break;
			}
		return count;
	}

	private boolean matches(String text, String sequence){
		return (sequence.startsWith(NEGATED_CLASS_START) ^ !sequence.contains(text));
	}

}
