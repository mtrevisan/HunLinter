package unit731.hunspeller.collections.radixtree.tree;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
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
 * Radix trees are key-value mappings which allow quick lookups on the strings. Radix trees also make it easy to grab the values
 * with a common prefix.
 * If {@link #prepare() prepare} is called, an implementation of the Aho-Corasick string matching algorithm (described in the paper
 * "Efficient String Matching: An Aid to Bibliographic Search", written by Alfred V. Aho and Margaret J. Corasick, Bell Laboratories, 1975)
 * is created, allowing for fast dictionary matching.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Radix_tree">Wikipedia</a>
 * @see <a href="https://github.com/thegedge/radix-tree">Radix Tree 1</a>
 * @see <a href="https://github.com/oroszgy/radixtree">Radix Tree 2</a>
 * @see <a href="https://en.wikipedia.org/wiki/Aho%E2%80%93Corasick_algorithm">Aho-Corasick algorithm</a>
 * @see <a href="http://www.cs.uku.fi/~kilpelai/BSA05/lectures/slides04.pdf">Biosequence Algorithms, Spring 2005 - Lecture 4: Set Matching and Aho-Corasick Algorithm</a>
 * @see <a href="http://docplayer.net/storage/63/50019668/1524237578/_U5NLg4tdVIt5CYrnzIXoA/50019668.pdf">Aho-Corasik Algorithm in Pattern Matching</a>
 *
 * @param <S>	The sequence/key type
 * @param <V>	The type of values stored in the tree
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class RadixTree<S, V extends Serializable> implements Map<S, V>, Serializable{

	private static final long serialVersionUID = -5213027224293608217L;


	@AllArgsConstructor
	private class TraverseElement{

		protected final RadixTreeNode<S, V> node;
		protected final S prefix;

	}

	private class VisitElement extends TraverseElement{

		private final RadixTreeNode<S, V> parent;


		VisitElement(RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent, S prefix){
			super(node, prefix);

			this.parent = parent;
		}

	}


	/** The root node in this tree */
	protected RadixTreeNode<S, V> root;
	protected SequencerInterface<S> sequencer;
	protected boolean noDuplicatesAllowed;

	private boolean prepared;
	private boolean putAll;


	public static <K, T extends Serializable> RadixTree<K, T> createTree(SequencerInterface<K> sequencer){
		RadixTree<K, T> tree = new RadixTree<>();
		tree.root = RadixTreeNode.createEmptyNode(sequencer.getEmptySequence());
		tree.sequencer = sequencer;
		return tree;
	}

	public static <K, T extends Serializable> RadixTree<K, T> createTreeNoDuplicates(SequencerInterface<K> sequencer){
		RadixTree<K, T> tree = new RadixTree<>();
		tree.root = RadixTreeNode.createEmptyNode(sequencer.getEmptySequence());
		tree.sequencer = sequencer;
		tree.noDuplicatesAllowed = true;
		return tree;
	}

	/** Initializes the fail transitions of all nodes (except for the root). */
	public void prepare(){
		//FIXME trasform this tree into a trie

		//process children of the root
		root.getChildren()
			.forEach(child -> child.setFailNode(root));

		RadixTreeTraverser<S, V> traverser = new RadixTreeTraverser<S, V>(){
			@Override
			public void traverse(S wholeKey, RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent){
				if(parent == root)
					return;

				S currentKey = node.getKey();
				int keySize = sequencer.length(currentKey);
				for(int i = 1; i <= keySize; i ++){
					S subkey = sequencer.subSequence(currentKey, 0, i);

					//FIXME should consider one character at a time and split in case

					//find the deepest node labeled by a proper suffix of the current child
					RadixTreeNode<S, V> fail = parent.getFailNode();
					while(fail != root && transit(fail, subkey) == null)
						fail = fail.getFailNode();

					RadixTreeNode<S, V> state = transit(fail, subkey);
					int lcpLength = longestCommonPrefixLength(subkey, state.getKey());
					if(lcpLength > 0){
						if(lcpLength < sequencer.length(state.getKey())){
							//split fail
							int nodeKeyLength = sequencer.length(state.getKey());
							S leftoverPrefix = sequencer.subSequence(state.getKey(), lcpLength, nodeKeyLength);
							RadixTreeNode<S, V> n = new RadixTreeNode<>(leftoverPrefix, state.getValue());
							n.getChildren().addAll(state.getChildren());

							state.setKey(sequencer.subSequence(state.getKey(), 0, lcpLength));
							state.getChildren().clear();
							state.getChildren().add(n);
							state.setValue(null);
						}
						if(lcpLength < sequencer.length(subkey)){
							//split node
							int nodeKeyLength = sequencer.length(subkey);
							S leftoverPrefix = sequencer.subSequence(subkey, lcpLength, nodeKeyLength);
							RadixTreeNode<S, V> n = new RadixTreeNode<>(leftoverPrefix, node.getValue());
							n.getChildren().addAll(node.getChildren());

							node.setKey(sequencer.subSequence(subkey, 0, lcpLength));
							node.getChildren().clear();
							node.getChildren().add(n);
							node.setValue(null);
						}

						//link fail to node
						node.setFailNode(state);

						//TODO
						//out(u) += out(f(u))
					}
				}
			}

			private RadixTreeNode<S, V> transit(RadixTreeNode<S, V> node, S prefix){
				RadixTreeNode<S, V> result = root;
				Iterator<RadixTreeNode<S, V>> itr = node.iterator();
				S seq0 = sequencer.charAt(prefix, 0);
				while(itr.hasNext()){
					RadixTreeNode<S, V> child = itr.next();
					if(sequencer.startsWith(child.getKey(), seq0)){
						result = child;
						break;
					}
				}
				return result;
			}
		};
		traverseBFS(traverser);

		prepared = true;
	}

	@Override
	public void clear(){
		root.getChildren().clear();
	}

	public void clearFailTransitions(){
		RadixTreeTraverser<S, V> traverser = new RadixTreeTraverser<S, V>(){
			@Override
			public void traverse(S wholeKey, RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent){
				node.setFailNode(null);
			}
		};
		traverseBFS(traverser);

		prepared = false;
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
			public boolean visit(S wholeKey, RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent){
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
			public boolean visit(S wholeKey, RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent){
				if(sequencer.equals(wholeKey, keyToCheck))
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
			public boolean visit(S wholeKey, RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent){
				V value = node.getValue();
				Map.Entry<S, V> entry = new AbstractMap.SimpleEntry<>(wholeKey, value);
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
			public boolean visit(S wholeKey, RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent){
				result ++;

				return false;
			}
		};
		visit(visitor);

		return visitor.getResult();
	}

	@Override
	public Set<Map.Entry<S, V>> entrySet(){
		List<Map.Entry<S, V>> entries = getEntriesWithPrefix(sequencer.getEmptySequence());
		return new HashSet<>(entries);
	}

	@Override
	public Set<S> keySet(){
		List<Map.Entry<S, V>> entries = getEntriesWithPrefix(sequencer.getEmptySequence());
		return entries.stream()
			.map(Map.Entry::getKey)
			.collect(Collectors.toSet());
	}

	@Override
	public Collection<V> values(){
		List<Map.Entry<S, V>> entries = getEntriesWithPrefix(sequencer.getEmptySequence());
		return entries.stream()
			.map(Map.Entry::getValue)
			.collect(Collectors.toSet());
	}

	@Override
	public void putAll(Map<? extends S, ? extends V> map){
		Objects.requireNonNull(map);

		putAll = true;
		boolean wasPrepared = prepared;
		if(prepared)
			clearFailTransitions();

		map.entrySet()
			.forEach(entry -> put(entry.getKey(), entry.getValue()));

		putAll = false;
		if(wasPrepared)
			prepare();
	}

	@Override
	public V put(S key, V value){
		Objects.requireNonNull(key);
		Objects.requireNonNull(value);

		boolean wasPrepared = prepared;
		if(!putAll && prepared)
			clearFailTransitions();

		try{
			V previousValue = put(key, value, root);

			if(!putAll && wasPrepared)
				prepare();

			return previousValue;
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
		int lcpLength = longestCommonPrefixLength(key, nodeKey);
		int keyLength = sequencer.length(key);
		int nodeKeyLength = sequencer.length(nodeKey);
		if(lcpLength == nodeKeyLength && lcpLength == keyLength){
			//found a node with an exact match
			ret = node.getValue();
			if(noDuplicatesAllowed && ret != null)
				throw new DuplicateKeyException();

			node.setValue(value);
		}
		else if(lcpLength == 0 || lcpLength < keyLength && lcpLength >= nodeKeyLength){
			//key is bigger than the prefix located at this node, so we need to see if there's a child that can possibly share a prefix, and if not, we just add
			//a new node to this node
			S leftoverKey = sequencer.subSequence(key, lcpLength, keyLength);

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
		else if(lcpLength < nodeKeyLength){
			//key and node.getPrefix() share a prefix, so split node
			S leftoverPrefix = sequencer.subSequence(nodeKey, lcpLength, nodeKeyLength);
			RadixTreeNode<S, V> n = new RadixTreeNode<>(leftoverPrefix, node.getValue());
			n.getChildren().addAll(node.getChildren());

			node.setKey(sequencer.subSequence(nodeKey, 0, lcpLength));
			node.getChildren().clear();
			node.getChildren().add(n);

			if(lcpLength == keyLength){
				//the largest prefix is equal to the key, so set this node's value
				ret = node.getValue();
				node.setValue(value);
			}
			else{
				//there's a leftover suffix on the key, so add another child 
				S leftoverKey = sequencer.subSequence(key, lcpLength, keyLength);
				RadixTreeNode<S, V> keyNode = new RadixTreeNode<>(leftoverKey, value);
				node.getChildren().add(keyNode);
				node.setValue(null);
			}
		}
		else{
			//node.getPrefix() is a prefix of key, so add as child
			S leftoverKey = sequencer.subSequence(key, lcpLength, keyLength);
			RadixTreeNode<S, V> n = new RadixTreeNode<>(leftoverKey, value);
			node.getChildren().add(n);
		}

		return ret;
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

	@Override
	public V remove(Object key){
		Objects.requireNonNull(key);

		RadixTreeVisitor<S, V, V> visitor = new RadixTreeVisitor<S, V, V>(null){
			@Override
			public boolean visit(S wholeKey, RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent){
				if(sequencer.equals(wholeKey, (S)key)){
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
	 * Performa a BFS traversal on the tree, calling the traverser for each node found
	 *
	 * @param traverser	The traverser
	 */
	public void traverseBFS(RadixTreeTraverser<S, V> traverser){
		Queue<TraverseElement> queue = new ArrayDeque<>();
		queue.add(new TraverseElement(root, root.getKey()));
		while(!queue.isEmpty()){
			TraverseElement elem = queue.remove();
			RadixTreeNode<S, V> parent = elem.node;
			S prefix = elem.prefix;

			for(RadixTreeNode<S, V> child : parent.getChildren()){
				S wholeSequence = sequencer.concat(prefix, child.getKey());
				traverser.traverse(wholeSequence, child, parent);

				queue.add(new TraverseElement(child, wholeSequence));
			}
		}
	}

	/**
	 * Traverses this radix tree using the given visitor.
	 * Note that the tree will be traversed in lexicographical order.
	 *
	 * @param visitor	The visitor
	 */
	public void visit(RadixTreeVisitor<S, V, ?> visitor){
		visit(visitor, sequencer.getEmptySequence());
	}

	/**
	 * Traverses this radix tree using the given visitor and starting at the given prefix.
	 * Note that the tree will be traversed in lexicographical order.
	 *
	 * @param visitor	The visitor
	 * @param prefixAllowed	The prefix used to restrict visitation
	 */
	public void visit(RadixTreeVisitor<S, V, ?> visitor, S prefixAllowed){
		Objects.requireNonNull(visitor);
		Objects.requireNonNull(prefixAllowed);

		Stack<VisitElement> stack = new Stack<>();
		stack.push(new VisitElement(root, null, root.getKey()));
		while(!stack.isEmpty()){
			VisitElement elem = stack.pop();
			RadixTreeNode<S, V> node = elem.node;
			S prefix = elem.prefix;

			if(node.hasValue() && (sequencer.startsWith(prefix, prefixAllowed) || sequencer.startsWith(prefixAllowed, prefix))){
				boolean stop = visitor.visit(prefix, node, elem.parent);
				if(stop)
					break;
			}

			int prefixLen = sequencer.length(prefix);
			for(RadixTreeNode<S, V> child : node){
				S newPrefix = sequencer.concat(prefix, child.getKey());
				if(prefixLen >= sequencer.length(prefixAllowed) || prefixLen >= sequencer.length(newPrefix) || sequencer.equalsAtIndex(newPrefix, prefixAllowed, prefixLen))
					stack.push(new VisitElement(child, node, newPrefix));
			}
		}
	}

}
