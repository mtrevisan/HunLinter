/**
 * Copyright (c) 2019-2022 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.datastructures.fsa.builders;

import io.github.mtrevisan.hunlinter.datastructures.fsa.FSAAbstract;
import io.github.mtrevisan.hunlinter.datastructures.fsa.FSATestUtils;
import io.github.mtrevisan.hunlinter.services.text.StringHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


class FSABuilderTest{

	@Test
	void emptyInput(){
		List<byte[]> input = Collections.emptyList();

		FSABuilder builder = new FSABuilder();
		FSAAbstract fsa = builder.build(input);

		FSATestUtils.checkCorrect(input, fsa);
	}

	@Test
	void hashResizeBug(){
		List<String> input = Arrays.asList("01", "02", "11", "21");
		List<byte[]> in = input.stream()
			.map(StringHelper::getRawBytes)
			.collect(Collectors.toList());

		FSABuilder builder = new FSABuilder();
		FSAAbstract fsa = builder.build(in);

		FSATestUtils.checkCorrect(in, fsa);
		FSATestUtils.checkMinimal(fsa);
	}

	@Test
	void smallInput(){
		List<String> input = Arrays.asList("abc", "bbc", "d");
		List<byte[]> in = input.stream()
			.map(StringHelper::getRawBytes)
			.collect(Collectors.toList());

		FSABuilder builder = new FSABuilder();
		FSAAbstract fsa = builder.build(in);

		FSATestUtils.checkCorrect(in, fsa);
	}

	@Test
	void lexicographicOrder(){
		List<byte[]> input = Arrays.asList(new byte[]{0}, new byte[]{1}, new byte[]{(byte)0xFF});
		input.sort(LexicographicalComparator.lexicographicalComparator());

		//check if lexical ordering is consistent with absolute byte value
		Assertions.assertEquals(0, input.get(0)[0]);
		Assertions.assertEquals(1, input.get(1)[0]);
		Assertions.assertEquals((byte)0xFF, input.get(2)[0]);

		FSABuilder builder = new FSABuilder();
		FSAAbstract fsa = builder.build(input);

		FSATestUtils.checkCorrect(input, fsa);

		int arc = fsa.getFirstArc(fsa.getRootNode());
		Assertions.assertEquals(0, fsa.getArcLabel(arc));
		arc = fsa.getNextArc(arc);
		Assertions.assertEquals(1, fsa.getArcLabel(arc));
		arc = fsa.getNextArc(arc);
		Assertions.assertEquals((byte)0xFF, fsa.getArcLabel(arc));
	}

	@Test
	void random25000_largerAlphabet(){
		List<byte[]> in = generateRandom(25_000, 1, 20, 0, 255);

		FSABuilder builder = new FSABuilder();
		FSAAbstract fsa = builder.build(in);

		FSATestUtils.checkCorrect(in, fsa);
		FSATestUtils.checkMinimal(fsa);
	}

	@Test
	void random25000_smallAlphabet(){
		List<byte[]> in = generateRandom(40, 1, 20, 0, 3);

		FSABuilder builder = new FSABuilder();
		FSAAbstract fsa = builder.build(in);

		FSATestUtils.checkCorrect(in, fsa);
		FSATestUtils.checkMinimal(fsa);
	}

	/** Generate a sorted list of random sequences. */
	private List<byte[]> generateRandom(int count, int lengthMin, int lengthMax, int alphabetMin, int alphabetMax){
		final List<byte[]> input = new ArrayList<>();
		final Random rnd = new Random(System.currentTimeMillis());
		for(int i = 0; i < count; i ++)
			input.add(randomByteSequence(rnd, lengthMin, lengthMax, alphabetMin, alphabetMax));
		input.sort(LexicographicalComparator.lexicographicalComparator());
		return input;
	}

	/** Generate a random string. */
	private byte[] randomByteSequence(Random rnd, int lengthMin, int lengthMax, int alphabetMin, int alphabetMax){
		byte[] bytes = new byte[lengthMin + rnd.nextInt(lengthMax - lengthMin + 1)];
		for(int i = 0; i < bytes.length; i ++)
			bytes[i] = (byte)(alphabetMin + rnd.nextInt(alphabetMax - alphabetMin + 1));
		return bytes;
	}

}
