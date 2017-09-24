package unit731.hunspeller.collections.trie;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import unit731.hunspeller.collections.trie.sequencers.TrieSequencer;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(of = {"sequence", "startIndex", "endIndex"})
public class TrieNode<T>{

	@Getter @Setter private String sequence;
	@Getter private int startIndex;
	@Getter private int endIndex;

	@Getter private T value;
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

	public void clear(){
		sequence = null;
		startIndex = 0;
		endIndex = 0;

		value = null;
		Optional.ofNullable(children)
			.ifPresent(Map::clear);
	}

	public String getSubSequence(){
		return sequence.substring(startIndex, endIndex);
	}

	public T setValue(T value){
		T previousValue = this.value;
		this.value = value;
		return previousValue;
	}

	public boolean isLeaf(){
		return (value != null);
	}

	public TrieNode<T> getChild(int stem){
		return Optional.ofNullable(children)
			.map(c -> c.get(stem))
			.orElse(null);
	}

	public void addChild(int stem, TrieNode<T> node){
		children = Optional.ofNullable(children)
			.orElseGet(HashMap::new);
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
		return (value == null && !hasChildren());
	}

	public boolean hasChildren(){
		return (children != null);
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
		lowerNode.children = children;
		children = null;
		endIndex = startIndex + index;

		int stem = sequencer.hashOf(sequence, endIndex);
		addChild(stem, lowerNode);

		return lowerNode;
	}

}
