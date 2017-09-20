package unit731.hunspeller.collections.trie;

import unit731.hunspeller.collections.trie.sequencers.StringTrieSequencer;
import unit731.hunspeller.collections.trie.sequencers.TrieSequencer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * @param <T>	The data type.
 */
public class Trie<T>{

	private final TrieNode<T> root;
	private final TrieSequencer<String> sequencer = new StringTrieSequencer();


	public Trie(){
		root = new TrieNode<>();
	}

	public Trie(Trie<T> trie){
		root = trie.root.clone();
	}

	public void clear(){
		root.clear();
	}

	public boolean isEmpty(){
		return root.isEmpty();
	}

	/**
	 * Adds a sequence into the Trie
	 *
	 * @param sequence		Sequence with which the specified value is to be associated
	 * @param value	Value to be associated with the specified key
	 * @return	The previous value associated with <tt>sequence</tt>, or <tt>null</tt> if there was no mapping for <tt>sequence</tt>.
	 *		(A <tt>null</tt> return can also indicate that the map previously associated <tt>null</tt> with <tt>sequence</tt>)
	 * @throws NullPointerException if the specified <tt>sequence</tt> is <tt>null</tt>
	 */
	public T put(String sequence, T value){
		Objects.requireNonNull(sequence);
		Objects.requireNonNull(value);

		TrieNode<T> node = root;
		int size = sequence.length();
		for(int i = 0; i < size; i ++){
			int stem = sequencer.hashOf(sequence, i);
			TrieNode<T> nextNode = node.getChild(stem);
			if(nextNode == null){
				nextNode = new TrieNode<>();
				node.addChild(stem, nextNode);
			}
			node = nextNode;
		}
		T oldValue = node.getValue();
		node.setValue(value);
		return oldValue;
	}

	public T get(String sequence){
		Objects.requireNonNull(sequence);

		T foundValue = null;
		TrieNode<T> node = root;
		int size = sequence.length();
		for(int i = 0; i < size; i ++){
			int stem = sequencer.hashOf(sequence, i);
			TrieNode<T> nextNode = node.getChild(stem);
			if(nextNode == null)
				break;

			if(nextNode.isLeaf() && i + 1 == size){
				foundValue = nextNode.getValue();
				break;
			}

			node = nextNode;
		}
		return foundValue;
	}

	/**
	 * Removes the sequence from the Trie and returns it's value. The sequence must be an exact match, otherwise nothing will be removed.
	 *
	 * @param sequence	The sequence to remove.
	 * @return	The data of the removed sequence, or null if no sequence was removed.
	 */
	public T remove(String sequence){
		Objects.requireNonNull(sequence);

		T foundValue = null;
		TrieNode<T> node = root;
		int size = sequence.length();
		for(int i = 0; i < size; i ++){
			int stem = sequencer.hashOf(sequence, i);
			TrieNode<T> nextNode = node.getChild(stem);
			if(nextNode == null)
				break;

			TrieNode<T> parent = node;
			node = nextNode;

			if(node.isLeaf() && i + 1 == size){
				parent.removeChild(stem);
				foundValue = node.getValue();
				break;
			}
		}
		return foundValue;
	}

	public Iterable<Prefix<T>> collectPrefixes(String sequence){
		Objects.requireNonNull(sequence);

		TrieNode<T> node = root;
		Collection<Prefix<T>> result = new ArrayList<>();
		int size = sequence.length();
		for(int i = 0; i < size; i ++){
			int stem = sequencer.hashOf(sequence, i);
			TrieNode<T> nextNode = node.getChild(stem);
			if(nextNode == null)
				break;

			if(nextNode.isLeaf())
				result.add(new Prefix<>(nextNode, i, node));

			node = nextNode;
		}
		return result;
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

		find(node -> {
			if(node.isLeaf())
				callback.accept(node);
			return false;
		});
	}

	/**
	 * Apply a function to each node, traversing the tree in level order, until the callback responds <code>true</code>.
	 * 
	 * @param callback	Function that will be executed for each node of the trie, it has to return <code>true</code> if a node matches
	 * @return	<code>true</code> if the node is found
	 */
	private boolean find(Function<TrieNode<T>, Boolean> callback){
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
