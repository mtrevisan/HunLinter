package unit731.hunlinter.services.fsa.builders;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.services.fsa.FSA;
import unit731.hunlinter.services.fsa.FSAFlags;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


class CFSA2SerializerTest{

	private FSASerializer createSerializer(){
		return new CFSA2Serializer();
	}

	@Test
	void testA() throws IOException{
		byte[][] input = new byte[][]{{'a'},};

		Arrays.sort(input, FSABuilder.LEXICAL_ORDERING);
		FSA s = FSABuilder.build(input);

		checkSerialization(input, s);
	}

	@Test
	void testArcsSharing() throws IOException{
		byte[][] input = new byte[][]{{'a', 'c', 'f'}, {'a', 'd', 'g'}, {'a', 'e', 'h'}, {'b', 'd', 'g'}, {'b', 'e', 'h'},};

		Arrays.sort(input, FSABuilder.LEXICAL_ORDERING);
		FSA s = FSABuilder.build(input);

		checkSerialization(input, s);
	}

	@Test
	void testFSA5SerializerSimple() throws IOException{
		byte[][] input = new byte[][]{{'a'}, {'a', 'b', 'a'}, {'a', 'c'}, {'b'}, {'b', 'a'}, {'c'},};

		Arrays.sort(input, FSABuilder.LEXICAL_ORDERING);
		FSA s = FSABuilder.build(input);

		checkSerialization(input, s);
	}

	@Test
	void testNotMinimal() throws IOException{
		byte[][] input = new byte[][]{{'a', 'b', 'a'}, {'b'}, {'b', 'a'}};

		Arrays.sort(input, FSABuilder.LEXICAL_ORDERING);
		FSA s = FSABuilder.build(input);

		checkSerialization(input, s);
	}

	@Test
	void testFSA5Bug0() throws IOException{
		checkCorrect(new String[]{"3-D+A+JJ", "3-D+A+NN", "4-F+A+NN", "z+A+NN",});
	}

	@Test
	void testFSA5Bug1() throws IOException{
		checkCorrect(new String[]{"+NP", "n+N", "n+NP",});
	}

	private void checkCorrect(String[] strings) throws IOException{
		byte[][] input = new byte[strings.length][];
		for(int i = 0; i < strings.length; i++){
			input[i] = strings[i].getBytes("ISO8859-1");
		}

		Arrays.sort(input, FSABuilder.LEXICAL_ORDERING);
		FSA s = FSABuilder.build(input);

		checkSerialization(input, s);
	}

	@Test
	void testEmptyInput() throws IOException{
		byte[][] input = new byte[][]{};
		FSA s = FSABuilder.build(input);

		checkSerialization(input, s);
	}

	@Test
	void test_abc() throws IOException{
		testBuiltIn(FSA.read(CFSA2SerializerTest.class.getResourceAsStream("/services/fsa/builders/abc.fsa")));
	}

	@Test
	void test_minimal() throws IOException{
		testBuiltIn(FSA.read(CFSA2SerializerTest.class.getResourceAsStream("/services/fsa/builders/minimal.fsa")));
	}

	@Test
	void test_minimal2() throws IOException{
		testBuiltIn(FSA.read(CFSA2SerializerTest.class.getResourceAsStream("/services/fsa/builders/minimal2.fsa")));
	}

	@Test
	void test_en_tst() throws IOException{
		testBuiltIn(FSA.read(CFSA2SerializerTest.class.getResourceAsStream("/services/fsa/builders/en_tst.dict")));
	}

	private void testBuiltIn(FSA fsa) throws IOException{
		final List<byte[]> sequences = new ArrayList<byte[]>();

		sequences.clear();
		for(ByteBuffer bb : fsa){
			sequences.add(Arrays.copyOf(bb.array(), bb.remaining()));
		}

		Collections.sort(sequences, FSABuilder.LEXICAL_ORDERING);

		final byte[][] in = sequences.toArray(new byte[sequences.size()][]);
		FSA root = FSABuilder.build(in);

		// Check if the DFSA is correct first.
		FSATestUtils.checkCorrect(in, root);

		// Check serialization.
		checkSerialization(in, root);
	}

	private void checkSerialization(byte[][] input, FSA root) throws IOException{
		checkSerialization0(createSerializer(), input, root);
		if(createSerializer().getFlags().contains(FSAFlags.NUMBERS)){
			checkSerialization0(createSerializer().serializeWithNumbers(), input, root);
		}
	}

	private void checkSerialization0(FSASerializer serializer, final byte[][] in, FSA root) throws IOException{
		final byte[] fsaData = serializer.serialize(root, new ByteArrayOutputStream()).toByteArray();

		FSA fsa = FSA.read(new ByteArrayInputStream(fsaData));
		checkCorrect(in, fsa);
	}

	/*
	 * Check if the FSA is correct with respect to the given input.
	 */
	protected void checkCorrect(byte[][] input, FSA fsa){
		// (1) All input sequences are in the right language.
		Set<ByteBuffer> rl = new HashSet<>();
		for(ByteBuffer bb : fsa){
			byte[] array = bb.array();
			int length = bb.remaining();
			rl.add(ByteBuffer.wrap(Arrays.copyOf(array, length)));
		}

		HashSet<ByteBuffer> uniqueInput = new HashSet<>();
		for(byte[] sequence : input){
			uniqueInput.add(ByteBuffer.wrap(sequence));
		}

		for(ByteBuffer sequence : uniqueInput){
			if(!rl.remove(sequence)){
				Assertions.fail("Not present in the right language: " + toString(sequence));
			}
		}

		// (2) No other sequence _other_ than the input is in the right
		// language.
		Assertions.assertEquals(0, rl.size());
	}

	@Test
	void testAutomatonWithNodeNumbers() throws IOException{
		Assertions.assertTrue(createSerializer().getFlags().contains(FSAFlags.NUMBERS));

		byte[][] input = new byte[][]{{'a'}, {'a', 'b', 'a'}, {'a', 'c'}, {'b'}, {'b', 'a'}, {'c'},};

		Arrays.sort(input, FSABuilder.LEXICAL_ORDERING);
		FSA s = FSABuilder.build(input);

		final byte[] fsaData = createSerializer().serializeWithNumbers().serialize(s, new ByteArrayOutputStream()).toByteArray();

		FSA fsa = FSA.read(new ByteArrayInputStream(fsaData));

		// Ensure we have the NUMBERS flag set.
		Assertions.assertTrue(fsa.getFlags().contains(FSAFlags.NUMBERS));

		// Get all numbers from nodes.
		byte[] buffer = new byte[128];
		final ArrayList<String> result = new ArrayList<String>();
		walkNode(buffer, 0, fsa, fsa.getRootNode(), 0, result);

		Collections.sort(result);
		Assertions.assertEquals(Arrays.asList("0 a", "1 aba", "2 ac", "3 b", "4 ba", "5 c"), result);
	}

	private static void walkNode(byte[] buffer, int depth, FSA fsa, int node, int cnt, List<String> result) throws IOException{
		for(int arc = fsa.getFirstArc(node); arc != 0; arc = fsa.getNextArc(arc)){
			buffer[depth] = fsa.getArcLabel(arc);

			if(fsa.isArcFinal(arc) || fsa.isArcTerminal(arc)){
				result.add(cnt + " " + new String(buffer, 0, depth + 1, "UTF-8"));
			}

			if(fsa.isArcFinal(arc)){
				cnt++;
			}

			if(!fsa.isArcTerminal(arc)){
				walkNode(buffer, depth + 1, fsa, fsa.getEndNode(arc), cnt, result);
				cnt += fsa.getRightLanguageCount(fsa.getEndNode(arc));
			}
		}
	}

	/** Drain bytes from a byte buffer to a string */
	private static String toString(ByteBuffer sequence){
		byte[] bytes = new byte[sequence.remaining()];
		sequence.get(bytes);
		return Arrays.toString(bytes);
	}

}
