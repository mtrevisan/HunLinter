package unit731.hunspeller.collections.trie;

import java.util.Collection;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import unit731.hunspeller.collections.trie.sequencers.RegExpTrieSequencer;


public class RegExpTrieTest{

	@Test
	public void splitting(){
		String[] parts = RegExpTrieSequencer.extractCharacters("abd");
		Assert.assertArrayEquals(new String[]{"a", "b", "d"}, parts);

		parts = RegExpTrieSequencer.extractCharacters("a[b]d");
		Assert.assertArrayEquals(new String[]{"a", "[b]", "d"}, parts);

		parts = RegExpTrieSequencer.extractCharacters("a[bc]d");
		Assert.assertArrayEquals(new String[]{"a", "[bc]", "d"}, parts);

		parts = RegExpTrieSequencer.extractCharacters("a[^b]d");
		Assert.assertArrayEquals(new String[]{"a", "[^b]", "d"}, parts);

		parts = RegExpTrieSequencer.extractCharacters("a[^bc]d");
		Assert.assertArrayEquals(new String[]{"a", "[^bc]", "d"}, parts);
	}

	@Test
	public void contains(){
		Trie<String[], String, Integer> trie = new Trie<>(new RegExpTrieSequencer());

		trie.put(RegExpTrieSequencer.extractCharacters("abc"), 1);
		trie.put(RegExpTrieSequencer.extractCharacters("abb"), 2);
		trie.put(RegExpTrieSequencer.extractCharacters("ac"), 3);
		trie.put(RegExpTrieSequencer.extractCharacters("a"), 4);

		Assert.assertTrue(trie.containsKey(RegExpTrieSequencer.extractCharacters("a")));
		Assert.assertFalse(trie.containsKey(RegExpTrieSequencer.extractCharacters("ab")));
		Assert.assertFalse(trie.containsKey(RegExpTrieSequencer.extractCharacters("c")));
	}

	@Test
	public void containsRegExp(){
		Trie<String[], String, Integer> trie = new Trie<>(new RegExpTrieSequencer());

		trie.put(RegExpTrieSequencer.extractCharacters("a[bd]c"), 1);
		trie.put(RegExpTrieSequencer.extractCharacters("a[bd]b"), 2);
		trie.put(RegExpTrieSequencer.extractCharacters("a[^bcd]"), 3);
		trie.put(RegExpTrieSequencer.extractCharacters("a"), 4);

		Assert.assertTrue(trie.containsKey(RegExpTrieSequencer.extractCharacters("a")));
		Assert.assertTrue(trie.containsKey(RegExpTrieSequencer.extractCharacters("abc")));
		Assert.assertTrue(trie.containsKey(RegExpTrieSequencer.extractCharacters("adc")));
		Assert.assertFalse(trie.containsKey(RegExpTrieSequencer.extractCharacters("aec")));
		Assert.assertTrue(trie.containsKey(RegExpTrieSequencer.extractCharacters("ae")));
		Assert.assertFalse(trie.containsKey(RegExpTrieSequencer.extractCharacters("c")));
	}

	@Test
	public void collectPrefixes(){
		Trie<String[], String, Integer> trie = new Trie<>(new RegExpTrieSequencer());

		trie.put(RegExpTrieSequencer.extractCharacters("a"), 1);
		trie.put(RegExpTrieSequencer.extractCharacters("ab"), 2);
		trie.put(RegExpTrieSequencer.extractCharacters("bc"), 3);
		trie.put(RegExpTrieSequencer.extractCharacters("cd"), 4);
		trie.put(RegExpTrieSequencer.extractCharacters("abc"), 5);

		Collection<TrieNode<String[], String, Integer>> prefixes = trie.collectPrefixes(RegExpTrieSequencer.extractCharacters("abcd"));
		Integer[] datas = prefixes.stream()
			.map(TrieNode::getValue)
			.toArray(Integer[]::new);
		Assert.assertArrayEquals(new Integer[]{1, 2, 5}, datas);
	}

	@Test
	public void collectPrefixesRegExp(){
		Trie<String[], String, Integer> trie = new Trie<>(new RegExpTrieSequencer());

		trie.put(RegExpTrieSequencer.extractCharacters("a"), 1);
		trie.put(RegExpTrieSequencer.extractCharacters("a[bcd]"), 2);
		trie.put(RegExpTrieSequencer.extractCharacters("[^ac]c"), 3);
		trie.put(RegExpTrieSequencer.extractCharacters("cd"), 4);
		trie.put(RegExpTrieSequencer.extractCharacters("aec"), 5);

		Collection<TrieNode<String[], String, Integer>> prefixes = trie.collectPrefixes(RegExpTrieSequencer.extractCharacters("abcd"));
		Integer[] datas = prefixes.stream()
			.map(TrieNode::getValue)
			.toArray(Integer[]::new);
		Assert.assertArrayEquals(new Integer[]{1, 2}, datas);
		prefixes = trie.collectPrefixes(RegExpTrieSequencer.extractCharacters("ec"));
		datas = prefixes.stream()
			.map(TrieNode::getValue)
			.toArray(Integer[]::new);
		Assert.assertArrayEquals(new Integer[]{3}, datas);
	}

	@Test
	public void emptyConstructor(){
		Trie<String[], String, Integer> trie = new Trie<>(new RegExpTrieSequencer());

		Assert.assertTrue(trie.isEmpty());
		Assert.assertFalse(trie.containsKey(RegExpTrieSequencer.extractCharacters("word")));
		Assert.assertFalse(trie.containsKey(RegExpTrieSequencer.extractCharacters(StringUtils.EMPTY)));
	}

	@Test
	public void defaultValueConstructor(){
		Trie<String[], String, Boolean> trie = new Trie<>(new RegExpTrieSequencer());

		Assert.assertNull(trie.get(RegExpTrieSequencer.extractCharacters("meow")));

		trie.put(RegExpTrieSequencer.extractCharacters("meow"), Boolean.TRUE);

		Assert.assertEquals(Boolean.TRUE, trie.get(RegExpTrieSequencer.extractCharacters("meow")));
		Assert.assertNull(trie.get(RegExpTrieSequencer.extractCharacters("world")));
	}

	@Test
	public void simplePut(){
		Trie<String[], String, Boolean> trie = new Trie<>(new RegExpTrieSequencer());

		Assert.assertTrue(trie.isEmpty());

		trie.put(RegExpTrieSequencer.extractCharacters("java.lang."), Boolean.TRUE);
		trie.put(RegExpTrieSequencer.extractCharacters("java.i"), Boolean.TRUE);
		trie.put(RegExpTrieSequencer.extractCharacters("java.io."), Boolean.TRUE);
		trie.put(RegExpTrieSequencer.extractCharacters("java.util.concurrent."), Boolean.TRUE);
		trie.put(RegExpTrieSequencer.extractCharacters("java.util."), Boolean.FALSE);
		trie.put(RegExpTrieSequencer.extractCharacters("java.lang.Boolean"), Boolean.FALSE);

		Assert.assertFalse(trie.isEmpty());
		Assert.assertTrue(trie.collectPrefixes(RegExpTrieSequencer.extractCharacters("java.lang.Integer")).iterator().hasNext());
		Assert.assertTrue(trie.collectPrefixes(RegExpTrieSequencer.extractCharacters("java.lang.Long")).iterator().hasNext());
		Assert.assertTrue(trie.collectPrefixes(RegExpTrieSequencer.extractCharacters("java.lang.Boolean")).iterator().hasNext());
		Assert.assertTrue(trie.collectPrefixes(RegExpTrieSequencer.extractCharacters("java.io.InputStream")).iterator().hasNext());
		Assert.assertTrue(trie.collectPrefixes(RegExpTrieSequencer.extractCharacters("java.util.ArrayList")).iterator().hasNext());
		Assert.assertTrue(trie.collectPrefixes(RegExpTrieSequencer.extractCharacters("java.util.concurrent.ConcurrentHashMap")).iterator().hasNext());
	}

	@Test
	public void hasStartsWithMatch(){
		Trie<String[], String, Boolean> trie = new Trie<>(new RegExpTrieSequencer());

		trie.put(RegExpTrieSequencer.extractCharacters("bookshelf"), Boolean.TRUE);
		trie.put(RegExpTrieSequencer.extractCharacters("wowza"), Boolean.FALSE);

		Assert.assertTrue(trie.collectPrefixes(RegExpTrieSequencer.extractCharacters("wowzacowza")).iterator().hasNext());
	}

	@Test
	public void hasExactMatch(){
		Trie<String[], String, Boolean> trie = new Trie<>(new RegExpTrieSequencer());

		trie.put(RegExpTrieSequencer.extractCharacters("bookshelf"), Boolean.TRUE);
		trie.put(RegExpTrieSequencer.extractCharacters("wowza"), Boolean.FALSE);

		Assert.assertTrue(trie.containsKey(RegExpTrieSequencer.extractCharacters("wowza")));
	}

	@Test
	public void getStartsWithMatch(){
		Trie<String[], String, Boolean> trie = new Trie<>(new RegExpTrieSequencer());

		trie.put(RegExpTrieSequencer.extractCharacters("bookshelf"), Boolean.TRUE);
		trie.put(RegExpTrieSequencer.extractCharacters("wowza"), Boolean.FALSE);

		Assert.assertTrue(trie.collectPrefixes(RegExpTrieSequencer.extractCharacters("wowzacowza")).iterator().hasNext());
		Assert.assertTrue(trie.collectPrefixes(RegExpTrieSequencer.extractCharacters("bookshelfmania")).iterator().hasNext());
	}

	@Test
	public void getExactMatch(){
		Trie<String[], String, Boolean> trie = new Trie<>(new RegExpTrieSequencer());

		trie.put(RegExpTrieSequencer.extractCharacters("bookshelf"), Boolean.TRUE);
		trie.put(RegExpTrieSequencer.extractCharacters("wowza"), Boolean.FALSE);

		Assert.assertEquals(Boolean.FALSE, trie.get(RegExpTrieSequencer.extractCharacters("wowza")));
		Assert.assertEquals(Boolean.TRUE, trie.get(RegExpTrieSequencer.extractCharacters("bookshelf")));
		Assert.assertNull(trie.get(RegExpTrieSequencer.extractCharacters("bookshelf2")));
	}

	@Test
	public void removeBack(){
		Trie<String[], String, Integer> trie = new Trie<>(new RegExpTrieSequencer());

		trie.put(RegExpTrieSequencer.extractCharacters("hello"), 0);
		trie.put(RegExpTrieSequencer.extractCharacters("hello world"), 1);

		Assert.assertEquals(0, trie.get(RegExpTrieSequencer.extractCharacters("hello")).intValue());
		Assert.assertEquals(1, trie.get(RegExpTrieSequencer.extractCharacters("hello world")).intValue());

		Integer r1 = trie.remove(RegExpTrieSequencer.extractCharacters("hello world"));

		Assert.assertNotNull(r1);
		Assert.assertEquals(1, r1.intValue());

		Assert.assertEquals(0, trie.get(RegExpTrieSequencer.extractCharacters("hello")).intValue());
		Assert.assertNull(trie.get(RegExpTrieSequencer.extractCharacters("hello world")));
	}

	@Test
	public void removeFront(){
		Trie<String[], String, Integer> trie = new Trie<>(new RegExpTrieSequencer());

		trie.put(RegExpTrieSequencer.extractCharacters("hello"), 0);
		trie.put(RegExpTrieSequencer.extractCharacters("hello world"), 1);

		Assert.assertEquals(0, trie.get(RegExpTrieSequencer.extractCharacters("hello")).intValue());
		Assert.assertEquals(1, trie.get(RegExpTrieSequencer.extractCharacters("hello world")).intValue());

		Integer r0 = trie.remove(RegExpTrieSequencer.extractCharacters("hello world"));

		Assert.assertNotNull(r0);
		Assert.assertEquals(1, r0.intValue());

		Assert.assertEquals(0, trie.get(RegExpTrieSequencer.extractCharacters("hello")).intValue());
		Assert.assertNull(trie.get(RegExpTrieSequencer.extractCharacters("hello world")));
	}

	@Test
	public void removeFrontManyChildren(){
		Trie<String[], String, Integer> trie = new Trie<>(new RegExpTrieSequencer());

		trie.put(RegExpTrieSequencer.extractCharacters("hello"), 0);
		trie.put(RegExpTrieSequencer.extractCharacters("hello world"), 1);
		trie.put(RegExpTrieSequencer.extractCharacters("hello, clarice"), 2);

		Assert.assertEquals(0, trie.get(RegExpTrieSequencer.extractCharacters("hello")).intValue());
		Assert.assertEquals(1, trie.get(RegExpTrieSequencer.extractCharacters("hello world")).intValue());
		Assert.assertEquals(2, trie.get(RegExpTrieSequencer.extractCharacters("hello, clarice")).intValue());

		Integer r0 = trie.remove(RegExpTrieSequencer.extractCharacters("hello world"));

		Assert.assertNotNull(r0);
		Assert.assertEquals(1, r0.intValue());

		Assert.assertNull(trie.get(RegExpTrieSequencer.extractCharacters("hello world")));
		Assert.assertEquals(0, trie.get(RegExpTrieSequencer.extractCharacters("hello")).intValue());
		Assert.assertEquals(2, trie.get(RegExpTrieSequencer.extractCharacters("hello, clarice")).intValue());
	}

}
