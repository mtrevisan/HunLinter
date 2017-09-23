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
	 * Adds a sequence into the RegExpTrie
	 *
	 * @param sequence	The sequence to be added
	 * @param data	The payload for the inserted sequence
	 * @return The node just inserted
	 */
	public RegExpTrieNode<T> add(String sequence, T data){
		RegExpTrieNode<T> node = root;
		String[] chars = characters(sequence);
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

	public boolean remove(String sequence){
		boolean result = false;
		List<RegExpPrefix<T>> prefixes = findPrefix(sequence);
		if(prefixes.size() == 1)
			result = removeSingle(sequence, prefixes.get(0));
		return result;
	}

	public boolean removeAll(String sequence){
		List<RegExpPrefix<T>> prefixes = findPrefix(sequence);
		return prefixes.stream()
			.map(prefix -> removeSingle(sequence, prefix))
			.reduce(true, (a, b) -> a && b);
	}

	private boolean removeSingle(String sequence, RegExpPrefix<T> pref){
		boolean result = false;
		if(pref.isLeaf()){
			String[] chars = characters(sequence);
			String pattern = chars[sequence.length() - 1];
			result = (pref.getParent().removeChild(pattern) != null);
		}
		return result;
	}

	public List<RegExpPrefix<T>> findSuffix(String sequence){
		sequence = new StringBuilder(sequence).reverse().toString();
		return findPrefix(sequence);
	}

	public List<RegExpPrefix<T>> findPrefix(String sequence){
		List<RegExpPrefix<T>> result = new ArrayList<>();
		Stack<RegExpTrieNode<T>> stack = new Stack<>();
		stack.push(root);
		List<RegExpTrieNode<T>> tmpStack = new ArrayList<>();
		int size = sequence.length();
		int i = 0;
		while(!stack.isEmpty()){
			RegExpTrieNode<T> parent = stack.pop();
			List<RegExpTrieNode<T>> tmp = parent.getChildrenMatching(sequence.charAt(i));

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
	 * Search the given string and return an object if it lands on a sequence, essentially testing if the sequence exists in the trie.
	 *
	 * @param sequence	The sequence to search for
	 * @return The list of nodes found if the sequence is contained into this trie
	 */
	public List<RegExpTrieNode<T>> containsKey(String sequence){
		Stack<RegExpTrieNode<T>> stack = new Stack<>();
		stack.push(root);
		List<RegExpTrieNode<T>> tmpStack = new ArrayList<>();
		int size = sequence.length();
		int i = 0;
		while(!stack.isEmpty()){
			RegExpTrieNode<T> parent = stack.pop();
			List<RegExpTrieNode<T>> tmp = parent.getChildrenMatching(sequence.charAt(i));

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

	private String[] characters(String sequence){
		String[] characters = PatternService.split(sequence, REGEX_PATTERN_EMPTY);
		List<String> list = new ArrayList<>();
		String group = null;
		boolean inGroup = false;
		for(String chr : characters){
			if(("[".equals(chr) || "]".equals(chr))){
				if(!inGroup){
					inGroup = true;
					group = "[";
				}
				else if(inGroup){
					inGroup = false;
					group += "]";
					list.add(group);
					group = StringUtils.EMPTY;
				}
			}
			else if(inGroup){
				if("^".equals(chr))
					group = "[^" + group.substring(1);
				else
					group += chr;
			}
			else
				list.add(chr);
		}
		return list.toArray(new String[0]);
	}

}
