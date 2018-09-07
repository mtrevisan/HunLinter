package unit731.hunspeller.services.regexgenerator;

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
public class NFA{

	//digraph of transitions
	private final Digraph graph;


	/**
	 * Initializes the NFA from the specified regular expression.
	 *
	 * @param regexp	The regular expression
	 */
	public NFA(String regexp){
		String[] parts = StringUtils.split(regexp, "()");

		int m = parts.length;
		graph = new Digraph(m + 1);
		for(int i = 0; i < m; i ++){
			//closure operator (uses 1-character lookahead)
			char nextChar = (i < m - 1 && parts[i + 1].length() == 1? parts[i + 1].charAt(0): 0);
			if(nextChar == '*'){
				graph.addEdge(i - 1, i + 2);
				graph.addEdge(i + 1, i);
			}
			else if(nextChar == '?')
				graph.addEdge(i - 1, i + 2);
		}
	}

	public static void main(String[] args){
		NFA nfa = new NFA("(as)(ert)?(b)*");
		System.out.println(nfa.graph);
	}

}
