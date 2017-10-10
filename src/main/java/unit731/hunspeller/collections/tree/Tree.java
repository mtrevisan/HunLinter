package unit731.hunspeller.collections.tree;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import unit731.hunspeller.collections.tree.sequencers.TreeSequencer;


/**
 * An implementation of a Tree.
 * 
 * @param <S>	The sequence type.
 * @param <H>	The hash type (used to find a particular child).
 * @param <V>	The value type.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Tree<S, H, V>{

	/** The matching logic used for retrieving values from a Trie or for determining the existence of values given an input/key sequence */
	public static enum TreeMatch{
		/**
		 * An exact match requires the input sequence to be an exact match to the sequences stored in the Tree. If the sequence "meow" is
		 * stored in the Tree, then it can only match on "meow".
		 */
		EXACT,
		/**
		 * A start-with match requires the input sequence to be a superset of the sequences stored in the Tree. If the sequence "meow" is
		 * stored in the Tree, then it can match on "meow", "meowa", "meowab", etc.
		 */
		STARTS_WITH
	}


	private final TreeNode<S, H, V> root = TreeNode.makeRoot();
	private TreeSequencer<S, H> sequencer;


	public Tree(TreeSequencer<S, H> sequencer){
		this.sequencer = sequencer;
	}

	public void clear(){
		root.clear();
	}

	public boolean isEmpty(){
		return root.isEmpty();
	}

	/**
	 * Puts the value in the Tree with the given sequence.
	 *
	 * @param sequence	Sequence with which the specified value is to be associated
	 * @param value		The value to place in the Tree
	 * @throws NullPointerException if the specified <code>sequence</code> or <code>value</code> is <code>null</code>
	 */
	public void put(S sequence, V value){
		Objects.requireNonNull(sequence);
		Objects.requireNonNull(value);

		int sequenceLength = sequencer.lengthOf(sequence);
		int sequenceOffset = 0;
		TreeNode<S, H, V> node = root;
		while(node != null){
			int nodeLength = node.getEndIndex() - node.getStartIndex();
			int max = Math.min(nodeLength, sequenceLength - sequenceOffset);
			int matches = sequencer.matchesPut(node.getSequence(), node.getStartIndex(), sequence, sequenceOffset, max);

			sequenceOffset += matches;

			//mismatch in current node
			if(matches != max){
				node.split(matches, node.getValue(), sequencer);

				createAndAttachNode(sequence, sequenceOffset, sequenceLength, value, node);

				break;
			}

			//partial match to the current node
			if(max < nodeLength){
				node.split(max, value, sequencer);
				node.setSequence(sequence);

				break;
			}

			//full match to sequence, replace/add value and sequence
			if(sequenceOffset == sequenceLength){
				node.setSequence(sequence);
				node.addValue(value);

				break;
			}

			//full match, end of the query, or node
			if(!node.hasChildren()){
				createAndAttachNode(sequence, sequenceOffset, sequenceLength, value, node);

				break;
			}

			//full match, end of node
			H stem = sequencer.hashOf(sequence, sequenceOffset);
			TreeNode<S, H, V> nextNode = node.getChildForInsert(stem);
			if(nextNode == null)
				createAndAttachNode(sequence, sequenceOffset, sequenceLength, value, node);

			//full match, query or node remaining
			node = nextNode;
		}
	}

	private void createAndAttachNode(S sequence, int startIndex, int endIndex, V value, TreeNode<S, H, V> parent){
		TreeNode<S, H, V> newNode = new TreeNode<>(sequence, startIndex, endIndex, value);

		H stem = sequencer.hashOf(sequence, startIndex);
		parent.addChild(stem, newNode);
	}

	/**
	 * Gets the value that matches the given sequence.
	 *
	 * @param sequence	The sequence to match.
	 * @return	The value for the given sequence, or the default value of the Tree if no match was found. The default value of a Tree is by default
	 *		null.
	 */
	public V get(S sequence){
		TreeNode<S, H, V> node = searchAndApply(sequence, TreeMatch.EXACT, null);
		return (node != null? node.getValue(): null);
	}

	/**
	 * Removes the sequence from the Tree and returns it's value. The sequence must be an exact match, otherwise nothing will be removed.
	 *
	 * @param sequence	The sequence to remove.
	 * @return	The data of the removed sequence, or null if no sequence was removed.
	 */
	public V remove(S sequence){
		TreeNode<S, H, V> node = searchAndApply(sequence, TreeMatch.EXACT, (parent, stem) -> parent.removeChild(stem, sequencer));
		return (node != null? node.getValue(): null);
	}

	public Collection<TreeNode<S, H, V>> collectPrefixes(S sequence){
		Set<TreeNode<S, H, V>> prefixes = new HashSet<>();
		searchAndApply(sequence, TreeMatch.STARTS_WITH, (parent, stem) -> prefixes.add(parent.getChildForRetrieve(stem, sequencer)));
		return prefixes;
	}

	/**
	 * Searches in the Tree based on the sequence query.
	 *
	 * @param sequence	The query sequence.
	 * @param matchType	The matching logic.
	 * @param callback	The callback to be executed on match found.
	 * @return	The node that best matched the query based on the logic.
	 */
	private TreeNode<S, H, V> searchAndApply(S sequence, TreeMatch matchType, BiConsumer<TreeNode<S, H, V>, H> callback){
		Objects.requireNonNull(sequence);

		int sequenceLength = sequencer.lengthOf(sequence);
		int sequenceOffset = root.getEndIndex();
		//if the sequence is empty, return null
		if(sequenceLength == 0 || sequenceLength < sequenceOffset)
			return null;

		H stem = sequencer.hashOf(sequence, sequenceOffset);
		TreeNode<S, H, V> parent = root;
		TreeNode<S, H, V> node = root.getChildForRetrieve(stem, sequencer);
		while(node != null){
			int nodeLength = node.getEndIndex() - node.getStartIndex();
			int max = Math.min(nodeLength, sequenceLength - sequenceOffset);
			int matches = sequencer.matchesGet(node.getSequence(), node.getStartIndex(), sequence, sequenceOffset, max);

			sequenceOffset += matches;

			if(matches != max || matches == max && max != nodeLength)
				//not found
				node = null;
			else if(sequenceOffset == sequenceLength || !node.hasChildren()){
				if(callback != null && node.isLeaf() && sequencer.startsWith(sequence, node.getSequence()))
					callback.accept(parent, stem);

				break;
			}
			else{
				//call callback for each leaf node found so far
				if(callback != null && node.isLeaf() && sequencer.startsWith(sequence, node.getSequence()))
					callback.accept(parent, stem);

				stem = sequencer.hashOf(sequence, sequenceOffset);
				TreeNode<S, H, V> next = node.getChildForRetrieve(stem, sequencer);

				//if there is no next, node could be a STARTS_WITH match
				if(next == null)
					break;

				parent = node;
				node = next;
			}
		}

		//EXACT matches
		if(node != null && matchType == TreeMatch.EXACT){
			//check length of last node against query
			int endIndex = node.getEndIndex();
			if(!node.isLeaf() || endIndex != sequenceLength)
				return null;

			//check actual sequence values
			S seq = node.getSequence();
			if(sequencer.lengthOf(seq) != sequenceLength || sequencer.matchesGet(seq, 0, sequence, 0, endIndex) != sequenceLength)
				return null;
		}

		return node;
	}

	/**
	 * Search the given string and return an object if it lands on a sequence, essentially testing if the sequence exists in the Tree.
	 *
	 * @param sequence	The sequence to search for
	 * @return Whether the sequence is fully contained into this Tree
	 */
	public boolean containsKey(S sequence){
		return (get(sequence) != null);
	}

	/**
	 * Apply a function to each leaf, traversing the tree in level order.
	 * 
	 * @param callback	Function that will be executed for each leaf of the Tree
	 */
	public void forEachLeaf(Consumer<TreeNode<S, H, V>> callback){
		Objects.requireNonNull(callback);

		find(root, node -> {
			if(node.isLeaf())
				callback.accept(node);
			return false;
		});
	}

	/**
	 * Apply a function to each node, traversing the tree in level order, until the callback responds <code>true</code>.
	 * 
	 * @param root			Node to start with
	 * @param callback	Function that will be executed for each node of the Tree, it has to return <code>true</code> if a node matches
	 * @return	<code>true</code> if the node is found
	 */
	public boolean find(TreeNode<S, H, V> root, Function<TreeNode<S, H, V>, Boolean> callback){
		Objects.requireNonNull(callback);

		boolean found = false;
		Stack<TreeNode<S, H, V>> level = new Stack<>();
		level.push(root);
		while(!level.empty()){
			TreeNode<S, H, V> node = level.pop();

			node.forEachChild(level::push);

			if(callback.apply(node)){
				found = true;
				break;
			}
		}
		return found;
	}

}
