/**
 * Copyright (c) 2019-2021 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.services.regexgenerator;

import io.github.mtrevisan.hunlinter.services.log.ShortPrefixNotNullToStringStyle;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


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

		@Override
		public boolean equals(final Object obj){
			if(obj == this)
				return true;
			if(obj == null || obj.getClass() != getClass())
				return false;

			final GeneratedElement rhs = (GeneratedElement)obj;
			return new EqualsBuilder()
				.append(word, rhs.word)
				.append(stateIndex, rhs.stateIndex)
				.isEquals();
		}

		@Override
		public int hashCode(){
			return new HashCodeBuilder()
				.append(word)
				.append(stateIndex)
				.toHashCode();
		}

	}


	private final Digraph<String> graph = new Digraph<>();
	private final int finalStateIndex;


	/**
	 * Initializes the NFA from the specified regular expression.
	 * <p>
	 * NOTE: each element should be enclosed in parentheses (e.g. {@code (as)(ert)?(b)*}), the managed operations are <code>*</code> and <code>?</code>
	 *
	 * @param regexpParts	The regular expression already subdivided into input and modifiers (e.g. ["ag", "ert", "?", "b", "*"])
	 */
	public HunSpellRegexWordGenerator(final String[] regexpParts){
		int offset = 0;
		final int size = regexpParts.length;
		for(int i = 0; i + offset < size; i ++){
			final int operatorIndex = i + offset + 1;
			final char next = (operatorIndex < size && regexpParts[operatorIndex].length() == 1
				? regexpParts[operatorIndex].charAt(0)
				: 0);
			//zero or more
			//skip operator
			//zero or one
			//skip operator
			//one
			switch(next){
				case '*' -> {
					graph.addEdge(i, i + 1);
					graph.addEdge(i, i, regexpParts[operatorIndex - 1]);
					offset ++;
				}
				case '?' -> {
					graph.addEdge(i, i + 1, regexpParts[operatorIndex - 1]);
					graph.addEdge(i, i + 1);
					offset ++;
				}
				default -> graph.addEdge(i, i + 1, regexpParts[operatorIndex - 1]);
			}
		}

		finalStateIndex = size - offset;
	}

	/**
	 * Generate a subList with a maximum size of {@code limit} of words that matches the given regex.
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
