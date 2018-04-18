package unit731.hunspeller.collections.radixtree.sequencers;

import java.util.function.Function;
import java.util.regex.Pattern;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.Memoizer;
import unit731.hunspeller.services.PatternService;


public class RegExpSequencer implements SequencerInterface{

	private static final Pattern REGEX_PATTERN = PatternService.pattern("(?<!\\[\\^?)(?![^\\[]*\\])");

	private static final String CLASS_START = "[";
	private static final String NEGATED_CLASS_START = CLASS_START + "^";

	private static final Function<String, String[]> FN_SPLIT_SEQUENCE = Memoizer.memoize(seq -> (seq.isEmpty()? new String[0]: PatternService.split(seq, REGEX_PATTERN)));


	@Override
	public boolean startsWith(String sequence, String prefix){
		String[] me = splitSequence(sequence);
		String[] pre = splitSequence(prefix);

		int count = pre.length;
		if(count > me.length)
			return false;

		int i = 0;
		int j = 0;
		while(-- count >= 0)
			if(!matches(me[j ++], pre[i ++]))
				return false;
		return true;
	}

	@Override
	public int length(String sequence){
		return splitSequence(sequence).length;
	}

	@Override
	public boolean equals(String sequenceA, String sequenceB){
		String[] seqA = splitSequence(sequenceA);
		String[] seqB = splitSequence(sequenceB);

		if(seqA.length != seqB.length)
			return false;

		for(int i = 0; i < seqA.length; i ++)
			if(!matches(seqA[i], seqB[i]))
				return false;
		return true;
	}

	@Override
	public boolean equalsAtIndex(String sequenceA, String sequenceB, int index){
		String[] seqA = splitSequence(sequenceA);
		String[] seqB = splitSequence(sequenceB);

		return matches(seqA[index], seqB[index]);
	}

	@Override
	public String subSequence(String sequence, int beginIndex){
		String[] seq = splitSequence(sequence);

		String[] sub = ArrayUtils.subarray(seq, beginIndex, seq.length);
		return StringUtils.join(sub, StringUtils.EMPTY);
	}

	@Override
	public String subSequence(String sequence, int beginIndex, int endIndex){
		String[] seq = splitSequence(sequence);

		String[] sub = ArrayUtils.subarray(seq, beginIndex, endIndex);
		return StringUtils.join(sub, StringUtils.EMPTY);
	}

	@Override
	public String concat(String sequenceA, String sequenceB){
		String[] seqA = splitSequence(sequenceA);
		String[] seqB = splitSequence(sequenceB);

		String[] whole = ArrayUtils.addAll(seqA, seqB);
		return StringUtils.join(whole, StringUtils.EMPTY);
	}

	public static String[] splitSequence(String sequence){
		return FN_SPLIT_SEQUENCE.apply(sequence);
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

}
