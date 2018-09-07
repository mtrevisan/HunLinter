package unit731.hunspeller.services.regexgenerator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import lombok.AllArgsConstructor;
import lombok.ToString;
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
 * @see <a href="https://www.dennis-grinch.co.uk/tutorial/enfa">ε-NFA in Java</a>
 */
@ToString
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

		int m = automaton.length + 1;
		graph = new Digraph();
		graph.addEdge(0, 1);
		for(int i = 1; i < m; ){
			char nextChar = (i < m - 1 && automaton[i].length() == 1? automaton[i].charAt(0): 0);
			switch(nextChar){
				//zero or more
				case '*':
					graph.addEpsilonTransition(i - 1, i + 1);
					graph.addEpsilonTransition(i, i);

					automaton = ArrayUtils.remove(automaton, i);
					m --;
					break;

				//zero or one
				case '?':
					graph.addEpsilonTransition(i - 1, i + 1);

					automaton = ArrayUtils.remove(automaton, i);
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

		int finalStateIndex = automaton.length + 1;

		Queue<GeneratedElement> queue = new LinkedList<>();
		queue.add(new GeneratedElement(StringUtils.EMPTY, 0));
		while(!queue.isEmpty()){
			GeneratedElement elem = queue.remove();
			String subword = elem.word;
			int stateIndex = elem.stateIndex;

			//final state not reached, add transitions
			if(stateIndex < finalStateIndex){
				Iterable<Integer> transitions = graph.epsilonTransitionVertices(stateIndex);
				for(int transition : transitions)
					queue.add(new GeneratedElement(subword, transition));
				transitions = graph.adjacentVertices(stateIndex);
				for(int transition : transitions)
					queue.add(new GeneratedElement((transition < finalStateIndex? subword + automaton[transition - 1]: subword), transition));
			}
			//if this is the accepting state add the generated word (skip empty generated word)
			else if(!subword.isEmpty()){
				matchedWords.add(subword);

				if(matchedWords.size() == limit)
					break;
			}
		}

		return matchedWords;
	}

	public static void main(String[] args){
		HunspellRegexWordGenerator nfa = new HunspellRegexWordGenerator("(as)(ert)?(b)*");
		System.out.println(nfa.graph);

		List<String> words = nfa.generateAll(10);
		words.forEach(System.out::println);
	}

}
