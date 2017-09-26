package unit731.hunspeller.collections.trie.sequencers;

import java.util.Map;
import unit731.hunspeller.collections.trie.TrieNode;



public class StringTrieSequencer implements TrieSequencer<String, Integer>{

	@Override
	public int lengthOf(String sequence){
		return sequence.length();
	}

	@Override
	public boolean startsWith(String sequence, String prefix){
		return sequence.startsWith(prefix);
	}

	@Override
	public String getTrueSequence(String sequence, int startIndex, int endIndex){
		return sequence.substring(startIndex, endIndex);
	}

	@Override
	public Integer hashOf(String sequence, int index){
		return (int)sequence.charAt(index);
	}

	@Override
	public <V> TrieNode<String, Integer, V> getChild(Map<Integer, TrieNode<String, Integer, V>> children, Integer stem){
		return children.get(stem);
	}

	@Override
	public int matchesPut(String sequenceA, int indexA, String sequenceB, int indexB, int maxCount){
		return matches(sequenceA, indexA, sequenceB, indexB, maxCount);
	}

	@Override
	public int matchesGet(String nodeSequence, int nodeIndex, String searchSequence, int searchIndex, int maxCount){
		return matches(nodeSequence, nodeIndex, searchSequence, searchIndex, maxCount);
	}

	private int matches(String sequenceA, int indexA, String sequenceB, int indexB, int maxCount){
		int count = maxCount;
		for(int i = 0; i < maxCount; i ++)
			if(sequenceA.charAt(indexA + i) != sequenceB.charAt(indexB + i)){
				count = i;
				break;
			}
		return count;
	}

}
