package unit731.hunlinter.services.fsa;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import unit731.hunlinter.services.fsa.builders.LexicographicalComparator;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class FSATestUtils{

	public static void walkNode(byte[] buffer, int depth, FSA fsa, int node, int cnt, List<String> result){
		for(int arc = fsa.getFirstArc(node); arc != 0; arc = fsa.getNextArc(arc)){
			buffer[depth] = fsa.getArcLabel(arc);

			if(fsa.isArcFinal(arc) || fsa.isArcTerminal(arc))
				result.add(cnt + StringUtils.SPACE + new String(buffer, 0, depth + 1, StandardCharsets.UTF_8));

			if(fsa.isArcFinal(arc))
				cnt ++;

			if(!fsa.isArcTerminal(arc)){
				walkNode(buffer, depth + 1, fsa, fsa.getEndNode(arc), cnt, result);
				cnt += fsa.getRightLanguageCount(fsa.getEndNode(arc));
			}
		}
	}

	/** Check if the DFSA is correct with respect to the given input */
	public static void checkCorrect(final List<byte[]> input, final FSA fsa){
		//(1) All input sequences are in the right language
		final Set<ByteBuffer> rl = new HashSet<>();
		for(final ByteBuffer bb : fsa){
			byte[] array = bb.array();
			int length = bb.remaining();
			rl.add(ByteBuffer.wrap(Arrays.copyOf(array, length)));
		}

		final Set<ByteBuffer> uniqueInput = new HashSet<>();
		for(final byte[] sequence : input)
			uniqueInput.add(ByteBuffer.wrap(sequence));

		for(final ByteBuffer sequence : uniqueInput)
			if(!rl.remove(sequence))
				Assertions.fail("Not present in the right language: " + toString(sequence));

		//(2) No other sequence _other_ than the input is in the right language
		Assertions.assertEquals(0, rl.size());
	}

	/* Drain bytes from a byte buffer to a string */
	public static String toString(ByteBuffer sequence) {
		byte [] bytes = new byte [sequence.remaining()];
		sequence.get(bytes);
		return Arrays.toString(bytes);
	}

	/*
	 * Check if the DFSA reachable from a given state is minimal.
	 * This means no two states have the same right language.
	 */
	public static void checkMinimal(final FSA fsa){
		final Map<String, Integer> stateLanguages = new HashMap<>();

		fsa.visitPostOrder(new StateVisitor(){
			private final StringBuffer sb = new StringBuffer();

			public boolean accept(int state){
				List<byte[]> rightLanguage = allSequences(fsa, state);
				Collections.sort(rightLanguage, LexicographicalComparator.lexicographicalComparator());

				sb.setLength(0);
				for(byte[] seq : rightLanguage)
					sb.append(Arrays.toString(seq)).append(',');
				String full = sb.toString();
				Assertions.assertFalse(stateLanguages.containsKey(full), "State exists: " + state + " " + full + " " + stateLanguages.get(full));
				stateLanguages.put(full, state);

				return true;
			}
		});
	}

	static List<byte[]> allSequences(FSA fsa, int state){
		List<byte[]> seq = new ArrayList<>();
		for(ByteBuffer bb : fsa.getSequences(state))
			seq.add(Arrays.copyOf(bb.array(), bb.remaining()));
		return seq;
	}

}
