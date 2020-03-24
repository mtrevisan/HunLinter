package unit731.hunlinter.collections.ahocorasicktrie;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import unit731.hunlinter.collections.ahocorasicktrie.dtos.HitProcessor;
import unit731.hunlinter.collections.ahocorasicktrie.dtos.SearchResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;


/**
 * An implementation of the Aho-Corasick Radix Trie algorithm based on a double-array data structure
 *
 * @see <a href="https://en.wikipedia.org/wiki/Aho%E2%80%93Corasick_algorithm">Aho–Corasick algorithm</a>
 * @see <a href="https://pdfs.semanticscholar.org/18b9/da082ef35aea8bf9853ae6b35242539ff7da.pdf">Efficient implementation of Unicode string pattern matching automaton in Java</a>
 * @see <a href="https://www.db.ics.keio.ac.jp/seminar/2011/20111007_ishizaki/20111007_ishizaki_CIAA.pdf">A table compression method for Extended Aho-Corasick Automaton</a>
 * @see <a href="http://www.cs.uku.fi/~kilpelai/BSA05/lectures/slides04.pdf">Biosequence Algorithms, Spring 2005 - Lecture 4: Set Matching and Aho-Corasick Algorithm</a>
 * @see <a href="http://informatika.stei.itb.ac.id/~rinaldi.munir/Stmik/2014-2015/Makalah2015/Makalah_IF221_Strategi_Algoritma_2015_032.pdf">Aho-Corasick Algorithm in Pattern Matching</a>
 * @see <a href="https://github.com/hankcs/AhoCorasickDoubleArrayTrie">Aho-Corasick double-array trie</a>
 * @see <a href="https://github.com/robert-bor/aho-corasick">Aho-Corasick</a>
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


	AhoCorasickTrie(){}

	/**
	 * Perform a search and return all the entries that are contained into the given text.
	 *
	 * @param text	The text
	 * @return	A list of outputs
	 */
	public List<SearchResult<V>> searchInText(final String text){
		final List<SearchResult<V>> collectedHits = new ArrayList<>();
		final BiFunction<int[], Integer, Boolean> consumer = (hits, index) -> {
			final int position = index + 1;
			for(final int hit : hits)
				collectedHits.add(new SearchResult<>(position - keyLength[hit], position, outerValue.get(hit)));
			return true;
		};
		searchInText(text, consumer);
		return collectedHits;
	}

	/**
	 * Perform a search and call the processor for each entry that are contained into the given text.
	 *
	 * @param text	The text
	 * @param processor	A processor which handles the output
	 */
	public void searchInText(final String text, final HitProcessor<V> processor){
		Objects.requireNonNull(processor);

		final BiFunction<int[], Integer, Boolean> consumer = (hits, index) -> {
			final int position = index + 1;
			for(final int hit : hits){
				final boolean proceed = processor.hit(position - keyLength[hit], position, outerValue.get(hit));
				if(!proceed)
					return false;
			}
			return true;
		};
		searchInText(text, consumer);
	}

	/**
	 * Checks if the text contains at least one substring
	 *
	 * @param text	Source text to check
	 * @return	<code>true</code> if string contains at least one substring
	 */
	public boolean containsKey(final String text){
		return searchInText(text, (hits, index) -> false);
	}

	/**
	 * Search text
	 *
	 * @param text	The text
	 * @param hitConsumer	The consumer called in case of a hit
	 */
	private boolean searchInText(final String text, final BiFunction<int[], Integer, Boolean> hitConsumer){
		Objects.requireNonNull(text);

		boolean found = false;
		if(isInitialized()){
			int currentNodeId = ROOT_NODE_ID;
			for(int i = 0; i < text.length(); i ++){
				currentNodeId = retrieveNextNodeId(currentNodeId, text.charAt(i));

				//store hits
				final int[] hits = output[currentNodeId];
				if(hits != null){
					found = true;

					final boolean proceed = hitConsumer.apply(hits, i);
					if(!proceed)
						break;
				}
			}
		}
		return found;
	}

	private int retrieveNextNodeId(int currentNodeId, final char character){
		int nextNodeId;
		while((nextNodeId = transitionWithRoot(currentNodeId, character)) == -1)
			currentNodeId = next[currentNodeId];
		return nextNodeId;
	}

	public boolean hasKey(final String key){
		final int id = exactMatchSearch(key);
		return (id >= 0);
	}

	public V get(final String key){
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
	public boolean set(final String key, final V value){
		final int id = exactMatchSearch(key);
		if(id >= 0){
			outerValue.set(id, value);
			return true;
		}
		return false;
	}

	/** Transition of a node, if the node is root and it failed, then returns the root */
	private int transitionWithRoot(final int nodeId, final char character){
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
	private int exactMatchSearch(final String key){
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
	private int exactMatchSearch(final String key, final int position, int length, int nodeId){
		Objects.requireNonNull(key);

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

	public int size(){
		return (outerValue != null? outerValue.size(): 0);
	}

	public boolean isEmpty(){
		return (size() == 0);
	}

	public boolean isInitialized(){
		return (output != null);
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final AhoCorasickTrie rhs = (AhoCorasickTrie)obj;
		return new EqualsBuilder()
			.append(base, rhs.base)
			.append(next, rhs.next)
			.append(check, rhs.check)
			.append(output, rhs.output)
			.append(outerValue, rhs.outerValue)
			.append(keyLength, rhs.keyLength)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(base)
			.append(next)
			.append(check)
			.append(output)
			.append(outerValue)
			.append(keyLength)
			.toHashCode();
	}

}
