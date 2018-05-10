package unit731.hunspeller.collections.radixtree.tree;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.function.Consumer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import unit731.hunspeller.collections.radixtree.sequencers.SequencerInterface;


/**
 * A node in a radix tree.
 *
 * @param <S>	The sequence/key type
 * @param <V>	The type of values stored in the tree
 */
@Getter
@EqualsAndHashCode(of = {"key", "value"})
public class RadixTreeNode<S, V extends Serializable> implements Iterable<RadixTreeNode<S, V>>, Serializable{

	private static final long serialVersionUID = -627223674493970063L;


	/** The key at this node */
	@Setter
	private S key;

	/** The value stored at this node, <code>null</code> if an internal node */
	@Setter
	private V value;

	/** The children for this node. */
	private Collection<RadixTreeNode<S, V>> children;

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

	public boolean isEmpty(){
		return (children == null || children.isEmpty());
	}

	public RadixTreeNode<S, V> getNextNode(int index, S sequence, SequencerInterface<S> sequencer){
		RadixTreeNode<S, V> currentNode = this;
		RadixTreeNode<S, V> newCurrentState = null;
		while(currentNode != null && (newCurrentState == null || !newCurrentState.hasValue())){
			newCurrentState = currentNode.getChild(index, sequence, sequencer);
			if(newCurrentState.hasValue())
				currentNode = currentNode.getFailNode();
		}
		return newCurrentState;
	}

	public RadixTreeNode<S, V> getChild(int index, S key, SequencerInterface<S> sequencer){
		RadixTreeNode<S, V> response = failNode;
		if(children != null)
			for(RadixTreeNode<S, V> child : children){
				boolean found = true;
				int size = sequencer.length(child.key);
				for(int i = 0; i < size; i ++)
					if(!sequencer.equalsAtIndex(child.key, key, i, index + i)){
						found = false;
						break;
					}
				if(found){
					response = child;
					break;
				}
			}
		return response;
	}

	public void addChild(RadixTreeNode<S, V> child){
		if(child != null)
			addChildren(Collections.singleton(child));
	}

	public void addChildren(Collection<RadixTreeNode<S, V>> children){
		if(children != null){
			//delayed creation of children to reduce memory cost
			this.children = ObjectUtils.defaultIfNull(this.children, new HashSet<>());

			this.children.addAll(children);
		}
	}

	public void clearChildren(){
		children = null;
	}

	public void forEachChildren(Consumer<? super RadixTreeNode<S, V>> action){
		if(children != null)
			children.forEach(action);
	}

	/**
	 * Whether or not this node has a value attached to it.
	 *
	 * @return	Whether or not this node has a value
	 */
	public boolean hasValue(){
		return (value != null);
	}

	/**
	 * Merge a child into its parent node.
	 * The operation is valid only if it is a child of the parent node and the parent node is not a real node.
	 *
	 * @param parent	The parent Node
	 * @param sequencer	The sequencer
	 */
	public void mergeWithAncestor(RadixTreeNode<S, V> parent, SequencerInterface<S> sequencer){
		parent.setKey(sequencer.concat(parent.getKey(), key));
		parent.setValue(value);
		parent.clearChildren();
	}

	/**
	 * Split a node at a given point.
	 * 
	 * @param splitIndex	The index used to split the key in two
	 * @param sequencer	The sequencer
	 * @return	The leaf node just created
	 */
	public RadixTreeNode<S, V> split(int splitIndex, SequencerInterface<S> sequencer){
		S leafPrefix = sequencer.subSequence(key, splitIndex);
		RadixTreeNode<S, V> leafNode = new RadixTreeNode<>(leafPrefix, value);
		leafNode.addChildren(children);

		key = sequencer.subSequence(key, 0, splitIndex);
		value = null;
		if(children != null)
			children.clear();
		addChild(leafNode);

		return leafNode;
	}

	@Override
	public Iterator<RadixTreeNode<S, V>> iterator(){
		return (children != null? children.iterator(): Collections.<RadixTreeNode<S, V>>emptyIterator());
	}

}
