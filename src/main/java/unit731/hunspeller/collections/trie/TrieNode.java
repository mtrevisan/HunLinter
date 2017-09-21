package unit731.hunspeller.collections.trie;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import unit731.hunspeller.collections.trie.sequencers.TrieSequencer;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TrieNode<T> implements Cloneable{

	@Getter private String sequence;
	@Getter private int startIndex;
	@Getter private int endIndex;

	@Getter @Setter private T value;
	private Map<Integer, TrieNode<T>> children;


	public TrieNode(String sequence, int startIndex, int endIndex, T value){
		this.sequence = sequence;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.value = value;
	}

	public static <T> TrieNode<T> makeRoot(){
		return new TrieNode<>();
	}

	@Override
	public TrieNode<T> clone(){
		TrieNode<T> clone = new TrieNode<>(sequence, startIndex, endIndex, value);
		if(children != null)
			clone.children = children.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()));
		return clone;
	}

	public void clear(){
		sequence = null;
		startIndex = 0;
		endIndex = 0;

		value = null;
		if(children != null)
			children.clear();
	}

	public boolean isLeaf(){
		return (value != null);
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
			if(node != null && node.children == null){
				removedNode = children.remove(stem);

				if(children.isEmpty())
					//claim memory
					children = null;
			}
		}
		return removedNode;
	}

	public void forEachChild(Consumer<TrieNode<T>> callback){
		if(children != null)
			children.values()
				.forEach(callback::accept);
	}

	public boolean isEmpty(){
		return (value == null && (children == null || children.isEmpty()));
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
	public TrieNode<T> split(int index, T value, TrieSequencer<String> sequencer){
		TrieNode<T> lowerNode = new TrieNode<>(sequence, startIndex + index, endIndex, value);
		if(children != null){
			children.forEach((stem, node) -> lowerNode.addChild(stem, node));
			children.clear();
		}

		endIndex = startIndex + index;

		int stem = sequencer.hashOf(sequence, endIndex);
		addChild(stem, lowerNode);

		return lowerNode;
	}

}
