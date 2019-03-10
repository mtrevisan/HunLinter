package unit731.hunspeller.collections.radixtree;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import unit731.hunspeller.collections.radixtree.sequencers.RegExpSequencer;
import unit731.hunspeller.collections.radixtree.utils.RadixTreeNode;
import unit731.hunspeller.collections.radixtree.dtos.VisitElement;


public class RegExpRadixTree<V extends Serializable> extends RadixTree<String[], V>{

	public static <T extends Serializable> RegExpRadixTree<T> createTree(){
		return new RegExpRadixTree<>(false);
	}

	public static <T extends Serializable> RegExpRadixTree<T> createTreeNoDuplicates(){
		return new RegExpRadixTree<>(true);
	}

	private RegExpRadixTree(boolean noDuplicatesAllowed){
		super(new RegExpSequencer(), noDuplicatesAllowed);
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

	public List<VisitElement<String[], V>> getEntries(String prefix){
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

		for(Map.Entry<? extends String, ? extends V> entry : map.entrySet())
			put(entry.getKey(), entry.getValue());
	}

	public V put(String key, V value){
		return put(RegExpSequencer.splitSequence(key), value);
	}

	public V remove(String key){
		return remove(RegExpSequencer.splitSequence(key));
	}

}
