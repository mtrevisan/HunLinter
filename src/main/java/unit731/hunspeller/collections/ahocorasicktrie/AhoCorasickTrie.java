package unit731.hunspeller.collections.ahocorasicktrie;

import unit731.hunspeller.collections.ahocorasicktrie.dtos.HitProcessor;
import unit731.hunspeller.collections.ahocorasicktrie.dtos.SearchResult;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.collections.ahocorasicktrie.dtos.VisitElement;


/**
 * An implementation of the Aho-Corasick Radix Trie algorithm based on a triple-array data structure
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Aho%E2%80%93Corasick_algorithm">Aho–Corasick algorithm</a>
 * @see <a href="https://github.com/hankcs/AhoCorasickDoubleArrayTrie">Aho-Corasick double-array trie</a>
 * @see <a href="https://pdfs.semanticscholar.org/18b9/da082ef35aea8bf9853ae6b35242539ff7da.pdf">Efficient implementation of Unicode string pattern matching automaton in Java</a>
 * @see <a href="https://www.db.ics.keio.ac.jp/seminar/2011/20111007_ishizaki/20111007_ishizaki_CIAA.pdf">A table compression method for Extended Aho-Corasick Automaton</a>
 * 
 * @param <V>	The type of values stored in the tree
 */
public class AhoCorasickTrie<V extends Serializable> implements Serializable{

	private static final long serialVersionUID = 5044611770728521000L;

	private static final int ROOT_NODE_ID = 0;


	int[] base;
	//failure function
	int[] next;
	int[] check;
	//output function
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
		int currentNodeId = ROOT_NODE_ID;
		List<SearchResult<V>> collectedHits = new ArrayList<>();
		for(int i = 0; i < text.length(); i ++){
			currentNodeId = retrieveNextNodeId(currentNodeId, text.charAt(i));

			storeHit(i + 1, currentNodeId, collectedHits);
		}
		return collectedHits;
	}

	/** Store output */
	private void storeHit(int position, int currentNodeId, List<SearchResult<V>> collectedHits){
		int[] hits = output[currentNodeId];
		if(hits != null)
			for(int hit : hits)
				collectedHits.add(new SearchResult<>(position - keyLength[hit], position, outerValue.get(hit)));
	}

	/**
	 * Search text
	 *
	 * @param text	The text
	 * @param processor	A processor which handles the output
	 */
	public void searchInText(CharSequence text, HitProcessor<V> processor){
		int currentNodeId = ROOT_NODE_ID;
		exit:
		for(int i = 0; i < text.length(); i ++){
			currentNodeId = retrieveNextNodeId(currentNodeId, text.charAt(i));

			int[] hitArray = output[currentNodeId];
			if(hitArray != null){
				final int position = i + 1;
				for(int hit : hitArray){
					final boolean proceed = processor.hit(position - keyLength[hit], position, outerValue.get(hit));
					if(!proceed)
						break exit;
				}
			}
		}
	}

	/**
	 * Checks if the text contains at least one substring
	 *
	 * @param text	Source text to check
	 * @return	<code>true</code> if string contains at least one substring
	 */
	public boolean containsKey(CharSequence text){
		if(isInitialized()){
			int currentNodeId = ROOT_NODE_ID;
			for(int i = 0; i < text.length(); i ++){
				currentNodeId = retrieveNextNodeId(currentNodeId, text.charAt(i));

				if(output[currentNodeId] != null)
					return true;
			}
		}
		return false;
	}

	private int retrieveNextNodeId(int currentNodeId, char character){
		int nextNodeId;
		while((nextNodeId = transitionWithRoot(currentNodeId, character)) == -1)
			currentNodeId = next[currentNodeId];
		return nextNodeId;
	}

	public boolean hasKey(CharSequence key){
		final int id = exactMatchSearch(key);
		return (id >= 0);
	}

	public V get(CharSequence key){
		final int id = exactMatchSearch(key);
		return (outerValue != null && id >= 0? outerValue.get(id): null);
	}

	/**
	 * Update a value corresponding to a key
	 *
	 * @param key	The key
	 * @param value	The value
	 * @return	successful or not（failure if there is no key）
	 */
	public boolean set(CharSequence key, V value){
		final int id = exactMatchSearch(key);
		if(id >= 0){
			outerValue.set(id, value);
			return true;
		}
		return false;
	}

	/** Transition of a node, if the node is root and it failed, then returns the root */
	private int transitionWithRoot(int nodeId, char character){
		final int b = base[nodeId];
		final int idx = b + character + 1;
		if(b != check[idx])
			return (nodeId == ROOT_NODE_ID? nodeId: -1);
		return idx;
	}

	/**
	 * Match exactly by a key
	 *
	 * @param key	The key
	 * @return	The id of the key (you can use it as a perfect hash function)
	 */
	private int exactMatchSearch(CharSequence key){
		return exactMatchSearch(key, 0, 0, ROOT_NODE_ID);
	}

	/**
	 * Match exactly by a key
	 *
	 * @param key	The key
	 * @param position	The begin index of char array
	 * @param length	The length of the key
	 * @param nodeId	The starting position of the node for searching
	 * @return	The id of the key (you can use it as a perfect hash function)
	 */
	private int exactMatchSearch(CharSequence key, int position, int length, int nodeId){
		int result = -1;
		if(isInitialized()){
			if(length <= 0)
				length = key.length();
			if(nodeId < 0)
				nodeId = ROOT_NODE_ID;

			int b = base[nodeId];
			int p;
			for(int i = position; i < length; i ++){
				p = b + key.charAt(i) + 1;
				if(b == check[p])
					b = base[p];
				else
					return result;
			}

			p = b;
			final int n = base[p];
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
	public void visit(Function<VisitElement<V>, Boolean> visitor, CharSequence prefix){
		Objects.requireNonNull(visitor);
		Objects.requireNonNull(prefix);

		final int prefixAllowedLength = sequencer.length(prefix);

		Stack<VisitElement<V>> stack = new Stack<>();
		stack.push(new VisitElement<>(root, null, root.getKey()));
		while(!stack.isEmpty()){
			VisitElement<V> elem = stack.pop();

			if(elem.getValue() != null && elem.getKey().startsWith(prefix) && visitor.apply(elem))
				break;

			//add nodes to stack
			Collection<Integer> childrenIds = elem.getNode().getChildrenIds();
			final int prefixLen = sequencer.length(elem.getKey());
			for(Integer childId : childrenIds){
				V childValue = outerValue.get(childId);
				if(prefixLen >= prefixAllowedLength || sequencer.equalsAtIndex(child.getKey(), prefix, 0, prefixLen)){
					String newPrefix = sequencer.concat(elem.getKey(), child.getKey());
					stack.push(new VisitElement<>(child, elem.getNode(), newPrefix));
				}
			}
		}
	}

	//FIXME
//	public Set<VisitElement<V>> entrySet(){
//		final List<VisitElement<V>> entries = getEntries(sequencer.getEmptySequence(), type);
//		return new HashSet<>(entries);
//	}

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
