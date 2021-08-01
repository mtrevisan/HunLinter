/**
 * Copyright (c) 2019-2020 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.datastructures.fsa.lookup;


/**
 * A matching result returned from {@link FSATraversal}.
 *
 * @see FSATraversal
 */
public class FSAMatchResult{

	/** The automaton has exactly one match for the input sequence */
	public static final int EXACT_MATCH = 0;

	/**
	 * The automaton has no match for the input sequence and no sequence in the automaton is a prefix of the input.
	 * <p>
	 * Note that to check for a general "input does not exist in the automaton" you have to check for
	 * both NO_MATCH and {@link #AUTOMATON_HAS_PREFIX}.
	 */
	public static final int NO_MATCH = -1;

	/**
	 * The automaton contains a prefix of the input sequence (but the full sequence does not exist).
	 * This translates to: one of the input sequences used to build the automaton is a prefix of the input sequence,
	 * but the input sequence contains a non-existent suffix.
	 *
	 * <p>{@link FSAMatchResult#index} will contain an index of the first character of the input sequence not present
	 * in the dictionary.</p>
	 */
	public static final int AUTOMATON_HAS_PREFIX = -3;

	/**
	 * The sequence is a prefix of at least one sequence in the automaton.
	 * {@link FSAMatchResult#node} returns the node from which all sequences with the given prefix start in the
	 * automaton.
	 */
	public static final int PREFIX_MATCH = -4;

	/**
	 * One of the match types defined in this class.
	 *
	 * @see #NO_MATCH
	 * @see #EXACT_MATCH
	 * @see #AUTOMATON_HAS_PREFIX
	 * @see #PREFIX_MATCH
	 */
	public int kind;
	/** Input sequence's index, interpretation depends on {@link #kind} */
	public int index;
	/** Automaton node, interpretation depends on the {@link #kind} */
	public int node;


	FSAMatchResult(final int kind, final int index, final int node){
		reset(kind, index, node);
	}

	FSAMatchResult(final int kind){
		reset(kind, 0, 0);
	}

	public FSAMatchResult(){
		reset(NO_MATCH, 0, 0);
	}

	final void reset(final int kind, final int index, final int node){
		this.kind = kind;
		this.index = index;
		this.node = node;
	}

}
