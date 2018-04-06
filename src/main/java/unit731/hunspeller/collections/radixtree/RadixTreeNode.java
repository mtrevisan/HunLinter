package unit731.hunspeller.collections.radixtree;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;
import lombok.Getter;
import lombok.Setter;


/**
 * A node in a radix tree.
 *
 * @param <V>	The type of values stored in the tree
 */
public class RadixTreeNode<V extends Serializable> implements Iterable<RadixTreeNode<V>>, Comparable<RadixTreeNode<V>>, Serializable{

	/** The prefix at this node */
	@Getter
	@Setter
	private String prefix;

	/** The value stored at this node, <code>null</code> if an internal node */
	@Getter
	@Setter
	private V value;

	/**
	 * Whether or not this node stores a value.
	 * This value is mainly used by {@link RadixTreeVisitor} to figure out whether or not this node should be visited.
	 */
	private boolean hasValue;

	/**
	 * The children for this node.
	 * Note, because we use {@link TreeSet} here, traversal of {@link RadixTree} will be in lexicographical order.
	 */
	private Collection<RadixTreeNode<V>> children;


	/**
	 * Constructs a node from the given prefix.
	 *
	 * @param prefix	The prefix
	 * @param value	The value
	 */
	public RadixTreeNode(String prefix){
		this(prefix, null);

		this.hasValue = false;
	}

	/**
	 * Constructs a node from the given prefix and value.
	 *
	 * @param prefix	The prefix
	 * @param value	The value
	 */
	public RadixTreeNode(String prefix, V value){
		this.prefix = prefix;
		this.value = value;
		this.hasValue = true;
	}

	/**
	 * Gets the children of this node.
	 *
	 * @return	The list of children
	 */
	public Collection<RadixTreeNode<V>> getChildren(){
		//delayed creation of children to reduce memory cost
		if(children == null)
			children = new TreeSet<>();

		return children;
	}

	/**
	 * Whether or not this node has a value attached to it.
	 *
	 * @return	Whether or not this node has a value
	 */
	public boolean hasValue(){
		return hasValue;
	}

	/**
	 * Sets whether or not this node has a value attached to it.
	 *
	 * @param hasValue	<code>true</code> if this node will have a value, <code>false</code> otherwise. If <code>false</code>,
	 *							{@link #getValue()} will return <code>null</code> after this call.
	 */
	public void setHasValue(boolean hasValue){
		this.hasValue = hasValue;

		if(!hasValue)
			this.value = null;
	}

	@Override
	public Iterator<RadixTreeNode<V>> iterator(){
		if(children == null){
			return new Iterator<RadixTreeNode<V>>(){
				@Override
				public boolean hasNext(){
					return false;
				}

				@Override
				public RadixTreeNode<V> next(){
					return null;
				}

				@Override
				public void remove(){
					//unimplemented
				}
			};
		}

		return children.iterator();
	}

	@Override
	public int compareTo(RadixTreeNode<V> node){
		return prefix.compareTo(node.getPrefix());
	}

}
