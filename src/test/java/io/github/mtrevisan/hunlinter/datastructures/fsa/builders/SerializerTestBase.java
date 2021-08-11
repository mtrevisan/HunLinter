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
import io.github.mtrevisan.hunlinter.datastructures.fsa.serializers.CFSA2Serializer;
import io.github.mtrevisan.hunlinter.datastructures.fsa.serializers.FSASerializer;
import io.github.mtrevisan.hunlinter.services.text.StringHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class SerializerTestBase{

	protected FSASerializer createSerializer(){
		return new CFSA2Serializer();
	}


	@Test
	void a() throws IOException{
		List<String> input = Collections.singletonList("a");
		List<byte[]> in = input.stream()
			.sorted()
			.map(StringHelper::getRawBytes)
			.collect(Collectors.toList());

		FSABuilder builder = new FSABuilder();
		FSA s = builder.build(in);

		checkSerialization(in, s);
	}

	@Test
	void arcsSharing() throws IOException{
		List<String> input = Arrays.asList("acf", "adg", "aeh", "bdg", "beh");
		List<byte[]> in = input.stream()
			.sorted()
			.map(StringHelper::getRawBytes)
			.collect(Collectors.toList());

		FSABuilder builder = new FSABuilder();
		FSA s = builder.build(in);

		checkSerialization(in, s);
	}

	@Test
	void fsa5SerializerSimple() throws IOException{
		List<String> input = Arrays.asList("a", "aba", "ac", "b", "ba", "c");
		List<byte[]> in = input.stream()
			.sorted()
			.map(StringHelper::getRawBytes)
			.collect(Collectors.toList());

		FSABuilder builder = new FSABuilder();
		FSA s = builder.build(in);

		checkSerialization(in, s);
	}

	@Test
	void notMinimal() throws IOException{
		List<String> input = Arrays.asList("aba", "b", "ba");
		List<byte[]> in = input.stream()
			.sorted()
			.map(StringHelper::getRawBytes)
			.collect(Collectors.toList());

		FSABuilder builder = new FSABuilder();
		FSA s = builder.build(in);

		checkSerialization(in, s);
	}

	@Test
	void fsa5Bug0() throws IOException{
		checkCorrect(Arrays.asList("3-D+A+JJ", "3-D+A+NN", "4-F+A+NN", "z+A+NN"));
	}

	@Test
	void fsa5Bug1() throws IOException{
		checkCorrect(Arrays.asList("+NP", "n+N", "n+NP"));
	}

	private void checkCorrect(List<String> input) throws IOException{
		List<byte[]> in = input.stream()
			.sorted()
			.map(StringHelper::getRawBytes)
			.collect(Collectors.toList());

		FSABuilder builder = new FSABuilder();
		FSA s = builder.build(in);

		checkSerialization(in, s);
	}

	@Test
	void emptyInput() throws IOException{
		List<byte[]> input = Collections.emptyList();
		FSABuilder builder = new FSABuilder();
		FSA s = builder.build(input);

		checkSerialization(input, s);
	}

	@Test
	void abc() throws IOException{
		testBuiltIn(FSA.read(getClass().getResourceAsStream("/services/fsa/builders/abc.fsa")));
	}

	@Test
	void minimal() throws IOException{
		testBuiltIn(FSA.read(getClass().getResourceAsStream("/services/fsa/builders/minimal.fsa")));
	}

	@Test
	void minimal2() throws IOException{
		testBuiltIn(FSA.read(getClass().getResourceAsStream("/services/fsa/builders/minimal2.fsa")));
	}

	@Test
	void en_tst() throws IOException{
		testBuiltIn(FSA.read(getClass().getResourceAsStream("/services/fsa/builders/en_tst.dict")));
	}

	private void testBuiltIn(FSA fsa) throws IOException{
		List<byte[]> input = new ArrayList<>();
		for(ByteBuffer bb : fsa)
			input.add(Arrays.copyOf(bb.array(), bb.remaining()));
		Collections.sort(input, LexicographicalComparator.lexicographicalComparator());

		FSABuilder builder = new FSABuilder();
		FSA root = builder.build(input);

		//check if the DFSA is correct first
		FSATestUtils.checkCorrect(input, root);

		//check serialization
		checkSerialization(input, root);
	}

	private void checkSerialization(List<byte[]> input, FSA root) throws IOException{
		FSASerializer serializer = createSerializer();
		checkSerialization0(serializer, input, root);
		if(serializer.getFlags().contains(FSAFlags.NUMBERS))
			checkSerialization0(serializer.serializeWithNumbers(), input, root);
	}

	private void checkSerialization0(FSASerializer serializer, List<byte[]> in, FSA root) throws IOException{
		byte[] fsaData = serializer.serialize(root, new ByteArrayOutputStream(), null).toByteArray();

		FSA fsa = FSA.read(new ByteArrayInputStream(fsaData));
		FSATestUtils.checkCorrect(in, fsa);
	}

	@Test
	void automatonWithNodeNumbers() throws IOException{
		Assertions.assertTrue(createSerializer().getFlags().contains(FSAFlags.NUMBERS));

		List<String> input = Arrays.asList("a", "aba", "ac", "b", "ba", "c");
		List<byte[]> in = input.stream()
			.sorted()
			.map(StringHelper::getRawBytes)
			.collect(Collectors.toList());

		FSABuilder builder = new FSABuilder();
		FSA s = builder.build(in);

		byte[] fsaData = createSerializer()
			.serializeWithNumbers()
			.serialize(s, new ByteArrayOutputStream(), null)
			.toByteArray();

		FSA fsa = FSA.read(new ByteArrayInputStream(fsaData));

		//ensure we have the NUMBERS flag set
		Assertions.assertTrue(fsa.getFlags().contains(FSAFlags.NUMBERS));

		//get all numbers from nodes
		byte[] buffer = new byte[128];
		List<String> result = new ArrayList<>();
		FSATestUtils.walkNode(buffer, 0, fsa, fsa.getRootNode(), 0, result);

		Collections.sort(result);
		Assertions.assertEquals(Arrays.asList("0 a", "1 aba", "2 ac", "3 b", "4 ba", "5 c"), result);
	}

}
