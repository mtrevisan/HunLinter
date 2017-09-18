package unit731.hunspeller.collections.trie;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;


public class TrieTest{

	private final Trie<Integer> trie = new Trie<>();


	@Test
	public void contains(){
		trie.clear();

		trie.put("abc", 1);
		trie.put("abb", 2);
		trie.put("ac", 3);
		trie.put("a", 4);

		Assert.assertNotNull(trie.containsKey("a"));
		Assert.assertNull(trie.containsKey("ab"));
		Assert.assertNull(trie.containsKey("c"));
	}

	@Test
	public void findPrefix(){
		trie.clear();

		trie.put("a", 1);
		trie.put("ab", 2);
		trie.put("bc", 3);
		trie.put("cd", 4);
		trie.put("abc", 5);

		List<Prefix<Integer>> prefixes = trie.findPrefix("abcd");
		Integer[] datas = prefixes.stream()
			.map(Prefix::getNode)
			.map(TrieNode::getValue)
			.toArray(Integer[]::new);
		Assert.assertArrayEquals(new Integer[]{1, 2, 5}, datas);
	}

}
