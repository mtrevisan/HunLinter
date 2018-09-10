package unit731.hunspeller.services.regexgenerator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
 * @see <a href="https://pdfs.semanticscholar.org/presentation/e14c/b69f0feb2856734a5e5e85b6ae1a210ab936.pdf">Automata & Languages</a>
 * @see <a href="http://www.dfki.de/compling/pdfs/SS06-fsa-presentation.pdf">Finite-State Automata and Algorithms</a>
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
	private final Digraph<String> graph = new Digraph<>();
	private final int finalStateIndex;


	/**
	 * Initializes the NFA from the specified regular expression.
	 * <p>
	 * NOTE: each element should be enclosed in parentheses (eg. <code>(as)(ert)?(b)*</code>), the managed operations are <code>*</code> and <code>?</code>
	 *
	 * @param regexp	The regular expression
	 */
	public HunspellRegexWordGenerator(String regexp){
		String[] parts = PatternHelper.split(regexp, SPLITTER);
		finalStateIndex = parts.length >> 1;
		for(int i = 0; i < finalStateIndex; i ++){
			int j = (i << 1) + 1;
			String next = (j + 1 < parts.length? parts[j + 1]: StringUtils.EMPTY);
			if(next.isEmpty())
				//one
				graph.addEdge(i, i + 1, parts[j]);
			else
				switch(next.charAt(0)){
					//zero or more
					case '*':
						graph.addEdge(i, i + 1);
						graph.addEdge(i, i, parts[j]);
						break;

					//zero or one
					case '?':
						graph.addEdge(i, i + 1, parts[j]);
						graph.addEdge(i, i + 1);
						break;
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

		StringBuilder sb = new StringBuilder();

		Queue<GeneratedElement> queue = new LinkedList<>();
		queue.add(new GeneratedElement(StringUtils.EMPTY, 0));
		while(!queue.isEmpty()){
			GeneratedElement elem = queue.remove();
			String subword = elem.word;
			int stateIndex = elem.stateIndex;

			//final state not reached, add transitions
			if(stateIndex < finalStateIndex){
				Iterable<Pair<Integer, String>> transitions = graph.adjacentVertices(stateIndex);
				for(Pair<Integer, String> transition : transitions){
					int key = transition.getKey();
					String value = transition.getValue();

					String nextword = subword;
					if(StringUtils.isNotBlank(value)){
						sb.setLength(0);
						nextword = sb.append(subword)
							.append(LEFT_PARENTHESIS)
							.append(value)
							.append(RIGHT_PARENTHESIS)
							.toString();
					}

					queue.add(new GeneratedElement(nextword, key));
				}
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
