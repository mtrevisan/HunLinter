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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;


/**
 * Aho-Corasick automaton for multi-pattern search.
 */
public class AhoCorasickReplacer{

	/** Node of the automaton (trie + failure links). */
	private static final class Node{
		//children transitions by character
		final Map<Character, Node> children = new HashMap<>();
		//failure link
		Node fail;
		//patterns that end at this node (can be multiple)
		final List<String> outputs = new ArrayList<>();

	}

	private final Node root = new Node();
	private final Map<String, String> replacements;


	/**
	 * Builds the automaton given search and replacement lists.
	 *
	 * @throws IllegalArgumentException	If lengths differ.
	 */
	public AhoCorasickReplacer(final String[] searchList, final String[] replacementList){
		if(searchList.length != replacementList.length)
			throw new IllegalArgumentException("Search and replacement lists must have equal length.");

		this.replacements = new HashMap<>(searchList.length);
		for(int i = 0, length = searchList.length; i < length; i ++){
			this.replacements.put(searchList[i], replacementList[i]);
			insert(searchList[i]);
		}
		buildFailureLinks();
	}

	/** Inserts a single pattern into the trie. */
	private void insert(final String pattern){
		Node node = root;
		for(int i = 0, length = pattern.length(); i < length; i ++){
			final char c = pattern.charAt(i);
			node = node.children.computeIfAbsent(c, k -> new Node());
		}
		node.outputs.add(pattern);
	}

	/** Builds failure links for the automaton via BFS. */
	private void buildFailureLinks(){
		final Queue<Node> q = new ArrayDeque<>();
		root.fail = root;
		// Initialize depth-1 nodes
		for(final Node child : root.children.values()){
			child.fail = root;
			q.add(child);
		}
		//BFS
		while(!q.isEmpty()){
			final Node cur = q.poll();
			for(final Map.Entry<Character, Node> e : cur.children.entrySet()){
				final char c = e.getKey();
				final Node nxt = e.getValue();
				q.add(nxt);

				Node f = cur.fail;
				while(f != root && !f.children.containsKey(c))
					f = f.fail;
				nxt.fail = f.children.getOrDefault(c, root);
				//merge outputs from fail link (classic AC)
				nxt.outputs.addAll(nxt.fail.outputs);
			}
		}
	}

	/**
	 * Performs a replacement by scanning the ORIGINAL text.
	 * For every match (ending at index i), we replace that exact occurrence.
	 * This is efficient, BUT its semantics differ from "resume in mutated string after replacement".
	 */
	public String replaceInOriginalText(final String text){
		final int length = text.length();
		final StringBuilder out = new StringBuilder(length);
		Node node = root;

		//collect replacements as we walk and write directly to out
		//to properly replace without losing characters, it is necessary to keep track of the last written index
		int lastWritten = 0;
		for(int i = 0; i < length; i ++){
			final char c = text.charAt(i);

			//follow fail links while there's no transition
			while(node != root && !node.children.containsKey(c))
				node = node.fail;
			node = node.children.getOrDefault(c, root);

			if(!node.outputs.isEmpty()){
				//there may be multiple outputs; we'll use the first (shortest) here, or you can define a tie-breaking
				//policy (e.g., longest-first).
				final String matched = node.outputs.getFirst();
				final int start = i - matched.length() + 1;

				//write any characters before the match that haven't been written yet
				if(lastWritten < start)
					out.append(text, lastWritten, start);

				//write the replacement
				out.append(replacements.get(matched));
				lastWritten = i + 1;
			}
		}

		//write any remaining chars
		if(lastWritten < text.length())
			out.append(text, lastWritten, text.length());
		return out.toString();
	}

}
