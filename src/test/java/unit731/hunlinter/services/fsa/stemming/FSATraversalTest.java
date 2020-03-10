package unit731.hunlinter.services.fsa.stemming;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.services.fsa.FSA;
import unit731.hunlinter.services.fsa.builders.FSABuilder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


class FSATraversalTest{

	@Test
	void testAutomatonHasPrefixBug(){
		List<String> words = Arrays.asList("a", "ab", "abc", "ad", "bcd", "bce");
		List<byte[]> input = words.stream()
			.map(word -> word.getBytes(StandardCharsets.UTF_8))
			.collect(Collectors.toList());
		FSA fsa = FSABuilder.build(input);

		FSATraversal fsaTraversal = new FSATraversal(fsa);
		Assertions.assertEquals(MatchResult.EXACT_MATCH, fsaTraversal.match("a".getBytes(StandardCharsets.UTF_8)).kind);
		Assertions.assertEquals(MatchResult.EXACT_MATCH, fsaTraversal.match("ab".getBytes(StandardCharsets.UTF_8)).kind);
		Assertions.assertEquals(MatchResult.EXACT_MATCH, fsaTraversal.match("abc".getBytes(StandardCharsets.UTF_8)).kind);
		Assertions.assertEquals(MatchResult.EXACT_MATCH, fsaTraversal.match("ad".getBytes(StandardCharsets.UTF_8)).kind);

		Assertions.assertEquals(MatchResult.SEQUENCE_IS_A_PREFIX, fsaTraversal.match("b".getBytes(StandardCharsets.UTF_8)).kind);
		Assertions.assertEquals(MatchResult.SEQUENCE_IS_A_PREFIX, fsaTraversal.match("bc".getBytes(StandardCharsets.UTF_8)).kind);

		MatchResult m;

		m = fsaTraversal.match("abcd".getBytes(StandardCharsets.UTF_8));
		Assertions.assertEquals(MatchResult.AUTOMATON_HAS_PREFIX, m.kind);
		Assertions.assertEquals(3, m.index);

		m = fsaTraversal.match("ade".getBytes(StandardCharsets.UTF_8));
		Assertions.assertEquals(MatchResult.AUTOMATON_HAS_PREFIX, m.kind);
		Assertions.assertEquals(2, m.index);

		m = fsaTraversal.match("ax".getBytes(StandardCharsets.UTF_8));
		Assertions.assertEquals(MatchResult.AUTOMATON_HAS_PREFIX, m.kind);
		Assertions.assertEquals(1, m.index);

		Assertions.assertEquals(MatchResult.NO_MATCH, fsaTraversal.match("d".getBytes(StandardCharsets.UTF_8)).kind);
	}

	@Test
	void testTraversalWithIterable() throws IOException{
		FSA fsa = FSA.read(FSATraversalTest.class.getResourceAsStream("/services/fsa/stemming/en_tst.dict"));
		int count = 0;
		for(ByteBuffer bb : fsa.getSequences()){
			Assertions.assertEquals(0, bb.arrayOffset());
			Assertions.assertEquals(0, bb.position());
			count++;
		}
		Assertions.assertEquals(346773, count);
	}

	@Test
	void testRecursiveTraversal() throws IOException{
		FSA fsa = FSA.read(FSATraversalTest.class.getResourceAsStream("/services/fsa/stemming/en_tst.dict"));

		final int[] counter = new int[]{0};

		class Recursion{
			public void dumpNode(final int node){
				int arc = fsa.getFirstArc(node);
				do{
					if(fsa.isArcFinal(arc)){
						counter[0]++;
					}

					if(!fsa.isArcTerminal(arc)){
						dumpNode(fsa.getEndNode(arc));
					}

					arc = fsa.getNextArc(arc);
				}while(arc != 0);
			}
		}

		new Recursion().dumpNode(fsa.getRootNode());

		Assertions.assertEquals(346773, counter[0]);
	}

	@Test
	void testMatch() throws IOException{
		final FSA fsa = FSA.read(FSATraversalTest.class.getResourceAsStream("/services/fsa/stemming/abc.fsa"));
		final FSATraversal traversalHelper = new FSATraversal(fsa);

		MatchResult m = traversalHelper.match("ax".getBytes());
		Assertions.assertEquals(MatchResult.AUTOMATON_HAS_PREFIX, m.kind);
		Assertions.assertEquals(1, m.index);
		Assertions.assertEquals(new HashSet<>(Arrays.asList("ba", "c")), suffixes(fsa, m.node));

		Assertions.assertEquals(MatchResult.EXACT_MATCH, traversalHelper.match("aba".getBytes()).kind);

		m = traversalHelper.match("abalonger".getBytes());
		Assertions.assertEquals(MatchResult.AUTOMATON_HAS_PREFIX, m.kind);
		Assertions.assertEquals("longer", "abalonger".substring(m.index));

		m = traversalHelper.match("ab".getBytes());
		Assertions.assertEquals(MatchResult.SEQUENCE_IS_A_PREFIX, m.kind);
		Assertions.assertEquals(new HashSet<>(Arrays.asList("a")), suffixes(fsa, m.node));
	}

	/** Return all sequences reachable from a given node, as strings */
	private Set<String> suffixes(FSA fsa, int node){
		Set<String> result = new HashSet<>();
		for(ByteBuffer bb : fsa.getSequences(node))
			result.add(new String(bb.array(), bb.position(), bb.remaining(), StandardCharsets.UTF_8));
		return result;
	}

}
