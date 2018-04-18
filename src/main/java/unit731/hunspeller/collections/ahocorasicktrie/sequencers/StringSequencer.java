package unit731.hunspeller.collections.ahocorasicktrie.sequencers;

import org.apache.commons.lang3.StringUtils;


public class StringSequencer implements SequencerInterface<String>{

	@Override
	public String getNullSequence(){
		return StringUtils.EMPTY;
	}

	@Override
	public boolean startsWith(String sequence, String prefix){
		return sequence.startsWith(prefix);
	}

	@Override
	public int length(String sequence){
		return sequence.length();
	}

	@Override
	public boolean equals(String sequenceA, String sequenceB){
		return sequenceA.equals(sequenceB);
	}

	@Override
	public boolean equalsAtIndex(String sequenceA, String sequenceB, int index){
		return (sequenceA.charAt(index) == sequenceB.charAt(index));
	}

	@Override
	public String charAtIndex(String sequence, int index){
		return String.valueOf(sequence.charAt(index));
	}

	@Override
	public String subSequence(String sequence, int beginIndex){
		return sequence.substring(beginIndex);
	}

	@Override
	public String subSequence(String sequence, int beginIndex, int endIndex){
		return sequence.substring(beginIndex, endIndex);
	}

	@Override
	public String concat(String sequenceA, String sequenceB){
		return String.join(StringUtils.EMPTY, sequenceA, sequenceB);
	}

	@Override
	public String toString(String sequence){
		return sequence;
	}
	
}
