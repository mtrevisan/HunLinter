package unit731.hunspeller.collections.trie;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;


public class TrieNode<T> implements Cloneable{

	@Getter @Setter private T value;
	private Map<Integer, TrieNode<T>> children;


	@Override
	public TrieNode<T> clone(){
		TrieNode<T> clone = new TrieNode<>();
		clone.value = value;
		if(children != null){
			clone.children = children.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()));
		}
		return clone;
	}

	public void clear(){
		value = null;
		if(children != null)
			children.clear();
	}

	public boolean isLeaf(){
		return (value != null);
	}

	public boolean hasChildren(){
		return (children != null && !children.isEmpty());
	}

	public TrieNode<T> getChild(int stem){
		return (children != null? children.get(stem): null);
	}

	public void addChild(int stem, TrieNode<T> node){
		if(children == null)
			children = new HashMap<>();

		children.put(stem, node);
	}

	public TrieNode<T> removeChild(int stem){
		return (children != null? children.remove(stem): null);
	}

	public void forEachChild(Consumer<TrieNode<T>> callback){
		if(children != null)
			children.values()
				.forEach(callback::accept);
	}

	public boolean isEmpty(){
		return (value == null && (children == null || children.isEmpty()));
	}

}
