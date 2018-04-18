package unit731.hunspeller.collections.radixtree.tree;

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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
 * @param <S>	The sequence/key type
 * @param <V>	The type of values stored in the tree
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class RadixTree<S, V extends Serializable> implements Map<S, V>, Serializable{

	@AllArgsConstructor
	private class VisitElement{
		private final RadixTreeNode<S, V> node;
		private final RadixTreeNode<S, V> parent;
		private final S prefixAllowed;
		private final S prefix;
	}


	/** The root node in this tree */
	protected RadixTreeNode<S, V> root;
	protected SequencerInterface<S> sequencer;
	protected boolean noDuplicatesAllowed;


	public static <K, T extends Serializable> RadixTree<K, T> createTree(SequencerInterface<K> sequencer){
		RadixTree<K, T> tree = new RadixTree<>();
		tree.root = RadixTreeNode.createEmptyNode(sequencer.getNullSequence());
		tree.sequencer = sequencer;
		return tree;
	}

	public static <K, T extends Serializable> RadixTree<K, T> createTreeNoDuplicates(SequencerInterface<K> sequencer){
		RadixTree<K, T> tree = new RadixTree<>();
		tree.root = RadixTreeNode.createEmptyNode(sequencer.getNullSequence());
		tree.sequencer = sequencer;
		tree.noDuplicatesAllowed = true;
		return tree;
	}

	@Override
	public void clear(){
		root.getChildren().clear();
	}

	@Override
	public boolean containsKey(Object keyToCheck){
		Objects.requireNonNull(keyToCheck);

		RadixTreeNode<S, V> foundNode = find((S)keyToCheck);
		return (foundNode != null);
	}

	@Override
	public boolean containsValue(Object value){
		Objects.requireNonNull(value);

		RadixTreeVisitor<S, V, Boolean> visitor = new RadixTreeVisitor<S, V, Boolean>(false){
			@Override
			public boolean visit(S key, RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent){
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
		Objects.requireNonNull(keyToCheck);

		RadixTreeNode<S, V> foundNode = find((S)keyToCheck);
		return (foundNode != null? foundNode.getValue(): null);
	}

	public RadixTreeNode<S, V> find(S keyToCheck){
		Objects.requireNonNull(keyToCheck);

		RadixTreeVisitor<S, V, RadixTreeNode<S, V>> visitor = new RadixTreeVisitor<S, V, RadixTreeNode<S, V>>(null){
			@Override
			public boolean visit(S key, RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent){
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
	public List<Map.Entry<S, V>> getEntriesWithPrefix(S prefix){
		Objects.requireNonNull(prefix);

		RadixTreeVisitor<S, V, List<Map.Entry<S, V>>> visitor = new RadixTreeVisitor<S, V, List<Map.Entry<S, V>>>(new ArrayList<>()){
			@Override
			public boolean visit(S key, RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent){
				V value = node.getValue();
				Map.Entry<S, V> entry = new AbstractMap.SimpleEntry<>(key, value);
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
	 * @throws NullPointerException	If the prefix is <code>null</code>
	 */
	public List<V> getValuesWithPrefix(S prefix){
		List<Map.Entry<S, V>> entries = getEntriesWithPrefix(prefix);
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
	public List<S> getKeysWithPrefix(S prefix){
		List<Map.Entry<S, V>> entries = getEntriesWithPrefix(prefix);
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
		RadixTreeVisitor<S, V, Integer> visitor = new RadixTreeVisitor<S, V, Integer>(0){
			@Override
			public boolean visit(S key, RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent){
				result ++;

				return false;
			}
		};
		visit(visitor);

		return visitor.getResult();
	}

	@Override
	public Set<Map.Entry<S, V>> entrySet(){
		List<Map.Entry<S, V>> entries = getEntriesWithPrefix(sequencer.getNullSequence());
		return new HashSet<>(entries);
	}

	@Override
	public Set<S> keySet(){
		List<Map.Entry<S, V>> entries = getEntriesWithPrefix(sequencer.getNullSequence());
		return entries.stream()
			.map(Map.Entry::getKey)
			.collect(Collectors.toSet());
	}

	@Override
	public Collection<V> values(){
		List<Map.Entry<S, V>> entries = getEntriesWithPrefix(sequencer.getNullSequence());
		return entries.stream()
			.map(Map.Entry::getValue)
			.collect(Collectors.toSet());
	}

	@Override
	public void putAll(Map<? extends S, ? extends V> map){
		Objects.requireNonNull(map);

		map.entrySet()
			.forEach(entry -> put(entry.getKey(), entry.getValue()));
	}

	@Override
	public V put(S key, V value){
		Objects.requireNonNull(key);
		Objects.requireNonNull(value);

		try{
			return put(key, value, root);
		}
		catch(DuplicateKeyException e){
			throw new DuplicateKeyException("Duplicate key: '" + sequencer.toString(key) + "'");
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
	private V put(S key, V value, RadixTreeNode<S, V> node){
		V ret = null;

		S nodeKey = node.getKey();
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
			S leftoverKey = sequencer.subSequence(key, largestPrefix);

			boolean found = false;
			for(RadixTreeNode<S, V> child : node)
				if(sequencer.equalsAtIndex(child.getKey(), leftoverKey, 0)){
					found = true;
					ret = put(leftoverKey, value, child);
					break;
				}

			if(!found){
				//no child exists with any prefix of the given key, so add a new one
				RadixTreeNode<S, V> n = new RadixTreeNode<>(leftoverKey, value);
				node.getChildren().add(n);
			}
		}
		else if(largestPrefix < nodeKeyLength){
			//key and node.getPrefix() share a prefix, so split node
			S leftoverPrefix = sequencer.subSequence(nodeKey, largestPrefix);
			RadixTreeNode<S, V> n = new RadixTreeNode<>(leftoverPrefix, node.getValue());
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
				S leftoverKey = sequencer.subSequence(key, largestPrefix);
				RadixTreeNode<S, V> keyNode = new RadixTreeNode<>(leftoverKey, value);
				node.getChildren().add(keyNode);
				node.setValue(null);
			}
		}
		else{
			//node.getPrefix() is a prefix of key, so add as child
			S leftoverKey = sequencer.subSequence(key, largestPrefix);
			RadixTreeNode<S, V> n = new RadixTreeNode<>(leftoverKey, value);
			node.getChildren().add(n);
		}

		return ret;
	}

	@Override
	public V remove(Object key){
		Objects.requireNonNull(key);

		RadixTreeVisitor<S, V, V> visitor = new RadixTreeVisitor<S, V, V>(null){
			@Override
			public boolean visit(S k, RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent){
				if(sequencer.equals(k, (S)key)){
					result = node.getValue();
					Collection<RadixTreeNode<S, V>> children = node.getChildren();

					//if there is no children of the node we need to delete it from the its parent children list
					if(children.isEmpty()){
						S key = node.getKey();
						Collection<RadixTreeNode<S, V>> parentChildren = parent.getChildren();
						Iterator<RadixTreeNode<S, V>> itr = parentChildren.iterator();
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
			private void mergeNodes(RadixTreeNode<S, V> parent, RadixTreeNode<S, V> child){
				parent.setKey(sequencer.concat(parent.getKey(), child.getKey()));
				parent.setValue(child.getValue());
				parent.getChildren().clear();
			}
		};

		visit(visitor, (S)key);

		return visitor.getResult();
	}

	/**
	 * Traverses this radix tree using the given visitor.
	 * Note that the tree will be traversed in lexicographical order.
	 *
	 * @param visitor	The visitor
	 */
	public void visit(RadixTreeVisitor<S, V, ?> visitor){
		visit(visitor, sequencer.getNullSequence());
	}

	/**
	 * Traverses this radix tree using the given visitor and starting at the given prefix.
	 * Note that the tree will be traversed in lexicographical order.
	 *
	 * @param visitor	The visitor
	 * @param prefix	The prefix used to restrict visitation
	 */
	public void visit(RadixTreeVisitor<S, V, ?> visitor, S prefix){
		Objects.requireNonNull(visitor);
		Objects.requireNonNull(prefix);

		Stack<VisitElement> stack = new Stack<>();
		stack.push(new VisitElement(root, null, prefix, root.getKey()));
		while(!stack.isEmpty()){
			VisitElement elem = stack.pop();
			RadixTreeNode<S, V> node = elem.node;
			S prefixAllowed = elem.prefixAllowed;
			prefix = elem.prefix;

			if(node.hasValue() && (sequencer.startsWith(prefix, prefixAllowed) || sequencer.startsWith(prefixAllowed, prefix))){
				boolean stop = visitor.visit(prefix, node, elem.parent);
				if(stop)
					break;
			}

			int prefixLen = sequencer.length(prefix);
			for(RadixTreeNode<S, V> child : node){
				S newPrefix = sequencer.concat(prefix, child.getKey());
				if(prefixLen >= sequencer.length(prefixAllowed) || prefixLen >= sequencer.length(newPrefix) || sequencer.equalsAtIndex(newPrefix, prefixAllowed, prefixLen))
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
	private int longestCommonPrefixLength(S keyA, S keyB){
		int len = 0;
		int size = Math.min(sequencer.length(keyA), sequencer.length(keyB));
		while(len < size){
			if(!sequencer.equalsAtIndex(keyA, keyB, len))
				break;

			len ++;
		}
		return len;
	}

}
