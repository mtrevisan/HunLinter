package unit731.hunspeller.collections.trie;

import java.util.Collection;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;


public class TrieTest{

	@Test
	public void contains(){
		Trie<Integer> trie = new Trie<>();

		trie.put("abc", 1);
		trie.put("abb", 2);
		trie.put("ac", 3);
		trie.put("a", 4);

		Assert.assertTrue(trie.containsKey("a"));
		Assert.assertFalse(trie.containsKey("ab"));
		Assert.assertFalse(trie.containsKey("c"));
	}

	@Test
	public void collectPrefixes(){
		Trie<Integer> trie = new Trie<>();

		trie.put("a", 1);
		trie.put("ab", 2);
		trie.put("bc", 3);
		trie.put("cd", 4);
		trie.put("abc", 5);

		Collection<TrieNode<Integer>> prefixes = trie.collectPrefixes("abcd");
		Integer[] datas = prefixes.stream()
			.map(TrieNode::getValue)
			.toArray(Integer[]::new);
		Assert.assertArrayEquals(new Integer[]{1, 2, 5}, datas);
	}

	@Test
	public void emptyConstructor(){
		Trie<Integer> trie = new Trie<>();

		Assert.assertTrue(trie.isEmpty());
		Assert.assertFalse(trie.containsKey("word"));
		Assert.assertFalse(trie.containsKey(StringUtils.EMPTY));
	}

	@Test
	public void defaultValueConstructor(){
		Trie<Boolean> trie = new Trie<>();

		Assert.assertEquals(null, trie.get("meow"));

		trie.put("meow", Boolean.TRUE);

		Assert.assertEquals(Boolean.TRUE, trie.get("meow"));
		Assert.assertEquals(null, trie.get("world"));
	}

	@Test
	public void simplePut(){
		Trie<Boolean> trie = new Trie<>();

		Assert.assertTrue(trie.isEmpty());

		trie.put("java.lang.", Boolean.TRUE);
		trie.put("java.i", Boolean.TRUE);
		trie.put("java.io.", Boolean.TRUE);
		trie.put("java.util.concurrent.", Boolean.TRUE);
		trie.put("java.util.", Boolean.FALSE);
		trie.put("java.lang.Boolean", Boolean.FALSE);

		Assert.assertFalse(trie.isEmpty());
		Assert.assertTrue(trie.collectPrefixes("java.lang.Integer").iterator().hasNext());
		Assert.assertTrue(trie.collectPrefixes("java.lang.Long").iterator().hasNext());
		Assert.assertTrue(trie.collectPrefixes("java.lang.Boolean").iterator().hasNext());
		Assert.assertTrue(trie.collectPrefixes("java.io.InputStream").iterator().hasNext());
		Assert.assertTrue(trie.collectPrefixes("java.util.ArrayList").iterator().hasNext());
		Assert.assertTrue(trie.collectPrefixes("java.util.concurrent.ConcurrentHashMap").iterator().hasNext());
	}

	@Test
	public void hasStartsWithMatch(){
		Trie<Boolean> trie = new Trie<>();

		trie.put("bookshelf", Boolean.TRUE);
		trie.put("wowza", Boolean.FALSE);

		Assert.assertTrue(trie.collectPrefixes("wowzacowza").iterator().hasNext());
	}

	@Test
	public void hasExactMatch(){
		Trie<Boolean> trie = new Trie<>();

		trie.put("bookshelf", Boolean.TRUE);
		trie.put("wowza", Boolean.FALSE);

		Assert.assertTrue(trie.containsKey("wowza"));
	}

	@Test
	public void getStartsWithMatch(){
		Trie<Boolean> trie = new Trie<>();

		trie.put("bookshelf", Boolean.TRUE);
		trie.put("wowza", Boolean.FALSE);

		Assert.assertTrue(trie.collectPrefixes("wowzacowza").iterator().hasNext());
		Assert.assertTrue(trie.collectPrefixes("bookshelfmania").iterator().hasNext());
	}

	@Test
	public void getExactMatch(){
		Trie<Boolean> trie = new Trie<>();

		trie.put("bookshelf", Boolean.TRUE);
		trie.put("wowza", Boolean.FALSE);

		Assert.assertEquals(Boolean.FALSE, trie.get("wowza"));
		Assert.assertEquals(Boolean.TRUE, trie.get("bookshelf"));
		Assert.assertNull(trie.get("bookshelf2"));
	}

	@Test
	public void removeBack(){
		Trie<Integer> trie = new Trie<>();

		trie.put("hello", 0);
		trie.put("hello world", 1);

		Assert.assertEquals(0, trie.get("hello").intValue());
		Assert.assertEquals(1, trie.get("hello world").intValue());

		Integer r1 = trie.remove("hello world");

		Assert.assertNotNull(r1);
		Assert.assertEquals(1, r1.intValue());

		Assert.assertEquals(0, trie.get("hello").intValue());
		Assert.assertNull(trie.get("hello world"));
	}

	@Test
	public void removeFront(){
		Trie<Integer> trie = new Trie<>();

		trie.put("hello", 0);
		trie.put("hello world", 1);

		Assert.assertEquals(0, trie.get("hello").intValue());
		Assert.assertEquals(1, trie.get("hello world").intValue());

		Integer r0 = trie.remove("hello world");

		Assert.assertNotNull(r0);
		Assert.assertEquals(1, r0.intValue());

		Assert.assertEquals(0, trie.get("hello").intValue());
		Assert.assertNull(trie.get("hello world"));
	}

	@Test
	public void removeFrontManyChildren(){
		Trie<Integer> trie = new Trie<>();

		trie.put("hello", 0);
		trie.put("hello world", 1);
		trie.put("hello, clarice", 2);

		Assert.assertEquals(0, trie.get("hello").intValue());
		Assert.assertEquals(1, trie.get("hello world").intValue());
		Assert.assertEquals(2, trie.get("hello, clarice").intValue());

		Integer r0 = trie.remove("hello world");

		Assert.assertNotNull(r0);
		Assert.assertEquals(1, r0.intValue());

		Assert.assertNull(trie.get("hello world"));
		Assert.assertEquals(0, trie.get("hello").intValue());
		Assert.assertEquals(2, trie.get("hello, clarice").intValue());
	}

}
