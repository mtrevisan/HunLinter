package unit731.hunlinter.services.fsa.builders;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.services.fsa.FSA;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
		List<String> input = new ArrayList<>(25_000);
		for(int i = 0; i < 25_000; i ++)
			input.add(RandomStringUtils.randomAlphanumeric(1, 20));
		input.sort(Comparator.naturalOrder());

		List<byte[]> in = input.stream()
			.map(word -> word.getBytes(StandardCharsets.UTF_8))
			.collect(Collectors.toList());
		FSA fsa = FSABuilder.build(in);

		FSATestUtils.checkCorrect(in, fsa);
		FSATestUtils.checkMinimal(fsa);
	}

}
