package unit731.hunspeller.collections.radixtree;

import unit731.hunspeller.collections.radixtree.tree.RadixTree;
import unit731.hunspeller.collections.radixtree.tree.RadixTreeNode;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import unit731.hunspeller.collections.radixtree.sequencers.RegExpSequencer;
import unit731.hunspeller.collections.radixtree.sequencers.SequencerInterface;


public class RegExpRadixTree<V extends Serializable> extends RadixTree<String[], V>{

	public static <T extends Serializable> RegExpRadixTree<T> createTree(){
		RegExpRadixTree<T> tree = new RegExpRadixTree<>();
		SequencerInterface<String[]> sequencer = new RegExpSequencer();
		tree.root = RadixTreeNode.createEmptyNode(sequencer.getNullSequence());
		tree.sequencer = sequencer;
		return tree;
	}

	public static <T extends Serializable> RegExpRadixTree<T> createTreeNoDuplicates(){
		RegExpRadixTree<T> tree = new RegExpRadixTree<>();
		SequencerInterface<String[]> sequencer = new RegExpSequencer();
		tree.root = RadixTreeNode.createEmptyNode(sequencer.getNullSequence());
		tree.sequencer = sequencer;
		tree.noDuplicatesAllowed = true;
		return tree;
	}

	public boolean containsKey(String keyToCheck){
		return containsKey(RegExpSequencer.splitSequence(keyToCheck));
	}

	public V get(String keyToCheck){
		return get(RegExpSequencer.splitSequence(keyToCheck));
	}

	public RadixTreeNode<String[], V> find(String keyToCheck){
		return find(RegExpSequencer.splitSequence(keyToCheck));
	}

	public List<Map.Entry<String[], V>> getEntriesWithPrefix(String prefix){
		return getEntriesWithPrefix(RegExpSequencer.splitSequence(prefix));
	}

	public List<V> getValuesWithPrefix(String prefix){
		return getValuesWithPrefix(RegExpSequencer.splitSequence(prefix));
	}

	public List<String[]> getKeysWithPrefix(String prefix){
		return getKeysWithPrefix(RegExpSequencer.splitSequence(prefix));
	}

	public void putAllWithFlatKey(Map<? extends String, ? extends V> map){
		Objects.requireNonNull(map);

		map.entrySet()
			.forEach(entry -> put(entry.getKey(), entry.getValue()));
	}

	public V put(String key, V value){
		return put(RegExpSequencer.splitSequence(key), value);
	}

	public V remove(String key){
		return remove(RegExpSequencer.splitSequence(key));
	}

}
