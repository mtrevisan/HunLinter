package unit731.hunlinter.services.fsa.builders;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.services.fsa.FSA;
import unit731.hunlinter.services.fsa.FSAFlags;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


class CFSA2SerializerTest{

	@Test
	void testEmptyInput() throws IOException{
		testInput(Collections.emptyList());
	}

	@Test
	void testA() throws IOException{
		testInput(Collections.singletonList("a"));
	}

	@Test
	void testArcsSharing() throws IOException{
		testInput(Arrays.asList("acf", "adg", "aeh", "bdg", "beh"));
	}

	@Test
	void testFSA5SerializerSimple() throws IOException{
		testInput(Arrays.asList("a", "aba", "ac", "b", "ba", "c"));
	}

	@Test
	void testNotMinimal() throws IOException{
		testInput(Arrays.asList("aba", "b", "ba"));
	}

	@Test
	void testFSA5Bug0() throws IOException{
		testInput(Arrays.asList("3-D+A+JJ", "3-D+A+NN", "4-F+A+NN", "z+A+NN"));
	}

	@Test
	void testFSA5Bug1() throws IOException{
		testInput(Arrays.asList("+NP", "n+N", "n+NP"));
	}

	private void testInput(List<String> input) throws IOException{
		input.sort(Comparator.naturalOrder());

		List<byte[]> in = input.stream()
			.map(word -> word.getBytes(StandardCharsets.UTF_8))
			.collect(Collectors.toList());
		FSABuilder builder = new FSABuilder();
		FSA fsa = builder.build(in);

		checkSerialization(in, fsa);
	}


	@Test
	void test_abc() throws IOException{
		testInput("abc.fsa");
	}

	@Test
	void test_minimal() throws IOException{
		testInput("minimal.fsa");
	}

	@Test
	void test_minimal2() throws IOException{
		testInput("minimal2.fsa");
	}

	@Test
	void test_en_tst() throws IOException{
		testInput("en_tst.dict");
	}

	private void testInput(String fsaFilename) throws IOException{
		InputStream stream = CFSA2SerializerTest.class.getResourceAsStream("/services/fsa/builders/" + fsaFilename);
		FSA fsa1 = FSA.read(stream);

		List<byte[]> input = new ArrayList<>();
		for(ByteBuffer bb : fsa1)
			input.add(bb.array());
		Collections.sort(input, FSABuilder.LEXICAL_ORDERING);

		FSABuilder builder = new FSABuilder();
		FSA fsa2 = builder.build(input);

		//check if the DFSA is correct first
		FSATestUtils.checkCorrect(input, fsa2);

		//check serialization
		checkSerialization(input, fsa2);
	}

	private void checkSerialization(List<byte[]> input, FSA root) throws IOException{
		checkSerialization0(createSerializer(), input, root);
		if(createSerializer().getFlags().contains(FSAFlags.NUMBERS))
			checkSerialization0(createSerializer().serializeWithNumbers(), input, root);
	}

	private void checkSerialization0(FSASerializer serializer, List<byte[]> in, FSA root) throws IOException{
		final byte[] fsaData = serializer.serialize(root, new ByteArrayOutputStream()).toByteArray();

		FSA fsa = FSA.read(new ByteArrayInputStream(fsaData));
		FSATestUtils.checkCorrect(in, fsa);
	}

	@Test
	void testAutomatonWithNodeNumbers() throws IOException{
		Assertions.assertTrue(createSerializer().getFlags().contains(FSAFlags.NUMBERS));

		List<String> input = Arrays.asList("a", "aba", "ac", "b", "ba", "c");
		input.sort(Comparator.naturalOrder());

		List<byte[]> in = input.stream()
			.map(word -> word.getBytes(StandardCharsets.UTF_8))
			.collect(Collectors.toList());
		FSABuilder builder = new FSABuilder();
		FSA fsa1 = builder.build(in);

		byte[] fsaData = createSerializer().serializeWithNumbers().serialize(fsa1, new ByteArrayOutputStream()).toByteArray();

		FSA fsa2 = FSA.read(new ByteArrayInputStream(fsaData));

		// Ensure we have the NUMBERS flag set.
		Assertions.assertTrue(fsa2.getFlags().contains(FSAFlags.NUMBERS));

		// Get all numbers from nodes.
		byte[] buffer = new byte[128];
		ArrayList<String> result = new ArrayList<>();
		walkNode(buffer, 0, fsa2, fsa2.getRootNode(), 0, result);

		Collections.sort(result);
		Assertions.assertEquals(Arrays.asList("0 a", "1 aba", "2 ac", "3 b", "4 ba", "5 c"), result);
	}

	private FSASerializer createSerializer(){
		return new CFSA2Serializer();
	}

	private static void walkNode(byte[] buffer, int depth, FSA fsa, int node, int cnt, List<String> result) throws IOException{
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

}
