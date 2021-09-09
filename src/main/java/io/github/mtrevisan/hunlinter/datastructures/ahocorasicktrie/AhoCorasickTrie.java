/**
 * Copyright (c) 2019-2021 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.datastructures.ahocorasicktrie;

import io.github.mtrevisan.hunlinter.datastructures.ahocorasicktrie.dtos.HitProcessor;
import io.github.mtrevisan.hunlinter.datastructures.ahocorasicktrie.dtos.SearchResult;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


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

	@Serial
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
	 * @param text	The text.
	 * @return	A list of outputs.
	 */
	public final List<SearchResult<V>> searchInText(final String text){
		final ArrayList<SearchResult<V>> collectedHits = new ArrayList<>();
		final HitConsumer consumer = (hits, index) -> {
			collectedHits.ensureCapacity(collectedHits.size() + hits.length);
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
	 * @param text	The text.
	 * @param processor	A processor which handles the output.
	 */
	public final void searchInText(final String text, final HitProcessor<V> processor){
		Objects.requireNonNull(processor, "Processor cannot be null");

		final HitConsumer consumer = (hits, index) -> {
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
	 * Checks if the text contains at least one substring.
	 *
	 * @param text	Source text to check.
	 * @return	{@code true} if string contains at least one substring.
	 */
	public final boolean containsKey(final String text){
		return searchInText(text, (hits, index) -> false);
	}

	/**
	 * Search text.
	 *
	 * @param text	The text.
	 * @param hitConsumer	The consumer called in case of a hit.
	 */
	private boolean searchInText(final String text, final HitConsumer hitConsumer){
		Objects.requireNonNull(text, "Text cannot be null");

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

	public final boolean hasKey(final String key){
		final int id = exactMatchSearch(key);
		return (id >= 0);
	}

	public final V get(final String key){
		final int id = exactMatchSearch(key);
		return (outerValue != null && id >= 0? outerValue.get(id): null);
	}

	/**
	 * Update a value corresponding to a key.
	 *
	 * @param key	The key.
	 * @param value	The value.
	 * @return	successful or not（failure if there is no key）.
	 */
	public final boolean set(final String key, final V value){
		final int id = exactMatchSearch(key);
		if(id >= 0){
			outerValue.set(id, value);
			return true;
		}
		return false;
	}

	/** Transition of a node, if the node is root, and it failed, then returns the root. */
	private int transitionWithRoot(final int nodeId, final char character){
		final int b = base[nodeId];
		final int idx = b + character + 1;
		if(b != check[idx])
			return (nodeId == ROOT_NODE_ID? nodeId: -1);
		return idx;
	}

	/**
	 * Match exactly by a key.
	 *
	 * @param key	The key.
	 * @return	The id of the key (you can use it as a perfect hash function).
	 */
	private int exactMatchSearch(final String key){
		return exactMatchSearch(key, 0, 0, ROOT_NODE_ID);
	}

	/**
	 * Match exactly by a key.
	 *
	 * @param key	The key.
	 * @param position	The starting index of char array.
	 * @param length	The length of the key.
	 * @param nodeId	The starting position of the node for searching.
	 * @return	The id of the key (you can use it as a perfect hash function).
	 */
	private int exactMatchSearch(final String key, final int position, int length, int nodeId){
		Objects.requireNonNull(key, "Key cannot be null");

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

	public final int size(){
		return (outerValue != null? outerValue.size(): 0);
	}

	public final boolean isEmpty(){
		return (size() == 0);
	}

	public final boolean isInitialized(){
		return (output != null);
	}

	@Override
	public final boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final AhoCorasickTrie<?> rhs = (AhoCorasickTrie<?>)obj;
		return (Arrays.equals(base, rhs.base)
			&& Arrays.equals(next, rhs.next)
			&& Arrays.equals(check, rhs.check)
			&& Arrays.deepEquals(output, rhs.output)
			&& outerValue.equals(rhs.outerValue)
			&& Arrays.equals(keyLength, rhs.keyLength));
	}

	@Override
	public final int hashCode(){
		int result = outerValue.hashCode();
		result = 31 * result + Arrays.hashCode(base);
		result = 31 * result + Arrays.hashCode(next);
		result = 31 * result + Arrays.hashCode(check);
		result = 31 * result + Arrays.deepHashCode(output);
		result = 31 * result + Arrays.hashCode(keyLength);
		return result;
	}


	@SuppressWarnings("unused")
	@Serial
	private void writeObject(final ObjectOutputStream os) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

	@SuppressWarnings("unused")
	@Serial
	private void readObject(final ObjectInputStream is) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

}
