package unit731.hunspeller.collections.regexptrie;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;


@AllArgsConstructor
@Getter
public class RegExpPrefix<T>{

	@NonNull
	private final RegExpTrieNode<T> node;
	private final int index;
	@NonNull
	private final RegExpTrieNode<T> parent;


	public boolean isLeaf(){
		return (node != null && node.isLeaf());
	}

}
