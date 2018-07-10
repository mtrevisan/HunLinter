package unit731.hunspeller.collections.radixtree.tree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
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
@EqualsAndHashCode(of = {"key", "value", "additionalValues"})
public class RadixTreeNode<S, V extends Serializable> implements Iterable<RadixTreeNode<S, V>>, Serializable{

	private static final long serialVersionUID = -627223674493970063L;


	/** The key at this node */
	@Setter
	private S key;

	/** The value stored at this node, <code>null</code> if an internal node */
	@Setter
	private V value;
	private List<V> additionalValues;

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
		return (Objects.isNull(children) || children.isEmpty());
	}

	public RadixTreeNode<S, V> getNextNode(S sequence, int index, SequencerInterface<S> sequencer){
		RadixTreeNode<S, V> newNode = null;
		RadixTreeNode<S, V> currentNode = this;
		while(Objects.nonNull(currentNode)){
			newNode = currentNode.getChild(sequence, index, sequencer);
			if(Objects.nonNull(newNode))
				break;

			currentNode = currentNode.getFailNode();
		}
		return newNode;
	}

	public RadixTreeNode<S, V> getChild(S key, int index, SequencerInterface<S> sequencer){
		RadixTreeNode<S, V> response = null;
		if(Objects.nonNull(children))
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
		if(Objects.nonNull(child))
			addChildren(Collections.singleton(child));
	}

	public void addChildren(Collection<RadixTreeNode<S, V>> children){
		if(Objects.nonNull(children)){
			//delayed creation of children to reduce memory cost
			this.children = ObjectUtils.defaultIfNull(this.children, new HashSet<>());

			this.children.addAll(children);
		}
	}

	public void clearChildren(){
		children = null;
	}

	public void forEachChildren(Consumer<? super RadixTreeNode<S, V>> action){
		if(Objects.nonNull(children))
			children.forEach(action);
	}

	/**
	 * Whether or not this node has a value attached to it.
	 *
	 * @return	Whether or not this node has a value
	 */
	public boolean hasValue(){
		return Objects.nonNull(value);
	}

	public void addAdditionalValues(RadixTreeNode<S, V> node){
		V nodeValue = node.getValue();
		List<V> nodeAdditionalValues = node.getAdditionalValues();
		if(Objects.nonNull(nodeValue) || Objects.nonNull(nodeAdditionalValues)){
			additionalValues = ObjectUtils.defaultIfNull(additionalValues, new ArrayList<>());
			if(Objects.nonNull(nodeValue))
				additionalValues.add(node.getValue());
			if(Objects.nonNull(nodeAdditionalValues))
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
		if(Objects.nonNull(children))
			children.clear();
		addChild(leafNode);

		return leafNode;
	}

	@Override
	public Iterator<RadixTreeNode<S, V>> iterator(){
		return (Objects.nonNull(children)? children.iterator(): Collections.<RadixTreeNode<S, V>>emptyIterator());
	}

}
