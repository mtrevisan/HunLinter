package unit731.hunspeller.collections.trie;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import unit731.hunspeller.collections.trie.sequencers.StringTrieSequencer;
import unit731.hunspeller.collections.trie.sequencers.TrieSequencer;


/**
 * An implementation of a compact Trie.
 * 
 * @see <a href="https://github.com/ClickerMonkey/TrieHard">TrieHard</a>
 * 
 * @param <T>	The data type.
 */
public class Trie<T>{

	/** The matching logic used for retrieving values from a Trie or for determining the existence of values given an input/key sequence */
	public enum TrieMatch{
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


	private final TrieNode<T> root = TrieNode.makeRoot();
	private final TrieSequencer<String> sequencer = new StringTrieSequencer();


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
	 * @return	The previous value associated with <tt>sequence</tt>, or <tt>null</tt> if there was no mapping for <tt>sequence</tt>.
	 *		(A <tt>null</tt> return can also indicate that the map previously associated <tt>null</tt> with <tt>sequence</tt>)
	 * @throws NullPointerException if the specified <tt>sequence</tt> or <tt>value</tt> is <tt>null</tt>
	 */
	public T put(String sequence, T value){
		Objects.requireNonNull(sequence);
		Objects.requireNonNull(value);

		int sequenceOffset = 0;
		int size = sequence.length();
		int stem = sequencer.hashOf(sequence, sequenceOffset);
		TrieNode<T> node = root.getChild(stem);
		if(node == null){
			node = new TrieNode<>(sequence, sequenceOffset, size, value);
			root.addChild(stem, node);

			return null;
		}

		while(node != null){
			int nodeLength = node.getEndIndex() - node.getStartIndex();
			int max = Math.min(nodeLength, size - sequenceOffset);
			int matches = sequencer.matches(node.getSequence(), node.getStartIndex(), sequence, sequenceOffset, max);

			sequenceOffset += matches;

			//mismatch in current node
			if(matches != max){
				node.split(matches, node.getValue(), sequencer);

				TrieNode<T> newNode = new TrieNode<>(sequence, sequenceOffset, size, value);
				stem = sequencer.hashOf(sequence, sequenceOffset);
				node.addChild(stem, newNode);

				return null;
			}

			//partial match to the current node
			if(max < nodeLength){
				node.split(max, value, sequencer);
				node.setSequence(sequence);

				return null;
			}

			//full match to sequence, replace value and sequence
			if(sequenceOffset == size){
				node.setSequence(sequence);

				return node.setValue(value);
			}

			//full match, end of the query, or node
			if(!node.hasChildren()){
				TrieNode<T> newNode = new TrieNode<>(sequence, sequenceOffset, size, value);
				stem = sequencer.hashOf(sequence, sequenceOffset);
				node.addChild(stem, newNode);

				return null;
			}

			//full match, end of node
			stem = sequencer.hashOf(sequence, sequenceOffset);
			TrieNode<T> nextNode = node.getChild(stem);
			if(nextNode == null){
				TrieNode<T> newNode = new TrieNode<>(sequence, sequenceOffset, size, value);
				node.addChild(stem, newNode);
			}

			//full match, query or node remaining
         node = nextNode;
		}
		return null;
	}

	/**
	 * Gets the value that matches the given sequence.
	 *
	 * @param sequence	The sequence to match.
	 * @return	The value for the given sequence, or the default value of the Trie if no match was found. The default value of a Trie is by default
	 *		null.
	 */
	public T get(String sequence){
		TrieNode<T> node = searchAndApply(sequence, TrieMatch.EXACT, null);
		return (node != null? node.getValue(): null);
	}

	/**
	 * Removes the sequence from the Trie and returns it's value. The sequence must be an exact match, otherwise nothing will be removed.
	 *
	 * @param sequence	The sequence to remove.
	 * @return	The data of the removed sequence, or null if no sequence was removed.
	 */
	public T remove(String sequence){
		TrieNode<T> node = searchAndApply(sequence, TrieMatch.EXACT, (parent, stem) -> parent.removeChild(stem));
		return (node != null? node.getValue(): null);
	}

	public Collection<TrieNode<T>> collectPrefixes(String sequence){
		Collection<TrieNode<T>> prefixes = new ArrayList<>();
		searchAndApply(sequence, TrieMatch.STARTS_WITH, (parent, stem) -> {
			TrieNode<T> node = parent.getChild(stem);
			if(!prefixes.contains(node))
				prefixes.add(node);
		});
		return prefixes;
	}

	/**
	 * Searches in the Trie based on the sequence query.
	 *
	 * @param sequence	The query sequence.
	 * @param match		The matching logic.
	 * @param callback	The callback to be executed on match found.
	 * @return	The node that best matched the query based on the logic.
	 */
	private TrieNode<T> searchAndApply(String sequence, TrieMatch matchType, BiConsumer<TrieNode<T>, Integer> callback){
		Objects.requireNonNull(sequence);

		int sequenceLength = sequence.length();
		int sequenceOffset = root.getEndIndex();
		//if the sequence is empty, return null
		if(sequenceLength == 0 || sequenceLength < sequenceOffset)
			return null;

		int stem = sequencer.hashOf(sequence, sequenceOffset);
		TrieNode<T> parent = root;
		TrieNode<T> node = root.getChild(stem);
		while(node != null){
			int nodeLength = node.getEndIndex() - node.getStartIndex();
			int max = Math.min(nodeLength, sequenceLength - sequenceOffset);
			int matches = sequencer.matches(node.getSequence(), node.getStartIndex(), sequence, sequenceOffset, max);

			sequenceOffset += matches;

			//not found
			if(matches != max || matches == max && max != nodeLength)
				node = null;
			//either EXACT or STARTS_WITH match
			else if(sequenceOffset == sequenceLength || !node.hasChildren()){
				if(callback != null && node.isLeaf())
					callback.accept(parent, stem);

				break;
			}
			else{
				//call callback for each leaf node found so far
				if(callback != null && node.isLeaf() && sequence.startsWith(node.getSequence()))
					callback.accept(parent, stem);

				stem = sequencer.hashOf(sequence, sequenceOffset);
				TrieNode<T> next = node.getChild(stem);

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
			if(!node.isLeaf() || endIndex != sequenceLength)
				return null;

			//check actual sequence values
			String seq = node.getSequence();
			if(seq.length() != sequenceLength || sequencer.matches(seq, 0, sequence, 0, endIndex) != sequenceLength)
				return null;
		}

		return node;
	}

	/**
	 * Search the given string and return an object if it lands on a sequence, essentially testing if the sequence exists in the trie.
	 *
	 * @param sequence	The sequence to search for
	 * @return Whether the sequence is fully contained into this trie
	 */
	public boolean containsKey(String sequence){
		return (get(sequence) != null);
	}

	/**
	 * Apply a function to each leaf, traversing the tree in level order.
	 * 
	 * @param callback	Function that will be executed for each leaf of the trie
	 */
	public void forEachLeaf(Consumer<TrieNode<T>> callback){
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
	 * @param callback	Function that will be executed for each node of the trie, it has to return <code>true</code> if a node matches
	 * @return	<code>true</code> if the node is found
	 */
	private boolean find(TrieNode<T> root, Function<TrieNode<T>, Boolean> callback){
		Objects.requireNonNull(callback);

		boolean found = false;
		Stack<TrieNode<T>> level = new Stack<>();
		level.push(root);
		while(!level.empty()){
			TrieNode<T> node = level.pop();

			node.forEachChild(level::push);

			if(callback.apply(node)){
				found = true;
				break;
			}
		}
		return found;
	}

}
