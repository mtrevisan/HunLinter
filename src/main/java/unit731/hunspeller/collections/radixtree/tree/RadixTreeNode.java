package unit731.hunspeller.collections.radixtree.tree;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;


/**
 * A node in a radix tree.
 *
 * @param <S>	The sequence/key type
 * @param <V>	The type of values stored in the tree
 */
@EqualsAndHashCode(of = {"key", "value"})
public class RadixTreeNode<S, V extends Serializable> implements Iterable<RadixTreeNode<S, V>>, Serializable{

	/** The key at this node */
	@Getter
	@Setter
	private S key;

	/** The value stored at this node, <code>null</code> if an internal node */
	@Getter
	@Setter
	private V value;

	/**
	 * The children for this node.
	 */
	private Collection<RadixTreeNode<S, V>> children;

	@Getter
	@Setter
	private RadixTreeNode<S, V> failNode;


	public static <K, T extends Serializable> RadixTreeNode<K, T> createEmptyNode(K emptySequence){
		return new RadixTreeNode<>(emptySequence, null);
	}

	/**
	 * Constructs a node from the given prefix and value.
	 *
	 * @param key	The prefix
	 * @param value	The value
	 */
	public RadixTreeNode(S key, V value){
		this.key = key;
		this.value = value;
	}

	/**
	 * Gets the children of this node.
	 *
	 * @return	The list of children
	 */
	public Collection<RadixTreeNode<S, V>> getChildren(){
		//delayed creation of children to reduce memory cost
		children = ObjectUtils.defaultIfNull(children, new HashSet<>());

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
	public Iterator<RadixTreeNode<S, V>> iterator(){
		return (children != null? children.iterator(): Collections.<RadixTreeNode<S, V>>emptyIterator());
	}

}
