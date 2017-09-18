package unit731.hunspeller.collections.trie;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * @param <T>	The data type.
 */
public class Trie<T>{

	private final TrieNode<T> root;


	public Trie(){
		root = new TrieNode<>();
	}

	public Trie(Trie<T> trie) throws CloneNotSupportedException{
		root = trie.root.clone();
	}

	public void clear(){
		root.clear();
	}

	public boolean isEmpty(){
		return root.isEmpty();
	}

	/**
	 * Adds a word into the Trie
	 *
	 * @param word		Word with which the specified value is to be associated
	 * @param value	Value to be associated with the specified key
	 * @return	The previous value associated with <tt>word</tt>, or <tt>null</tt> if there was no mapping for <tt>word</tt>.
	 *		(A <tt>null</tt> return can also indicate that the map previously associated <tt>null</tt> with <tt>word</tt>)
	 * @throws NullPointerException if the specified <tt>word</tt> is <tt>null</tt>
	 */
	public T put(String word, T value){
		Objects.requireNonNull(word);

		TrieNode<T> node = root;
		int size = word.length();
		for(int i = 0; i < size; i ++){
			Character stem = getAtIndex(word, i);
			TrieNode<T> nextNode = node.getChild(stem);
			if(nextNode == null){
				nextNode = new TrieNode<>();
				node.addChild(stem, nextNode);
			}
			node = nextNode;
		}
		T oldValue = node.getValue();
		node.setValue(value);
		node.setLeaf();
		return oldValue;
	}

	public void putAll(Map<String, ? extends T> map){
		map.entrySet()
			.forEach(e -> put(e.getKey(), e.getValue()));
	}

	/**
	 * Removes the word from the Trie and returns it's value. The sequence must be an exact match, otherwise nothing will be removed.
	 *
	 * @param word	The word to remove.
	 * @return	The data of the removed word, or null if no word was removed.
	 */
	public T remove(String word){
		T result = null;
		List<Prefix<T>> prefixes = findPrefix(word);
		if(prefixes.size() == 1)
			result = removeSingle(word, prefixes.get(0));
		return result;
	}

	public boolean removeAll(String word){
		List<Prefix<T>> prefixes = findPrefix(word);
		return prefixes.stream()
			.map(prefix -> removeSingle(word, prefix))
			.map(Objects::nonNull)
			.reduce(true, (a, b) -> a && b);
	}

	private T removeSingle(String word, Prefix<T> pref){
		T value = null;
		if(pref.isLeaf()){
			Character stem = getAtIndex(word, word.length() - 1);
			TrieNode<T> node = pref.getParent().removeChild(stem);
			value = (node != null? node.getValue(): null);
		}
		return value;
	}

	public List<Prefix<T>> findPrefix(String word){
		Objects.requireNonNull(word);

		TrieNode<T> node = root;
		List<Prefix<T>> result = new ArrayList<>();
		int size = word.length();
		for(int i = 0; i < size; i ++){
			Character stem = getAtIndex(word, i);
			TrieNode<T> nextNode = node.getChild(stem);
			if(nextNode == null)
				break;

			TrieNode<T> parent = node;
			node = nextNode;

			if(node.isLeaf())
				result.add(new Prefix<>(node, i, parent));
		}
		return result;
	}

	/**
	 * Search the given string and return an object if it lands on a word, essentially testing if the word exists in the trie.
	 *
	 * @param word	The word to search for
	 * @return The node found if the word is contained into this trie
	 */
	public TrieNode<T> containsKey(String word){
		Objects.requireNonNull(word);

		TrieNode<T> node = root;
		int i;
		int size = word.length();
		for(i = 0; i < size; i ++){
			Character stem = getAtIndex(word, i);
			TrieNode<T> nextNode = node.getChild(stem);
			if(nextNode == null)
				break;

			node = nextNode;
		}
		return (i == size && node != null && node.isLeaf()? node: null);
	}

	private Character getAtIndex(String word, int index){
		return word.charAt(index);
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
	public boolean find(Function<TrieNode<T>, Boolean> callback){
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
