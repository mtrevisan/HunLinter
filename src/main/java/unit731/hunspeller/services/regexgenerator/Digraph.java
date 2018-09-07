package unit731.hunspeller.services.regexgenerator;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import lombok.Getter;


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
public class Digraph{

	private static final String NEWLINE = System.getProperty("line.separator");

	//number of vertices in this digraph
	@Getter
	private final int vertices;
	//number of edges in this digraph
	@Getter
	private int edges;
	//adjacency list for given vertex
	private Set<Integer>[] adjacency;
	//indegree of given vertex
	private int[] inDegree;


	/**
	 * Initializes an empty digraph with <em>V</em> vertices.
	 *
	 * @param vertices	The number of vertices
	 * @throws IllegalArgumentException if {@code vertices < 0}
	 */
	public Digraph(int vertices){
		if(vertices < 0)
			throw new IllegalArgumentException("Number of vertices in a Digraph must be nonnegative");

		this.vertices = vertices;
		edges = 0;
		adjacency = new HashSet[vertices];
		for(int v = 0; v < vertices; v ++)
			adjacency[v] = new HashSet<>();
		inDegree = new int[vertices];
	}

	/**
	 * Initializes a new digraph that is a deep copy of the specified digraph.
	 *
	 * @param graph	The digraph to copy
	 */
	public Digraph(Digraph graph){
		this(graph.vertices);

		edges = graph.edges;
		for(int v = 0; v < vertices; v ++)
			inDegree[v] = graph.inDegree(v);
		for(int v = 0; v < graph.vertices; v ++){
			//reverse so that adjacency list is in same order as original
			Stack<Integer> reverse = new Stack<>();
			for(int w : graph.adjacency[v])
				reverse.push(w);
			for(int w : reverse)
				adjacency[v].add(w);
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
		validateVertex(v);
		validateVertex(w);

		adjacency[v].add(w);
		inDegree[w] ++;
		edges ++;
	}

	/**
	 * Returns the vertices adjacent from vertex {@code vertex} in this digraph.
	 *
	 * @param vertex the vertex
	 * @return the vertices adjacent from vertex {@code vertex} in this digraph, as an iterable
	 * @throws IllegalArgumentException unless {@code 0 <= vertex < vertices}
	 */
	public Iterable<Integer> adjacentVertices(int vertex){
		validateVertex(vertex);

		return adjacency[vertex];
	}

	/**
	 * Returns the number of directed edges incident from vertex {@code vertex}.
	 * <p>
	 * This is known as the <em>out degree</em> of vertex {@code vertex}.
	 *
	 * @param vertex	The vertex
	 * @return	the outdegree of vertex {@code v}
	 * @throws IllegalArgumentException	unless {@code 0 <= vertex < vertics}
	 */
	public int outDegree(int vertex){
		validateVertex(vertex);

		return adjacency[vertex].size();
	}

	/**
	 * Returns the number of directed edges incident to vertex {@code vertex}.
	 * <p>
	 * This is known as the <em>in degree</em> of vertex {@code vertex}.
	 *
	 * @param vertex	The vertex
	 * @return	the indegree of vertex {@code vertex}
	 * @throws IllegalArgumentException	unless {@code 0 <= vertex < vertices}
	 */
	public int inDegree(int vertex){
		validateVertex(vertex);

		return inDegree[vertex];
	}

	//throw an IllegalArgumentException unless {@code 0 <= vertex < vertices}
	private void validateVertex(int vertex){
		if(vertex < 0 || vertex >= vertices)
			throw new IllegalArgumentException("vertex " + vertex + " is not between 0 and " + (vertices - 1));
	}

	/**
	 * Returns the reverse of the digraph.
	 *
	 * @return	the reverse of the digraph
	 */
	public Digraph reverse(){
		Digraph reverse = new Digraph(vertices);
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
		s.append(vertices).append(" vertices, ").append(edges).append(" edges ").append(NEWLINE);
		for(int v = 0; v < vertices; v ++){
			s.append(String.format("%d: ", v));
			for(int w : adjacency[v])
				s.append(String.format("%d ", w));
			s.append(NEWLINE);
		}
		return s.toString();
	}

}
