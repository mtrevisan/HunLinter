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
package io.github.mtrevisan.hunlinter.datastructures.fsa.serializers;

import io.github.mtrevisan.hunlinter.datastructures.fsa.FSAAbstract;
import io.github.mtrevisan.hunlinter.datastructures.fsa.FSATestUtils;
import io.github.mtrevisan.hunlinter.datastructures.fsa.builders.FSABuilder;
import io.github.mtrevisan.hunlinter.datastructures.fsa.builders.FSAFlags;
import io.github.mtrevisan.hunlinter.datastructures.fsa.builders.LexicographicalComparator;
import io.github.mtrevisan.hunlinter.services.text.StringHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


class CFSASerializerTest{

	@Test
	void emptyInput() throws IOException{
		testInput(Collections.emptyList());
	}

	@Test
	void a() throws IOException{
		testInput(Collections.singletonList("a"));
	}

	@Test
	void arcsSharing() throws IOException{
		testInput(Arrays.asList("acf", "adg", "aeh", "bdg", "beh"));
	}

	@Test
	void fsa5SerializerSimple() throws IOException{
		testInput(Arrays.asList("a", "aba", "ac", "b", "ba", "c"));
	}

	@Test
	void notMinimal() throws IOException{
		testInput(Arrays.asList("aba", "b", "ba"));
	}

	@Test
	void fsa5Bug0() throws IOException{
		testInput(Arrays.asList("3-D+A+JJ", "3-D+A+NN", "4-F+A+NN", "z+A+NN"));
	}

	@Test
	void fsa5Bug1() throws IOException{
		testInput(Arrays.asList("+NP", "n+N", "n+NP"));
	}

	private void testInput(List<String> input) throws IOException{
		input.sort(Comparator.naturalOrder());

		List<byte[]> in = input.stream()
			.map(word -> StringHelper.getRawBytes(word))
			.collect(Collectors.toList());
		FSABuilder builder = new FSABuilder();
		FSAAbstract fsa = builder.build(in);

		checkSerialization(in, fsa);
	}


	@Test
	void abc() throws IOException{
		testInput("abc.fsa");
	}

	@Test
	void minimal() throws IOException{
		testInput("minimal.fsa");
	}

	@Test
	void minimal2() throws IOException{
		testInput("minimal2.fsa");
	}

	@Test
	void en_tst() throws IOException{
		testInput("en_tst.dict");
	}

	private void testInput(String fsaFilename) throws IOException{
		InputStream stream = CFSASerializerTest.class.getResourceAsStream("/services/fsa/builders/" + fsaFilename);
		FSAAbstract fsa1 = FSAAbstract.read(stream);

		List<byte[]> input = new ArrayList<>();
		for(ByteBuffer bb : fsa1)
			input.add(bb.array());
		Collections.sort(input, LexicographicalComparator.lexicographicalComparator());

		FSABuilder builder = new FSABuilder();
		FSAAbstract fsa2 = builder.build(input);

		//check if the DFSA is correct first
		FSATestUtils.checkCorrect(input, fsa2);

		//check serialization
		checkSerialization(input, fsa2);
	}

	private void checkSerialization(List<byte[]> input, FSAAbstract root) throws IOException{
		checkSerialization0(createSerializer(), input, root);
		if(createSerializer().getSupportedFlags().contains(FSAFlags.NUMBERS))
			checkSerialization0(createSerializer().serializeWithNumbers(), input, root);
	}

	private void checkSerialization0(FSASerializerInterface serializer, List<byte[]> in, FSAAbstract root) throws IOException{
		final byte[] fsaData = serializer.serialize(root, new ByteArrayOutputStream(), null).toByteArray();

		FSAAbstract fsa = FSAAbstract.read(new ByteArrayInputStream(fsaData));
		FSATestUtils.checkCorrect(in, fsa);
	}

	@Test
	void automatonWithNodeNumbers() throws IOException{
		Assertions.assertTrue(createSerializer().getSupportedFlags().contains(FSAFlags.NUMBERS));

		List<String> input = Arrays.asList("a", "aba", "ac", "b", "ba", "c");
		input.sort(Comparator.naturalOrder());

		List<byte[]> in = input.stream()
			.map(StringHelper::getRawBytes)
			.collect(Collectors.toList());
		FSABuilder builder = new FSABuilder();
		FSAAbstract fsa1 = builder.build(in);

		byte[] fsaData = createSerializer().serializeWithNumbers().serialize(fsa1, new ByteArrayOutputStream(), null).toByteArray();

		FSAAbstract fsa2 = FSAAbstract.read(new ByteArrayInputStream(fsaData));

		// Ensure we have the NUMBERS flag set.
		Assertions.assertTrue(fsa2.getFlags().contains(FSAFlags.NUMBERS));

		// Get all numbers from nodes.
		byte[] buffer = new byte[128];
		ArrayList<String> result = new ArrayList<>();
		FSATestUtils.walkNode(buffer, 0, fsa2, fsa2.getRootNode(), 0, result);

		Collections.sort(result);
		Assertions.assertEquals(Arrays.asList("0 a", "1 aba", "2 ac", "3 b", "4 ba", "5 c"), result);
	}

	private FSASerializerInterface createSerializer(){
		return new CFSASerializer();
	}

}
