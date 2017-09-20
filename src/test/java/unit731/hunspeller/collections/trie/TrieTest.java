package unit731.hunspeller.collections.trie;

import java.util.stream.StreamSupport;
import org.apache.commons.lang3.StringUtils;
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

		Assert.assertTrue(trie.containsKey("a"));
		Assert.assertFalse(trie.containsKey("ab"));
		Assert.assertFalse(trie.containsKey("c"));
	}

	@Test
	public void collectPrefixes(){
		trie.clear();

		trie.put("a", 1);
		trie.put("ab", 2);
		trie.put("bc", 3);
		trie.put("cd", 4);
		trie.put("abc", 5);

		Iterable<Prefix<Integer>> prefixes = trie.collectPrefixes("abcd");
		Integer[] datas = StreamSupport.stream(prefixes.spliterator(), false)
			.map(Prefix::getNode)
			.map(TrieNode::getValue)
			.toArray(Integer[]::new);
		Assert.assertArrayEquals(new Integer[]{1, 2, 5}, datas);
	}

	@Test
	public void emptyConstructor(){
		Trie<Boolean> t = new Trie<>();

		Assert.assertTrue(t.isEmpty());
		Assert.assertFalse(t.containsKey("word"));
		Assert.assertFalse(t.containsKey(StringUtils.EMPTY));
	}

	@Test
	public void defaultValueConstructor(){
		Trie<Boolean> t = new Trie<>();

		Assert.assertEquals(null, t.get("meow"));

		t.put("meow", Boolean.TRUE);

		Assert.assertEquals(Boolean.TRUE, t.get("meow"));
		Assert.assertEquals(null, t.get("world"));
	}

	@Test
	public void simplePut(){
		Trie<Boolean> t = new Trie<>();

		Assert.assertTrue(t.isEmpty());

		t.put("java.lang.", Boolean.TRUE);
		t.put("java.io.", Boolean.TRUE);
		t.put("java.util.concurrent.", Boolean.TRUE);
		t.put("java.util.", Boolean.FALSE);
		t.put("java.lang.Boolean", Boolean.FALSE);

		Assert.assertFalse(t.isEmpty());
		Assert.assertTrue(t.collectPrefixes("java.lang.Integer").iterator().hasNext());
		Assert.assertTrue(t.collectPrefixes("java.lang.Long").iterator().hasNext());
		Assert.assertTrue(t.collectPrefixes("java.lang.Boolean").iterator().hasNext());
		Assert.assertTrue(t.collectPrefixes("java.io.InputStream").iterator().hasNext());
		Assert.assertTrue(t.collectPrefixes("java.util.ArrayList").iterator().hasNext());
		Assert.assertTrue(t.collectPrefixes("java.util.concurrent.ConcurrentHashMap").iterator().hasNext());
	}

	@Test
	public void hasPartialMatch(){
		Trie<Boolean> t = new Trie<>();

		t.put("bookshelf", Boolean.TRUE);
		t.put("wowza", Boolean.FALSE);

		Assert.assertFalse(t.collectPrefixes("wow").iterator().hasNext());
		Assert.assertFalse(t.collectPrefixes("book").iterator().hasNext());
	}

	@Test
	public void hasStartsWithMatch(){
		Trie<Boolean> t = new Trie<>();

		t.put("bookshelf", Boolean.TRUE);
		t.put("wowza", Boolean.FALSE);

		Assert.assertTrue(t.collectPrefixes("wowzacowza").iterator().hasNext());
	}

	@Test
	public void hasExactMatch(){
		Trie<Boolean> t = new Trie<>();

		t.put("bookshelf", Boolean.TRUE);
		t.put("wowza", Boolean.FALSE);

		Assert.assertTrue(t.collectPrefixes("wowza").iterator().hasNext());
	}

	@Test
	public void getPartialMatch(){
		Trie<Boolean> t = new Trie<>();

		t.put("bookshelf", Boolean.TRUE);
		t.put("wowza", Boolean.FALSE);

		Assert.assertFalse(t.collectPrefixes("wow").iterator().hasNext());
		Assert.assertFalse(t.collectPrefixes("book").iterator().hasNext());
	}

	@Test
	public void getStartsWithMatch(){
		Trie<Boolean> t = new Trie<>();

		t.put("bookshelf", Boolean.TRUE);
		t.put("wowza", Boolean.FALSE);

		Assert.assertTrue(t.collectPrefixes("wowzacowza").iterator().hasNext());
		Assert.assertTrue(t.collectPrefixes("bookshelfmania").iterator().hasNext());
	}

	@Test
	public void getExactMatch(){
		Trie<Boolean> t = new Trie<>();

		t.put("bookshelf", Boolean.TRUE);
		t.put("wowza", Boolean.FALSE);

		Assert.assertEquals(Boolean.FALSE, t.get("wowza"));
		Assert.assertEquals(Boolean.TRUE, t.get("bookshelf"));
	}

	@Test
	public void removeBack(){
		Trie<Integer> t = new Trie<>();

		t.put("hello", 0);
		t.put("hello world", 1);

		Assert.assertEquals(0, t.get("hello").intValue());
		Assert.assertEquals(1, t.get("hello world").intValue());

		Integer r1 = t.remove("hello world");

		Assert.assertNotNull(r1);
		Assert.assertEquals(1, r1.intValue());

		Assert.assertEquals(0, t.get("hello").intValue());
		Assert.assertNull(t.get("hello world"));
	}

	@Test
	public void removeFront(){
		Trie<Integer> t = new Trie<>();

		t.put("hello", 0);
		t.put("hello world", 1);

		Assert.assertEquals(0, t.get("hello").intValue());
		Assert.assertEquals(1, t.get("hello world").intValue());

		Integer r0 = t.remove("hello world");

		Assert.assertNotNull(r0);
		Assert.assertEquals(1, r0.intValue());

		Assert.assertEquals(0, t.get("hello").intValue());
		Assert.assertNull(t.get("hello world"));
	}

	@Test
	public void removeFrontManyChildren(){
		Trie<Integer> t = new Trie<>();

		t.put("hello", 0);
		t.put("hello world", 1);
		t.put("hello, clarice", 2);

		Assert.assertEquals(0, t.get("hello").intValue());
		Assert.assertEquals(1, t.get("hello world").intValue());
		Assert.assertEquals(2, t.get("hello, clarice").intValue());

		Integer r0 = t.remove("hello world");

		Assert.assertNotNull(r0);
		Assert.assertEquals(1, r0.intValue());

		Assert.assertNull(t.get("hello world"));
		Assert.assertEquals(0, t.get("hello").intValue());
		Assert.assertEquals(2, t.get("hello, clarice").intValue());
	}

}
