package unit731.hunspeller.services.regexgenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import lombok.NoArgsConstructor;


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
 */
@NoArgsConstructor
public final class Digraph{

	private static final String NEWLINE = System.getProperty("line.separator");

	//adjacency list for given vertex
	private List<List<Integer>> adjacency = new ArrayList<>(0);


	/**
	 * Initializes a new digraph that is a deep copy of the specified digraph.
	 *
	 * @param graph	The digraph to copy
	 */
	public Digraph(Digraph graph){
		int vertices = graph.adjacency.size();
		adjacency = new ArrayList<>(vertices);
		for(int v = 0; v < vertices; v ++){
			//reverse so that adjacency list is in same order as original
			Stack<Integer> reverse = new Stack<>();
			for(int w : graph.adjacency.get(v))
				reverse.push(w);
			for(int w : reverse)
				addEdge(v, w);
		}
	}

	/**
	 * Adds the directed edge v→w to this digraph.
	 *
	 * @param v	The tail vertex
	 * @param w	The head vertex
	 */
	public void addEdge(int v, int w){
		while(v >= adjacency.size())
			adjacency.add(new ArrayList<>(0));
		adjacency.get(v).add(0, w);
	}

	/**
	 * Adds the directed edge v→w to this digraph.
	 *
	 * @param v	The tail vertex
	 * @param w	The head vertex
	 * @return <code>true</code> if this list contained the specified element
	 */
	public boolean removeEdge(int v, int w){
		return adjacency.get(v).remove(Integer.valueOf(w));
	}

	/**
	 * Returns the vertices adjacent from vertex {@code vertex} in this digraph.
	 *
	 * @param vertex the vertex
	 * @return the vertices adjacent from vertex {@code vertex}
	 * @throws IllegalArgumentException unless {@code 0 <= vertex < vertices}
	 */
	public Iterable<Integer> adjacentVertices(int vertex){
		return (vertex < adjacency.size()? adjacency.get(vertex): Collections.<Integer>emptyList());
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
			Iterable<Integer> transitions = adjacentVertices(v);
			for(int w : transitions)
				reverse.addEdge(w, v);
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
			s.append(String.format("%d: ", v));
			for(int w : adjacency.get(v))
				s.append(String.format("%d ", w));
			s.append(NEWLINE);
		}
		return s.toString();
	}

}
