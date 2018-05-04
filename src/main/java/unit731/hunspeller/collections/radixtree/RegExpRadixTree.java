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
		return new RegExpRadixTree<>();
	}

	public static <T extends Serializable> RegExpRadixTree<T> createTreeNoDuplicates(){
		RegExpRadixTree<T> tree = new RegExpRadixTree<>();
		tree.noDuplicatesAllowed = true;
		return tree;
	}

	private RegExpRadixTree(){
		SequencerInterface<String[]> seq = new RegExpSequencer();

		root = RadixTreeNode.createEmptyNode(seq.getEmptySequence());
		this.sequencer = seq;
	}

	public boolean containsKey(String keyToCheck){
		return containsKey(RegExpSequencer.splitSequence(keyToCheck));
	}

	public V get(String keyToCheck){
		return get(RegExpSequencer.splitSequence(keyToCheck));
	}

	public RadixTreeNode<String[], V> findPrefixedBy(String keyToCheck){
		return findPrefixedBy(RegExpSequencer.splitSequence(keyToCheck));
	}

	public List<Map.Entry<String[], V>> getEntriesPrefixedBy(String prefix){
		return getEntriesPrefixedBy(RegExpSequencer.splitSequence(prefix));
	}

	public List<V> getValuesPrefixedBy(String prefix){
		return getValuesPrefixedBy(RegExpSequencer.splitSequence(prefix));
	}

	public List<String[]> getKeysPrefixedBy(String prefix){
		return getKeysPrefixedBy(RegExpSequencer.splitSequence(prefix));
	}

	public RadixTreeNode<String[], V> find(String keyToCheck){
		return find(RegExpSequencer.splitSequence(keyToCheck));
	}

	public List<Map.Entry<String[], V>> getEntries(String prefix){
		return getEntries(RegExpSequencer.splitSequence(prefix));
	}

	public List<V> getValues(String prefix){
		return getValues(RegExpSequencer.splitSequence(prefix));
	}

	public List<String[]> getKeys(String prefix){
		return getKeys(RegExpSequencer.splitSequence(prefix));
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
