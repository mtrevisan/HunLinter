package unit731.hunlinter.services.regexgenerator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunlinter.services.log.ShortPrefixNotNullToStringStyle;


/**
 * The {@code NFA} class provides a data type for creating a <em>Non-deterministic Finite state Automaton</em> (NFA) from a regular
 * expression and testing whether a given string is matched by that regular expression.
 *
 * @see <a href="https://algs4.cs.princeton.edu/54regexp/NFA.java.html">NFA.java</a>
 * @see <a href="https://algs4.cs.princeton.edu/lectures/54RegularExpressions.pdf">Algorithms - Robert Sedgewick, Kevin Wayne</a>
 * @see <a href="https://www.dennis-grinch.co.uk/tutorial/enfa">ε-NFA in Java</a>
 * @see <a href="https://pdfs.semanticscholar.org/presentation/e14c/b69f0feb2856734a5e5e85b6ae1a210ab936.pdf">Automata &amp; Languages</a>
 * @see <a href="http://www.dfki.de/compling/pdfs/SS06-fsa-presentation.pdf">Finite-State Automata and Algorithms</a>
 */
public class HunSpellRegexWordGenerator{

	private static class GeneratedElement{
		private final List<String> word;
		private final int stateIndex;

		GeneratedElement(final List<String> word, final int stateIndex){
			this.word = word;
			this.stateIndex = stateIndex;
		}

	}


	private final Digraph<String> graph = new Digraph<>();
	private final int finalStateIndex;


	/**
	 * Initializes the NFA from the specified regular expression.
	 * <p>
	 * NOTE: each element should be enclosed in parentheses (eg. <code>(as)(ert)?(b)*</code>), the managed operations are <code>*</code> and <code>?</code>
	 *
	 * @param regexpParts	The regular expression already subdivided into input and modifiers (eg. ["ag", "ert", "?", "b", "*"])
	 */
	public HunSpellRegexWordGenerator(final String[] regexpParts){
		int offset = 0;
		final int size = regexpParts.length;
		for(int i = 0; i + offset < size; i ++){
			final int operatorIndex = i + offset + 1;
			final char next = (operatorIndex < size && regexpParts[operatorIndex].length() == 1?
				regexpParts[operatorIndex].charAt(0): 0);
			switch(next){
				//zero or more
				case '*':
					graph.addEdge(i, i + 1);
					graph.addEdge(i, i, regexpParts[operatorIndex - 1]);

					//skip operator
					offset ++;
					break;

				//zero or one
				case '?':
					graph.addEdge(i, i + 1, regexpParts[operatorIndex - 1]);
					graph.addEdge(i, i + 1);

					//skip operator
					offset ++;
					break;

				default:
					//one
					graph.addEdge(i, i + 1, regexpParts[operatorIndex - 1]);
			}
		}

		finalStateIndex = size - offset;
	}

	/**
	 * Generate a subList with a maximum size of <code>limit</code> of words that matches the given regex.
	 * <p>
	 * The Strings are ordered in lexicographical order.
	 *
	 * @param minimumSubwords	The minimum number of compounds that forms the generated word
	 * @param limit	The maximum size of the list
	 * @return	The list of words that matcher the given regex
	 */
	public List<List<String>> generateAll(final int minimumSubwords, final int limit){
		final List<List<String>> matchedWords = new ArrayList<>(limit);

		final Queue<GeneratedElement> queue = new LinkedList<>();
		queue.add(new GeneratedElement(new ArrayList<>(0), 0));
		while(!queue.isEmpty()){
			final GeneratedElement elem = queue.remove();
			final List<String> subword = elem.word;
			final int stateIndex = elem.stateIndex;

			//final state not reached, add transitions
			if(stateIndex < finalStateIndex){
				final Iterable<Pair<Integer, String>> transitions = graph.adjacentVertices(stateIndex);
				for(final Pair<Integer, String> transition : transitions){
					final int key = transition.getKey();
					final String value = transition.getValue();

					List<String> nextWord = subword;
					if(StringUtils.isNotBlank(value)){
						nextWord = new ArrayList<>(subword);
						nextWord.add(value);
					}

					queue.add(new GeneratedElement(nextWord, key));
				}
			}
			//if this is the accepting state add the generated word (skip empty generated word)
			else if(subword.size() >= minimumSubwords){
				matchedWords.add(subword);

				if(matchedWords.size() == limit)
					break;
			}
		}

		return matchedWords;
	}

	@Override
	public String toString(){
		return new ToStringBuilder(this, ShortPrefixNotNullToStringStyle.SHORT_PREFIX_NOT_NULL_STYLE)
			.append("graph", graph)
			.append("finalStateIndex", finalStateIndex)
			.toString();
	}

}
