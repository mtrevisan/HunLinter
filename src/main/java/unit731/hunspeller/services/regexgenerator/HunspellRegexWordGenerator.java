package unit731.hunspeller.services.regexgenerator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.PatternHelper;


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
 * @see <a href="https://algs4.cs.princeton.edu/lectures/54RegularExpressions.pdf">Algorithms - Robert Sedgewick, Kevin Wayne</a>
 * @see <a href="https://www.dennis-grinch.co.uk/tutorial/enfa">ε-NFA in Java</a>
 */
@ToString
public class HunspellRegexWordGenerator{

	private static final Pattern SPLITTER = PatternHelper.pattern("(?<!\\\\)[()]");

	private static final String LEFT_PARENTHESIS = "(";
	private static final String RIGHT_PARENTHESIS = ")";


	@AllArgsConstructor
	private static class GeneratedElement{
		private final String word;
		private final int stateIndex;
	}

	//graph of ε-transitions
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
		automaton = PatternHelper.split(regexp, SPLITTER);
		automaton = ArrayUtils.remove(automaton, 0);

		int m = automaton.length;
		graph = new Digraph();
		for(int i = 1; i < m; i += 2)
			if(!automaton[i].isEmpty())
				switch(automaton[i].charAt(0)){
					//zero or more
					case '*':
						graph.addEdge(i - 1, i);
						graph.addEdge(i, i - 1);
						break;

					//zero or one
					case '?':
						graph.addEdge(i - 1, i);
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

		int finalStateIndex = automaton.length;
		StringBuilder sb = new StringBuilder();

		Queue<GeneratedElement> queue = new LinkedList<>();
		queue.add(new GeneratedElement(StringUtils.EMPTY, 0));
		while(!queue.isEmpty()){
			GeneratedElement elem = queue.remove();
			String subword = elem.word;
			int stateIndex = elem.stateIndex;

			//final state not reached, add transitions
			if(stateIndex < finalStateIndex){
				Iterable<Integer> transitions = graph.adjacentVertices(stateIndex);
				for(int transition : transitions)
					queue.add(new GeneratedElement(subword, transition));

				String part = automaton[stateIndex];
				int size = part.length();
				if(size > 0){
					char op = (size == 1? part.charAt(0): 0);
					if(op != '*' && op != '?'){
						sb.setLength(0);
						subword = sb.append(subword)
							.append(LEFT_PARENTHESIS)
							.append(part)
							.append(RIGHT_PARENTHESIS)
							.toString();
					}
				}
				queue.add(new GeneratedElement(subword, stateIndex + 1));
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

}
