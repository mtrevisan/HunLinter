package unit731.hunspeller.collections.trie;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;


public class TrieNode<T>{

	@Getter @Setter private T data;
	@Getter private boolean leaf;
	private final Map<Character, TrieNode<T>> children = new HashMap<>();


	@Override
	public TrieNode<T> clone() throws CloneNotSupportedException{
		super.clone();

		TrieNode<T> clone = new TrieNode<>();
		clone.data = data;
		clone.leaf = leaf;
		Set<Map.Entry<Character, TrieNode<T>>> entries = children.entrySet();
		for(Map.Entry<Character, TrieNode<T>> entry : entries)
			clone.children.put(entry.getKey(), entry.getValue().clone());
		return clone;
	}

	public void clear(){
		data = null;
		leaf = false;
		children.clear();
	}

	public void setLeaf(){
		leaf = true;
	}

	public TrieNode<T> getChild(Character stem){
		return children.get(stem);
	}

	public void addChild(Character stem, TrieNode<T> nextNode){
		children.put(stem, nextNode);
	}

	public TrieNode<T> removeChild(Character stem){
		return children.remove(stem);
	}

	public void forEachChild(Consumer<TrieNode<T>> callback){
		children.forEach((key, value) -> callback.accept(value));
	}

}
