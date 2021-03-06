/**
 * Copyright (c) 2019-2020 Mauro Trevisan
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
package unit731.hunlinter.datastructures.fsa.lookup;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.datastructures.fsa.CFSA2;
import unit731.hunlinter.datastructures.fsa.FSA;
import unit731.hunlinter.datastructures.fsa.builders.FSABuilder;
import unit731.hunlinter.datastructures.fsa.builders.LexicographicalComparator;
import unit731.hunlinter.datastructures.fsa.serializers.CFSA2Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;


class FSATraversalTest{

	@Test
	void automatonHasPrefixBug(){
		FSABuilder builder = new FSABuilder();
		FSA fsa = builder.build(Arrays.asList("a".getBytes(StandardCharsets.UTF_8), "ab".getBytes(StandardCharsets.UTF_8),
			"abc".getBytes(StandardCharsets.UTF_8), "ad".getBytes(StandardCharsets.UTF_8), "bcd".getBytes(StandardCharsets.UTF_8),
			"bce".getBytes(StandardCharsets.UTF_8)));

		FSATraversal fsaTraversal = new FSATraversal(fsa);
		Assertions.assertEquals(FSAMatchResult.EXACT_MATCH, fsaTraversal.match("a".getBytes(StandardCharsets.UTF_8)).kind);
		Assertions.assertEquals(FSAMatchResult.EXACT_MATCH, fsaTraversal.match("ab".getBytes(StandardCharsets.UTF_8)).kind);
		Assertions.assertEquals(FSAMatchResult.EXACT_MATCH, fsaTraversal.match("abc".getBytes(StandardCharsets.UTF_8)).kind);
		Assertions.assertEquals(FSAMatchResult.EXACT_MATCH, fsaTraversal.match("ad".getBytes(StandardCharsets.UTF_8)).kind);

		Assertions.assertEquals(FSAMatchResult.PREFIX_MATCH, fsaTraversal.match("b".getBytes(StandardCharsets.UTF_8)).kind);
		Assertions.assertEquals(FSAMatchResult.PREFIX_MATCH, fsaTraversal.match("bc".getBytes(StandardCharsets.UTF_8)).kind);

		FSAMatchResult m = fsaTraversal.match("abcd".getBytes(StandardCharsets.UTF_8));
		Assertions.assertEquals(FSAMatchResult.AUTOMATON_HAS_PREFIX, m.kind);
		Assertions.assertEquals(3, m.index);

		m = fsaTraversal.match("ade".getBytes(StandardCharsets.UTF_8));
		Assertions.assertEquals(FSAMatchResult.AUTOMATON_HAS_PREFIX, m.kind);
		Assertions.assertEquals(2, m.index);

		m = fsaTraversal.match("ax".getBytes(StandardCharsets.UTF_8));
		Assertions.assertEquals(FSAMatchResult.AUTOMATON_HAS_PREFIX, m.kind);
		Assertions.assertEquals(1, m.index);

		Assertions.assertEquals(FSAMatchResult.NO_MATCH, fsaTraversal.match("d".getBytes(StandardCharsets.UTF_8)).kind);
	}

	@Test
	void traversalWithIterable() throws IOException{
		FSA fsa = FSA.read(getClass().getResourceAsStream("/services/fsa/builders/en_tst.dict"));
		int count = 0;
		for(ByteBuffer bb : fsa.getSequences()){
			Assertions.assertEquals(0, bb.arrayOffset());
			Assertions.assertEquals(0, bb.position());
			count ++;
		}
		Assertions.assertEquals(346773, count);
	}

	@Test
	void perfectHash() throws IOException{
		byte[][] input = new byte[][]{{'a'}, {'a', 'b', 'a'}, {'a', 'c'}, {'b'}, {'b', 'a'}, {'c'}};

		Arrays.sort(input, LexicographicalComparator.lexicographicalComparator());
		FSA s = new FSABuilder()
			.build(input);

		final byte[] fsaData = new CFSA2Serializer()
			.serializeWithNumbers()
			.serialize(s, new ByteArrayOutputStream(), null)
			.toByteArray();

		final CFSA2 fsa = FSA.read(new ByteArrayInputStream(fsaData), CFSA2.class);
		final FSATraversal traversal = new FSATraversal(fsa);

		int i = 0;
		for(byte[] seq : input)
			Assertions.assertEquals(i ++, traversal.perfectHash(seq), new String(seq));

		// Check if the total number of sequences is encoded at the root node.
		Assertions.assertEquals(6, fsa.getRightLanguageCount(fsa.getRootNode()));

		// Check sub/super sequence scenarios.
		Assertions.assertEquals(FSAMatchResult.AUTOMATON_HAS_PREFIX, traversal.perfectHash("abax".getBytes(StandardCharsets.UTF_8)));
		Assertions.assertEquals(FSAMatchResult.AUTOMATON_HAS_PREFIX, traversal.perfectHash("abx".getBytes(StandardCharsets.UTF_8)));
		Assertions.assertEquals(FSAMatchResult.PREFIX_MATCH, traversal.perfectHash("ab".getBytes(StandardCharsets.UTF_8)));
		Assertions.assertEquals(FSAMatchResult.NO_MATCH, traversal.perfectHash("d".getBytes(StandardCharsets.UTF_8)));
		Assertions.assertEquals(FSAMatchResult.NO_MATCH, traversal.perfectHash(new byte[]{0}));
	}

	@Test
	void recursiveTraversal() throws IOException{
		FSA fsa = FSA.read(getClass().getResourceAsStream("/services/fsa/builders/en_tst.dict"));

		final int[] counter = new int[]{0};

		class Recursion{
			public void dumpNode(final int node){
				int arc = fsa.getFirstArc(node);
				do{
					if(fsa.isArcFinal(arc))
						counter[0] ++;

					if(!fsa.isArcTerminal(arc))
						dumpNode(fsa.getEndNode(arc));

					arc = fsa.getNextArc(arc);
				}
				while(arc != 0);
			}
		}

		new Recursion().dumpNode(fsa.getRootNode());

		Assertions.assertEquals(346773, counter[0]);
	}

	@Test
	void match() throws IOException{
		final FSA fsa = FSA.read(getClass().getResourceAsStream("/services/fsa/builders/abc.fsa"));
		final FSATraversal traversalHelper = new FSATraversal(fsa);

		FSAMatchResult m = traversalHelper.match("ax".getBytes());
		Assertions.assertEquals(FSAMatchResult.AUTOMATON_HAS_PREFIX, m.kind);
		Assertions.assertEquals(1, m.index);
		Assertions.assertEquals(new HashSet<>(Arrays.asList("ba", "c")), suffixes(fsa, m.node));

		Assertions.assertEquals(FSAMatchResult.EXACT_MATCH, traversalHelper.match("aba".getBytes()).kind);

		m = traversalHelper.match("abalonger".getBytes());
		Assertions.assertEquals(FSAMatchResult.AUTOMATON_HAS_PREFIX, m.kind);
		Assertions.assertEquals("longer", "abalonger".substring(m.index));

		m = traversalHelper.match("ab".getBytes());
		Assertions.assertEquals(FSAMatchResult.PREFIX_MATCH, m.kind);
		Assertions.assertEquals(new HashSet<>(Collections.singletonList("a")), suffixes(fsa, m.node));
	}


	/** Return all sequences reachable from a given node, as strings */
	private HashSet<String> suffixes(FSA fsa, int node){
		HashSet<String> result = new HashSet<>();
		for(ByteBuffer bb : fsa.getSequences(node))
			result.add(new String(bb.array(), bb.position(), bb.remaining(), StandardCharsets.UTF_8));
		return result;
	}

}
