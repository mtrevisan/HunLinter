package unit731.hunspeller.services.regexgenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * The {@code Digraph} class represents a directed graph of vertices named 0 through <em>V</em> - 1.
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
public class Digraph{

	private static final String NEWLINE = System.getProperty("line.separator");

	//number of vertices in this digraph
	@Getter
	private int vertices;
	//adjacency list for given vertex
	private List<List<Integer>> adjacency = new ArrayList<>(0);


	/**
	 * Initializes a new digraph that is a deep copy of the specified digraph.
	 *
	 * @param graph	The digraph to copy
	 */
	public Digraph(Digraph graph){
		vertices = graph.vertices;
		adjacency = new ArrayList<>(vertices);
		for(int v = 0; v < graph.vertices; v ++){
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
	 * @throws IllegalArgumentException	unless both {@code 0 <= v < vertices} and {@code 0 <= w < vertices}
	 */
	public void addEdge(int v, int w){
		while(v >= adjacency.size()){
			adjacency.add(new ArrayList<>(0));
			vertices ++;
		}
		adjacency.get(v).add(0, w);
	}

	/**
	 * Returns the vertices adjacent from vertex {@code vertex} in this digraph.
	 *
	 * @param vertex the vertex
	 * @return the vertices adjacent from vertex {@code vertex} in this digraph, as an iterable
	 * @throws IllegalArgumentException unless {@code 0 <= vertex < vertices}
	 */
	public Iterable<Integer> adjacentVertices(int vertex){
		return adjacency.get(vertex);
	}

	/**
	 * Returns the reverse of the digraph.
	 *
	 * @return	the reverse of the digraph
	 */
	public Digraph reverse(){
		Digraph reverse = new Digraph();
		for(int v = 0; v < vertices; v ++)
			for(int w : adjacentVertices(v))
				reverse.addEdge(w, v);
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
		s.append(vertices).append(" vertices, ").append(NEWLINE);
		for(int v = 0; v < vertices; v ++){
			s.append(String.format("%d: ", v));
			for(int w : adjacency.get(v))
				s.append(String.format("%d ", w));
			s.append(NEWLINE);
		}
		return s.toString();
	}

}
