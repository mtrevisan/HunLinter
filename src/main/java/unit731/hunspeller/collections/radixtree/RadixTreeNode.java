package unit731.hunspeller.collections.radixtree;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


/**
 * A node in a radix tree.
 *
 * @param <V>	The type of values stored in the tree
 */
@EqualsAndHashCode(of = {"key", "value"})
public class RadixTreeNode<V extends Serializable> implements Iterable<RadixTreeNode<V>>, Comparable<RadixTreeNode<V>>, Serializable{

	/** The key at this node */
	@Getter
	@Setter
	private String key;

	/** The value stored at this node, <code>null</code> if an internal node */
	@Getter
	@Setter
	private V value;

	/**
	 * The children for this node.
	 * Note, because we use {@link TreeSet} here, traversal of {@link RadixTree} will be in lexicographical order.
	 */
	private Collection<RadixTreeNode<V>> children;


	/**
	 * Constructs a node from the given prefix.
	 *
	 * @param key	The prefix
	 */
	public RadixTreeNode(String key){
		this(key, null);
	}

	/**
	 * Constructs a node from the given prefix and value.
	 *
	 * @param key	The prefix
	 * @param value	The value
	 */
	public RadixTreeNode(String key, V value){
		this.key = key;
		this.value = value;
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
		return (value != null);
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
		return key.compareTo(node.getKey());
	}

	/**
	 * Finds the length of the largest prefix
	 *
	 * @param key	Character sequence
	 * @return	The length of largest prefix of <code>a</code> and <code>b</code>
	 * @throws IllegalArgumentException	If either <code>a</code> or <code>b</code> is <code>null</code>
	 */
	public int largestPrefixLength(CharSequence key){
		int len;
		int size = Math.min(this.key.length(), key.length());
		for(len = 0; len < size; len ++)
			if(this.key.charAt(len) != key.charAt(len))
				break;
		return len;
	}

}
