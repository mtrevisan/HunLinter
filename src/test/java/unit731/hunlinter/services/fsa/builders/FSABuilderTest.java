package unit731.hunlinter.services.fsa.builders;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.services.fsa.FSA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


class FSABuilderTest{

	@Test
	void testEmptyInput(){
		List<String> input = Collections.emptyList();

		FSA fsa = FSABuilder.build(input);

		FSATestUtils.checkCorrect(input, fsa);
	}

	@Test
	void testHashResizeBug(){
		List<String> input = Arrays.asList("01", "02", "11", "21");

		FSA fsa = FSABuilder.build(input);

		FSATestUtils.checkCorrect(input, fsa);
		FSATestUtils.checkMinimal(fsa);
	}

	@Test
	void testSmallInput(){
		List<String> input = Arrays.asList("abc", "bbc", "d");

		FSA fsa = FSABuilder.build(input);

		FSATestUtils.checkCorrect(input, fsa);
	}

	@Test
	void testLexicographicOrder(){
		byte[][] input = {{0}, {1}, {(byte)0xff}};
		Arrays.sort(input, FSABuilder.LEXICAL_ORDERING);

		//check if lexical ordering is consistent with absolute byte value
		Assertions.assertEquals(0, input[0][0]);
		Assertions.assertEquals(1, input[1][0]);
		Assertions.assertEquals((byte)0xFF, input[2][0]);

		FSA fsa = FSABuilder.build(input);

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
		List<String> input = new ArrayList<>(25_000);
		for(int i = 0; i < 25_000; i ++)
			input.add(RandomStringUtils.randomAlphanumeric(1, 20));
		input.sort(Comparator.naturalOrder());

		FSA fsa = FSABuilder.build(input);

		FSATestUtils.checkCorrect(input, fsa);
		FSATestUtils.checkMinimal(fsa);
	}

}
