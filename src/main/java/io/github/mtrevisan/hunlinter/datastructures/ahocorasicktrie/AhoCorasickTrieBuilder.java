/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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

import io.github.mtrevisan.hunlinter.workers.core.IndexDataPair;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;


/**
 * A builder to build the AhoCorasickTrie
 *
 * @param <V>	The type of values stored in the tree
 */
public class AhoCorasickTrieBuilder<V extends Serializable>{

	private final RadixTrieNode rootNode = new RadixTrieNode();
	private AhoCorasickTrie<V> trie;

	/** Whether the position has been used. */
	private boolean[] used;
	/** The allocation size of the dynamic array. */
	private int allocSize;
	/** A parameter that controls the memory growth rate of the dynamic array. */
	private int memoryGrowthRate;
	/** The next position to check for unused memory. */
	private int nextCheckPos;
	/** The size of the key-pair sets. */
	private int keySize;


	/**
	 * Build a AhoCorasickTrie from a map
	 *
	 * @param map	A map containing key-value pairs
	 * @return	The trie
	 */
	public final AhoCorasickTrie<V> build(final Map<String, V> map){
		Objects.requireNonNull(map, "Map cannot be null");

		trie = new AhoCorasickTrie<>();

		//save the outer values
		final int size = map.size();
		trie.outerValue = new ArrayList<>(map.values());
		trie.keyLength = new int[size];

		//construct a two-point trie tree
		final Set<String> keySet = map.keySet();
		addAllKeywords(keySet);

		//building a double array trie tree based on a two-point trie tree
		buildTrie(keySet.size());

		//build the failure table and merge the output table
		constructFailureNodes();

		return trie;
	}

	/**
	 * Fetch siblings of a parent node.
	 *
	 * @param parent	Parent node.
	 * @param siblings	Parent node's child nodes, i.e. the siblings.
	 * @return	The amount of the siblings.
	 */
	private static int fetch(final RadixTrieNode parent, final Collection<IndexDataPair<RadixTrieNode>> siblings){
		if(parent.isAcceptable()){
			final RadixTrieNode fakeNode = new RadixTrieNode(-parent.getDepth() - 1);
			fakeNode.addChildrenId(parent.getLargestChildrenId());
			siblings.add(IndexDataPair.of(0, fakeNode));
		}
		for(final Map.Entry<Character, RadixTrieNode> entry : parent.getSuccess().entrySet())
			siblings.add(IndexDataPair.of(entry.getKey() + 1, entry.getValue()));
		return siblings.size();
	}

	/**
	 * add a collection of keywords
	 *
	 * @param keywordSet the collection holding keywords
	 */
	private void addAllKeywords(final Iterable<String> keywordSet){
		int i = 0;
		for(final String keyword : keywordSet)
			addKeyword(keyword, i ++);
	}

	/**
	 * Add a keyword
	 *
	 * @param keyword	A keyword
	 * @param id	The index of the keyword
	 */
	private void addKeyword(final String keyword, final int id){
		RadixTrieNode currentNode = rootNode;
		for(final Character character : keyword.toCharArray())
			currentNode = currentNode.addNode(character);
		currentNode.addChildrenId(id);
		trie.keyLength[id] = keyword.length();
	}

	private void constructFailureNodes(){
		if(!trie.isEmpty()){
			final int size = trie.check.length + 1;
			trie.next = new int[size];
			trie.next[1] = trie.base[0];
			trie.output = new int[size][];
			final Queue<RadixTrieNode> queue = new ArrayDeque<>();

			//the first step is to set the failure of the node with depth 1 to the root node
			for(final RadixTrieNode depthOneNode : rootNode.getNodes()){
				depthOneNode.setFailure(rootNode, trie.next);
				queue.add(depthOneNode);
				constructOutput(depthOneNode);
			}

			//the second step is to create the failure table
			constructFailureTable(queue);
		}
	}

	/** Create a failure table for the node with depth {@code > 1} (this is a BFS). */
	private void constructFailureTable(final Queue<RadixTrieNode> queue){
		while(!queue.isEmpty()){
			final RadixTrieNode currentNode = queue.remove();

			for(final Character transition : currentNode.getTransitions()){
				final RadixTrieNode targetNode = currentNode.nextNode(transition);
				queue.add(targetNode);

				final RadixTrieNode newFailureNode = currentNode.getFailureNode(transition);
				targetNode.setFailure(newFailureNode, trie.next);
				targetNode.addChildrenIds(newFailureNode.getChildrenIds());
				constructOutput(targetNode);
			}
		}
	}

	private void constructOutput(final RadixTrieNode targetNode){
		final Collection<Integer> childrenIds = targetNode.getChildrenIds();
		if(childrenIds == null || childrenIds.isEmpty())
			return;

		final int[] output = new int[childrenIds.size()];
		final Iterator<Integer> it = childrenIds.iterator();
		for(int i = 0; i < output.length; i ++)
			output[i] = it.next();
		trie.output[targetNode.getId()] = output;
	}

	private void buildTrie(final int keySize){
		memoryGrowthRate = 0;
		this.keySize = keySize;

		int totalKeysLen = 0;
		for(int i = 0; i < trie.keyLength.length; i ++)
			totalKeysLen += trie.keyLength[i];
		final int newSize = 65_536 + (totalKeysLen << 1) + 1;
		trie.check = new int[newSize];
		trie.base = new int[newSize];
		used = new boolean[newSize];
		allocSize = newSize;

		trie.base[0] = 1;
		nextCheckPos = 0;

		final List<IndexDataPair<RadixTrieNode>> siblings = new ArrayList<>(rootNode.getSuccess().entrySet().size());
		fetch(rootNode, siblings);
		insert(siblings);
	}

	/** Allocate the memory of the dynamic array. */
	private void resize(final int newSize){
		trie.base = Arrays.copyOf(trie.base, newSize);
		trie.check = Arrays.copyOf(trie.check, newSize);
		used = Arrays.copyOf(used, newSize);
		allocSize = newSize;
	}

	/**
	 * Insert the siblings into the double-array trie
	 *
	 * @param siblings	The siblings being inserted
	 * @return	The position to insert them
	 */
	private int insert(final List<IndexDataPair<RadixTrieNode>> siblings){
		int begin = 0;
		if(!siblings.isEmpty()){
			int checkPos = Math.max(siblings.get(0).getIndex() + 1, nextCheckPos) - 1;
			int nonZeroNum = 0;
			int first = 0;

			if(allocSize <= checkPos)
				resize(checkPos + 1);

			//the goal of this loop body is to find `n` free spaces that satisfy `base[begin + a_1…a_n] == 0`, `a_1` to `a_n` are `n` nodes
			//in siblings
			outer:
			while(true){
				checkPos ++;

				if(allocSize <= checkPos)
					resize(checkPos + 1);

				if(trie.check[checkPos] != 0){
					nonZeroNum ++;
					continue;
				}
				else if(first == 0){
					nextCheckPos = checkPos;
					first = 1;
				}

				//the distance of the current position from the first sibling node
				begin = checkPos - siblings.get(0).getIndex();
				if(allocSize <= (begin + siblings.get(siblings.size() - 1).getIndex())){
					//prevent progress from generating zero divide errors
					final double l = Math.max(1.05, (double)keySize / (memoryGrowthRate + 1));
					resize((int)(allocSize * l));
				}

				if(used[begin])
					continue;

				for(int i = 1; i < siblings.size(); i ++)
					if(trie.check[begin + siblings.get(i).getIndex()] != 0)
						continue outer;

				break;
			}

			// -- Simple heuristics --
			//if the percentage of non-empty contents in check between the index `nextCheckPos` and `checkPos` is greater than
			//some constant value (e.g. 0.9), new `nextCheckPos` index is written by `checkPos`
			if((double)nonZeroNum / (checkPos - nextCheckPos + 1) >= 0.95)
				//from the position `nextCheckPos` to `checkPos`, if the occupied space is above 95%, the next time you insert the node,
				//you can start directly from the `checkPos` position
				nextCheckPos = checkPos;
			used[begin] = true;

			for(final IndexDataPair<RadixTrieNode> sibling : siblings)
				trie.check[begin + sibling.getIndex()] = begin;

			final ArrayList<IndexDataPair<RadixTrieNode>> newSiblings = new ArrayList<>(0);
			for(final IndexDataPair<RadixTrieNode> sibling : siblings){
				newSiblings.clear();
				newSiblings.ensureCapacity(sibling.getData().getSuccess().entrySet().size() + 1);

				//the termination of a word and not the prefix of other words, in fact, is the leaf node
				if(fetch(sibling.getData(), newSiblings) == 0){
					trie.base[begin + sibling.getIndex()] = -sibling.getData().getLargestChildrenId() - 1;
					memoryGrowthRate ++;
				}
				else{
					//DFS
					final int h = insert(newSiblings);
					trie.base[begin + sibling.getIndex()] = h;
				}
				sibling.getData()
					.setId(begin + sibling.getIndex());
			}
		}
		return begin;
	}

}
