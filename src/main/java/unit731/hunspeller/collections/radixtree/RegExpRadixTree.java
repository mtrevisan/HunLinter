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

	public boolean containsKeyPrefixedBy(String keyToCheck){
		return containsKeyPrefixedBy(RegExpSequencer.splitSequence(keyToCheck));
	}

	public V getPrefixedBy(String keyToCheck){
		return getPrefixedBy(RegExpSequencer.splitSequence(keyToCheck));
	}

	public RadixTreeNode<String[], V> find(String keyToCheck, PrefixType type){
		return find(RegExpSequencer.splitSequence(keyToCheck), type);
	}

	public List<Map.Entry<String[], V>> getEntriesPrefixed(String prefix, PrefixType type){
		List<VisitElement<String[], V>> entries = getEntries(RegExpSequencer.splitSequence(prefix), type);
		return entries.stream()
			.map(entry -> new AbstractMap.SimpleEntry<>(entry.getPrefix(), entry.getNode().getValue()))
			.collect(Collectors.toList());
	}

	public List<V> getValuesPrefixedBy(String prefix){
		return getValues(RegExpSequencer.splitSequence(prefix), RadixTree.PrefixType.PREFIXED_BY);
	}

//	public List<String[]> getKeysPrefixedBy(String prefix){
//		return getKeysPrefixedBy(RegExpSequencer.splitSequence(prefix));
//	}

//	public RadixTreeNode<String[], V> findPrefixedTo(String keyToCheck){
//		return findPrefixedTo(RegExpSequencer.splitSequence(keyToCheck));
//	}

	public List<VisitElement<String[], V>> getEntriesPrefixedTo(String prefix, PrefixType type){
		return getEntries(RegExpSequencer.splitSequence(prefix), type);
	}

	public List<V> getValuesPrefixedTo(String prefix){
		return getValues(RegExpSequencer.splitSequence(prefix), RadixTree.PrefixType.PREFIXED_TO);
	}

//	public List<String[]> getKeysPrefixedTo(String prefix){
//		return getKeysPrefixedTo(RegExpSequencer.splitSequence(prefix));
//	}

	public void putAllWithFlatKey(Map<? extends String, ? extends V> map){
		Objects.requireNonNull(map);

		for(Map.Entry<? extends String, ? extends V> entry : map.entrySet())
			put(entry.getKey(), entry.getValue());
	}

	public V put(String key, V value){
		return put(RegExpSequencer.splitSequence(key), value);
	}

	public V removePrefixedBy(String key){
		return remove(RegExpSequencer.splitSequence(key));
	}

}
