package unit731.hunspeller.services.regexgenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;


/**
 * The {@code Digraph} class represents a directed graph of vertices.
 * <p>
 * It supports the following two primary operations: add an edge to the digraph, iterate over all of the vertices adjacent from a given vertex.
 * Parallel edges and self-loops are permitted.
 * <p>
 * This implementation uses an adjacency-lists representation, which is a vertex-indexed array of {@link Bag} objects.
 * All operations take constant time (in the worst case) except iterating over the vertices adjacent from a given vertex, which takes
 * time proportional to the number of such vertices.
 * <p>
 * For additional documentation, see <a href="https://algs4.cs.princeton.edu/42digraph">Section 4.2</a> of
 * <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 * 
 * @param <T>	Type of values stored in transitions
 */
@NoArgsConstructor
public final class Digraph<T>{

	private static final String NEWLINE = System.getProperty("line.separator");

	//adjacency list for given vertex
	private final List<List<Pair<Integer, T>>> adjacency = new ArrayList<>(0);


	/**
	 * Initializes a new digraph that is a deep copy of the specified digraph.
	 *
	 * @param graph	The digraph to copy
	 */
	public Digraph(Digraph<T> graph){
		int vertices = graph.adjacency.size();
		for(int v = 0; v < vertices; v ++){
			//reverse so that adjacency list is in same order as original
			Stack<Pair<Integer, T>> reverse = new Stack<>();
			for(Pair<Integer, T> w : graph.adjacency.get(v))
				reverse.push(w);
			for(Pair<Integer, T> w : reverse)
				addEdge(v, w.getKey(), w.getValue());
		}
	}

	/**
	 * Adds the directed edge v→w to this digraph.
	 *
	 * @param v	The tail vertex
	 * @param w	The head vertex
	 */
	public void addEdge(int v, int w){
		addEdge(v, w, null);
	}

	/**
	 * Adds the directed edge v→w to this digraph.
	 *
	 * @param v	The tail vertex
	 * @param w	The head vertex
	 * @param value	The value associated with this transition
	 */
	public void addEdge(int v, int w, T value){
		while(v >= adjacency.size())
			adjacency.add(new ArrayList<>(0));
		adjacency.get(v).add(0, Pair.of(w, value));
	}

	/**
	 * Returns the vertices adjacent from vertex {@code vertex} in this digraph.
	 *
	 * @param vertex the vertex
	 * @return the vertices adjacent from vertex {@code vertex}
	 * @throws IllegalArgumentException unless {@code 0 <= vertex < vertices}
	 */
	public Iterable<Pair<Integer, T>> adjacentVertices(int vertex){
		return (vertex < adjacency.size()? adjacency.get(vertex): Collections.<Pair<Integer, T>>emptyList());
	}

	/**
	 * Returns the reverse of the digraph.
	 *
	 * @return	the reverse of the digraph
	 */
	public Digraph reverse(){
		Digraph reverse = new Digraph();
		int vertices = adjacency.size();
		for(int v = 0; v < vertices; v ++){
			Iterable<Pair<Integer, T>> transitions = adjacentVertices(v);
			for(Pair<Integer, T> w : transitions)
				reverse.addEdge(w.getKey(), v, w.getValue());
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
		StringBuilder s = new StringBuilder();
		s.append(NEWLINE);
		int vertices = adjacency.size();
		for(int v = 0; v < vertices; v ++){
			String transitions = adjacency.get(v).stream()
				.map(w -> String.format("%d (%s)", w.getKey(), (w.getValue() != null? w.getValue(): "ε")))
				.collect(Collectors.joining(", "));
			s.append(String.format("%d: ", v))
				.append(transitions)
				.append(NEWLINE);
		}
		return s.toString();
	}

}
