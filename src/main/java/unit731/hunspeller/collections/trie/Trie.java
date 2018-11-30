package unit731.hunspeller.collections.trie;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import unit731.hunspeller.collections.trie.sequencers.TrieSequencerInterface;


/**
 * An implementation of a compact Trie.
 * 
 * @see <a href="https://github.com/ClickerMonkey/TrieHard">TrieHard</a>
 * 
 * @param <S>	The sequence type.
 * @param <H>	The hash type (used to find a particular child).
 * @param <V>	The value type.
 */
public class Trie<S, H, V>{

	/** The matching logic used for retrieving values from a Trie or for determining the existence of values given an input/key sequence */
	public static enum TrieMatch{
		/**
		 * An exact match requires the input sequence to be an exact match to the sequences stored in the Trie. If the sequence "meow" is
		 * stored in the Trie, then it can only match on "meow".
		 */
		EXACT,
		/**
		 * A start-with match requires the input sequence to be a superset of the sequences stored in the Trie. If the sequence "meow" is
		 * stored in the Trie, then it can match on "meow", "meowa", "meowab", etc.
		 */
		STARTS_WITH
	}


	private final TrieNode<S, H, V> root = TrieNode.makeRoot();

	private final TrieSequencerInterface<S, H> sequencer;


	public Trie(TrieSequencerInterface<S, H> sequencer){
		this.sequencer = sequencer;
	}

	public void clear(){
		root.clear();
	}

	public boolean isEmpty(){
		return root.isEmpty();
	}

	/**
	 * Puts the value in the Trie with the given sequence.
	 *
	 * @param sequence	Sequence with which the specified value is to be associated
	 * @param value		The value to place in the Trie
	 * @throws NullPointerException if the specified <code>sequence</code> or <code>value</code> is <code>null</code>
	 */
	public void put(S sequence, V value){
		Objects.requireNonNull(sequence);
		Objects.requireNonNull(value);

		int sequenceLength = sequencer.lengthOf(sequence);
		int sequenceOffset = 0;
		TrieNode<S, H, V> node = root;
		while(node != null){
			int nodeLength = node.getLength();
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
			TrieNode<S, H, V> nextNode = node.getChildForInsert(stem);
			if(nextNode == null)
				createAndAttachNode(sequence, sequenceOffset, sequenceLength, value, node);

			//full match, query or node remaining
			node = nextNode;
		}
	}

	private void createAndAttachNode(S sequence, int startIndex, int endIndex, V value, TrieNode<S, H, V> parent){
		TrieNode<S, H, V> newNode = new TrieNode<>(sequence, startIndex, endIndex, value);

		H stem = sequencer.hashOf(sequence, startIndex);
		parent.addChild(stem, newNode);
	}

	/**
	 * Gets the value that matches the given sequence.
	 *
	 * @param sequence	The sequence to match.
	 * @return	The value for the given sequence, or the default value of the Trie if no match was found. The default value of a Trie is by default
	 *		null.
	 */
	public V get(S sequence){
		Objects.requireNonNull(sequence);

		TrieNode<S, H, V> node = searchAndApply(sequence, TrieMatch.EXACT, null);
		return (node != null? node.getValue(): null);
	}

	/**
	 * Removes the sequence from the Trie and returns it's value. The sequence must be an exact match, otherwise nothing will be removed.
	 *
	 * @param sequence	The sequence to remove.
	 * @return	The data of the removed sequence, or null if no sequence was removed.
	 */
	public V remove(S sequence){
		Objects.requireNonNull(sequence);

		TrieNode<S, H, V> node = searchAndApply(sequence, TrieMatch.EXACT, (parent, stem) -> parent.removeChild(stem, sequencer));
		return (node != null? node.getValue(): null);
	}

	public Collection<TrieNode<S, H, V>> collectPrefixes(S sequence){
		Objects.requireNonNull(sequence);

		Set<TrieNode<S, H, V>> prefixes = new HashSet<>();
		searchAndApply(sequence, TrieMatch.STARTS_WITH, (parent, stem) -> prefixes.add(parent.getChildForRetrieve(stem, sequencer)));
		return prefixes;
	}

	/**
	 * Searches in the Trie based on the sequence query.
	 *
	 * @param sequence	The query sequence.
	 * @param matchType	The matching logic.
	 * @param callback	The callback to be executed on match found.
	 * @return	The node that best matched the query based on the logic.
	 */
	private TrieNode<S, H, V> searchAndApply(S sequence, TrieMatch matchType, BiConsumer<TrieNode<S, H, V>, H> callback){
		int sequenceLength = sequencer.lengthOf(sequence);
		int sequenceOffset = root.getEndIndex();
		//if the sequence is empty, return null
		if(sequenceLength == 0 || sequenceLength < sequenceOffset)
			return null;

		H stem = sequencer.hashOf(sequence, sequenceOffset);
		TrieNode<S, H, V> parent = root;
		TrieNode<S, H, V> node = root.getChildForRetrieve(stem, sequencer);
		while(node != null){
			int nodeLength = node.getLength();
			int max = Math.min(nodeLength, sequenceLength - sequenceOffset);
			int matches = sequencer.matchesGet(node.getSequence(), node.getStartIndex(), sequence, sequenceOffset, max);

			sequenceOffset += matches;

			if(matches != max || max != nodeLength)
				//not found
				node = null;
			else if(sequenceOffset == sequenceLength || !node.hasChildren()){
				if(callback != null && node.isLeaf(sequencer) && sequencer.startsWith(sequence, node.getSequence()))
					callback.accept(parent, stem);

				break;
			}
			else{
				//call callback for each leaf node found so far
				if(callback != null && node.isLeaf(sequencer) && sequencer.startsWith(sequence, node.getSequence()))
					callback.accept(parent, stem);

				stem = sequencer.hashOf(sequence, sequenceOffset);
				TrieNode<S, H, V> next = node.getChildForRetrieve(stem, sequencer);

				//if there is no next, node could be a STARTS_WITH match
				if(next == null)
					break;

				parent = node;
				node = next;
			}
		}

		//EXACT matches
		if(node != null && matchType == TrieMatch.EXACT){
			//check length of last node against query
			int endIndex = node.getEndIndex();
			if(!node.isLeaf(sequencer) || endIndex != sequenceLength)
				return null;

			//check actual sequence values
			S seq = node.getSequence();
			if(sequencer.lengthOf(seq) != sequenceLength || sequencer.matchesGet(seq, 0, sequence, 0, endIndex) != sequenceLength)
				return null;
		}

		return node;
	}

	/**
	 * Search the given string and return an object if it lands on a sequence, essentially testing if the sequence exists in the Trie.
	 *
	 * @param sequence	The sequence to search for
	 * @return Whether the sequence is fully contained into this Trie
	 */
	public boolean containsKey(S sequence){
		return (get(sequence) != null);
	}

	/**
	 * Apply a function to each leaf, traversing the tree in level order.
	 * 
	 * @param callback	Function that will be executed for each leaf of the Trie
	 */
	public void forEachLeaf(Consumer<TrieNode<S, H, V>> callback){
		Objects.requireNonNull(callback);

		find(root, node -> {
			if(node.isLeaf(sequencer))
				callback.accept(node);
			return false;
		});
	}

	/**
	 * Apply a function to each node, traversing the tree in level order, until the callback responds <code>true</code>.
	 * 
	 * @param root			Node to start with
	 * @param callback	Function that will be executed for each node of the Trie, it has to return <code>true</code> if a node matches
	 * @return	<code>true</code> if the node is found
	 */
	public boolean find(TrieNode<S, H, V> root, Function<TrieNode<S, H, V>, Boolean> callback){
		Objects.requireNonNull(callback);

		boolean found = false;
		Stack<TrieNode<S, H, V>> level = new Stack<>();
		level.push(root);
		while(!level.empty()){
			TrieNode<S, H, V> node = level.pop();

			node.forEachChild(level::push);

			if(callback.apply(node)){
				found = true;
				break;
			}
		}
		return found;
	}

}
