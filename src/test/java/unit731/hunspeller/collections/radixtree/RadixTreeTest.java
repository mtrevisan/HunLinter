package unit731.hunspeller.collections.radixtree;

import org.junit.Assert;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;


public class RadixTreeTest{

	private final SecureRandom rng = new SecureRandom();


	@Test
	public void testLargestPrefix(){
		Assert.assertEquals(5, RadixTreeUtil.largestPrefixLength("abcdefg", "abcdexyz"));
		Assert.assertEquals(3, RadixTreeUtil.largestPrefixLength("abcdefg", "abcxyz"));
		Assert.assertEquals(3, RadixTreeUtil.largestPrefixLength("abcdefg", "abctuvxyz"));
		Assert.assertEquals(0, RadixTreeUtil.largestPrefixLength("abcdefg", StringUtils.EMPTY));
		Assert.assertEquals(0, RadixTreeUtil.largestPrefixLength(StringUtils.EMPTY, "abcxyz"));
		Assert.assertEquals(0, RadixTreeUtil.largestPrefixLength("xyz", "abcxyz"));
	}

	@Test
	public void testEmptyTree(){
		RadixTree<Integer> tree = new RadixTree<>();

		Assert.assertEquals(0, tree.size());
	}

	@Test
	public void testSingleInsertion(){
		RadixTree<Integer> tree = new RadixTree<>();

		tree.put("test", 1);

		Assert.assertEquals(1, tree.size());
		Assert.assertTrue(tree.containsKey("test"));
		Assert.assertFalse(tree.containsKey("tes"));
		Assert.assertFalse(tree.containsKey("testt"));
	}

	@Test
	public void testMultipleInsertions(){
		RadixTree<Integer> tree = new RadixTree<>();

		tree.put("test", 1);
		tree.put("tent", 2);
		tree.put("tank", 3);
		tree.put("rest", 4);

		Assert.assertEquals(4, tree.size());
		Assert.assertEquals(1, tree.get("test").intValue());
		Assert.assertEquals(2, tree.get("tent").intValue());
		Assert.assertEquals(3, tree.get("tank").intValue());
		Assert.assertEquals(4, tree.get("rest").intValue());
	}

	@Test
	public void testMultipleInsertionOfTheSameKey(){
		RadixTree<Integer> tree = new RadixTree<>();

		tree.put("test", 1);
		tree.put("tent", 2);
		tree.put("tank", 3);
		tree.put("rest", 4);

		Assert.assertEquals(4, tree.size());
		Assert.assertEquals(1, tree.get("test").intValue());
		Assert.assertEquals(2, tree.get("tent").intValue());
		Assert.assertEquals(3, tree.get("tank").intValue());
		Assert.assertEquals(4, tree.get("rest").intValue());

		tree.put("test", 9);

		Assert.assertEquals(4, tree.size());
		Assert.assertEquals(9, tree.get("test").intValue());
		Assert.assertEquals(2, tree.get("tent").intValue());
		Assert.assertEquals(3, tree.get("tank").intValue());
		Assert.assertEquals(4, tree.get("rest").intValue());
	}

	@Test
	public void testPrefixFetch(){
		RadixTree<Integer> tree = new RadixTree<>();

		tree.put("test", 1);
		tree.put("tent", 2);
		tree.put("rest", 3);
		tree.put("tank", 4);

		Assert.assertEquals(4, tree.size());
		assertEqualsWithSort(tree.getValuesWithPrefix(StringUtils.EMPTY), new ArrayList<>(tree.values()));
		assertEqualsWithSort(tree.getValuesWithPrefix("t").toArray(new Integer[0]), new Integer[]{1, 2, 4});
		assertEqualsWithSort(tree.getValuesWithPrefix("te").toArray(new Integer[0]), new Integer[]{1, 2});
		assertEqualsWithSort(tree.getValuesWithPrefix("asd").toArray(new Integer[0]), new Integer[0]);
	}

	@Test
	public void testSpook(){
		RadixTree<Integer> tree = new RadixTree<>();

		tree.put("pook", 1);
		tree.put("spook", 2);

		Assert.assertEquals(2, tree.size());
		assertEqualsWithSort(tree.keySet().toArray(new String[0]), new String[]{
			"pook",
			"spook"
		});
	}

	@Test
	public void testRemoval(){
		RadixTree<Integer> tree = new RadixTree<>();

		tree.put("test", 1);
		tree.put("tent", 2);
		tree.put("tank", 3);

		Assert.assertEquals(3, tree.size());
		Assert.assertTrue(tree.containsKey("tent"));

		tree.remove("key");

		Assert.assertEquals(3, tree.size());
		Assert.assertTrue(tree.containsKey("tent"));

		tree.remove("tent");

		Assert.assertEquals(2, tree.size());
		Assert.assertEquals(1, tree.get("test").intValue());
		Assert.assertFalse(tree.containsKey("tent"));
		Assert.assertEquals(3, tree.get("tank").intValue());
	}

	@Test
	public void testManyInsertions(){
		RadixTree<BigInteger> tree = new RadixTree<>();

		//n in [100, 500]
		int n = rng.nextInt(401) + 100;

		List<BigInteger> strings = generateRandomStrings(n);
		strings.forEach(x -> tree.put(x.toString(32), x));

		Assert.assertEquals(strings.size(), tree.size());
		strings.forEach(x -> Assert.assertTrue(tree.containsKey(x.toString(32))));
		assertEqualsWithSort(strings, new ArrayList<>(tree.values()));
	}

	private List<BigInteger> generateRandomStrings(int n){
		List<BigInteger> strings = new ArrayList<>();
		while(n -- > 0){
			BigInteger bigint = new BigInteger(20, rng);
			if(!strings.contains(bigint))
				strings.add(bigint);
		}
		return strings;
	}


	@Test
	public void contains(){
		RadixTree<Integer> tree = new RadixTree<>();

		tree.put("abc", 1);
		tree.put("abb", 2);
		tree.put("ac", 3);
		tree.put("a", 4);

		Assert.assertTrue(tree.containsKey("a"));
		Assert.assertFalse(tree.containsKey("ab"));
		Assert.assertFalse(tree.containsKey("c"));
	}

	@Test
	public void duplicatedEntry(){
		RadixTree<Integer> tree = new RadixTree<>();

		tree.put("abc", 1);
		tree.put("abc", 2);

		Assert.assertFalse(tree.containsKey("a"));
		Assert.assertFalse(tree.containsKey("ab"));
		Assert.assertTrue(tree.containsKey("abc"));
		Assert.assertEquals(2, tree.get("abc").intValue());
		Assert.assertFalse(tree.containsKey("c"));
	}

	@Test
	public void collectPrefixes(){
		RadixTree<Integer> tree = new RadixTree<>();

		tree.put("a", 1);
		tree.put("ab", 2);
		tree.put("bc", 3);
		tree.put("cd", 4);
		tree.put("abc", 5);

		List<Integer> prefixes = tree.getValuesWithPrefix("abcd");
		Integer[] datas = prefixes.stream()
			.sorted()
			.toArray(Integer[]::new);
		Assert.assertArrayEquals(new Integer[]{1, 2, 5}, datas);
	}

	@Test
	public void emptyConstructor(){
		RadixTree<Integer> tree = new RadixTree<>();

		Assert.assertTrue(tree.isEmpty());
		Assert.assertFalse(tree.containsKey("word"));
		Assert.assertFalse(tree.containsKey(StringUtils.EMPTY));
	}

	@Test
	public void defaultValueConstructor(){
		RadixTree<Boolean> tree = new RadixTree<>();

		Assert.assertNull(tree.get("meow"));

		tree.put("meow", Boolean.TRUE);

		Assert.assertEquals(Boolean.TRUE, tree.get("meow"));
		Assert.assertNull(tree.get("world"));
	}

	@Test
	public void simplePut(){
		RadixTree<Boolean> tree = new RadixTree<>();

		Assert.assertTrue(tree.isEmpty());

		tree.put("java.lang.", Boolean.TRUE);
		tree.put("java.i", Boolean.TRUE);
		tree.put("java.io.", Boolean.TRUE);
		tree.put("java.util.concurrent.", Boolean.TRUE);
		tree.put("java.util.", Boolean.FALSE);
		tree.put("java.lang.Boolean", Boolean.FALSE);

		Assert.assertFalse(tree.isEmpty());
		Assert.assertEquals(1, tree.getValuesWithPrefix("java.lang.Integer").size());
		Assert.assertEquals(1, tree.getValuesWithPrefix("java.lang.Long").size());
		Assert.assertEquals(2, tree.getValuesWithPrefix("java.lang.Boolean").size());
		Assert.assertEquals(2, tree.getValuesWithPrefix("java.io.InputStream").size());
		Assert.assertEquals(1, tree.getValuesWithPrefix("java.util.ArrayList").size());
		Assert.assertEquals(2, tree.getValuesWithPrefix("java.util.concurrent.ConcurrentHashMap").size());
	}

	@Test
	public void hasStartsWithMatch(){
		RadixTree<Boolean> tree = new RadixTree<>();

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assert.assertEquals(1, tree.getValuesWithPrefix("wowzacowza").size());
	}

	@Test
	public void hasExactMatch(){
		RadixTree<Boolean> tree = new RadixTree<>();

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assert.assertTrue(tree.containsKey("wowza"));
	}

	@Test
	public void getStartsWithMatch(){
		RadixTree<Boolean> tree = new RadixTree<>();

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assert.assertEquals(1, tree.getValuesWithPrefix("wowzacowza").size());
		Assert.assertEquals(1, tree.getValuesWithPrefix("bookshelfmania").size());
	}

	@Test
	public void getExactMatch(){
		RadixTree<Boolean> tree = new RadixTree<>();

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assert.assertEquals(Boolean.FALSE, tree.get("wowza"));
		Assert.assertEquals(Boolean.TRUE, tree.get("bookshelf"));
		Assert.assertNull(tree.get("bookshelf2"));
	}

	@Test
	public void removeBack(){
		RadixTree<Integer> tree = new RadixTree<>();

		tree.put("hello", 0);
		tree.put("hello world", 1);

		Assert.assertEquals(0, tree.get("hello").intValue());
		Assert.assertEquals(1, tree.get("hello world").intValue());

		Integer r1 = tree.remove("hello world");

		Assert.assertNotNull(r1);
		Assert.assertEquals(1, r1.intValue());

		Assert.assertEquals(0, tree.get("hello").intValue());
		Assert.assertNull(tree.get("hello world"));
	}

	@Test
	public void removeFront(){
		RadixTree<Integer> tree = new RadixTree<>();

		tree.put("hello", 0);
		tree.put("hello world", 1);

		Assert.assertEquals(0, tree.get("hello").intValue());
		Assert.assertEquals(1, tree.get("hello world").intValue());

		Integer r0 = tree.remove("hello world");

		Assert.assertNotNull(r0);
		Assert.assertEquals(1, r0.intValue());

		Assert.assertEquals(0, tree.get("hello").intValue());
		Assert.assertNull(tree.get("hello world"));
	}

	@Test
	public void removeFrontManyChildren(){
		RadixTree<Integer> tree = new RadixTree<>();

		tree.put("hello", 0);
		tree.put("hello world", 1);
		tree.put("hello, clarice", 2);

		Assert.assertEquals(0, tree.get("hello").intValue());
		Assert.assertEquals(1, tree.get("hello world").intValue());
		Assert.assertEquals(2, tree.get("hello, clarice").intValue());

		Integer r0 = tree.remove("hello world");

		Assert.assertNotNull(r0);
		Assert.assertEquals(1, r0.intValue());

		Assert.assertNull(tree.get("hello world"));
		Assert.assertEquals(0, tree.get("hello").intValue());
		Assert.assertEquals(2, tree.get("hello, clarice").intValue());
	}


	public static <T extends Comparable<? super T>> void assertEqualsWithSort(List<T> a, List<T> b){
		Collections.sort(a);
		Collections.sort(b);
		Assert.assertEquals(a, b);
	}

	public static <T extends Comparable<? super T>> void assertEqualsWithSort(T[] a, T[] b){
		Arrays.sort(a);
		Arrays.sort(b);
		Assert.assertArrayEquals(a, b);
	}

}
