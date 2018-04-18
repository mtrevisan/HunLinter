package unit731.hunspeller.collections.radixtree.sequencers;

import java.util.function.Function;
import java.util.regex.Pattern;
import org.apache.commons.lang3.ArrayUtils;
import unit731.hunspeller.services.Memoizer;
import unit731.hunspeller.services.PatternService;


public class RegExpSequencer implements SequencerInterface<String[]>{

	private static final Pattern REGEX_PATTERN = PatternService.pattern("(?<!\\[\\^?)(?![^\\[]*\\])");

	private static final String CLASS_START = "[";
	private static final String NEGATED_CLASS_START = CLASS_START + "^";

	private static final Function<String, String[]> FN_SPLIT_SEQUENCE = Memoizer.memoize(seq -> (seq.isEmpty()? new String[0]: PatternService.split(seq, REGEX_PATTERN)));


	@Override
	public String[] getNullSequence(){
		return new String[0];
	}

	@Override
	public boolean startsWith(String[] sequence, String[] prefix){
		int count = prefix.length;
		if(count > sequence.length)
			return false;

		for(int i = 0; i < count; i ++)
			if(!matches(sequence[i], prefix[i]))
				return false;
		return true;
	}

	@Override
	public int length(String[] sequence){
		return sequence.length;
	}

	@Override
	public boolean equals(String[] sequenceA, String[] sequenceB){
		if(sequenceA.length != sequenceB.length)
			return false;

		for(int i = 0; i < sequenceA.length; i ++)
			if(!matches(sequenceA[i], sequenceB[i]))
				return false;
		return true;
	}

	@Override
	public boolean equalsAtIndex(String[] sequenceA, String[] sequenceB, int index){
		return matches(sequenceA[index], sequenceB[index]);
	}

	@Override
	public String[] subSequence(String[] sequence, int beginIndex){
		return ArrayUtils.subarray(sequence, beginIndex, sequence.length);
	}

	@Override
	public String[] subSequence(String[] sequence, int beginIndex, int endIndex){
		return ArrayUtils.subarray(sequence, beginIndex, endIndex);
	}

	@Override
	public String[] concat(String[] sequenceA, String[] sequenceB){
		return ArrayUtils.addAll(sequenceA, sequenceB);
	}

	private boolean matches(String fieldA, String fieldB){
		boolean response;
		boolean fieldAHasClassStart = fieldA.startsWith(CLASS_START);
		boolean fieldBHasClassStart = fieldB.startsWith(CLASS_START);
		if(!fieldAHasClassStart && fieldBHasClassStart)
			response = (fieldB.startsWith(NEGATED_CLASS_START) ^ fieldB.contains(fieldA));
		else if(!fieldBHasClassStart)
			response = (fieldA.startsWith(NEGATED_CLASS_START) ^ fieldA.contains(fieldB));
		else
			response = fieldA.equals(fieldB);
		return response;
	}

	public static String[] splitSequence(String sequence){
		return FN_SPLIT_SEQUENCE.apply(sequence);
	}

}
