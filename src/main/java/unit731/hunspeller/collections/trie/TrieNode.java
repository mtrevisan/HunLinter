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


/**
 * @param <S>	The sequence type.
 * @param <H>	The hash type (used to find a particular child).
 * @param <V>	The value type.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(of = {"sequence", "startIndex", "endIndex"})
public class TrieNode<S, H, V>{

	@Getter @Setter private S sequence;
	@Getter private int startIndex;
	@Getter private int endIndex;

	@Getter private V value;
	private Map<H, TrieNode<S, H, V>> children;


	public TrieNode(S sequence, int startIndex, int endIndex, V value){
		this.sequence = sequence;
		this.startIndex = startIndex;
		this.endIndex = endIndex;

		this.value = value;
	}

	public static <S, H, V> TrieNode<S, H, V> makeRoot(){
		return new TrieNode<>();
	}

	public void clear(){
		sequence = null;
		startIndex = 0;
		endIndex = 0;

		value = null;
		children = null;
	}

	public V setValue(V value){
		V previousValue = this.value;
		this.value = value;
		return previousValue;
	}

	public boolean isLeaf(){
		return (value != null);
	}

	public TrieNode<S, H, V> getChild(H stem, TrieSequencer<S, H> sequencer){
		TrieNode<S, H, V> child = null;
		if(children != null)
			child = sequencer.getChild(children, stem);
		return child;
	}

	public void addChild(H stem, TrieNode<S, H, V> node){
		children = Optional.ofNullable(children)
			.orElseGet(HashMap::new);
		children.put(stem, node);
	}

	public TrieNode<S, H, V> removeChild(H stem, TrieSequencer<S, H> sequencer){
		TrieNode<S, H, V> removedNode = null;
		if(children != null){
			TrieNode<S, H, V> node = sequencer.getChild(children, stem);
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

	public void forEachChild(Consumer<TrieNode<S, H, V>> callback){
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
	public TrieNode<S, H, V> split(int index, V value, TrieSequencer<S, H> sequencer){
		TrieNode<S, H, V> lowerNode = new TrieNode<>(sequence, startIndex + index, endIndex, value);
		lowerNode.children = children;
		children = null;
		endIndex = startIndex + index;

		H stem = sequencer.hashOf(sequence, endIndex);
		addChild(stem, lowerNode);

		return lowerNode;
	}

}
