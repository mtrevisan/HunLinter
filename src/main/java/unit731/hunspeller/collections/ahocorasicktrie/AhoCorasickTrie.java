package unit731.hunspeller.collections.ahocorasicktrie;

import unit731.hunspeller.collections.ahocorasicktrie.dtos.HitProcessor;
import unit731.hunspeller.collections.ahocorasicktrie.dtos.SearchResult;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.collections.ahocorasicktrie.dtos.VisitElement;


/**
 * An implementation of the Aho-Corasick Radix Trie algorithm based on Double-Array
 * 
 * @see <a href="https://github.com/hankcs/AhoCorasickDoubleArrayTrie">Aho-Corasick double-array trie</a>
 * 
 * @param <V>	The type of values stored in the tree
 */
public class AhoCorasickTrie<V extends Serializable> implements Serializable{

	private static final long serialVersionUID = 5044611770728521000L;


	int check[];
	int base[];
	int fail[];
	int[][] output;
	List<V> outerValue;

	int[] keyLength;


	/**
	 * Search text
	 *
	 * @param text	The text
	 * @return	A list of outputs
	 */
	public List<SearchResult<V>> searchInText(CharSequence text){
		int currentState = 0;
		List<SearchResult<V>> collectedEmits = new ArrayList<>();
		for(int i = 0; i < text.length(); i ++){
			currentState = getState(currentState, text.charAt(i));
			storeEmits(i + 1, currentState, collectedEmits);
		}
		return collectedEmits;
	}

	/** Store output */
	private void storeEmits(int position, int currentState, List<SearchResult<V>> collectedEmits){
		int[] hitArray = output[currentState];
		if(hitArray != null)
			for(int hit : hitArray)
				collectedEmits.add(new SearchResult<>(position - keyLength[hit], position, outerValue.get(hit)));
	}

	/**
	 * Search text
	 *
	 * @param text	The text
	 * @param processor	A processor which handles the output
	 */
	public void searchInText(CharSequence text, HitProcessor<V> processor){
		int currentState = 0;
		exit:
		for(int i = 0; i < text.length(); i ++){
			final int position = i + 1;
			currentState = getState(currentState, text.charAt(i));
			int[] hitArray = output[currentState];
			if(hitArray != null)
				for(int hit : hitArray){
					boolean proceed = processor.hit(position - keyLength[hit], position, outerValue.get(hit));
					if(!proceed)
						break exit;
				}
		}
	}

	/**
	 * Checks if the text contains at least one substring
	 *
	 * @param text	Source text to check
	 * @return	<code>true</code> if string contains at least one substring
	 */
	public boolean containsKey(String text){
		if(isInitialized()){
			int currentState = 0;
			for(int i = 0; i < text.length(); i ++){
				currentState = getState(currentState, text.charAt(i));
				if(output[currentState] != null)
					return true;
			}
		}
		return false;
	}

	public boolean hasKey(String key){
		int index = exactMatchSearch(key);
		return (index >= 0);
	}

	public V get(String key){
		int index = exactMatchSearch(key);
		return (outerValue != null && index >= 0? outerValue.get(index): null);
	}

	/**
	 * Update a value corresponding to a key
	 *
	 * @param key	The key
	 * @param value	The value
	 * @return	successful or not（failure if there is no key）
	 */
	public boolean set(String key, V value){
		int index = exactMatchSearch(key);
		if(index >= 0){
			outerValue.set(index, value);
			return true;
		}
		return false;
	}

	/**
	 * NOTE: Supports failure function
	 */
	private int getState(int currentState, char character){
		int newCurrentState = transitionWithRoot(currentState, character);
		//backup to failure nodes
		while(newCurrentState == -1){
			currentState = fail[currentState];
			newCurrentState = transitionWithRoot(currentState, character);
		}
		return newCurrentState;
	}

	/** Transition of a state, if the state is root and it failed, then returns the root */
	private int transitionWithRoot(int nodePosition, char character){
		int b = base[nodePosition];
		int idx = b + character + 1;
		if(b != check[idx])
			return (nodePosition == 0? 0: -1);
		return idx;
	}

	/**
	 * Match exactly by a key
	 *
	 * @param key	The key
	 * @return	The index of the key (you can use it as a perfect hash function)
	 */
	private int exactMatchSearch(String key){
		return exactMatchSearch(key, 0, 0, 0);
	}

	/**
	 * Match exactly by a key
	 *
	 * @param key	The key
	 * @param posistion	The begin index of char array
	 * @param length	The length of the key
	 * @param nodePosition	The starting position of the node for searching
	 * @return	The index of the key (you can use it as a perfect hash function)
	 */
	private int exactMatchSearch(String key, int posistion, int length, int nodePosition){
		int result = -1;
		if(isInitialized()){
			if(length <= 0)
				length = key.length();
			if(nodePosition < 0)
				nodePosition = 0;

			char[] keyChars = key.toCharArray();

			int b = base[nodePosition];
			int p;
			for(int i = posistion; i < length; i ++){
				p = b + (keyChars[i]) + 1;
				if(b == check[p])
					b = base[p];
				else
					return result;
			}

			p = b;
			int n = base[p];
			if(b == check[p] && n < 0)
				result = -n - 1;
		}
		return result;
	}


	/**
	 * Traverses this radix tree using the given visitor.
	 * Note that the tree will be traversed in lexicographical order.
	 *
	 * @param visitor	The visitor
	 */
	public void visit(Function<VisitElement<V>, Boolean> visitor){
		visit(visitor, StringUtils.EMPTY);
	}

	/**
	 * Traverses this radix tree using the given visitor and starting at the given prefix.
	 * Note that the tree will be traversed in lexicographical order.
	 *
	 * @param visitor	The visitor
	 * @param prefix	The prefix used to restrict visitation
	 * @throws NullPointerException	If the given visitor or prefix allowed is <code>null</code>
	 */
	private void visit(Function<VisitElement<V>, Boolean> visitor, String prefix){
		Objects.requireNonNull(visitor);
		Objects.requireNonNull(prefix);

		BiFunction<String, String, Boolean> condition = (pre, preAllowed) -> sequencer.startsWith(pre, preAllowed);

		int prefixAllowedLength = sequencer.length(prefix);

		Stack<VisitElement<V>> stack = new Stack<>();
		stack.push(new VisitElement<>(root, null, root.getKey()));
		while(!stack.isEmpty()){
			VisitElement<V> elem = stack.pop();

			if(elem.getNode().hasValue() && condition.apply(elem.getPrefix(), prefix) && visitor.apply(elem))
				break;

			addNodesToStack(elem, prefixAllowedLength, prefix, stack);
		}
	}

	private void addNodesToStack(VisitElement<V> elem, int prefixAllowedLength, String prefix, Stack<VisitElement< V>> stack){
		Collection<RadixTrieNode> children = elem.getNode().getChildren();
		if(children != null){
			int prefixLen = sequencer.length(elem.getPrefix());
			for(RadixTrieNode child : children)
				if(prefixLen >= prefixAllowedLength || sequencer.equalsAtIndex(child.getKey(), prefix, 0, prefixLen)){
					String newPrefix = sequencer.concat(elem.getPrefix(), child.getKey());
					stack.push(new VisitElement<>(child, elem.getNode(), newPrefix));
				}
		}
	}

	public Set<V> values(){
		return new HashSet<>(outerValue);
	}

	public int size(){
		return (outerValue != null? outerValue.size(): 0);
	}

	public boolean isEmpty(){
		return (size() == 0);
	}

	public boolean isInitialized(){
		return (output != null);
	}

}
