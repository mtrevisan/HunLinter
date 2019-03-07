package unit731.hunspeller.collections.radixtree.tree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import unit731.hunspeller.collections.radixtree.sequencers.SequencerInterface;


/**
 * A node in a radix tree.
 *
 * @param <S>	The sequence/key type
 * @param <V>	The type of values stored in the tree
 */
public class RadixTreeNode<S, V extends Serializable> implements Serializable{

	private static final long serialVersionUID = -627223674493970063L;


	/** The key at this node */
	private S key;

	/** The value stored at this node, <code>null</code> if an internal node */
	private V value;
	private List<V> additionalValues;

	/** The children for this node. */
	private Collection<RadixTreeNode<S, V>> children;

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

	public S getKey(){
		return key;
	}

	public V getValue(){
		return value;
	}

	public void setValue(V value){
		this.value = value;
	}

	public List<V> getAdditionalValues(){
		return additionalValues;
	}

	public Collection<RadixTreeNode<S, V>> getChildren(){
		return children;
	}

	public RadixTreeNode<S, V> getFailNode(){
		return failNode;
	}

	public void setFailNode(RadixTreeNode<S, V> failNode){
		this.failNode = failNode;
	}

	public boolean isEmpty(){
		return (children == null || children.isEmpty());
	}

	public RadixTreeNode<S, V> getNextNode(S sequence, int index, SequencerInterface<S> sequencer){
		RadixTreeNode<S, V> newNode = null;
		RadixTreeNode<S, V> currentNode = this;
		while(currentNode != null){
			newNode = currentNode.getChild(sequence, index, sequencer);
			if(newNode != null)
				break;

			currentNode = currentNode.getFailNode();
		}
		return newNode;
	}

	public RadixTreeNode<S, V> getChild(S key, int index, SequencerInterface<S> sequencer){
		RadixTreeNode<S, V> response = null;
		if(children != null)
			for(RadixTreeNode<S, V> child : children){
				boolean found = true;
				int size = sequencer.length(child.key);
				if(sequencer.length(key) - index >= size){
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
			for(RadixTreeNode<S, V> child : children)
				action.accept(child);
	}

	/**
	 * Whether or not this node has a value attached to it.
	 *
	 * @return	Whether or not this node has a value
	 */
	public boolean hasValue(){
		return (value != null);
	}

	public void addAdditionalValues(RadixTreeNode<S, V> node){
		V nodeValue = node.getValue();
		List<V> nodeAdditionalValues = node.getAdditionalValues();
		if(nodeValue != null || nodeAdditionalValues != null){
			additionalValues = ObjectUtils.defaultIfNull(additionalValues, new ArrayList<>());
			if(nodeValue != null)
				additionalValues.add(node.getValue());
			if(nodeAdditionalValues != null)
				additionalValues.addAll(nodeAdditionalValues);
		}
	}

	public void clearAdditionalValues(){
		additionalValues = null;
	}

	/**
	 * Merge a child into its parent node.
	 * The operation is valid only if it is a child of the parent node and the parent node is not a real node.
	 *
	 * @param parent	The parent Node
	 * @param sequencer	The sequencer
	 */
	public void mergeWithAncestor(RadixTreeNode<S, V> parent, SequencerInterface<S> sequencer){
		parent.key = sequencer.concat(parent.getKey(), key);
		parent.value = value;
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
	public boolean equals(Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		RadixTreeNode<?, ?> rhs = (RadixTreeNode<?, ?>)obj;
		return new EqualsBuilder()
			.append(key, rhs.key)
			.append(value, rhs.value)
			.append(additionalValues, rhs.additionalValues)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(key)
			.append(value)
			.append(additionalValues)
			.toHashCode();
	}

}
