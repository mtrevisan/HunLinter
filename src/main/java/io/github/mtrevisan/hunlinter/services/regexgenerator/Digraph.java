/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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

import io.github.mtrevisan.hunlinter.workers.core.IndexDataPair;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.StringJoiner;


/**
 * The {@code Digraph} class represents a directed graph of vertices.
 * <p>
 * It supports the following two primary operations: add an edge to the digraph, iterate over all the vertices adjacent from a given vertex.
 * Parallel edges and self-loops are permitted.
 * <p>
 * This implementation uses an adjacency-lists representation, which is a vertex-indexed array of {@link Pair} objects.
 * All operations take constant time (in the worst case) except iterating over the vertices adjacent from a given vertex, which takes
 * time proportional to the number of such vertices.
 * <p>
 * For additional documentation, see <a href="https://algs4.cs.princeton.edu/42digraph">Section 4.2</a> of
 * <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 *
 * @param <T>	Type of values stored in transitions
 */
public final class Digraph<T>{

	private static final String NEWLINE = "\r\n";

	//adjacency list for given vertex
	private final List<List<IndexDataPair<T>>> adjacency = new ArrayList<>(0);


	public Digraph(){}

	/**
	 * Initializes a new digraph that is a deep copy of the specified digraph.
	 *
	 * @param graph	The digraph to copy
	 */
	public Digraph(final Digraph<T> graph){
		final int vertices = graph.adjacency.size();
		final Deque<IndexDataPair<T>> reverse = new ArrayDeque<>();
		for(int v = 0; v < vertices; v ++){
			//reverse so that adjacency list is in same order as original
			reverse.clear();
			for(final IndexDataPair<T> w : graph.adjacency.get(v))
				reverse.push(w);
			for(final IndexDataPair<T> w : reverse)
				addEdge(v, w.getIndex(), w.getData());
		}
	}

	/**
	 * Adds the directed edge {@code v→w} upon ε-transition to this digraph.
	 *
	 * @param v	The tail vertex
	 * @param w	The head vertex
	 */
	public void addEdge(final int v, final int w){
		addEdge(v, w, null);
	}

	/**
	 * Adds the directed edge {@code v→w} upon input {@code value} to this digraph.
	 *
	 * @param v	The tail vertex
	 * @param w	The head vertex
	 * @param value	The value associated with this transition
	 */
	public void addEdge(final int v, final int w, final T value){
		while(v >= adjacency.size())
			adjacency.add(new ArrayList<>(0));
		adjacency.get(v)
			.add(0, IndexDataPair.of(w, value));
	}

	/**
	 * Returns the vertices adjacent from vertex {@code vertex} in this digraph.
	 *
	 * @param vertex the vertex
	 * @return the vertices adjacent from vertex {@code vertex}
	 * @throws IndexOutOfBoundsException unless {@code 0 <= vertex < vertices}
	 */
	public Iterable<IndexDataPair<T>> adjacentVertices(final int vertex){
		return (vertex < adjacency.size()? adjacency.get(vertex): Collections.emptyList());
	}

	/**
	 * Returns the reverse of the digraph.
	 *
	 * @return	the reverse of the digraph
	 */
	public Digraph<T> reverse(){
		final Digraph<T> reverse = new Digraph<>();
		final int vertices = adjacency.size();
		for(int v = 0; v < vertices; v ++){
			final Iterable<IndexDataPair<T>> transitions = adjacentVertices(v);
			for(final IndexDataPair<T> w : transitions)
				reverse.addEdge(w.getIndex(), v, w.getData());
		}
		return reverse;
	}

	/**
	 * Returns a string representation of the graph.
	 *
	 * @return	the number of vertices <em>V</em>, followed by the number of edges <em>E</em>, followed by the <em>V</em> adjacency lists
	 */
	@Override
	public String toString(){
		final StringBuilder sb = new StringBuilder();
		sb.append(NEWLINE);
		final int vertices = adjacency.size();
		for(int v = 0; v < vertices; v ++){
			final StringJoiner transitions = new StringJoiner(", ");
			forEach(adjacency.get(v), transitions);
			sb.append(v)
				.append(':')
				.append(StringUtils.SPACE)
				.append(transitions)
				.append(NEWLINE);
		}
		return sb.toString();
	}

	public static <T> void forEach(final Iterable<IndexDataPair<T>> adjacency, final StringJoiner transitions){
		for(final IndexDataPair<T> w : adjacency)
			transitions.add(w.getIndex() + StringUtils.SPACE + "(" + (w.getData() != null? w.getData(): "ε") + ")");
	}

}
