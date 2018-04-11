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
		RadixTree<Integer> tree = RadixTree.createTree();
		Assert.assertEquals(5, tree.largestPrefixLength("abcdefg", "abcdexyz"));
		Assert.assertEquals(3, tree.largestPrefixLength("abcdefg", "abcxyz"));
		Assert.assertEquals(3, tree.largestPrefixLength("abcdefg", "abctuvxyz"));
		Assert.assertEquals(0, tree.largestPrefixLength("abcdefg", StringUtils.EMPTY));
		Assert.assertEquals(0, tree.largestPrefixLength(StringUtils.EMPTY, "abcxyz"));
		Assert.assertEquals(0, tree.largestPrefixLength("xyz", "abcxyz"));
	}

	@Test
	public void testEmptyTree(){
		RadixTree<Integer> tree = RadixTree.createTree();

		Assert.assertTrue(tree.isEmpty());
		Assert.assertEquals(0, tree.size());
	}

	@Test
	public void testSingleInsertion(){
		RadixTree<Integer> tree = RadixTree.createTree();

		tree.put("test", 1);

		Assert.assertEquals(1, tree.size());
		Assert.assertTrue(tree.containsKey("test"));
		Assert.assertFalse(tree.containsKey("tes"));
		Assert.assertFalse(tree.containsKey("testt"));
	}

	@Test
	public void testMultipleInsertions(){
		RadixTree<Integer> tree = RadixTree.createTree();

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
		RadixTree<Integer> tree = RadixTree.createTree();

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
		RadixTree<Integer> tree = RadixTree.createTree();

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
		RadixTree<Integer> tree = RadixTree.createTree();

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
		RadixTree<Integer> tree = RadixTree.createTree();

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
		RadixTree<BigInteger> tree = RadixTree.createTree();

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
		RadixTree<Integer> tree = RadixTree.createTree();

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
		RadixTree<Integer> tree = RadixTree.createTree();

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
		RadixTree<Integer> tree = RadixTree.createTree();

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
		RadixTree<Integer> tree = RadixTree.createTree();

		Assert.assertTrue(tree.isEmpty());
		Assert.assertFalse(tree.containsKey("word"));
		Assert.assertFalse(tree.containsKey(StringUtils.EMPTY));
	}

	@Test
	public void defaultValueConstructor(){
		RadixTree<Boolean> tree = RadixTree.createTree();

		Assert.assertNull(tree.get("meow"));

		tree.put("meow", Boolean.TRUE);

		Assert.assertEquals(Boolean.TRUE, tree.get("meow"));
		Assert.assertNull(tree.get("world"));
	}

	@Test
	public void simplePut(){
		RadixTree<Boolean> tree = RadixTree.createTree();

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
		RadixTree<Boolean> tree = RadixTree.createTree();

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assert.assertEquals(1, tree.getValuesWithPrefix("wowzacowza").size());
	}

	@Test
	public void hasExactMatch(){
		RadixTree<Boolean> tree = RadixTree.createTree();

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assert.assertTrue(tree.containsKey("wowza"));
	}

	@Test
	public void getStartsWithMatch(){
		RadixTree<Boolean> tree = RadixTree.createTree();

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assert.assertEquals(1, tree.getValuesWithPrefix("wowzacowza").size());
		Assert.assertEquals(1, tree.getValuesWithPrefix("bookshelfmania").size());
	}

	@Test
	public void getExactMatch(){
		RadixTree<Boolean> tree = RadixTree.createTree();

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assert.assertEquals(Boolean.FALSE, tree.get("wowza"));
		Assert.assertEquals(Boolean.TRUE, tree.get("bookshelf"));
		Assert.assertNull(tree.get("bookshelf2"));
	}

	@Test
	public void removeBack(){
		RadixTree<Integer> tree = RadixTree.createTree();

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
		RadixTree<Integer> tree = RadixTree.createTree();

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
		RadixTree<Integer> tree = RadixTree.createTree();

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
		RadixTree<String> tree = RadixTree.createTree();

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assert.assertEquals(0, tree.getValuesWithPrefix("abe").size());
		Assert.assertEquals(0, tree.getValuesWithPrefix("abd").size());
	}

	@Test
	public void testSearchForLeafNodesWhenOverlapExists(){
		RadixTree<String> tree = RadixTree.createTree();

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assert.assertEquals(1, tree.getValuesWithPrefix("abcd").size());
		Assert.assertEquals(1, tree.getValuesWithPrefix("abce").size());
	}

	@Test
	public void testSearchForStringSmallerThanSharedParentWhenOverlapExists(){
		RadixTree<String> tree = RadixTree.createTree();

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assert.assertEquals(2, tree.getValuesWithPrefix("ab").size());
		Assert.assertEquals(2, tree.getValuesWithPrefix("a").size());
	}

	@Test
	public void testSearchForStringEqualToSharedParentWhenOverlapExists(){
		RadixTree<String> tree = RadixTree.createTree();

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assert.assertEquals(2, tree.getValuesWithPrefix("abc").size());
	}

	@Test
	public void testInsert(){
		RadixTree<String> tree = RadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("bat", "bat");
		tree.put("ape", "ape");
		tree.put("bath", "bath");
		tree.put("banana", "banana");

		Assert.assertEquals(new RadixTreeNode("ple", "apple"), tree.find("apple"));
		Assert.assertEquals(new RadixTreeNode("t", "bat"), tree.find("bat"));
		Assert.assertEquals(new RadixTreeNode("e", "ape"), tree.find("ape"));
		Assert.assertEquals(new RadixTreeNode("h", "bath"), tree.find("bath"));
		Assert.assertEquals(new RadixTreeNode("nana", "banana"), tree.find("banana"));
	}

	@Test
	public void testInsertExistingUnrealNodeConvertsItToReal(){
		RadixTree<String> tree = RadixTree.createTree();

		tree.put("applepie", "applepie");
		tree.put("applecrisp", "applecrisp");

		Assert.assertFalse(tree.containsKey("apple"));

		tree.put("apple", "apple");

		Assert.assertTrue(tree.containsKey("apple"));
	}

	@Test
	public void testDuplicatesAllowed(){
		RadixTree<String> tree = RadixTree.createTree();

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
		RadixTree<String> tree = RadixTree.createTreeNoDuplicates();

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
		RadixTree<String> tree = RadixTree.createTree();

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
		RadixTree<String> tree = RadixTree.createTree();

		tree.put("apple", "apple");

		Assert.assertNotNull(tree.remove("apple"));
	}

	@Test
	public void testDeleteNodeWithOneChild(){
		RadixTree<String> tree = RadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("applepie", "applepie");

		Assert.assertNotNull(tree.remove("apple"));
		Assert.assertTrue(tree.containsKey("applepie"));
		Assert.assertFalse(tree.containsKey("apple"));
	}

	@Test
	public void testDeleteNodeWithMultipleChildren(){
		RadixTree<String> tree = RadixTree.createTree();

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
		RadixTree<String> tree = RadixTree.createTree();

		Assert.assertNull(tree.remove("apple"));
	}

	@Test
	public void testCantDeleteSomethingThatWasAlreadyDeleted(){
		RadixTree<String> tree = RadixTree.createTree();

		tree.put("apple", "apple");
		tree.remove("apple");

		Assert.assertNull(tree.remove("apple"));
	}

	@Test
	public void testChildrenNotAffectedWhenOneIsDeleted(){
		RadixTree<String> tree = RadixTree.createTree();

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
		RadixTree<String> tree = RadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("ball", "ball");

		tree.remove("apple");

		Assert.assertTrue(tree.containsKey("ball"));
	}

	@Test
	public void testCantDeleteUnrealNode(){
		RadixTree<String> tree = RadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("ape", "ape");

		Assert.assertNull(tree.remove("ap"));
	}

	@Test
	public void testCantFindRootNode(){
		RadixTree<String> tree = RadixTree.createTree();

		Assert.assertNull(tree.find(""));
	}

	@Test
	public void testFindSimpleInsert(){
		RadixTree<String> tree = RadixTree.createTree();

		tree.put("apple", "apple");

		Assert.assertNotNull(tree.find("apple"));
	}

	@Test
	public void testContainsSimpleInsert(){
		RadixTree<String> tree = RadixTree.createTree();

		tree.put("apple", "apple");

		Assert.assertTrue(tree.containsKey("apple"));
	}

	@Test
	public void testFindChildInsert(){
		RadixTree<String> tree = RadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("ape", "ape");
		tree.put("appletree", "appletree");
		tree.put("appleshackcream", "appleshackcream");

		Assert.assertNotNull(tree.find("appletree"));
		Assert.assertNotNull(tree.find("appleshackcream"));
		Assert.assertNotNull(tree.containsKey("ape"));
	}

	@Test
	public void testGetPrefixes(){
		RadixTree<String> tree = RadixTree.createTree();

		tree.put("h", "h");
		tree.put("hey", "hey");
		tree.put("hell", "hell");
		tree.put("hello", "hello");
		tree.put("hat", "hat");
		tree.put("cat", "cat");

		Assert.assertFalse(tree.getValuesWithPrefix("helloworld").isEmpty());
		Assert.assertTrue(tree.getValuesWithPrefix("helloworld").contains("h"));
		Assert.assertTrue(tree.getValuesWithPrefix("helloworld").contains("hell"));
		Assert.assertTrue(tree.getValuesWithPrefix("helloworld").contains("hello"));
		Assert.assertTrue(!tree.getValuesWithPrefix("helloworld").contains("he"));
		Assert.assertTrue(!tree.getValuesWithPrefix("helloworld").contains("hat"));
		Assert.assertTrue(!tree.getValuesWithPrefix("helloworld").contains("cat"));
		Assert.assertTrue(!tree.getValuesWithPrefix("helloworld").contains("hey"));
		Assert.assertTrue(tree.getValuesWithPrefix("animal").isEmpty());
	}

	@Test
	public void testContainsChildInsert(){
		RadixTree<String> tree = RadixTree.createTree();

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
		RadixTree<String> tree = RadixTree.createTree();

		Assert.assertNull(tree.find("apple"));
	}

	@Test
	public void testDoesntContainNonexistantNode(){
		RadixTree<String> tree = RadixTree.createTree();

		Assert.assertFalse(tree.containsKey("apple"));
	}

	@Test
	public void testCantFindUnrealNode(){
		RadixTree<String> tree = RadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("ape", "ape");

		Assert.assertNull(tree.find("ap"));
	}

	@Test
	public void testDoesntContainUnrealNode(){
		RadixTree<String> tree = RadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("ape", "ape");

		Assert.assertFalse(tree.containsKey("ap"));
	}

	@Test
	public void testSearchPrefix_LimitGreaterThanPossibleResults(){
		RadixTree<String> tree = RadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");
		tree.put("appleshackcream", "appleshackcream");
		tree.put("applepie", "applepie");
		tree.put("ape", "ape");

		List<String> result = tree.getValuesWithPrefix("app");
		Assert.assertEquals(4, result.size());

		Assert.assertTrue(result.contains("appleshack"));
		Assert.assertTrue(result.contains("appleshackcream"));
		Assert.assertTrue(result.contains("applepie"));
		Assert.assertTrue(result.contains("apple"));
	}

	@Test
	public void testSearchPrefix_LimitLessThanPossibleResults(){
		RadixTree<String> tree = RadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");
		tree.put("appleshackcream", "appleshackcream");
		tree.put("applepie", "applepie");
		tree.put("ape", "ape");

		List<String> result = tree.getValuesWithPrefix("appl");
		Assert.assertEquals(4, result.size());

		Assert.assertTrue(result.contains("appleshack"));
		Assert.assertTrue(result.contains("applepie"));
		Assert.assertTrue(result.contains("apple"));
	}

	@Test
	public void testGetSize(){
		RadixTree<String> tree = RadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");
		tree.put("appleshackcream", "appleshackcream");
		tree.put("applepie", "applepie");
		tree.put("ape", "ape");

		Assert.assertTrue(tree.size() == 5);
	}

	@Test
	public void testDeleteReducesSize(){
		RadixTree<String> tree = RadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");

		tree.remove("appleshack");

		Assert.assertTrue(tree.size() == 1);
	}

	@Test
	public void testCompletePrefix(){
		RadixTree<String> tree = RadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");
		tree.put("applepie", "applepie");
		tree.put("applegold", "applegold");
		tree.put("applegood", "applegood");

		Assert.assertEquals("", tree.completePrefix("z"));
		Assert.assertEquals("apple", tree.completePrefix("a"));
		Assert.assertEquals("apple", tree.completePrefix("app"));
		Assert.assertEquals("appleshack", tree.completePrefix("apples"));
		Assert.assertEquals("applego", tree.completePrefix("appleg"));
	}

}
