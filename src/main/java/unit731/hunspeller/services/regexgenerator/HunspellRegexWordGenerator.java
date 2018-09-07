package unit731.hunspeller.services.regexgenerator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;


/**
 * The {@code NFA} class provides a data type for creating a <em>Nondeterministic Finite state Automaton</em> (NFA) from a regular
 * expression and testing whether a given string is matched by that regular expression.
 * <p>
 * It supports the following operations: <em>concatenation</em> <em>closure</em>, <em>binary or</em>, and <em>parentheses</em>.
 * It does not support <em>mutiway or</em>, <em>character classes</em>, <em>metacharacters</em> (either in the text or pattern),
 * <em>capturing capabilities</em>, <em>greedy</em> or <em>relucantant</em> modifiers, and other features in industrial-strength implementations
 * such as {@link java.util.regex.Pattern} and {@link java.util.regex.Matcher}.
 * <p>
 * This implementation builds the NFA using a digraph and a stack and simulates the NFA using digraph search (see the textbook for details).
 * The constructor takes time proportional to <em>m</em>, where <em>m</em> is the number of characters in the regular expression.
 * <p>
 * The <em>recognizes</em> method takes time proportional to <em>m n</em>, where <em>n</em> is the number of characters in the text.
 * <p>
 * For additional documentation, see <a href="https://algs4.cs.princeton.edu/54regexp">Section 5.4</a> of
 * <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 * 
 * @see <a href="https://algs4.cs.princeton.edu/54regexp/NFA.java.html">NFA.java</a>
 */
public class HunspellRegexWordGenerator{

	@AllArgsConstructor
	private static class GeneratedElement{
		private final String word;
		private final int stateIndex;
	}

	private final Digraph graph;
	private String[] automaton;


	/**
	 * Initializes the NFA from the specified regular expression.
	 * <p>
	 * NOTE: each element should be enclosed in parentheses (eg. <code>(as)(ert)?(b)*</code>), the managed operations are <code>*</code> and <code>?</code>
	 *
	 * @param regexp	The regular expression
	 */
	public HunspellRegexWordGenerator(String regexp){
		automaton = StringUtils.split(regexp, "()");

		int m = automaton.length;
		graph = new Digraph(m + 1);
		for(int i = 0; i < m; ){
			char nextChar = (i < m - 1 && automaton[i + 1].length() == 1? automaton[i + 1].charAt(0): 0);
			switch(nextChar){
				//zero or more
				case '*':
					graph.addEdge(i - 1, i + 1);
					graph.addEdge(i, i);

					graph.setVertices(m);
					automaton = ArrayUtils.remove(automaton, i + 1);
					m --;
					break;

				//zero or one
				case '?':
					graph.addEdge(i - 1, i + 1);

					graph.setVertices(m);
					automaton = ArrayUtils.remove(automaton, i + 1);
					m --;
					break;

				//one
				default:
					graph.addEdge(i, i + 1);
					i ++;
			}
		}
	}

	/**
	 * Generate a subList with a maximum size of <code>limit</code> of words that matches the given regex.
	 * <p>
	 * The Strings are ordered in lexicographical order.
	 *
	 * @param limit	The maximum size of the list
	 * @return	The list of words that matcher the given regex
	 */
	public List<String> generateAll(int limit){
		List<String> matchedWords = new ArrayList<>(limit);

		int acceptingStateIndex = automaton.length;

		Queue<GeneratedElement> queue = new LinkedList<>();
		queue.add(new GeneratedElement(automaton[0], 0));
		while(!queue.isEmpty()){
			GeneratedElement elem = queue.remove();
			String subword = elem.word;
			int stateIndex = elem.stateIndex;

			//if this is the accepting state (skip empty generated word)
			if(!subword.isEmpty() && stateIndex == acceptingStateIndex){
				matchedWords.add(subword);

				if(matchedWords.size() == limit)
					break;
			}

			Iterable<Integer> transitions = graph.adjacentVertices(stateIndex);
			for(int transition : transitions)
				queue.add(new GeneratedElement((transition < acceptingStateIndex? subword + automaton[transition]: subword), transition));
		}

		return matchedWords;
	}

}
