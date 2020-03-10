package unit731.hunlinter.services.fsa.builders;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.services.fsa.FSA;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


class FSABuilderTest{

	@Test
	void testEmptyInput(){
		List<byte[]> input = Collections.emptyList();

		FSA fsa = FSABuilder.build(input);

		FSATestUtils.checkCorrect(input, fsa);
	}

	@Test
	void testHashResizeBug(){
		List<String> input = Arrays.asList("01", "02", "11", "21");

		List<byte[]> in = input.stream()
			.map(word -> word.getBytes(StandardCharsets.UTF_8))
			.collect(Collectors.toList());
		FSA fsa = FSABuilder.build(in);

		FSATestUtils.checkCorrect(in, fsa);
		FSATestUtils.checkMinimal(fsa);
	}

	@Test
	void testSmallInput(){
		List<String> input = Arrays.asList("abc", "bbc", "d");

		List<byte[]> in = input.stream()
			.map(word -> word.getBytes(StandardCharsets.UTF_8))
			.collect(Collectors.toList());
		FSA fsa = FSABuilder.build(in);

		FSATestUtils.checkCorrect(in, fsa);
	}

	@Test
	void testLexicographicOrder(){
		final String str1 = Character.toString(0);
		final String str2 = Character.toString(1);
		final String str3 = Character.toString(0xFF);
		List<String> input = Arrays.asList(str1, str2, str3);
		input.sort(Comparator.naturalOrder());

		//check if lexical ordering is consistent with absolute byte value
		Assertions.assertEquals(str1, input.get(0));
		Assertions.assertEquals(str2, input.get(1));
		Assertions.assertEquals(str3, input.get(2));

		List<byte[]> in = input.stream()
			.map(word -> word.getBytes(StandardCharsets.UTF_8))
			.collect(Collectors.toList());
		FSA fsa = FSABuilder.build(in);

		FSATestUtils.checkCorrect(in, fsa);

		int arc = fsa.getFirstArc(fsa.getRootNode());
		Assertions.assertEquals(str1, Character.toString(fsa.getArcLabel(arc)));
		arc = fsa.getNextArc(arc);
		Assertions.assertEquals(str2, Character.toString(fsa.getArcLabel(arc)));
		arc = fsa.getNextArc(arc);
		Assertions.assertEquals(str3, Character.toString(fsa.getArcLabel(arc) & 0xFF));
	}

	@Test
	void testRandom25000_largerAlphabet(){
		List<byte[]> in = generateRandom(25_000, 1, 20, 0, 255);

		FSA fsa = FSABuilder.build(in);

		FSATestUtils.checkCorrect(in, fsa);
		FSATestUtils.checkMinimal(fsa);
	}

	@Test
	public void testRandom25000_smallAlphabet(){
		List<byte[]> in = generateRandom(40, 1, 20, 0, 3);

		FSA fsa = FSABuilder.build(in);

		FSATestUtils.checkCorrect(in, fsa);
		FSATestUtils.checkMinimal(fsa);
	}

	/** Generate a sorted list of random sequences */
	private List<byte[]> generateRandom(int count, int lengthMin, int lengthMax, int alphabetMin, int alphabetMax){
		final List<byte[]> input = new ArrayList<>();
		final Random rnd = new Random(System.currentTimeMillis());
		for(int i = 0; i < count; i ++)
			input.add(randomByteSequence(rnd, lengthMin, lengthMax, alphabetMin, alphabetMax));
		Collections.sort(input, FSABuilder.LEXICAL_ORDERING);
		return input;
	}

	/** Generate a random string */
	private byte[] randomByteSequence(Random rnd, int lengthMin, int lengthMax, int alphabetMin, int alphabetMax){
		byte[] bytes = new byte[lengthMin + rnd.nextInt(lengthMax - lengthMin + 1)];
		for(int i = 0; i < bytes.length; i ++)
			bytes[i] = (byte) (alphabetMin + rnd.nextInt(alphabetMax - alphabetMin + 1));
		return bytes;
	}

}
