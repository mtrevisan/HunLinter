/**
 * Copyright (c) 2019-2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.services;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * Optimized Trie-based replacer with:
 * - Flat node pool (no per-node object arrays): transitions are stored in a single contiguous int[].
 * - Compressed alphabet: UTF-16 char -> compact index [0..alphabetSize-1] or -1 if not used by patterns.
 * - CharSequence input support to avoid unnecessary conversions.
 * - Pre-sized StringBuilder to minimize reallocations.
 * <p>
 * Semantics: "earliest match at current position".
 * For each position 'p' in the input:
 * - walk the trie from p forward; if a terminal node is found, replace and advance p by the pattern length;
 * - otherwise copy one character and advance by one.
 * <p>
 * Design details:
 * - Node IDs are ints [0..nodeCount-1].
 * - Transitions table is a flat int[] of size (nodeCount * alphabetSize). Entry = next node ID, or -1 if missing.
 * - Out-index table holds the matched pattern index for terminal nodes, or -1.
 * <p>
 * Thread-safety: instances are immutable after construction and can be used concurrently.
 */
public final class TrieReplacer{

	//compact alphabet as a mini open-addressing hash table
	private final char[] alphaKeys;
	//values: compact index of each character (0..alphabetSize-1)
	private final int[] alphaIndex;
	/** mask for open-addressing probing */
	private final int alphaMask;
	/** size of compact alphabet (number of distinct chars appearing in patterns) */
	private final int alphabetSize;

	/**
	 * Flat transitions table:
	 * For node 's' and alphabet symbol 'a' (0..alphabetSize-1), the transition is at index (s * alphabetSize + a).
	 * Value = next node id, or -1 if no transition.
	 * <p>
	 * NOTE: We keep this as a single contiguous array for better locality and fewer allocations.
	 */
	private int[] transitions;
	/** Output pattern index per node; -1 means "no pattern ends here". */
	private int[] outIndex;
	/** Total number of nodes in the trie (root is node 0). */
	private int nodeCount;

	/** Replacement strings (same length as searchList). */
	private final String[] replacementList;
	/** Precomputed pattern lengths to avoid repeated String.length() calls in the hot path. */
	private final int[] patternLengths;

	private static final int INITIAL_NODES = 4;


	/**
	 * Constructs a compressed-trie replacer with a flat node pool.
	 *
	 * @param searchList	Array of non-{@code null}, non-empty patterns to search for.
	 * @param replacementList	Array of non-{@code null} replacement strings.
	 * @throws IllegalArgumentException	If array lengths differ, or a pattern is {@code null}/empty, or a replacement is {@code null}.
	 */
	public TrieReplacer(final String[] searchList, final String[] replacementList){
		if(searchList == null || replacementList == null)
			throw new IllegalArgumentException("searchList and replacementList must not be null.");
		final int length = searchList.length;
		if(length != replacementList.length)
			throw new IllegalArgumentException("Search and replacement lists must have equal length.");

		this.replacementList = replacementList;

		//precompute pattern lengths
		this.patternLengths = new int[length];
		for(int i = 0; i < length; i ++)
			patternLengths[i] = searchList[i].length();

		//build compact alphabet
		final Set<Character> alphabet = new LinkedHashSet<>();
		for(int i = 0; i < length; i ++){
			final String pat = searchList[i];
			for(int j = 0, size = patternLengths[i]; j < size; j ++)
				alphabet.add(pat.charAt(j));
		}
		this.alphabetSize = alphabet.size();

		//table size = next power of 2 >= alphabetSize × 2
		int tableSize = 1;
		while(tableSize < alphabetSize * 2)
			tableSize <<= 1;
		this.alphaMask = tableSize - 1;

		this.alphaKeys = new char[tableSize];
		this.alphaIndex = new int[tableSize];
		Arrays.fill(alphaIndex, -1);

		//assign compact indices [0..alphabetSize-1] to the seen characters
		int idx = 0;
		for(final char chr : alphabet)
			insertAlphabetCharacter(chr, idx ++);

		//initialize flat node pool with a few nodes and grow as needed
		this.nodeCount = 0;
		ensureNodeCapacity(INITIAL_NODES);

		//create root node at id 0
		addNode();

		//build the trie using the compact alphabet
		for(int i = 0; i < length; i ++)
			insert(searchList[i], i);
	}

	private void insertAlphabetCharacter(final char chr, final int index){
		int slot = (chr & alphaMask);
		while(alphaIndex[slot] != -1)
			slot = (slot + 1) & alphaMask;
		alphaKeys[slot] = chr;
		alphaIndex[slot] = index;
	}

	private int getAlphabetCharacterIndex(final char chr){
		int slot = (chr & alphaMask);
		while(true){
			final int ix = alphaIndex[slot];
			if(ix == -1)
				return -1;
			if(alphaKeys[slot] == chr)
				return ix;

			slot = (slot + 1) & alphaMask;
		}
	}

	/**
	 * Adds one node to the pool and returns its node id.
	 * Initializes its transitions to -1 and outIndex to -1.
	 */
	private int addNode(){
		final int id = nodeCount;
		nodeCount ++;
		ensureNodeCapacity(nodeCount);
		//transitions for this node are already -1 thanks to ensureNodeCapacity()
		//outIndex[id] already -1 thanks to ensureNodeCapacity()
		return id;
	}

	/**
	 * Inserts a single pattern into the trie, marking the terminal node with outIndex = pattern index.
	 * Uses compact alphabet indices via charMap[]; all pattern chars must exist in the compressed alphabet.
	 */
	private void insert(final String pattern, final int index){
		int state = 0;
		for(int i = 0, length = patternLengths[index]; i < length; i ++){
			final int chr = getAlphabetCharacterIndex(pattern.charAt(i));
			final int tIndex = state * alphabetSize + chr;
			int next = transitions[tIndex];
			if(next == -1){
				next = addNode();
				transitions[tIndex] = next;
			}
			state = next;
		}
		outIndex[state] = index;
	}

	/**
	 * Ensures the flat arrays (transitions, outIndex) can store at least 'minNodes' nodes.
	 * Grows both arrays if needed and initializes new entries to -1.
	 */
	private void ensureNodeCapacity(final int minNodes){
		final int neededTransSize = minNodes * alphabetSize;
		if(transitions == null){
			//first allocation
			transitions = new int[Math.max(neededTransSize, Math.max(1, alphabetSize))];
			Arrays.fill(transitions, -1);
		}
		else if(transitions.length < neededTransSize){
			final int oldLen = transitions.length;
			transitions = Arrays.copyOf(transitions, growSize(oldLen, neededTransSize));
			//initialize the newly added segment(s) to -1
			Arrays.fill(transitions, oldLen, transitions.length, -1);
		}

		if(outIndex == null){
			outIndex = new int[Math.max(minNodes, 1)];
			Arrays.fill(outIndex, -1);
		}
		else if(outIndex.length < minNodes){
			final int oldLen = outIndex.length;
			outIndex = Arrays.copyOf(outIndex, growSize(oldLen, minNodes));
			Arrays.fill(outIndex, oldLen, outIndex.length, -1);
		}
	}

	/**
	 * Computes a "grown" size for arrays, doubling while ensuring at least minNeeded.
	 *
	 * @param current	Current array length.
	 * @param minNeeded	Minimum required length.
	 * @return	New length (>= minNeeded).
	 */
	private static int growSize(final int current, final int minNeeded){
		//double strategy: newSize = max(current * 2, minNeeded), with overflow guard
		int newSize = (current << 1);
		if(newSize < 0)
			newSize = Integer.MAX_VALUE;
		if(newSize < minNeeded)
			newSize = minNeeded;
		return newSize;
	}

	/**
	 * Performs the replacement respecting "earliest match at current position" semantics.
	 * Efficient scanning:
	 * - At each position 'pos', walks the trie along the input characters without creating substrings.
	 * - Breaks on the first terminal node (earliest match) and applies the corresponding replacement.
	 * - If no match starts at 'pos', appends a single character and moves on.
	 *
	 * @param text	Any CharSequence (String, StringBuilder, etc.).
	 * @return	The replaced string
	 */
	public String replaceEach(final CharSequence text){
		final int length = text.length();

		//pre-size the builder to the input length to avoid most reallocations
		final StringBuilder sb = new StringBuilder(length);

		int position = 0;
		while(position < length){
			//start from root at each position
			int state = 0;
			int j = position;
			//index into replacementList/searchList
			int matchedIndex = -1;

			//traverse the trie starting at 'position'
			//using the compact alphabet: if a char is not in the alphabet, we cannot match any pattern through it
			while(j < length){
				final int chr = getAlphabetCharacterIndex(text.charAt(j));
				if(chr < 0)
					//character not present in any pattern: no match can start at 'position' through this char
					break;

				final int tIndex = state * alphabetSize + chr;
				state = transitions[tIndex];
				if(state == -1)
					//the path diverged: no match starting at 'position'
					break;

				final int out = outIndex[state];
				if(out != -1){
					//earliest match at current position found; take it
					matchedIndex = out;
					break;
				}
				j ++;
			}

			if(matchedIndex != -1){
				//append the replacement for the matched pattern
				sb.append(replacementList[matchedIndex]);
				//advance 'position' by the length of the matched pattern (mutated-string semantics)
				position += patternLengths[matchedIndex];
			}
			else{
				//no match starting at 'position': copy the current character and move on
				sb.append(text.charAt(position));
				position ++;
			}
		}

		return sb.toString();
	}

}
