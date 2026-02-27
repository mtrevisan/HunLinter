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
package io.github.mtrevisan.hunlinter.services;

import java.util.HashMap;
import java.util.Map;


/**
 * Trie-based replacer that respects the exact semantics:
 * - at each current position, try to match any search pattern starting THERE;
 * - if matched, replace, then resume from AFTER the replacement in the MUTATED string;
 * - if not matched, advance by one character.
 *
 * https://github.com/hankcs/AhoCorasickDoubleArrayTrie/tree/master/src/main/java/com/hankcs/algorithm
 */
public class TrieReplacer{

	/** Node of the prefix trie. */
	private static final class Node{
		final Map<Character, Node> children = new HashMap<>();
		//index of pattern ending here; -1 means no pattern ends here
		int outIndex = -1;
	}


	private final Node root = new Node();
	private final String[] searchList;
	private final String[] replacementList;

	/**
	 * Constructs the trie from the given search and replacement lists.
	 *
	 * @throws IllegalArgumentException	If lengths differ.
	 */
	public TrieReplacer(final String[] searchList, final String[] replacementList){
		int length = searchList.length;
		if(length != replacementList.length)
			throw new IllegalArgumentException("Search and replacement lists must have equal length.");

		this.searchList = searchList;
		this.replacementList = replacementList;
		for(int i = 0; i < length; i ++)
			insert(searchList[i], i);
	}

	/** Inserts one pattern into the trie with its terminal index. */
	private void insert(final String pattern, final int index){
		Node node = root;
		for(int i = 0, length = pattern.length(); i < length; i ++){
			final char c = pattern.charAt(i);
			node = node.children.computeIfAbsent(c, k -> new Node());
		}
		node.outIndex = index;
	}

	/**
	 * Performs the replacement respecting the mutated-string semantics.
	 * Efficient: avoids substring creation and checks via trie at the current position only.
	 */
	public String replaceEach(final String text){
		final int length = text.length();
		final StringBuilder sb = new StringBuilder(length);
		int position = 0;
		while(position < length){
			Node node = root;
			int j = position;
			int matchedIndex = -1;

			//traverse the trie along characters from the current position
			while(j < length){
				node = node.children.get(text.charAt(j));
				if(node == null)
					break;

				if(node.outIndex != -1){
					matchedIndex = node.outIndex;

					//earliest match at current position
					break;
				}
				j ++;
			}

			if(matchedIndex != -1){
				sb.append(replacementList[matchedIndex]);

				//resume from after the replacement (in the mutated string)
				position += searchList[matchedIndex].length();
			}
			else{
				sb.append(text.charAt(position));

				//no match at current position: advance by one
				position ++;
			}
		}

		return sb.toString();
	}

}
