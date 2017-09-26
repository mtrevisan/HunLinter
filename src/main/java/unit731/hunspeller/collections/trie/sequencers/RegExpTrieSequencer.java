package unit731.hunspeller.collections.trie.sequencers;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import unit731.hunspeller.collections.trie.TrieNode;
import unit731.hunspeller.services.PatternService;


public class RegExpTrieSequencer implements TrieSequencer<String[]>{

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
			if(i == size || p.startsWith(NEGATED_CLASS_START) ^ !p.contains(sequence[i]))
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
		return (String)sequence[index];
	}

	@Override
	public <V> TrieNode<String[], V> getChild(Map<Object, TrieNode<String[], V>> children, Object stem){
		Set<Object> keys = children.keySet();
		for(Object key : keys){
			String k = (String)key;
			if(k.startsWith(NEGATED_CLASS_START) ^ k.contains((String)stem))
				return children.get(key);
		}
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
		for(int i = 0; i < maxCount; i ++){
			String ns = nodeSequence[nodeIndex + i];
			String ss = searchSequence[searchIndex + i];
			if(ns.startsWith(NEGATED_CLASS_START) ^ !ns.contains(ss)){
				count = i;
				break;
			}
		}
		return count;
	}

}
