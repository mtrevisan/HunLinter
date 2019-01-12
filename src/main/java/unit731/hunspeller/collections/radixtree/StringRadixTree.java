package unit731.hunspeller.collections.radixtree;

import java.io.Serializable;
import unit731.hunspeller.collections.radixtree.sequencers.StringSequencer;
import unit731.hunspeller.collections.radixtree.tree.RadixTree;


public class StringRadixTree<V extends Serializable> extends RadixTree<String, V>{

	public static <T extends Serializable> StringRadixTree<T> createTree(){
		return new StringRadixTree<>(false);
	}

	public static <T extends Serializable> StringRadixTree<T> createTreeNoDuplicates(){
		return new StringRadixTree<>(true);
	}

	private StringRadixTree(boolean noDuplicatesAllowed){
		super(new StringSequencer(), noDuplicatesAllowed);
	}

}
