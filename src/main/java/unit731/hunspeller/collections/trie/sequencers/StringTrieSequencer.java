package unit731.hunspeller.collections.trie.sequencers;


public class StringTrieSequencer implements TrieSequencer<String>{

	@Override
	public int hashOf(String sequence, int index){
		return sequence.charAt(index);
	}

	@Override
	public int matches(String sequenceA, int indexA, String sequenceB, int indexB, int maxCount){
		int count = maxCount;
		for(int i = 0; i < maxCount; i ++)
			if(sequenceA.charAt(indexA + i) != sequenceB.charAt(indexB + i)){
				count = i;
				break;
			}
		return count;
	}

}
