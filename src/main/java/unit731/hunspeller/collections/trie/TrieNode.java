package unit731.hunspeller.collections.trie;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import unit731.hunspeller.collections.trie.sequencers.TrieSequencer;


//@NoArgsConstructor
public class TrieNode<T> implements Cloneable{

//	private String sequence;
//	private int startIndex;
//	@Getter private int endIndex;

	@Getter private T value;
	@Getter private boolean leaf;
	private Map<Integer, TrieNode<T>> children;


//	public TrieNode(String sequence, int startIndex, int endIndex, T value){
//		this.sequence = sequence;
//		this.startIndex = startIndex;
//		this.endIndex = endIndex;
//		this.value = value;
//	}

	@Override
	public TrieNode<T> clone(){
		TrieNode<T> clone = new TrieNode<>();
		clone.value = value;
		clone.leaf = leaf;
		if(children != null)
			clone.children = children.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()));
		return clone;
	}

	public void clear(){
		value = null;
		leaf = false;
		if(children != null)
			children.clear();
	}

	public void setValue(T value){
		this.value = value;
		setLeaf();
	}

	public void setLeaf(){
		leaf = true;
	}

	public TrieNode<T> getChild(int stem){
		return (children != null? children.get(stem): null);
	}

	public void addChild(int stem, TrieNode<T> node){
		if(children == null)
			children = new HashMap<>();

		children.put(stem, node);
	}

	public TrieNode<T> removeChild(int stem){
		TrieNode<T> removedNode = null;
		if(children != null){
			TrieNode<T> node = children.get(stem);
			//when there are no children, remove this node from it's parent
			if(node.children == null || node.children.isEmpty())
				removedNode = children.remove(stem);
		}
		return removedNode;
	}

	public void forEachChild(Consumer<TrieNode<T>> callback){
		if(children != null)
			children.values()
				.forEach(callback::accept);
	}

	public boolean isEmpty(){
		return (value == null && !leaf && (children == null || children.isEmpty()));
	}

	public boolean hasChildren(){
		return (children != null && !children.isEmpty());
	}

	/**
	 * Splits this node at the given relative index and returns the TrieNode with
	 * the sequence starting at index. The returned TrieNode has this node's
	 * sequence, value, and children. The returned TrieNode is also the only
	 * child of this node when this method returns.
	 *
	 * @param index	The relative index (starting at 0 and going to end - start - 1) in the sequence.
	 * @param value	The new value of this node.
	 * @param sequencer	The sequencer to use to determine the place of the node in the children's list
	 * @return	The reference to the child node created that's sequence starts at index.
	 */
//	public TrieNode<T> split(int index, T value, TrieSequencer<String> sequencer){
//		TrieNode<T> lowerNode = new TrieNode<>(sequence, startIndex + index, endIndex, value);
//		if(children != null){
//			children.forEach((stem, node) -> lowerNode.addChild(stem, node));
//			children.clear();
//		}
//
//		endIndex = startIndex + index;
//
//		int stem = sequencer.hashOf(sequence, endIndex);
//		addChild(stem, lowerNode);
//
//		return lowerNode;
//	}

}
