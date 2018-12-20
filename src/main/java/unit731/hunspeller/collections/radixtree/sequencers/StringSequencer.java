package unit731.hunspeller.collections.radixtree.sequencers;

import org.apache.commons.lang3.StringUtils;


public class StringSequencer implements SequencerInterface<String>{

	@Override
	public String getEmptySequence(){
		return StringUtils.EMPTY;
	}

	@Override
	public int length(String sequence){
		return sequence.length();
	}

	@Override
	public boolean startsWith(String sequence, String prefix){
		return sequence.startsWith(prefix);
	}

	@Override
	public boolean endsWith(String sequence, String prefix){
		return sequence.endsWith(prefix);
	}

	@Override
	public boolean equalsAtIndex(String sequenceA, String sequenceB, int indexA, int indexB){
		return (sequenceA.charAt(indexA) == sequenceB.charAt(indexB));
	}

	@Override
	public boolean equals(String sequenceA, String sequenceB){
		return sequenceA.equals(sequenceB);
	}

	@Override
	public String subSequence(String sequence, int beginIndex, int endIndex){
		return sequence.substring(beginIndex, endIndex);
	}

	@Override
	public String concat(String sequenceA, String sequenceB){
		return sequenceA + sequenceB;
	}

	@Override
	public String toString(String sequence){
		return sequence;
	}

}
