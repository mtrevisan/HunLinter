package unit731.hunlinter.services.fsa.builders;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.services.fsa.FSA;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;


class FSABuilderTest{

	private static byte[][] input;
	private static byte[][] input2;


	@BeforeAll
	static void prepareByteInput(){
		input = FSATestUtils.generateRandom(25000, new MinMax(1, 20), new MinMax(0, 255));
		input2 = FSATestUtils.generateRandom(40, new MinMax(1, 20), new MinMax(0, 3));
	}

	@Test
	void testEmptyInput(){
		byte[][] input = {};
		FSATestUtils.checkCorrect(input, FSABuilder.build(input));
	}

	@Test
	void testHashResizeBug(){
		byte[][] input = {{0, 1}, {0, 2}, {1, 1}, {2, 1},};

		FSA fsa = FSABuilder.build(input);
		FSATestUtils.checkCorrect(input, FSABuilder.build(input));
		FSATestUtils.checkMinimal(fsa);
	}

	@Test
	void testSmallInput(){
		byte[][] input = {
			"abc".getBytes(StandardCharsets.UTF_8),
			"bbc".getBytes(StandardCharsets.UTF_8),
			"d".getBytes(StandardCharsets.UTF_8)
		};
		FSATestUtils.checkCorrect(input, FSABuilder.build(input));
	}

	@Test
	void testLexicographicOrder(){
		byte[][] input = {{0}, {1}, {(byte) 0xff},};
		Arrays.sort(input, FSABuilder.LEXICAL_ORDERING);

		//check if lexical ordering is consistent with absolute byte value
		Assertions.assertEquals(0, input[0][0]);
		Assertions.assertEquals(1, input[1][0]);
		Assertions.assertEquals((byte)0xFF, input[2][0]);

		final FSA fsa;
		FSATestUtils.checkCorrect(input, fsa = FSABuilder.build(input));

		int arc = fsa.getFirstArc(fsa.getRootNode());
		Assertions.assertEquals(0, fsa.getArcLabel(arc));
		arc = fsa.getNextArc(arc);
		Assertions.assertEquals(1, fsa.getArcLabel(arc));
		arc = fsa.getNextArc(arc);
		Assertions.assertEquals((byte)0xFF, fsa.getArcLabel(arc));
	}

	@Test
	void testRandom25000_largerAlphabet(){
		FSA fsa = FSABuilder.build(input);
		FSATestUtils.checkCorrect(input, fsa);
		FSATestUtils.checkMinimal(fsa);
	}

	@Test
	void testRandom25000_smallAlphabet(){
		FSA fsa = FSABuilder.build(input2);
		FSATestUtils.checkCorrect(input2, fsa);
		FSATestUtils.checkMinimal(fsa);
	}

}
