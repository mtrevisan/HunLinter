package unit731.hunspeller.collections.radixtree;

import unit731.hunspeller.collections.radixtree.tree.RadixTree;
import java.io.Serializable;
import unit731.hunspeller.collections.radixtree.sequencers.SequencerInterface;
import unit731.hunspeller.collections.radixtree.sequencers.StringSequencer;
import unit731.hunspeller.collections.radixtree.tree.RadixTreeNode;


public class StringRadixTree<V extends Serializable> extends RadixTree<String, V>{

	public static <T extends Serializable> StringRadixTree<T> createTree(){
		return new StringRadixTree<>();
	}

	public static <T extends Serializable> StringRadixTree<T> createTreeNoDuplicates(){
		StringRadixTree<T> tree = new StringRadixTree<>();
		tree.noDuplicatesAllowed = true;
		return tree;
	}

	private StringRadixTree(){
		SequencerInterface<String> seq = new StringSequencer();

		root = RadixTreeNode.createEmptyNode(seq.getEmptySequence());
		this.sequencer = seq;
	}

}
