package unit731.hunspeller.collections.trie;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.NoArgsConstructor;


@NoArgsConstructor
public class Trie<T>{

	/** the root of the Trie */
	private TrieNode<T> root = new TrieNode<>();


	public Trie(Trie<T> trie) throws CloneNotSupportedException{
		root = trie.root.clone();
	}

	public void clear(){
		root.clear();
	}

	/**
	 * Adds a word into the Trie
	 *
	 * @param word	The word to be added
	 * @param data	The payload for the inserted word
	 * @return The node just inserted
	 */
	public TrieNode<T> add(String word, T data){
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
		node.setData(data);
		node.setLeaf();
		return node;
	}

	public boolean remove(String word){
		boolean result = false;
		List<Prefix<T>> prefixes = findPrefix(word);
		if(prefixes.size() == 1)
			result = removeSingle(word, prefixes.get(0));
		return result;
	}

	public boolean removeAll(String word){
		List<Prefix<T>> prefixes = findPrefix(word);
		return prefixes.stream()
			.map(prefix -> removeSingle(word, prefix))
			.reduce(true, (a, b) -> a && b);
	}

	private boolean removeSingle(String word, Prefix<T> pref){
		boolean result = false;
		if(pref.isLeaf()){
			Character stem = getAtIndex(word, word.length() - 1);
			result = (pref.getParent().removeChild(stem) != null);
		}
		return result;
	}

	public List<Prefix<T>> findPrefix(String word){
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
	public TrieNode<T> contains(String word){
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
