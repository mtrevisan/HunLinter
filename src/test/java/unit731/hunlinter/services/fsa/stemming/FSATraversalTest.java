package unit731.hunlinter.services.fsa.stemming;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.services.fsa.FSA;
import unit731.hunlinter.services.fsa.FSA5;
import unit731.hunlinter.services.fsa.builders.CFSA2Serializer;
import unit731.hunlinter.services.fsa.builders.FSABuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;


public class FSATraversalTest{

	private FSA fsa;


	@BeforeAll
	public void setUp() throws Exception{
		fsa = FSA.read(this.getClass().getResourceAsStream("en_tst.dict"));
	}

	@Test
	public void testAutomatonHasPrefixBug() throws Exception{
		FSA fsa = FSABuilder.build(Arrays.asList("a", "ab", "abc", "ad", "bcd", "bce"));

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
	public void testTraversalWithIterable(){
		int count = 0;
		for(ByteBuffer bb : fsa.getSequences()){
			Assertions.assertEquals(0, bb.arrayOffset());
			Assertions.assertEquals(0, bb.position());
			count++;
		}
		Assertions.assertEquals(346773, count);
	}

	@Test
	public void testPerfectHash() throws IOException{
		byte[][] input = new byte[][]{{'a'}, {'a', 'b', 'a'}, {'a', 'c'}, {'b'}, {'b', 'a'}, {'c'},};

		Arrays.sort(input, FSABuilder.LEXICAL_ORDERING);
		FSA s = FSABuilder.build(input);

		final byte[] fsaData = new CFSA2Serializer()
			.serialize(s, new ByteArrayOutputStream())
			.toByteArray();

		final FSA5 fsa = FSA.read(new ByteArrayInputStream(fsaData), FSA5.class);
		final FSATraversal traversal = new FSATraversal(fsa);

		int i = 0;
		for(byte[] seq : input){
			Assertions.assertEquals(new String(seq), i ++, traversal.perfectHash(seq));
		}

		// Check if the total number of sequences is encoded at the root node.
		Assertions.assertEquals(6, fsa.getRightLanguageCount(fsa.getRootNode()));

		// Check sub/super sequence scenarios.
		Assertions.assertEquals(MatchResult.AUTOMATON_HAS_PREFIX, traversal.perfectHash("abax".getBytes(StandardCharsets.UTF_8)));
		Assertions.assertEquals(MatchResult.AUTOMATON_HAS_PREFIX, traversal.perfectHash("abx".getBytes(StandardCharsets.UTF_8)));
		Assertions.assertEquals(MatchResult.SEQUENCE_IS_A_PREFIX, traversal.perfectHash("ab".getBytes(StandardCharsets.UTF_8)));
		Assertions.assertEquals(MatchResult.NO_MATCH, traversal.perfectHash("d".getBytes(StandardCharsets.UTF_8)));
		Assertions.assertEquals(MatchResult.NO_MATCH, traversal.perfectHash(new byte[]{0}));

		Assertions.assertTrue(MatchResult.AUTOMATON_HAS_PREFIX < 0);
		Assertions.assertTrue(MatchResult.SEQUENCE_IS_A_PREFIX < 0);
		Assertions.assertTrue(MatchResult.NO_MATCH < 0);
	}

	@Test
	public void testRecursiveTraversal(){
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
	public void testMatch() throws IOException{
		final FSA fsa = FSA.read(this.getClass().getResourceAsStream("abc.fsa"));
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

	/**
	 * Return all sequences reachable from a given node, as strings.
	 */
	private HashSet<String> suffixes(FSA fsa, int node){
		HashSet<String> result = new HashSet<String>();
		for(ByteBuffer bb : fsa.getSequences(node)){
			result.add(new String(bb.array(), bb.position(), bb.remaining(), StandardCharsets.UTF_8));
		}
		return result;
	}

}
