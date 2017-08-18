package unit731.hunspeller.collections.trie;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;


@AllArgsConstructor
@Getter
public class Prefix<T>{

	@NonNull
	private final TrieNode<T> node;
	private final int index;
	@NonNull
	private final TrieNode<T> parent;


	public boolean isLeaf(){
		return (node != null && node.isLeaf());
	}

}
