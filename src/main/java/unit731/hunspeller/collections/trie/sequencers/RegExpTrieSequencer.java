package unit731.hunspeller.collections.trie.sequencers;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import unit731.hunspeller.collections.trie.TrieNode;
import unit731.hunspeller.services.PatternService;


public class RegExpTrieSequencer implements TrieSequencerInterface<String[], String>{

	private static final Pattern PATTERN = PatternService.pattern("(?<!\\[\\^?)(?![^\\[]*\\])");

	private static final String NEGATED_CLASS_START = "[^";


	public static String[] extractCharacters(String sequence){
		return PatternService.split(sequence, PATTERN);
	}

	@Override
	public int lengthOf(String[] sequence){
		return sequence.length;
	}

	@Override
	public boolean startsWith(String[] sequence, String[] prefix){
		int size = Math.min(sequence.length, prefix.length);
		for(int i = 0; i < size; i ++)
			if(matches(sequence[i], prefix[i]))
				return false;
		return true;
	}

	@Override
	public String hashOf(String[] sequence, int index){
		return sequence[index];
	}

	@Override
	public <V> TrieNode<String[], String, V> getChild(Map<String, TrieNode<String[], String, V>> children, String stem){
		Set<Map.Entry<String, TrieNode<String[], String, V>>> entries = children.entrySet();
		for(Map.Entry<String, TrieNode<String[], String, V>> entry : entries)
			if(!matches(stem, entry.getKey()))
				return entry.getValue();
		return null;
	}

	@Override
	public int matchesPut(String[] sequenceA, int indexA, String[] sequenceB, int indexB, int maxCount){
		for(int i = 0; i < maxCount; i ++)
			if(!sequenceA[indexA + i].equals(sequenceB[indexB + i]))
				return i;
		return maxCount;
	}

	@Override
	public int matchesGet(String[] nodeSequence, int nodeIndex, String[] searchSequence, int searchIndex, int maxCount){
		for(int i = 0; i < maxCount; i ++)
			if(matches(searchSequence[searchIndex + i], nodeSequence[nodeIndex + i]))
				return i;
		return maxCount;
	}

	private boolean matches(String text, String sequence){
		return (sequence.startsWith(NEGATED_CLASS_START) ^ !sequence.contains(text));
	}

}
