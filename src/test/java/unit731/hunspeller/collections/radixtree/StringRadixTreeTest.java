package unit731.hunspeller.collections.radixtree;

import unit731.hunspeller.collections.radixtree.tree.RadixTreeNode;
import unit731.hunspeller.collections.radixtree.tree.RadixTree;
import unit731.hunspeller.collections.radixtree.tree.DuplicateKeyException;
import org.junit.Assert;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import unit731.hunspeller.collections.radixtree.sequencers.StringSequencer;


public class StringRadixTreeTest{

	private final SecureRandom rng = new SecureRandom();


	@Test
	public void testEmptyTree(){
		RadixTree<String, Integer> tree = RadixTree.createTree(new StringSequencer());

		Assert.assertTrue(tree.isEmpty());
		Assert.assertEquals(0, tree.size());
	}

	@Test
	public void testSingleInsertion(){
		RadixTree<String, Integer> tree = RadixTree.createTree(new StringSequencer());

		tree.put("test", 1);

		Assert.assertEquals(1, tree.size());
		Assert.assertTrue(tree.containsKey("test"));
		Assert.assertFalse(tree.containsKey("tes"));
		Assert.assertFalse(tree.containsKey("testt"));
	}

	@Test
	public void testMultipleInsertions(){
		RadixTree<String, Integer> tree = RadixTree.createTree(new StringSequencer());

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
	public void testPrepare(){
		RadixTree<String, Integer> tree = RadixTree.createTree(new StringSequencer());

		tree.put("test", 1);
		tree.put("tent", 2);
		tree.put("tentest", 21);
		tree.put("tank", 3);
		tree.put("rest", 4);
		tree.prepare();

		Assert.assertEquals(5, tree.size());
		Assert.assertEquals(1, tree.get("test").intValue());
		Assert.assertEquals(2, tree.get("tent").intValue());
		Assert.assertEquals(3, tree.get("tank").intValue());
		Assert.assertEquals(4, tree.get("rest").intValue());

		Iterator<RadixTreeNode<String, Integer>> itr = tree.search("resting in the tent");
		Assert.assertTrue(itr.hasNext());
		Assert.assertEquals(4, itr.next().getValue().intValue());
		Assert.assertEquals(2, itr.next().getValue().intValue());
		Assert.assertFalse(itr.hasNext());
	}

	@Test
	public void testMultipleInsertionOfTheSameKey(){
		RadixTree<String, Integer> tree = RadixTree.createTree(new StringSequencer());

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
		RadixTree<String, Integer> tree = RadixTree.createTree(new StringSequencer());

		tree.put("test", 1);
		tree.put("tent", 2);
		tree.put("rest", 3);
		tree.put("tank", 4);

		Assert.assertEquals(4, tree.size());
		assertEqualsWithSort(tree.getValuesPrefixedBy(StringUtils.EMPTY), new ArrayList<>(tree.values()));
		assertEqualsWithSort(tree.getValuesPrefixedBy("t").toArray(new Integer[0]), new Integer[]{1, 2, 4});
		assertEqualsWithSort(tree.getValuesPrefixedBy("te").toArray(new Integer[0]), new Integer[]{1, 2});
		assertEqualsWithSort(tree.getValuesPrefixedBy("asd").toArray(new Integer[0]), new Integer[0]);
	}

	@Test
	public void testSpook(){
		RadixTree<String, Integer> tree = RadixTree.createTree(new StringSequencer());

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
		RadixTree<String, Integer> tree = RadixTree.createTree(new StringSequencer());

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
		RadixTree<String, BigInteger> tree = RadixTree.createTree(new StringSequencer());

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
		RadixTree<String, Integer> tree = RadixTree.createTree(new StringSequencer());

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
		RadixTree<String, Integer> tree = RadixTree.createTree(new StringSequencer());

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
		RadixTree<String, Integer> tree = RadixTree.createTree(new StringSequencer());

		tree.put("a", 1);
		tree.put("ab", 2);
		tree.put("bc", 3);
		tree.put("cd", 4);
		tree.put("abc", 5);

		List<Integer> prefixes = tree.getValues("abcd");
		Integer[] datas = prefixes.stream()
			.sorted()
			.toArray(Integer[]::new);
		Assert.assertArrayEquals(new Integer[]{1, 2, 5}, datas);
	}

	@Test
	public void emptyConstructor(){
		RadixTree<String, Integer> tree = RadixTree.createTree(new StringSequencer());

		Assert.assertTrue(tree.isEmpty());
		Assert.assertFalse(tree.containsKey("word"));
		Assert.assertFalse(tree.containsKey(StringUtils.EMPTY));
	}

	@Test
	public void defaultValueConstructor(){
		RadixTree<String, Boolean> tree = RadixTree.createTree(new StringSequencer());

		Assert.assertNull(tree.get("meow"));

		tree.put("meow", Boolean.TRUE);

		Assert.assertEquals(Boolean.TRUE, tree.get("meow"));
		Assert.assertNull(tree.get("world"));
	}

	@Test
	public void simplePut(){
		RadixTree<String, Boolean> tree = RadixTree.createTree(new StringSequencer());

		Assert.assertTrue(tree.isEmpty());

		tree.put("java.lang.", Boolean.TRUE);
		tree.put("java.i", Boolean.TRUE);
		tree.put("java.io.", Boolean.TRUE);
		tree.put("java.util.concurrent.", Boolean.TRUE);
		tree.put("java.util.", Boolean.FALSE);
		tree.put("java.lang.Boolean", Boolean.FALSE);

		Assert.assertFalse(tree.isEmpty());
		Assert.assertEquals(1, tree.getValues("java.lang.Integer").size());
		Assert.assertEquals(1, tree.getValues("java.lang.Long").size());
		Assert.assertEquals(2, tree.getValues("java.lang.Boolean").size());
		Assert.assertEquals(2, tree.getValues("java.io.InputStream").size());
		Assert.assertEquals(1, tree.getValues("java.util.ArrayList").size());
		Assert.assertEquals(2, tree.getValues("java.util.concurrent.ConcurrentHashMap").size());
	}

	@Test
	public void hasStartsWithMatch(){
		RadixTree<String, Boolean> tree = RadixTree.createTree(new StringSequencer());

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assert.assertEquals(1, tree.getValues("wowzacowza").size());
	}

	@Test
	public void hasExactMatch(){
		RadixTree<String, Boolean> tree = RadixTree.createTree(new StringSequencer());

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assert.assertTrue(tree.containsKey("wowza"));
	}

	@Test
	public void getStartsWithMatch(){
		RadixTree<String, Boolean> tree = RadixTree.createTree(new StringSequencer());

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assert.assertEquals(1, tree.getValues("wowzacowza").size());
		Assert.assertEquals(1, tree.getValues("bookshelfmania").size());
	}

	@Test
	public void getExactMatch(){
		RadixTree<String, Boolean> tree = RadixTree.createTree(new StringSequencer());

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assert.assertEquals(Boolean.FALSE, tree.get("wowza"));
		Assert.assertEquals(Boolean.TRUE, tree.get("bookshelf"));
		Assert.assertNull(tree.get("bookshelf2"));
	}

	@Test
	public void removeBack(){
		RadixTree<String, Integer> tree = RadixTree.createTree(new StringSequencer());

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
		RadixTree<String, Integer> tree = RadixTree.createTree(new StringSequencer());

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
		RadixTree<String, Integer> tree = RadixTree.createTree(new StringSequencer());

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


	@Test
	public void testSearchForPartialParentAndLeafKeyWhenOverlapExists(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assert.assertEquals(0, tree.getValuesPrefixedBy("abe").size());
		Assert.assertEquals(0, tree.getValuesPrefixedBy("abd").size());
	}

	@Test
	public void testSearchForLeafNodesWhenOverlapExists(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assert.assertEquals(1, tree.getValuesPrefixedBy("abcd").size());
		Assert.assertEquals(1, tree.getValuesPrefixedBy("abce").size());
	}

	@Test
	public void testSearchForStringSmallerThanSharedParentWhenOverlapExists(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assert.assertEquals(2, tree.getValuesPrefixedBy("ab").size());
		Assert.assertEquals(2, tree.getValuesPrefixedBy("a").size());
	}

	@Test
	public void testSearchForStringEqualToSharedParentWhenOverlapExists(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assert.assertEquals(2, tree.getValuesPrefixedBy("abc").size());
	}

	@Test
	public void testInsert(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("bat", "bat");
		tree.put("ape", "ape");
		tree.put("bath", "bath");
		tree.put("banana", "banana");

		Assert.assertEquals(new RadixTreeNode<>("ple", "apple"), tree.findPrefixedBy("apple"));
		Assert.assertEquals(new RadixTreeNode<>("t", "bat"), tree.findPrefixedBy("bat"));
		Assert.assertEquals(new RadixTreeNode<>("e", "ape"), tree.findPrefixedBy("ape"));
		Assert.assertEquals(new RadixTreeNode<>("h", "bath"), tree.findPrefixedBy("bath"));
		Assert.assertEquals(new RadixTreeNode<>("nana", "banana"), tree.findPrefixedBy("banana"));
	}

	@Test
	public void testInsertExistingUnrealNodeConvertsItToReal(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("applepie", "applepie");
		tree.put("applecrisp", "applecrisp");

		Assert.assertFalse(tree.containsKey("apple"));

		tree.put("apple", "apple");

		Assert.assertTrue(tree.containsKey("apple"));
	}

	@Test
	public void testDuplicatesAllowed(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("apple", "apple");

		try{
			tree.put("apple", "apple2");

			Assert.assertTrue(true);
		}
		catch(DuplicateKeyException e){
			Assert.fail("Duplicate should have been allowed");
		}
	}

	@Test
	public void testDuplicatesNotAllowed(){
		RadixTree<String, String> tree = RadixTree.createTreeNoDuplicates(new StringSequencer());

		tree.put("apple", "apple");

		try{
			tree.put("apple", "apple2");

			Assert.fail("Duplicate should not have been allowed");
		}
		catch(DuplicateKeyException e){
			Assert.assertEquals("Duplicate key: 'apple'", e.getMessage());
		}
	}

	@Test
	public void testInsertWithRepeatingPatternsInKey(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("xbox 360", "xbox 360");
		tree.put("xbox", "xbox");
		tree.put("xbox 360 games", "xbox 360 games");
		tree.put("xbox games", "xbox games");
		tree.put("xbox xbox 360", "xbox xbox 360");
		tree.put("xbox xbox", "xbox xbox");
		tree.put("xbox 360 xbox games", "xbox 360 xbox games");
		tree.put("xbox games 360", "xbox games 360");
		tree.put("xbox 360 360", "xbox 360 360");
		tree.put("xbox 360 xbox 360", "xbox 360 xbox 360");
		tree.put("360 xbox games 360", "360 xbox games 360");
		tree.put("xbox xbox 361", "xbox xbox 361");

		Assert.assertEquals(12, tree.size());
	}

	@Test
	public void testDeleteNodeWithNoChildren(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("apple", "apple");

		Assert.assertNotNull(tree.remove("apple"));
	}

	@Test
	public void testDeleteNodeWithOneChild(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("applepie", "applepie");

		Assert.assertNotNull(tree.remove("apple"));
		Assert.assertTrue(tree.containsKey("applepie"));
		Assert.assertFalse(tree.containsKey("apple"));
	}

	@Test
	public void testDeleteNodeWithMultipleChildren(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("applepie", "applepie");
		tree.put("applecrisp", "applecrisp");

		Assert.assertNotNull(tree.remove("apple"));
		Assert.assertTrue(tree.containsKey("applepie"));
		Assert.assertTrue(tree.containsKey("applecrisp"));
		Assert.assertFalse(tree.containsKey("apple"));
	}

	@Test
	public void testCantDeleteSomethingThatDoesntExist(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		Assert.assertNull(tree.remove("apple"));
	}

	@Test
	public void testCantDeleteSomethingThatWasAlreadyDeleted(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.remove("apple");

		Assert.assertNull(tree.remove("apple"));
	}

	@Test
	public void testChildrenNotAffectedWhenOneIsDeleted(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");
		tree.put("applepie", "applepie");
		tree.put("ape", "ape");

		tree.remove("apple");

		Assert.assertTrue(tree.containsKey("appleshack"));
		Assert.assertTrue(tree.containsKey("applepie"));
		Assert.assertTrue(tree.containsKey("ape"));
		Assert.assertFalse(tree.containsKey("apple"));
	}

	@Test
	public void testSiblingsNotAffectedWhenOneIsDeleted(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("ball", "ball");

		tree.remove("apple");

		Assert.assertTrue(tree.containsKey("ball"));
	}

	@Test
	public void testCantDeleteUnrealNode(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("ape", "ape");

		Assert.assertNull(tree.remove("ap"));
	}

	@Test
	public void testCantFindRootNode(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		Assert.assertNull(tree.findPrefixedBy(""));
	}

	@Test
	public void testFindSimpleInsert(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("apple", "apple");

		Assert.assertNotNull(tree.findPrefixedBy("apple"));
	}

	@Test
	public void testContainsSimpleInsert(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("apple", "apple");

		Assert.assertTrue(tree.containsKey("apple"));
	}

	@Test
	public void testFindChildInsert(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("ape", "ape");
		tree.put("appletree", "appletree");
		tree.put("appleshackcream", "appleshackcream");

		Assert.assertNotNull(tree.findPrefixedBy("appletree"));
		Assert.assertNotNull(tree.findPrefixedBy("appleshackcream"));
		Assert.assertNotNull(tree.containsKey("ape"));
	}

	@Test
	public void testGetPrefixes(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("h", "h");
		tree.put("hey", "hey");
		tree.put("hell", "hell");
		tree.put("hello", "hello");
		tree.put("hat", "hat");
		tree.put("cat", "cat");

		Assert.assertFalse(tree.getValues("helloworld").isEmpty());
		Assert.assertTrue(tree.getValues("helloworld").contains("h"));
		Assert.assertTrue(tree.getValues("helloworld").contains("hell"));
		Assert.assertTrue(tree.getValues("helloworld").contains("hello"));
		Assert.assertTrue(!tree.getValues("helloworld").contains("he"));
		Assert.assertTrue(!tree.getValues("helloworld").contains("hat"));
		Assert.assertTrue(!tree.getValues("helloworld").contains("cat"));
		Assert.assertTrue(!tree.getValues("helloworld").contains("hey"));
		Assert.assertTrue(tree.getValues("animal").isEmpty());
	}

	@Test
	public void testContainsChildInsert(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("ape", "ape");
		tree.put("appletree", "appletree");
		tree.put("appleshackcream", "appleshackcream");

		Assert.assertTrue(tree.containsKey("appletree"));
		Assert.assertTrue(tree.containsKey("appleshackcream"));
		Assert.assertTrue(tree.containsKey("ape"));
	}

	@Test
	public void testCantFindNonexistantNode(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		Assert.assertNull(tree.findPrefixedBy("apple"));
	}

	@Test
	public void testDoesntContainNonexistantNode(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		Assert.assertFalse(tree.containsKey("apple"));
	}

	@Test
	public void testCantFindUnrealNode(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("ape", "ape");

		Assert.assertNull(tree.findPrefixedBy("ap"));
	}

	@Test
	public void testDoesntContainUnrealNode(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("ape", "ape");

		Assert.assertFalse(tree.containsKey("ap"));
	}

	@Test
	public void testSearchPrefix_LimitGreaterThanPossibleResults(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");
		tree.put("appleshackcream", "appleshackcream");
		tree.put("applepie", "applepie");
		tree.put("ape", "ape");

		List<String> result = tree.getValuesPrefixedBy("app");
		Assert.assertEquals(4, result.size());

		Assert.assertTrue(result.contains("appleshack"));
		Assert.assertTrue(result.contains("appleshackcream"));
		Assert.assertTrue(result.contains("applepie"));
		Assert.assertTrue(result.contains("apple"));
	}

	@Test
	public void testSearchPrefix_LimitLessThanPossibleResults(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");
		tree.put("appleshackcream", "appleshackcream");
		tree.put("applepie", "applepie");
		tree.put("ape", "ape");

		List<String> result = tree.getValuesPrefixedBy("appl");
		Assert.assertEquals(4, result.size());

		Assert.assertTrue(result.contains("appleshack"));
		Assert.assertTrue(result.contains("applepie"));
		Assert.assertTrue(result.contains("apple"));
	}

	@Test
	public void testGetSize(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");
		tree.put("appleshackcream", "appleshackcream");
		tree.put("applepie", "applepie");
		tree.put("ape", "ape");

		Assert.assertTrue(tree.size() == 5);
	}

	@Test
	public void testDeleteReducesSize(){
		RadixTree<String, String> tree = RadixTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");

		tree.remove("appleshack");

		Assert.assertTrue(tree.size() == 1);
	}

}
