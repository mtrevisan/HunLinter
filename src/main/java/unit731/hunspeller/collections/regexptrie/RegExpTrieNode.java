package unit731.hunspeller.collections.regexptrie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.NoArgsConstructor;


@NoArgsConstructor
public class RegExpTrieNode<T>{

	@Getter private final List<T> data = new ArrayList<>();
	@Getter private boolean leaf;
	private final Map<String, RegExpTrieNode<T>> children = new HashMap<>();


	@Override
	public RegExpTrieNode<T> clone(){
		RegExpTrieNode<T> clone = new RegExpTrieNode<>();
		clone.data.addAll(data);
		clone.leaf = leaf;
		children.forEach((key, value) -> clone.children.put(key, value.clone()));
		return clone;
	}

	public void clear(){
		data.clear();
		leaf = false;
		children.clear();
	}

	public void setLeaf(){
		leaf = true;
	}

	public RegExpTrieNode<T> getChild(String pattern){
		return children.get(pattern);
	}

	public List<RegExpTrieNode<T>> getChildrenMatching(Character stem){
		List<RegExpTrieNode<T>> result = new ArrayList<>();
		children.entrySet().forEach(child -> {
			String pattern = child.getKey();
			int index = (".".equals(pattern)? 0: pattern.indexOf(stem));
			boolean insert;
			if(pattern.startsWith("[^"))
				insert = (index < 0);
			else if(pattern.startsWith("["))
				insert = (index >= 0);
			else
				insert = (index == 0);
			if(insert)
				result.add(child.getValue());
		});
		return result;
	}

	public void addChild(String pattern, RegExpTrieNode<T> nextNode){
		children.put(pattern, nextNode);
	}

	public RegExpTrieNode<T> removeChild(String pattern){
		return children.remove(pattern);
	}

	public void forEachChild(Consumer<RegExpTrieNode<T>> callback){
		children.forEach((key, value) -> callback.accept(value));
	}

}
