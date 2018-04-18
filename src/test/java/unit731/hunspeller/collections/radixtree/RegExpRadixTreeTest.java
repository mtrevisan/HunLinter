package unit731.hunspeller.collections.radixtree;

import unit731.hunspeller.collections.radixtree.tree.DuplicateKeyException;
import org.junit.Assert;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import unit731.hunspeller.collections.radixtree.sequencers.RegExpSequencer;


public class RegExpRadixTreeTest{

	private final SecureRandom rng = new SecureRandom();


	@Test
	public void testEmptyTree(){
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

		Assert.assertTrue(tree.isEmpty());
		Assert.assertEquals(0, tree.size());
	}

	@Test
	public void testSingleInsertion(){
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

		tree.put("test", 1);

		Assert.assertEquals(1, tree.size());
		Assert.assertTrue(tree.containsKey("test"));
		Assert.assertFalse(tree.containsKey("tes"));
		Assert.assertFalse(tree.containsKey("testt"));
	}

	@Test
	public void testMultipleInsertions(){
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

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
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

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
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

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
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

		tree.put("pook", 1);
		tree.put("spook", 2);

		Assert.assertEquals(2, tree.size());
		Assert.assertEquals(tree.keySet().stream().map(key -> String.join(StringUtils.EMPTY, key)).collect(Collectors.toSet()),
			new HashSet<>(Arrays.asList("pook", "spook")));
	}

	@Test
	public void testRemoval(){
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

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
		RegExpRadixTree<BigInteger> tree = RegExpRadixTree.createTree();

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
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

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
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

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
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

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
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

		Assert.assertTrue(tree.isEmpty());
		Assert.assertFalse(tree.containsKey("word"));
		Assert.assertFalse(tree.containsKey(StringUtils.EMPTY));
	}

	@Test
	public void defaultValueConstructor(){
		RegExpRadixTree<Boolean> tree = RegExpRadixTree.createTree();

		Assert.assertNull(tree.get("meow"));

		tree.put("meow", Boolean.TRUE);

		Assert.assertEquals(Boolean.TRUE, tree.get("meow"));
		Assert.assertNull(tree.get("world"));
	}

	@Test
	public void simplePut(){
		RegExpRadixTree<Boolean> tree = RegExpRadixTree.createTree();

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
		RegExpRadixTree<Boolean> tree = RegExpRadixTree.createTree();

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assert.assertEquals(1, tree.getValuesWithPrefix("wowzacowza").size());
	}

	@Test
	public void hasExactMatch(){
		RegExpRadixTree<Boolean> tree = RegExpRadixTree.createTree();

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assert.assertTrue(tree.containsKey("wowza"));
	}

	@Test
	public void getStartsWithMatch(){
		RegExpRadixTree<Boolean> tree = RegExpRadixTree.createTree();

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assert.assertEquals(1, tree.getValuesWithPrefix("wowzacowza").size());
		Assert.assertEquals(1, tree.getValuesWithPrefix("bookshelfmania").size());
	}

	@Test
	public void getExactMatch(){
		RegExpRadixTree<Boolean> tree = RegExpRadixTree.createTree();

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assert.assertEquals(Boolean.FALSE, tree.get("wowza"));
		Assert.assertEquals(Boolean.TRUE, tree.get("bookshelf"));
		Assert.assertNull(tree.get("bookshelf2"));
	}

	@Test
	public void removeBack(){
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

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
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

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
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

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
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assert.assertEquals(0, tree.getValuesWithPrefix("abe").size());
		Assert.assertEquals(0, tree.getValuesWithPrefix("abd").size());
	}

	@Test
	public void testSearchForLeafNodesWhenOverlapExists(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assert.assertEquals(1, tree.getValuesWithPrefix("abcd").size());
		Assert.assertEquals(1, tree.getValuesWithPrefix("abce").size());
	}

	@Test
	public void testSearchForStringSmallerThanSharedParentWhenOverlapExists(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assert.assertEquals(2, tree.getValuesWithPrefix("ab").size());
		Assert.assertEquals(2, tree.getValuesWithPrefix("a").size());
	}

	@Test
	public void testSearchForStringEqualToSharedParentWhenOverlapExists(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assert.assertEquals(2, tree.getValuesWithPrefix("abc").size());
	}

//	@Test
//	public void testInsert(){
//		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();
//
//		tree.put("apple", "apple");
//		tree.put("bat", "bat");
//		tree.put("ape", "ape");
//		tree.put("bath", "bath");
//		tree.put("banana", "banana");
//
//		Assert.assertEquals(new RadixTreeNode(RegExpSequencer.splitSequence("ple"), "apple"), tree.find("apple"));
//		Assert.assertEquals(new RadixTreeNode(RegExpSequencer.splitSequence("t"), "bat"), tree.find("bat"));
//		Assert.assertEquals(new RadixTreeNode(RegExpSequencer.splitSequence("e"), "ape"), tree.find("ape"));
//		Assert.assertEquals(new RadixTreeNode(RegExpSequencer.splitSequence("h"), "bath"), tree.find("bath"));
//		Assert.assertEquals(new RadixTreeNode(RegExpSequencer.splitSequence("nana"), "banana"), tree.find("banana"));
//	}

	@Test
	public void testInsertExistingUnrealNodeConvertsItToReal(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("applepie", "applepie");
		tree.put("applecrisp", "applecrisp");

		Assert.assertFalse(tree.containsKey("apple"));

		tree.put("apple", "apple");

		Assert.assertTrue(tree.containsKey("apple"));
	}

	@Test
	public void testDuplicatesAllowed(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

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
		RegExpRadixTree<String> tree = RegExpRadixTree.createTreeNoDuplicates();

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
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

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
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");

		Assert.assertNotNull(tree.remove("apple"));
	}

	@Test
	public void testDeleteNodeWithOneChild(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("applepie", "applepie");

		Assert.assertNotNull(tree.remove("apple"));
		Assert.assertTrue(tree.containsKey("applepie"));
		Assert.assertFalse(tree.containsKey("apple"));
	}

	@Test
	public void testDeleteNodeWithMultipleChildren(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

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
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		Assert.assertNull(tree.remove("apple"));
	}

	@Test
	public void testCantDeleteSomethingThatWasAlreadyDeleted(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");
		tree.remove("apple");

		Assert.assertNull(tree.remove("apple"));
	}

	@Test
	public void testChildrenNotAffectedWhenOneIsDeleted(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

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
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("ball", "ball");

		tree.remove("apple");

		Assert.assertTrue(tree.containsKey("ball"));
	}

	@Test
	public void testCantDeleteUnrealNode(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("ape", "ape");

		Assert.assertNull(tree.remove("ap"));
	}

	@Test
	public void testCantFindRootNode(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		Assert.assertNull(tree.find(""));
	}

	@Test
	public void testFindSimpleInsert(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");

		Assert.assertNotNull(tree.find("apple"));
	}

	@Test
	public void testContainsSimpleInsert(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");

		Assert.assertTrue(tree.containsKey("apple"));
	}

	@Test
	public void testFindChildInsert(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

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
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

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
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

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
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		Assert.assertNull(tree.find("apple"));
	}

	@Test
	public void testDoesntContainNonexistantNode(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		Assert.assertFalse(tree.containsKey("apple"));
	}

	@Test
	public void testCantFindUnrealNode(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("ape", "ape");

		Assert.assertNull(tree.find("ap"));
	}

	@Test
	public void testDoesntContainUnrealNode(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("ape", "ape");

		Assert.assertFalse(tree.containsKey("ap"));
	}

	@Test
	public void testSearchPrefix_LimitGreaterThanPossibleResults(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

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
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

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
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");
		tree.put("appleshackcream", "appleshackcream");
		tree.put("applepie", "applepie");
		tree.put("ape", "ape");

		Assert.assertTrue(tree.size() == 5);
	}

	@Test
	public void testDeleteReducesSize(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");

		tree.remove("appleshack");

		Assert.assertTrue(tree.size() == 1);
	}


	@Test
	public void splitting(){
		String[] parts = RegExpSequencer.splitSequence("abd");
		Assert.assertArrayEquals(new String[]{"a", "b", "d"}, parts);

		parts = RegExpSequencer.splitSequence("a[b]d");
		Assert.assertArrayEquals(new String[]{"a", "[b]", "d"}, parts);

		parts = RegExpSequencer.splitSequence("a[bc]d");
		Assert.assertArrayEquals(new String[]{"a", "[bc]", "d"}, parts);

		parts = RegExpSequencer.splitSequence("a[^b]d");
		Assert.assertArrayEquals(new String[]{"a", "[^b]", "d"}, parts);

		parts = RegExpSequencer.splitSequence("a[^bc]d");
		Assert.assertArrayEquals(new String[]{"a", "[^bc]", "d"}, parts);
	}

	@Test
	public void containsRegExp(){
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

		tree.put("a[bd]c", 1);
		tree.put("a[bd]b", 2);
		tree.put("a[^bcd]", 3);
		tree.put("a", 4);

		Assert.assertTrue(tree.containsKey("a"));
		Assert.assertTrue(tree.containsKey("abc"));
		Assert.assertTrue(tree.containsKey("adc"));
		Assert.assertFalse(tree.containsKey("aec"));
		Assert.assertTrue(tree.containsKey("ae"));
		Assert.assertFalse(tree.containsKey("ac"));
		Assert.assertFalse(tree.containsKey("c"));
	}

	@Test
	public void duplicatedEntryRegExp(){
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

		tree.put("a[bc]c", 1);
		tree.put("a[bc]c", 2);

		Assert.assertFalse(tree.containsKey("a"));
		Assert.assertFalse(tree.containsKey("ab"));
		Assert.assertTrue(tree.containsKey("abc"));
		Assert.assertEquals(2, tree.get("acc").intValue());
		Assert.assertFalse(tree.containsKey("c"));
	}

	@Test
	public void collectPrefixesRegExp(){
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

		tree.put("a", 1);
		tree.put("a[bcd]", 2);
		tree.put("[^ac]c", 3);
		tree.put("cd", 4);
		tree.put("aec", 5);

		Integer[] datas = tree.getValuesWithPrefix("abcd")
			.toArray(new Integer[0]);
		Assert.assertArrayEquals(new Integer[]{1, 2}, datas);
		datas = tree.getValuesWithPrefix("ec")
			.toArray(new Integer[0]);
		Assert.assertArrayEquals(new Integer[]{3}, datas);
	}

}
