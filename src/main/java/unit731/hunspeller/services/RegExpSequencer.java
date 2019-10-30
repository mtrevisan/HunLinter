package unit731.hunspeller.services;

import java.util.Arrays;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;


public class RegExpSequencer{

	private static final Pattern PATTERN = PatternHelper.pattern("(?<!\\[\\^?)(?![^\\[]*\\])");

	private static final String CLASS_START = "[";
	private static final String NEGATED_CLASS_START = CLASS_START + "^";

	private static final Function<String, String[]> SPLIT_SEQUENCE = Memoizer.memoize(seq -> (seq.isEmpty()? new String[0]: PatternHelper.split(seq, PATTERN)));


	public static String[] splitSequence(String sequence){
		return SPLIT_SEQUENCE.apply(sequence);
	}

	public String[] getEmptySequence(){
		return new String[0];
	}

	public int length(String[] sequence){
		return sequence.length;
	}

	/**
	 * Tests if this sequence starts with the specified prefix.
	 *
	 * @param sequence	The sequence.
	 * @param prefix	The prefix.
	 * @return	{@code true} if the sequence represented by the argument is a prefix of the sequence represented by this sequence; {@code false} otherwise.
	 *				Note also that {@code true} will be returned if the argument is an empty sequence or is equal to this {@code RadixTreeKey} object as
	 *				determined by the {@link #equals} method.
	 */
	public boolean startsWith(String[] sequence, String[] prefix){
		int count = prefix.length;
		if(count > sequence.length)
			return false;

		for(int i = 0; i < count; i ++)
			if(!matches(sequence[i], prefix[i]))
				return false;
		return true;
	}

	/**
	 * Tests if this sequence ends with the specified prefix.
	 *
	 * @param sequence	The sequence.
	 * @param suffix	The suffix.
	 * @return	{@code true} if the sequence represented by the argument is a suffix of the sequence represented by this sequence; {@code false} otherwise.
	 *				Note also that {@code true} will be returned if the argument is an empty sequence or is equal to this {@code RadixTreeKey} object as
	 *				determined by the {@link #equals} method.
	 */
	public boolean endsWith(String[] sequence, String[] suffix){
		int count = suffix.length;
		if(count > sequence.length)
			return false;

		for(int i = 1; i <= count; i ++)
			if(!matches(sequence[sequence.length - i], suffix[suffix.length - i]))
				return false;
		return true;
	}

	/**
	 * Returns a sequence that is a subsequence of this sequence.
	 * The subsequence begins at the specified {@code beginIndex} and extends to the end of the sequence.
	 *
	 * @param sequence	The sequence.
	 * @param index	The index.
	 * @return	The specified `character` at index `index`.
	 * @exception IndexOutOfBoundsException	If the {@code beginIndex} is negative, or the end of the sequence is larger than the length of
	 *														this sequence, or {@code beginIndex} is larger than the length of the sequence.
	 */
	public String[] characterAt(String[] sequence, int index){
		return subSequence(sequence, index, index + 1);
	}

	/**
	 * Returns a sequence that is a subsequence of this sequence.
	 * The subsequence begins at the specified {@code beginIndex} and extends to the end of the sequence.
	 *
	 * @param sequence	The sequence.
	 * @param beginIndex	The beginning index, inclusive.
	 * @return	The specified substring.
	 * @exception IndexOutOfBoundsException	If the {@code beginIndex} is negative, or the end of the sequence is larger than the length of
	 *														this sequence, or {@code beginIndex} is larger than the length of the sequence.
	 */
	public String[] subSequence(String[] sequence, int beginIndex){
		return (beginIndex > 0? subSequence(sequence, beginIndex, length(sequence)): sequence);
	}

	public boolean equals(String[] sequenceA, String[] sequenceB){
		if(sequenceA.length != sequenceB.length)
			return false;

		for(int i = 0; i < sequenceA.length; i ++)
			if(!matches(sequenceA[i], sequenceB[i]))
				return false;
		return true;
	}

	public boolean equalsAtIndex(String[] sequenceA, String[] sequenceB, int indexA, int indexB){
		return matches(sequenceA[indexA], sequenceB[indexB]);
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

	/**
	 * Returns a sequence that is a subsequence of this sequence.
	 * The subsequence begins at the specified {@code beginIndex} and extends to the character at index {@code endIndex - 1}.
	 *
	 * @param sequence	The sequence.
	 * @param beginIndex	The beginning index, inclusive.
	 * @param endIndex	The ending index, exclusive.
	 * @return	The specified substring.
	 * @exception IndexOutOfBoundsException	If the {@code beginIndex} is negative, or {@code endIndex} is larger than the length of
	 *														this sequence, or {@code beginIndex} is larger than {@code endIndex}.
	 */
	public String[] subSequence(String[] sequence, int beginIndex, int endIndex){
		return ArrayUtils.subarray(sequence, beginIndex, endIndex);
	}

	public String[] concat(String[] sequenceA, String[] sequenceB){
		return (sequenceA.length > 0? ArrayUtils.addAll(sequenceA, sequenceB): sequenceB);
	}

	public String[] reverse(String[] sequence){
		String[] reverse = Arrays.copyOf(sequence, sequence.length);
		ArrayUtils.reverse(reverse);
		return reverse;
	}

	public String toString(String[] sequence){
		return String.join(StringUtils.EMPTY, sequence);
	}

}
