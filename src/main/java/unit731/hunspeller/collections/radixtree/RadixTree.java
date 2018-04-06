package unit731.hunspeller.collections.radixtree;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;


/**
 * A radix tree. Radix trees are String to Object mappings which allow quick lookups on the strings. Radix trees also make it easy to grab the objects
 * with a common prefix.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Radix_tree">Wikipedia</a>
 * @see <a href="https://github.com/thegedge/radix-tree">Radix Tree</a>
 *
 * @param <V> the type of values stored in the tree
 */
@NoArgsConstructor
public class RadixTree<V extends Serializable> implements Map<String, V>, Serializable{

	public static final String PREFIX_CANNOT_BE_NULL = "prefix cannot be null";
	public static final String KEY_CANNOT_BE_NULL = "key cannot be null";
	public static final String KEYS_MUST_BE_STRING_INSTANCES = "keys must be String instances";


	/**
	 * The root node in this tree
	 */
	@Getter
	private final RadixTreeNode<V> root = new RadixTreeNode<>(StringUtils.EMPTY);


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
	 * Traverses this radix tree using the given visitor.
	 * Only values with the given prefix will be visited. Note that the tree will be traversed in lexicographical order.
	 *
	 * @param prefix	The prefix used to restrict visitation
	 * @param visitor	The visitor
	 */
	public void visit(RadixTreeVisitor<V, ?> visitor, String prefix){
		visit(root, prefix, StringUtils.EMPTY, visitor);
	}

	/**
	 * Visits the given node of this tree with the given prefix and visitor. Also, recursively visits the left/right subtrees of this node.
	 *
	 * @param node	The node
	 * @param prefix	The prefix
	 * @param visitor	The visitor
	 */
	private boolean visit(RadixTreeNode<V> node, String prefixAllowed, String prefix, RadixTreeVisitor<V, ?> visitor){
		boolean exitValue = false;
		if(node.hasValue() && (prefix.startsWith(prefixAllowed) || prefixAllowed.startsWith(prefix)))
			exitValue = visitor.visit(prefix, node.getValue());

		if(!exitValue){
			int prefixLen = prefix.length();
			for(RadixTreeNode<V> child : node){
				String newPrefix = prefix + child.getKey();
				if(prefixLen >= prefixAllowed.length() || prefixLen >= newPrefix.length() || newPrefix.charAt(prefixLen) == prefixAllowed.charAt(prefixLen)){
					exitValue = visit(child, prefixAllowed, newPrefix, visitor);

					if(exitValue)
						break;
				}
			}
		}
		return exitValue;
	}

	@Override
	public void clear(){
		root.getChildren().clear();
	}

	@Override
	public boolean containsKey(Object keyToCheck){
		if(keyToCheck == null)
			throw new NullPointerException(KEY_CANNOT_BE_NULL);
		if(!(keyToCheck instanceof String))
			throw new ClassCastException(KEYS_MUST_BE_STRING_INSTANCES);

		RadixTreeVisitor<V, Boolean> visitor = new RadixTreeVisitor<V, Boolean>(false){
			@Override
			public boolean visit(String key, V value){
				if(key.equals(keyToCheck))
					result = true;

				return result;
			}
		};
		visit(visitor, (String)keyToCheck);

		return visitor.getResult();
	}

	@Override
	public boolean containsValue(Object val){
		RadixTreeVisitor<V, Boolean> visitor = new RadixTreeVisitor<V, Boolean>(false){
			@Override
			public boolean visit(String key, V value){
				if(val == value || (value != null && value.equals(val)))
					result = true;

				return result;
			}
		};
		visit(visitor);

		return visitor.getResult();
	}

	@Override
	public V get(Object keyToCheck){
		if(keyToCheck == null)
			throw new NullPointerException(KEY_CANNOT_BE_NULL);
		if(!(keyToCheck instanceof String))
			throw new ClassCastException(KEYS_MUST_BE_STRING_INSTANCES);

		RadixTreeVisitor<V, V> visitor = new RadixTreeVisitor<V, V>(null){
			@Override
			public boolean visit(String key, V value){
				if(key.equals(keyToCheck))
					result = value;

				return false;
			}
		};
		visit(visitor, (String)keyToCheck);

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
		RadixTreeVisitor<V, List<Map.Entry<String, V>>> visitor = new RadixTreeVisitor<V, List<Map.Entry<String, V>>>(new ArrayList<>()){
			@Override
			public boolean visit(String key, V value){
				result.add(new AbstractMap.SimpleEntry<>(key, value));

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
		if(prefix == null)
			throw new NullPointerException(PREFIX_CANNOT_BE_NULL);

		RadixTreeVisitor<V, List<V>> visitor = new RadixTreeVisitor<V, List<V>>(new ArrayList<>()){
			@Override
			public boolean visit(String key, V value){
				result.add(value);

				return false;
			}
		};
		visit(visitor, prefix);

		return visitor.getResult();
	}

	/**
	 * Gets a list of keys with the given prefix.
	 *
	 * @param prefix	The prefix to look for
	 * @return	The list of prefixes
	 * @throws NullPointerException	If prefix is <code>null</code>
	 */
	public List<String> getKeysWithPrefix(String prefix){
		if(prefix == null)
			throw new NullPointerException(PREFIX_CANNOT_BE_NULL);

		RadixTreeVisitor<V, List<String>> visitor = new RadixTreeVisitor<V, List<String>>(new ArrayList<>()){
			@Override
			public boolean visit(String key, V value){
				result.add(key);

				return false;
			}
		};
		visit(visitor, prefix);

		return visitor.getResult();
	}

	@Override
	public boolean isEmpty(){
		return root.getChildren().isEmpty();
	}

	@Override
	public void putAll(Map<? extends String, ? extends V> map){
		map.entrySet()
			.forEach(entry -> put(entry.getKey(), entry.getValue()));
	}

	@Override
	public int size(){
		RadixTreeVisitor<V, Integer> visitor = new RadixTreeVisitor<V, Integer>(0){
			@Override
			public boolean visit(String key, V value){
				result ++;

				return false;
			}
		};
		visit(visitor);

		return visitor.getResult();
	}

	@Override
	public Set<Map.Entry<String, V>> entrySet(){
		//TODO documentation of Map.entrySet() specifies that this is a view of the entries, and modifications to this collection should be reflected in the parent structure
		RadixTreeVisitor<V, Set<Map.Entry<String, V>>> visitor = new RadixTreeVisitor<V, Set<Map.Entry<String, V>>>(new HashSet<>()){
			@Override
			public boolean visit(String key, V value){
				result.add(new AbstractMap.SimpleEntry<>(key, value));

				return false;
			}
		};
		visit(visitor);

		return visitor.getResult();
	}

	@Override
	public Set<String> keySet(){
		//TODO documentation of Map.keySet() specifies that this is a view of the keys, and modifications to this collection should be reflected in the parent structure
		RadixTreeVisitor<V, Set<String>> visitor = new RadixTreeVisitor<V, Set<String>>(new TreeSet<>()){
			@Override
			public boolean visit(String key, V value){
				result.add(key);

				return false;
			}
		};
		visit(visitor);

		return visitor.getResult();
	}

	@Override
	public Collection<V> values(){
		//TODO documentation of Map.values() specifies that this is a view of the values, and modifications to this collection should be reflected in the parent structure
		RadixTreeVisitor<V, Collection<V>> visitor = new RadixTreeVisitor<V, Collection<V>>(new ArrayList<>()){
			@Override
			public boolean visit(String key, V value){
				result.add(value);

				return false;
			}
		};
		visit(visitor);

		return visitor.getResult();
	}

	@Override
	public V put(String key, V value){
		if(key == null)
			throw new NullPointerException(KEY_CANNOT_BE_NULL);

		return put(key, value, root);
	}

	/**
	 * Remove the value with the given key from the subtree rooted at the given node.
	 *
	 * @param key	The key
	 * @param node	The node to start searching from
	 * @return	The old value associated with the given key, or <code>null</code> if there was no mapping for <code>key</code>
	 */
	private V put(String key, V value, RadixTreeNode<V> node){
		V ret = null;

		int largestPrefix = RadixTreeUtil.largestPrefixLength(key, node.getKey());
		if(largestPrefix == node.getKey().length() && largestPrefix == key.length()){
			//found a node with an exact match
			ret = node.getValue();
			node.setValue(value);
		}
		else if(largestPrefix == 0 || (largestPrefix < key.length() && largestPrefix >= node.getKey().length())){
			//key is bigger than the prefix located at this node, so we need to see if there's a child that can possibly share a prefix, and if not, we just add
			//a new node to this node
			String leftoverKey = key.substring(largestPrefix);

			boolean found = false;
			for(RadixTreeNode<V> child : node)
				if(child.getKey().charAt(0) == leftoverKey.charAt(0)){
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
		else if(largestPrefix < node.getKey().length()){
			//kKey and node.getPrefix() share a prefix, so split node
			String leftoverPrefix = node.getKey().substring(largestPrefix);
			RadixTreeNode<V> n = new RadixTreeNode<>(leftoverPrefix, node.getValue());
			n.getChildren().addAll(node.getChildren());

			node.setKey(node.getKey().substring(0, largestPrefix));
			node.getChildren().clear();
			node.getChildren().add(n);

			if(largestPrefix == key.length()){
				//the largest prefix is equal to the key, so set this node's value
				ret = node.getValue();
				node.setValue(value);
			}
			else{
				//there's a leftover suffix on the key, so add another child 
				String leftoverKey = key.substring(largestPrefix);
				RadixTreeNode<V> keyNode = new RadixTreeNode<>(leftoverKey, value);
				node.getChildren().add(keyNode);
				node.setValue(null);
			}
		}
		else{
			//node.getPrefix() is a prefix of key, so add as child
			String leftoverKey = key.substring(largestPrefix);
			RadixTreeNode<V> n = new RadixTreeNode<>(leftoverKey, value);
			node.getChildren().add(n);
		}

		return ret;
	}

	@Override
	public V remove(Object key){
		if(key == null)
			throw new NullPointerException(KEY_CANNOT_BE_NULL);
		if(!(key instanceof String))
			throw new ClassCastException(KEYS_MUST_BE_STRING_INSTANCES);

		//special case for removing empty string (root node)
		String sKey = (String)key;
		if(sKey.isEmpty()){
			V value = root.getValue();
			root.setValue(null);
			return value;
		}

		return remove(sKey, root);
	}

	/**
	 * Remove the value with the given key from the subtree rooted at the given node.
	 *
	 * @param key	The key
	 * @param node	The node to start searching from
	 * @return	The value associated with the given key, or <code>null</code> if there was no mapping for <code>key</code>
	 */
	private V remove(String key, RadixTreeNode<V> node){
		V ret = null;
		Iterator<RadixTreeNode<V>> iter = node.getChildren().iterator();
		while(iter.hasNext()){
			RadixTreeNode<V> child = iter.next();
			int largestPrefix = RadixTreeUtil.largestPrefixLength(key, child.getKey());
			if(largestPrefix == child.getKey().length() && largestPrefix == key.length()){
				//found our match, remove the value from this node
				if(child.getChildren().isEmpty()){
					//leaf node, simply remove from parent
					ret = child.getValue();
					iter.remove();
					break;
				}
				else if(child.hasValue()){
					//internal node
					ret = child.getValue();
					child.setValue(null);

					if(child.getChildren().size() == 1){
						//the subchild's prefix can be reused, with a little modification
						RadixTreeNode<V> subchild = child.getChildren().iterator().next();
						String newPrefix = child.getKey() + subchild.getKey();

						//merge child node with its single child
						child.setValue(subchild.getValue());
						child.setKey(newPrefix);
						child.getChildren().clear();
					}

					break;
				}
			}
			else if(largestPrefix > 0 && largestPrefix < key.length()){
				//continue down subtree of child
				String leftoverKey = key.substring(largestPrefix);
				ret = remove(leftoverKey, child);
				break;
			}
		}

		return ret;
	}

}
