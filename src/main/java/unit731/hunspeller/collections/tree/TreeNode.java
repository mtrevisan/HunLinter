package unit731.hunspeller.collections.tree;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
public class TreeNode<S, H, V>{

	@Getter @Setter private S sequence;
	@Getter private int startIndex;
	@Getter private int endIndex;

	@Getter private V value;
	private Set<TreeNode<S, H, V>> children;


	public TreeNode(S sequence, int startIndex, int endIndex, V value){
		this.sequence = sequence;
		this.startIndex = startIndex;
		this.endIndex = endIndex;

		this.value = value;
	}

	public static <S, H, V> TreeNode<S, H, V> makeRoot(){
		return new TreeNode<>();
	}

	public void clear(){
		sequence = null;
		startIndex = 0;
		endIndex = 0;

		value = null;
		children = null;
	}

	/**
	 * Add the current value (along with the old one if the value type is a {@link List})
	 * 
	 * @param value	The value to add
	 */
	@SuppressWarnings("unchecked")
	public void addValue(V value){
		if(this.value != null && List.class.isAssignableFrom(value.getClass()))
			((List)this.value).addAll((List<?>)value);
		else
			this.value = value;
	}

	public boolean isLeaf(){
		return (value != null);
	}

	public TreeNode<S, H, V> getChildForInsert(H stem){
		return (children != null? children.get(stem): null);
	}

	public TreeNode<S, H, V> getChildForRetrieve(H stem, TrieSequencer<S, H> sequencer){
		return (children != null? sequencer.getChild(children, stem): null);
	}

	public void addChild(H stem, TreeNode<S, H, V> node){
		children = Optional.ofNullable(children)
			.orElseGet(HashSet::new);
		children.put(stem, node);
	}

	public TreeNode<S, H, V> removeChild(H stem, TrieSequencer<S, H> sequencer){
		TreeNode<S, H, V> removedNode = null;
		if(children != null){
			TreeNode<S, H, V> node = sequencer.getChild(children, stem);
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

	public void forEachChild(Consumer<TreeNode<S, H, V>> callback){
		if(children != null)
			children.forEach(callback::accept);
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
	public TreeNode<S, H, V> split(int index, V value, TrieSequencer<S, H> sequencer){
		TreeNode<S, H, V> lowerNode = new TreeNode<>(sequence, startIndex + index, endIndex, value);
		lowerNode.children = children;
		if(lowerNode.startIndex == endIndex - 1)
			this.value = null;
		children = null;
		endIndex = lowerNode.startIndex;

		H stem = sequencer.hashOf(sequence, endIndex);
		addChild(stem, lowerNode);

		return lowerNode;
	}

}
