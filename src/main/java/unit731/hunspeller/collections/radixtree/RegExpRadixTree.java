package unit731.hunspeller.collections.radixtree;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
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
		return containsKeyPrefixedBy(RegExpSequencer.splitSequence(keyToCheck));
	}

	public V get(String keyToCheck){
		return getPrefixedBy(RegExpSequencer.splitSequence(keyToCheck));
	}

	public RadixTreeNode<String[], V> findPrefixedBy(String keyToCheck){
		return findPrefixedBy(RegExpSequencer.splitSequence(keyToCheck));
	}

	public List<Map.Entry<String[], V>> getEntriesPrefixedBy(String prefix){
		List<VisitElement<String[], V>> entries = getEntriesPrefixedBy(RegExpSequencer.splitSequence(prefix));
		return entries.stream()
			.map(entry -> new AbstractMap.SimpleEntry<>(entry.getPrefix(), entry.getNode().getValue()))
			.collect(Collectors.toList());
	}

	public List<V> getValuesPrefixedBy(String prefix){
		return getValuesPrefixedBy(RegExpSequencer.splitSequence(prefix));
	}

	public List<String[]> getKeysPrefixedBy(String prefix){
		return getKeysPrefixedBy(RegExpSequencer.splitSequence(prefix));
	}

	public RadixTreeNode<String[], V> find(String keyToCheck){
		return findPrefixedTo(RegExpSequencer.splitSequence(keyToCheck));
	}

	public List<VisitElement<String[], V>> getEntries(String prefix){
		return getEntriesPrefixedTo(RegExpSequencer.splitSequence(prefix));
	}

	public List<V> getValues(String prefix){
		return getValuesPrefixedTo(RegExpSequencer.splitSequence(prefix));
	}

	public List<String[]> getKeys(String prefix){
		return getKeysPrefixedTo(RegExpSequencer.splitSequence(prefix));
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
		return removePrefixedBy(RegExpSequencer.splitSequence(key));
	}

}
