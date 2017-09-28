package unit731.hunspeller.collections.trie;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import unit731.hunspeller.collections.trie.sequencers.StringTrieSequencer;


public class ListOfStringTrieTest{

	@Test
	public void contains(){
		Trie<String, Integer, List<Integer>> trie = new Trie<>(new StringTrieSequencer());

		List<Integer> oneValue = new ArrayList<>();
		oneValue.add(1);
		trie.put("abc", oneValue);
		trie.put("abc", Arrays.asList(2));
		trie.put("abb", Arrays.asList(2));
		trie.put("ac", Arrays.asList(3));
		trie.put("a", Arrays.asList(4));

		Assert.assertTrue(trie.containsKey("a"));
		Assert.assertFalse(trie.containsKey("ab"));
		Assert.assertFalse(trie.containsKey("c"));
		Assert.assertEquals(2, trie.get("abc").size());
		Assert.assertEquals(Arrays.asList(1, 2), trie.get("abc").stream().map(Integer::intValue).sorted().collect(Collectors.toList()));
	}

	@Test
	public void duplicatedEntry(){
		Trie<String, Integer, List<Integer>> trie = new Trie<>(new StringTrieSequencer());

		List<Integer> oneValue = new ArrayList<>();
		oneValue.add(1);
		trie.put("abc", oneValue);
		trie.put("abc", Arrays.asList(2));

		Assert.assertFalse(trie.containsKey("a"));
		Assert.assertFalse(trie.containsKey("ab"));
		Assert.assertTrue(trie.containsKey("abc"));
		Assert.assertEquals(2, trie.get("abc").size());
		Assert.assertEquals(Arrays.asList(1, 2), trie.get("abc").stream().map(Integer::intValue).sorted().collect(Collectors.toList()));
		Assert.assertFalse(trie.containsKey("c"));
	}

	@Test
	public void collectPrefixes(){
		Trie<String, Integer, List<Integer>> trie = new Trie<>(new StringTrieSequencer());

		trie.put("a", Arrays.asList(1));
		trie.put("ab", Arrays.asList(2));
		trie.put("bc", Arrays.asList(3));
		trie.put("cd", Arrays.asList(4));
		trie.put("abc", Arrays.asList(5));

		Collection<TrieNode<String, Integer, List<Integer>>> prefixes = trie.collectPrefixes("abcd");
		Integer[] datas = prefixes.stream()
			.flatMap(p -> p.getValue().stream())
			.sorted()
			.toArray(Integer[]::new);
		Assert.assertArrayEquals(new Integer[]{1, 2, 5}, datas);
	}

	@Test
	public void emptyConstructor(){
		Trie<String, Integer, List<Integer>> trie = new Trie<>(new StringTrieSequencer());

		Assert.assertTrue(trie.isEmpty());
		Assert.assertFalse(trie.containsKey("word"));
		Assert.assertFalse(trie.containsKey(StringUtils.EMPTY));
	}

	@Test
	public void defaultValueConstructor(){
		Trie<String, Integer, List<Boolean>> trie = new Trie<>(new StringTrieSequencer());

		Assert.assertNull(trie.get("meow"));

		trie.put("meow", Arrays.asList(Boolean.TRUE));

		Assert.assertEquals(1, trie.get("meow").size());
		Assert.assertTrue(trie.get("meow").get(0));
		Assert.assertNull(trie.get("world"));
	}

	@Test
	public void simplePut(){
		Trie<String, Integer, List<Boolean>> trie = new Trie<>(new StringTrieSequencer());

		Assert.assertTrue(trie.isEmpty());

		trie.put("java.lang.", Arrays.asList(Boolean.TRUE));
		trie.put("java.i", Arrays.asList(Boolean.TRUE));
		trie.put("java.io.", Arrays.asList(Boolean.TRUE));
		trie.put("java.util.concurrent.", Arrays.asList(Boolean.TRUE));
		trie.put("java.util.", Arrays.asList(Boolean.FALSE));
		trie.put("java.lang.Boolean", Arrays.asList(Boolean.FALSE));

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
		Trie<String, Integer, List<Boolean>> trie = new Trie<>(new StringTrieSequencer());

		trie.put("bookshelf", Arrays.asList(Boolean.TRUE));
		trie.put("wowza", Arrays.asList(Boolean.FALSE));

		Assert.assertTrue(trie.collectPrefixes("wowzacowza").iterator().hasNext());
	}

	@Test
	public void hasExactMatch(){
		Trie<String, Integer, List<Boolean>> trie = new Trie<>(new StringTrieSequencer());

		trie.put("bookshelf", Arrays.asList(Boolean.TRUE));
		trie.put("wowza", Arrays.asList(Boolean.FALSE));

		Assert.assertTrue(trie.containsKey("wowza"));
	}

	@Test
	public void getStartsWithMatch(){
		Trie<String, Integer, List<Boolean>> trie = new Trie<>(new StringTrieSequencer());

		trie.put("bookshelf", Arrays.asList(Boolean.TRUE));
		trie.put("wowza", Arrays.asList(Boolean.FALSE));

		Assert.assertTrue(trie.collectPrefixes("wowzacowza").iterator().hasNext());
		Assert.assertTrue(trie.collectPrefixes("bookshelfmania").iterator().hasNext());
	}

	@Test
	public void getExactMatch(){
		Trie<String, Integer, List<Boolean>> trie = new Trie<>(new StringTrieSequencer());

		trie.put("bookshelf", Arrays.asList(Boolean.TRUE));
		trie.put("wowza", Arrays.asList(Boolean.FALSE));

		Assert.assertEquals(1, trie.get("wowza").size());
		Assert.assertFalse(trie.get("wowza").get(0));
		Assert.assertEquals(1, trie.get("bookshelf").size());
		Assert.assertTrue(trie.get("bookshelf").get(0));
		Assert.assertNull(trie.get("bookshelf2"));
	}

	@Test
	public void removeBack(){
		Trie<String, Integer, List<Integer>> trie = new Trie<>(new StringTrieSequencer());

		trie.put("hello", Arrays.asList(0));
		trie.put("hello world", Arrays.asList(1));

		Assert.assertEquals(1, trie.get("hello").size());
		Assert.assertEquals(0, trie.get("hello").get(0).intValue());
		Assert.assertEquals(1, trie.get("hello world").size());
		Assert.assertEquals(1, trie.get("hello world").get(0).intValue());

		List<Integer> r1 = trie.remove("hello world");

		Assert.assertNotNull(r1);
		Assert.assertEquals(1, r1.size());
		Assert.assertEquals(1, r1.get(0).intValue());

		Assert.assertEquals(1, trie.get("hello").size());
		Assert.assertEquals(0, trie.get("hello").get(0).intValue());
		Assert.assertNull(trie.get("hello world"));
	}

	@Test
	public void removeFront(){
		Trie<String, Integer, List<Integer>> trie = new Trie<>(new StringTrieSequencer());

		trie.put("hello", Arrays.asList(0));
		trie.put("hello world", Arrays.asList(1));

		Assert.assertEquals(1, trie.get("hello").size());
		Assert.assertEquals(0, trie.get("hello").get(0).intValue());
		Assert.assertEquals(1, trie.get("hello world").size());
		Assert.assertEquals(1, trie.get("hello world").get(0).intValue());

		List<Integer> r0 = trie.remove("hello world");

		Assert.assertNotNull(r0);
		Assert.assertEquals(1, r0.size());
		Assert.assertEquals(1, r0.get(0).intValue());

		Assert.assertEquals(1, trie.get("hello").size());
		Assert.assertEquals(0, trie.get("hello").get(0).intValue());
		Assert.assertNull(trie.get("hello world"));
	}

	@Test
	public void removeFrontManyChildren(){
		Trie<String, Integer, List<Integer>> trie = new Trie<>(new StringTrieSequencer());

		trie.put("hello", Arrays.asList(0));
		trie.put("hello world", Arrays.asList(1));
		trie.put("hello, clarice", Arrays.asList(2));

		Assert.assertEquals(1, trie.get("hello").size());
		Assert.assertEquals(0, trie.get("hello").get(0).intValue());
		Assert.assertEquals(1, trie.get("hello world").size());
		Assert.assertEquals(1, trie.get("hello world").get(0).intValue());
		Assert.assertEquals(1, trie.get("hello, clarice").size());
		Assert.assertEquals(2, trie.get("hello, clarice").get(0).intValue());

		List<Integer> r0 = trie.remove("hello world");

		Assert.assertNotNull(r0);
		Assert.assertEquals(1, r0.size());
		Assert.assertEquals(1, r0.get(0).intValue());

		Assert.assertNull(trie.get("hello world"));
		Assert.assertEquals(1, trie.get("hello").size());
		Assert.assertEquals(0, trie.get("hello").get(0).intValue());
		Assert.assertEquals(1, trie.get("hello, clarice").size());
		Assert.assertEquals(2, trie.get("hello, clarice").get(0).intValue());
	}

}
