package unit731.hunspeller.collections.trie;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Getter;


public class TrieNode<T> implements Cloneable{

	@Getter private T value;
	@Getter private boolean leaf;
	private Map<Integer, TrieNode<T>> children;


	@Override
	public TrieNode<T> clone(){
		TrieNode<T> clone = new TrieNode<>();
		clone.value = value;
		clone.leaf = leaf;
		if(children != null){
			clone.children = children.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()));
		}
		return clone;
	}

	public void clear(){
		value = null;
		leaf = false;
		if(children != null)
			children.clear();
	}

	public void setValue(T value){
		this.value = value;
		setLeaf();
	}

	public void setLeaf(){
		leaf = true;
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
		TrieNode<T> removedNode = null;
		if(children != null){
			TrieNode<T> node = children.get(stem);
			//when there are no children, remove this node from it's parent
			if(node.children == null || node.children.isEmpty())
				removedNode = children.remove(stem);
		}
		return removedNode;
	}

	public void forEachChild(Consumer<TrieNode<T>> callback){
		if(children != null)
			children.values()
				.forEach(callback::accept);
	}

	public boolean isEmpty(){
		return (value == null && !leaf && (children == null || children.isEmpty()));
	}

}
