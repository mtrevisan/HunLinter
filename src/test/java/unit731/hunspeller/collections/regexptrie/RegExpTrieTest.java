package unit731.hunspeller.collections.regexptrie;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;


public class RegExpTrieTest{

	private final RegExpTrie<Integer> trie = new RegExpTrie<>();


	@Test
	public void contains(){
		trie.clear();

		trie.add("abc", 1);
		trie.add("abb", 2);
		trie.add("ac", 3);
		trie.add("a", 4);

		Assert.assertEquals(1, trie.containsKey("a").size());
		Assert.assertEquals(0, trie.containsKey("ab").size());
		Assert.assertEquals(0, trie.containsKey("c").size());
	}

	@Test
	public void findPrefixSimple(){
		trie.clear();

		trie.add("a", 1);
		trie.add("ab", 2);
		trie.add("bc", 3);
		trie.add("cd", 4);
		trie.add("abc", 5);

		List<RegExpPrefix<Integer>> prefixes = trie.findPrefix("abcd");
		Integer[] datas = prefixes.stream()
			.map(RegExpPrefix::getNode)
			.map(RegExpTrieNode::getData)
			.flatMap(List::stream)
			.toArray(Integer[]::new);
		Assert.assertArrayEquals(new Integer[]{1, 2, 5}, datas);
	}

	@Test
	public void findPrefixComplex(){
		trie.clear();

		trie.add("a", 1);
		trie.add(".b", 2);
		trie.add("bc", 3);
		trie.add("cd", 4);
		trie.add("a[bd]c", 5);

		List<RegExpPrefix<Integer>> prefixes = trie.findPrefix("abcd");
		Integer[] datas = prefixes.stream()
			.map(RegExpPrefix::getNode)
			.map(RegExpTrieNode::getData)
			.flatMap(List::stream)
			.toArray(Integer[]::new);
		Assert.assertArrayEquals(new Integer[]{1, 2, 5}, datas);
	}

}
