package unit731.hunlinter.services.fsa.stemming;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.services.fsa.FSA;
import unit731.hunlinter.services.fsa.builders.FSABuilder;
import unit731.hunlinter.services.text.StringHelper;

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
			.map(StringHelper::getRawBytes)
			.collect(Collectors.toList());
		FSABuilder builder = new FSABuilder();
		FSA fsa = builder.build(input);

		FSATraversal fsaTraversal = new FSATraversal(fsa);
		Assertions.assertEquals(MatchResult.EXACT_MATCH, fsaTraversal.match(StringHelper.getRawBytes("a")).kind);
		Assertions.assertEquals(MatchResult.EXACT_MATCH, fsaTraversal.match(StringHelper.getRawBytes("ab")).kind);
		Assertions.assertEquals(MatchResult.EXACT_MATCH, fsaTraversal.match(StringHelper.getRawBytes("abc")).kind);
		Assertions.assertEquals(MatchResult.EXACT_MATCH, fsaTraversal.match(StringHelper.getRawBytes("ad")).kind);

		Assertions.assertEquals(MatchResult.SEQUENCE_IS_A_PREFIX, fsaTraversal.match(StringHelper.getRawBytes("b")).kind);
		Assertions.assertEquals(MatchResult.SEQUENCE_IS_A_PREFIX, fsaTraversal.match(StringHelper.getRawBytes("bc")).kind);

		MatchResult m;

		m = fsaTraversal.match(StringHelper.getRawBytes("abcd"));
		Assertions.assertEquals(MatchResult.AUTOMATON_HAS_PREFIX, m.kind);
		Assertions.assertEquals(3, m.index);

		m = fsaTraversal.match(StringHelper.getRawBytes("ade"));
		Assertions.assertEquals(MatchResult.AUTOMATON_HAS_PREFIX, m.kind);
		Assertions.assertEquals(2, m.index);

		m = fsaTraversal.match(StringHelper.getRawBytes("ax"));
		Assertions.assertEquals(MatchResult.AUTOMATON_HAS_PREFIX, m.kind);
		Assertions.assertEquals(1, m.index);

		Assertions.assertEquals(MatchResult.NO_MATCH, fsaTraversal.match(StringHelper.getRawBytes("d")).kind);
	}

	@Test
	void testTraversalWithIterable() throws IOException{
		FSA fsa = FSA.read(FSATraversalTest.class.getResourceAsStream("/services/fsa/stemming/en_tst.dict"));
		int count = 0;
		for(ByteBuffer bb : fsa.getSequences()){
			Assertions.assertEquals(0, bb.arrayOffset());
			Assertions.assertEquals(0, bb.position());
			count ++;
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
						counter[0] ++;
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

		MatchResult m = traversalHelper.match(StringHelper.getRawBytes("ax"));
		Assertions.assertEquals(MatchResult.AUTOMATON_HAS_PREFIX, m.kind);
		Assertions.assertEquals(1, m.index);
		Assertions.assertEquals(new HashSet<>(Arrays.asList("ba", "c")), suffixes(fsa, m.node));

		Assertions.assertEquals(MatchResult.EXACT_MATCH, traversalHelper.match(StringHelper.getRawBytes("aba")).kind);

		m = traversalHelper.match(StringHelper.getRawBytes("abalonger"));
		Assertions.assertEquals(MatchResult.AUTOMATON_HAS_PREFIX, m.kind);
		Assertions.assertEquals("longer", "abalonger".substring(m.index));

		m = traversalHelper.match(StringHelper.getRawBytes("ab"));
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
