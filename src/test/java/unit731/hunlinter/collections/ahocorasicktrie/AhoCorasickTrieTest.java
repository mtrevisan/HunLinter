package unit731.hunlinter.collections.ahocorasicktrie;

import unit731.hunlinter.collections.ahocorasicktrie.dtos.SearchResult;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class AhoCorasickTrieTest{

	@Test
	void emptyTree(){
		Map<String, String> map = new HashMap<>();
		AhoCorasickTrie<String> trie = new AhoCorasickTrieBuilder<String>()
			.build(map);

		Assertions.assertTrue(trie.isEmpty());
		Assertions.assertFalse(trie.containsKey("word"));
		Assertions.assertNull(trie.get("word"));
		Assertions.assertFalse(trie.containsKey(StringUtils.EMPTY));
		Assertions.assertNull(trie.get(StringUtils.EMPTY));
		Assertions.assertEquals(0, trie.size());
	}

	@Test
	void singleInsertion(){
		Map<String, Integer> map = Collections.singletonMap("test", 1);
		AhoCorasickTrie<Integer> trie = new AhoCorasickTrieBuilder<Integer>()
			.build(map);

		Assertions.assertEquals(1, trie.size());
		Assertions.assertTrue(trie.hasKey("test"));
		Assertions.assertFalse(trie.hasKey("tes"));
		Assertions.assertFalse(trie.hasKey("testt"));
		Assertions.assertTrue(trie.containsKey("test"));
		Assertions.assertFalse(trie.containsKey("tes"));
		Assertions.assertTrue(trie.containsKey("testt"));
	}

	@Test
	void multipleInsertions(){
		Map<String, Integer> map = new HashMap<>();
		map.put("test", 1);
		map.put("tent", 2);
		map.put("tentest", 21);
		map.put("tank", 3);
		map.put("rest", 4);
		AhoCorasickTrie<Integer> trie = new AhoCorasickTrieBuilder<Integer>()
			.build(map);

		Assertions.assertEquals(5, trie.size());
		Assertions.assertEquals(1, trie.get("test").intValue());
		Assertions.assertEquals(2, trie.get("tent").intValue());
		Assertions.assertEquals(3, trie.get("tank").intValue());
		Assertions.assertEquals(4, trie.get("rest").intValue());

		List<SearchResult<Integer>> results = trie.searchInText("resting in the test");
		Assertions.assertEquals(2, results.size());
		SearchResult<Integer> search = results.get(0);
		Assertions.assertEquals(0, search.getIndexBegin());
		Assertions.assertEquals(4, search.getValue().intValue());
		search = results.get(1);
		Assertions.assertEquals(15, search.getIndexBegin());
		Assertions.assertEquals(1, search.getValue().intValue());

		results = trie.searchInText("blah");
		Assertions.assertTrue(results.isEmpty());
	}

	@Test
	void multipleInsertionOfTheSameKey(){
		Map<String, Integer> map = new HashMap<>();
		map.put("test", 1);
		map.put("tent", 2);
		map.put("tank", 3);
		map.put("rest", 4);
		map.put("test", 9);
		AhoCorasickTrie<Integer> trie = new AhoCorasickTrieBuilder<Integer>()
			.build(map);

		Assertions.assertEquals(4, trie.size());
		Assertions.assertEquals(9, trie.get("test").intValue());
		Assertions.assertEquals(2, trie.get("tent").intValue());
		Assertions.assertEquals(3, trie.get("tank").intValue());
		Assertions.assertEquals(4, trie.get("rest").intValue());
	}

	@Test
	void simplePut(){
		Map<String, Boolean> map = new HashMap<>();
		map.put("java.lang.", Boolean.TRUE);
		map.put("java.i", Boolean.TRUE);
		map.put("java.io.", Boolean.TRUE);
		map.put("java.util.concurrent.", Boolean.TRUE);
		map.put("java.util.", Boolean.FALSE);
		map.put("java.lang.Boolean", Boolean.FALSE);
		AhoCorasickTrie<Boolean> trie = new AhoCorasickTrieBuilder<Boolean>()
			.build(map);

		Assertions.assertFalse(trie.isEmpty());
		Assertions.assertEquals(1, trie.searchInText("java.lang.Integer").size());
		Assertions.assertEquals(1, trie.searchInText("java.lang.Long").size());
		Assertions.assertEquals(2, trie.searchInText("java.lang.Boolean").size());
		Assertions.assertEquals(2, trie.searchInText("java.io.InputStream").size());
		Assertions.assertEquals(1, trie.searchInText("java.util.ArrayList").size());
		Assertions.assertEquals(2, trie.searchInText("java.util.concurrent.ConcurrentHashMap").size());
	}

	@Test
	void hasStartsWithMatch(){
		Map<String, Boolean> map = new HashMap<>();
		map.put("bookshelf", Boolean.TRUE);
		map.put("wowza", Boolean.FALSE);
		AhoCorasickTrie<Boolean> trie = new AhoCorasickTrieBuilder<Boolean>()
			.build(map);

		Assertions.assertEquals(1, trie.searchInText("wowzacowza").size());
		Assertions.assertEquals(1, trie.searchInText("bookshelfmania").size());
		Assertions.assertEquals(Boolean.FALSE, trie.get("wowza"));
		Assertions.assertEquals(Boolean.TRUE, trie.get("bookshelf"));
		Assertions.assertNull(trie.get("bookshelf2"));
	}

	@Test
	void searchForPartialParentAndLeafKeyWhenOverlapExists(){
		Map<String, String> map = new HashMap<>();
		map.put("abcd", "abcd");
		map.put("abce", "abce");
		AhoCorasickTrie<String> trie = new AhoCorasickTrieBuilder<String>()
			.build(map);

		Assertions.assertEquals(0, trie.searchInText("abe").size());
		Assertions.assertEquals(0, trie.searchInText("abd").size());
	}

	@Test
	void searchForLeafNodesWhenOverlapExists(){
		Map<String, String> map = new HashMap<>();
		map.put("abcd", "abcd");
		map.put("abce", "abce");
		AhoCorasickTrie<String> trie = new AhoCorasickTrieBuilder<String>()
			.build(map);

		Assertions.assertEquals(1, trie.searchInText("abcd").size());
		Assertions.assertEquals(1, trie.searchInText("abce").size());
	}

	@Test
	void searchForStringSmallerThanSharedParentWhenOverlapExists(){
		Map<String, String> map = new HashMap<>();
		map.put("abcd", "abcd");
		map.put("abce", "abce");
		AhoCorasickTrie<String> trie = new AhoCorasickTrieBuilder<String>()
			.build(map);

		Assertions.assertTrue(trie.searchInText("ab").isEmpty());
	}

	@Test
	void searchForStringEqualToSharedParentWhenOverlapExists(){
		Map<String, String> map = new HashMap<>();
		map.put("abcd", "abcd");
		map.put("abce", "abce");
		AhoCorasickTrie<String> trie = new AhoCorasickTrieBuilder<String>()
			.build(map);

		Assertions.assertTrue(trie.searchInText("abc").isEmpty());
	}

	@Test
	void size(){
		Map<String, String> map = new HashMap<>();
		map.put("apple", "apple");
		map.put("appleshack", "appleshack");
		map.put("appleshackcream", "appleshackcream");
		map.put("applepie", "applepie");
		map.put("ape", "ape");
		AhoCorasickTrie<String> trie = new AhoCorasickTrieBuilder<String>()
			.build(map);

		Assertions.assertEquals(5, trie.size());
	}

}
