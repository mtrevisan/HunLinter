package unit731.hunspeller.collections.radixtree;

import org.junit.Assert;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;


public class RadixTreeTest{

	private final SecureRandom rng = new SecureRandom();


	@Test
	public void testLargestPrefix(){
		Assert.assertEquals(5, RadixTreeUtil.largestPrefixLength("abcdefg", "abcdexyz"));
		Assert.assertEquals(3, RadixTreeUtil.largestPrefixLength("abcdefg", "abcxyz"));
		Assert.assertEquals(3, RadixTreeUtil.largestPrefixLength("abcdefg", "abctuvxyz"));
		Assert.assertEquals(0, RadixTreeUtil.largestPrefixLength("abcdefg", ""));
		Assert.assertEquals(0, RadixTreeUtil.largestPrefixLength("", "abcxyz"));
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
		assertEqualsWithSort(tree.getValuesWithPrefix(""), new ArrayList<>(tree.values()));
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
