package unit731.hunspeller.collections.radixtree;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeSet;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;


/**
 * A node in a radix tree.
 *
 * @param <V>	The type of values stored in the tree
 */
@EqualsAndHashCode(of = {"key"})
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


	public static <T extends Serializable> RadixTreeNode<T> createEmptyNode(){
		return new RadixTreeNode<>(StringUtils.EMPTY, null);
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
		children = ObjectUtils.defaultIfNull(children, new TreeSet<>());

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
		return (children != null? children.iterator(): Collections.<RadixTreeNode<V>>emptyIterator());
	}

	@Override
	public int compareTo(RadixTreeNode<V> node){
		return key.compareTo(node.key);
	}

}
