package unit731.hunspeller.collections.intervalmap;

public abstract class Traversal<K extends Comparable<K>, V, R>{

	/**
	 * @param node	The node to be visited
	 * @return	Return <code>null</code> to continue traversal
	 */
	public abstract R visit(IntervalNode<K, V> node);

}
