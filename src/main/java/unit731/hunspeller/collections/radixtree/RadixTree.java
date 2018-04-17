package unit731.hunspeller.collections.radixtree;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.collections.radixtree.sequencers.SequencerInterface;


/**
 * A radix tree.
 * Radix trees are String to Object mappings which allow quick lookups on the strings. Radix trees also make it easy to grab the objects
 * with a common prefix.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Radix_tree">Wikipedia</a>
 * @see <a href="https://github.com/thegedge/radix-tree">Radix Tree 1</a>
 * @see <a href="https://github.com/oroszgy/radixtree">Radix Tree 2</a>
 *
 * @param <V> the type of values stored in the tree
 */
@Getter
public class RadixTree<V extends Serializable> implements Map<String, V>, Serializable{

	@AllArgsConstructor
	private class VisitElement{
		private final RadixTreeNode<V> node;
		private final RadixTreeNode<V> parent;
		private final String prefixAllowed;
		private final String prefix;
	}


	/** The root node in this tree */
	private final RadixTreeNode<V> root = RadixTreeNode.createEmptyNode();
	private boolean noDuplicatesAllowed;
	private SequencerInterface sequencer;


	public static <T extends Serializable> RadixTree<T> createTree(SequencerInterface sequencer){
		RadixTree<T> tree = new RadixTree<>();
		tree.sequencer = sequencer;
		return tree;
	}

	public static <T extends Serializable> RadixTree<T> createTreeNoDuplicates(SequencerInterface sequencer){
		RadixTree<T> tree = new RadixTree<>();
		tree.noDuplicatesAllowed = true;
		tree.sequencer = sequencer;
		return tree;
	}

	@Override
	public void clear(){
		root.getChildren().clear();
	}

	@Override
	public boolean containsKey(Object keyToCheck){
		validateKey(keyToCheck);

		RadixTreeNode<V> foundNode = find((String)keyToCheck);
		return (foundNode != null);
	}

	@Override
	public boolean containsValue(Object value){
		Objects.requireNonNull(value);

		RadixTreeVisitor<V, Boolean> visitor = new RadixTreeVisitor<V, Boolean>(false){
			@Override
			public boolean visit(String key, RadixTreeNode<V> node, RadixTreeNode<V> parent){
				V v = node.getValue();
				result = (v == value || v.equals(value));

				return result;
			}
		};
		visit(visitor);

		return visitor.getResult();
	}

	@Override
	public V get(Object keyToCheck){
		validateKey(keyToCheck);

		RadixTreeNode<V> foundNode = find((String)keyToCheck);
		return (foundNode != null? foundNode.getValue(): null);
	}

	public RadixTreeNode<V> find(String keyToCheck){
		Objects.requireNonNull(keyToCheck);

		RadixTreeVisitor<V, RadixTreeNode<V>> visitor = new RadixTreeVisitor<V, RadixTreeNode<V>>(null){
			@Override
			public boolean visit(String key, RadixTreeNode<V> node, RadixTreeNode<V> parent){
				if(sequencer.equals(key, keyToCheck))
					result = node;

				return (result != null);
			}
		};
		visit(visitor, keyToCheck);

		return visitor.getResult();
	}

	/**
	 * Gets a list of entries whose associated keys have the given prefix.
	 *
	 * @param prefix	The prefix to look for
	 * @return	The list of values
	 * @throws NullPointerException	If prefix is <code>null</code>
	 */
	public List<Map.Entry<String, V>> getEntriesWithPrefix(String prefix){
		Objects.requireNonNull(prefix);

		RadixTreeVisitor<V, List<Map.Entry<String, V>>> visitor = new RadixTreeVisitor<V, List<Map.Entry<String, V>>>(new ArrayList<>()){
			@Override
			public boolean visit(String key, RadixTreeNode<V> node, RadixTreeNode<V> parent){
				V value = node.getValue();
				Map.Entry<String, V> entry = new AbstractMap.SimpleEntry<>(key, value);
				result.add(entry);

				return false;
			}
		};
		visit(visitor, prefix);

		return visitor.getResult();
	}

	/**
	 * Gets a list of values whose associated keys have the given prefix, or are contained into the prefix.
	 *
	 * @param prefix	The prefix to look for
	 * @return	The list of values
	 * @throws NullPointerException	If prefix is <code>null</code>
	 */
	public List<V> getValuesWithPrefix(String prefix){
		List<Map.Entry<String, V>> entries = getEntriesWithPrefix(prefix);
		return entries.stream()
			.map(Map.Entry::getValue)
			.collect(Collectors.toList());
	}

	/**
	 * Gets a list of keys with the given prefix.
	 *
	 * @param prefix	The prefix to look for
	 * @return	The list of prefixes
	 * @throws NullPointerException	If prefix is <code>null</code>
	 */
	public List<String> getKeysWithPrefix(String prefix){
		List<Map.Entry<String, V>> entries = getEntriesWithPrefix(prefix);
		return entries.stream()
			.map(Map.Entry::getKey)
			.collect(Collectors.toList());
	}

	@Override
	public boolean isEmpty(){
		return root.getChildren().isEmpty();
	}

	@Override
	public int size(){
		RadixTreeVisitor<V, Integer> visitor = new RadixTreeVisitor<V, Integer>(0){
			@Override
			public boolean visit(String key, RadixTreeNode<V> node, RadixTreeNode<V> parent){
				result ++;

				return false;
			}
		};
		visit(visitor);

		return visitor.getResult();
	}

	@Override
	public Set<Map.Entry<String, V>> entrySet(){
		List<Map.Entry<String, V>> entries = getEntriesWithPrefix(StringUtils.EMPTY);
		return new HashSet<>(entries);
	}

	@Override
	public Set<String> keySet(){
		List<Map.Entry<String, V>> entries = getEntriesWithPrefix(StringUtils.EMPTY);
		return entries.stream()
			.map(Map.Entry::getKey)
			.collect(Collectors.toSet());
	}

	@Override
	public Collection<V> values(){
		List<Map.Entry<String, V>> entries = getEntriesWithPrefix(StringUtils.EMPTY);
		return entries.stream()
			.map(Map.Entry::getValue)
			.collect(Collectors.toSet());
	}

	@Override
	public void putAll(Map<? extends String, ? extends V> map){
		Objects.requireNonNull(map);

		map.entrySet()
			.forEach(entry -> put(entry.getKey(), entry.getValue()));
	}

	@Override
	public V put(String key, V value){
		Objects.requireNonNull(key);
		Objects.requireNonNull(value);

		try{
			return put(key, value, root);
		}
		catch(DuplicateKeyException e){
			throw new DuplicateKeyException("Duplicate key: '" + key + "'");
		}
	}

	/**
	 * Insert the value with the given key from the subtree rooted at the given node.
	 *
	 * @param key	The key
	 * @param value	The value
	 * @param node	The node to start searching from
	 * @return	The old value associated with the given key, or <code>null</code> if there was no mapping for <code>key</code>
	 */
	private V put(String key, V value, RadixTreeNode<V> node){
		V ret = null;

		String nodeKey = node.getKey();
		int largestPrefix = longestCommonPrefixLength(key, nodeKey);
		int keyLength = sequencer.length(key);
		int nodeKeyLength = sequencer.length(nodeKey);
		if(largestPrefix == nodeKeyLength && largestPrefix == keyLength){
			//found a node with an exact match
			ret = node.getValue();
			if(noDuplicatesAllowed && ret != null)
				throw new DuplicateKeyException();

			node.setValue(value);
		}
		else if(largestPrefix == 0 || largestPrefix < keyLength && largestPrefix >= nodeKeyLength){
			//key is bigger than the prefix located at this node, so we need to see if there's a child that can possibly share a prefix, and if not, we just add
			//a new node to this node
			String leftoverKey = sequencer.subSequence(key, largestPrefix);

			boolean found = false;
			for(RadixTreeNode<V> child : node)
				if(sequencer.equalsAtIndex(child.getKey(), leftoverKey, 0)){
					found = true;
					ret = put(leftoverKey, value, child);
					break;
				}

			if(!found){
				//no child exists with any prefix of the given key, so add a new one
				RadixTreeNode<V> n = new RadixTreeNode<>(leftoverKey, value);
				node.getChildren().add(n);
			}
		}
		else if(largestPrefix < nodeKeyLength){
			//key and node.getPrefix() share a prefix, so split node
			String leftoverPrefix = sequencer.subSequence(nodeKey, largestPrefix);
			RadixTreeNode<V> n = new RadixTreeNode<>(leftoverPrefix, node.getValue());
			n.getChildren().addAll(node.getChildren());

			node.setKey(sequencer.subSequence(nodeKey, 0, largestPrefix));
			node.getChildren().clear();
			node.getChildren().add(n);

			if(largestPrefix == keyLength){
				//the largest prefix is equal to the key, so set this node's value
				ret = node.getValue();
				node.setValue(value);
			}
			else{
				//there's a leftover suffix on the key, so add another child 
				String leftoverKey = sequencer.subSequence(key, largestPrefix);
				RadixTreeNode<V> keyNode = new RadixTreeNode<>(leftoverKey, value);
				node.getChildren().add(keyNode);
				node.setValue(null);
			}
		}
		else{
			//node.getPrefix() is a prefix of key, so add as child
			String leftoverKey = sequencer.subSequence(key, largestPrefix);
			RadixTreeNode<V> n = new RadixTreeNode<>(leftoverKey, value);
			node.getChildren().add(n);
		}

		return ret;
	}

	@Override
	public V remove(Object key){
		validateKey(key);

		RadixTreeVisitor<V, V> visitor = new RadixTreeVisitor<V, V>(null){
			@Override
			public boolean visit(String k, RadixTreeNode<V> node, RadixTreeNode<V> parent){
				if(sequencer.equals(k, (String)key)){
					result = node.getValue();
					Collection<RadixTreeNode<V>> children = node.getChildren();

					//if there is no children of the node we need to delete it from the its parent children list
					if(children.isEmpty()){
						String key = node.getKey();
						Collection<RadixTreeNode<V>> parentChildren = parent.getChildren();
						Iterator<RadixTreeNode<V>> itr = parentChildren.iterator();
						while(itr.hasNext())
							if(sequencer.equals(itr.next().getKey(), key)){
								itr.remove();
								break;
							}

						//if parent is not real node and has only one child then they need to be merged.
						if(parentChildren.size() == 1 && !parent.hasValue() && parent != root)
							mergeNodes(parent, parentChildren.iterator().next());
					}
					else if(children.size() == 1)
						//we need to merge the only child of this node with itself
						mergeNodes(node, children.iterator().next());
					else
						node.setValue(null);
				}

				return (result != null);
			}

			/**
			 * Merge a child into its parent node.
			 * The operation is valid only if it is a child of the parent node and the parent node is not a real node.
			 *
			 * @param parent	The parent Node
			 * @param child	The child Node
			 */
			private void mergeNodes(RadixTreeNode<V> parent, RadixTreeNode<V> child){
				parent.setKey(sequencer.concat(parent.getKey(), child.getKey()));
				parent.setValue(child.getValue());
				parent.getChildren().clear();
			}
		};

		visit(visitor, (String)key);

		return visitor.getResult();
	}

	/**
	 * Traverses this radix tree using the given visitor.
	 * Note that the tree will be traversed in lexicographical order.
	 *
	 * @param visitor	The visitor
	 */
	public void visit(RadixTreeVisitor<V, ?> visitor){
		visit(visitor, StringUtils.EMPTY);
	}

	/**
	 * Traverses this radix tree using the given visitor and starting at the given prefix.
	 * Note that the tree will be traversed in lexicographical order.
	 *
	 * @param visitor	The visitor
	 * @param prefix	The prefix used to restrict visitation
	 */
	public void visit(RadixTreeVisitor<V, ?> visitor, String prefix){
		Objects.requireNonNull(visitor);
		Objects.requireNonNull(prefix);

		Stack<VisitElement> stack = new Stack<>();
		stack.push(new VisitElement(root, null, prefix, root.getKey()));
		while(!stack.isEmpty()){
			VisitElement elem = stack.pop();
			RadixTreeNode<V> node = elem.node;
			String prefixAllowed = elem.prefixAllowed;
			prefix = elem.prefix;

			if(node.hasValue() && (sequencer.startsWith(prefix, prefixAllowed) || sequencer.startsWith(prefixAllowed, prefix))){
				boolean exitValue = visitor.visit(prefix, node, elem.parent);
				if(exitValue)
					break;
			}

			int prefixLen = sequencer.length(prefix);
			for(RadixTreeNode<V> child : node){
				String newPrefix = sequencer.concat(prefix, child.getKey());
				if(prefixLen >= sequencer.length(prefixAllowed) || prefixLen >= newPrefix.length() || sequencer.equalsAtIndex(newPrefix, prefixAllowed, prefixLen))
					stack.push(new VisitElement(child, node, prefixAllowed, newPrefix));
			}
		}
	}

	/**
	 * Finds the length of the Longest Common Prefix
	 *
	 * @param keyA	Character sequence A
	 * @param keyB	Character sequence B
	 * @return	The length of largest prefix of <code>A</code> and <code>B</code>
	 * @throws IllegalArgumentException	If either <code>A</code> or <code>B</code> is <code>null</code>
	 */
	private int longestCommonPrefixLength(String keyA, String keyB){
		int len = 0;
		int size = Math.min(sequencer.length(keyA), sequencer.length(keyB));
		while(len < size){
			if(!sequencer.equalsAtIndex(keyA, keyB, len))
				break;

			len ++;
		}
		return len;
	}

	private void validateKey(Object key){
		Objects.requireNonNull(key);
		if(!(key instanceof String))
			throw new IllegalArgumentException("key must be a String");
	}

}
