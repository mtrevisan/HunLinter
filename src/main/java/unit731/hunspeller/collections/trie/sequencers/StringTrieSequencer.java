package unit731.hunspeller.collections.trie.sequencers;


public class StringTrieSequencer implements TrieSequencer<String>{

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
	public Object hashOf(String sequence, int index){
		return sequence.charAt(index);
	}

	@Override
	public int matchesPut(String sequenceA, int indexA, String sequenceB, int indexB, int maxCount){
		return matches(sequenceA, indexA, sequenceB, indexB, maxCount);
	}

	@Override
	public int matchesGet(String sequenceA, int indexA, String sequenceB, int indexB, int maxCount){
		return matches(sequenceA, indexA, sequenceB, indexB, maxCount);
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
