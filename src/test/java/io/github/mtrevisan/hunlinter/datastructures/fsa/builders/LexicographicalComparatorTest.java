/**
 * Copyright (c) 2019-2021 Mauro Trevisan
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

import io.github.mtrevisan.hunlinter.datastructures.fsa.FSA;
import io.github.mtrevisan.hunlinter.datastructures.fsa.FSATestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;


class LexicographicalComparatorTest{

	@Test
	void lexicographicOrder(){
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
	void random25000_largerAlphabet(){
		List<byte[]> in = generateRandom(25_000, 1, 20, 0, 255);

		FSABuilder builder = new FSABuilder();
		FSA fsa = builder.build(in);

		FSATestUtils.checkCorrect(in, fsa);
		FSATestUtils.checkMinimal(fsa);
	}

	@Test
	void random25000_smallAlphabet(){
		List<byte[]> in = generateRandom(40, 1, 20, 0, 3);

		FSABuilder builder = new FSABuilder();
		FSA fsa = builder.build(in);

		FSATestUtils.checkCorrect(in, fsa);
		FSATestUtils.checkMinimal(fsa);
	}

	/** Generate a sorted list of random sequences. */
	private List<byte[]> generateRandom(int count, int lengthMin, int lengthMax, int alphabetMin, int alphabetMax){
		final List<byte[]> input = new ArrayList<>();
		final Random rnd = new Random(System.currentTimeMillis());
		for(int i = 0; i < count; i ++)
			input.add(randomByteSequence(rnd, lengthMin, lengthMax, alphabetMin, alphabetMax));
		Collections.sort(input, LexicographicalComparator.lexicographicalComparator());
		return input;
	}

	/** Generate a random string. */
	private byte[] randomByteSequence(Random rnd, int lengthMin, int lengthMax, int alphabetMin, int alphabetMax){
		byte[] bytes = new byte[lengthMin + rnd.nextInt(lengthMax - lengthMin + 1)];
		for(int i = 0; i < bytes.length; i ++)
			bytes[i] = (byte)(alphabetMin + rnd.nextInt(alphabetMax - alphabetMin + 1));
		return bytes;
	}


	private static final byte LEAST = 0;
	private static final byte GREATEST = (byte)255;

	@Test
	void lexicographicalComparator(){
		List<byte[]> ordered = Arrays.asList(
			new byte[]{},
			new byte[]{LEAST},
			new byte[]{LEAST, LEAST},
			new byte[]{LEAST, (byte)1},
			new byte[]{(byte)1},
			new byte[]{(byte)1, LEAST},
			new byte[]{GREATEST, GREATEST - (byte)1},
			new byte[]{GREATEST, GREATEST},
			new byte[]{GREATEST, GREATEST, GREATEST});

		//the Unsafe implementation if it's available (otherwise, the Java implementation)
		Comparator<byte[]> comparator = LexicographicalComparator.lexicographicalComparator();
		testComparator(comparator, ordered);

		//the Java implementation
		Comparator<byte[]> javaImpl = LexicographicalComparator.lexicographicalComparatorJavaImpl();
		testComparator(javaImpl, ordered);

		//the custom implementation
		testComparator(LexicographicalComparator.lexicographicalComparator(), ordered);
	}

	@Test
	void lexicographicalComparatorLongInputs(){
		Random rnd = new Random();
		final List<Comparator<byte[]>> comparators = Arrays.asList(
			LexicographicalComparator.lexicographicalComparator(),
			LexicographicalComparator.lexicographicalComparatorJavaImpl());
		for(Comparator<byte[]> comparator : comparators){
			for(int trials = 10; trials -- > 0; ){
				byte[] left = new byte[1 + rnd.nextInt(32)];
				rnd.nextBytes(left);
				byte[] right = left.clone();
				Assertions.assertTrue(comparator.compare(left, right) == 0);
				int i = rnd.nextInt(left.length);
				left[i] ^= (byte)(1 + rnd.nextInt(255));
				Assertions.assertTrue(comparator.compare(left, right) != 0);
				Assertions.assertEquals(comparator.compare(left, right) > 0, (left[i] & 0xFF) - (right[i] & 0xFF) > 0);
			}
		}
	}

	/**
	 * Asserts that all pairs of {@code T} values within {@code valuesInExpectedOrder} are ordered
	 * consistently between their order within {@code valuesInExpectedOrder} and the order implied by
	 * the given {@code comparator}.
	 *
	 * <p>In detail, this method asserts
	 *
	 * <ul>
	 *   <li><i>reflexivity</i>: {@code comparator.compare(t, t) = 0} for all {@code t} in {@code
	 *       valuesInExpectedOrder}; and
	 *   <li><i>consistency</i>: {@code comparator.compare(ti, tj) < 0} and {@code
	 *       comparator.compare(tj, ti) > 0} for {@code i < j}, where {@code ti =
	 *       valuesInExpectedOrder.get(i)} and {@code tj = valuesInExpectedOrder.get(j)}.
	 * </ul>
	 */
	private static <T> void testComparator(Comparator<? super T> comparator, List<T> valuesInExpectedOrder){
		//this does an O(n^2) test of all pairs of values in both orders
		for(int i = 0; i < valuesInExpectedOrder.size(); i ++){
			T t = valuesInExpectedOrder.get(i);

			for(int j = 0; j < i; j ++){
				T lesser = valuesInExpectedOrder.get(j);
				Assertions.assertTrue(comparator.compare(lesser, t) < 0, comparator + ".compare(" + lesser + ", " + t + ")");
			}

			Assertions.assertEquals(0, comparator.compare(t, t), comparator + ".compare(" + t + ", " + t + ")");

			for(int j = i + 1; j < valuesInExpectedOrder.size(); j ++){
				T greater = valuesInExpectedOrder.get(j);
				Assertions.assertTrue(comparator.compare(greater, t) > 0, comparator + ".compare(" + greater + ", " + t + ")");
			}
		}
	}

}
