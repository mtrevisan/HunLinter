package unit731.hunlinter.services.fsa;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import unit731.hunlinter.services.fsa.builders.FSABuilder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


public class FSATestUtils{

	public static void walkNode(byte[] buffer, int depth, FSA fsa, int node, int cnt, List<String> result) throws IOException{
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
	public static void checkCorrect(final byte[][] input, final FSA fsa){
		//(1) All input sequences are in the right language
		final Set<ByteBuffer> rl = new HashSet<>();
		for(final ByteBuffer bb : fsa)
			rl.add(ByteBuffer.wrap(Arrays.copyOf(bb.array(), bb.remaining())));

		final Set<ByteBuffer> uniqueInput = new HashSet<>();
		for(final byte[] sequence : input)
			uniqueInput.add(ByteBuffer.wrap(sequence));

		for(final ByteBuffer sequence : uniqueInput)
			if(!rl.remove(sequence))
				Assertions.fail("Not present in the right language: " + toString(sequence));

		//(2) No other sequence _other_ than the input is in the right language
		Assertions.assertEquals(0, rl.size());
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
		final Map<String, Integer> stateLanguages = new HashMap<String, Integer>();

		fsa.visitPostOrder(new StateVisitor(){
			private StringBuffer sb = new StringBuffer();

			public boolean accept(int state){
				List<byte[]> rightLanguage = allSequences(fsa, state);
				Collections.sort(rightLanguage, FSABuilder.LEXICAL_ORDERING);

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

	/*
	 * Check if two FSAs are identical.
	 */
	public static void checkIdentical(FSA fsa1, FSA fsa2){
		Deque<String> fromRoot = new ArrayDeque<String>();
		checkIdentical(fromRoot, fsa1, fsa1.getRootNode(), new BitSet(), fsa2, fsa2.getRootNode(), new BitSet());
	}

	static void checkIdentical(Deque<String> fromRoot, FSA fsa1, int node1, BitSet visited1, FSA fsa2, int node2, BitSet visited2){
		int arc1 = fsa1.getFirstArc(node1);
		int arc2 = fsa2.getFirstArc(node2);

		if(visited1.get(node1) != visited2.get(node2)){
			throw new RuntimeException("Two nodes should either be visited or not visited: " + Arrays.toString(fromRoot.toArray()) + " " + " node1: " + node1 + " " + " node2: " + node2);
		}
		visited1.set(node1);
		visited2.set(node2);

		Set<Character> labels1 = new TreeSet<Character>();
		Set<Character> labels2 = new TreeSet<Character>();
		while(true){
			labels1.add((char) fsa1.getArcLabel(arc1));
			labels2.add((char) fsa2.getArcLabel(arc2));

			arc1 = fsa1.getNextArc(arc1);
			arc2 = fsa2.getNextArc(arc2);

			if(arc1 == 0 || arc2 == 0){
				if(arc1 != arc2){
					throw new RuntimeException("Different number of labels at path: " + Arrays.toString(fromRoot.toArray()));
				}
				break;
			}
		}

		if(!labels1.equals(labels2)){
			throw new RuntimeException("Different sets of labels at path: " + Arrays.toString(fromRoot.toArray()) + ":\n" + labels1 + "\n" + labels2);
		}

		// recurse.
		for(char chr : labels1){
			byte label = (byte) chr;
			fromRoot.push(Character.isLetterOrDigit(chr)? Character.toString(chr): Integer.toString(chr));

			arc1 = fsa1.getArc(node1, label);
			arc2 = fsa2.getArc(node2, label);

			if(fsa1.isArcFinal(arc1) != fsa2.isArcFinal(arc2)){
				throw new RuntimeException("Different final flag on arcs at: " + Arrays.toString(fromRoot.toArray()) + ", label: " + label);
			}

			if(fsa1.isArcTerminal(arc1) != fsa2.isArcTerminal(arc2)){
				throw new RuntimeException("Different terminal flag on arcs at: " + Arrays.toString(fromRoot.toArray()) + ", label: " + label);
			}

			if(!fsa1.isArcTerminal(arc1)){
				checkIdentical(fromRoot, fsa1, fsa1.getEndNode(arc1), visited1, fsa2, fsa2.getEndNode(arc2), visited2);
			}

			fromRoot.pop();
		}
	}

}
