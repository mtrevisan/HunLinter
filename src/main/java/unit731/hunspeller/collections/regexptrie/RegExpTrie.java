package unit731.hunspeller.collections.regexptrie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.PatternService;


public class RegExpTrie<T>{

	private static final Pattern REGEX_PATTERN_EMPTY = PatternService.pattern(StringUtils.EMPTY);


	/** the root of the RegExpTrie */
	private final RegExpTrieNode<T> root = new RegExpTrieNode<>();


	public void clear(){
		root.clear();
	}

	/**
	 * Adds a word into the RegExpTrie
	 *
	 * @param word	The word to be added
	 * @param data	The payload for the inserted word
	 * @return The node just inserted
	 */
	public RegExpTrieNode<T> add(String word, T data){
		RegExpTrieNode<T> node = root;
		String[] chars = characters(word);
		for(String stem : chars){
			RegExpTrieNode<T> nextNode = node.getChild(stem);
			if(nextNode == null){
				nextNode = new RegExpTrieNode<>();
				node.addChild(stem, nextNode);
			}
			node = nextNode;
		}
		node.getData().add(data);
		node.setLeaf();
		return node;
	}

	public boolean remove(String word){
		boolean result = false;
		List<RegExpPrefix<T>> prefixes = findPrefix(word);
		if(prefixes.size() == 1)
			result = removeSingle(word, prefixes.get(0));
		return result;
	}

	public boolean removeAll(String word){
		List<RegExpPrefix<T>> prefixes = findPrefix(word);
		return prefixes.stream()
			.map(prefix -> removeSingle(word, prefix))
			.reduce(true, (a, b) -> a && b);
	}

	private boolean removeSingle(String word, RegExpPrefix<T> pref){
		boolean result = false;
		if(pref.isLeaf()){
			String[] chars = characters(word);
			String pattern = chars[word.length() - 1];
			result = (pref.getParent().removeChild(pattern) != null);
		}
		return result;
	}

	public List<RegExpPrefix<T>> findSuffix(String word){
		word = new StringBuilder(word).reverse().toString();
		return findPrefix(word);
	}

	public List<RegExpPrefix<T>> findPrefix(String word){
		List<RegExpPrefix<T>> result = new ArrayList<>();
		Stack<RegExpTrieNode<T>> stack = new Stack<>();
		stack.push(root);
		List<RegExpTrieNode<T>> tmpStack = new ArrayList<>();
		int size = word.length();
		int i = 0;
		while(!stack.isEmpty()){
			RegExpTrieNode<T> parent = stack.pop();
			List<RegExpTrieNode<T>> tmp = parent.getChildrenMatching(word.charAt(i));

			int index = i;
			tmp.stream()
				.filter(RegExpTrieNode::isLeaf)
				.map(n -> new RegExpPrefix<>(n, index, parent))
				.forEach(result::add);

			tmpStack.addAll(tmp);
			if(stack.isEmpty()){
				i++;
				if(i == size)
					break;

				stack.addAll(tmpStack);
				tmpStack.clear();
			}
		}
		return result;
	}

	/**
	 * Search the given string and return an object if it lands on a word, essentially testing if the word exists in the trie.
	 *
	 * @param word	The word to search for
	 * @return The list of nodes found if the word is contained into this trie
	 */
	public List<RegExpTrieNode<T>> contains(String word){
		Stack<RegExpTrieNode<T>> stack = new Stack<>();
		stack.push(root);
		List<RegExpTrieNode<T>> tmpStack = new ArrayList<>();
		int size = word.length();
		int i = 0;
		while(!stack.isEmpty()){
			RegExpTrieNode<T> parent = stack.pop();
			List<RegExpTrieNode<T>> tmp = parent.getChildrenMatching(word.charAt(i));

			tmpStack.addAll(tmp);
			if(stack.isEmpty()){
				i++;
				if(i == size)
					break;

				stack.addAll(tmpStack);
				tmpStack.clear();
			}
		}
		return (i == size? tmpStack.stream().filter(RegExpTrieNode::isLeaf).collect(Collectors.toList()): Collections.<RegExpTrieNode<T>>emptyList());
	}

	private String[] characters(String word){
		String[] characters = PatternService.split(word, REGEX_PATTERN_EMPTY);
		List<String> list = new ArrayList<>();
		String group = null;
		boolean inGroup = false;
		for(String chr : characters){
			if(!inGroup && ("[".equals(chr) || "]".equals(chr))){
				inGroup = true;
				group = "[";
			}
			else if(inGroup && "^".equals(chr))
				group = "[^" + group.substring(1);
			else if(inGroup && ("[".equals(chr) || "]".equals(chr))){
				inGroup = false;
				group += "]";
				list.add(group);
			}
			else if(inGroup)
				group += chr;
			else
				list.add(chr);
		}
		return list.toArray(new String[0]);
	}

}
