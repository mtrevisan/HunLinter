package unit731.hunlinter.datastructures.fsa.builders;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.datastructures.fsa.FSA;
import unit731.hunlinter.datastructures.fsa.FSATestUtils;
import unit731.hunlinter.services.text.StringHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


class FSABuilderTest{

	@Test
	void testEmptyInput(){
		List<byte[]> input = Collections.emptyList();

		FSABuilder builder = new FSABuilder();
		FSA fsa = builder.build(input);

		FSATestUtils.checkCorrect(input, fsa);
	}

	@Test
	void testHashResizeBug(){
		List<String> input = Arrays.asList("01", "02", "11", "21");
		List<byte[]> in = input.stream()
			.map(StringHelper::getRawBytes)
			.collect(Collectors.toList());

		FSABuilder builder = new FSABuilder();
		FSA fsa = builder.build(in);

		FSATestUtils.checkCorrect(in, fsa);
		FSATestUtils.checkMinimal(fsa);
	}

	@Test
	void testSmallInput(){
		List<String> input = Arrays.asList("abc", "bbc", "d");
		List<byte[]> in = input.stream()
			.map(StringHelper::getRawBytes)
			.collect(Collectors.toList());

		FSABuilder builder = new FSABuilder();
		FSA fsa = builder.build(in);

		FSATestUtils.checkCorrect(in, fsa);
	}

	@Test
	void testLexicographicOrder(){
		List<byte[]> input = Arrays.asList(new byte[]{0}, new byte[]{1}, new byte[]{(byte)0xFF});
		Collections.sort(input, LexicographicalComparator.lexicographicalComparator());

		//check if lexical ordering is consistent with absolute byte value
		Assertions.assertEquals(0, input.get(0)[0]);
		Assertions.assertEquals(1, input.get(1)[0]);
		Assertions.assertEquals((byte)0xFF, input.get(2)[0]);

		FSABuilder builder = new FSABuilder();
		FSA fsa = builder.build(input);

		FSATestUtils.checkCorrect(input, fsa);

		int arc = fsa.getFirstArc(fsa.getRootNode());
		Assertions.assertEquals(0, fsa.getArcLabel(arc));
		arc = fsa.getNextArc(arc);
		Assertions.assertEquals(1, fsa.getArcLabel(arc));
		arc = fsa.getNextArc(arc);
		Assertions.assertEquals((byte)0xFF, fsa.getArcLabel(arc));
	}

	@Test
	void testRandom25000_largerAlphabet(){
		List<byte[]> in = generateRandom(25_000, 1, 20, 0, 255);

		FSABuilder builder = new FSABuilder();
		FSA fsa = builder.build(in);

		FSATestUtils.checkCorrect(in, fsa);
		FSATestUtils.checkMinimal(fsa);
	}

	@Test
	public void testRandom25000_smallAlphabet(){
		List<byte[]> in = generateRandom(40, 1, 20, 0, 3);

		FSABuilder builder = new FSABuilder();
		FSA fsa = builder.build(in);

		FSATestUtils.checkCorrect(in, fsa);
		FSATestUtils.checkMinimal(fsa);
	}

	/** Generate a sorted list of random sequences */
	private List<byte[]> generateRandom(int count, int lengthMin, int lengthMax, int alphabetMin, int alphabetMax){
		final List<byte[]> input = new ArrayList<>();
		final Random rnd = new Random(System.currentTimeMillis());
		for(int i = 0; i < count; i ++)
			input.add(randomByteSequence(rnd, lengthMin, lengthMax, alphabetMin, alphabetMax));
		Collections.sort(input, LexicographicalComparator.lexicographicalComparator());
		return input;
	}

	/** Generate a random string */
	private byte[] randomByteSequence(Random rnd, int lengthMin, int lengthMax, int alphabetMin, int alphabetMax){
		byte[] bytes = new byte[lengthMin + rnd.nextInt(lengthMax - lengthMin + 1)];
		for(int i = 0; i < bytes.length; i ++)
			bytes[i] = (byte)(alphabetMin + rnd.nextInt(alphabetMax - alphabetMin + 1));
		return bytes;
	}

}
