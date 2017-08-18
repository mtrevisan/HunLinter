package unit731.hunspeller.collections.trie;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;


public class TrieTest{

	private final Trie<Integer> trie = new Trie<>();


	@Test
	public void contains(){
		trie.clear();

		trie.add("abc", 1);
		trie.add("abb", 2);
		trie.add("ac", 3);
		trie.add("a", 4);

		Assert.assertNotNull(trie.contains("a"));
		Assert.assertNull(trie.contains("ab"));
		Assert.assertNull(trie.contains("c"));
	}

	@Test
	public void findPrefix(){
		trie.clear();

		trie.add("a", 1);
		trie.add("ab", 2);
		trie.add("bc", 3);
		trie.add("cd", 4);
		trie.add("abc", 5);

		List<Prefix<Integer>> prefixes = trie.findPrefix("abcd");
		Integer[] datas = prefixes.stream()
			.map(Prefix::getNode)
			.map(TrieNode::getData)
			.toArray(Integer[]::new);
		Assert.assertArrayEquals(new Integer[]{1, 2, 5}, datas);
	}

}
